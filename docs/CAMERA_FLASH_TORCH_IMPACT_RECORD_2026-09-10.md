# Camera Flash and Torch Controls — Level 2 Impact Record

Date: 2026-09-10

Status: PHYSICAL DEVICE PASS — READY TO MERGE

Branch: `feat/camera-flash-torch-controls`

Base: `2a66e80378e5c3853a111121104e532df1bf6a03`

Exact automated-tested runtime head: `15a74ba2fab5f3799093824ec372d20b15a8fe08`

Android CI run: `34554155260`

APK artifact: `10181991210` (`field-photo-prep-internal-apk`)

Artifact digest: `sha256:bb7de0960ebaf44366c1993e57a06ae36a658dac4aa4c8d0377d5e5cc4311980`

## User-facing problem

The in-app CameraX workflow is now field-proven for multi-shot capture and automatic preparation, but dark interiors and low-light inspection areas still require better camera lighting control.

## Approved behavior

Add two separate camera controls:

- **Flash** cycles `Auto → On → Off → Auto` and defaults to **Auto** for every new camera session.
- **Torch** toggles continuous light `Off ↔ On` and defaults to **Off** for every new camera session.

The current session retains its lighting selections across ordinary Activity recreation. A future camera session starts again at Flash Auto and Torch Off.

## Change classification

Level 2 — normal camera UI/control behavior.

No photo identity, protected-original persistence, prepared-copy policy, Drive access, SAF grant, upload, retry, reconciliation, cleanup, or stored-data schema changes are approved in this slice.

## Owning files

- `CameraCaptureActivity.java` — binds CameraX lighting controls to the active rear camera and renders the controls.
- `CameraFlashMode.java` — pure session flash-mode cycle/label model.
- `CameraFlashModeTest.java` — focused mode-cycle coverage.

## Read/write surfaces

The lighting controls read only CameraX camera capability/state for the current in-app camera session. They write only CameraX flash mode and torch state for that active camera.

No local photo metadata, Drive metadata, queue state, prepared file, or remote state is changed by selecting a lighting mode.

## Protected behavior

- every shutter still reserves a unique protected photo before bytes are written;
- capture remains bound to the exact work-order destination already stored for the session;
- lighting changes never finalize, discard, upload, or redirect a photo;
- a device without a flash unit disables both lighting controls instead of failing capture;
- torch state is best-effort and failures are surfaced without corrupting camera/photo state;
- leaving the camera does not intentionally leave the torch enabled;
- flash defaults to Auto and torch defaults to Off on a fresh session.

## Automated verification

PASS on exact runtime head `15a74ba2fab5f3799093824ec372d20b15a8fe08`.

Android CI run `34554155260` completed successfully with:

- complete JVM/unit tests including the Flash Auto → On → Off → Auto cycle;
- internal debug APK build;
- stable test-signer verification;
- Android emulator instrumentation and internal launch smoke; and
- APK artifact packaging.

No executable changes follow that runtime head in this branch; later contract/status updates are documentation-only.

## Physical-device result

**PASS on the operator's Samsung phone on 2026-09-11.**

The operator tested each lighting control in the combined staged APK and reported that they work. This closes the device-specific control boundary for:

- Flash **Auto**;
- Flash **On**;
- Flash **Off**;
- Torch **On**; and
- Torch **Off**.

The existing multi-shot camera/session behavior had already passed its physical-device gate on the same supported Android device. No new photo identity, persistence, preparation, or Drive behavior was introduced by this lighting slice.

## Merge state

The required Level 2 automated and physical-device evidence is complete. PR #26 may be merged. No additional operator approval is required because the approved scope did not change.

## Rollback

Return to base `2a66e80378e5c3853a111121104e532df1bf6a03`. Existing photos, queue records, prepared copies, and Drive data remain untouched.
