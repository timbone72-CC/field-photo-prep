# Phase 9 — Device Feedback Correction — Photo Thumbnails

Date: 2026-09-12

Status: **CORRECTION STAGED — REPLACEMENT DEVICE SMOKE PENDING**

PR: #32

## Device feedback

The first physical Concept 3 build did not visually match the approved concept closely enough. The operator specifically reported that the Photos screen did not show the photo thumbnails that were part of the approved Concept 3 direction.

That feedback is a Phase 9 presentation failure, not a request to change the camera, Drive, queue, upload, retry, reconciliation, cleanup, or destination-identity behavior.

## Correction

The replacement runtime adds:

- a read-only thumbnail decoder for local protected/prepared photo files;
- EXIF-aware thumbnail orientation;
- compact image-card photo rows;
- real thumbnail imagery when a local image copy exists;
- a neutral placeholder only when no local image copy remains, such as after confirmed-success local cleanup;
- compact operator-facing state badges;
- the existing checkbox and photo-selection listener remain the workflow owners;
- the existing sticky **Upload Selected (N)** action remains the batch workflow owner.

No thumbnail code writes to, replaces, deletes, moves, compresses, or mutates the protected original or prepared upload copy.

## Camera lock

`CameraCaptureActivity` remains outside this correction and is unchanged.

## Automation

Runtime with the thumbnail correction: `bb8dd1d4ec02a05de8fdfce1ac4a027bb79271e2`

Android CI run `34695881103`: **PASS**

Artifact ID: `10298781352`

Artifact digest: `sha256:5088e933ffd4df262ce7c80e1f75858ee6a90b6e01f0b8a464374e14b88a88e8`

A follow-up instrumentation assertion was added to require a real local field photo to produce a thumbnail view. A subsequent GitHub Actions dispatch failed before job steps started; that infrastructure-level dispatch is not treated as runtime evidence. The runtime itself is covered by the successful exact-runtime CI above, and the new assertion remains in the branch for the next normal CI execution.

## Remaining gate

Install the replacement internal APK on the primary Samsung phone and verify that existing local photos visibly render as thumbnails and that the resulting Photos screen now matches the approved Concept 3 hierarchy much more closely.
