# Phase 12B Dedicated Supabase Auth — Level 3 Impact Record

Date: 2026-09-24

Status: **DESIGN RECORDED — IMPLEMENTATION NOT STARTED — PRE-MERGE APPROVAL PENDING**

## Problem

Original FPP now has an approved User/Organization/Membership identity model, but no selected authentication/backend implementation.

The separate Team app already uses Supabase, but Team governance requires a hard separation. Original FPP needs its own backend if Supabase is selected.

## Approved design scope

Design only:

- Supabase is selected for original FPP;
- create a **separate dedicated Supabase project** for original FPP;
- use Supabase Auth email/password;
- invitation-only v1 after first Owner bootstrap;
- use Auth UUID as FPP User identity;
- use separate FPP Organization/Membership/Invitation tables;
- RLS + explicit Data API grants;
- privileged account writes only through trusted server-side actions;
- encrypted Android session state;
- 72-hour same-Organization offline grace;
- preserve complete separation from Android SAF/Drive authorization;
- preserve complete separation from Team.

No runtime or Supabase schema is changed by this design PR.

## Owning files for this design

- `docs/PHASE_12B_SUPABASE_AUTH_ARCHITECTURE_2026-09-24.md`
- `docs/PHASE_12B_SUPABASE_AUTH_IMPACT_2026-09-24.md`
- `docs/IDENTITY_MODEL_V1.md`
- `CONTRACT.md`
- `docs/ROADMAP.md`

## Read surfaces

- Phase 12A Identity Model v1;
- current original-FPP Java/Android architecture;
- current Android backup exclusions;
- current SAF/Drive identity contracts;
- Team separation rules as a negative boundary only;
- current Supabase Auth/RLS/session/email guidance.

## Write surfaces

This design PR writes repository documentation only.

Future implementation will write:
- one new dedicated Supabase project;
- original-FPP Auth users;
- FPP Organizations/Memberships/Invitations;
- encrypted local FPP auth/session state.

It will not write Team data or FPP job/photo data into Supabase.

## Required data

Backend:
- Auth User UUID;
- Organization UUID/name/status;
- Membership UUID/User/Organization/role/status;
- Invitation UUID/org/email/role/status/expiry;
- timestamps.

Android:
- access/refresh session;
- expiry;
- User UUID;
- active Organization/Membership;
- last Membership validation time.

## Optional data

Not required initially:
- profile display name;
- avatar;
- phone number;
- Organization branding/preferences.

## Schema / identity / permission changes

Future Phase 12C will introduce a new backend identity system for original FPP.

It will not change Drive provider identity.

Every exposed table requires:
- explicit Data API privileges;
- RLS;
- least-privilege policies.

Service-role/secret credentials remain server-side only.

## Destination assumptions

Supabase is not a photo destination.

Google Drive remains source of truth for company/property/work-order/photo content.

Queued work-order provider IDs remain immutable.

Supabase outage or auth failure must never cause destination substitution.

## Duplicate / idempotency behavior

- one Auth UUID = one FPP User;
- one Organization/User Membership maximum;
- repeated invitation cannot create duplicate Membership;
- repeated acceptance is idempotent;
- first Owner bootstrap creates one Organization/Owner relationship only;
- visible email/name never substitutes for UUID identity.

## Offline / stale behavior

- online when possible: refresh session + revalidate Membership;
- cached `ACTIVE` Membership supports same-Organization field work for 72 hours;
- no Organization switching or membership administration offline;
- after 72 hours without revalidation, preserve existing work but block new capture/remote writes;
- authoritative `REVOKED` state stops new work when learned.

## Safe fixture plan

Phase 12C+ tests use:
- dedicated original-FPP Supabase project;
- disposable FPP test Auth users/Organization where possible;
- existing disposable FPP Drive fixture when Drive interaction is required.

Do not use Team Supabase as a fixture.

Do not use live customer folders for auth testing.

## Verification expected later

Schema/backend:
- RLS allow/deny tests;
- wrong-user/wrong-Organization denial;
- Owner vs Member authorization;
- invite idempotency/cancel/expiry;
- revoke/reactivate;
- no duplicate Membership.

Android:
- login;
- token restore/refresh;
- encrypted credential storage;
- restart;
- 72-hour offline boundary;
- revocation;
- protected-work sign-out guard;
- new-device no-session behavior;
- full existing Android regression suite.

## Failure recovery

Design-only rollback: revert the documentation branch.

Future runtime/schema slices require independent rollback.

Identity/auth failures must never delete protected originals or redirect Drive destinations.

## Primary risks

- accidentally sharing Team backend identity/data;
- treating email as User identity;
- exposing secret/service-role credentials;
- weak RLS;
- stale authorization continuing too long offline;
- sign-out/revocation stranding protected photos;
- backend scope expanding into job/photo storage.

## Explicit pre-merge approval

**APPROVED by operator on 2026-09-24.**

The operator explicitly approved this exact Phase 12B dedicated-Supabase architecture before merge.
