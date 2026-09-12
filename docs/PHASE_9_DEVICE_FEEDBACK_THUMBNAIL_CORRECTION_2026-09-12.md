# Phase 9 — Device Feedback Corrections

Date: 2026-09-12

Status: **CORRECTION IN PROGRESS — REPLACEMENT DEVICE SMOKE PENDING**

PR: #32

## Device feedback 1 — Photos did not match the approved concept

The first physical Concept 3 build did not visually match the approved concept closely enough. The operator specifically reported that the Photos screen did not show the photo thumbnails that were part of the approved Concept 3 direction.

That feedback is a Phase 9 presentation failure, not a request to change the camera, Drive, queue, upload, retry, reconciliation, cleanup, or destination-identity behavior.

### Thumbnail correction

The replacement runtime added:

- a read-only thumbnail decoder for local protected/prepared photo files;
- EXIF-aware thumbnail orientation;
- compact image-card photo rows;
- real thumbnail imagery when a local image copy exists;
- a neutral placeholder only when no local image copy remains, such as after confirmed-success local cleanup;
- compact operator-facing state badges;
- the existing checkbox and photo-selection listener remain the workflow owners;
- the existing sticky **Upload Selected (N)** action remains the batch workflow owner.

No thumbnail code writes to, replaces, deletes, moves, compresses, or mutates the protected original or prepared upload copy.

Thumbnail runtime: `bb8dd1d4ec02a05de8fdfce1ac4a027bb79271e2`

Android CI run `34695881103`: **PASS**

Artifact ID: `10298781352`

Artifact digest: `sha256:5088e933ffd4df262ce7c80e1f75858ee6a90b6e01f0b8a464374e14b88a88e8`

## Device feedback 2 — Home/Addresses still looked like the old app

The operator then supplied an on-device screenshot showing that the Home/Addresses screen still retained the old structural hierarchy: oversized app title/subtitle, large standalone status line, a large Drive card, a full-width oversized New Address action, tall address cards, raw underscore-heavy Drive folder names, and content crowding the system bars.

The operator's assessment was explicit: **"This looks the same."**

This is treated as a second Phase 9 presentation failure. Merely restyling the old hierarchy is not sufficient.

### Structural Home/Addresses correction

Runtime `d019fc8ce6feac91bd597a9016744d90cbdbde88` replaces the prior presentation-only sizing pass with a compact field-app structure while preserving the original MainActivity listeners and Drive behavior:

- compact app header and subtitle;
- explicit system-bar inset handling so the title/list do not collide with status/navigation bars;
- compact Drive status card instead of the oversized green block;
- connected Drive's folder remains visible while the connection action becomes a quieter Change control;
- compact New Address action rather than a full-width oversized bar;
- compact Properties heading;
- materially shorter property cards;
- friendlier display labels that replace folder-name underscores with spaces without changing the stored Drive folder name or provider identity;
- compact work-order cards using the same presentation language;
- no new Drive calls, no renamed Drive folders, and no persisted-data changes.

A GitHub Actions dispatch for this runtime failed before executable test steps were exposed by the runner. That infrastructure-level dispatch is not treated as passing runtime evidence. A fresh full CI execution is required before another APK is staged.

## Camera lock

`CameraCaptureActivity` remains outside these corrections and is unchanged.

## Remaining gate

Do not stage another device APK until the final corrected runtime completes the full Android CI suite. The next physical smoke should verify that the Home/Addresses screen is unmistakably different from the supplied screenshot and that the Photos screen shows real local thumbnails where local photo bytes remain.
