# Camera Flash and Torch Controls — Level 2 Impact Record

Date: 2026-09-10

Status: IN PROGRESS

Branch: `feat/camera-flash-torch-controls`

Base: `2a66e80378e5c3853a111121104e532df1bf6a03`

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

## Focused verification

- flash mode cycles Auto → On → Off → Auto;
- app compiles against CameraX lighting APIs;
- complete Android CI passes on the final runtime head;
- physical Samsung phone gate confirms Flash Auto/On/Off and Torch On/Off are usable without breaking repeated capture.

## Rollback

Return to base `2a66e80378e5c3853a111121104e532df1bf6a03`. Existing photos, queue records, prepared copies, and Drive data remain untouched.
