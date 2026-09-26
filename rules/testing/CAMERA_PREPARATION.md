# Testing Rule Pack — Camera & Preparation

## Applies when

Read this file when work touches:
- camera capture or camera lifecycle;
- multi-shot behavior;
- flash, torch, zoom, lens selection, orientation, or camera layout;
- capture-to-photo identity;
- automatic/manual photo preparation;
- bitmap/EXIF preparation behavior.

Use together with `TESTING_CONTRACT.md` and the affected product rules in `CONTRACT.md`.

## Focused regression boundary

For the current Android in-app camera workflow, focused coverage must preserve:

- one camera session may save multiple photos before **Done**;
- each shot receives a fresh protected identity and cannot overwrite another shot;
- all shots retain the same immutable selected work-order destination for that session;
- an unused empty reservation may be cleaned up, but non-empty captured data is preserved;
- Flash mode cycles **Auto → On → Off → Auto**, and a fresh camera session defaults to **Auto**;
- Torch is a separate continuous-light control, defaults **Off** on a fresh session, and must not remain intentionally enabled after leaving the camera;
- devices without a usable flash unit disable flash/torch controls instead of breaking ordinary capture;
- lighting-state changes must not mutate capture identity, queue state, destination identity, preparation state, or Drive state;
- zoom slider and pinch requests remain clamped to the active camera's reported range;
- **1×** means the normal default rear-camera framing rather than merely minimum linear zoom;
- an ultra-wide shortcut is exposed only from a real CameraX-reported sub-1× rear-camera path and must fail back to ordinary capture if that path cannot bind safely;
- effective zoom readout for a physical wide camera accounts for that camera's intrinsic zoom ratio;
- portrait/landscape reflow and target rotation must not mutate capture identity or destination identity;
- zoom/lens controls cannot race an active shutter write;
- finishing one shot places it into durable waiting state before background preparation is notified;
- automatic preparation failure cannot invalidate a durable capture;
- only one full-image preparation operation owns the process-wide preparation gate at a time; and
- restart recovery can requeue eligible waiting photos whose prepared derivative is missing.

These are state/identity boundaries first. UI-only tests are not a substitute.

## Device and emulator evidence

Camera behavior cannot be proven entirely by JVM/unit tests. When capture behavior changes, perform the smallest affected device or emulator check that proves the changed surface.

Synthetic bitmap/EXIF transformation can be proven with Android emulator instrumentation.

Actual field-camera capture, device-specific camera behavior, repeated in-app shutter use, control placement around system navigation, and final visual/orientation confidence require a physical supported Android device before those behaviors are called field-proven.

For camera-lighting changes, the physical-device gate must confirm:
- fresh session starts at **Flash Auto / Torch Off**;
- Flash Auto/On/Off can be selected;
- Torch can be turned On and Off;
- repeated capture still works without lighting controls racing the shutter.

A device without flash capability may satisfy the alternative path by proving the controls fail closed/disabled without breaking capture.

For camera zoom/orientation/lens changes, the physical-device gate must confirm:
- normal **1×** framing;
- slider and pinch behavior;
- correct portrait/landscape reflow and saved orientation;
- any quick preset actually exposed by the device.

A wide preset counts as proven only when the real phone visibly produces wider-than-1× framing through the app. If the device does not expose ultra-wide through the current CameraX path, record that capability boundary instead of inventing a pass.

Existing protected multi-shot, Flash/Torch, and **Done** behavior should be checked only once as part of the same combined camera gate.

When automatic preparation changes, automated instrumentation should prove the real Android bitmap/EXIF path and protected-original preservation. The physical-device gate should then be limited to behavior automation cannot establish honestly, such as camera responsiveness while preparation runs and the operator-visible no-extra-tap workflow.

Device checks do not substitute for automated identity, queue, photo-preservation, and destination-boundary tests.
