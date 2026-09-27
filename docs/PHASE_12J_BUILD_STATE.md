# Phase 12J Build State

Date: 2026-09-26

Status: **PRE-IMPLEMENTATION — AUTHORITATIVE LINE ESTABLISHED**

## Governed base

- main/rollback commit: `1476afec4305d366931c39f66d6fe13e63445db9`;
- Phase 12I is merged through PR #82.

## Authoritative implementation line

- branch: `phase-12j/recovery-account-state-ux`;
- PR: #83 — `Phase 12J: recovery and account-state UX` (Draft);
- competing 12J branch/PR at takeover: none;
- unrelated draft PR #39 does not overlap this scope.

## Classification

**Level 2**

12J is presentation/navigation around existing settled auth, Drive, queue, reconciliation, and sign-out behavior.

## Completed preflight

- exact protected `main` verified at the Phase 12I merge commit;
- open PRs checked;
- branch names checked for 12J overlap;
- Phase 12 master plan and complete design reread;
- change-control, testing, product, identity, and regression boundaries loaded;
- existing owners inventoried:
  - `AppStatusActivity`;
  - `AppStatusSnapshot`;
  - `RuntimeAuthorizationManager`;
  - `AuthorizationDecision`;
  - `AuthActivity`;
  - `OrganizationDriveBindingGuard`;
  - `ProtectedWorkGuard`;
  - existing UNCERTAIN reconciliation path.

## Protected behavior

Must remain unchanged:
- 12E authorization decisions;
- 12H Drive binding;
- 12G first-run/invitation behavior;
- 12I diagnostics/support-summary privacy boundary;
- sign-out protected-work guard;
- UNCERTAIN no-blind-retry contract;
- immutable queued destinations;
- Home / Work Orders / Photos workflow.

## Implemented checkpoints

### Pure recovery guidance

`RecoveryGuidancePolicy` maps the existing 12I snapshot into:
- safe-now text;
- blocked-now text;
- protected-data text;
- one primary next-action classification.

Focused unit coverage passed before UI integration.

### Exact sign-out protection

The existing read-only diagnostics collector now reports the exact protected-item count that matches the existing sign-out guard semantics:
- non-empty CAPTURING original;
- WAITING / UPLOADING / FAILED / UNCERTAIN record;
- UPLOADED record with local cleanup pending;
- unreadable metadata.

No queue/photo record is changed to compute this count.

### App Status recovery surface

App Status now renders:
- **Safe now**;
- **Blocked now**;
- **Protected**;
- **Next action**.

Its action button delegates only to existing owners:
- Sign In → `AuthActivity`;
- Recheck Account → `RuntimeAuthorizationManager`;
- Connect Drive → existing `MainActivity` / 12H SAF path;
- Open Photos → existing `MainActivity` Photos path, whose Reconcile All Uncertain workflow scans the durable global UNCERTAIN backlog and checks each stored work-order destination without blind retry.

Revoked/no-membership guidance tells the operator to contact an Organization Owner first; the follow-up button is explicitly **Recheck After Access Is Restored**.

## Verification status

- preflight docs: PASS;
- pure recovery-policy unit gate: PASS;
- exact sign-out protection model unit gate: PASS;
- final App Status UI/runtime CI: pending on the final runtime head;
- physical/provider gate: not required unless Drive/provider semantics change;
- Level 3 merge approval: N/A.
