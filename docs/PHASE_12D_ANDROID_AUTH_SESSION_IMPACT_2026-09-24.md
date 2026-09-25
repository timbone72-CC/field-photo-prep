# Phase 12D Android Auth / Session Foundation — Level 3 Impact Record

Date: 2026-09-24

Status: **IMPLEMENTATION IN PROGRESS — PRE-MERGE APPROVAL NOT YET DUE**

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
