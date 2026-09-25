# Phase 12D — Android Auth / Session Foundation

Date: 2026-09-24

Status: **IMPLEMENTATION IN PROGRESS — DEVICE GATE PENDING**

## Purpose

Connect the original FPP Android app to the dedicated Supabase identity backend without yet making authentication a hard startup gate.

This slice fixes the proven `localhost:3000` recovery/invite redirect problem and builds the smallest safe native-Java auth/session foundation.

## Scope boundary

Phase 12D includes:
- package-specific Android deep links;
- email/password sign-in;
- password-recovery request;
- recovery callback handling;
- password update after recovery;
- Supabase Auth user validation;
- exact FPP Membership + Organization validation;
- Android Keystore-backed encrypted session persistence;
- a small Account entry point from the existing Home overflow;
- tests and a physical Android recovery/sign-in gate.

Phase 12D does **not** yet:
- block Main/Home when signed out;
- enforce the 72-hour offline rule;
- enforce revocation against field actions;
- implement Owner member/invitation administration;
- implement sign-out while protected work exists;
- change Drive/SAF behavior.

Those enforcement paths begin only after the auth plumbing is proven on the phone.

## Dedicated backend

Project:
- **Field Photo Prep**
- ref: `vtyiktvqhbgabawotkrj`
- URL: `https://vtyiktvqhbgabawotkrj.supabase.co`

Android uses only the project's publishable key.

No secret/service-role key is present in Android or repository runtime source.

## Redirect design

Current production application ID:
`com.inandout.fieldphotoprep`

Current internal/debug application ID:
`com.inandout.fieldphotoprep.internal`

Use separate custom URI schemes:

- production: `com.inandout.fieldphotoprep://auth-callback`
- internal: `com.inandout.fieldphotoprep.internal://auth-callback`

This prevents the side-by-side production/internal apps from intentionally sharing the same auth callback scheme.

Supabase Additional Redirect URLs must contain both exact redirect URIs before the phone gate.

The existing `localhost:3000` Site URL must not be relied on by FPP runtime auth calls. Recovery/invitation calls must pass an explicit build-specific redirect URI.

### Why custom scheme now

The business does not currently own a domain for verified Android App Links.

Supabase documents native mobile custom schemes as a supported deep-link route. Android documents that custom schemes can be claimed by more than one app, while verified App Links add domain ownership verification.

Therefore:
- custom scheme is accepted for internal/current controlled FPP use;
- exact scheme + host validation is mandatory;
- a future externally distributed/public release should move auth callbacks to verified Android App Links when an owned HTTPS domain exists.

## Android callback surface

Add one dedicated exported `AuthActivity` with:
- normal explicit launch for Account/login/recovery;
- `VIEW + DEFAULT + BROWSABLE` intent filter;
- exact build-specific scheme;
- exact host `auth-callback`.

Do not add the auth deep-link filter to MainActivity.

AuthActivity must reject:
- wrong scheme;
- wrong host;
- missing/invalid token payload;
- unknown callback type;
- callback errors.

Callback tokens must never be logged or shown in diagnostics.

## Auth flow

### Sign in

1. user enters email + password;
2. POST to Supabase Auth password-token endpoint using publishable key;
3. validate returned Auth User UUID;
4. read RLS-protected ACTIVE FPP Membership;
5. read exact FPP Organization;
6. require exactly one usable ACTIVE Membership for this initial implementation;
7. persist encrypted session + identity snapshot;
8. return to existing Home.

Zero active Memberships fails closed.

Multiple active Memberships fail closed in 12D rather than guessing; a future Organization selector can be added when a real need exists.

### Password recovery

1. user chooses Forgot Password;
2. app calls Supabase password-recovery with the build-specific redirect URI;
3. email link resolves through Supabase and redirects to the installed FPP build;
4. AuthActivity validates the exact callback;
5. recovery access/refresh tokens establish a temporary Supabase session;
6. app asks for a new password;
7. app updates the password through Supabase Auth;
8. app validates User + ACTIVE Membership + Organization;
9. app stores the resulting session/identity securely.

This is also the correct path for the already-created first Owner if the Owner does not know the generated invite credential.

## Invitation callbacks

AuthActivity may safely recognize Supabase `invite` callbacks, but Phase 12D does not build Owner invitation administration.

An invite callback with no authoritative ACTIVE Membership does not grant FPP Organization access.

Invitation lifecycle activation remains server-controlled in the later member-administration slice.

## Narrow Java client

Keep original FPP Java-first.

Use `HttpURLConnection` + Android/Java JSON utilities.

Do not add:
- Kotlin;
- Compose;
- Room;
- Supabase Storage/Realtime SDKs;
- broad Supabase client framework.

Required client operations:
- sign in with password;
- refresh session;
- read current Auth user;
- request password recovery;
- update password from recovery session;
- read own Membership;
- read exact Organization.

## Session storage

Use Android Keystore directly with AES/GCM.

Store one encrypted session payload in app-private SharedPreferences.

Payload:
- access token;
- refresh token;
- access-token expiry;
- Auth User UUID;
- user email;
- active Organization UUID;
- Organization name;
- Membership UUID;
- role;
- Membership status;
- last successful Membership validation timestamp.

Key material never leaves Android Keystore.

If decrypt/authentication fails:
- fail closed;
- clear unreadable auth session state only;
- never touch protected photo/Drive state.

Existing Phase 10F backup/device-transfer rules already exclude all SharedPreferences and app state, so auth credentials do not migrate to another phone.

## Session refresh

Access tokens are short lived; Supabase refresh tokens are rotating.

Only one serialized auth/session owner may refresh at a time.

On successful refresh:
- persist the complete new access + refresh pair atomically.

Do not allow multiple parallel refresh attempts using the same stored refresh token.

## 12D rollout safety

12D does not become a mandatory startup gate.

The existing Drive/photo workflow remains available while we prove:
- Account screen opens;
- password recovery email opens the internal app;
- new password succeeds;
- sign in succeeds;
- real Owner Membership is validated;
- encrypted session survives restart;
- no auth state appears in Android backup/transfer surfaces.

Phase 12E will consume the proven session layer and add the actual startup/offline/revocation enforcement.

## Phone gate

On the Samsung field phone/internal app:

1. configure Supabase redirect allowlist for `com.inandout.fieldphotoprep.internal://auth-callback`;
2. install/update the internal APK;
3. open Account;
4. request password recovery for `inandoutinspections2026@gmail.com`;
5. tap the recovery email;
6. verify Android opens **Field Photo Prep Internal**, not localhost/browser dead-end;
7. set an operator-chosen password;
8. verify ACTIVE OWNER identity loads;
9. close/reopen app;
10. verify session/identity restore;
11. verify normal existing Drive workflow remains unchanged.

Production redirect URI may be allowlisted at the same time but is not physically validated until a production-signed build exists.

## Known limitation

Custom URI schemes are not domain-verified.

Before broad/public distribution, revisit verified Android App Links under the release-readiness phase if the business has an owned domain.

## Completion gate

12D completes only when:
- exact runtime diff reviewed;
- unit/instrumentation tests pass;
- full Android CI passes;
- Supabase internal redirect allowlist is configured;
- physical recovery/sign-in/restart phone gate passes;
- existing field workflow smoke passes;
- final Level 3 operator merge approval is recorded.
