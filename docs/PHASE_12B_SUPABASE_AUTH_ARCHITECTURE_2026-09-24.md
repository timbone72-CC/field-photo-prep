# Phase 12B — Supabase Authentication Architecture

Date: 2026-09-24

Status: **SETTLED DESIGN — IMPLEMENTATION NOT STARTED**

This record resolves the Phase 12A choices that were incorrectly left open after Supabase had already been selected and proven in the separate Field Photo Prep Team track.

## Backend selection

**Supabase is the approved FPP identity/account backend.**

Use the existing Supabase project:

- project name: `Field Photo Prep Team`
- project ref: `vyocaujuwrivoqynvitm`
- region: `us-east-2`

Do not create another Supabase project merely for normal FPP identity.

The existing project already contains Team operational data. Normal FPP must reuse only the shared Auth/business identity foundation and must not read or write Team work-order/photo operational tables.

## Shared versus separate data

Reuse:
- Supabase Auth `auth.users` as permanent FPP User identity;
- the existing `public.organizations` business identity for **In And Out Cleaner Inspections LLC**;
- the existing Supabase project/publishable-client model;
- trusted server-side functions/Edge Functions for privileged account actions.

Normal FPP adds only its own identity/account tables, namespaced so they cannot be confused with Team authorization:

- `public.fpp_memberships`
- `public.fpp_invitations`

Normal FPP must not use Team `work_orders`, `photos`, assignment, contractor-seat, or Team lifecycle tables.

## User identity

For FPP v1:

`user_id = auth.users.id`

No duplicate public Users table is required merely to mirror Supabase Auth.

Email is login/profile data, not permanent identity.

Any user-visible display name may remain profile metadata and must not be used for authorization.

## Membership model

`fpp_memberships` represents User ↔ Organization authorization.

Required columns:

- `id uuid primary key default gen_random_uuid()`
- `organization_id uuid not null references public.organizations(id) on delete restrict`
- `user_id uuid not null references auth.users(id) on delete restrict`
- `role text not null check (role in ('OWNER','MEMBER'))`
- `status text not null check (status in ('INVITED','ACTIVE','REVOKED'))`
- `created_at timestamptz not null default now()`
- `updated_at timestamptz not null default now()`
- unique `(organization_id, user_id)`

Authorization is based on this table, not mutable email and not Team's `app_metadata.role`.

A person may later have Membership rows in more than one Organization. The Android app still permits only one active Organization locally at a time under Identity Model v1.

## Invitation model

`fpp_invitations` is FPP-specific and does not reuse Team Contractor invitations.

Required identity/state:

- `id uuid primary key default gen_random_uuid()`
- `organization_id`
- normalized invited email
- intended FPP role
- invitation status
- nullable linked `auth_user_id`
- `created_by`
- timestamps

Initial invitation states may include:

`RESERVED → SENT → ACCEPTED`

and fail-closed outcomes:

`CANCELLED`, `FAILED`, `PROBLEM`.

A trusted FPP invitation function may:
- reuse an existing exact Supabase Auth user when appropriate;
- create/send a Supabase Auth invitation for a new user;
- link only the exact resulting Auth user to the exact invitation;
- create/activate the FPP Membership only after the invitation/account identity is proven.

The Android client never receives a Supabase service-role/secret key.

## Initial login method

FPP v1 uses **Supabase Auth email + password**.

Reasons:
- it is already proven in the Team Android app;
- it requires no new Google OAuth/brand setup;
- it keeps FPP sign-in visibly separate from Google Drive account/provider choice;
- it can be implemented in the existing Java app without adopting a Kotlin/Supabase SDK stack;
- invitation/password recovery can stay in Supabase Auth.

Google Sign in / Android Credential Manager is explicitly **not required for v1**. It may be added later under a separate recorded design without changing permanent User identity.

## Android client implementation boundary

Keep the current Java/Android architecture.

Use a narrow Java auth client over HTTPS, following the already-proven Team approach, rather than adding a broad backend framework.

Expected responsibilities:
- sign in with email/password;
- refresh Supabase session;
- fetch the current user's FPP Membership rows;
- call narrowly approved FPP account RPC/Edge Function actions;
- sign out.

No service-role key, Google Drive OAuth token, or Team operational API is placed in normal FPP.

The Supabase publishable key is a public client credential and may be supplied to the app build. Secret/service-role credentials remain server-side only.

## Session model

Persist only the session material required for Supabase Auth:

- access token;
- refresh token;
- access-token expiry;
- Supabase Auth user UUID;
- cached active Organization/Membership identity;
- `last_membership_validated_at`.

Token material must be encrypted at rest using an Android Keystore-backed app-local mechanism.

Session/auth state remains excluded from Android backup/device transfer by the existing Phase 10F backup rules.

Do not log/export tokens.

## Online startup

When network is available:

1. restore encrypted local Supabase session if present;
2. refresh an expired/nearly-expired access token;
3. validate the current Auth user;
4. fetch authoritative FPP Membership rows;
5. confirm the locally active Organization still has an `ACTIVE` Membership;
6. update `last_membership_validated_at`;
7. continue to the existing Drive/company workflow.

An Auth account with no active FPP Membership cannot enter normal FPP Organization work.

## Offline startup

Normal FPP may continue field work offline only when:

- a prior Supabase session exists locally;
- the same User and active Organization were previously validated;
- the cached Membership was `ACTIVE`;
- the most recent successful Membership validation is no more than **7 days old**.

While operating from cached offline identity:
- the active Organization cannot be switched;
- membership/invitation administration is unavailable;
- protected photo capture/preparation remains available;
- existing Drive/photo destination rules remain unchanged;
- no local state may be represented as newly accepted by Supabase.

If the 7-day window is exceeded while the account service is unavailable:
- preserve all protected/local work;
- allow recovery/viewing of existing protected work;
- require successful identity revalidation before starting new Organization work/capture.

The 7-day value is a v1 safety/usability limit and may be changed only from field evidence through a recorded design change.

## Invalid or revoked session

A network error is not the same as revoked authorization.

If refresh/validation fails only because the network/backend is unavailable, apply the offline rule above.

If Supabase authoritatively reports the session/account invalid, or the FPP Membership is `REVOKED`:
- stop new ordinary Organization work;
- preserve protected originals and queue/reconciliation evidence;
- do not rewrite Drive destinations;
- do not silently upload unresolved work under another identity;
- route the app into explicit sign-in/recovery status.

## Sign-out

Normal sign-out is allowed only when the existing protected-work guard says no unresolved local work would be stranded.

When sign-out is allowed:
- clear local Supabase token/session material;
- clear active User/Organization navigation state;
- retain the locally persisted Drive binding only as an Organization-bound binding that cannot be used by another Organization without validation;
- never delete Drive data or protected photo history merely because the user signs out.

If online, request Supabase session sign-out/revocation before clearing local session state. A failure to reach Supabase must not cause protected data deletion.

## Account recovery

Use Supabase Auth's normal password-recovery flow.

Owners do not choose, see, or receive Member passwords.

No custom plaintext-password reset system is permitted.

## Authorization / RLS

Every FPP identity table exposed through the Data API must have RLS enabled.

Normal client authorization must be based on:
- `auth.uid()`;
- exact `fpp_memberships` rows;
- exact Organization identity;
- required Membership role/status.

Do not authorize FPP from `user_metadata`.

Do not rely on Team's single-role `app_metadata.role` / `organization_id` claims for FPP Membership authorization.

Owner-only privileged operations should use narrowly scoped private functions or JWT-protected Edge Functions with explicit caller/membership checks. Any `SECURITY DEFINER` function must stay outside the exposed public API implementation surface, explicitly validate `auth.uid()`, and have default PUBLIC execution revoked.

## Existing Organization bootstrap

The current In And Out Cleaner Inspections LLC Organization row is reused.

The initial FPP Owner Membership is created through one controlled bootstrap/migration after the intended Supabase Auth user is identified.

Do not create a second Organization row merely because normal FPP is a different Android app.

## Drive boundary

Nothing in this Phase changes Drive authorization.

Supabase sign-in:
- identifies the FPP User;
- authorizes Organization Membership.

Android SAF:
- chooses/authorizes the actual Drive provider/account/workspace.

Supabase never selects or rewrites the SAF tree.

The current personal Drive may remain production. A later Shared Drive migration changes only the Organization-bound local Drive binding/provider identities.

## Explicit exclusions

Phase 12B does not authorize:
- a second Supabase project;
- Google Sign in / Credential Manager;
- Team work-order/photo APIs in normal FPP;
- Room/SQLite merely for auth;
- WorkManager merely for auth;
- syncing customer addresses/jobs/photos into Supabase;
- Supabase Storage for FPP photos;
- Drive OAuth inside Supabase;
- service-role credentials in Android;
- multi-Organization simultaneous operation;
- subscription/licensing.

## Next implementation slice

After this architecture is merged, implementation order is:

1. Supabase schema/RLS for FPP memberships + invitations and initial Owner bootstrap;
2. Java Supabase auth/session client with secure local token storage;
3. Sign-in/startup gate using authoritative/cached Membership state;
4. sign-out/recovery behavior;
5. invitation/member administration;
6. account/Drive mismatch and diagnostics work.

Each Level 3 schema/account slice keeps its own impact/test evidence and pre-merge approval gate.
