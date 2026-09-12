# Field Camera Zoom, Landscape, and Wide Angle — Level 2 Impact Record

Date: 2026-09-11

Status: MERGED — AUTOMATED PASS, PHYSICAL DEVICE SMOKE PASS

Branch: `feat/camera-zoom-controls`

Merged via PR #30 at `1b6192a84110acd5ad113660d243a09e3f4a9ef0`.

Rollback baseline: `2d161b464c4f4b9acde9c53daad5675d3012f07c`

Exact automated-tested runtime head: `ffb8c68a5d1b58e0537832241792e1c945ad4da2`

Android CI run: `34671156169` — PASS

APK artifact: `10290314715` (`field-photo-prep-internal-apk`)

Artifact digest: `sha256:c2cdfe5f326cf4cf06185b9b7eea40c6d65daaf46b194c98f425ba356d9f74c2`

Staged APK SHA-256: `d33aa156d816d48ee3b7e864dfa9b2ee8baba2c431c0767e24f73f3055e76a27`

Later documentation/contract commits after the runtime head do not change the executable tree.

## User-facing problem

Field inspection photos need the in-app camera to behave more like the operator's Samsung camera. The operator needs fast zoom, landscape framing, and true wide-angle coverage without leaving the protected Field Photo Prep capture session.

## Approved behavior

The camera uses a phone-camera-style layout with the live preview as the dominant surface.

- compact Flash and Torch controls stay at the preview edge;
- a large round shutter is the primary capture control;
- **Done** remains obvious for ending the protected multi-shot session;
- the session photo count remains visible;
- quick zoom/lens presets sit close to the shutter;
- **1×** always means the normal rear-camera framing and is the true reset point;
- **3×** appears only when the normal rear camera reports enough supported zoom;
- an ultra-wide preset appears only when CameraX exposes a real sub-1× rear-camera path;
- the ultra-wide label uses the device-reported effective ratio rather than inventing `0.5×`;
- pinch-to-zoom remains available;
- the fine zoom slider is normally hidden and appears while zoom is being adjusted;
- CameraX `ZoomState` remains the displayed source of truth;
- portrait and landscape are both supported;
- landscape reflows controls into a compact side rail so the preview stays dominant;
- rotation during the camera session does not intentionally change protected photo identity or destination;
- capture target rotation follows the current display so saved stills remain visually usable.

If the logical default rear camera already reports zoom below 1×, that CameraX path is preferred. Otherwise the app may use a CameraX-exposed physical/alternate rear camera whose intrinsic zoom ratio is genuinely below 1×. If that camera cannot be bound safely, the ultra-wide choice is suppressed and ordinary 1× capture is restored. The app never labels an ordinary digitally cropped view as ultra-wide.

## Change classification

Level 2 — normal camera interaction, source selection, orientation, and UI behavior. This work does not alter persisted photo schema, protected-original persistence, address/work-order identity, Drive destination, SAF permissions, upload/retry/UNCERTAIN semantics, cleanup, signing, or deployment behavior.

## Owning files

- `CameraCaptureActivity.java` — camera-style portrait/landscape UI, CameraX binding, quick presets, physical-wide capability discovery, pinch, slider, target rotation, flash/torch, and protected multi-shot capture interaction.
- `CameraZoomMath.java` — pure slider and pinch range math.
- `CameraLensMath.java` — pure intrinsic/effective lens ratio conversion and ultra-wide capability math.
- `CameraZoomMathTest.java` — focused zoom math coverage.
- `CameraLensMathTest.java` — focused lens/effective-ratio coverage.
- `AndroidManifest.xml` — camera activity follows the full sensor and handles orientation/screen-size configuration changes in place.

## Read surfaces

- CameraX `ZoomState` for current/min/max/local linear zoom state;
- CameraX rear-camera and physical-camera capability metadata;
- CameraX intrinsic zoom ratio for truthful wide-angle labeling;
- current display rotation/orientation;
- current camera-session UI state.

## Write surfaces

- CameraX `CameraControl` zoom and torch state;
- CameraX use-case binding when selecting the normal or a capability-proven ultra-wide rear camera;
- CameraX preview/image-capture target rotation;
- activity saved-instance state for current effective zoom and physical-wide mode.

No queue record, protected-photo metadata, work-order provider ID, Drive object, SAF permission, preparation record, or upload state is written by these controls.

## Protected behavior

- every shutter press still reserves and protects a unique original before camera bytes are written;
- multi-shot capture remains independent per photo;
- every shot remains bound to the exact address/work-order provider identity established before camera launch;
- non-empty bytes are still preserved even when a camera callback reports failure;
- flash Auto/On/Off and Torch On/Off remain camera-session controls only;
- lens/zoom/orientation changes cannot redirect a captured photo;
- zoom/lens controls are disabled during an active shutter write;
- ordinary capture remains available when adjustable zoom or ultra-wide is unsupported;
- true ultra-wide is capability-gated and fails back to the normal camera rather than guessing;
- protected originals, automatic preparation, selectable batch upload, UNCERTAIN handling, reconciliation, and confirmed cleanup remain unchanged;
- leaving the camera still finalizes/preserves any non-empty capture reservation safely.

## Focused automated verification

PASS on exact runtime `ffb8c68a5d1b58e0537832241792e1c945ad4da2`.

Coverage proves:

- slider progress maps safely across CameraX linear zoom;
- slider values clamp at both ends;
- pinch multiplication clamps to the camera-reported min/max range;
- invalid pinch scale input cannot escape the supported range;
- effective displayed zoom correctly combines physical-camera intrinsic ratio with local camera zoom;
- desired effective zoom maps back to a valid local physical-camera zoom ratio;
- physical-camera ratio conversion clamps to its reported range;
- only a real usable ratio below 1× qualifies as ultra-wide.

Android CI `34671156169` passed the complete unit suite, internal debug build, stable test-signer verification, Android emulator instrumentation, internal launch smoke, and APK artifact packaging.

## Physical-device result

PASS on the operator's Samsung phone using the staged combined camera build. After the requested one-pass field-camera check, the operator reported that it **"seems to work fine"** and requested cleanup rather than another test cycle.

No camera-layout, portrait/landscape, zoom-control, lighting-control, capture, orientation, or **Done** defect was reported from that pass. Under the project no-loop rule, this physical observation is accepted and is not repeated merely for confidence.

Ultra-wide remains capability-gated by design. This record does not invent a sub-1× result that was not separately reported: when CameraX exposes a truthful ultra-wide path the preset may appear; when it does not, suppressing that preset is the correct behavior and ordinary capture remains valid.

## Primary risks and mitigations

- **Fake wide-angle labeling:** mitigated by requiring a CameraX-reported sub-1× logical range or a rear camera with a real sub-1× intrinsic ratio.
- **Physical-camera bind failure:** fail back to normal 1× capture and suppress the unsupported wide option.
- **Wrong meaning of 1×:** the 1× preset explicitly returns to the default rear camera and requests effective 1× rather than assuming minimum linear zoom equals 1×.
- **Cramped landscape controls:** orientation-specific overlay keeps the preview dominant and moves shutter/presets into a compact right rail.
- **Wrong saved orientation:** preview and `ImageCapture` target rotation are updated from the active display before capture and on configuration changes.
- **Zoom state drift:** CameraX `ZoomState` is authoritative for slider/readout/preset highlighting.
- **Capture race:** zoom/lens controls are disabled while a shutter write is active.

## Affected physical-device gate

The required physical-device smoke check is complete. No Drive upload retest is required.

The staged check covered the camera as an integrated operator surface rather than repeating previously proven camera behavior in isolation: portrait/landscape use, quick zoom controls, pinch/slider behavior, lighting controls, protected still capture, orientation handling, Done, and layout safety. Device-specific ultra-wide availability remains capability-driven and absence of an unsupported preset is not a failure.

## Drive/upload test scope

No Drive upload retest is required for this change because no Drive, SAF, queue, preparation, retry, uncertainty, reconciliation, or cleanup runtime code is modified.

## Rollback

Revert PR #30 to `main` commit `2d161b464c4f4b9acde9c53daad5675d3012f07c`. Existing queued photos, protected originals, prepared copies, remote identities, and Drive content require no migration because camera zoom/orientation/lens selection introduces no persisted queue or Drive schema.
