# Camera Zoom Controls — Level 2 Impact Record

Date: 2026-09-11

Status: STAGED — AUTOMATED PASS, DEVICE VERIFICATION PENDING

Branch: `feat/camera-zoom-controls`

Rollback baseline: `2d161b464c4f4b9acde9c53daad5675d3012f07c`

Exact automated-tested runtime head: `76fd693f6cb34c53b43b77b5ddfef96f1f290f4a`

Android CI run: `34665707170` — PASS

APK artifact: `10288587467` (`field-photo-prep-internal-apk`)

Artifact digest: `sha256:aec98bc8dbe050d855b37524b2401b51521835a1225be97d7c29e2107cb6eb23`

Staged APK SHA-256: `22e2a7d755f55de8ca823e41f258cd1c1b457bb9f5d7003a2878605175665840`

Later documentation/contract commits after the runtime head do not change the executable tree.

## User-facing problem

Field inspection photos sometimes require framing details that cannot be reached physically. The in-app camera needs a practical zoom control without sending the operator back to the system camera.

## Approved behavior

The in-app CameraX camera provides both:

- pinch-to-zoom directly on the live preview; and
- a visible zoom slider for one-handed field use.

A live readout shows the effective zoom ratio. A **Reset 1×** control returns the camera to normal zoom. A fresh camera session starts at 1×. Zoom remains in effect across multiple shutter presses during the same camera session and survives ordinary activity state restoration for that session.

The slider uses the exact CameraX-supported linear zoom range for the active back camera. Pinch zoom is clamped to that camera's reported minimum and maximum zoom ratios. Devices that expose no adjustable zoom range keep ordinary capture working with zoom controls disabled.

## Change classification

Level 2 — normal camera interaction/UI feature. It does not alter stored photo schema, protected-original persistence, address/work-order identity, Drive destination, upload/retry behavior, SAF permissions, cleanup, or signing/deployment semantics.

## Owning files

- `CameraCaptureActivity.java` — renders zoom UI, handles pinch gesture, observes CameraX `ZoomState`, and applies zoom through `CameraControl`.
- `CameraZoomMath.java` — pure range/clamp and slider conversion helper.
- `CameraZoomMathTest.java` — focused JVM coverage for clamp and slider math.

## Read surfaces

- CameraX `ZoomState` for current ratio, minimum/maximum ratio, and linear zoom position.
- current camera-session UI state.

## Write surfaces

- CameraX camera control only (`setZoomRatio` / `setLinearZoom`).
- activity saved-instance state for the current session's linear zoom position.

No queue, photo metadata, Drive, SAF, or provider state is written.

## Protected behavior

- every shutter press still reserves and protects a unique original before camera bytes are written;
- multi-shot capture remains independent per photo;
- flash Auto/On/Off and Torch On/Off behavior remains unchanged;
- zoom changes cannot redirect a photo or alter its work-order identity;
- zoom controls are disabled during an active shutter write;
- protected originals, automatic preparation, selectable batch upload, UNCERTAIN handling, and reconciliation remain unchanged;
- leaving the camera still finalizes/preserves any non-empty capture reservation safely.

## Focused tests

PASS on exact runtime `76fd693f6cb34c53b43b77b5ddfef96f1f290f4a`.

Coverage proves:

- slider progress 0/50/100% maps to CameraX linear zoom 0/0.5/1;
- out-of-range slider progress clamps safely;
- CameraX linear zoom maps back to slider progress;
- pinch multiplication clamps to the device-reported min/max ratio;
- invalid pinch scale input cannot escape the supported range.

The complete Android CI also passed unit tests, internal debug build, stable test signer verification, emulator instrumentation, internal launch smoke, and APK artifact upload.

## Primary risks

- a gesture or slider bug could request an unsupported zoom value;
- camera controls could become cramped on the Samsung field device;
- zoom state could become visually out of sync with CameraX after pinch/slider changes;
- zoom controls could interfere with capture if left active during a shutter write.

The implementation mitigates these by using CameraX `ZoomState` as the displayed source of truth, clamping pinch requests, using CameraX linear zoom for the slider, and disabling zoom controls during capture.

## Affected smoke checks

On the operator's Samsung phone:

1. open a disposable work order and camera;
2. confirm fresh session starts at 1×;
3. move the slider and verify the preview zooms and the numeric readout changes;
4. pinch in/out and verify the slider/readout follow the CameraX state;
5. tap **Reset 1×** and verify normal framing returns;
6. take at least two photos at different zoom levels and confirm both remain separate protected captures;
7. confirm Flash/Torch controls and Done remain usable and are not pushed into the system-navigation area.

No Drive upload retest is required unless executable Drive/upload code changes, because this branch does not touch those surfaces.

## Rollback

Revert this feature branch/PR to main commit `2d161b464c4f4b9acde9c53daad5675d3012f07c`. Existing queued photos and Drive content require no migration because zoom introduces no persisted schema.
