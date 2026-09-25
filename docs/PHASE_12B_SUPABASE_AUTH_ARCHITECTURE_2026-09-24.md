# Phase 12B — Dedicated Supabase Authentication Architecture

Date: 2026-09-24

Status: **SETTLED DESIGN — IMPLEMENTATION NOT STARTED**

This design applies only to the original Field Photo Prep app in `timbone72-CC/field-photo-prep`.

It does **not** reuse the Field Photo Prep Team Supabase project, Team Auth users, Team tables, Team keys, Team roles, or Team work-order/photo backend.

## Goal

Add durable FPP User/Organization authentication with the smallest practical backend while preserving the existing Android SAF/Google Drive workflow.

Supabase is used only for FPP account identity and membership.

Google Drive remains authoritative for:
- client-company folders;
- properties/addresses;
- work orders;
- photos.

## Dedicated Supabase boundary

Create one dedicated Supabase project for original FPP.

Working project name:

`Field Photo Prep`

This project is separate from:

`Field Photo Prep Team`

Separation is mandatory:
- separate Supabase project/reference;
- separate Auth users;
- separate database;
- separate API/publishable keys;
- separate Edge Functions/secrets;
- separate migration history;
- no cross-project user, membership, work-order, photo, or session reuse.

The two projects may exist under the same Supabase account/organization, but that administrative convenience does not make them one runtime/backend environment.

## Authentication method

FPP v1 uses **Supabase Auth email + password**.

Reasons:
- simple on Android;
- keeps FPP sign-in visibly separate from Google Drive account selection;
- does not require Google OAuth setup;
- supports invitation and password-recovery flows;
- avoids changing the existing Java app to a broader authentication framework.

Google/social sign-in is not part of Phase 12B.

It may be added later without changing permanent FPP User identity.

## Signup model

FPP v1 is **invitation-only** after the initial Owner bootstrap.

Do not expose open public self-signup in v1.

Initial setup:
1. create the dedicated FPP Supabase project;
2. create the first FPP Auth user through a controlled bootstrap;
3. create the first FPP Organization;
4. create that User's `OWNER` Membership;
5. normal additional users enter through the governed invitation path.

Future public self-service Organization creation is a separate product/release decision.

## Permanent identities

### User

Permanent FPP User identity:

`auth.users.id`

The Supabase Auth UUID is the internal FPP `user_id`.

Email is a credential/contact field and is never permanent identity.

### Organization

Create a new FPP Organization UUID in the dedicated FPP Supabase project.

The current Organization display name is:

`In And Out Cleaner Inspections LLC`

Do **not** reuse a Team Organization UUID merely because the real-world business is the same.

The two products have separate backend identity spaces.

### Membership

A Membership joins one FPP User to one FPP Organization.

Roles:
- `OWNER`
- `MEMBER`

Statuses:
- `INVITED`
- `ACTIVE`
- `REVOKED`

One User may eventually have Membership in more than one Organization, but Android Identity v1 still permits only one active Organization locally at a time.

## Minimum database model

### `organizations`

Required fields:
- `id uuid primary key`
- `name text not null`
- `status text not null` — initial values `ACTIVE` / `CLOSED`
- `created_at`
- `updated_at`

### `memberships`

Required fields:
- `id uuid primary key`
- `organization_id uuid not null`
- `user_id uuid not null references auth.users(id)`
- `role text not null` — `OWNER` / `MEMBER`
- `status text not null` — `INVITED` / `ACTIVE` / `REVOKED`
- `created_at`
- `updated_at`
- unique `(organization_id, user_id)`

### `invitations`

Required fields:
- `id uuid primary key`
- `organization_id uuid not null`
- normalized invited email
- intended role
- invitation status
- nullable linked `auth_user_id`
- `invited_by_user_id`
- expiration timestamp
- `created_at`
- `updated_at`

Initial invitation states:
- `PENDING`
- `ACCEPTED`
- `CANCELLED`
- `EXPIRED`

No property, work-order, address, photo, Drive provider ID, SAF URI, route, or inspection table belongs in the FPP identity backend.

## No duplicate profile database

A separate public `users` table is not required merely to mirror `auth.users`.

If a future user-facing profile requires data beyond Auth, add only the minimum separately governed profile fields.

Authorization never depends on display name or editable user metadata.

## Authorization

Database Membership rows are authoritative for FPP Organization authorization.

Do not use:
- email equality as authorization;
- `user_metadata`;
- Team's `app_metadata.role`;
- Team Organization claims;
- Drive account identity.

All exposed FPP tables must use RLS.

Required rule shape:
- signed-in User may read only Organizations reachable through their own Membership;
- User may read only their own Membership unless Owner administration requires broader same-Organization visibility;
- only active Owners may administer Memberships/Invitations for their Organization;
- one Organization can never read another Organization's Membership/Invitation data.

Because new Supabase projects no longer automatically expose every new public table to the Data API, Phase 12C must make required grants explicit and pair them with RLS.

## Privileged actions

The Android app never receives a Supabase service-role/secret key.

Privileged operations run server-side through narrowly scoped Edge Functions or private database functions.

Examples:
- create/cancel invitation;
- accept invitation;
- revoke/reactivate Membership;
- change MEMBER ↔ OWNER;
- first Owner bootstrap.

Each privileged action must:
- validate the caller's current Auth identity;
- validate exact Organization Membership;
- enforce required role/status;
- be retry-safe/idempotent;
- avoid broad unrestricted table mutation.

Any `SECURITY DEFINER` helper must remain out of an exposed API surface, explicitly validate caller identity, use a fixed safe `search_path`, and revoke default PUBLIC execution.

## Invitation behavior

Owner flow:
1. Owner enters an email and role.
2. Server normalizes the email.
3. Server verifies the caller is an `ACTIVE OWNER`.
4. Existing active/pending Membership/Invitation state is checked first.
5. One pending invitation is created or reused.
6. Supabase Auth issues the user setup/invite flow.
7. Recipient proves the exact Auth identity.
8. Invitation acceptance activates exactly one Membership.

Duplicate rules:
- repeating the same invite must not create duplicate Memberships;
- one `(organization_id,user_id)` Membership maximum;
- cancelled/expired invitations never silently activate;
- invitation email is discovery/contact data, not permanent identity.

Owner may invite another Owner.

## Auth email delivery

During internal development and low-volume internal use, the Supabase-provided Auth email service may be used for invite/confirmation/recovery testing.

Before outside-user/public release, configure a production-suitable custom SMTP/email delivery path.

Do not make purchasing a domain or SMTP service a prerequisite for beginning Phase 12 implementation.

Email-template branding is not required for the first internal build.

## Password recovery

Use Supabase Auth password recovery.

Owners never:
- see another user's password;
- choose another user's password;
- receive another user's password;
- store plaintext passwords.

Recovery proves the user's Auth identity through Supabase's recovery flow.

## Android client boundary

Keep original FPP Java-first.

Use a narrow HTTPS/Auth client for the required Supabase endpoints.

Do not migrate the app to Kotlin, Compose, Room, or another framework merely to add Supabase.

The Android app may contain:
- Supabase project URL;
- Supabase publishable key.

It must never contain:
- secret/service-role key;
- database password;
- SMTP password;
- Edge Function secret;
- Team Supabase credentials.

## Local session storage

Persist only what is required for authenticated/offline use:
- Supabase access token;
- Supabase refresh token;
- access-token expiry;
- authenticated User UUID;
- active Organization UUID;
- active Membership UUID/role/status snapshot;
- last successful Membership validation timestamp.

Auth token material must be encrypted at rest using Android Keystore-backed encryption.

Existing Phase 10F backup/device-transfer exclusions continue to apply: FPP auth/session state must not silently restore to another phone.

Never log tokens or include them in copied diagnostics.

## Online validation

Whenever usable network connectivity exists at startup/resume:
1. restore the encrypted local session if present;
2. refresh the Supabase Auth session when needed;
3. validate the Auth user;
4. fetch authoritative Membership state;
5. confirm the active Organization Membership is still `ACTIVE`;
6. record the successful validation time;
7. continue the existing Drive/company workflow.

A valid Auth account without an `ACTIVE` FPP Membership cannot begin normal Organization field work.

## Offline grace policy

FPP is a rural field app and must remain useful through ordinary connectivity loss.

A previously validated `ACTIVE` Membership may continue normal work for the **same active Organization for up to 72 hours** after the last successful server Membership validation.

Within that 72-hour window:
- existing Organization context remains usable;
- normal local capture/preparation remains available;
- existing Drive workflow may continue when Android/Drive access itself is available;
- Organization switching is unavailable;
- Membership/invitation administration is unavailable.

If 72 hours elapse without successful FPP Membership revalidation:
- preserve all protected originals and queue evidence;
- allow viewing/recovery/diagnostics;
- do not begin new capture for the Organization;
- do not start new remote Drive mutations;
- require successful Membership revalidation before ordinary work resumes.

A network/backend outage is not the same thing as revocation.

If the server authoritatively reports `REVOKED`, the offline grace ends immediately once that state is learned.

## Revocation

When FPP learns that a Membership is `REVOKED`:
- block new ordinary Organization work;
- block new remote Drive writes under that Membership;
- preserve protected originals;
- preserve queue/reconciliation evidence;
- do not rewrite stored Drive destinations;
- do not delete Drive data;
- do not transfer unfinished work to another User or Organization automatically.

Recovery of unfinished protected work requires an explicit governed path.

## Sign-out

Normal sign-out remains blocked while unresolved protected work would be stranded.

When sign-out is allowed:
- attempt normal Supabase session sign-out/revocation when online;
- clear local encrypted auth/session credentials;
- clear active User/Organization navigation state;
- do not delete protected photos;
- do not delete Drive content;
- do not change sharing;
- do not rewrite provider IDs.

The locally stored Drive binding remains Organization-bound and cannot authorize a different FPP Organization merely because Android still holds a SAF grant.

## Drive boundary

Supabase answers:

**Who is this person, and which FPP Organization may they operate for?**

Android SAF answers:

**Which Drive workspace on this device may FPP use?**

Supabase must never:
- select the Google Drive account;
- choose the SAF workspace;
- store the SAF tree URI;
- store provider document IDs;
- authorize Drive access merely because the same email happens to be used.

FPP Auth email and Android Drive account email are allowed to be different.

## Separate Team boundary

Original FPP must never read/write:
- Team Supabase Auth users;
- Team organizations/roles;
- Team work orders;
- Team photos;
- Team Room state;
- Team Edge Functions;
- Team Drive credentials;
- Team Supabase project keys.

Team likewise does not use the original-FPP Supabase project as a Team test surface.

Sharing implementation ideas is allowed; sharing runtime identity/data is not.

## Explicit exclusions

Phase 12B does not add:
- Supabase Storage;
- Supabase Realtime;
- job/work-order/photo synchronization;
- route data;
- client-company database records;
- Google Drive OAuth;
- Google/social sign-in;
- open public signup;
- subscriptions/licensing;
- employee scheduling;
- background location;
- Room/SQLite;
- WorkManager;
- a permanent local photo library.

## Phase 12C implementation order

After Phase 12B is approved/merged:

1. create the dedicated original-FPP Supabase project;
2. add the minimum schema + explicit grants + RLS;
3. build safe Owner bootstrap;
4. prove wrong-user/wrong-Organization access is denied;
5. add the narrow Java Auth/session client;
6. add encrypted local session storage;
7. add startup/72-hour offline gate;
8. add invitation/member administration;
9. run full Android regression and proportional phone reality gates.

No runtime authentication work begins before the Phase 12B contract/design merge.
