# CameraX Multi-Shot Session — Level 2 Impact Record

Date: 2026-09-10

Status: STAGED — AUTOMATED PASS, PHYSICAL DEVICE GATE PENDING

Branch: `feat/in-app-camerax-multishot-session`

Parent CameraX shell head: `11235778813383044ca74dca2b56d6803c13b014`

Rollback point for this slice: `11235778813383044ca74dca2b56d6803c13b014`

Exact automated-tested runtime/test head: `5647229173ef732522b312caacdadd256df8e0bc`

Android CI run: `34549789161`

APK artifact ID: `10180466451`

Artifact digest: `sha256:5c975c9af38bb03939b10d97f847b48cc75c8e16a241482377609ea255cbb9e3`

## User-facing problem

The repaired in-app CameraX shutter works on the physical phone, but two workflow problems remain:

1. the camera controls sit too close to Android's system navigation controls; and
2. every successful photo currently closes the camera and returns to the work-order photo screen.

That is too slow for field work where many photos may be required for one work order.

## Approved behavior

Keep the camera open for one work-order photo session:

`open work order → camera → Take Photo → Take Photo → Take Photo → Done`

Each successful shutter press remains one independent protected-photo transaction. The app must bind every photo to the same immutable address/work-order provider identities captured before the camera session began.

The bottom camera controls must respect Android system-bar insets so app controls do not overlap or crowd the phone's Recents/Home/Back area.

This slice does not add automatic preparation or upload. Captured photos return as ordinary protected `WAITING` records and continue through the existing preparation/upload workflow.

## Change classification

Level 2 — normal camera/UI feature.

No queue schema, Drive permission, SAF tree grant, folder identity, upload destination, retry, reconciliation, cleanup, or compression policy changes are approved in this slice.

## Owning files

- `CameraCaptureActivity.java` — multi-shot session state, per-shot reservation/finalization, Done behavior, photo count, safe system-bar insets.
- `PhotoCaptureActivity.java` — consume a multi-shot result and select the last successfully captured photo without re-finalizing already completed shots.
- `MultiShotCaptureStoreTest.java` — focused repeated-capture identity/isolation coverage.

## Read surfaces

The camera session reads only:

- the initial pre-reserved capture record;
- the immutable address/work-order identity stored on that record; and
- subsequent per-shot records created from that same immutable identity.

The camera does not rediscover or read a newly selected work order from current UI state between shots.

## Write surfaces

For every shutter press:

1. reserve a unique local photo record before CameraX writes bytes;
2. write only to that record's app-private protected-original file;
3. finalize non-empty capture data to `WAITING` through `PendingPhotoStore.finishCaptureIfImageExists(...)`;
4. never overwrite a prior protected photo with a later shutter press.

No Drive write occurs from the camera session.

## Protected behavior

- every photo has a unique local UUID;
- every photo retains the exact same stored address/work-order identities for the session;
- a successful shot is durable before the next shot is allowed;
- a failed shot cannot reuse a prior successfully captured file;
- partial/non-empty image data is preserved rather than silently deleted;
- an empty unused reservation may be removed through the existing capture-finalization rule;
- Back/Done are blocked while a file write is actively in progress;
- restart reconciliation remains able to recover interrupted `CAPTURING` records;
- original photo sizing/compression policy is unchanged;
- preparation and Drive upload remain separate existing stages.

## Control placement

Because target SDK 36 uses edge-to-edge system-bar behavior, the camera root applies Android system-bar insets at runtime. This gives the camera UI safe top/bottom spacing on devices with gesture navigation or three-button navigation without hard-coding one phone model's navigation-bar height.

## Automated verification

PASS on exact head `5647229173ef732522b312caacdadd256df8e0bc`.

Android CI run `34549789161` completed successfully with:

- complete JVM/unit tests, including the new three-shot identity/isolation test;
- internal debug APK build;
- stable test-signer verification;
- emulator instrumentation tests;
- internal app launch smoke test; and
- APK artifact packaging.

The focused repeated-capture test proves three sequential captures receive distinct local UUIDs and image files while retaining the same exact address/work-order stored destination identity and independent `WAITING` state.

## Physical-device gate

Use one disposable work order and verify:

1. camera controls are clearly separated from Android's system navigation area;
2. press Take Photo three times without returning to the work-order screen between shots;
3. photo count advances after each successful shot;
4. press Done once;
5. app returns to the original work-order photo screen only after Done;
6. three separate protected photo records exist for the same work order;
7. the last captured photo is selected on return.

Stop there. Preparation/upload behavior is unchanged and does not need to be re-proven for this Level 2 camera-session slice.

## Merge state

PR #24 remains draft. Do not merge until the physical multi-shot/control-placement gate passes.

## Rollback

Return this branch to `11235778813383044ca74dca2b56d6803c13b014`. Existing pending photos and protected originals must remain untouched by rollback.
