# Phase 12F — Owner/Member Administration: Level 3 Impact Record

Date: 2026-09-25  
Status: impact record ready; implementation not started  
Baseline / rollback commit: `31bfaaffa012cadf2da0c9c31c6da64967d9d24f` (Phase 12E merged main)  
Branch: `phase-12f/owner-member-administration`

## Problem and approved scope

Phase 12E makes the authenticated FPP User + Organization authoritative for ordinary runtime work, but an OWNER still has no governed way to administer Memberships and invitations from the app. Phase 12F adds OWNER/MEMBER administration only: same-Organization roster/pending-invitation reads; invite/resend; cancel invitation; explicit role change; revoke; reactivate; invitation acceptance; duplicate/idempotency protection; last-active-Owner protection; and minimal server-side audit evidence.

This slice does not add Organization switching, Drive binding, first-run Drive setup, diagnostics, production signing, job/photo mirroring, public signup, or Team integration.

## Change class and ownership

Level 3 because this changes remote identity/Membership state and adds privileged server operations.

Planned owners:
- Supabase migration(s): transactional Membership/Invitation invariants, narrow RPC/server contracts, audit table/function support where needed.
- Supabase Edge Function(s), only where Supabase Auth administration/email delivery requires a server-held secret. The service-role key remains server-side and never enters Android or the repository.
- Android account administration client: authenticated calls using the existing publishable key + current user access token.
- A narrow owner-only administration screen/activity. UI renders server state and requests operations; it does not become the authorization source.
- Focused JVM/instrumented tests for request/response parsing and OWNER/MEMBER UI gating.
- Hosted disposable-user tests for RLS, cross-Organization isolation, duplicate/idempotency, last-Owner concurrency, invitation delivery/acceptance, revoke/reactivate, and audit behavior.

## Required and optional data

Required:
- current authenticated Auth User UUID and access token;
- exact active Organization UUID;
- exact caller Membership UUID/role/status;
- target Membership UUID for role/revoke/reactivate;
- normalized invitation email, intended role, invitation UUID/status/expiry;
- server timestamp for expiry/audit;
- minimal actor/target/action/result audit metadata.

Optional:
- display email/contact for roster/pending invitation presentation;
- server-generated delivery/error classification needed to show retryable invitation delivery state.

Forbidden from 12F backend/admin data:
- photos or image bytes;
- property/address/work-order data;
- Drive provider IDs, SAF URIs, Google account identity;
- passwords, refresh tokens, access tokens, service-role keys;
- customer notes.

## Schema, identity, permission and platform-access changes

Expected backend changes:
- add only the minimal schema/functions needed to make privileged membership/invitation mutations atomic and auditable;
- narrow server operations authenticate `auth.uid()`, re-read the exact caller Membership as `ACTIVE OWNER` inside the operation, and scope every target by Organization UUID;
- enforce one pending invitation per Organization + normalized email;
- serialize/guard any operation that could remove/demote/revoke the last ACTIVE OWNER;
- preserve stable Auth User UUID and stable Membership UUID;
- MEMBER receives no admin mutation authority through RLS, RPC, Edge Function, or direct REST;
- authenticated table grants remain read-only unless a narrowly reviewed change is required. Android never receives insert/update/delete grants merely for convenience.

Expected server-secret boundary:
- if invitation email delivery or Auth-user administration needs Supabase Admin APIs, use a deployed Edge Function/server runtime with the service-role secret in platform secrets only;
- no service-role credential is committed, returned to Android, logged, or embedded in an APK.

Android changes:
- no new Android permission;
- no Drive-tree/SAF permission change;
- no persisted Drive or photo destination change.

No Google Drive integration impact.

## Reads and writes

Reads:
- same-Organization minimal roster and pending invitations;
- current runtime authorization decision/session;
- operation result/status.

Writes:
- invitation PENDING/CANCELLED/ACCEPTED/EXPIRED state;
- Membership role/status transitions;
- invited Auth User linkage after proven Auth email control;
- minimal audit rows;
- Auth invitation/recovery delivery through server-side Supabase Auth administration where required.

No operation may rewrite a photo, queue record, local destination, Drive folder, or provider identity.

## Invitation lifecycle and idempotency

Invite input is normalized email + intended role + exact Organization.

Rules:
- at most one PENDING invitation per Organization + normalized email;
- repeated invite for the same intended role is idempotent and reuses the pending invitation identity;
- resend refreshes delivery safely rather than creating a second Membership;
- a different intended role requires an explicit Owner update/role operation, not silent replacement;
- cancel/expiry makes the invitation unusable for Organization activation;
- acceptance proves the currently authenticated Auth User controls the invited email through the Auth flow, then atomically creates or activates exactly one Membership in the invited Organization;
- duplicate acceptance returns the same Membership;
- a callback token by itself never grants Membership;
- cross-Organization invitation data is not exposed.

## Membership lifecycle and last-Owner protection

Role change, revoke, and reactivate target exact Membership UUID + Organization UUID.

Rules:
- repeat same-state operations are idempotent;
- MEMBER cannot call any admin mutation directly or through API;
- the final ACTIVE OWNER cannot be demoted, revoked, or removed;
- two concurrent operations that would otherwise leave zero ACTIVE OWNERs must serialize/fail safely so at least one ACTIVE OWNER remains;
- owner self-revocation through another device/server operation cannot depend on target-phone local protected-work state; the revoked phone becomes read-only when 12E learns the authoritative revocation;
- local Sign Out remains separately guarded by 12E protected-work truth;
- reactivation requires authoritative ACTIVE revalidation; local stale grace cannot restore admin authority.

## Offline and stale-state behavior

Administration is online-only.

- VALIDATED OWNER may administer when the server operation re-validates OWNER authority.
- GRACE never grants member administration.
- MEMBER never gains admin controls.
- stale roster UI is informational only and cannot authorize a mutation.
- network failure leaves prior server state authoritative; do not optimistically mark a role/revocation/invite as successful.
- after any successful membership mutation affecting the current device/user, runtime authorization must be revalidated before ordinary protected work continues under the new state.

## Safe fixtures and hosted tests

Use disposable Supabase Auth users and a disposable Organization/Memberships/Invitations fixture in the dedicated Field Photo Prep Supabase project. Do not use customer photo/job data and do not touch the separate Team project.

Required hosted evidence:
- OWNER can read same-Organization minimal roster and pending invitations;
- MEMBER can read only what design permits and cannot invoke any admin mutation;
- unrelated/cross-Organization authenticated users cannot read or mutate the fixture;
- duplicate same-role invite is idempotent;
- different-role change is explicit;
- cancel makes invitation unusable;
- invitation delivery + authenticated acceptance creates/activates exactly one Membership;
- duplicate acceptance returns same Membership;
- revoke/reactivate is idempotent;
- last-owner demote/revoke is rejected;
- concurrent last-owner operations cannot leave zero ACTIVE OWNERs;
- audit contains only allowed minimal metadata;
- disposable rows/users are removed or returned to the documented safe fixture state after verification.

## Android tests and UI smoke

Focused tests:
- owner-only admin entry visibility/enablement;
- MEMBER/no-membership/revoked/grace states cannot administer;
- request normalization and exact Organization/Membership targeting;
- server error/idempotent result parsing;
- no service-role key or privileged credential appears in Android source/resources/APK configuration.

Instrumented/device smoke:
- OWNER opens administration, sees same-Organization roster/pending invite state;
- one disposable invitation operation and resulting state are visible;
- MEMBER view has no mutation controls;
- revocation/reactivation reflected after authoritative recheck.

Run the complete Android suite once on the exact final runtime head before merge.

## Baseline, expected result and rollback

Baseline/rollback: `31bfaaffa012cadf2da0c9c31c6da64967d9d24f`.

Expected:
- existing field/camera/Drive workflow remains unchanged;
- OWNER gains narrow same-Organization administration;
- MEMBER cannot administer;
- all privileged state transitions remain atomic/server-authoritative;
- zero-active-owner state cannot be produced by supported operations;
- invitations are duplicate-safe and acceptance is exact/idempotent;
- Android contains only publishable project credentials.

Failure recovery:
- stop affected admin operations;
- preserve current Membership/Invitation records and audit evidence;
- do not delete protected photos or alter Drive bindings as rollback;
- revert the narrow Android/server branch changes if required;
- if a hosted mutation partially fails, inspect authoritative server state before retry; never infer success from client timeout;
- if invitation delivery is uncertain, retain retryable pending state and do not create a duplicate Membership.

## Structural preconditions before implementation

Before mutation code:
1. inventory the exact existing 12C schema/RLS/grants and confirm read paths;
2. choose the narrow server boundary for Auth invite delivery without exposing service-role credentials;
3. define exact transactional locking for last-owner concurrency;
4. define invitation acceptance proof using the authenticated Auth User/email rather than callback possession alone;
5. confirm current Android account/session client can supply the authenticated access token and Organization identity without bypassing 12E.

Any contradiction with the Phase 12 complete design or contracts stops implementation until reconciled.

## Approval gate

The user's request to continue Phase 12F authorizes implementation of this documented scope. This record does not authorize merge. Phase 12F remains Level 3 and requires explicit operator approval after focused tests, hosted disposable-user evidence, full final-head Android CI, and the proportional device/admin reality gate pass.
