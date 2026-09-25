# Phase 12F — Hosted Backend Verification

Date: 2026-09-25
Project: `vtyiktvqhbgabawotkrj` — Field Photo Prep
Status: backend verification in progress; core authorization/idempotency/race gates PASS

## Source-control reconciliation

The live project already had these applied migrations before the 12F Git branch was created:
- `20260925195816_phase_12f_owner_member_admin`
- `20260925195901_phase_12f_rpc_grant_hardening`

The branch now source-controls reconstructed versions of both applied migration files and the deployed `fpp-owner-invite` Edge Function.

## Live ACL / RLS observations

- Organizations, Memberships and Invitations remain authenticated SELECT-only tables.
- No authenticated INSERT/UPDATE/DELETE table grants were added.
- `fpp_admin_audit` has RLS enabled and no client policy or table grant; it is server-write-only.
- Private lock/owner/audit helpers are not executable by authenticated or anon.
- Public 12F RPCs are executable only by authenticated/service_role and internally re-check exact authority.
- `fpp-owner-invite` has JWT verification enabled.

Supabase security advisor reports:
- INFO: audit table RLS enabled with no policy — intentional because clients must not read it.
- WARN: authenticated can execute the ten SECURITY DEFINER RPCs — intentional only because each is the narrow public server operation and internally validates caller/Organization.
- WARN: leaked-password protection disabled — existing Free-plan limitation recorded in Phase 12C.

Performance advisor reports audit foreign-key indexes as informational opportunities; no current correctness blocker.

## Disposable fixture

Organization:
- `Phase 12F Race Fixture`

Two disposable Auth/Membership identities exist. The fixture was restored after concurrency testing to:
- Owner A: REVOKED
- Owner B: ACTIVE

No production Organization Membership was mutated by these tests.

## PASS — final Owner protection

Rollback-only test as the sole ACTIVE disposable Owner:
- revoke self -> `LAST_OWNER_BLOCKED`
- demote self OWNER -> MEMBER -> `LAST_OWNER_BLOCKED`

## PASS — invitation normalization / duplicate / explicit role update

Rollback-only test:
- first invite of normalized disposable email as MEMBER -> `CREATED`
- resend same address with whitespace/case variation and same role -> `RESEND_READY`
- resend returned the same invitation UUID
- request same pending invitation as OWNER -> `ROLE_CHANGE_REQUIRED`

No duplicate Membership/Invitation was persisted because the test transaction rolled back.

## PASS — cross-Organization and inactive-Owner denial

Rollback-only tests:
- disposable active Owner attempted to list the real Organization -> SQLSTATE `42501`; denied
- disposable revoked Owner attempted admin list on its own Organization -> SQLSTATE `42501`; denied

## PASS — invitation acceptance identity and idempotency

Rollback-only test using existing disposable confirmed Auth identity:
- wrong authenticated email -> `INVITATION_UNAVAILABLE`
- exact invited confirmed Auth user -> `ACCEPTED`
- resulting Membership role/status -> MEMBER / ACTIVE
- second acceptance -> `ALREADY_ACCEPTED`
- second acceptance returned the exact same Membership UUID

## PASS — concurrent last-Owner race

The disposable fixture was put temporarily into a two-ACTIVE-Owner state.

Two separate hosted SQL sessions concurrently attempted:
- Owner A revoke Owner B
- Owner B revoke Owner A

Observed:
- one revocation committed;
- the competing call failed `OWNER_REQUIRED` after the organization-row lock serialized the operations;
- final observed state contained exactly one ACTIVE OWNER, never zero.

The fixture was then restored to its prior A=REVOKED / B=ACTIVE state.

## Remaining backend gates

Still required:
- explicit pending-role-update and cancel/expired acceptance hosted checks;
- delivery failure visibility/retry behavior through the Edge Function;
- real invitation email/deep-link acceptance on a disposable user;
- final RLS/grant/catalog diff and advisors;
- cleanup of the disposable Race Fixture only after its evidence is no longer needed.

Do not claim Phase 12F complete from this document alone.
