# Phase 12L Production Invitation Redirect Defect

Date: 2026-09-28

Status: **IN PROGRESS — LEVEL 3 EXTERNAL AUTH CONFIGURATION DEFECT**

## Problem

During Phase 12L Gate D, a real production MEMBER invitation was created successfully and displayed as `PENDING / delivery SENT`, but opening the received invitation email on Android landed at `http://localhost:3000` with `ERR_CONNECTION_REFUSED` instead of opening the production Field Photo Prep callback.

## Governed base / rollback

- base `main`: `c7970238e00139415a417f669d86915c4260154e`;
- authoritative defect branch: `fix/phase-12l-production-invite-redirect`;
- Phase 12L evidence remains on PR #85 and is paused at Gate D;
- unrelated PR #39 remains outside this scope.

## Classification

**Level 3 — production authentication/deployment configuration.**

The Android runtime, Drive/provider semantics, queue/photo behavior, signing identity, Supabase schema/RLS, and Edge Function authorization behavior are not being redesigned.

Explicit operator approval remains required before this defect PR merges.

## Verified evidence

1. Deployed `fpp-owner-invite` is ACTIVE and still builds:
   `com.inandout.fieldphotoprep://auth-callback?fpp_invitation_id=<uuid>`.
2. The deployed function's allowed redirect bases include:
   - `com.inandout.fieldphotoprep.internal://auth-callback`;
   - `com.inandout.fieldphotoprep://auth-callback`.
3. Supabase Auth live log for the failed invitation click shows:
   `type=invite&redirect_to=http%3A%2F%2Flocalhost%3A3000`.
4. Current Supabase Auth documentation states that a supplied `redirectTo` must match the hosted Redirect URL allowlist; otherwise the project Site URL is used.
5. Phase 12K production recovery passed because it used the exact base callback with no FPP invitation query parameter.
6. Historical Phase 12F internal invitation testing required the query-capable internal allowlist entry:
   `com.inandout.fieldphotoprep.internal://auth-callback*`.

## Root cause

The production Auth Redirect URL allowlist covers the base production callback but does not currently match the invitation callback once `?fpp_invitation_id=...` is appended. Supabase therefore substitutes the project Site URL, currently `http://localhost:3000`.

## Approved fix shape

Add this non-secret production Redirect URL allowlist entry in the dedicated FPP Supabase project:

`com.inandout.fieldphotoprep://auth-callback*`

Retain the exact base callback entry:

`com.inandout.fieldphotoprep://auth-callback`

Do not change:
- Android redirect parsing;
- package/application IDs;
- production signer;
- Edge Function invitation ID propagation;
- Site URL merely to mask an allowlist mismatch;
- internal callback entries;
- Membership/Invitation schema or RLS.

## External-state parity

The hosted Auth Redirect URL allowlist is not represented by a database migration and is not readable/writable through the currently connected Supabase project tools. The exact non-secret required entry is therefore recorded in this governed defect record and release runbook.

After the operator changes the hosted configuration, live physical verification must prove parity.

## Verification

Focused reality gate after the allowlist update:

1. keep the existing disposable MEMBER invitation or use its safe resend path;
2. resend the same pending invitation rather than create a duplicate;
3. open the new invitation email on the production Android package;
4. confirm it opens `com.inandout.fieldphotoprep://auth-callback` in Field Photo Prep, not localhost;
5. complete invited-user password/account activation;
6. confirm exact Organization + MEMBER role;
7. confirm Drive remains unbound / not inherited for the invited identity on the fresh-state test surface;
8. inspect live Auth logs to confirm the redirect no longer falls back to localhost;
9. resume Phase 12L Gate D only after this focused defect verification passes.

## Failure recovery / rollback

If the new allowlist entry does not fix the invite link:
- stop Gate D;
- do not broaden Android redirect handling;
- do not change the Site URL as a workaround;
- remove only the newly added production wildcard entry if rollback is required;
- preserve the pending invitation and logs for diagnosis.

## Merge approval

Level 3 pre-merge operator approval: **PENDING**.
