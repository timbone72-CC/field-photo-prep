# Pending Invitation Recovery — Impact / Build State

Date: 2026-10-08
Status: SOURCE IMPLEMENTED / AUTOMATED CI PASS / LIVE BACKEND UNCHANGED — REALITY GATES PENDING
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

## 2026-10-08 verification checkpoint

Authoritative draft PR: [#97](https://github.com/timbone72-CC/field-photo-prep/pull/97).
Runtime source head: `5d0d694de6b4a556909897e202b8996b27e63067`.
Implementation-only commit includes a narrow confirmed-self invitation-lookup RPC migration, Android typed lookup/sign-in confirmation path, and `PendingInvitationRecoveryInstrumentedTest`.

### Automated and repository evidence
- Android CI [37799742270](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37799742270): **SUCCESS** for source head above. Unit tests, debug APK build, production identity/fail-closed signer checks, test-signer verification, complete `connectedDebugAndroidTest` (including new instrumentation test), theme/rendering verification, and launch smoke succeeded. The workflow's deliberately failed unsigned production build is an **expected successful fail-closed test**, not an overall CI failure.
- Artifact `field-photo-prep-internal-apk`, ID `11560810573`, from this CI run. **Not authorized for installation on the recipient's phone**: this branch inherits `main` internal versionCode `42` / `0.28.5-internal`, below the separately installed `0.28.6-internal` / versionCode `43`. Do not uninstall/reset/downgrade to bypass this; plan a monotonically higher approved candidate after reconciling open PR #95's version 44.
- Governance [37799888320](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37799888320): **SUCCESS**, after formatting PR's Level 3 merge approval as `PENDING`. Earlier formatting-only governance failure [37799743082](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37799743082) is superseded by this passing rerun without altering runtime code.

### Read-only production backend evidence
Supabase original-FPP project `vtyiktvqhbgabawotkrj` inspected, **not changed**.
- Current live migrations remain only `20260925012939`, `20260925013000`, `20260925195816`, `20260925195901`. The new `20261008161000` migration has **not** been applied.
- Existing `public.fpp_accept_invitation`: SECURITY DEFINER, EXECUTE permitted for `authenticated`, not `anon`.
- `public.fpp_find_my_pending_invitation` is **absent** on the live backend, as expected.
- `fpp_invitations`, `fpp_memberships`, `fpp_organizations`: RLS enabled.
- SQL was source-reviewed for exact `auth.uid()`/confirmed-email restriction, no-existing-membership fail-closed behavior, PENDING/non-expired/ACTIVE organization filtering, and ambiguous result refusal. This is a **source review**, not a passed hosted SQL isolation or deployment test.

### Remaining gates and ordering
1. In a safe disposable test environment, validate the new SQL function with Auth contexts: anonymous denial, other-email denial, unconfirmed-email denial, no invitation, expired/cancelled invitation, multiple pending invitations, and existing/revoked membership. Prove no membership mutation from the lookup alone. Check function grants and RLS after migration there. **Not yet done.**
2. Reconcile PR #95's versionCode 44 before preparing an installable Internal version newer than the recipient's versionCode 43. Preserve production signer identity if a production build is selected. New installation/update is a separate approved release path.
3. Before any live deployment, operator reviews the Level-3 reality-gate plan and authorizes the staged live backend change; then apply only the reviewed migration, inspect grants and run one disposable real-server/device invitation-acceptance test. The existing invitation/recipient account remains unchanged during fixture testing.
4. Physical check on recipient's phone: correct Auth account → pending invite displayed → explicit Accept → validated MEMBER/ACTIVE, with Drive unbound until separately selected. Stop on wrong organization, any unexpected provider binding, duplicated membership, or permission mismatch.
5. Obtain distinct explicit Level-3 merge/deployment approval after evidence. PR remains DRAFT, no merge. Auth passwords/tokens, photos, queue, company Drive, SAF bindings, and real invitation are not mutated by this checkpoint.

No claim of backend compatibility, active new Membership, physical update success, or complete production readiness is made.
