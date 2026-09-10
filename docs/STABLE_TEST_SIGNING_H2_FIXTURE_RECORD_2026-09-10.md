# Stable Test Signing H2 Fixture Record — 2026-09-10

Status: **AUTHORIZED TEST FIXTURE BRANCH**

Purpose: rebuild the completed H2 development-line app with the same stable non-production test signer introduced by `docs/STABLE_TEST_SIGNING_IMPACT_RECORD_2026-09-10.md`, so the physical Android gate can prove an update-in-place from H2 behavior to Phase 7B while preserving app-private test state.

Exact fixture base:

`d1e2e2c4ea60e0ebad57b097d29abc555dd2c4f3`

Exact H2 runtime beneath that documentation head:

`8d23b061307725589aef69a31a00249744523406`

The fixture branch may change only debug/test signing configuration, CI signer verification, and the checked-in non-production test signing asset/documentation. It must not alter H2 Java runtime behavior, queue/schema state, Drive logic, app ID, versionCode 14, or versionName.

Expected stable signer certificate SHA-256:

`2C:0A:96:16:FD:81:93:33:ED:98:B3:35:93:FB:E5:97:E1:20:E9:59:36:10:3C:6B:7A:76:27:0E:98:5D:3F:BA`

This branch is a disposable compatibility-test fixture and is not a merge target for product runtime behavior.
