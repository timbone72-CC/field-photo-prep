# Stable Test Signing Implementation Record — 2026-09-10

Status: **IMPLEMENTED / AUTOMATED VERIFICATION PASS / PHYSICAL UPDATE GATE PENDING**

Governed by `docs/STABLE_TEST_SIGNING_IMPACT_RECORD_2026-09-10.md`.

## Purpose

Fix Android update-in-place testing after the Samsung Galaxy A16 correctly rejected a Phase 7B APK signed by a different ephemeral GitHub Actions debug key.

## Exact signing-change line

Branch: `feat/stable-test-signing`

Base / rollback point:

`19c48cc43bf932480df0c322d1f9be1c1568398e`

Exact final tested build/runtime head:

`1d8d3acb95f84be4f5fefa0c1c0357265f58730a`

The underlying Phase 7B runtime behavior remains the previously frozen runtime; this change alters only debug/test signing and CI signer verification.

## Implemented surface

- added `ci/field-photo-prep-test.jks` as the stable non-production test signer;
- added `ci/README.md` documenting that the signer is not secret, is test-only, and must never sign production releases;
- bound only the Android `debug` build type to the stable signer in `app/build.gradle`;
- left the release signing configuration untouched;
- added a CI gate that verifies the built debug APK's certificate SHA-256 before emulator testing/artifact publication.

No Java runtime, manifest permission, application ID, queue schema, Drive behavior, capture behavior, upload/retry semantics, cleanup semantics, or remote/local data logic changed.

## Stable signing identity

Alias: `field-photo-prep-test`

Certificate SHA-256:

`2C:0A:96:16:FD:81:93:33:ED:98:B3:35:93:FB:E5:97:E1:20:E9:59:36:10:3C:6B:7A:76:27:0E:98:5D:3F:BA`

Keystore SHA-256:

`9de15c655cb50e86fbc84bbd48b1f44c1fbb3a6dd6de50a7077ca8cc00a31197`

This identity provides test-package update continuity only. It is not a production trust identity.

## Verification — Phase 7B stable-signed build

Exact head:

`1d8d3acb95f84be4f5fefa0c1c0357265f58730a`

GitHub Actions:

- run: `34516597291`
- job: `103003428037`
- result: **PASS**
- unit tests: PASS
- debug build: PASS
- stable signer verification: PASS
- Android instrumentation + install/launch smoke: PASS
- artifact upload: PASS
- artifact ID: `10168097825`
- artifact digest: `sha256:b0605953a02aac919e31800bbdb41fdab00969402ee4ab5add36c2a112b0a6d7`
- extracted APK SHA-256: `445a3c88da685f8af299ddea3d593f9c384369691e79fe243f9fc9202cb1d1a8`
- application ID remains `com.inandout.fieldphotoprep`
- versionCode remains `15`
- versionName remains `0.10-phase7b-reconciliation-cleanup`

## Verification — stable-signed H2 compatibility fixture

Fixture branch: `feat/stable-test-signing-h2-fixture`

Exact fixture head:

`5fcc06b937f9008c1dfa3265ea8d894b8b1f162b`

The fixture is based on H2 documentation head `d1e2e2c4ea60e0ebad57b097d29abc555dd2c4f3`, whose exact tested H2 runtime beneath it is `8d23b061307725589aef69a31a00249744523406`.

GitHub Actions:

- run: `34516638178`
- job: `103003565364`
- result: **PASS**
- unit tests: PASS
- debug build: PASS
- stable signer verification: PASS
- Android instrumentation + install/launch smoke: PASS
- artifact upload: PASS
- artifact ID: `10168100773`
- artifact digest: `sha256:07d23b3c09bb269075d0955cba67031d288a8741747d61a220a7a3b18bc36e6f`
- extracted APK SHA-256: `d895177b15fa078133fbccd02844b0f50d389314565f33aac9d555b0da25c85f`
- application ID remains `com.inandout.fieldphotoprep`
- versionCode remains `14`
- versionName remains `0.9-phase6b-drive-upload`

Both CI runs independently verified the same expected stable certificate fingerprint. Therefore the H2 fixture and Phase 7B test APKs are signing-compatible, while Phase 7B has the higher versionCode required for normal Android update installation.

## Focused failure encountered and corrected

The first CI signer-check implementation built the APK successfully but its shell parser failed to extract the digest from the runner's `apksigner` output. CI stopped before artifact publication as intended.

The parser was replaced with a robust `apksigner` output capture plus Python regex verification. The exact corrected heads above then passed signer verification and the complete existing Android suite.

No runtime behavior was changed to fix that test-harness failure.

## Existing-device safety check

Before authorizing removal of the currently installed ephemeral-signed test app, the previously confirmed H4 Drive JPEG was rechecked and still exists:

- `field-photo-88ba77d6-2af9-4a3d-ad07-0bdefe30232c.jpg`
- Google Drive backend ID `1wEEcTKq6hdOjzfR-1079F7Kh_D0opIsB`

Therefore the current app-private retained H4 image copies are disposable test data; the confirmed Drive source-of-truth copy is preserved.

## Physical update gate

Straight-line path:

1. uninstall the currently installed ephemeral-signed Field Photo Prep exactly once;
2. install the stable-signed H2 fixture APK (`versionCode 14`);
3. reconnect/select the approved `HNP Jobs` master if the uninstall removed the persisted SAF grant;
4. under the safe test hierarchy, capture one disposable photo, prepare it, upload it successfully once, and stop with H2 showing `UPLOADED` while local original/prepared copies remain;
5. install the stable-signed Phase 7B APK (`versionCode 15`) **over H2 without uninstalling or clearing data**;
6. Android must accept the update;
7. open the same work order and verify the confirmed upload metadata remains while Phase 7B removes the retained local original/prepared copies;
8. verify the Drive JPEG remains intact;
9. continue the already staged Phase 7B fresh-upload/restart smoke gate.

If Android rejects the stable H2 → Phase 7B update, stop. Do not uninstall around that second failure.

## Merge status

This Level-3 signing change is not merge-approved by this record. Explicit operator approval is still required before merge.
