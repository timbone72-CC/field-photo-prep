# Field Photo Prep — Project Profile

## Product

Field Photo Prep is a native Android field-photo workflow for capturing, protecting, preparing, and safely sending photos to the exact operator-selected Google Drive work-order destination with minimal field friction.

## Authoritative systems

**GitHub**
- repository: `timbone72-CC/field-photo-prep`;
- completed source baseline: `main`;
- in-progress source: the single authoritative active branch/PR plus its build-state record.

**Android local state**
- protected photo records/files;
- upload/reconciliation queue state;
- device-local SAF/provider identity;
- encrypted FPP auth/session state defined by the identity contract.

Do not reconstruct local identity from folder names, UI assumptions, or another device's provider IDs.

**Google Drive / Android DocumentsProvider**
- authoritative for client-company, property/address, work-order folders, and remote photos;
- Android SAF/DocumentsProvider is the Drive access boundary;
- FPP authentication does not grant Drive access, and Drive identity does not grant FPP Organization membership.

**Supabase**
- the dedicated original-FPP project is authoritative for Auth User, Organization, Membership, Invitation, and approved server-side identity administration/audit;
- it is separate from Field Photo Prep Team;
- it is not the job/photo database and does not replace Drive.

## Protected invariants

Unless an approved change explicitly modifies them:
- captured originals remain protected until safe completion/recovery permits cleanup;
- queued photos keep their immutable stored destination identity;
- ambiguous remote outcomes fail closed instead of blind retry;
- use disposable fixtures instead of live customer jobs whenever possible;
- Drive writes use the exact approved provider identity, not guessed names;
- provider IDs/SAF URIs are device/platform-context identity and are not assumed portable;
- FPP identity and Drive authorization stay separate;
- Android never contains Supabase service-role/secret credentials;
- Field Photo Prep Team stays separate;
- sign-out/revocation/account/UI changes do not delete protected local work or Drive business data unless that destructive behavior is explicitly approved.

## Consistency boundaries

Preserve the established Home / Work Orders / Photos workflow unless approved design changes it.

Reuse established terminology and status meanings. Put actionable status where the operator can identify the affected job, not only in global counts.

Avoid duplicate controls or alternate flows for the same action unless an explicit use case requires them. Keep normal screens field-first and free of developer terminology.

UI requests/renders behavior; it does not become a second persistence, authorization, queue, or Drive implementation.

## External state requiring parity

When touched, reconcile under `GOVERNANCE.md`:
- Supabase schema/migrations;
- RPC grants/RLS/function definitions;
- Edge Functions;
- supported Auth redirect/configuration;
- Android package/signing/release configuration;
- deployed runtime artifacts when applicable.

Google Drive customer/work content is operational data, not source-controlled configuration; its reality gates are governed by the Drive/integration rules.

## Detailed rule owners

- product/photo/queue: `CONTRACT.md`
- change level/approval/rollback: `CHANGE_CONTROL_CONTRACT.md`
- universal testing: `TESTING_CONTRACT.md`
- feature testing: `rules/testing/*`
- Drive/DocumentsProvider: `INTEGRATION_CONTRACT.md`
- regression inventory: `REGRESSION_CHECKLIST.md`
- staging/device boundaries: `docs/PHASE_STAGING_DOCTRINE.md`
- identity: `docs/IDENTITY_MODEL_V1.md`
- current approved phase/design records: scope-specific behavior

Use `RULE_INDEX.md` to load only what applies.
