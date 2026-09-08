# Field Photo Prep Contract

This contract defines the approved behavior of **Field Photo Prep**. It protects field-work photos from accidental loss or alteration while allowing the app to create smaller copies for faster upload and sharing.

The contract describes behavior that is approved now. It does not require features that have not yet been built.

## 1. App purpose

Field Photo Prep is a company-neutral Android app for preparing field-work photos for faster upload.

The initial workflow is:

1. the operator selects one or more existing photos;
2. the app reads those photos as source input;
3. the app creates resized copies using the selected resize percentage;
4. the app preserves supported metadata when available;
5. the app saves the resized copies separately from the originals;
6. the operator may then share or upload those copies through Android's normal sharing/storage workflow.

The app is not tied to HNP or any other vendor.

## 2. Original-photo protection

Original field photos are protected source evidence.

1. An original photo is read-only input.
2. The app must never overwrite an original photo.
3. The app must never resize an original photo in place.
4. The app must never rename an original photo.
5. The app must never move an original photo.
6. The app must never delete an original photo.
7. The app must never replace an original photo with a resized copy.
8. The app must never silently change the original photo's metadata.
9. A failed resize, metadata copy, save, share, cancellation, app crash, or low-storage condition must leave every original photo unchanged.
10. No future cleanup feature may delete originals unless this contract is explicitly changed with operator approval.

## 3. Photo selection

1. The app must support selecting multiple photos in one batch.
2. Selection must use Android-supported photo/file access rather than broad unrestricted storage access when practical.
3. Selecting a photo grants permission to read that photo for processing; it does not grant permission to modify the original.
4. Canceling selection performs no photo processing and changes no files.
5. The app must not silently add photos the operator did not select.

## 4. Resize rules

1. The default resize target is **60% of the original pixel dimensions**.
2. Width and height are both scaled proportionally so the image aspect ratio is preserved.
3. A 4032 x 3024 source photo resized to 60% should produce approximately 2419 x 1814 pixels, subject only to normal integer rounding.
4. The app must not stretch, crop, rotate incorrectly, or otherwise distort the image merely to meet the resize target.
5. Additional operator-selectable resize percentages may be added later.
6. Adding a new resize percentage must not change the original-photo protection rules.
7. The resize percentage affects pixel dimensions, not the original file.
8. The app must create a new encoded image file for the resized copy.
9. The initial implementation should preserve normal field-photo visual quality rather than pursue the smallest technically possible file size.
10. On representative Galaxy S21 field photos, the 60% setting must produce a materially smaller output file while retaining visually acceptable field-photo detail. No fixed megabyte target is required unless later testing supports one.

## 5. Output rules

1. Resized photos are saved as separate copies.
2. Resized copies are written to a separate app-owned **Field Photo Prep** output location.
3. The output location must not be the source photo's original file path.
4. Saving a resized copy must never require moving or replacing the original.
5. If Android prevents saving a requested copy, the app must report the failure rather than alter the source photo.
6. Successfully created copies may remain available after sharing so the operator can retry an upload without resizing the originals again.
7. Automatic deletion of successful resized copies is not part of the initial approved behavior.
8. A future cleanup feature may delete only output files that the app can positively identify as Field Photo Prep-created copies. It must never infer that an original is safe to delete merely from its filename or folder location.

## 6. Filename rules

1. A resized copy should keep the original filename when possible.
2. The app must never overwrite an existing output file merely because the preferred filename already exists.
3. When an output filename already exists, the app must create a unique filename for the new copy.
4. A safe suffix such as `_2`, `_3`, and so on is acceptable.
5. Filename conflict handling must be deterministic enough that each selected source photo receives its own output file.
6. The app must not rename the source photo while resolving an output-name conflict.

## 7. Metadata rules

1. The app should preserve the source photo's original capture date/time in the resized copy when that metadata is available and the output format supports it.
2. The app should preserve GPS/location metadata when it is available and the output format supports it.
3. Other non-destructive EXIF metadata may also be preserved when practical.
4. Metadata preservation applies only to the resized copy; the source metadata must remain untouched.
5. Missing source metadata is not an error.
6. If image resizing succeeds but some available metadata cannot be copied, the resized photo may still be kept.
7. In that case, the app must clearly report that metadata was not fully preserved.
8. The app must not invent GPS coordinates, capture times, camera information, or other metadata that was not available from the source.
9. Image orientation must be handled so the resized copy displays in the same intended orientation as the original.

## 8. Batch-processing rules

1. Each operator-started processing run is one batch, and each selected photo is processed as its own item within that batch.
2. The current batch must remain identifiable separately from older prepared output so sharing or retrying the current job does not silently include photos from an earlier batch.
3. The normal Share action must share only the current batch, or an explicitly selected prepared batch, and must not silently include older prepared photos.
4. One failed photo must not cause already completed resized copies to be deleted.
5. One failed photo should not prevent remaining selected photos from being attempted unless continuing would risk data corruption or the device cannot safely continue.
6. The final result must distinguish successful photos from failed photos.
7. The app must not report a batch as fully successful when one or more selected photos failed.
8. The operator must be able to identify that a failure occurred without inspecting Android system logs.
9. Processing must be designed to avoid loading an entire large batch of full-resolution images into memory at the same time.
10. If the operator cancels after processing has begun, already completed resized copies remain available, unprocessed source photos remain unchanged, and the batch is reported as cancelled or incomplete rather than fully successful.

## 9. Supported files and output format

1. The initial release is intended primarily for ordinary field photos taken on Android phones.
2. JPEG/JPG source photos must be supported.
3. The initial output format should be JPEG/JPG unless a later approved change adds another format.
4. Unsupported or unreadable files must be rejected without modifying them.
5. Adding support for HEIC/HEIF, PNG, RAW, or other formats later must preserve the same original-photo protection rules.

## 10. Android permissions and privacy

1. The initial app must not require a remote server.
2. The initial app must not require a subscription.
3. The initial app must not require a company-specific account.
4. Photo resizing must occur locally on the Android device.
5. The app must request only the Android permissions reasonably necessary for its approved functions.
6. Broad access to all user storage must not be requested when Android's scoped photo/file access can perform the approved workflow.
7. The app must not upload selected photos anywhere automatically.
8. The app must not transmit photo contents, filenames, EXIF metadata, or GPS data to an app-owned remote service in the initial release.
9. Any future feature that introduces automatic network transfer, cloud storage, analytics containing photo information, or expanded storage permissions requires contract review before implementation.

## 11. Sharing and Google Drive

1. The initial approved Google Drive workflow is operator-initiated sharing or storage through Android's normal share/file interfaces.
2. Field Photo Prep does not need a Google account sign-in for the initial release.
3. The app must not automatically upload photos to Google Drive in the background.
4. The app must not require full access to the operator's Google Drive for the initial release.
5. The app hands off resized copies, not originals, for the normal upload workflow unless the operator separately chooses an original outside Field Photo Prep.
6. Failure or cancellation in Google Drive or another receiving app must not alter the original photos.

## 12. Failure and storage-safety rules

1. Before writing a resized copy, the app should use Android storage APIs in a way that avoids exposing a partially written file as a completed result when practical.
2. If available device storage is insufficient, processing must fail safely and report the problem.
3. The app must not delete unrelated files to make space.
4. A failed output write must never fall back to overwriting the original.
5. An unexpected app shutdown must leave original photos unchanged.
6. If a partial output file remains after a failure, it must not be reported as a successful resized photo.
7. Retrying a failed photo must follow the same no-overwrite filename rules as normal processing.

## 13. Installation and release baseline

1. The initial release may be distributed as an APK installed directly on the operator's Android device.
2. Google Play Store publication is not required for the initial release.
3. Release signing material, passwords, tokens, and private keys must never be committed to the repository.
4. Build or release changes that could make existing installations unable to update safely must be treated as high-risk under `CHANGE_CONTROL_CONTRACT.md`.

## 14. Initial release boundary

The first working release is intentionally narrow.

It should provide:

- batch photo selection;
- 60% resize as the default;
- separate resized copies;
- filename conflict protection;
- supported metadata preservation;
- clear success/failure reporting;
- current-batch isolation so older prepared photos are not silently mixed into a new job;
- operator-initiated sharing of resized copies.

The following are outside the initial required scope unless separately approved:

- direct Google Drive API integration;
- automatic creation of vendor/job/address folders in Drive;
- automatic uploads;
- background synchronization;
- photo deletion or cleanup automation;
- photo editing beyond resizing and required orientation handling;
- watermarks;
- job databases;
- vendor-specific workflows;
- cloud accounts or remote servers;
- Play Store publication.

## 15. Contract change rule

A code change may not silently redefine this contract.

If implementation reveals that an approved rule is technically impossible, unsafe, or materially different from expected Android behavior, implementation must stop on that point and report the conflict.

Changing original-photo protection, deletion behavior, storage permissions, automatic upload behavior, metadata privacy, or output ownership requires explicit operator approval before implementation.
