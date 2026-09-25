# Phase 12D — Android Auth / Session Foundation

Date: 2026-09-24

Status: **FINAL HARDENING IN PROGRESS — RECOVERY DEVICE GATE PENDING**

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
8. because a password change is security-sensitive and may terminate the recovery session, app performs a normal email/password sign-in using the newly chosen password;
9. app validates the fresh signed-in User + ACTIVE Membership + Organization;
10. app stores only that fresh normal session/identity securely.

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
- persist the complete new access + refresh pair atomically **before** any later Membership/Organization request can fail;
- preserve the previous identity snapshot and its last-successful Membership validation timestamp until authoritative Membership revalidation succeeds;
- after revalidation succeeds, atomically replace the stored snapshot with the newly validated identity + refreshed token pair.

Do not allow multiple parallel refresh attempts using the same stored refresh token. A transient Membership/API failure after a successful token rotation must not strand the app with the already-consumed old refresh token.

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

## 2026-09-25 final-review hardening

Final review against the current Supabase session documentation and the Phase 12D contract found two narrow sequencing corrections before the last phone recovery gate:

1. **Rotating refresh-token persistence:** Supabase refresh tokens are one-use/rotating. The refreshed access + refresh pair must be persisted immediately after a successful refresh, before Membership/Organization revalidation can fail. The prior validated identity snapshot remains unchanged until revalidation succeeds.
2. **Fresh session after password recovery:** Supabase documents password change as a security-sensitive action that can terminate a session. After the recovery session successfully changes the password, FPP must establish a fresh normal email/password session with the new password, validate ACTIVE Membership + Organization, then persist that fresh session.

These corrections do not change Drive/SAF behavior, photo state, destination identity, public signup policy, or the Phase 12D rollout boundary.

Already accepted physical evidence is retained and must not be repeated:
- internal APK installed successfully on the Samsung field phone;
- normal sign-in succeeded;
- ACTIVE OWNER identity loaded;
- Recheck Account succeeded against live Supabase;
- encrypted stored session survived app close/reopen;
- existing company/workspace + Drive-backed navigation remained usable.

The only remaining physical auth observation after the hardening build is:
**fresh recovery email → newest link on Samsung → Field Photo Prep Internal → set password → fresh sign-in/ACTIVE OWNER → restart/recheck.**

## Completion gate

12D completes only when:
- exact runtime diff reviewed;
- unit/instrumentation tests pass;
- full Android CI passes;
- Supabase internal redirect allowlist is configured;
- physical recovery/sign-in/restart phone gate passes;
- existing field workflow smoke passes;
- final Level 3 operator merge approval is recorded.


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


## Password minimum

The Android password-setup screen requires **8 characters minimum**.

This matches Supabase's current recommendation that passwords shorter than 8 characters are not recommended, while avoiding an unnecessary 12-character local requirement.

FPP does not impose extra composition rules in 12D beyond the hosted Supabase project policy.
