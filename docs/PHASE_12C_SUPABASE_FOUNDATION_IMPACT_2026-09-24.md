# Phase 12C — Dedicated FPP Supabase Foundation — Level 3 Impact Record

Date: 2026-09-24

Status: **BACKEND IMPLEMENTATION COMPLETE — FINAL LEVEL 3 MERGE APPROVAL PENDING**

## User-facing problem

Phase 12B selected Supabase for original FPP identity, but original FPP still has no separate Supabase project or identity schema.

The backend foundation must be created without importing Team data, Team identities, customer jobs, photos, Drive IDs, or Android runtime behavior.

## Approved behavior

Phase 12C is backend-only.

Create one dedicated Supabase project for original FPP and prove the minimum identity model:
- Organizations;
- Memberships;
- Invitations;
- RLS-backed read authorization;
- explicit Data API grants;
- controlled first-Owner bootstrap;
- wrong-user / wrong-Organization denial.

Do not add Android auth code in this slice.

## Dedicated environment boundary

Supabase organization currently available:
- **In And Out Cleaner Inspections**
- organization ID: `zotxwywaqzhabixrnhso`

Existing Team project:
- **Field Photo Prep Team**
- project ref: `vyocaujuwrivoqynvitm`
- remains separate and untouched.

New project:
- name: **Field Photo Prep**
- project ref: `vtyiktvqhbgabawotkrj`
- region: **us-east-2**
- status after creation: `ACTIVE_HEALTHY`
- quoted cost at creation: **$0/month**
- initial public tables: **0**
- initial migration history: **0**.

No Team project key, Auth user, table, Edge Function, migration, Organization UUID, or session may be reused.

## Change classification

Level 3 because this creates:
- a new authentication/backend environment;
- persisted identity schema;
- RLS authorization boundaries;
- first-Owner identity state.

## Owning repository surfaces

- `docs/PHASE_12C_SUPABASE_FOUNDATION_2026-09-24.md`
- `docs/PHASE_12C_SUPABASE_FOUNDATION_IMPACT_2026-09-24.md`
- `docs/PHASE_12C_SUPABASE_SCHEMA_DRAFT_2026-09-24.sql`
- `docs/ROADMAP.md` when completion evidence exists.

No Android runtime file is authorized by this slice.

## Read surfaces

- Phase 12A Identity Model;
- Phase 12B dedicated-Supabase architecture;
- Supabase Auth user identity;
- Membership rows;
- Organization rows;
- Invitation rows.

## Write surfaces

Future external/backend writes in this slice:
- create one dedicated Supabase project;
- create minimum FPP identity schema;
- create test fixtures;
- create first real FPP Owner Auth user only at controlled bootstrap;
- create the first real FPP Organization + Owner Membership.

No Google Drive write is part of 12C.

## Required data

### Organization
- UUID;
- name;
- status;
- timestamps.

### Membership
- UUID;
- Organization UUID;
- Auth User UUID;
- OWNER/MEMBER;
- INVITED/ACTIVE/REVOKED;
- timestamps.

### Invitation
- UUID;
- Organization UUID;
- normalized email;
- intended role;
- PENDING/ACCEPTED/CANCELLED/EXPIRED;
- optional linked Auth User UUID;
- inviting Auth User UUID;
- expiration;
- timestamps.

## Optional data

Not required in 12C:
- profile display name;
- avatar;
- phone;
- client-company names;
- addresses;
- work orders;
- photos;
- Drive IDs;
- SAF URIs.

## Schema / identity / permission changes

Phase 12C adds the first original-FPP server identity schema.

Every public FPP table:
- has RLS enabled;
- grants only required read privileges to `authenticated`;
- grants nothing to `anon`;
- exposes no direct authenticated INSERT/UPDATE/DELETE path.

Privileged mutations remain server/admin-only until a later invitation/member-admin slice.

Private helper functions may use `SECURITY DEFINER` only where required to evaluate Membership without recursive RLS. They must:
- live in non-exposed `private` schema;
- set an empty/fixed `search_path`;
- validate `auth.uid()`;
- have PUBLIC execute revoked;
- grant only required execution to `authenticated`.

## Authorization model

Authenticated User may:
- read an Organization only through an ACTIVE Membership;
- read their own Membership;
- if ACTIVE OWNER, read same-Organization Memberships;
- if ACTIVE OWNER, read same-Organization Invitations.

Authenticated Android clients may not directly:
- insert Organization;
- insert/update/revoke Membership;
- create/cancel Invitation;
- close Organization.

Those actions require later trusted server-side account actions.

## Duplicate / idempotency behavior

- one Membership maximum per `(organization_id,user_id)`;
- one pending Invitation maximum per `(organization_id,normalized_email)`;
- repeated first-Owner bootstrap must detect an existing exact Organization/Owner relationship instead of creating another;
- User identity is Auth UUID, never email;
- Organization identity is FPP UUID, never Drive folder or Team Organization UUID.

## Offline/stale-state behavior

12C is backend-only, so Android offline behavior does not change yet.

The schema must support later 12B rules:
- ACTIVE;
- INVITED;
- REVOKED;
- OWNER/MEMBER.

No cached/mobile authorization is implemented here.

## Safe fixture plan

Before first real Owner bootstrap:
- use disposable Auth users and disposable Organizations to test RLS;
- use no Team project;
- use no live customer data;
- use no Google Drive data.

Prove:
1. member can read own Organization;
2. member cannot read another Organization;
3. member can read own Membership;
4. member cannot enumerate unrelated Memberships;
5. owner can read same-Organization Memberships;
6. owner can read same-Organization Invitations;
7. member cannot read same-Organization Invitations;
8. anon can read none of the FPP tables;
9. authenticated client cannot directly mutate identity rows;
10. duplicate Membership and duplicate pending Invitation are blocked.

## Baseline expected verification

After schema creation:
- list tables/columns/foreign keys;
- run focused allow/deny SQL/API tests;
- inspect migration history;
- run Supabase security advisor;
- run performance advisor;
- verify no public/anon mutation path exists;
- verify Team project remains unchanged.

No Android test is required until Android runtime auth work begins.

## Failure recovery

Before real Owner bootstrap:
- project/schema may be discarded and recreated if the design itself is invalid.

After real Owner bootstrap:
- fix forward through a governed migration;
- do not silently regenerate User or Organization identity because of an RLS/policy defect.

Team is never used as rollback.

## Rollback point

Repository base:
`a430c6334ad4e28be35e987a0acc09724d762783`

External rollback before first real bootstrap:
- delete/pause only the new dedicated FPP project if necessary;
- Team remains untouched.

## Primary risks

- accidentally creating in Team project;
- weak RLS allowing cross-Organization reads;
- direct client mutation privileges;
- duplicate Owner/Organization bootstrap;
- using email as identity;
- introducing customer/job/photo data into Supabase.

## Explicit pre-merge approval

Phase 12C implementation may be developed and tested on its dedicated branch/environment under the approved roadmap.

Final Level 3 merge still requires explicit operator pre-merge approval after exact schema/RLS evidence is available.


## Hosted verification evidence

Applied migrations:
- `20260925012939_phase_12c_identity_foundation`
- `20260925013000_phase_12c_invitation_fk_indexes`

Verification results:
- all three FPP identity tables have RLS enabled;
- authenticated role has SELECT only;
- anon has no SELECT;
- authenticated role has no direct identity mutation privileges;
- Member cross-Organization isolation: PASS;
- Owner same-Organization Membership/Invitation read: PASS;
- Member Invitation read: DENIED as designed;
- duplicate Membership constraint: PASS;
- duplicate pending Invitation constraint: PASS;
- disposable fixtures rolled back fully;
- security advisor: 0 lints;
- performance advisor: no missing-FK-index findings after the second migration.

Permanent evidence:
`docs/PHASE_12C_SUPABASE_VERIFICATION_2026-09-24.md`

The remaining implementation gate is the first real FPP Owner bootstrap. No owner email has been selected in audited original-FPP history, so that identity must not be guessed.


## First Owner identity selection — 2026-09-24

The operator selected the first original-FPP Owner login email:

`inandoutinspections2026@gmail.com`

This email is bootstrap/login data only. Permanent FPP User identity remains the resulting Supabase Auth UUID.

The connected Supabase tool surface does not expose a supported Auth-admin create/invite-user action, and temporary server-bootstrap deployment attempts were blocked by platform safety checks. The project will not bypass Supabase Auth by directly manufacturing a production login row in `auth.users`.

Required next action:
- create the Auth user through Supabase's normal hosted Authentication → Users flow using the selected email and an operator-chosen password;
- once the Auth UUID exists, Phase 12C will create/reuse exactly one **In And Out Cleaner Inspections LLC** Organization and exactly one `ACTIVE OWNER` Membership for that UUID;
- then hosted Owner-scope verification completes the backend gate.


## Real Owner bootstrap completion

Verified real identities:
- Auth User UUID: `789bc0d8-6f9d-4ab4-9f87-33195109a65a`
- Organization UUID: `3198253e-8d41-4419-874e-46e2d3906928`
- Membership UUID: `de55393f-6386-480a-9823-b6ee4d9901a0`
- Membership: `OWNER / ACTIVE`

Hosted exact-scope test:
- real Owner reads exactly their Organization and Membership;
- unrelated authenticated UUID reads no FPP identity rows.

The initial invite exposed a default `localhost:3000` redirect. That redirect issue belongs to the following Android Auth/session/invitation-flow slice and does not invalidate the Phase 12C backend identity/RLS proof.

Final Phase 12C Level 3 merge approval is now the remaining gate.
