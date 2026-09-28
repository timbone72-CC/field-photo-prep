# Phase 12L Production Invite Redirect Defect — Build State

Current-state note (2026-09-28): implementation and applicable approvals are complete. Historical planning, pending gates, and next-step text below are retained as evidence, not current instructions. `PHASE_12M_BUILD_STATE.md` owns phase-wide closeout and limits.

Date: 2026-09-28

Status: **APPROVED RECORD — IMPLEMENTATION MERGED; SEE PHASE_12M_BUILD_STATE.md**

## Authoritative line

- branch: `fix/phase-12l-production-invite-redirect`;
- base/rollback: `c7970238e00139415a417f669d86915c4260154e`;
- Phase 12L PR #85: paused at Gate D while this defect line is reconciled;
- implementation ownership: hosted Supabase Auth Redirect URL allowlist + source-of-truth documentation only.

## Implemented configuration correction

The operator added the required production query-capable redirect entry in Supabase Auth URL Configuration:

`com.inandout.fieldphotoprep://auth-callback*`

The existing exact production callback entry was retained:

`com.inandout.fieldphotoprep://auth-callback`

The Site URL and internal callback entries were not changed.

## Live verification evidence

The focused production invitation retest now passes end to end.

- `fpp-owner-invite`: ACTIVE;
- existing disposable FPP invitation was reused; no duplicate FPP invitation was created;
- an earlier resend attempt returned Auth `422 email_exists` because the first broken invitation link had already created/confirmed the disposable Auth user before falling back to localhost;
- only that disposable `+12lmember` Auth user/session was reset, while the existing FPP invitation remained `PENDING`;
- resend of the same invitation then succeeded;
- live Auth invite request used the production callback with `?fpp_invitation_id=...`;
- the newest invitation email opened the production Field Photo Prep app rather than localhost;
- invitation verification reached the app's **Set Password** screen;
- password completion succeeded;
- final FPP invitation state: `ACCEPTED`;
- final delivery state: `SENT`;
- final Membership role: `MEMBER`;
- final Membership status: `ACTIVE`;
- the invited user landed on Home with Google Drive **Not connected**, proving invitation acceptance did not silently grant or inherit a Drive workspace.

A stale invitation link in the existing Gmail conversation initially produced `Email link is invalid or has expired`; refreshing the email conversation exposed the newest resend, which completed successfully. This was test-email/thread state, not a failure of the corrected production callback.

## Defect result

**PASS.**

The missing query-capable production Redirect URL allowlist entry was the bounded hosted configuration defect. No Android runtime code, Drive code, database schema, package identity, or signing change was required.

## Approval and fixture disposition

- explicit Level 3 operator merge approval: **GRANTED 2026-09-28**;
- the disposable `+12lmember` MEMBER/Auth fixture is deliberately retained for the immediately following Phase 12L Gate E account-transition/boundary-safety evidence;
- it must be cleaned after that evidence unless the Phase 12L record explicitly documents another reason to retain it.

## Exact next checkpoint

Do not perform additional runtime work on this branch.

Merge PR #86 after required GitHub checks pass. Then resume Phase 12L PR #85, reconcile Gate D as PASS, and begin Gate E.
