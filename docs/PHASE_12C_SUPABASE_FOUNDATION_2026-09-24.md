# Phase 12C — Dedicated FPP Supabase Foundation

Date: 2026-09-24

Status: **IN PROGRESS — SCHEMA/RLS VERIFIED; FIRST OWNER PENDING**

## Scope

Create and prove the minimum original-FPP Supabase identity backend.

No Android authentication code is included.

## Straight-line implementation

1. **COMPLETE** — dedicated Supabase project created: **Field Photo Prep**, project ref `vtyiktvqhbgabawotkrj`, under **In And Out Cleaner Inspections**.
2. Keep region aligned with the existing Team project unless cost/availability requires otherwise: `us-east-2`.
3. **COMPLETE** — applied the reviewed FPP-only schema.
4. **COMPLETE** — verified tables, constraints, grants, RLS, helper functions, and migration history.
5. **COMPLETE** — created disposable rollback-only Auth/User/Organization fixtures.
6. **COMPLETE** — proved hosted allow/deny behavior.
7. **COMPLETE** — ran security + performance advisors; security clean, only expected empty-database unused-index informational notices remain.
8. **COMPLETE** — disposable fixtures rolled back; persistent identity row counts returned to zero.
9. **PENDING** — create the first real FPP Auth Owner identity.
10. Bootstrap **In And Out Cleaner Inspections LLC** + exact OWNER Membership.
11. Verify the Owner can read exactly their Organization/Membership.
12. Record project ref, migration identity, verification, and rollback evidence in the repository.
13. Stop at the backend boundary; Android auth work begins in the next governed slice.

## Explicit non-goals

Do not:
- touch Team;
- add Android login UI;
- add Supabase SDK/framework dependencies;
- add customer/job/photo data;
- add Supabase Storage;
- add Realtime;
- add Google OAuth;
- add public signup;
- deploy member invitation UI yet;
- change Drive/SAF behavior.

## First Owner bootstrap

The first real Owner is not created by public self-signup.

Bootstrap is a one-time trusted admin operation after the schema/RLS proof.

Required sequence:
1. create/confirm the intended Auth user;
2. capture exact `auth.users.id`;
3. in one trusted transaction, create the FPP Organization if absent;
4. create the exact ACTIVE OWNER Membership if absent;
5. re-read exact Organization/User/Membership identities;
6. fail rather than creating a duplicate if conflicting identity already exists.

Do not hardcode the future Organization UUID or User UUID into repository migration SQL.

## Completion gate

Phase 12C is complete only when:
- dedicated FPP Supabase project exists;
- Team remains separate and untouched;
- schema migration is applied;
- RLS/grants are proven;
- negative authorization tests pass;
- duplicate constraints pass;
- advisors reviewed;
- first real Owner bootstrap succeeds;
- repository record is current;
- exact pre-merge approval is given.


## Project creation evidence — 2026-09-24

- Supabase organization: **In And Out Cleaner Inspections**
- organization ID: `zotxwywaqzhabixrnhso`
- project: **Field Photo Prep**
- project ref: `vtyiktvqhbgabawotkrj`
- region: `us-east-2`
- project status after creation: `ACTIVE_HEALTHY`
- quoted project cost at creation: **$0/month**
- initial public tables: **0**
- initial migration history: **0**
- separate Team project ref: `vyocaujuwrivoqynvitm`
- Team status at verification: `INACTIVE`

Result: the original-FPP backend now has its own clean Supabase project and Team remains separate.


## Hosted backend verification summary

Permanent evidence:
`docs/PHASE_12C_SUPABASE_VERIFICATION_2026-09-24.md`

Applied migration history:
- `20260925012939_phase_12c_identity_foundation`
- `20260925013000_phase_12c_invitation_fk_indexes`

Hosted authorization tests passed:
- Member cross-Organization isolation;
- Owner same-Organization Membership/Invitation read;
- Member Invitation denial;
- duplicate Membership prevention;
- duplicate pending Invitation prevention;
- anon read denial;
- direct authenticated mutation denial.

Security advisor: **0 lints**.

Current persistent rows after rollback-only tests:
- Organizations: 0
- Memberships: 0
- Invitations: 0
- Auth users: 0

Next genuine gate:
- select the real first-Owner login email;
- create that user through supported Supabase Auth;
- bootstrap the real Organization + ACTIVE OWNER Membership;
- verify exact Owner scope.


## First Owner identity selection — 2026-09-24

The operator selected the first original-FPP Owner login email:

`inandoutinspections2026@gmail.com`

This email is bootstrap/login data only. Permanent FPP User identity remains the resulting Supabase Auth UUID.

The connected Supabase tool surface does not expose a supported Auth-admin create/invite-user action, and temporary server-bootstrap deployment attempts were blocked by platform safety checks. The project will not bypass Supabase Auth by directly manufacturing a production login row in `auth.users`.

Required next action:
- create the Auth user through Supabase's normal hosted Authentication → Users flow using the selected email and an operator-chosen password;
- once the Auth UUID exists, Phase 12C will create/reuse exactly one **In And Out Cleaner Inspections LLC** Organization and exactly one `ACTIVE OWNER` Membership for that UUID;
- then hosted Owner-scope verification completes the backend gate.
