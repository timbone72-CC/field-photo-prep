#!/usr/bin/env bash
set -euo pipefail

APK="${1:?Usage: verify-release-apk.sh path/to/app.apk}"

required_env=(
  FPP_EXPECTED_PACKAGE
  FPP_EXPECTED_VERSION_CODE
  FPP_EXPECTED_VERSION_NAME
  FPP_EXPECTED_AUTH_REDIRECT_SCHEME
  FPP_EXPECTED_AUTH_REDIRECT_HOST
  FPP_EXPECTED_SUPABASE_URL
  FPP_EXPECTED_SUPABASE_PUBLISHABLE_KEY
  FPP_EXPECTED_CERT_SHA256
)

for name in "${required_env[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
done

if [[ ! -f "${APK}" ]]; then
  echo "APK not found: ${APK}" >&2
  exit 1
fi

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "${ANDROID_HOME}" ]]; then
  echo "ANDROID_HOME or ANDROID_SDK_ROOT is required." >&2
  exit 1
fi

AAPT="$(find "${ANDROID_HOME}/build-tools" -type f -name aapt | sort -V | tail -n 1)"
APKSIGNER="$(find "${ANDROID_HOME}/build-tools" -type f -name apksigner | sort -V | tail -n 1)"
if [[ -z "${AAPT}" || -z "${APKSIGNER}" ]]; then
  echo "Could not locate aapt/apksigner in Android build-tools." >&2
  exit 1
fi

normalize_fingerprint() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]' | tr -d ':[:space:]'
}

badging="$("${AAPT}" dump badging "${APK}" | head -n 1)"
actual_package="$(printf '%s\n' "${badging}" | sed -n "s/.*package: name='\([^']*\)'.*/\1/p")"
actual_version_code="$(printf '%s\n' "${badging}" | sed -n "s/.*versionCode='\([^']*\)'.*/\1/p")"
actual_version_name="$(printf '%s\n' "${badging}" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p")"

[[ "${actual_package}" == "${FPP_EXPECTED_PACKAGE}" ]] || {
  echo "Package mismatch: expected ${FPP_EXPECTED_PACKAGE}, got ${actual_package}" >&2
  exit 1
}
[[ "${actual_version_code}" == "${FPP_EXPECTED_VERSION_CODE}" ]] || {
  echo "versionCode mismatch: expected ${FPP_EXPECTED_VERSION_CODE}, got ${actual_version_code}" >&2
  exit 1
}
[[ "${actual_version_name}" == "${FPP_EXPECTED_VERSION_NAME}" ]] || {
  echo "versionName mismatch: expected ${FPP_EXPECTED_VERSION_NAME}, got ${actual_version_name}" >&2
  exit 1
}

if [[ -n "${FPP_MIN_PREVIOUS_VERSION_CODE:-}" ]]; then
  if (( actual_version_code <= FPP_MIN_PREVIOUS_VERSION_CODE )); then
    echo "versionCode ${actual_version_code} must be greater than previous ${FPP_MIN_PREVIOUS_VERSION_CODE}." >&2
    exit 1
  fi
fi

manifest_dump="$(mktemp)"
dex_strings="$(mktemp)"
signer_dump="$(mktemp)"
cleanup() {
  rm -f "${manifest_dump}" "${dex_strings}" "${signer_dump}"
}
trap cleanup EXIT

"${AAPT}" dump xmltree "${APK}" AndroidManifest.xml > "${manifest_dump}"

grep -Eq "android:scheme.*\"${FPP_EXPECTED_AUTH_REDIRECT_SCHEME}\"" "${manifest_dump}" || {
  echo "Expected auth redirect scheme is missing from AndroidManifest.xml." >&2
  exit 1
}
grep -Eq "android:host.*\"${FPP_EXPECTED_AUTH_REDIRECT_HOST}\"" "${manifest_dump}" || {
  echo "Expected auth redirect host is missing from AndroidManifest.xml." >&2
  exit 1
}

while IFS= read -r dex; do
  unzip -p "${APK}" "${dex}" | strings
done < <(unzip -Z1 "${APK}" | grep -E '^classes([0-9]+)?\.dex$') > "${dex_strings}"

grep -Fq "${FPP_EXPECTED_SUPABASE_URL}" "${dex_strings}" || {
  echo "Expected dedicated Supabase URL is missing from APK bytecode." >&2
  exit 1
}
grep -Fq "${FPP_EXPECTED_SUPABASE_PUBLISHABLE_KEY}" "${dex_strings}" || {
  echo "Expected Supabase publishable key is missing from APK bytecode." >&2
  exit 1
}

if grep -Eqi 'sb_secret_|service[_-]?role' "${dex_strings}"; then
  echo "Potential Supabase secret/service-role material detected in APK bytecode." >&2
  exit 1
fi

if [[ -n "${FPP_FORBIDDEN_STRING:-}" ]] && grep -Fq "${FPP_FORBIDDEN_STRING}" "${dex_strings}" "${manifest_dump}"; then
  echo "Forbidden release identity string detected: ${FPP_FORBIDDEN_STRING}" >&2
  exit 1
fi

"${APKSIGNER}" verify --print-certs "${APK}" > "${signer_dump}"
actual_cert="$(sed -n 's/.*certificate SHA-256 digest: *//p' "${signer_dump}" | head -n 1)"
actual_cert="$(normalize_fingerprint "${actual_cert}")"
expected_cert="$(normalize_fingerprint "${FPP_EXPECTED_CERT_SHA256}")"

[[ "${actual_cert}" == "${expected_cert}" ]] || {
  echo "Signer mismatch: expected ${expected_cert}, got ${actual_cert}" >&2
  exit 1
}

apk_sha256="$(sha256sum "${APK}" | awk '{print $1}')"
evidence_path="${FPP_EVIDENCE_PATH:-release-evidence.txt}"
mkdir -p "$(dirname "${evidence_path}")"

cat > "${evidence_path}" <<EOF
Field Photo Prep release artifact evidence
source_commit=${FPP_SOURCE_COMMIT:-unknown}
package=${actual_package}
version_code=${actual_version_code}
version_name=${actual_version_name}
auth_callback=${FPP_EXPECTED_AUTH_REDIRECT_SCHEME}://${FPP_EXPECTED_AUTH_REDIRECT_HOST}
supabase_project_ref=vtyiktvqhbgabawotkrj
supabase_url=${FPP_EXPECTED_SUPABASE_URL}
supabase_publishable_key_present=yes
signer_sha256=${actual_cert}
apk_sha256=${apk_sha256}
EOF

cat "${evidence_path}"
