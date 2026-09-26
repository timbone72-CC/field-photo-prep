# Phase 12F — Hosted Backend Verification

Date: 2026-09-25
Project: `vtyiktvqhbgabawotkrj` — Field Photo Prep
Status: source-control reconciliation confirmed; core authorization/idempotency/race gates PASS; delivery/email gates remain

## Source-control reconciliation

The live project already had these applied migrations before the 12F Git branch was created:
- `20260925195816_phase_12f_owner_member_admin`
- `20260925195901_phase_12f_rpc_grant_hardening`

The branch source-controls reconstructed versions of both applied migration files and the deployed `fpp-owner-invite` Edge Function.

Recheck on 2026-09-25:
- project status is **ACTIVE_HEALTHY**;
- live migration history still contains the exact two Phase 12F versions above;
- deployed `fpp-owner-invite` is ACTIVE version 1 with JWT verification enabled;
- deployed `index.ts` text remains an exact match to PR #74 source;
- current advisor output remains limited to the already-recorded audit-table/RPC/password warnings and informational performance findings.

The Phase 12F Android/backend implementation checkpoint is `a0f12f75d08d596a5bfc53169e879a31fbb392dd`; Android CI run `36190542857` passed. After governance/CI integration, branch snapshot `05a7c761bd56f41eb4923da87e50349349a53b54` also passed Android CI run `36203850206`.

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

Performance advisor reports four audit foreign-key indexes as informational opportunities and two invitation indexes as currently unused; no current correctness blocker.

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

## PASS — explicit pending-role change, cancellation and expiry

Rollback-only hosted checks:
- PENDING invitation MEMBER -> explicit Owner role update -> OWNER returned `UPDATED`;
- matching invited identity after cancellation -> `CANCELLED`, no Membership activation;
- matching invited identity after forced expiry -> `EXPIRED`, no Membership activation.

These checks used only the disposable Phase 12F fixture and rolled back.

## Clean pre-reality-gate baseline

Immediately before the physical invitation gate:
- persisted `public.fpp_invitations` rows: **0**;
- no matching `fpp-owner-invite` / delivery events were found in the inspected previous 24-hour Edge Function log window.

This confirms the real email/deep-link gate has not yet been executed and avoids mistaking rollback-only fixture evidence for a production delivery pass.

## Remaining backend gates

Still required:
- delivery failure visibility/retry behavior through the Edge Function;
- real invitation email/deep-link acceptance on a disposable user;
- final RLS/grant/catalog diff and advisors;
- cleanup of the disposable Race Fixture only after its evidence is no longer needed.

Do not claim Phase 12F complete from this document alone.
