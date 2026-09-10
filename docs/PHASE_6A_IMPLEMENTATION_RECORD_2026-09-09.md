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

## Failure behavior

- Missing/empty original → no prepared success; original metadata remains unchanged.
- Corrupt/undecodable image → fail preparation; do not alter/delete the original.
- Invalid or unsupported image dimensions → fail closed.
- Temporary output write/compression failure → remove only the incomplete temp file when possible; preserve original and any previously valid prepared copy.
- Final move failure → preserve original; do not claim a new prepared copy succeeded.
- A preparation attempt may never mutate destination address/work-order IDs in the pending-photo record.

## Automated coverage

Focused coverage must include:

- target dimension calculations preserve aspect ratio;
- images at/below 2048 long edge are not upscaled;
- larger landscape and portrait images scale correctly;
- deterministic prepared filename derives from local UUID;
- prepared path cannot escape its controlled directory;
- missing/empty original fails without deleting metadata;
- repeated preparation targets the same derivative identity;
- actual Android image preparation on emulator produces a decodable JPEG;
- actual Android orientation handling rotates an EXIF-oriented source into upright output;
- original file bytes remain unchanged after successful preparation;
- preparation failure leaves the original unchanged.

Final runtime head must pass the complete JVM suite, debug build, Android emulator instrumentation for the preparation boundary, normal app launch smoke test, and APK packaging.

## Real-device status

A real Android phone is not required to prove the synthetic image transformation itself if emulator instrumentation proves decode/scale/orientation/output behavior. However, once the operator has a working phone, one ordinary real camera photo should be prepared and visually checked before Phase 6 is ultimately treated as field-proven.

## Merge status

Implementation authorized by the operator's instruction to continue work while the primary phone is unavailable.

Pre-merge status: **not approved**. Phase 6A is stacked on unmerged Phase 5 and remains unmerged until its dependency and Level 3 approval requirements are satisfied.
