# Stable Test Signing Impact Record — 2026-09-10

Status: **IMPLEMENTATION AUTHORIZED — runtime/build behavior not yet changed by this record**

## Problem

Physical Phase 7B installation on the Samsung Galaxy A16 was blocked because successive GitHub Actions debug APKs were signed by different ephemeral Android debug keys. Android correctly refused the Phase 7B APK with `App not installed as package conflicts with an existing package.`

This prevents update-in-place testing and would repeatedly break any device gate that depends on preserving app-private queue/photo state across APK upgrades.

## Change level

**Level 3.** Android signing/deployment changes are explicitly high risk under `CHANGE_CONTROL_CONTRACT.md`.

The operator explicitly authorized this signing fix after the blocker was identified.

## Exact base / rollback point

Repository: `timbone72-CC/field-photo-prep`

Branch: `feat/stable-test-signing`

Exact base / rollback point:

`19c48cc43bf932480df0c322d1f9be1c1568398e`

That base contains the staged Phase 7B documentation; the exact frozen Phase 7B runtime remains:

`f25868dd768dcdddb11ac4d6ab3879a7cde85d2c`

## Approved behavior

Create one stable **non-production** Android signing identity for repository debug/test APKs so future test builds of the same `applicationId` can update one another without uninstalling and losing app-private test state.

The stable test signer is not a release credential and must never be used as the production/release signing identity.

## Signing identity

Alias: `field-photo-prep-test`

Certificate SHA-256:

`2C:0A:96:16:FD:81:93:33:ED:98:B3:35:93:FB:E5:97:E1:20:E9:59:36:10:3C:6B:7A:76:27:0E:98:5D:3F:BA`

Keystore file SHA-256:

`9de15c655cb50e86fbc84bbd48b1f44c1fbb3a6dd6de50a7077ca8cc00a31197`

The key is intentionally checked into this private repository as a test-only asset with non-secret test credentials. It provides package-update continuity only; it is not relied on for production authenticity or secrecy.

## Owning files

Expected build/test surfaces:

- `ci/field-photo-prep-test.jks` — stable test-only signing asset;
- `ci/README.md` — explicit non-production limitations and release prohibition;
- `app/build.gradle` — bind only the Android `debug` build type to the stable test signer;
- `.github/workflows/android-ci.yml` — verify the produced debug APK is signed by the expected certificate before it is packaged as an artifact.

No production Java/Kotlin runtime source, manifest permission, Drive integration, local queue schema, photo handling, destination identity, upload logic, cleanup logic, or application ID is in scope.

## Read/write surfaces

Build-time reads:

- checked-in test keystore;
- debug signing configuration;
- generated debug APK signer certificate.

Build-time writes:

- signed debug APK and normal CI artifacts only.

Runtime data writes: **none introduced by this change.**

Drive writes: **none introduced by this change.**

## Protected behavior

This change must preserve all existing Phase 7B runtime semantics exactly, including:

- `applicationId` remains `com.inandout.fieldphotoprep`;
- versionCode/versionName remain owned by their phase runtime;
- exact photo/work-order destination identity is unchanged;
- no queue/schema migration is added;
- no Drive permission, create, upload, reconciliation, cleanup, or retry behavior changes;
- no production/release signing configuration is created;
- the stable test key cannot silently become the release signing key.

## Security boundary

The checked-in signer is deliberately **not secret**. Anyone who can read the private repository could sign an APK with this test identity. Therefore:

- it is suitable only for internal debug/test packages;
- it must not be used to establish production trust;
- a future production release must use a distinct secured release key;
- release builds remain outside this signing configuration and outside this change.

## Verification plan

Focused verification:

1. Build a debug APK and verify its signer certificate SHA-256 equals the fingerprint above.
2. Repeat from a separate CI run/branch and verify the same signer fingerprint.
3. Produce an H2 compatibility fixture APK using the same test signer.
4. Produce the Phase 7B APK using the same test signer.
5. Verify both APKs have the same package name and signer while retaining their intended version codes (`14` for H2 fixture, `15` for Phase 7B).

Final automated gate:

- complete existing repository CI once on the final signing-change head;
- CI must fail before artifact publication if signer verification does not match the expected certificate.

Physical update smoke:

- once the existing ephemeral-signed app is no longer needed, uninstall it exactly once;
- install the stable-signed H2 fixture;
- create one confirmed-upload retained-local-data fixture;
- install the stable-signed Phase 7B APK **over** H2 without uninstalling or clearing data;
- Android must accept the update;
- continue the existing Phase 7B device gate to verify local cleanup and Drive preservation.

## Existing-device data safety

The currently installed ephemeral-signed H2 test app cannot be updated to the new signer because Android intentionally forbids changing signatures in place.

Before uninstalling it, confirm the only app-private data intentionally sacrificed is disposable test state whose Drive photo has already been independently confirmed. The existing H4 Drive JPEG is already confirmed in Drive; no live customer/job data is used for this signing transition.

The retained-local-cleanup upgrade fixture will therefore be recreated once using the stable-signed H2 fixture before testing the H2 → Phase 7B update.

## Failure posture

- signer mismatch in CI: fail the build/publication path;
- package name drift: fail verification;
- H2/Phase 7B stable-signed APK signer mismatch: stop device testing;
- Android still refuses stable-signed H2 → Phase 7B update: stop and inspect exact package/signer/version evidence; do not uninstall around the second failure;
- no failure authorizes Drive or queue changes.

## Rollback

For repository behavior, revert the stable-signing change to exact base `19c48cc43bf932480df0c322d1f9be1c1568398e`.

For a device already moved onto the stable test signer, do not attempt to install an older ephemeral-signed APK over it. If rollback of test APK lineage becomes necessary, preserve any unconfirmed photo first, then uninstall/reinstall only under an explicit device recovery step.

## Approval status

Implementation authorization: **GRANTED by operator on 2026-09-10.**

Pre-merge authorization: **NOT YET GRANTED.** Passing tests/device checks will not merge this Level-3 change without explicit operator approval.
