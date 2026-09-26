# Phase 12F — Membership and Invitation Lifecycle Impact

Date: 2026-09-25
Status: ACTIVE — Level 3
Branch: `phase-12f/membership-invitation-lifecycle`
Rollback base: `31bfaaffa012cadf2da0c9c31c6da64967d9d24f`

## Exact problem

Phase 12E established runtime authorization, but FPP still needs the approved Owner administration lifecycle: invite MEMBER/OWNER, resend/cancel pending invitations, accept an invitation safely, list same-Organization members/invitations, change role, revoke, and reactivate Memberships without exposing broad table writes.

During Phase 12F preflight, the live dedicated FPP Supabase project was found to already contain two 12F migrations and an active `fpp-owner-invite` Edge Function that are not present on `main`. The repository is therefore temporarily behind the live backend. Do not create a second competing implementation.

## Approved behavior

The approved Phase 12 design governs:
- all privileged mutations authenticate the caller and re-read exact ACTIVE OWNER Membership;
- every mutation is scoped by Organization UUID;
- MEMBER has no administration authority, including direct API/RPC calls;
- one pending invitation per Organization + normalized email;
- same-role invite resend is idempotent and reuses the invitation identity;
- changing intended role requires an explicit Owner action;
- invitation cancellation/expiry blocks later activation;
- acceptance requires the authenticated, email-confirmed Auth User to control the invited email;
- duplicate acceptance returns the same Membership;
- role/revoke/reactivate target exact Membership UUID;
- final ACTIVE OWNER cannot be demoted/revoked;
- concurrent last-Owner operations must be serialized;
- server audit stores only actor/target/action/time/result, never photo/job/Drive/token data;
- Android contains only the publishable key and a narrow Owner UI.

## Current live backend discovered during preflight

Dedicated project: `vtyiktvqhbgabawotkrj` (Field Photo Prep).

Applied live migrations not yet present in the repo:
- `20260925195816_phase_12f_owner_member_admin`
- `20260925195901_phase_12f_rpc_grant_hardening`

Live additions include:
- invitation delivery status/attempt fields;
- `public.fpp_admin_audit`;
- private organization lock / active-owner / audit helpers;
- public Owner RPCs for member/invitation listing and mutations;
- `public.fpp_accept_invitation`;
- active `fpp-owner-invite` Edge Function with JWT verification enabled.

A disposable Organization named `Phase 12F Race Fixture` is also present and must not be mistaken for production Organization data.

## Owning surfaces

Expected server/repo owners:
- `supabase/migrations/*phase_12f*.sql`
- `supabase/functions/fpp-owner-invite/index.ts`
- Phase 12F design/evidence/build-state docs

Expected Android owners after backend reconciliation:
- `SupabaseAuthClient.java` or one narrowly scoped admin client
- `AuthActivity.java` and/or a small Owner administration Activity/layout
- invitation redirect acceptance path
- focused tests for serialization/parsing/role gating

No Drive, photo destination, queue, capture, preparation, or provider identity owner is transferred to Phase 12F.

## Read/write surfaces

Server reads:
- current `auth.uid()` and confirmed Auth email;
- exact Organization;
- exact same-Organization Membership;
- same-Organization minimal member roster;
- same-Organization invitation rows.

Server writes:
- invitation row lifecycle/delivery metadata;
- Membership role/status;
- minimal admin audit rows;
- Supabase Auth invite delivery through the server-only Edge Function.

Android reads:
- current central authorization/session state;
- minimal Owner roster/invitation results.

Android writes:
- only narrow RPC/Edge Function requests; never direct table INSERT/UPDATE/DELETE.

## Required data

Required:
- Organization UUID;
- current Auth User/session;
- exact Membership UUID for member mutations;
- normalized invitation email;
- intended role OWNER or MEMBER;
- invitation UUID for invitation mutations/acceptance.

Optional:
- redirect URI limited to approved FPP internal/production callbacks.

Forbidden backend data for this phase:
- photos, addresses, work-order data, provider IDs, SAF URI, Drive identity, passwords, tokens in audit/log payloads.

## Schema / identity / permission changes

This phase adds privileged server operations and minimal invitation-delivery/audit schema only. Permanent User identity remains `auth.users.id`; Membership and Organization UUIDs remain authoritative. No email-based permanent identity is introduced.

Authenticated table grants stay read-only. Mutation authority is exposed only through explicitly granted narrow SECURITY DEFINER RPCs and the JWT-protected invite Edge Function. Private helper execution remains unavailable to authenticated/anon callers.

## Duplicate / idempotency behavior

- normalized Organization + email has at most one PENDING invitation;
- same-role resend reuses the pending invitation;
- Membership unique `(organization_id,user_id)` remains authoritative;
- duplicate acceptance returns the existing Membership;
- repeated role/revoke/reactivate/cancel operations are safe/idempotent;
- no silent role replacement during invite resend.

## Offline / stale-state behavior

Owner administration is online-only. GRACE does not grant member administration. A stale Android Owner UI is not authority: every server mutation rechecks the current caller inside the operation. Revocation learned by the device remains governed by Phase 12E.

## Drive / destination impact

No Google Drive integration impact.

No SAF binding, company/property/work-order folder, queued destination, upload, retry, cleanup, or remote provider identity may be changed by Phase 12F.

## Fixtures and safety

Backend tests use disposable Auth users / Organizations / Memberships / Invitations only. The live production Organization `In And Out Cleaner Inspections LLC` is not a mutation test target except for read-only Owner-scope verification and deliberate operator invitation acceptance.

The existing `Phase 12F Race Fixture` must be reconciled and removed only after its evidence value is understood.

## Focused tests

Required before merge:
- MEMBER/anon direct mutation denied;
- cross-Organization list/mutation denied;
- invite normalization and duplicate resend;
- explicit invitation role change;
- cancel/expired acceptance denied;
- confirmed-email acceptance and duplicate acceptance idempotency;
- role/revoke/reactivate idempotency;
- final-owner demotion/revoke blocked;
- concurrent last-owner operations leave at least one ACTIVE OWNER;
- audit writes only approved minimal fields;
- invitation delivery failure remains visible/retryable;
- Android Owner UI unavailable to MEMBER/non-validated state;
- invitation acceptance does not borrow a different identity/session.

## Baseline / complete regression

Baseline is merged Phase 12E `31bfaaffa012cadf2da0c9c31c6da64967d9d24f`.

Final runtime head must pass the complete Android CI suite. Backend schema/grants/RLS/advisors and disposable hosted authorization checks must pass on the exact deployed state.

## Failure recovery / rollback

If a server mutation path is unsafe:
1. stop Owner mutation UI/Edge Function use;
2. preserve existing Membership/Invitation rows;
3. do not alter Drive/photo state;
4. revert the narrow Android/UI change if needed;
5. harden or revoke the affected public RPC grant/function;
6. restore the prior known-good migration/function definition through a new forward migration rather than editing applied migration history.

No rollback deletes Auth users, protected photos, queue state, or Drive data.

## Merge approval

Not yet granted for Phase 12F. Level 3 explicit operator approval is required after exact-head automated/backend/device evidence passes.
