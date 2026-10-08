# Pending Invitation Recovery — Impact / Build State

Date: 2026-10-08
Status: IN PROGRESS — IMPLEMENTATION NOT MERGED, NOT DEPLOYED
Scope key: fpp-verified-invitation-recovery
Change level: **Level 3** — membership/authentication authorization, new constrained Supabase RPC.

## Grounded problem / approval
An Owner invited an outside person as MEMBER using the existing production Field Photo Prep app. The recipient's first invite link reached the wrong package and consumed the Auth invite verification; a later invitation link was invalid. Password recovery succeeded but left the original Organization invitation PENDING and no ACTIVE Membership. Password sign-in then returned “This account does not have an active Field Photo Prep membership.” Operator approved implementing a recovery path on 2026-10-08. This approval is implementation authorization only, **not Level-3 merge/deployment approval**.

## Authority / external state
- Repository: `timbone72-CC/field-photo-prep`
- Governed base and rollback: `c9e6965dc934d697b18e7b62e5f4ebbc0dfb9656` (`main` at takeover)
- Authoritative branch: `fix/pending-invitation-after-password-recovery`; PR pending creation
- Existing open PR #95 owns address/work-order cleanup and PR #96 owns workflow pinning, neither owns this authentication recovery path.
- Dedicated Supabase project: `vtyiktvqhbgabawotkrj` (original Field Photo Prep; not Field Work Hub)
- A real recipient has a PENDING invitation and an email-confirmed Auth user, but no activated Organization membership; **do not mutate this live account as a development fixture**.
- No live database, identity, Auth redirect, Drive, permission, or app installation change is authorized by a source commit.

## Proposed smallest safe behavior
When a password is valid but ordinary Membership validation finds zero ACTIVE memberships, the app may ask its existing Supabase backend for *only* the unexpired PENDING invitation for that exact authenticated, email-confirmed user. A user with zero invitations, ambiguous multiple invitations, an existing (including revoked) Membership, or an unconfirmed email remains blocked. The app displays the exact Organization and intended role, and requires explicit **Accept Invitation**. Acceptance calls the *existing* `fpp_accept_invitation` RPC with the returned invitation UUID. It then performs the *existing* complete Membership validation and only afterward stores a validated session. The unvalidated sign-in tokens remain in memory for the brief recovery screen and never grant field/Drive rights.

## Affected owner surfaces
- `supabase/migrations/*pending_invitation_recovery*.sql`: narrow verified-self lookup RPC, no broad table grants.
- `app/src/main/java/com/inandout/fieldphotoprep/SupabaseAuthClient.java`: typed no-membership outcome / self lookup.
- `app/src/main/java/com/inandout/fieldphotoprep/AuthActivity.java`: explicit pending-invitation confirmation UI inside existing Account screen.
- Focused auth parsing/state tests. No extra sign-in or invitation implementation, no new persistence owner.

## Risk / verification / rollback
- Required data: Auth User UUID and verified email, one PENDING non-expired invitation, exact Organization and role, existing acceptance RPC. No new persisted client data.
- Optional data: user-visible Organization name for informed consent. No customer names/photos/Drive identifiers in Auth.
- Security gates: no anonymous/MEMBER/Owner cross-account lookup; revoke/unconfirmed/expired/multiple pending stay blocked; invalid/expired tokens denied; no old persisted session overwritten before full acceptance; no bypass of offline/revocation/Drive gates; no automatic membership mutation on lookup; idempotent acceptance only via existing RPC.
- Focused tests for response state and sign-in/missing-membership UI. CI complete on exact final runtime head; hosted Auth/RLS/grant/reality test with disposable account/invitation before live use. Supabase integration requires verifying backend migration parity first and deployment only after operator approval.
- No code on `main`, no live schema/config changes, no new APK distribution from this branch until gates pass.
- Rollback: revert this branch/new client changes and, if deployed later, revoke the added RPC EXECUTE grant or drop only that RPC through governed forward migration. Preserve all real invitations, Auth users, protected photos, queue, SAF grants, and Drive data.
- Explicit Level-3 approval required before merge and deployment. No claim of a successful real recipient activation until physically verified.
