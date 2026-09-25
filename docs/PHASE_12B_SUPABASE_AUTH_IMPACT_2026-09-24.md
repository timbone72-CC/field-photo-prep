# Phase 12B Supabase Authentication Architecture — Level 3 Impact Record

Date: 2026-09-24

Status: **DESIGN RECORDED — IMPLEMENTATION NOT STARTED — PRE-MERGE APPROVAL PENDING**

## Problem

Phase 12A recorded the identity model but incorrectly left the authentication/backend vendor open even though the earlier, separate Field Photo Prep Team work had already selected and proven Supabase.

Continuing to ask the operator to select a backend would create planning drift and duplicate work.

## Approved design

Reconcile the existing decision into normal FPP:

- Supabase is the FPP identity/account backend.
- Reuse existing project `Field Photo Prep Team` (`vyocaujuwrivoqynvitm`).
- Reuse Supabase Auth users and preserve the already-established business Organization UUID. The missing Organization row will be restored with that exact UUID in Phase 12C.
- Add FPP-specific Membership/Invitation tables only.
- Do not use Team work-order/photo operational data in normal FPP.
- Initial FPP login is Supabase email/password.
- Keep Google Drive authorization entirely in Android SAF.
- Cache validated identity for offline field work for up to 7 days.
- Secure local token material with Android Keystore-backed encryption.
- Keep the current Java architecture; no Kotlin/Supabase SDK migration is required.

## Owning files for this design record

- `docs/PHASE_12B_SUPABASE_AUTH_ARCHITECTURE_2026-09-24.md`
- this impact record
- `docs/IDENTITY_MODEL_V1.md`
- `CONTRACT.md`
- `docs/ROADMAP.md`

No runtime source or Supabase schema mutation is part of this design-only PR.

## Existing evidence

The separate `timbone72-CC/field-photo-prep-team` repo already proves:
- Supabase project/config exists;
- Supabase Auth email/password sign-in exists in Android Java;
- publishable-key client access exists;
- organization/role authorization exists;
- RLS exists;
- trusted invitation Edge Function exists;
- service-role credentials are kept server-side.

The existing Supabase project was restored from INACTIVE on 2026-09-24 and inspected.

Verified live state:
- all existing public Team operational tables are empty;
- two Supabase Auth users exist;
- both reference Organization UUID `494154a6-2a7a-4c98-a0a8-2143443fec0e`;
- the corresponding `public.organizations` row is missing.

Database writes were not performed as part of this design record.

## Required data

Future normal FPP identity implementation requires:
- `auth.users.id` as User identity;
- Organization UUID `494154a6-2a7a-4c98-a0a8-2143443fec0e`, restored into `public.organizations` during the controlled Phase 12C bootstrap;
- FPP Membership UUID, role, status;
- FPP Invitation UUID/status;
- local encrypted Supabase access/refresh session;
- cached active Organization/Membership + last successful validation time.

## Permission and schema impact

Future implementation adds FPP-specific identity tables/RLS to the existing Supabase project.

It must not broaden normal FPP access to Team operational tables.

FPP Member/Owner authorization comes from FPP Membership rows, not Team `app_metadata.role`.

## Offline/stale behavior

Cached active Membership may authorize the same Organization's normal offline field workflow for 7 days from last successful server validation.

After that window, existing protected work remains preserved, but new Organization work waits for successful revalidation.

Known server revocation overrides cached active state once learned.

## Duplicate/idempotency behavior

- one `auth.users.id` is one permanent FPP User identity;
- one Membership per `(organization_id,user_id)`;
- repeated invitation acceptance must not create duplicate Membership;
- existing exact Supabase Auth user may be linked to an FPP invitation rather than duplicated;
- the already-established In And Out Organization UUID is preserved; the missing row is restored with that exact UUID rather than generating another Organization identity.

## Security boundaries

- no service-role/secret key in Android;
- no authorization from user-editable metadata;
- RLS on exposed FPP tables;
- privileged invitation/bootstrap operations server-side;
- Android token material encrypted with Keystore-backed keys;
- auth/session data remains excluded from Android backup/transfer;
- Supabase login never authorizes or selects Drive.

## Baseline verification

Design-only PR:
- mandatory governance/identity/integration reread complete;
- prior Team Supabase implementation inspected;
- current Supabase project existence confirmed;
- current Supabase Auth/Google/Android documentation checked where relevant;
- documentation/contract diff review only; no Android runtime tests required.

Future implementation requires:
- schema/RLS tests;
- wrong-user/wrong-org negative tests;
- invitation idempotency tests;
- token restore/refresh/sign-out tests;
- offline 7-day boundary tests;
- revoked Membership tests;
- full Android CI on final runtime head;
- proportional Samsung sign-in/offline reality gate.

## Rollback

This PR changes only governance documentation. Revert the governance commit if the design is rejected before implementation.

Future schema/runtime slices must provide their own rollback.

## Explicit pre-merge approval

**PENDING.**

Level 3 requires explicit operator approval of this exact architecture before merge.
