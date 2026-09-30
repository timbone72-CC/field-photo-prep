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
