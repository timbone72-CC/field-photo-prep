# Phase 12F — Owner/Member Administration Build State

Last updated: 2026-09-25

This is the durable handoff point for Phase 12F. Use the repository, hosted Supabase state, and exact CI results rather than restarting the audit.

## Governed base

- Phase 12F base / rollback: `31bfaaffa012cadf2da0c9c31c6da64967d9d24f` (Phase 12E merged main)
- Branch: `phase-12f/owner-member-administration`
- Draft PR: #73 `Phase 12F: owner/member administration`
- Level 3: do not merge without explicit operator approval after final evidence.

## Implemented and committed

- Level 3 impact record:
  - `docs/PHASE_12F_OWNER_MEMBER_ADMIN_IMPACT_2026-09-25.md`
- Transactional server-side administration contract:
  - exact ACTIVE OWNER recheck inside every Owner mutation
  - exact Organization UUID scoping
  - Organization-row lock for last-Owner serialization
  - minimal server-side audit rows
  - duplicate-safe invitation prepare/resend
  - explicit pending-invitation role change
  - invitation cancel
  - delivery status/attempt bookkeeping
  - membership role change
  - revoke/reactivate
  - exact authenticated invitation acceptance
  - duplicate acceptance returns same Membership
- Anonymous RPC execution explicitly revoked.
- Android remains publishable-key + user-JWT only.
- Server-only invitation-delivery Edge Function:
  - `fpp-owner-invite`
  - deployed hosted version 1
  - JWT verification enabled
  - server secret is read only from Edge Function environment
  - exact internal/production FPP callback bases allowlisted
- Android invitation callback now preserves exact `fpp_invitation_id`.
- Password/invite setup must call server-side `fpp_accept_invitation` before Membership validation/storage.
- Narrow Android Owner administration client for roster/invitations/mutations.
- Non-exported Owner administration activity.
- Account screen exposes `Manage Members` only when central 12E decision is VALIDATED OWNER.
- Administration revalidates online Owner authority before every refresh/mutation; GRACE cannot administer.
- Local Owner self-revocation checks durable protected-photo truth before server mutation.
- Large-text-safe stacked administration controls.

## Hosted Supabase state

Dedicated Field Photo Prep project: `vtyiktvqhbgabawotkrj`.

Applied migrations:
- `20260925012939 phase_12c_identity_foundation`
- `20260925013000 phase_12c_invitation_fk_indexes`
- `20260925195816 phase_12f_owner_member_admin`
- `20260925195901 phase_12f_rpc_grant_hardening`

Edge Functions:
- `fpp-owner-invite` — ACTIVE, version 1, verify_jwt=true

No Google Drive schema/data/permissions were changed.

## Hosted disposable transaction evidence

All fixtures below were created inside transactions and rolled back.

PASS:
- normalized first invitation -> CREATED
- same Organization + same normalized email + same role -> RESEND_READY with the same invitation ID
- same pending invitation + different role -> ROLE_CHANGE_REQUIRED
- final ACTIVE OWNER demotion -> LAST_OWNER_BLOCKED
- final ACTIVE OWNER revocation -> LAST_OWNER_BLOCKED
- member revoke repeated safely -> REVOKED
- member reactivate repeated safely -> ACTIVE
- MEMBER attempting Owner administration -> OWNER_REQUIRED
- Owner attempting another Organization -> OWNER_REQUIRED
- invitation acceptance by wrong email -> INVITATION_UNAVAILABLE
- cancelled invitation acceptance -> CANCELLED
- valid exact-email acceptance -> ACCEPTED
- duplicate acceptance -> ALREADY_ACCEPTED with the same Membership ID

One evidence query displayed membership count 0 because that final direct SELECT still ran under an unrelated authenticated fixture's RLS view. Do not interpret that as a failed acceptance; the ACCEPTED and ALREADY_ACCEPTED results returned the same Membership ID. If a literal count is needed for final evidence, rerun after RESET ROLE inside the rolled-back fixture.

## Security advisor state

After grant hardening:
- anonymous SECURITY DEFINER execution warnings: cleared
- `fpp_admin_audit` RLS-with-no-policy INFO remains intentional; client roles have no table grants
- authenticated SECURITY DEFINER warnings remain expected because the narrowly scoped RPCs are the server operations; each re-checks exact authority internally
- leaked-password protection warning predates 12F and is a project-plan/Auth setting concern, not introduced by 12F

## Email-delivery constraint

Supabase built-in hosted SMTP is suitable only for controlled tests and, without custom SMTP, sends only to pre-authorized Supabase organization team addresses. Production-suitable external email delivery remains a later release-readiness concern (Phase 12K). Do not silently expand 12F into SMTP/domain setup.

The Edge Function intentionally retains a pending invitation and records FAILED/retryable delivery if Auth email delivery fails.

## Current exact runtime head

Runtime head at this checkpoint:
`3151cced5a4a32731e8055b4aac0751b7dae7ada`

Android CI run 909 (`36183753317`) was in progress when this checkpoint was written. Unit tests, debug build, and signer verification had already passed; emulator/instrumented tests were still running.

## Remaining Phase 12F work

1. Confirm exact current Android CI head passes emulator/instrumented suite.
2. Install current internal APK on Samsung and verify:
   - Owner sees Manage Members
   - roster shows exact same-Organization Owner row
   - screen remains usable at current large text size
   - ordinary photo/Drive field workflow is unchanged
3. Controlled invitation-delivery gate using a safe pre-authorized test email:
   - SENT or a correctly visible FAILED/retryable pending invitation
   - resend reuses invitation identity
   - callback opens exact FPP build
   - acceptance creates/activates one exact Membership
4. Verify a MEMBER account has no administration entry/mutation ability.
5. Add/finish focused Android tests for the new admin client/UI and invitation acceptance path.
6. Correct literal hosted membership-count evidence under RESET ROLE.
7. Add concurrency evidence for last-Owner serialization.
8. Rerun security/performance advisors and inspect Edge Function logs after live gate.
9. Run full Android CI on exact final runtime head.
10. Update Phase 12F completion evidence and PR #73 metadata.
11. Stop for explicit Level 3 merge approval.

## Working rule

Keep future changes bounded and committed. Do not restart Phase 12F design/audit unless repository or hosted evidence contradicts this checkpoint.
