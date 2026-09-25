# Phase 12C — Hosted Supabase Verification Evidence

Date: 2026-09-24

Project:
- name: **Field Photo Prep**
- ref: `vtyiktvqhbgabawotkrj`
- region: `us-east-2`

Separate Team project:
- name: **Field Photo Prep Team**
- ref: `vyocaujuwrivoqynvitm`
- status during final verification: `INACTIVE`

## Applied migrations

1. `20260925012939_phase_12c_identity_foundation`
2. `20260925013000_phase_12c_invitation_fk_indexes`

The second migration was added after the Supabase performance advisor identified two invitation foreign keys without covering indexes.

## Schema verification

Hosted project contains exactly the Phase 12C public identity tables:
- `fpp_organizations`
- `fpp_memberships`
- `fpp_invitations`

All three report RLS enabled.

## Privilege verification

`authenticated` has SELECT only on the three FPP tables.

`anon` has no SELECT privileges on the three FPP tables.

`authenticated` has no INSERT/UPDATE/DELETE table privileges for Organization/Membership/Invitation identity data.

Privileged account mutations therefore remain outside the ordinary client Data API path.

## Helper function verification

Private helper surface:
- `private.fpp_is_active_member(uuid)` — SECURITY DEFINER; authenticated EXECUTE only
- `private.fpp_is_active_owner(uuid)` — SECURITY DEFINER; authenticated EXECUTE only
- `private.fpp_set_updated_at()` — not SECURITY DEFINER; postgres EXECUTE only

Default PUBLIC execute is absent from all FPP private helpers.

## Hosted RLS tests

Disposable placeholder Auth rows and FPP fixtures were created only inside rollback-only transactions using the Supabase-documented development seed pattern.

### Member cross-Organization isolation — PASS

Authenticated MEMBER A:
- visible Organizations: 1
- visible Organization: only Fixture Org A
- visible Memberships: 1
- visible Membership: only their own
- visible Invitations: 0

Unrelated Fixture Org B and its Membership were not visible.

### Owner same-Organization administration read — PASS

Authenticated OWNER:
- visible Organizations: 1
- visible Memberships: 2 (OWNER + MEMBER in the same Organization)
- visible Invitations: 1
- unrelated Organization not visible

### Member invitation isolation — PASS

Authenticated MEMBER in an Organization containing a pending Invitation:
- visible Organizations: 1
- visible Memberships: 1 (self)
- visible Invitations: 0

### Duplicate constraints — PASS

Actual inserts proved:
- duplicate `(organization_id,user_id)` Membership blocked by unique constraint;
- duplicate PENDING Invitation for the same normalized Organization/email blocked by partial unique index.

### Direct-client mutation privileges — PASS

Verified false for authenticated:
- Organization INSERT / UPDATE / DELETE
- Membership INSERT / UPDATE
- Invitation INSERT / UPDATE

Verified false for anon table SELECT.

## Fixture cleanup verification

After all rollback-only authorization tests:

- persistent Organizations: 0
- persistent Memberships: 0
- persistent Invitations: 0
- persistent Auth users: 0

No disposable test identity remained.

## Advisor results

Security advisor:
- **0 lints**

Performance advisor after FK-index migration:
- no missing-foreign-key-index findings;
- only `unused_index` informational notices remain, expected on a newly created empty database.

Do not delete those indexes merely because an empty database has not used them yet.

## Current boundary

Backend schema/RLS foundation is proven.

Still outstanding before Phase 12C can be called complete:
- create the first real FPP Auth Owner through the supported Supabase Auth path;
- bootstrap the real FPP Organization + ACTIVE OWNER Membership;
- verify exact Owner read scope;
- update the Phase 12C completion record;
- obtain final Level 3 pre-merge approval.

No Android authentication runtime has started.


## First Owner identity selection — 2026-09-24

The operator selected the first original-FPP Owner login email:

`inandoutinspections2026@gmail.com`

This email is bootstrap/login data only. Permanent FPP User identity remains the resulting Supabase Auth UUID.

The connected Supabase tool surface does not expose a supported Auth-admin create/invite-user action, and temporary server-bootstrap deployment attempts were blocked by platform safety checks. The project will not bypass Supabase Auth by directly manufacturing a production login row in `auth.users`.

Required next action:
- create the Auth user through Supabase's normal hosted Authentication → Users flow using the selected email and an operator-chosen password;
- once the Auth UUID exists, Phase 12C will create/reuse exactly one **In And Out Cleaner Inspections LLC** Organization and exactly one `ACTIVE OWNER` Membership for that UUID;
- then hosted Owner-scope verification completes the backend gate.
