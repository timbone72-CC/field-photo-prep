# CameraX Capture Shell — Level 2 Impact Record

Date: 2026-09-10

Status: **PHYSICAL DEVICE PASS — superseded by integrated multi-shot camera workflow**

Branch: `feat/in-app-camerax-capture-shell`

Rollback baseline: `0f25b5ee2dda19b316aca32dcf056ac804b78c7b`

Exact repaired automated-tested runtime head: `b63ecf92468962df7894f8bf597566648e76a161`

Android CI run: `34534165067`

Internal APK artifact: `10174883327` (`field-photo-prep-internal-apk`)

Artifact digest: `sha256:e22345d7bfa5f8ed93fb2f053a54d3cb3960bbb8bb6688a70041c7ee3991fee3`

## User-facing problem

The current photo workflow launches the external Samsung/system camera. One field photo therefore requires an extra vendor-camera confirmation step, and after returning to Field Photo Prep the newly captured photo is not automatically selected. Repeating that interaction across a property adds unnecessary friction.

Field use has now demonstrated that the Phase 8A decision to defer an in-app camera is no longer the best workflow tradeoff.

## Approved behavior for this slice

Replace the external still-camera launch with a minimal in-app CameraX capture shell while preserving the existing protected-original and queue architecture.

This first slice is intentionally one-photo-per-camera-session:

`Take Photo → in-app preview → shutter → return with captured photo already selected`

Multi-shot session capture (`shutter → shutter → shutter → Done`) is the intended follow-on, but it is not part of this slice.

## Change classification

Level 2 — normal camera/UI feature.

The change adds the Android runtime `CAMERA` permission but does not alter Google Drive permissions, the persisted SAF tree grant, folder identity, upload destination construction, upload/retry semantics, queue schema, or cleanup semantics.

If implementation requires any of those Level 3 surfaces to change, this branch must stop and be reclassified before that expanded work proceeds.

## Owning files

- `app/src/main/java/com/inandout/fieldphotoprep/CameraCaptureActivity.java` — CameraX preview, runtime camera permission, shutter, capture lifecycle, and writing into the already-reserved protected-original file.
- `app/src/main/java/com/inandout/fieldphotoprep/PhotoCaptureActivity.java` — reserve the existing capture record, launch the in-app camera activity, finalize the protected image, and auto-select a successful/non-empty return.
- `app/src/main/java/com/inandout/fieldphotoprep/FieldPhotoPrepApplication.java` — provide CameraX 1.6 with the explicit Camera2 backend configuration while preserving existing queue startup recovery.
- `app/src/main/AndroidManifest.xml` — declare camera permission and the new internal activity.
- `app/build.gradle` — add stable AndroidX Activity and CameraX dependencies.

## Read surfaces

The new camera activity may read only:

- the capture UUID passed by `PhotoCaptureActivity`;
- the existing `CAPTURING` `PendingPhotoRecord` for that UUID; and
- the app-private protected-original path already owned by `PendingPhotoStore`.

It does not read or rediscover the current Drive destination from UI state.

## Write surfaces

The new camera activity writes image bytes only to the exact app-private protected-original file already reserved by `PendingPhotoStore.beginCapture(...)`.

It does not write queue metadata, Drive content, Drive folder state, SAF grants, prepared copies, or upload results. `PhotoCaptureActivity` continues to call `finishCaptureIfImageExists(...)` after the camera returns.

## Protected behavior

- exact address/work-order identities are persisted before camera capture begins;
- camera capture works without network access;
- a non-empty protected original is preserved even if the camera/result path reports failure;
- an empty cancelled reservation may be removed using the existing store rule;
- app/process restart reconciliation remains owned by `PendingPhotoStore`;
- preparation remains separate and non-destructive;
- Drive upload still uses only the photo's stored immutable destination;
- retry, UNCERTAIN handling, confirmed-success bookkeeping, and cleanup are unchanged;
- no permanent in-app photo library is introduced;
- still photos only; video remains out of scope.

## Photo sizing boundary

CameraX does not intentionally resize or compress the protected original in this slice. It captures a normal JPEG into the protected-original file.

The existing preparation layer remains responsible for the upload-sized derivative:

- maximum long edge: `2048` pixels;
- JPEG quality: `85`;
- smaller images are not upscaled;
- the protected original remains unchanged until confirmed Drive success permits cleanup.

The Drive upload coordinator uploads the prepared derivative, not the protected original.

## Dependency choice

Use stable releases only for this field-facing slice:

- AndroidX CameraX `1.6.2` (`camera-camera2`, `camera-lifecycle`, `camera-view`);
- AndroidX Activity `1.13.0` for lifecycle ownership.

CameraX 1.6 uses explicit application-level backend configuration here, so the existing application class implements `CameraXConfig.Provider` and returns `Camera2Config.defaultConfig()`. Existing queue startup recovery in that class remains unchanged.

These were the current stable AndroidX releases when this record was created on 2026-09-10.

## First physical-device result — FAIL

The first staged CameraX APK opened the new camera surface, but the operator could not take a photo because the camera-screen buttons did not behave as usable controls on the physical phone.

This result is classified as a camera UI/control failure. It does not invalidate the protected-original, destination, preparation, upload, retry, or Drive evidence because no new remote behavior was exercised.

Observed product issue:

- `Take Photo` could be non-actionable while CameraX was still binding, with no useful tap feedback;
- the first programmatic camera layout did not sufficiently isolate the bottom controls from the preview surface for the real-device gate.

## Control repair

Runtime commit `b63ecf92468962df7894f8bf597566648e76a161` changes only the in-app camera control surface:

- the preview is isolated inside its own frame and is explicitly non-clickable/non-focusable;
- the bottom control bar is fixed outside the preview touch surface and raised to the front;
- both buttons receive large equal-width touch targets;
- `Take Photo` is always a usable control rather than a silently disabled control;
- tapping before CameraX is ready reports `Camera is still starting` instead of doing nothing;
- successful binding reports `Ready — tap Take Photo`;
- camera-start failure remains visible and leaves Cancel available;
- capture failure re-enables Take Photo and Cancel rather than leaving the screen stuck;
- active file save still blocks Cancel/Back to avoid racing the protected-original write.

No Drive, queue, folder-identity, preparation, retry, or cleanup implementation changed in this repair.

## Automated verification

PASS on exact repaired runtime head `b63ecf92468962df7894f8bf597566648e76a161`.

Android CI run `34534165067` completed successfully with:

- unit tests;
- internal debug APK build;
- stable test-signer verification;
- emulator instrumentation tests;
- internal app launch smoke test; and
- APK artifact packaging.

Existing `PendingPhotoStoreTest` coverage remains the focused persistence boundary, especially:

- `beginCapturePersistsExactBindingBeforeImageDataExists`;
- `successfulCaptureRequiresBytesThenPersistsWaitingAcrossReload`;
- `emptyCameraReturnRemovesOnlyEmptyReservation`;
- `interruptedCaptureWithBytesIsPreservedAsWaiting`;
- `destinationBindingDoesNotChangeWhenAnotherWorkOrderExists`.

## Physical-device retest — PASS

The repaired CameraX shell passed the supported Samsung-phone smoke gate on 2026-09-10/11:

1. Field Photo Prep opened its own live rear-camera preview for the selected disposable work order.
2. The camera reached the ready state.
3. **Take Photo** behaved as an active control and captured successfully.
4. The app returned to the same work-order photo screen with the new non-empty protected photo selected.
5. No Samsung/system-camera accept/reselect loop was required.

The one-shot return behavior in this slice was intentionally temporary and was subsequently superseded by the separately governed multi-shot session.

## Merge state

Physical gate passed. This original stacked PR is superseded by consolidated PR #25, which contains this exact camera work plus the physically proven multi-shot and automatic-preparation follow-ons directly against `main`.

## Rollback

Revert this branch to baseline commit `0f25b5ee2dda19b316aca32dcf056ac804b78c7b`. Rolling back the camera UI must not delete existing pending photos or protected originals.
