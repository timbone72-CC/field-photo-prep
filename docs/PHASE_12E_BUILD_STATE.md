# Phase 12E — Runtime Authorization Enforcement Build State

Last updated: 2026-09-25

This is the durable handoff point. Use the current branch and CI results rather than repeating the historical audit.

## Governed branch

- Base: main `0e1104db2c2d6026a427bfa7b5baa9213b2fac25`.
- Branch: `phase-12e/runtime-authorization-enforcement`.
- Draft PR: #72. Do not merge or modify main.
- Latest runtime head when this checkpoint was written: `87720f6f660051e38eeb762497320fcfd99730f6`.
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
- Head `25abb2b`: new sign-out/replacement race tests; CI run 874 (`36172253312`) was running when this checkpoint was prepared. Its unit-test step PASS; confirm final job result.
- Head `87720f6`: conditional Sign Out race fix; CI run 875 (`36172452699`) was running when this checkpoint was prepared. Unit tests, debug build, and signer steps PASS; confirm emulator and final job result before treating it as final verification.
- Local workspace lacks Gradle/Android SDK; use exact-head GitHub CI evidence. Do not report a local focused run that was not performed.

## Current lower-level bypass inventory

Direct provider mutations in production are `DriveClient.createFolder`, `renameFolder`, `deleteDocument`, and `DrivePhotoUploader.create`; each has a required guard. `PhotoUploadCoordinator.upload` checks before queue transition. Production constructor call sites inject the central guard. Capture reservations originate in `PhotoCaptureActivity.beginCameraCapture` and `CameraCaptureActivity.reserveCaptureIfNeeded`; both are guarded before a new reservation, and each shutter rechecks. `DrivePhotoReconciler` queries provider state and does not mutate it. Existing focused tests deny Drive folder mutations before provider calls, photo creation before provider calls, and upload before queue state changes; the guard test observes authority loss before a second camera shutter and next Drive attempt. These claims are source-audit and fake-provider/JVM evidence, not physical device proof.

## Exact next action

1. Confirm final CI result for run 875 and fix only a current-head failure if one occurs.
2. Inspect PR #72 final diff and changed-file list against the governed scope; check denied capture/Drive tests and the lower-level inventory above for omissions.
3. If no runtime fixes are needed, update the Phase 12E completion record and roadmap with exact final head/CI/artifact, then stage only the proportional Samsung offline/online/protected-work reality gate from the Phase 12 plan. Do not wait 72 real hours.
4. Stop before merge for the operator's Level 3 approval. No branch result or CI result authorizes merging by itself.

When a gate fails, stop that affected path and preserve photos, queue state, and the prior working APK. Use a bounded fix on this branch; do not expand Phase 12 scope.
