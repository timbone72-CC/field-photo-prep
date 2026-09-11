# Automatic Photo Preparation — Level 2 Impact Record

Date: 2026-09-10

Status: PHYSICAL DEVICE PASS

Branch: `feat/automatic-photo-preparation`

Parent multi-shot branch: `feat/in-app-camerax-multishot-session`

Parent physical gate: PASS on the operator's Samsung phone. The in-app camera can take repeated photos in one session and returns only after **Done**.

## User-facing problem

The CameraX multi-shot workflow now removes repeated camera confirmation and work-order-screen bouncing, but every protected photo still requires a separate manual **Prepare Selected Photo for Upload** action before it can be uploaded efficiently.

That is too much repeated work for field jobs with many photos.

## Approved behavior

Every successful protected photo should automatically enter the existing preparation pipeline after it reaches durable `WAITING` state.

Normal field flow becomes:

`Take Photo → protected original saved → smaller upload copy prepared in background → keep taking photos`

Preparation must not delay the next CameraX shutter press.

Prepared copies continue to use the existing approved policy:

- maximum long edge: `2048` pixels;
- JPEG quality: `85`;
- smaller images are not upscaled;
- EXIF orientation is applied correctly;
- protected original remains unchanged until confirmed Drive success allows cleanup.

## Change classification

Level 2 — normal preparation/workflow automation.

No queue schema, Drive permissions, SAF tree grant, folder identity, upload destination, upload idempotency, UNCERTAIN reconciliation, confirmed-success bookkeeping, or cleanup policy changes are approved in this slice.

## Owning files

- `PhotoCaptureCompletionBus.java` — non-throwing notification that a protected photo has durably reached `WAITING`.
- `AutomaticPhotoPreparationQueue.java` — process-local serial preparation queue.
- `PhotoPreparationGate.java` — shared process-wide preparation ownership so manual and automatic preparation cannot run concurrently.
- `PendingPhotoStore.java` — publish a completion notification only after durable transition to `WAITING`.
- `FieldPhotoPrepApplication.java` — initialize the automatic preparation queue and recover eligible waiting photos after process restart.
- focused unit/instrumented tests for notification, serialization, restart recovery, and prepared-output behavior.

## Read surfaces

Automatic preparation may read only:

- persisted pending-photo records;
- the protected-original file associated with the exact photo ID;
- the existing prepared-photo path derived from that same photo ID; and
- existing preparation policy constants.

It does not read or reconstruct Drive destination hierarchy and does not alter stored address/work-order identity.

## Write surfaces

Automatic preparation writes only the existing app-private prepared-photo derivative through `PhotoPreparer`.

It does not modify the protected original, queue state, Drive content, Drive folder state, SAF permissions, upload result state, provisional remote identity, or confirmed remote identity.

## Scheduling behavior

- `PendingPhotoStore` publishes an event only after `WAITING` metadata has been durably written.
- event delivery is best-effort and non-throwing: a scheduling failure must never turn an already-good camera capture into a failed capture.
- the automatic queue deduplicates photo IDs and uses one worker at a time.
- preparation is revalidated immediately before work begins.
- manual and automatic preparation share one process-wide `PhotoPreparationGate` so image decoding/compression cannot run concurrently.
- if the process dies, no queue metadata is required: the protected `WAITING` record is already durable.
- on next app startup, eligible `WAITING` photos that still lack a prepared copy are queued again.
- a preparation failure leaves the protected original and `WAITING` state untouched so manual retry or a later process restart remains safe.

## Protected behavior

- a good capture remains good even if automatic preparation cannot start;
- the original photo is never overwritten by resize/compression;
- each prepared copy belongs to exactly one immutable photo ID;
- no later photo can overwrite another photo's prepared copy;
- no parallel preparation spike is allowed;
- Drive upload still requires and uploads the prepared derivative;
- confirmed Drive cleanup rules remain unchanged;
- the camera remains usable while preparation runs in the background.

## Primary risks

1. automatic and manual preparation could otherwise decode multiple full photos at once and create memory pressure;
2. a callback exception after capture finalization could otherwise make the camera believe a durable photo failed;
3. process death could otherwise leave a `WAITING` photo permanently unprepared;
4. repeated events could otherwise perform duplicate preparation work;
5. automatic preparation must never mutate queue or Drive identity while creating the derivative.

## Focused verification

Automated coverage proves:

- successful `WAITING` transition emits one completion event;
- empty/cancelled capture emits no preparation event;
- a broken completion listener cannot make `finishCaptureIfImageExists(...)` fail after durable capture;
- separate `PhotoPreparationGate` instances share one process-wide owner;
- repeated enqueue of the same photo does not prepare it twice;
- multiple queued photo IDs are processed serially;
- startup/backlog scan queues a valid `WAITING` photo that lacks a prepared copy;
- actual Android preparation creates a valid derivative constrained to the existing 2048px policy while preserving the original.

## Automated verification

PASS on exact runtime commit `75073387f9bf479c13f708e3d8a011189191ce1e`.

Android CI run `34551100116` completed successfully with:

- complete JVM unit tests;
- internal debug APK build;
- stable test-signer verification;
- Android instrumented image/preparation tests;
- internal app launch smoke test; and
- APK artifact packaging.

Artifact `10180954286` was produced from the exact runtime commit above.

This documentation-only status commit follows the verified runtime and does not change executable behavior.

## Physical-device gate

PASS on the operator's Samsung phone on 2026-09-10.

Verified behavior:

- multi-shot CameraX capture remained responsive;
- photos were captured without returning to the work-order screen between shots;
- automatic preparation completed without pressing the manual **Prepare Selected Photo for Upload** button;
- the tested field workflow operated as intended.

No Drive upload was required for this Level 2 gate.

## Merge state

Physical-device verification is complete. Keep the pull request unmerged until explicit integration/merge authorization is given.

## Rollback

Return this branch to its parent multi-shot head. Existing pending photos, protected originals, prepared copies, and Drive data must remain untouched by rollback.
