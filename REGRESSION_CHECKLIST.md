# Field Photo Prep Regression Checklist

This checklist verifies approved Field Photo Prep behavior before runtime changes are merged or released. Use only the sections affected by the change during development. Run the complete applicable checklist on the final runtime head before release when required by `TESTING_CONTRACT.md` and `CHANGE_CONTROL_CONTRACT.md`.

## 1. Original-photo protection

For every runtime change that reads, processes, saves, shares, or otherwise touches photos:

- Confirm the selected original files still exist after processing.
- Confirm original filenames are unchanged.
- Confirm original file locations are unchanged.
- Confirm original pixel dimensions are unchanged.
- Confirm original file contents are unchanged.
- Confirm original metadata is unchanged.
- Cancel processing partway through and confirm all originals remain unchanged.
- Force or simulate a processing failure and confirm all originals remain unchanged.
- If output storage is unavailable or full, confirm all originals remain unchanged.

Any failure in this section blocks merge or release.

## 2. Photo selection

- Confirm one photo can be selected.
- Confirm multiple photos can be selected in one batch.
- Confirm a realistic batch of at least 30 photos can be selected and processed without losing or changing an original.
- Confirm cancelling the Android photo picker creates no output and changes no source photo.
- Confirm unsupported or unreadable input is reported clearly and does not stop unrelated valid originals from remaining safe.

## 3. Resize behavior

- Confirm the default resize choice is 60% of the original pixel dimensions.
- Confirm aspect ratio is preserved.
- Confirm portrait images remain portrait.
- Confirm landscape images remain landscape.
- Confirm rotation/orientation is visually correct after processing.
- Confirm a resized copy is visibly smaller in pixel dimensions than its original when the selected percentage is below 100%.
- On representative Galaxy S21 field photos, record original and resized file sizes and confirm the 60% output is materially smaller while retaining visually acceptable field-photo detail.
- Do not require a fixed megabyte target unless later approved testing establishes one.
- Confirm resizing creates a new file rather than modifying the source file.
- If additional resize percentages exist, verify each supported percentage independently.

## 4. Output-file safety

- Confirm resized copies are written only to the approved Field Photo Prep output location or another destination explicitly chosen by the operator.
- Confirm output creation never replaces an original.
- Confirm the original filename is retained when that output filename is available.
- Create an existing output file with the same name and confirm the new copy receives a unique filename instead of overwriting the existing file.
- Repeat the same input more than once and confirm every successful output remains separately accessible.
- Confirm partially written or failed output files are not reported as successful.

## 5. Metadata preservation

Use disposable test photos that contain known metadata.

- Confirm original capture date/time is copied when Android and the source format expose it.
- Confirm GPS/location metadata is copied when Android and the source format expose it.
- Confirm orientation metadata or equivalent visual orientation is preserved correctly.
- Confirm metadata-copy failure does not modify the original.
- Confirm a resized photo can still be created when optional metadata cannot be preserved.
- Confirm the app clearly reports when metadata was not fully preserved.
- Confirm the app never invents GPS, date/time, or other source metadata that was not present or available.

## 6. Batch processing

- Process at least 30 normal Galaxy-style JPEG photos in one batch.
- Confirm each processing run remains identifiable separately from older prepared output.
- Prepare one batch, then prepare a second batch, and confirm the second batch does not silently contain or share photos from the first.
- Confirm the success count matches the number of successfully created copies.
- Confirm failed items are identified without being counted as successful.
- Confirm one failed item does not corrupt or alter other originals.
- Cancel processing after at least one output copy has completed and confirm completed copies remain available, unprocessed originals remain untouched, and the batch is reported as cancelled or incomplete.
- Confirm the app remains responsive enough for the operator to understand that processing is still underway.
- Confirm the operator receives a clear completion result when the batch finishes.

## 7. Android permissions

- Confirm the app requests only the photo/file access needed for the approved workflow.
- Deny photo access and confirm the app fails safely with a clear explanation.
- Confirm denying permission does not crash the app or alter any file.
- Confirm the app does not require unrestricted device-storage access when Android-supported scoped photo/file access can perform the approved workflow.
- Any new permission must match an approved contract change before release.

## 8. Share / Google Drive handoff

- Confirm sharing is initiated by the operator.
- Confirm the Android share workflow receives resized copies, not originals, unless the operator explicitly chooses otherwise in a future approved feature.
- Confirm the normal Share action includes only the current batch or an explicitly selected prepared batch, not older prepared photos.
- Confirm a successfully prepared batch can be shared again without resizing the originals again.
- Confirm Google Drive can be selected from the Android share workflow when installed and available.
- Cancel the share flow and confirm both originals and resized copies remain intact.
- Confirm a share failure does not delete or alter originals or completed resized copies.
- Confirm the app does not require a Google account, remote server, or company-specific account merely to resize photos locally.

## 9. Offline behavior

- Disable network access and confirm local photo selection and resizing still work when Android local-file access is available.
- Confirm no network connection is required to create resized copies.
- Confirm attempting to share to an online destination while offline does not damage originals or completed local copies.

## 10. Installation and launch

For release candidates:

- Install the APK on the supported test Android device.
- Launch the app successfully.
- Confirm the app identifies itself as Field Photo Prep.
- Confirm the first-use workflow can reach photo selection without requiring a subscription, remote account, or company-specific login.
- Confirm upgrading over the previous approved build does not broaden permissions unexpectedly.

## 11. Final release gate

Before approving a runtime release:

- Required automated tests pass on the final runtime head.
- Required build and static checks pass on the final runtime head.
- All applicable checklist sections above pass.
- The diff contains no unrelated changes.
- The recorded rollback point is still available.
- No protected original photo was modified, moved, renamed, overwritten, or deleted during verification.
- Any Level 3 change has explicit operator approval before merge.

A failure involving original-photo safety, data loss, unexpected permissions, overwrite behavior, or silent cross-batch photo mixing is a hard stop. Do not merge, publish, or distribute that build until the failure is corrected and the affected checks pass again.
