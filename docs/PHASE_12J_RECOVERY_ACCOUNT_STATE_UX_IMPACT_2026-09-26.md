# Phase 12J — Recovery & Account-State UX Impact Record

Current-state note (2026-09-28): implementation and applicable approvals are complete. Historical planning, pending gates, and next-step text below are retained as evidence, not current instructions. `PHASE_12M_BUILD_STATE.md` owns phase-wide closeout and limits.

Date: 2026-09-26

Status: **APPROVED RECORD — IMPLEMENTATION MERGED; SEE PHASE_12M_BUILD_STATE.md**

## Goal

Make degraded/blocked account and Drive states understandable without creating a second authorization, Drive, queue, reconciliation, or sign-out system.

The approved user-facing contract is one deterministic explanation containing:
1. what is safe now;
2. what is blocked now;
3. what remains protected;
4. one practical next action when one exists.

## Classification

**Level 2**

Reason:
- presentation/navigation only around existing settled behavior;
- no persisted schema change;
- no new authorization state;
- no new Drive/provider write path;
- no queue/reconciliation mutation;
- no Supabase schema/RLS/RPC/Edge Function change;
- no release/signing change.

If implementation requires a new recovery mutation, new account/Organization switch behavior, new Drive binding mutation outside the existing owner, forced sign-out, photo deletion, blind UNCERTAIN retry, or destination reassignment, stop and split/reclassify before continuing.

## Authoritative line

- governed base / rollback: `1476afec4305d366931c39f66d6fe13e63445db9`;
- base is the governed Phase 12I merge commit from PR #82;
- authoritative branch: `phase-12j/recovery-account-state-ux`;
- authoritative PR: created after this documentation checkpoint;
- no competing 12J branch or PR existed at takeover;
- unrelated draft PR #39 remains outside Phase 12J.

## Required owners to consume

### Authorization

`RuntimeAuthorizationManager` / `AuthorizationDecision` remain authoritative for:
- VALIDATED;
- GRACE;
- RECHECK_REQUIRED;
- SIGN_IN_REQUIRED;
- NO_MEMBERSHIP;
- REVOKED;
- DRIVE_DISCONNECTED.

12J must not infer a second permission state.

### Drive

`OrganizationDriveBindingGuard` remains authoritative for:
- NO_WORKSPACE;
- AUTHORIZATION_REQUIRED;
- LEGACY_UNBOUND;
- INVALID_BINDING;
- WRONG_ORGANIZATION;
- PERMISSION_MISSING;
- USABLE.

Recovery UX may explain or delegate to the already-approved Connect Drive path. It must not choose a workspace, Google account, or provider by itself.

### Protected work / queue

`AppStatusSnapshot.QueueCounts`, `PendingPhotoStore`, and existing UNCERTAIN reconciliation owners remain authoritative.

12J may explain that protected work remains local and identify counts/state categories. It may not upload, retry, reconcile, discard, delete, or redirect photos.

### Sign-out

`AuthActivity.signOutSafely()` + `ProtectedWorkGuard` remain the only sign-out/protected-work guard.

12J may explain why sign-out can be blocked. It must not add a force sign-out or bypass.

## Approved recovery situations

At minimum:
- temporary account outage inside grace;
- grace expired / revalidation required;
- Membership revoked;
- no ACTIVE Membership;
- unreadable/expired session requiring sign-in;
- Drive workspace disconnected/unavailable;
- wrong Organization / invalid or expired Drive binding;
- unresolved protected work after authorization is lost;
- UNCERTAIN upload;
- sign-out blocked by protected/unresolved work;
- normal validated/connected state.

## Recovery priority

Guidance must prefer the highest-risk blocking condition:

1. SIGN_IN_REQUIRED / REVOKED / NO_MEMBERSHIP / RECHECK_REQUIRED;
2. UNCERTAIN or unreadable protected work;
3. Drive binding unusable;
4. offline GRACE guidance;
5. ordinary FAILED/WAITING protected work context;
6. healthy validated/connected state.

This priority is presentation only and must not alter authorization or queue behavior.

## Single-action rule

Recovery guidance exposes at most one primary next action:
- SIGN_IN → existing AuthActivity;
- RECHECK_ACCOUNT → existing RuntimeAuthorizationManager revalidation;
- CONTACT_OWNER → informational only, with Recheck Account remaining available after access is restored;
- CONNECT_DRIVE → existing MainActivity / 12H SAF path;
- OPEN_PHOTOS → existing Photos surface for FAILED/UNCERTAIN/reconciliation work;
- NONE → no recovery action required.

No new operation is authorized.

## Protected-data language

When unresolved/protected local work exists, recovery copy must explicitly state that it remains protected/local and is not deleted or reassigned by account/Drive recovery.

When UNCERTAIN exists, guidance must explicitly prohibit blind retry and direct the operator to the existing reconciliation path.

When authorization is lost, protected work must remain visible as protected; UX must not suggest deleting photos to fix account access.

## Owning files expected

Likely owning path:
- `AppStatusSnapshot.java`;
- a small pure recovery-guidance policy/value object;
- `AppStatusActivity.java`;
- `screen_app_status.xml`;
- focused unit/instrumented tests;
- Phase 12 build-state/roadmap/master docs.

`AuthActivity.java`, `OrganizationDriveBindingGuard.java`, queue coordinators, and Drive upload owners should remain unchanged unless a concrete presentation-only gap cannot be solved from App Status. Any mutation change triggers reassessment.

## Read/write surfaces

### Reads
- existing 12I snapshot fields;
- authorization state;
- Drive-binding state;
- queue/protected counts.

### Writes
No recovery-state persistence.

Permitted side effects:
- navigate to existing Sign In;
- trigger existing Recheck Account;
- delegate to existing Connect Drive;
- navigate to existing Photos/reconciliation surface.

## Risks

- accidentally creating a second recovery state machine that disagrees with 12E/12H;
- presenting an unsafe action for REVOKED/NO_MEMBERSHIP;
- suggesting blind UNCERTAIN retry;
- hiding protected work when authorization is lost;
- allowing Drive recovery during GRACE when 12H requires online VALIDATED identity;
- adding a force sign-out shortcut;
- exposing raw backend/provider error wording.

## Verification plan

Focused pure tests:
- every authorization state has deterministic safe/blocked/protected/next-action guidance;
- every unusable Drive state has deterministic guidance;
- UNCERTAIN outranks ordinary FAILED/WAITING guidance;
- SIGN_IN_REQUIRED / REVOKED / NO_MEMBERSHIP outrank Drive guidance;
- GRACE explains existing work may continue inside grace but admin/Drive rebinding are unavailable;
- protected counts appear in protection language without exposing customer identifiers;
- no guidance recommends deletion, blind retry, Organization switching, or automatic workspace selection.

Instrumented coverage:
- App Status renders the four recovery sections;
- Sign In delegates to AuthActivity;
- Recheck Account remains existing manager behavior;
- Connect Drive delegates to existing Main/12H path;
- Photos/reconciliation navigation uses existing Photos surface;
- Home/Work Orders/Photos regressions remain green.

Final gate:
- full Android CI once on exact final runtime head;
- emulator/UI smoke PASS;
- no real Drive reality gate if Drive/provider semantics remain unchanged.

## Google Drive integration impact

**No new Google Drive integration impact is authorized.**

12J may explain existing 12H binding states and delegate to the existing Connect Drive owner only.

## External systems

- Supabase: no schema/config/backend mutation;
- Google Drive: no content/permission/provider mutation;
- Android: presentation/navigation only;
- GitHub: branch/PR/source changes.

## Rollback

Revert Phase 12J runtime/UI commits to governed base `1476afec4305d366931c39f66d6fe13e63445db9`.

Rollback must not clear or rewrite:
- FPP auth/session state;
- Organization↔Drive binding;
- SAF permissions;
- selected provider identities;
- queue/reconciliation state;
- protected originals/prepared copies;
- Drive content.

## Stop conditions

Stop and reconcile/reclassify if:
- a force sign-out or identity-switch recovery is proposed;
- a recovery action mutates queue/photo state;
- a recovery action performs Drive writes instead of delegating existing binding flow;
- UNCERTAIN handling requires a new retry/reconciliation semantic;
- backend or signing/release changes appear necessary;
- scope expands into 12K.
