# Address and Work-Order Fresh Refresh — 2026-09-30

Status: IMPLEMENTED / FULL ANDROID CI PASSED — awaiting physical Samsung/Google Drive read-only gate; Level 3 merge and release NOT APPROVED.
Authoritative branch: `fix/address-workorder-fresh-refresh`.
Governed base/rollback: `c9e6965dc934d697b18e7b62e5f4ebbc0dfb9656` (main, including merged Phase 13 PR #93 and closeout PR #94).
Affected code owner: `MainActivity.refreshAddressFolders` and `MainActivity.refreshWorkOrderFolders`.
Required governance: AGENTS.md, GOVERNANCE.md, PROJECT_PROFILE.md, RULE_INDEX.md, CHANGE_CONTROL_CONTRACT.md, CONTRACT.md, TESTING_CONTRACT.md, INTEGRATION_CONTRACT.md, rules/testing/DRIVE_PROVIDER.md, affected REGRESSION_CHECKLIST sections.

## Reproduced field symptom and external evidence

For HNP's live customer property displayed as `509 SOUTH BOUNDARY WALTERS OK`, on 2026-09-30 the Work Orders screen continued to display an undated `GRASS CUT` after explicit refresh and a full app restart. The connected Google Drive address folder independently listed only `GRASS CUT - 2026-09-29`, containing 54 verified September 29 photographs. A separately named historical address folder `509_W_SOUTH_BOUNDARY_WALTERS_OK` existed but was independently empty after the user moved the dated folder. The older undated `GRASS CUT` remained individually addressable through Drive metadata, though it was absent from the address's current direct-children list. The mobile provider's exact local document IDs and account/provider context have not been exported; do NOT equate connector Drive IDs with Android SAF provider IDs.

The implementation uses `DriveClient.listFolders` for address and work-order screen refresh. That method can return a single non-loading cached query without `listFoldersFresh` settled-state verification. This is a plausible contributor to the mismatch, not independently proven as the only cause.

## Scope, risk and safety

Change the two existing read-only UI folder-refresh call sites to require the existing `DriveClient.listFoldersFresh` boundary; retain exact selected property and work-order identity reconciliation. No new identity format, company selection behavior, persistence schema, queue rewriting, Drive write, photo deletion, folder movement, retry behavior, or broad refresh framework. No changes to FPP Team.

Classify Level 3 conservatively because address/work-order listings can influence selection of the upload destination. The correct outcome is the fresh settled list or an explicit error/fail-closed state, never an unverified guess by folder name. If a selected old work-order provider ID is no longer returned, the existing reconciliation must clear selection and persisted current-work-order preference; it must not silently substitute the similarly named dated folder.

## Verification and rollback

1. Inspect the exact diff and prevent unrelated changes. Existing provider-freshness tests cover the shared DriveClient boundary; CI must build the unchanged method semantics and pass the complete suite on the final source head. The real Samsung/Google Drive observation is required before treating the field defect as resolved.
2. Real-device gate on operator-approved, non-destructive navigation: on the existing selected HNP company, tap refresh on Home and open the intended 509 address. Compare displayed property name and dated work-order listing to the independently observed Drive hierarchy. A real provider inability to establish fresh state must show a failure, not a stale success. Do not capture, reuse, delete, rename, rebind accounts, or move any live customer folders for this gate.
3. Use disposable provider fixtures for any additional tests requiring folder mutation. Protect pending local work and preserve all existing customer images. Do not delete the empty alternate address or old work-order folders as part of this code correction. Any later external cleanup requires its own identity and pending-work guard.
4. CI pass is necessary but cannot prove real Google Drive sync on the field device. Stop at a physical gate if freshness remains inconsistent.
5. Roll back only this narrow MainActivity read-path change to base `c9e6965dc934d697b18e7b62e5f4ebbc0dfb9656` if the affected read path cannot establish usable provider state. Rollback must not change Drive content, protected originals or pending destinations.
6. Level 3 requires explicit operator pre-merge approval after all applicable proof. Do not merge or distribute a new APK based solely on the branch or CI.

## External state

No Drive or runtime external state will be mutated for this implementation. The 54 live photos in `GRASS CUT - 2026-09-29` are independently verified under the intended existing Google Drive address; remote metadata/listing disagreement about the legacy undated folder remains an unresolved observation pending physical-provider verification.

## Automated verification checkpoint

- Scoped runtime change commit: `c5b0e3d984796efe8b266648bff6d031af6f4093`.
- Exact runtime Android CI run `36730707716`: **SUCCESS**; single test job completed successfully, including unit tests, production identity checks, internal debug build, fail-closed production signing verification, stable test APK signer, APK evidence tooling, complete instrumented tests and internal launch smoke.
- APK artifact `field-photo-prep-internal-apk`, GitHub artifact ID `11104807787`; signed internal build `0.28.5-internal`, versionCode `42` per the unchanged build.gradle source. Extracted `app-debug.apk` SHA-256: `57e6a3318a4d8a71f60a048b805823ee16664c3df0943a374991be0a957f21ec`.
- Reviewed runtime PR patch confirms exactly two read-only call-site changes, both `listFolders` → `listFoldersFresh`; no change to DriveClient, photo storage, exact identities or Drive writes.
- This documentation-only status update does not change runtime behavior. Runtime is **NOT YET** verified on a physical device; no live HNP cleanup, release or merge is authorized by this checkpoint.

Next gate: operator installs the same-signer internal APK as an in-place update without uninstalling/clearing any live application data, opens HNP's intended 509 SOUTH BOUNDARY property, and supplies the actual Work Orders result. Expected verified listing is `GRASS CUT - 2026-09-29`; alternatively an explicit provider freshness/read error is informative. If old undated GRASS CUT remains, stop and diagnose local provider ID/binding instead of assuming this read-path fix cured the root cause. Preserve the live 54 photos and both currently identified Drive address folders throughout.

## Field screenshot after distributing candidate (2026-09-30)

Operator supplied Work Orders screenshot for 509 SOUTH BOUNDARY after receiving the PR #95 APK: display still reads selected undated `GRASS CUT`, row subtitle `Existing work order`. The screenshot does **not** expose installed APK fingerprint/build commit, Android provider IDs, or evidence that the downloaded APK was successfully installed. The PR #95 physical reality gate therefore remains **UNVERIFIED / SCREEN STILL WRONG**; don't claim this code change fixed the field defect. Verify actual installed APK identity before concluding the fresh listing still returns stale data. If correct install is proven, investigate selected Android SAF address ID, provider account/tree and exact fresh child-folder identities without automatic rebinding or customer Drive writes. Keep both legacy folders and all 54 known-good September 29 photos untouched.

## ADB provider-read probe after failed external query

2026-09-30: The operator confirmed the installed PR #95 APK by package `com.inandout.fieldphotoprep.internal` and exact SHA-256 `57e6a3318a4d8a71f60a048b805823ee16664c3df0943a374991be0a957f21ec`. Its in-app selected company is `HNP Jobs`, address `509 SOUTH BOUNDARY WALTERS OK`, and saved selected work order is undated `GRASS CUT`. Persisted SAF workspace and company share account prefix `acc=12`; that prefix alone is **not** evidence they point to the Google Drive folder inspected through the connector. The user ran `adb shell run-as ... /system/bin/content query` for the saved tree/address; Android rejected it with `ACCESS_CONTENT_PROVIDERS_EXTERNALLY`. This is a shell-tool permission boundary, not FPP's in-process permission failure. Do not request root, chmod, clearing caches or re-granting unrelated system permissions.

The normal in-app provider reader has no logging/export of resolved child names/IDs. Temporarily instrument **only the internal debug build** on the existing PR #95 authoritative branch: read-only `Log.i` of folder parent IDs/names, current saved navigation IDs, and returned settled folder names/IDs from the existing Home/work-order fresh refresh calls; log failures. No additional provider queries, persistent state or UI behavior, automatic identity substitution, folder deletion, or production-release logging. Capture logs with `adb logcat` while navigating the app on the physical device. Remove temporary diagnostic instrumentation before final merge, rerun focused checks and final CI. This scope expansion remains Level 3, with all earlier safety constraints and rollback unchanged.

## Temporary in-app probe build and CI verification

- Probe runtime source commit: `9400e6bdfd7a41ed4b224fd111034190292e9e9d` (same authoritative PR #95). Temporary diagnostic logs are guarded by `BuildConfig.DEBUG`, limit address candidates to the selected street number, report only trailing provider-ID fingerprints and avoid tree URIs; the selected address's returned work orders are included. No new provider query, mutation, persisted data or production-release diagnostic was added.
- Final probe runtime Android CI run `36734301078`: **SUCCESS**; includes unit, internal build/signing, production identity and Android emulator instrumentation/launch smoke. APK artifact name `field-photo-prep-internal-apk`, ID `11106387720`, extracted file `app-debug.apk` SHA-256 `fe78997ff1fc0f0b95f393d2ac4ee14a429231cd274343a5b4054bd768c3948e`. Its version stays `0.28.5-internal` / versionCode `42`; only exact APK fingerprint distinguishes it from the prior PR #95 candidate.
- Next **read-only physical gate**: operator installs this same-signer APK over the existing internal app with `adb install -r` (never uninstall or clear data). Reopen FPP and refresh HNP addresses, open 509 SOUTH BOUNDARY work orders; run `adb logcat -d -s FPPFolderProbe:I '*:S'`. Compare `ADDRESSES` matching-address candidate ID tails and `WORK_ORDERS` selected parent / returned children to known persisted IDs. If no output, confirm installed APK fingerprint before further changes. Do not automatically map SAF IDs to Google Drive API IDs or infer causality from names alone.
- Preserve real HNP contents, existing exact identities and photo queues. Do not merge, clean up old folders, or claim the root cause is established until the real device report is reconciled. Remove diagnostic code before final merge and rerun required final checks.

## Reconciled physical Samsung diagnostic gate — 2026-09-30

The operator installed the actual internal diagnostic build on the physical Samsung and opened HNP / 509 SOUTH BOUNDARY. ADB logcat `FPPFolderProbe` from 10:17–10:21 local revealed two repeated, settled in-app Drive refreshes:

- `ADDRESSES`: parent displayed as `HNP Jobs` with saved parent ID suffix `sBO9vZKXSF4=`; 24 child folders; displayed `509 SOUTH BOUNDARY WALTERS OK` child ID suffix `Mpf5p-FDVEU=` exactly matches the saved address suffix. Neither refresh emitted an alternate `509` candidate.
- `WORK_ORDERS`: selected address ID suffix `Mpf5p-FDVEU=`; saved selected undated `GRASS CUT` with ID suffix `6rHlWaPnOMI=`; **one returned child**, also undated `GRASS CUT` with precisely that saved ID suffix. This proves the live app's `listFoldersFresh` returned the undated work-order folder; it is not merely stale local UI selection. It does NOT establish whether provider and cloud refer to the same backend Google Drive folder, because Android DocumentsProvider IDs and Google Drive API IDs cannot be safely equated here.
- An external read-only check using connected personal Drive (timbone72@gmail.com) again found **24 HNP Jobs child folders**, including the canonical named address; direct children of canonical API folder contained **one dated** `GRASS CUT - 2026-09-29` (not undated). The dated work-order contained **54 images**. The individually fetchable old undated `GRASS CUT` and alternate address returned former parent metadata, but **neither appeared in current direct child lists**. The connector did not establish whether historical folders are trashed or cached; don't claim that they are.
- A separate connected business Drive account could not fetch the personal account's canonical address folder by Google Drive API ID. This is not a verified mapping from Android provider `acc=12` to either account.

**Gate decision: FAIL / unresolved backend-provider view mismatch.** The read-path code change alone has not resolved the defect. Do not merge draft PR #95, move/delete either customer folder, clear FPP data, edit persisted provider IDs or upload to the undated folder. A repeat of the same listFoldersFresh call is not the next diagnostic.

**Next single non-destructive boundary:** On the **same physical Samsung**, open the *Google Drive app*, explicitly select `timbone72@gmail.com`, navigate `HNP Jobs → 509 SOUTH BOUNDARY WALTERS OK`, and show which work-order folder(s) the Drive app itself lists. This separates Android SAF provider-only stale listing from an account/folder-tree discrepancy. Keep all external contents unchanged. If Drive UI shows dated child, investigate Samsung Google Drive DocumentsProvider cache/read identity, with a safe offline-work/queue check before any cache changes. If Drive UI shows undated child or wrong parent/account, investigate account selection and shared-folder location without silently rebinding FPP.

Temp diagnostic code remains on authoritative draft PR #95; remove it before any final merge and rerun required focused/full checks after a bounded solution has passed physical evidence.

## Physical Samsung Drive app reconciliation

Operator confirmed that the *regular Google Drive app on the same Samsung*, after the requested personal-account HNP address check, displays the dated work-order folder with the September 29 photos. Thus the Drive app agrees with the connected personal Drive API while FPP's authorized Android SAF DocumentsProvider tree at saved address identity still reports only the undated GRASS CUT. Do not yet assert stale cache as sole root cause: the picker/provider may expose a different account scope or folder identity even when display names match.

**Next safe discriminating gate**: launch the stock Android `ACTION_OPEN_DOCUMENT_TREE` picker from ADB without returning any selection to FPP, navigate the *same* Google Drive personal account → HNP Jobs → 509 SOUTH BOUNDARY WALTERS OK, and observe whether the picker shows dated versus undated GRASS CUT. Do NOT tap `Use this folder`, approve access, select a new FPP workspace, clear cache/data, or mutate Drive. The standalone picker cannot on its own prove equivalence to FPP's saved exact provider ID; preserve that uncertainty and reconcile based on observed folders. If the picker cannot navigate Drive or does not launch, record the blocker and use an alternative non-destructive in-app identity probe.

## Standalone Android picker result

The operator ran a standalone Android `OPEN_DOCUMENT_TREE` on the same Samsung and navigated Google Drive → HNP Jobs → 509 SOUTH BOUNDARY WALTERS OK **without selecting Use this folder**. The system picker showed the **dated** work-order folder, agreeing with the same-device Google Drive app and the connected Google Drive API. Meanwhile the read-only FPP diagnostic in its existing saved account/tree/address context returned the **undated** work order after settled refresh. This strongly narrows the discrepancy to saved address/provider context or provider-scoped stale listing. It does *not* yet prove identical underlying IDs across picker and FPP or identify the cause. No permissions were granted or changed, no folders were selected for FPP, no Drive contents modified.

Next zero-mutation gate: cancel standalone picker, reopen FPP on HNP's existing 509 address, tap Work Orders refresh, inspect only recent `FPPFolderProbe` logcat lines. If it still reports undated `GRASS CUT`, stop repeating refresh and instrument a narrowly scoped, non-persisting in-app picker-ID comparison on the same existing PR; **do not** overwrite saved tree/selected address, create any work orders or modify queue identities. The existing diagnostic must be removed prior to merge and all required gates rerun.
