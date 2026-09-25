# Phase 12E — Runtime Authorization Enforcement Build State

Last updated: 2026-09-25

This is the durable handoff point. Use the current branch and CI results rather than repeating the historical audit.

## Governed branch

- Base: main `0e1104db2c2d6026a427bfa7b5baa9213b2fac25`.
- Branch: `phase-12e/runtime-authorization-enforcement`.
- Draft PR: #72. Do not merge or modify main.
- Exact automated-tested runtime head: `87720f6f660051e38eeb762497320fcfd99730f6`. Documentation commits after it do not change runtime.
- Level 3: final operator approval is required before merge.

## Implemented on branch

- One immutable authorization decision and deterministic 72-hour grace policy with exact boundary and clock rollback tests.
- Membership validation, revocation persistence, session-generation protection, serialized refresh/revalidation, and rotated-token persistence before Membership lookup.
- Authenticated-session replacement routed through the manager; explicit Sign Out UI; protected-work guard from durable records and local copies.
- Conditional Sign Out: a delayed request cannot clear a newer or refreshed session. Tests cover sign-out during refresh, sign-out during Membership validation, replacement during validation, and stale Sign Out after replacement.
- Startup/foreground revalidation; new-capture and per-shutter gates; lower-level Drive create, rename, delete, and upload-attempt guards.
- Reconciliation remains a read-only provider operation. Existing in-flight uploads continue to classification; a later batch item is checked before starting.

The old remaining-work list naming session replacement and Sign Out UI was stale; code at `59b2984` already contained both. Do not reimplement them.

## Verification

- Head `59b2984`: Android CI run 873 (`36171576493`) PASS, including unit tests, debug APK, signer, and emulator instrumentation/launch.
- Head `25abb2b`: new sign-out/replacement race tests; CI run 874 (`36172253312`) PASS, including unit tests, debug build, signer and emulator instrumentation.
- Head `87720f6`: conditional Sign Out race fix; CI run 875 (`36172452699`) PASS, including unit tests, debug build, signer and emulator instrumentation. APK artifact ID `10880572226`; ZIP digest `sha256:b2a3498c8eff8fbf02eac739031d3cede68242944439d0cd82137c443ec6ea2c`.
- Local workspace lacks Gradle/Android SDK; use exact-head GitHub CI evidence. Do not report a local focused run that was not performed.

## Current lower-level bypass inventory

Direct provider mutations in production are `DriveClient.createFolder`, `renameFolder`, `deleteDocument`, and `DrivePhotoUploader.create`; each has a required guard. `PhotoUploadCoordinator.upload` checks before queue transition. Production constructor call sites inject the central guard. Capture reservations originate in `PhotoCaptureActivity.beginCameraCapture` and `CameraCaptureActivity.reserveCaptureIfNeeded`; both are guarded before a new reservation, and each shutter rechecks. `DrivePhotoReconciler` queries provider state and does not mutate it. Existing focused tests deny Drive folder mutations before provider calls, photo creation before provider calls, and upload before queue state changes; the guard test observes authority loss before a second camera shutter and next Drive attempt. These claims are source-audit and fake-provider/JVM evidence, not physical device proof.

## Exact next action

1. Run the proportional Samsung gate in `docs/PHASE_12E_COMPLETION_2026-09-25.md` using the exact CI 875 internal APK. Do not wait 72 real hours.
2. Record device PASS/BLOCKED/FAIL and any concrete finding; fix only an actual defect on this branch.
3. After the physical gate passes, refresh PR #72 changed files and exact evidence, then stop before merge for the operator's Level 3 approval. No branch result or CI result authorizes merging by itself.

When a gate fails, stop that affected path and preserve photos, queue state, and the prior working APK. Use a bounded fix on this branch; do not expand Phase 12 scope.
