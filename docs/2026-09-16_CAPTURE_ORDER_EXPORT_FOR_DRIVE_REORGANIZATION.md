# Capture Order Export for Drive Reorganization — 2026-09-16

## Change class

Level 2 runtime UI/read-only local-history feature. This branch performs no Google Drive rename, move, delete, upload, create, permission, destination, or retry operation.

## Problem

A current field job has 111 confirmed Drive photos whose visible Drive filenames do not express the order in which the photos were captured. Drive upload timestamps are not authoritative capture timestamps, and the operator explicitly does not want the order guessed.

## Approved behavior

- Use FPP's persisted immutable `createdAtEpochMs` plus photo ID ordering as the authoritative capture sequence.
- Export that sequence only when every local photo record for the exact open address/work-order binding is durably `UPLOADED` with confirmed remote identity.
- Reuse the existing Photos-screen bulk-reconciliation button surface after the global UNCERTAIN backlog reaches zero, changing it to `Copy Capture Order (N)` for a fully confirmed open work order.
- Copy a machine-readable manifest to the Android clipboard. The manifest includes sequence number, capture timestamp, local photo ID, and deterministic Drive filename.
- Do not put provider folder IDs or confirmed remote file IDs into the clipboard manifest.
- Drive remains unchanged by this feature.

## Owning files

- `CaptureOrderManifest.java`: validation, deterministic sort, manifest generation.
- `PhotoCaptureActivity.java`: exact-open-work-order filtering, clipboard copy, and button presentation.
- `CaptureOrderManifestTest.java`: ordering and fail-closed validation.
- `app/build.gradle`: internal version identification only.

## Protected behavior

- No Drive write path is added.
- No upload/retry/reconciliation semantics are changed.
- No photo record or stored provider identity is modified.
- UNCERTAIN records continue to require reconciliation and block capture-order export for their work order.
- Capture order is never derived from Drive upload time, file list order, or visible folder order.
- The manifest is read-only evidence for a separately authorized Drive-side rename operation.

## Verification

Focused tests must prove:

1. records sort by persisted capture-created timestamp, then immutable photo ID for an exact tie;
2. deterministic remote filename matches the existing upload naming rule;
3. provider/remote IDs are omitted from clipboard text;
4. any non-UPLOADED record fails closed;
5. any wrong address/work-order binding fails closed.

Final runtime head must pass the repository CI suite before the APK is offered for device use.

## Current-job workflow

1. Resolve the 820 Meta UNCERTAIN backlog through the existing strict bulk reconciliation path.
2. Confirm the work order shows all 111 records as uploaded and zero unresolved uncertainty.
3. Install the capture-order-export internal build.
4. Open the exact 820 Meta work order and use `Copy Capture Order (111)`.
5. Paste the manifest into the working chat.
6. Compare all manifest filenames to the exact Drive folder before any rename.
7. If and only if every mapping is unique and complete, rename the existing Drive files in place with sequence prefixes. No copies or moves are required.
