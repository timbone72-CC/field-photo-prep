# CameraX Capture Shell — Level 2 Impact Record

Date: 2026-09-10

Status: **STAGED — automated verification PASS; physical camera gate pending**

Branch: `feat/in-app-camerax-capture-shell`

Rollback baseline: `0f25b5ee2dda19b316aca32dcf056ac804b78c7b`

Exact automated-tested runtime head: `9f4bc9a2e45cd22be4bcb2730b2f8677190a3105`

Android CI run: `34533037807`

Internal APK artifact: `10174455137` (`field-photo-prep-internal-apk`)

Artifact digest: `sha256:98e5a1ff3f9ae46ec55f36e36db753ab640e9ec5723f95ae4accf21d72236d20`

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

## Dependency choice

Use stable releases only for this field-facing slice:

- AndroidX CameraX `1.6.2` (`camera-camera2`, `camera-lifecycle`, `camera-view`);
- AndroidX Activity `1.13.0` for lifecycle ownership.

CameraX 1.6 uses explicit application-level backend configuration here, so the existing application class implements `CameraXConfig.Provider` and returns `Camera2Config.defaultConfig()`. Existing queue startup recovery in that class remains unchanged.

These were the current stable AndroidX releases when this record was created on 2026-09-10.

## Primary risks

1. Camera permission denial could leave an empty reservation if the return path is not finalized correctly.
2. Activity/process interruption during capture could leave `CAPTURING` state; existing restart reconciliation must remain able to preserve non-empty data or remove only an empty reservation.
3. The camera activity must never reconstruct destination identity from whatever work order is currently open.
4. Back/cancel during an active file write must not race the parent into deleting an empty-looking reservation before CameraX finishes.
5. Device-specific CameraX preview/orientation behavior cannot be called field-proven from CI alone.

## Automated verification

PASS on exact runtime head `9f4bc9a2e45cd22be4bcb2730b2f8677190a3105`.

Android CI run `34533037807` completed successfully with:

- unit tests;
- internal debug APK build;
- stable test-signer verification;
- emulator instrumentation tests;
- internal app launch smoke test; and
- APK artifact packaging.

The later documentation-only branch commits do not alter the tested runtime.

Existing `PendingPhotoStoreTest` coverage remains the focused persistence boundary, especially:

- `beginCapturePersistsExactBindingBeforeImageDataExists`;
- `successfulCaptureRequiresBytesThenPersistsWaitingAcrossReload`;
- `emptyCameraReturnRemovesOnlyEmptyReservation`;
- `interruptedCaptureWithBytesIsPreservedAsWaiting`;
- `destinationBindingDoesNotChangeWhenAnotherWorkOrderExists`.

## Physical-device smoke check

Before this behavior is called field-proven on the supported Android phone:

1. open one disposable test work order;
2. tap **Take Photo** and confirm Field Photo Prep shows its own live rear-camera preview rather than launching Samsung Camera;
3. take one ordinary still photo with one shutter press and no Samsung OK/Retake confirmation;
4. confirm the app returns to the same work order with the new non-empty photo already selected;
5. confirm the photo remains recoverable across a close/reopen before upload;
6. confirm capture works with network connectivity disabled;
7. cancel one camera session before pressing the shutter and confirm no empty photo is kept;
8. visually confirm usable orientation/content.

This is the smallest required CameraX device reality gate for this slice. It does not require a Drive upload because upload behavior is unchanged.

## Merge state

Do not merge this draft pull request until the physical-camera smoke gate passes. Do not add multi-shot capture to this slice.

## Rollback

Revert this branch to baseline commit `0f25b5ee2dda19b316aca32dcf056ac804b78c7b`. Rolling back the camera UI must not delete existing pending photos or protected originals.
