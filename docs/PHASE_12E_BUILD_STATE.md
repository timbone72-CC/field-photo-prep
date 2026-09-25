# Phase 12E — Runtime Authorization Enforcement Build State

Last updated: 2026-09-25

This is the durable handoff point. Use the current branch and CI results rather than repeating the historical audit.

## Governed branch

- Base: main `0e1104db2c2d6026a427bfa7b5baa9213b2fac25`.
- Branch: `phase-12e/runtime-authorization-enforcement`.
- Draft PR: #72. Do not merge or modify main.
- Exact automated-tested runtime head: `ff8403edec07e4b60bb23e17eccd3af08c66ad2c`.
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
- Head `ff8403e`: protected-photo location badges on Home property rows and exact work-order rows; Android CI run 887 (`36176813606`) PASS, including unit tests, debug build, signer and emulator instrumentation.
- Samsung device check on 2026-09-25: PASS — protected-photo counts appeared beside the correct property/work order, and Sign Out followed by Sign In completed successfully.
- Local workspace lacks Gradle/Android SDK; use exact-head GitHub CI evidence. Do not report a local focused run that was not performed.

## Current lower-level bypass inventory

Direct provider mutations in production are `DriveClient.createFolder`, `renameFolder`, `deleteDocument`, and `DrivePhotoUploader.create`; each has a required guard. `PhotoUploadCoordinator.upload` checks before queue transition. Production constructor call sites inject the central guard. Capture reservations originate in `PhotoCaptureActivity.beginCameraCapture` and `CameraCaptureActivity.reserveCaptureIfNeeded`; both are guarded before a new reservation, and each shutter rechecks. `DrivePhotoReconciler` queries provider state and does not mutate it. Existing focused tests deny Drive folder mutations before provider calls, photo creation before provider calls, and upload before queue state changes; the guard test observes authority loss before a second camera shutter and next Drive attempt. These claims are source-audit and fake-provider/JVM evidence, not physical device proof.

## Exact next action

1. Complete any remaining proportional Samsung checks in `docs/PHASE_12E_COMPLETION_2026-09-25.md`; the protected-photo location UI and Sign Out → Sign In round trip are already device-PASS.
2. Record any remaining device PASS/BLOCKED/FAIL result and fix only an actual defect on this branch.
3. After the remaining physical gate passes, refresh PR #72 changed files and exact evidence, then stop before merge for the operator's Level 3 approval. No branch result or CI result authorizes merging by itself.

When a gate fails, stop that affected path and preserve photos, queue state, and the prior working APK. Use a bounded fix on this branch; do not expand Phase 12 scope.
