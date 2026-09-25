# Phase 12C — Dedicated FPP Supabase Foundation

Date: 2026-09-24

Status: **STAGED**

## Scope

Create and prove the minimum original-FPP Supabase identity backend.

No Android authentication code is included.

## Straight-line implementation

1. Create a second Supabase project named **Field Photo Prep** under the approved Supabase organization.
2. Keep region aligned with the existing Team project unless cost/availability requires otherwise: `us-east-2`.
3. Apply the reviewed FPP-only schema.
4. Verify tables, constraints, grants, RLS, helper functions, and migration history.
5. Create disposable Auth/User/Organization fixtures.
6. Prove allow/deny behavior.
7. Run security + performance advisors.
8. Remove disposable identity fixtures.
9. Create the first real FPP Auth Owner identity.
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
