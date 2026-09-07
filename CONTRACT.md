# Field Photo Prep Contract

This contract protects behavior the user has approved. It does not require features that have not yet been built.

## 1. App purpose

Field Photo Prep is an Android field-work app for taking job photos and sending prepared copies to the correct job folder in the user's Google Drive while protecting the captured originals.

The core operator flow is:

**Open app → choose or create job → take photos → photos are prepared and sent to that job's Drive folder.**

## 2. Job and folder identity

1. A job is represented by one app job record and one Google Drive destination folder.
2. The user selects one approved master Drive folder for the app's field-work folders.
3. New job folders created by the app must be created under that approved master folder unless the user explicitly chooses a different approved parent in a future feature.
4. Google Drive folder ID is the permanent destination identity. Folder name is display information only.
5. A renamed Drive folder remains the same destination when its Drive folder ID is unchanged.
6. Two folders with the same visible name are not the same destination.
7. The app must not guess a destination from a folder name when a stored folder ID exists.
8. Reopening the app or reopening a job must not create a second Drive folder merely because the folder name is the same or because the app restarted.
9. If the stored destination folder can no longer be accessed, the app must stop the affected upload and report the problem rather than silently creating or choosing another folder.
10. The app must not move, rename, delete, or change sharing permissions on an existing Drive folder unless that exact behavior is separately approved.

## 3. Photo capture and original protection

1. Photos may be taken inside the app using the device camera.
2. The app must preserve a local original capture before any destructive preparation step or Drive upload is treated as complete.
3. Compression, resizing, rotation normalization, metadata handling, and upload copies must not destroy the protected original.
4. A failed preparation or upload leaves the protected original available for retry or recovery.
5. The job destination is bound to the photo when the photo is accepted for that job. Later navigation to another job must not redirect an already captured photo.
6. The app must clearly distinguish photos that are waiting, uploading, uploaded, or failed.
7. A photo may not be shown as uploaded until Google Drive has confirmed creation of the destination file.
8. Camera capture must not depend on an active internet connection.
9. Initial implementation is still-photo only. Video capture is outside the current approved scope.

## 4. Prepared upload copy

1. The app may create a smaller prepared copy for Drive upload to reduce field data usage and upload time.
2. The protected original and prepared upload copy are separate assets.
3. Changing compression or resize settings later must not alter already protected originals.
4. The prepared copy must remain visually usable for field-service documentation.
5. Upload preparation must preserve correct photo orientation.
6. A failed prepared-copy creation must stop that photo's upload and report failure without damaging the original.

## 5. Google Drive behavior

1. The app authenticates as the user's Google account; app-created Drive files belong to that authenticated account or its applicable Drive context.
2. The app must use the least Drive access that supports the approved workflow. Broader Drive access may not be added without a documented need and user approval.
3. The app may create job folders under the approved master folder and upload prepared photo files into the exact stored destination folder.
4. Every Drive write must target an explicit parent folder ID.
5. The app must store the returned Drive ID for each created job folder.
6. The app must not infer upload success from a local queue update alone.
7. The app must not automatically share uploaded photos or job folders, change inherited permissions, or create public links.
8. Files created under a shared parent may inherit that parent's Drive permissions; the app does not independently broaden sharing.
9. Sign-in or Drive authorization failure must not delete local photos or local job state.

## 6. Upload queue and retry

1. Photos may remain queued locally when the device is offline or Drive is unavailable.
2. Each queued photo keeps its own immutable local identity and its bound destination folder ID.
3. Retry uses the original queued destination. It must not switch to whichever job is currently open.
4. A retry must not knowingly create a second Drive copy after the app has already confirmed the first upload.
5. If upload result is uncertain, the app must resolve the uncertainty before creating another copy or clearly require operator action rather than guessing.
6. Restarting the app must not discard queued photos that have not been safely uploaded or intentionally removed.
7. A successful upload changes only that photo's queue state.

## 7. Photo naming and duplicate protection

1. Uploaded photo filenames must be unique within normal app operation without depending only on the visible job name.
2. A timestamp may be part of the filename, but timestamp alone must not be the only uniqueness protection when collisions are possible.
3. Internal photo identity is separate from the visible Drive filename.
4. Renaming a job folder does not change the identity of photos already bound to that folder.
5. Duplicate prevention must favor keeping one extra recoverable local item over silently losing a photo.

## 8. Local data and deletion

1. Local app state may store job records, Drive folder IDs, photo queue records, upload status, and settings needed for the approved workflow.
2. Google authentication credentials or refresh tokens must not be written into ordinary app backups, logs, or exported job records.
3. Clearing an upload from the queue is not the same as deleting the protected original unless the user is explicitly told and confirms that behavior.
4. Deleting a local job record must not automatically delete its Drive folder or Drive photos.
5. Deleting a Drive photo or Drive folder from inside the app is not part of the initial approved scope.

## 9. Initial user interface scope

The first working app needs only the surfaces required for the core workflow:

- choose or confirm the Google account;
- select the approved master Drive folder;
- see existing app jobs;
- create a job and its Drive folder;
- open a job;
- take photos for that job;
- see each photo's upload status; and
- retry failed or waiting uploads.

The app should keep this workflow direct and field-friendly rather than presenting one long configuration page.

## 10. Initial non-requirements

The following are not required for the first working version unless separately approved:

- workbook import;
- Free Map Router integration;
- automatic route creation;
- photo labels or watermarks;
- before/after categories;
- automatic Drive sharing changes;
- automatic deletion from the phone after upload;
- video capture;
- background location tracking;
- OCR;
- AI photo classification; or
- complicated nested folder rules beyond the approved master folder → job folder structure.

## 11. Safety priority

When two behaviors conflict, protect the photo and preserve its exact destination identity before optimizing convenience, speed, cleanup, or storage use.
