# Phase 12K — Controlled Production Release Runbook

Date: 2026-09-26

This runbook prepares and verifies a **controlled private production APK candidate**. It does not publish to Google Play, create a public GitHub Release, enable public signup, or authorize outside-user distribution.

## Permanent production identity

- package: `com.inandout.fieldphotoprep`;
- callback: `com.inandout.fieldphotoprep://auth-callback`;
- Phase 12K candidate versionCode: `37`;
- versionName: `0.28.0`;
- Supabase project ref: `vtyiktvqhbgabawotkrj`;
- internal package remains `com.inandout.fieldphotoprep.internal`;
- internal stable test signer remains non-production and must never sign production.

The first production signer used for a distributed/installed production baseline becomes part of Android update continuity. Protect it accordingly.

## 1. Check the physical device for an old unsuffixed package

Before creating any permanent production key, connect the Android device to the operator-controlled Linux machine and run:

```bash
adb shell pm path com.inandout.fieldphotoprep
adb shell pm path com.inandout.fieldphotoprep.internal
```

Expected modern working package: `com.inandout.fieldphotoprep.internal`.

If the first command returns no APK path, there is no currently installed unsuffixed production-package app on that device and the first permanent production signer may be established.

If `com.inandout.fieldphotoprep` is present, **do not uninstall it yet**. Read its version and signer:

```bash
adb shell dumpsys package com.inandout.fieldphotoprep | grep -E 'versionCode=|versionName='
OLD_APK_PATH="$(adb shell pm path com.inandout.fieldphotoprep | head -n 1 | sed 's/^package://')"
adb pull "$OLD_APK_PATH" /tmp/fpp-existing-production-package.apk
APKSIGNER="$(find "$ANDROID_HOME/build-tools" -type f -name apksigner | sort -V | tail -n 1)"
"$APKSIGNER" verify --print-certs /tmp/fpp-existing-production-package.apk
```

Known historical non-production stable test signer SHA-256:

`2C:0A:96:16:FD:81:93:33:ED:98:B3:35:93:FB:E5:97:E1:20:E9:59:36:10:3C:6B:7A:76:27:0E:98:5D:3F:BA`

If the existing unsuffixed package uses any other signer, STOP: treat it as a potentially real production identity and recover/identify that key before continuing.

If it uses the known test signer, confirm that old package contains no protected live work that must be preserved before any uninstall. The current internal package must not be disturbed by this check.

Historical context: the test signer briefly signed unsuffixed debug/test builds before commit `ceda1509b0b2c94b602b76b312c828ff89f727bb` separated debug builds into `.internal`. Later versionCode 18 CI evidence is confirmed internal, but the pre-split package must still be checked on-device.

## 2. Create or recover the production keystore locally

Do this on the operator-controlled Linux machine, **not** in GitHub Actions and not in the repository.

Only after the package/signer check above proves a new signer is safe should a first production signer be created.

For a confirmed first production baseline, a suitable local command is:

```bash
mkdir -p "$HOME/.field-photo-prep/release"
chmod 700 "$HOME/.field-photo-prep/release"

keytool -genkeypair -v \
  -keystore "$HOME/.field-photo-prep/release/field-photo-prep-production.jks" \
  -alias field-photo-prep-production \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Choose strong unique passwords when prompted. Do not paste them into chat, shell history, source files, or repository docs.

Immediately record the certificate fingerprint:

```bash
keytool -list -v \
  -keystore "$HOME/.field-photo-prep/release/field-photo-prep-production.jks" \
  -alias field-photo-prep-production \
  | grep -i 'SHA256:'
```

Store an encrypted/offline backup of the keystore and its recovery information before distributing any APK signed by it.

## 3. Local signed release build

Set sensitive values only in the current shell/session or another secure local secret mechanism:

```bash
export FPP_RELEASE_STORE_FILE="$HOME/.field-photo-prep/release/field-photo-prep-production.jks"
export FPP_RELEASE_STORE_PASSWORD='...'
export FPP_RELEASE_KEY_ALIAS='field-photo-prep-production'
export FPP_RELEASE_KEY_PASSWORD='...'
export FPP_RELEASE_CERT_SHA256='AA:BB:...'
```

Then build:

```bash
gradle :app:verifyProductionReleaseIdentity :app:assembleRelease
```

The release build fails closed if any signing input is missing.

## 4. Verify the local APK

```bash
export FPP_EXPECTED_PACKAGE='com.inandout.fieldphotoprep'
export FPP_EXPECTED_VERSION_CODE='37'
export FPP_EXPECTED_VERSION_NAME='0.28.0'
export FPP_EXPECTED_AUTH_REDIRECT_SCHEME='com.inandout.fieldphotoprep'
export FPP_EXPECTED_AUTH_REDIRECT_HOST='auth-callback'
export FPP_EXPECTED_SUPABASE_URL='https://vtyiktvqhbgabawotkrj.supabase.co'
export FPP_EXPECTED_SUPABASE_PUBLISHABLE_KEY='sb_publishable_QXGedlj5wCmMpV2HncZp9A_kfgLzuAQ'
export FPP_EXPECTED_CERT_SHA256="$FPP_RELEASE_CERT_SHA256"
export FPP_MIN_PREVIOUS_VERSION_CODE='36'
export FPP_FORBIDDEN_STRING='com.inandout.fieldphotoprep.internal'
export FPP_SOURCE_COMMIT="$(git rev-parse HEAD)"
export FPP_EVIDENCE_PATH="$PWD/release-evidence/release-evidence.txt"

bash scripts/verify-release-apk.sh app/build/outputs/apk/release/app-release.apk
```

Keep the resulting evidence beside the exact APK being tested.

## 5. Configure GitHub Actions secrets

PR/source code references only secret **names**. The following values must be configured outside git before using the manual Production Release Candidate workflow:

- `FPP_RELEASE_KEYSTORE_B64` — base64 of the production keystore bytes;
- `FPP_RELEASE_STORE_PASSWORD`;
- `FPP_RELEASE_KEY_ALIAS`;
- `FPP_RELEASE_KEY_PASSWORD`;
- `FPP_RELEASE_CERT_SHA256`.

On Linux, create the base64 value without line wrapping:

```bash
base64 -w 0 "$HOME/.field-photo-prep/release/field-photo-prep-production.jks"
```

Do not save that base64 text into the repository.

The GitHub workflow materializes the keystore only in runner temporary storage and uploads only the signed APK and non-secret evidence.

## 6. Manual GitHub candidate

Run **Production Release Candidate** manually.

Provide the highest production versionCode already installed/distributed. For a confirmed first 12K production baseline with no prior production package, use the governed baseline value `36`.

The workflow must fail unless:
- signing secrets are present;
- versionCode 37 advances the supplied previous code;
- package is production package;
- callback is production callback;
- only the dedicated FPP Supabase URL and modern publishable client key are present;
- no obvious service-role/secret-key marker is detected;
- APK signer matches the expected production certificate;
- artifact SHA-256 is recorded.

The workflow does not publish the candidate publicly.

## 7. Production callback reality gate

The connected Supabase management interface used during 12K does not expose the Auth redirect allowlist, so the source configuration alone is not sufficient evidence.

On the Android device with the **signed production package** installed:

1. trigger the existing password-recovery flow for the controlled operator account;
2. confirm the recovery link returns to `com.inandout.fieldphotoprep://auth-callback` and opens the production FPP package;
3. complete the existing recovery flow;
4. confirm normal connected-account state returns;
5. confirm no localhost/browser dead-end occurs.

If the callback fails, fix the Supabase allowed redirect configuration. Do not weaken redirect parsing or substitute a localhost fallback.

## 8. Install/update continuity gate

### If no production package exists yet

Install the signed v37 APK as the first production baseline.

Record:
- APK SHA-256;
- signer SHA-256;
- source commit;
- package/version;
- successful launch/auth callback.

This establishes the production signer baseline. A subsequent production candidate must prove install-over-existing preservation before wider distribution.

### If a production package already exists

Before installing:
- verify its package is `com.inandout.fieldphotoprep`;
- identify its signer fingerprint;
- verify the new candidate uses the same signer;
- verify the candidate versionCode is higher.

Then install as an update **without uninstalling or clearing app data**.

After update verify:
- app opens normally;
- FPP account/session behavior remains correct;
- protected local work remains present;
- existing Drive binding/company context remains intact;
- no queue destination or provider identity changed.

A signer mismatch is a STOP condition. Never uninstall a live production app with protected work just to bypass the mismatch.

## 9. Rollback

Source rollback point: `c502260b7207d75a1349581a2f72b397e7fa5e70`.

Artifact rollback must use a previously retained APK that:
- has the same production package;
- is signed by the same production signer;
- is accepted by Android's version rules for the intended rollback method.

Ordinary Android installs do not permit versionCode downgrade without special/destructive handling. Therefore the safer response to a bad candidate is to stop distribution and produce a corrected higher-version candidate from a known-good source, preserving app data.

## 10. Phase 12K merge gate

Before merging PR #84, the build-state record must contain:
- final source/tooling CI PASS;
- production signer fingerprint;
- production candidate APK SHA-256;
- release evidence;
- production callback reality result;
- install/update continuity result appropriate to the actual existing production state;
- explicit operator approval: **APPROVED**.

Until then PR #84 remains Draft and Level-3 merge approval remains **PENDING**.
