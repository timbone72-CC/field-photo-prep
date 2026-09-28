# Phase 12G — First-Run / New-Device Flow Impact Record

Current-state note (2026-09-28): implementation and applicable approvals are complete. Historical planning, pending gates, and next-step text below are retained as evidence, not current instructions. `PHASE_12M_BUILD_STATE.md` owns phase-wide closeout and limits.

Date: 2026-09-26

Status: **APPROVED RECORD — IMPLEMENTATION MERGED; SEE PHASE_12M_BUILD_STATE.md**

## Goal

Make the existing Field Photo Prep identity and Drive setup understandable and safe on a new install or new device without inventing a second authentication, Organization, or Drive-binding system.

Normal returning/new-device flow:

`Open/install → sign in → validate exactly one ACTIVE Membership → establish the active Organization → evaluate the local 12H Drive binding → if absent/unusable, deliberately connect/confirm the approved Drive workspace through Android SAF → choose Client Company → normal field workflow`

Invited-user flow:

`Open/install → accept invitation/sign in → activate and validate Membership → establish active Organization → independently connect Drive on this device → choose Client Company → normal field workflow`

## Classification

**Level 2**

Reason:
- this slice is limited to navigation/orchestration over the already-authoritative Phase 12E authorization/session owner and Phase 12H Organization↔Drive binding owner;
- it does not add or alter persisted Drive-binding semantics;
- it does not add Supabase schema/RLS/RPC/Edge Function behavior;
- it does not change queue/photo destination identity, upload/retry semantics, or destructive Drive behavior.

If implementation requires any new persisted Organization↔Drive field, provider/account-selection semantic, SAF permission semantic, authorization state, or cross-Organization recovery rule, stop and reclassify before continuing. Those changes are Level 3 and are not authorized by this record.

## Authoritative line

- governed base / rollback: `7b4c2017e9fa013929247751039db08cdae9418f`;
- base state: Phase 12H merged and complete through PR #80;
- authoritative branch: `phase-12g/first-run-new-device-flow`;
- authoritative PR: opened from this branch after this preflight checkpoint;
- no competing 12G branch or PR existed at takeover;
- unrelated open draft PR #39 retains its own UNCERTAIN-reconciliation scope and is not part of 12G.

## Required rule packs

- `GOVERNANCE.md`;
- `PROJECT_PROFILE.md`;
- `RULE_INDEX.md`;
- `CHANGE_CONTROL_CONTRACT.md`;
- `TESTING_CONTRACT.md`;
- relevant identity/Drive portions of `CONTRACT.md`;
- `INTEGRATION_CONTRACT.md`;
- `rules/testing/DRIVE_PROVIDER.md` when runtime changes depend on the SAF/provider setup path;
- `docs/PHASE_STAGING_DOCTRINE.md`;
- `docs/IDENTITY_MODEL_V1.md`;
- `docs/PHASE_12_COMPLETE_DESIGN_2026-09-25.md`;
- `docs/PHASE_12_MASTER_PLAN_2026-09-25.md`.

## Existing owners that 12G must consume

### FPP account / invitation owner

`app/src/main/java/com/inandout/fieldphotoprep/AuthActivity.java`

Already owns:
- sign in;
- invitation callback/acceptance;
- password recovery;
- Membership validation;
- Recheck Account;
- safe session replacement/sign-out.

12G may route users through this owner. It must not create a second sign-in or Membership-validation path.

### Runtime authorization owner

`RuntimeAuthorizationManager`, `RuntimeAuthorizationPolicy`, and `AuthorizationActionGuard`

Already own the current User/Organization authorization decision and action gates.

12G renders/routes from that decision; UI must not persist its own authorization flag.

### Organization ↔ Drive binding owner

`OrganizationDriveBindingGuard` + `FolderPrefs.DriveBinding`

Phase 12H already owns:
- same-Organization binding reuse;
- no-workspace state;
- legacy-unbound quarantine;
- wrong-Organization rejection;
- missing-permission rejection;
- online-VALIDATED requirement for creating/rebinding a workspace;
- local Organization UUID + binding version beside the provider-bound Drive root.

12G consumes these states. It must not add another Drive-binding record or infer a Google account from FPP Auth identity.

### Home / Drive setup orchestration owner

`MainActivity`

Already owns:
- Home rendering;
- Account entry;
- Connect/Confirm/Change Workspace actions;
- Android SAF picker launch;
- verified provider-root persistence through `bindSelectedDriveRoot(...)`;
- company discovery/selection;
- saved Drive restoration/reconciliation on resume.

12G should extend this existing route rather than add a parallel setup stack unless implementation evidence proves a separate screen is necessary.

## Read surfaces

12G may read:
- current authorization decision and stored FPP identity;
- existing 12H Drive-binding state;
- persisted SAF permission availability;
- current company/address/work-order navigation state;
- invitation/auth completion result.

## Write surfaces

The intended 12G code itself writes only ordinary UI/navigation state.

When the user deliberately connects a workspace, 12G invokes the existing 12H/SAF path that already owns provider verification and `FolderPrefs.bindSelectedDriveRoot(...)`. No new storage format is authorized.

No Supabase schema or hosted function change is authorized.

## Required behavior

1. No readable/validated FPP identity must route to the existing sign-in/invitation path before ordinary field setup.
2. Sign-in success alone never selects, confirms, or authorizes a Drive workspace.
3. A same-Organization usable local 12H binding may be reused.
4. No local binding must present a deliberate Connect Drive path.
5. Legacy, wrong-Organization, invalid, or permission-lost binding states remain governed by the 12H guard and fail closed.
6. The user must deliberately select the provider root through Android SAF when a binding is needed.
7. After a usable binding exists, the user deliberately selects the Client Company when one is not already valid.
8. Auth email and Google Drive account email are never compared as an authorization rule.
9. Provider IDs and SAF URIs are never restored from another phone merely because FPP sign-in succeeded.
10. Invitation acceptance grants FPP Membership only; it never grants Drive sharing or Drive authorization.
11. Multiple usable ACTIVE Memberships fail closed under the existing v1 identity rule; 12G does not invent an Organization switcher.
12. Public Organization self-creation remains out of scope.
13. Backup exclusions remain authoritative.
14. Existing protected originals, queue evidence, and immutable photo destinations remain unchanged.

## Primary risks

- accidentally treating successful FPP sign-in as Drive authorization;
- bypassing the 12H guard during first-run navigation;
- duplicating authorization or Drive state in UI;
- stale Home/company state surviving a changed account/binding state;
- making a new-device claim that assumes provider IDs/SAF grants are portable;
- turning the onboarding slice into a new Organization-selection or backend-registration feature.

## Focused verification plan

Automated coverage must prove, at minimum:
- no-session launch routes toward existing FPP authentication rather than normal work;
- VALIDATED identity + no workspace exposes deliberate Drive connection and does not auto-bind;
- VALIDATED identity + same-Organization usable binding reuses that binding;
- wrong-Organization/legacy/invalid binding states remain fail-closed through the 12H owner;
- invitation completion does not create/select a Drive binding;
- client-company selection occurs only after a usable workspace;
- multiple usable Memberships do not proceed into ordinary work;
- 12G navigation does not alter queued destination identity or protected local work;
- existing account recheck and workspace reconnect routes remain usable.

Final runtime head:
- focused tests PASS;
- complete Android CI PASS once on the exact final runtime head;
- affected UI/navigation smoke only.

## Physical / provider boundary

Do **not** wipe or repeatedly rebuild the operator's working phone merely to prove 12G.

The final clean-install/new-user physical claim is intentionally deferred to Phase 12L.

12G may reuse valid Phase 12H real-provider evidence for unchanged binding semantics. If 12G changes only navigation into the existing picker/binding path, any physical check should be the smallest non-destructive smoke needed to prove the route, not a repeat of the full 12H provider gate.

## Rollback

Revert the 12G runtime/UI commits to governed base `7b4c2017e9fa013929247751039db08cdae9418f`.

Rollback must not clear:
- protected photos;
- queue/reconciliation state;
- persisted SAF permission;
- the valid 12H Organization↔Drive binding;
- Drive content;
- Supabase Membership/Invitation state.

## Stop conditions

Stop implementation and reconcile/reclassify if:
- a new persisted Drive-binding or account-selection field appears necessary;
- 12G would change `FolderPrefs` binding semantics;
- 12G would change `OrganizationDriveBindingGuard` authorization rules rather than consume them;
- Supabase schema/RLS/RPC/Edge Functions would need to change;
- multiple-Organization selection becomes a product requirement;
- protected/queued work would need to be reassigned or rewritten;
- live provider/account state contradicts the approved design;
- scope expands into 12I diagnostics, 12J recovery UX, 12K release/signing, or 12L clean-install claims.
