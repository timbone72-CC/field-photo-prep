# Phase 8C — Second-Phone Availability Update

Date: 2026-09-28
Status: **AVAILABLE — PORTABILITY TESTING NOT YET PERFORMED**

The operator reports that a second suitable Android phone is now available for the Phase 8C reality gate.

This changes only the execution input for the deferred gate. It does not claim that portability has passed and does not alter the audit findings in `docs/FIELD_PHOTO_PREP_AUDIT_2026-09-28.md`.

The remaining Phase 8C gate is unchanged:

- install the internal build normally on the second phone;
- use that phone user's own Google Drive access to select the approved shared workspace;
- reopen a disposable existing address/work order without duplicates;
- capture, prepare, and upload one disposable photo;
- verify destination and visual result;
- restart and confirm safe state; and
- prove the app does not assume SAF/provider IDs are portable between phones or accounts.

No device test, Drive write, account change, or live-system mutation was performed by this update.

## 2026-09-29 — Samsung A16 pre-install checkpoint

Status: **VERIFIED INSTALLER READY — DEVICE UPDATE AND PORTABILITY TEST PENDING**

### Governance

- Goal/surfaces: preserve the second-phone setup evidence and verified installer provenance in this status document only.
- Change level: Level 1; no Android runtime or configuration changes.
- Authoritative line: `docs/field-photo-prep-audit-20260928`, PR #89.
- Rules: AGENTS.md, GOVERNANCE.md, PROJECT_PROFILE.md, RULE_INDEX.md, CHANGE_CONTROL_CONTRACT.md and TESTING_CONTRACT.md; Phase 8C procedure and applicable Drive/provider, staging, camera/preparation and queue rules govern subsequent physical testing.
- Protected behavior: photo protection, queue state, Drive destination identity, authorization, signing and release behavior are unchanged.
- External systems touched: GitHub documentation branch and delivery of the existing CI artifact; no Drive, Supabase or device mutation performed in this checkpoint.
- Rollback: documentation parent `b96720aa44a25d76f4a933b42306cf35f2bbe081`.
- Verification boundary: documentation review and artifact provenance/hash comparison. Physical installation and provider behavior remain unverified.

### Operator and screenshot evidence

- Second device: Samsung A16, with the app already installed and the same Google account as the primary phone (operator reported).
- App info screenshot: Field Photo Prep Internal, version `0.13-field-ui-internal`.
- Earlier app screenshots: HNP Jobs, Drive connected, 20 properties. These are historical/pre-update observations, not a current-build portability pass.
- Operator confirmed no pending/unreconciled photos on the A16.
- Same-account second-device testing cannot establish different-account sharing or Drive ACL isolation.
- Phase 12 and the 53/53 UNCERTAIN reconciliation remain resolved; this checkpoint does not reopen them.

### Verified installer provenance

- Current documented main: `2ffa723c9be500765503af31a4c53baef7fdb992`.
- CI runtime source: `c7970238e00139415a417f669d86915c4260154e`; changes from this source to the documented main are Markdown only.
- Successful Android CI run: https://github.com/timbone72-CC/field-photo-prep/actions/runs/36316289354
- Artifact: `field-photo-prep-internal-apk`, ID `10930657806`.
- Delivered filename: `Field-Photo-Prep-Internal-0.28.0-v37.apk`.
- Package: `com.inandout.fieldphotoprep.internal`.
- Version: `0.28.0-internal`, versionCode `37`.
- Downloaded artifact ZIP SHA-256 matches GitHub metadata: `81a954d6660e2e3045021f7f2d9001197830df8f177a5fc8c285d45295fa1f06`.
- Extracted APK SHA-256 matches CI build evidence: `67782f7acacb2a9f226960895f758a6c36d4556e7dd65797d3f231ea413fb146`.
- CI-verified signer SHA-256: `2c0a9616fd819333ed98b33593fbe597e120e95936103c6b7a76270e985d3fba`. The installed device certificate has not been independently read.

### Remaining physical verification

1. Open this APK on the A16 and use the Android in-place Update flow. Preserve app data; do not uninstall or clear storage. If installation fails, record the exact error and stop.
2. Record the first screen after opening and verify the updated version. Resolve the observed authentication/workspace state using the documented flow before test writes.
3. Select the approved workspace using this device's provider access, then reopen a disposable existing address/work order without duplication.
4. Capture, prepare and upload one disposable photo; inspect the exact Drive destination and visual result.
5. Restart and confirm safe state. Record actual outcomes before changing Phase 8C status.

No installation, disposable-photo upload, restart verification, or portability pass is claimed by this record.
