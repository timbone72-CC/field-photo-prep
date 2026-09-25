# Phase 12D Android Auth / Session Foundation — Level 3 Impact Record

Date: 2026-09-24

Status: **AUTOMATED GATE PASSED — HOSTED REDIRECT + PHONE GATE PENDING**

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
- password update during recovery.

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
- stored good session remains intact unless Supabase proves it invalid.

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
- password recovery email → internal app;
- password change;
- real Owner Membership validation;
- restart session restore;
- existing Drive smoke.

## Approval

Implementation/testing may continue on this branch.

Final Level 3 merge requires explicit operator approval after exact runtime + phone evidence.


## Automated CI evidence

Android CI run: `36086075764`

- attempt 1:
  - unit tests: PASS
  - internal debug build: PASS
  - stable test signer verification: PASS
  - connected Android tests: PASS
  - job conclusion: FAILURE only because the emulator went offline during the final rendered-screen script after successful instrumentation
- attempt 2, same code/head:
  - **PASS**
  - run conclusion: **SUCCESS**

Exact tested branch head at the automated gate:
`59ed1759f6f38fed00fe7b6e348350ae87014ac3`

No runtime change was made to turn the first failed run green; rerunning the same head passed, confirming an emulator/ADB infrastructure flake rather than an app regression.

Internal test APK artifact:
`field-photo-prep-internal-apk`

Next gate:
1. allowlist the exact internal and production callback URIs in Supabase Auth;
2. install the internal APK on the Samsung field phone;
3. run recovery → app deep link → password update → ACTIVE OWNER validation → restart session restore;
4. smoke the existing Drive workflow.
