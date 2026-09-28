# Phase 12L Production Invite Redirect Defect — Build State

Date: 2026-09-28

Status: **IN PROGRESS — CONFIGURATION REALITY GATE**

## Authoritative line

- branch: `fix/phase-12l-production-invite-redirect`;
- base/rollback: `c7970238e00139415a417f669d86915c4260154e`;
- Phase 12L PR #85: paused at Gate D;
- implementation ownership: hosted Supabase Auth Redirect URL allowlist + source-of-truth documentation only.

## Current live state

- `fpp-owner-invite`: ACTIVE;
- invitation creation/delivery: PASS;
- production invite click: FAIL — localhost fallback;
- production recovery callback: previously PASS;
- required production query-capable allowlist entry: `com.inandout.fieldphotoprep://auth-callback*`;
- hosted allowlist mutation: **PENDING OPERATOR ACTION**.

## Exact next checkpoint

Operator adds the required production wildcard callback in Supabase Auth URL Configuration without removing the exact base callback.

Then use **RESEND** on the same pending disposable MEMBER invitation and repeat only the failed invitation-click path.

Do not create a second invitation and do not modify Android runtime code unless this focused configuration fix fails.
