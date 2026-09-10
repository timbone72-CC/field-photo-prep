# Phase 6A Implementation Record — Photo Preparation

Date: 2026-09-09

## Goal

Create a smaller upload-ready JPEG from one protected Phase 5 original without changing, deleting, moving, or weakening the recoverability of that original.

## Approved scope

Phase 6A only:

- accept one existing protected `WAITING` photo record from the Phase 5 pending-photo store;
- read the protected original without modifying it;
- read source EXIF orientation and produce an upright prepared image;
- downscale only when needed, with a maximum long edge of 2048 pixels;
- encode the prepared copy as JPEG quality 85;
- write to a temporary prepared file first, sync it, then atomically/replace-move into the final prepared path;
- use deterministic prepared filename `prepared-<photo UUID>.jpg` under app-private `files/prepared_photos/`;
- keep prepared-copy identity derived from the immutable local photo UUID rather than current address/work-order UI state;
- allow preparation to be safely repeated for the same photo;
- run decode/resize/orientation/JPEG preparation off the Android UI thread;
- prevent a second preparation or local discard from racing the active preparation operation;
- verify the original still exists and is byte-for-byte unchanged during automated preparation tests;
- expose only enough UI/status to prepare a selected waiting photo and report original/prepared byte sizes.

Explicitly excluded:

- Google Drive upload;
- upload queue state changes such as `UPLOADING`, `UPLOADED`, or `FAILED`;
- deletion of protected originals after preparation;
- background retry;
- camera capture changes;
- Drive folder creation/reuse/deletion;
- permanent local gallery;
- video, watermarking, OCR, AI, or photo categorization;
- user-configurable compression settings in this phase.

## Dependency and governed base

Phase 6A is intentionally stacked on the tested Phase 5 runtime because preparation consumes Phase 5 protected-photo records and originals.

Exact Phase 5 runtime base / rollback point:

`f7da7ab02169919ec359dd4c85fc9fbd441469ae`

Phase 6A branch:

`feat/phase-6a-photo-preparation`

Phase 6A must not merge to `main` before its Phase 5 dependency is safely resolved.

## Change level

Level 3.

Reason: although the intended operation is non-destructive, this code touches the protected-original photo path. A defect that overwrote or deleted the source could lose an unuploaded field photo.

Explicit operator approval is required before merge after verification and after the dependency chain is safe to merge.

## Storage contract

Protected original remains:

`files/pending_photos/photo-<uuid>.jpg`

Prepared copy:

`files/prepared_photos/prepared-<uuid>.jpg`

Temporary write:

`files/prepared_photos/prepared-<uuid>.jpg.tmp-<random UUID>`

The prepared directory is not exposed through the camera FileProvider path.

Prepared copies are derivative and replaceable. The protected original is authoritative until a later Drive-upload phase confirms remote success and local bookkeeping is secure.

## Image policy

- JPEG output.
- Maximum long edge: 2048 px.
- Never upscale a smaller source.
- JPEG quality: 85.
- Apply source EXIF orientation to pixels before output.
- Output is written upright; no downstream consumer may need the source orientation tag to display it correctly.
- Preserve aspect ratio within integer rounding.

No promise is made that every already-small source becomes smaller in byte count. The purpose is bounded dimensions and practical field-upload size without sacrificing the protected original.

## Background-processing hardening

The initial tested Phase 6A implementation performed preparation synchronously from the photo screen. That was safe for file integrity but could make a large phone photo temporarily freeze the UI. The hardened runtime moves the expensive decode/resize/orientation/JPEG work to a dedicated background thread while keeping all Android view updates on the UI thread.

A process-local `PhotoPreparationGate` owns preparation concurrency:

1. only one photo preparation may own the gate at a time;
2. the gate records the exact local photo identity being prepared;
3. repeated Prepare taps cannot start a second preparation while one is active;
4. Take Photo is blocked while preparation is active so capture and image preparation do not compete for memory;
5. local Discard is blocked while preparation is active and is re-checked again immediately before deletion;
6. the protected photo record and image are re-resolved on the worker immediately before preparation;
7. the gate is released in a `finally` block on success or failure;
8. leaving/recreating the activity does not authorize concurrent preparation because the gate is process-local rather than owned only by one activity instance;
9. process death ends the worker and also clears the process-local gate; the existing atomic temp-write design means an incomplete preparation never replaces the protected original.

The UI remains responsive and explicitly shows `PREPARING` / background-preparation status. No upload state is introduced by this hardening.

## Failure behavior

- Missing/empty original → no prepared success; original metadata remains unchanged.
- Corrupt/undecodable image → fail preparation; do not alter/delete the original.
- Invalid or unsupported image dimensions → fail closed.
- Temporary output write/compression failure → remove only the incomplete temp file when possible; preserve original and any previously valid prepared copy.
- Final move failure → preserve original; do not claim a new prepared copy succeeded.
- Worker-start failure → release the preparation gate and preserve the original.
- Background preparation failure → release the gate, report failure on the UI if the activity still exists, and preserve the original.
- A preparation attempt may never mutate destination address/work-order IDs in the pending-photo record.

## Automated coverage

Focused coverage includes:

- target dimension calculations preserve aspect ratio;
- images at/below 2048 long edge are not upscaled;
- larger landscape and portrait images scale correctly;
- deterministic prepared filename derives from local UUID;
- prepared path cannot escape its controlled directory;
- missing/empty original fails without deleting metadata;
- repeated preparation targets the same derivative identity;
- preparation gate permits only one active preparation;
- a different/wrong photo identity cannot release another photo's preparation ownership;
- blank preparation identity is rejected;
- actual Android image preparation on emulator produces a decodable JPEG;
- actual Android orientation handling rotates an EXIF-oriented source into upright output;
- original file bytes remain unchanged after successful preparation;
- preparation failure leaves the original unchanged.

## Final automated verification

Final hardened runtime head:

`484f6df2ac8aa11762f4696b275f198391f21bf1`

Android CI run `34432245469` passed on that exact runtime head:

- complete JVM/unit suite passed, including the preparation ownership-gate tests;
- debug APK build passed;
- Android emulator instrumentation passed, including actual JPEG decode/resize and EXIF-orientation transformation;
- instrumentation confirmed the protected original remained byte-for-byte unchanged;
- normal Android app launch smoke passed;
- debug APK artifact packaged successfully.

Artifact ID: `10134992252`

Artifact digest:

`sha256:584f7dd71e37ab95de10d6be89f230c218cb0a680e96c7916a318ffd0263ca26`

Test build:

- versionCode `12`
- versionName `0.7.1-phase6a-background-preparation`

## Real-device status

A real Android phone is not required to prove the synthetic image transformation itself if emulator instrumentation proves decode/scale/orientation/output behavior. However, once the operator has a working phone, one ordinary real camera photo should be prepared and visually checked before Phase 6 is ultimately treated as field-proven. The phone test should also confirm the screen remains responsive while preparation runs and that Prepare/Discard/Take Photo cannot race the active preparation.

## Merge status

Implementation and background-processing hardening were authorized by the operator's instruction to continue work while the primary phone is unavailable.

Pre-merge status: **not approved**. Phase 6A is stacked on unmerged Phase 5 and remains unmerged until its dependency and Level 3 approval requirements are satisfied.
