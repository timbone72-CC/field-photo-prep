# Phase 12D Android Auth / Session Foundation — Level 3 Impact Record

Date: 2026-09-24

Status: **DEVICE GATE PASSED — FINAL LEVEL 3 MERGE APPROVAL PENDING**

## Classification

Level 3:
- introduces Android authentication/session state;
- introduces credential storage;
- adds an exported auth deep-link activity;
- connects Android runtime to the dedicated identity backend.

## Reads

Android:
- auth credential fields entered by operator;
- incoming exact auth callback URI;
- encrypted app-private auth state;
- RLS-protected own Membership and Organization.

Supabase:
- Auth session/User;
- own FPP Membership;
- exact Organization.

## Writes

Android:
- Keystore alias;
- encrypted app-private auth session payload.

Supabase Auth:
- normal sign-in session refresh;
- password recovery email request;
- password update during recovery;
- one fresh normal email/password sign-in immediately after a successful recovery password change so the stored post-recovery session is not dependent on a security-sensitive recovery session remaining valid.

No Phase 12D write to:
- FPP Organization/Membership tables;
- Drive;
- photos;
- Team.

## Exported surface

New exported AuthActivity accepts only:
- action VIEW;
- BROWSABLE/DEFAULT;
- exact build-specific auth scheme;
- exact host `auth-callback`.

Runtime revalidates the URI before consuming tokens.

## Secrets

Allowed in Android:
- Supabase URL;
- Supabase publishable key.

Forbidden:
- secret key;
- service-role key;
- DB password;
- Team credentials.

## Failure behavior

Network failure:
- no destructive action;
- existing Drive/photo workflow remains available in 12D;
- if token refresh itself fails, the prior stored session remains unchanged;
- if token refresh succeeds, the newly rotated token pair is persisted before Membership/Organization revalidation so a later transient API failure cannot strand the device on the consumed old refresh token;
- the previous authoritative Membership validation timestamp is preserved until Membership revalidation succeeds.

Malformed deep link:
- reject;
- do not persist tokens.

Membership missing/revoked:
- do not persist a new active Organization snapshot.

Keystore decrypt failure:
- clear unreadable auth session only;
- preserve Drive/photo state.

## Backup

Phase 10F already excludes all SharedPreferences and app-private state from cloud backup and device transfer.

Add a focused regression test proving the auth preference file remains covered by those global exclusions.

## Final-review sequencing corrections

The 2026-09-25 final review found two narrow session-sequencing issues inside the already approved Phase 12D scope:

1. successful rotating-token refresh must persist the returned token pair before later Membership/Organization calls;
2. successful recovery password change must be followed by a fresh normal password sign-in before the final session is stored.

No schema, RLS, Drive, SAF, photo, queue, upload, organization-membership, or invitation behavior changes.

## Rollback

Pre-hardening runtime rollback point:
`79fa50467265e1ba2c6837a793d2b7d3c7470a10`

If either hardening correction fails focused or final verification before merge:
- stop publication/merge;
- restore the Phase 12D branch runtime files to `79fa50467265e1ba2c6837a793d2b7d3c7470a10`;
- retain the captured device/Supabase evidence;
- do not retry the recovery email merely to work around a code failure.

Whole-Phase-12D pre-merge rollback remains closing PR #67 and leaving `main` at its Phase 12C base `1ca2f2fe2aa92374a7bbd4185e592bfd8256090c`. No production deployment or database migration is part of 12D, so rollback requires no Drive mutation or Supabase schema rollback.

## Regression boundary

Must not alter:
- Drive tree/company/property/work-order provider identity;
- camera;
- photo queue;
- upload/reconcile/cleanup;
- Company switching;
- existing protected-work semantics.

## Supabase configuration dependency

Hosted Auth configuration must allow the internal callback URI:

`com.inandout.fieldphotoprep.internal://auth-callback`

and may also allow:

`com.inandout.fieldphotoprep://auth-callback`

FPP runtime recovery calls always send an explicit redirect URI and do not depend on the default `localhost:3000` Site URL.

## Testing

Automated:
- redirect parser exact-match tests;
- malformed/error callback tests;
- session serialization tests;
- 72-hour policy data retained for future gate;
- instrumentation Keystore encrypt/decrypt/clear;
- manifest deep-link structure;
- backup exclusion;
- existing full Android suite.

Phone:
- already proven and retained: normal sign-in, real ACTIVE OWNER validation, Recheck Account, restart session restore, existing Drive/workspace smoke;
- remaining after the hardening APK: fresh password recovery email → internal app → password change → fresh normal sign-in → ACTIVE OWNER validation → restart/recheck.

## Approval

Implementation/testing may continue on this branch.

Final Level 3 merge requires explicit operator approval after exact runtime + phone evidence.


## Final automated CI evidence

Exact final runtime head:
`df02a34981e601d3009875edc8fe154e2a23c936`

Android CI:
- run: `36122577021`
- run number: `822`
- conclusion: **SUCCESS**
- focused rotating-token state coverage: PASS inside unit-test step;
- full unit suite: PASS;
- internal debug build: PASS;
- stable APK signer verification: PASS;
- connected/instrumented tests and internal launch smoke: PASS.

Final internal APK:
- artifact ID: `10857473938`
- digest: `sha256:53f49a0187bcea1abe70f1dbda6ffe289ef3632bcd1aa815d977ee7d90d63ee5`
- versionCode: `36`
- versionName: `0.27.1-auth-session-hardening-internal`

The final runtime diff is limited to:
- token-rotation persistence sequencing;
- post-recovery fresh sign-in sequencing;
- one focused token-rotation identity-snapshot test;
- version bump for unambiguous device installation.

No Drive, SAF, photo, queue, upload, reconciliation, cleanup, database schema, RLS, or Team runtime surface changed.

## Final device-gate evidence

The exact final hardening APK was installed on Samsung SM-G996U / Android 15 and the recovery path passed:

- fresh recovery email → **Field Photo Prep Internal** deep link: PASS;
- password update: PASS;
- fresh normal password sign-in after update: PASS;
- ACTIVE OWNER Membership + exact Organization validation: PASS;
- close/reopen encrypted session restore: PASS;
- Recheck Account refresh + Membership/Organization validation: PASS;
- prior existing Drive/workspace smoke remains valid and was not repeated because the hardening changed no Drive/SAF runtime surface.

Live Supabase evidence confirmed the final recheck with:
- refresh-token exchange: HTTP 200;
- ACTIVE Membership read: HTTP 200;
- exact Organization read: HTTP 200.

**Device gate classification: PASS.**

The only remaining Level 3 completion requirement is explicit operator pre-merge approval for PR #67.
