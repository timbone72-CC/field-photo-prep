# Field Photo Prep — Project Profile

## Purpose

This file defines the project-specific systems and safety boundaries that the universal governance rules operate on. It does not replace the detailed contracts.

## Product

Field Photo Prep is a native Android field-photo workflow. Its job is to capture, protect, prepare, and safely send photos to the exact operator-selected Google Drive work-order destination while minimizing field friction.

## Authoritative systems

### GitHub

Repository:
- `timbone72-CC/field-photo-prep`

Completed source baseline:
- `main`

In-progress implementation:
- the single authoritative active branch/PR for that scope, discovered through open PRs and the active build-state record.

Do not hard-code a phase PR number here; active work changes over time.

### Android local state

Android app-private state is authoritative for:
- protected local photo records/files;
- upload/reconciliation queue state;
- device-local provider/SAF identity;
- encrypted local FPP auth/session state where defined by the identity contract.

Local state must never be casually rewritten from folder names, UI assumptions, or another device's provider IDs.

### Google Drive / Android DocumentsProvider

Google Drive remains authoritative for:
- client-company folders;
- property/address folders;
- work-order folders;
- remotely stored photos.

Android SAF/DocumentsProvider access is the approved Drive access boundary.

FPP authentication does not grant Drive access. Drive account identity does not grant FPP Organization membership.

### Supabase

The dedicated original-FPP Supabase project is authoritative for FPP account identity:
- Auth User;
- Organization;
- Membership;
- Invitation;
- server-side membership/invitation administration and its approved audit evidence.

The original FPP project remains separate from Field Photo Prep Team.

Supabase is not the job/photo database and does not replace Google Drive as the client-work data store.

## Protected project invariants

Unless an approved change explicitly modifies them:

- captured originals remain protected until confirmed safe completion/recovery rules permit cleanup;
- queued photos retain their immutable stored destination identity;
- unknown/ambiguous remote outcomes fail closed rather than blind-retrying;
- no live field/customer job is used as an experiment when a disposable fixture can prove the behavior;
- Drive writes target the exact approved provider identity, not a guessed name match;
- provider IDs/SAF URIs are device/platform-context identity and are not assumed portable;
- original FPP identity and Google Drive authorization remain separate;
- Android never contains Supabase service-role/secret credentials;
- Field Photo Prep Team remains a separate product/backend;
- sign-out, revocation, account changes, or UI cleanup do not delete protected local work or Drive business data unless that exact destructive behavior is separately approved.

## Consistency boundaries

Field Photo Prep should feel like one app, not a collection of phase-specific flows.

When changing user-facing behavior:
- preserve established Home / Work Orders / Photos navigation unless the approved design changes it;
- reuse established terminology for photos, work orders, companies, account states, and queue states;
- put status information where it helps the operator identify the affected job, not only in global counters;
- avoid duplicate controls or alternate pathways for the same action unless an explicit use case requires them;
- preserve field-first behavior and avoid developer/internal terminology on normal screens.

When changing runtime behavior:
- one module/state owner remains authoritative for each concern;
- UI requests and renders behavior; it does not become a second persistence/authorization/Drive implementation.

## External-state systems requiring parity

When touched, these require source/evidence reconciliation under `GOVERNANCE.md`:
- Supabase database schema/migrations;
- Supabase RPC grants/RLS/function definitions;
- Supabase Edge Functions;
- Auth redirect/configuration relevant to supported app callbacks;
- Android package/signing/release configuration;
- deployed runtime artifacts when a deployment phase exists.

Google Drive customer/work content is data, not source-controlled configuration. Its reality gates are governed by `INTEGRATION_CONTRACT.md` and the relevant regression rules.

## Current detailed rule owners

- Product/photo/queue behavior: `CONTRACT.md`
- Change classification/approval/rollback: `CHANGE_CONTROL_CONTRACT.md`
- Test selection and failure behavior: `TESTING_CONTRACT.md`
- Drive/DocumentsProvider behavior: `INTEGRATION_CONTRACT.md`
- Regression surface inventory: `REGRESSION_CHECKLIST.md`
- Phase staging/device-boundary philosophy: `docs/PHASE_STAGING_DOCTRINE.md`
- FPP identity model: `docs/IDENTITY_MODEL_V1.md`
- Current Phase 12 identity/release design while Phase 12 is active: `docs/PHASE_12_COMPLETE_DESIGN_2026-09-25.md`

Use `RULE_INDEX.md` to decide which of these to load for a task.
