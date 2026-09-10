# Phase 8B — Internal / Production Package Separation

Date: 2026-09-10

Branch: `phase8b/internal-production-package-separation`

Governed base / rollback: `c0a709e628ea4cc869de73cd57119a8707d9fa79`

## Problem

The stable test signing identity is intentionally checked into the repository and is therefore **not a production trust identity**. It currently signs debug APKs using the final Android application ID `com.inandout.fieldphotoprep`.

Android requires an update of one application ID to be signed by a compatible signing identity. If the project later switches `com.inandout.fieldphotoprep` from the public test key to a secure production key, Android will reject that as an update. The project already observed this class of failure once when two debug builds used different signing identities.

The clean release-hardening fix is to stop using the final production application ID for internal/test APKs before real field data begins accumulating.

## Change level

**Level 3** because Android application identity, signing/update continuity, and delivery behavior are affected.

Implementation is authorized by the operator's instruction to continue Phase 8. Explicit pre-merge approval remains required after automated and physical install/update verification.

## Approved behavior

### Internal/test build

Debug/internal APKs will use:

- application ID: `com.inandout.fieldphotoprep.internal`
- app label: `Field Photo Prep Internal`
- the existing stable **non-production** test signing identity
- the same runtime functionality and local data schema as the production code line

Internal builds remain updateable over later internal builds as long as the stable test signer and suffixed package identity remain unchanged.

### Future production build

The production application ID remains:

`com.inandout.fieldphotoprep`

The checked-in stable test key must **not** sign that production application ID.

No production signing private key is created or committed by this phase. A future production signer must be stored outside source control and backed up by the operator before public/external distribution.

The ordinary `release` variant may remain unsigned until that secure signing identity is deliberately configured.

## Runtime semantics protected unchanged

This phase must not change:

- SAF master-folder selection or persisted permission semantics;
- address/work-order provider identity rules;
- provider freshness behavior;
- Phase 3B Clear & Reuse semantics;
- camera capture implementation;
- protected original handling;
- photo preparation;
- queue schema/state transitions;
- upload destination construction;
- upload confirmation;
- UNCERTAIN reconciliation;
- confirmed local cleanup;
- Drive sharing/permissions.

## Required data / migration

No app data migration between the old test package and the new internal package is attempted.

Android treats `com.inandout.fieldphotoprep.internal` as a separate app, so its app-private data and persisted SAF grants start clean. This is intentional and safer than trying to copy private state between package identities.

The existing test app on the Samsung Galaxy A16 contains only disposable test state; all photos used for completed upload gates have confirmed Drive copies and confirmed local cleanup. Therefore the old package can remain installed while the new internal package is validated, then be removed after the internal package is proven.

## FileProvider / platform identity

`AndroidManifest.xml` already declares FileProvider authority as `${applicationId}.fileprovider`, and capture code derives the runtime authority from `getPackageName() + ".fileprovider"`.

Therefore the internal suffix should naturally produce:

`com.inandout.fieldphotoprep.internal.fileprovider`

without hard-coded production authority reuse.

## Planned files

- `app/build.gradle`
  - add debug/internal application ID suffix;
  - add internal version-name suffix;
  - continue stable test signing on debug only;
  - bump project version for the new Phase 8B build.
- `app/src/main/AndroidManifest.xml`
  - move the visible label to an app-name string resource so build-specific labels can differ.
- `app/src/main/res/values/strings.xml`
  - production/default app label `Field Photo Prep`.
- `app/src/debug/res/values/strings.xml`
  - internal app label `Field Photo Prep Internal`.
- `.github/workflows/android-ci.yml`
  - launch/pid smoke uses the internal application ID;
  - artifact name becomes `field-photo-prep-internal-apk` instead of the inherited Phase-1 name;
  - stable signer verification remains unchanged.
- `.gitignore`
  - ignore future untracked production keystore files (`*.jks`, `*.keystore`) while leaving the already tracked governed test keystore intact.

## Duplicate / Drive behavior

None. Package separation does not change Drive duplicate rules. A newly installed internal app must select the approved master through the system picker and then rely on actual Drive provider state to discover/reuse existing address and work-order folders.

It must not recreate folders merely because its app-private state starts empty.

## Offline / stale-state behavior

No semantic change. The new package begins with no local queue history. Once photos are captured under the internal package, all existing offline/restart/UNCERTAIN rules apply unchanged.

## Automated verification

Final Phase 8B head must pass:

1. complete JVM/unit suite;
2. debug/internal APK build;
3. stable test signer fingerprint check;
4. Android instrumentation suite;
5. install/launch smoke targeting `com.inandout.fieldphotoprep.internal`;
6. artifact packaging under the new internal artifact name.

Review the diff to confirm no photo/Drive/queue logic changed.

## Physical Android reality gate

Use the Samsung Galaxy A16 first.

1. Leave the currently installed old test package in place.
2. Install the new internal APK; it should install **side-by-side**, not as an update/conflict.
3. Confirm the launcher label clearly says `Field Photo Prep Internal`.
4. Open the internal app and select `HNP Jobs` through the system picker.
5. Refresh/discover `FIELD PHOTO PREP TEST` and reopen an existing safe work order without creating duplicate folders.
6. Take/prepare/upload one disposable photo under the safe test work order.
7. Confirm its Drive file appears under the correct existing work-order parent and looks usable.
8. Confirm local cleanup after success.
9. Restart the internal app and confirm persisted master access and confirmed metadata survive.
10. Confirm the old test package and new internal package are distinct installs.
11. After the internal package passes, the old final-package test build may be uninstalled because its retained test data is no longer needed.

This gate also prepares the internal build for Phase 8C testing on the second Android phone.

## Failure handling

- If the internal APK conflicts with the old package, stop; do not uninstall either app until the package identity is inspected.
- If FileProvider/camera capture fails only under the suffix, stop and inspect authority resolution before retrying capture.
- If folder discovery would create a duplicate, stop the Drive action and preserve evidence.
- If upload becomes UNCERTAIN, follow existing Phase 7B rules and do not blindly retry.
- Do not use a production signing private key as a troubleshooting shortcut.

## Rollback

Before merge, rollback is the governed base `c0a709e628ea4cc869de73cd57119a8707d9fa79`.

If Phase 8B is rejected, the old test package remains usable with its existing stable test signer. No remote Drive migration or cleanup is required because package separation itself does not alter Drive content.

## Merge approval

**Not yet merged.** Explicit Level-3 operator approval is required after the automated gate and the physical A16 package-separation smoke pass.
