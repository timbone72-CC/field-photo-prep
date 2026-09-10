# Phase 5 Implementation Record — Camera + Temporary Photo Protection

Date: 2026-09-09

## Goal

Allow the operator to take still photos for one exact selected work occurrence while preserving each unuploaded photo locally and binding it permanently to the exact work-order destination identity that was active when capture began.

## Approved scope

Phase 5 only:

- launch the Android system still-camera flow from the selected work order;
- create a unique local photo identity before camera launch;
- create the temporary full-resolution image file before camera launch;
- persist the capture record before handing control to the camera app;
- bind the record to the exact selected address ID and work-order folder ID plus display names for operator context;
- store unconfirmed images in app-private internal storage;
- after a successful camera return, require non-empty image data before marking the record waiting/ready for a future upload phase;
- if camera return is cancelled but image bytes exist, preserve the photo rather than deleting uncertain captured data;
- if camera return is cancelled and the reserved image is still empty, remove that empty reservation;
- on app/photo-screen restart, reconcile interrupted capture records: preserve non-empty image data and remove only empty abandoned reservations;
- show waiting temporary photos for the current work occurrence;
- allow explicit operator discard of one selected temporary photo with confirmation;
- deleting a temporary photo removes only that photo's app-private image and local metadata; it never deletes Drive content.

Explicitly excluded:

- Google Drive upload;
- compression/resize/prepared copies;
- upload status beyond capture/waiting local state;
- background retry;
- permanent in-app gallery;
- video;
- watermarking;
- OCR/AI classification;
- address-folder creation from Phase 4;
- Clear & Reuse from Phase 3B;
- any Drive write.

## Governed base and rollback

Exact base / rollback commit:

`8467381b8dac628c6afbb7ea141ba94f0b298417`

Phase 5 branch:

`feat/phase-5-camera-temp-photo-protection`

Rollback is to the exact base above. Phase 5 creates only app-private local temporary files and metadata; no Drive cleanup is required for rollback.

## Change level

Level 3.

Reason: capture-to-job binding and protected-original persistence are safety-critical. A defect could lose an unuploaded field photo or cause later upload to the wrong work-order destination.

Explicit operator approval is required before merge after automated verification and the smallest practical camera device/emulator reality gate.

## Camera architecture

Use the Android system camera through `MediaStore.ACTION_IMAGE_CAPTURE` rather than embedding a general camera framework.

Rationale:

- keeps the app lean;
- avoids building a second camera UI;
- capture works without network access;
- the app can request a full-resolution output URI;
- no direct camera permission is required because the app delegates capture to the installed camera activity;
- the app retains ownership of the temporary destination file.

The output URI is provided through AndroidX `FileProvider`, restricted to the app's internal `files/pending_photos/` directory. The provider is non-exported and grants URI access only to the launched camera flow.

## Local photo identity and naming

Each capture receives an immutable random UUID before camera launch.

Internal full-resolution filename:

`photo-<uuid>.jpg`

Metadata filename:

`photo-<uuid>.properties`

The UUID, not the visible filename or timestamp, is the local photo identity. The visible filename is derived from that UUID, making normal-operation collisions impractical without relying on timestamp alone.

## Local record fields

Required record fields:

- local photo UUID;
- local image filename;
- capture state (`CAPTURING` or `WAITING` in Phase 5);
- created-at epoch milliseconds;
- exact address provider document ID;
- address display name;
- exact work-order provider document ID;
- work-order display name.

No upload or remote-file ID exists yet in Phase 5.

## Storage boundary

Temporary original image and its metadata are stored under app-private internal storage:

`files/pending_photos/`

This is intentionally not a permanent gallery and is not placed in shared Pictures/DCIM storage.

Metadata uses one Java `.properties` file per photo so the persistence format can be tested with ordinary JVM tests without adding a database or JSON framework. Metadata writes use a temporary metadata file followed by rename into place; a capture is not launched unless its initial metadata record is successfully persisted.

## Capture state machine

### Begin capture

1. Require an exact selected address and work-order from existing `FolderPrefs` state.
2. Generate UUID.
3. Create an empty image reservation in app-private storage.
4. Persist metadata as `CAPTURING` bound to exact address/work-order IDs.
5. Build a FileProvider URI for that exact image file.
6. Launch system camera with `EXTRA_OUTPUT` and temporary read/write URI grants.

If any pre-launch step fails, camera is not launched. Empty partial reservations are cleaned up when safe.

### Camera returns RESULT_OK

1. Resolve the exact pending capture ID.
2. Verify its image file exists and has non-zero length.
3. Atomically update metadata to `WAITING`.
4. Keep the image in local storage for later preparation/upload phases.

If RESULT_OK returns without usable bytes, treat it as capture failure; do not report a waiting photo from an empty image.

### Camera returns cancelled/other result

- If the reserved image file is non-empty, preserve it and mark it `WAITING` because data exists and preserving a potentially valid field photo has priority over cleanup.
- If the reserved image is absent/empty, remove the empty reservation and metadata.

### Restart/interruption reconciliation

On opening the photo-capture surface, scan persisted records:

- `CAPTURING` + non-empty image → convert to `WAITING`;
- `CAPTURING` + empty/missing image → remove the abandoned empty reservation;
- `WAITING` + non-empty image → retain;
- `WAITING` + missing/empty image → surface as corrupt/invalid rather than pretending the photo is safe. Phase 5 must not silently claim a usable waiting photo when bytes are absent.

## Destination immutability

The selected work-order ID is copied into the photo record before camera launch. Later navigation to another address/work order cannot update that record's destination.

Phase 5 does not upload anything, but this immutable binding is the source of truth that Phases 6–7 must use. They must never substitute the currently open work order for the record's stored work-order ID.

## Explicit local discard

The photo screen may list temporary photos bound to the current work occurrence.

Discard behavior:

1. operator selects one exact local photo record;
2. confirmation shows that photo's work-order context;
3. confirmation removes only that record's app-private image and metadata;
4. failure to delete either part is surfaced; no Drive action is attempted.

No automatic age-based cleanup is added in Phase 5.

## Failure behavior

- No selected work order → no camera launch.
- Metadata cannot persist → no camera launch.
- Image reservation cannot be created → no camera launch.
- No camera activity can handle capture → preserve/clean the prelaunch reservation according to whether image data exists; report the failure.
- RESULT_OK with zero bytes → do not report success.
- Unexpected cancellation with image bytes → preserve the image.
- Restart with image bytes → preserve/reconcile to waiting.
- Record parse failure → do not delete the paired image automatically; surface the record as needing inspection rather than destroying potentially valuable data.
- Changing current folder selection never rewrites existing pending-photo destination fields.

## Automated coverage plan

Focused JVM coverage must include:

- unique local photo IDs/filenames;
- initial `CAPTURING` persistence before capture;
- exact address/work-order ID binding;
- record round-trip persistence;
- restart/repository reload retains waiting photo metadata;
- interrupted `CAPTURING` + non-empty file reconciles to `WAITING`;
- interrupted `CAPTURING` + empty file removes only the empty reservation;
- cancelled capture with bytes is preserved;
- successful capture requires non-empty bytes;
- explicit discard removes only selected local record/file;
- one photo operation does not mutate another record;
- corrupt metadata never causes automatic paired-image deletion;
- filename/path derivation stays within the controlled pending-photo directory.

Final runtime head must pass the complete repository unit suite, debug APK build, and Android launch smoke test.

## Camera reality gate

The testing contract states camera behavior cannot be proven entirely by JVM tests.

While the operator's phone is unavailable, CI/emulator may prove installation, app launch, photo-screen launch, and any camera intent path the emulator supports. It must not be represented as full real-device proof if actual image capture/orientation cannot be demonstrated.

When an Android phone is available, use only a safe test work order under `FIELD PHOTO PREP TEST` and verify:

1. select exact test address/work order;
2. launch Photo Capture and confirm displayed bound hierarchy is correct;
3. take one ordinary still photo offline/airplane-mode if practical;
4. return to app and confirm it appears as waiting;
5. fully close/reopen app and confirm the same waiting photo remains;
6. switch to another work order and confirm the first photo remains bound to the original work-order identity;
7. take a second photo and confirm both records remain independent;
8. explicitly discard one disposable test photo and prove the other remains;
9. inspect orientation/usability of the retained full-resolution test image when a viewing surface is available or during Phase 6 preparation testing;
10. no Drive file is created by Phase 5.

## Merge status

Implementation authorized by the user's instruction to continue while the primary phone is unavailable.

Pre-merge status: **not approved**. Level 3 merge remains blocked until the required camera reality gate is completed and the operator explicitly approves the merge.
