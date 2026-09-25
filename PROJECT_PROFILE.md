# Field Photo Prep — Project Profile

## Purpose

This file defines the FPP-specific systems and permanent safety boundaries that `GOVERNANCE.md` operates on.

## Product

Field Photo Prep is a native Android field-photo workflow for capturing, protecting, preparing, and safely sending photos to the exact operator-selected Google Drive work-order destination with minimal field friction.

## Authoritative systems

### GitHub
- Repository: `timbone72-CC/field-photo-prep`
- Completed code baseline: `main`
- In-progress work: the single authoritative active branch/PR plus its build-state/impact record

Do not hard-code the current phase PR here.

### Android local state
Android app-private state is authoritative for protected local photos/files, queue/reconciliation state, device-local provider/SAF identity, and encrypted FPP auth/session state where defined by the identity contract.

Do not reconstruct that state casually from names, UI assumptions, or another device's provider IDs.

### Google Drive / Android DocumentsProvider
Google Drive remains authoritative for client-company folders, property/address folders, work-order folders, and remote photos.

Android SAF/DocumentsProvider is the approved Drive access boundary.

FPP authentication does not grant Drive access. Drive account identity does not grant FPP Organization membership.

### Supabase
The dedicated original-FPP Supabase project is authoritative for FPP account identity: Auth User, Organization, Membership, Invitation, and approved server-side membership/invitation administration.

Original FPP remains separate from Field Photo Prep Team.

Supabase is not the job/photo database and does not replace Google Drive for client work.

## Protected invariants

Unless an approved change explicitly modifies them:

- captured originals remain protected until safe completion/recovery rules permit cleanup;
- queued photos retain immutable stored destination identity;
- ambiguous remote outcomes fail closed instead of blind retry;
- disposable fixtures are used instead of live customer work when possible;
- Drive writes use exact approved provider identity, not guessed folder-name matches;
- provider IDs/SAF URIs are device/platform-context identity and are not assumed portable;
- FPP identity and Google Drive authorization remain separate;
- Android never contains Supabase service-role/secret credentials;
- Field Photo Prep Team remains a separate backend/product;
- sign-out, revocation, account changes, or UI cleanup do not delete protected local work or Drive business data unless that destructive behavior is explicitly approved.

## Consistency boundaries

Field Photo Prep should feel like one app, not a collection of phase-specific flows.

Unless intentionally redesigned:
- preserve Home / Work Orders / Photos navigation;
- reuse established terminology for photos, work orders, companies, account states, and queue states;
- place operator-facing status where it helps identify the affected job, not only in global counters;
- avoid duplicate controls or alternate pathways for the same action without a real use case;
- keep normal screens field-first and free of developer/internal terminology;
- keep one authoritative module/state owner for each concern;
- UI requests/renders behavior and does not become a second persistence, authorization, or Drive implementation.

## External systems requiring parity

When touched, reconcile these under `GOVERNANCE.md`:
- Supabase schema/migrations;
- Supabase RPC grants/RLS/function definitions;
- Supabase Edge Functions;
- supported Auth redirect/configuration;
- Android package/signing/release configuration;
- deployed runtime artifacts when applicable.

Google Drive customer/work content is project data, not source-controlled configuration. Its safety/reality gates are governed by `INTEGRATION_CONTRACT.md` and routed testing rules.

## Detailed rule owners

Use `RULE_INDEX.md` to route work to:
- `CONTRACT.md`;
- `CHANGE_CONTROL_CONTRACT.md`;
- `TESTING_CONTRACT.md` and `rules/testing/*`;
- `INTEGRATION_CONTRACT.md`;
- `REGRESSION_CHECKLIST.md`;
- `docs/PHASE_STAGING_DOCTRINE.md`;
- `docs/IDENTITY_MODEL_V1.md`;
- current approved phase/design/build-state records.
