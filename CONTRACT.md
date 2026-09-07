# Field Photo Prep Contract

This contract protects behavior the user has approved. It does not require features that have not yet been built.

## 1. App purpose

Field Photo Prep is an Android field-work app for taking job photos and sending prepared copies to the correct job folder in the user's Google Drive.

Google Drive is the long-term photo store. Field Photo Prep is not a second photo library.

The core operator flow is:

**Open app → choose an existing Drive job folder or create a job folder → take photos → send them to that exact Drive folder.**

## 2. Job and folder identity

1. A job is represented by one Google Drive destination folder plus only the local app metadata needed to reopen and use that destination efficiently.
2. The user selects one approved master Drive folder for the app's field-work folders.
3. Google Drive is the source of truth for which job folders currently exist under that master folder.
4. The app may read the folder metadata needed to show existing job folders, including folder name and Drive folder ID.
5. Before creating a new job folder, the app must check the approved master folder for an existing usable folder with the requested name.
6. If exactly one matching existing folder is found, the app reuses that folder instead of creating a duplicate.
7. If more than one matching folder exists, the app must require the operator to choose the intended folder. It must not guess.
8. If no matching folder exists, the app may create one under the approved master folder.
9. Google Drive folder ID is the permanent destination identity. Folder name is display information only.
10. A renamed Drive folder remains the same destination when its Drive folder ID is unchanged.
11. Two folders with the same visible name are not the same destination.
12. The app must not guess a destination from a folder name when a stored folder ID exists.
13. Reopening the app or reopening a job must not create a second Drive folder merely because the app restarted.
14. If a stored destination folder can no longer be accessed, the app must stop the affected upload and report the problem rather than silently creating or choosing another folder.
15. The app must not move, rename, delete, or change sharing permissions on an existing Drive folder unless that exact behavior is separately approved.

## 3. Photo capture and temporary protection

1. Photos may be taken inside the app using the device camera.
2. The app does not keep successfully uploaded photos as a permanent local photo library.
3. A newly captured photo must be retained temporarily until its Drive upload is confirmed or the operator explicitly discards it.
4. A failed preparation, authentication, network request, or Drive upload must not destroy a photo that has not yet been confirmed in Drive.
5. The job destination is bound to the photo when the photo is accepted for that job. Later navigation to another job must not redirect an already captured photo.
6. The app must clearly distinguish photos that are waiting, uploading, uploaded, or failed while local temporary state still exists.
7. A photo may not be shown as uploaded until Google Drive has confirmed creation of the destination file.
8. After confirmed Drive upload and successful local status update, the app may automatically remove the temporary local image data for that photo.
9. The app may retain only lightweight upload history or remote identity needed for duplicate protection; it need not retain the image itself.
10. Camera capture must not depend on an active internet connection.
11. Initial implementation is still-photo only. Video capture is outside the current approved scope.

## 4. Prepared upload copy

1. The app may create a smaller prepared copy for Drive upload to reduce field data usage and upload time.
2. Preparation may use temporary local storage only for as long as needed to safely complete or retry the upload.
3. Changing compression or resize settings later must not alter photos already confirmed in Drive.
4. The prepared copy must remain visually usable for field-service documentation.
5. Upload preparation must preserve correct photo orientation.
6. A failed prepared-copy creation must stop that photo's upload and leave enough recoverable temporary data to retry or recapture safely.

## 5. Google Drive behavior

1. The app authenticates as the user's Google account; app-created Drive files belong to that authenticated account or its applicable Drive context.
2. The app must use the least Drive access that supports the approved workflow. Broader Drive access may not be added without a documented need and user approval.
3. The app may read the metadata needed to discover existing folders under the approved master folder.
4. The app may create job folders under the approved master folder and upload prepared photo files into the exact selected or stored destination folder.
5. Every Drive write must target an explicit parent folder ID.
6. The app must store or otherwise retain the returned Drive ID for each job folder it creates or the operator selects for reuse.
7. The app must not infer upload success from a local queue update alone.
8. The app must not automatically share uploaded photos or job folders, change inherited permissions, or create public links.
9. Files created under a shared parent may inherit that parent's Drive permissions; the app does not independently broaden sharing.
10. Sign-in or Drive authorization failure must not delete temporary photos that have not yet been confirmed in Drive.

## 6. Upload queue and retry

1. Photos may remain temporarily queued locally when the device is offline or Drive is unavailable.
2. Each queued photo keeps its own immutable local identity and its bound destination folder ID.
3. Retry uses the original queued destination. It must not switch to whichever job is currently open.
4. A retry must not knowingly create a second Drive copy after the app has already confirmed the first upload.
5. If upload result is uncertain, the app must resolve the uncertainty before creating another copy or clearly require operator action rather than guessing.
6. Restarting the app must not discard queued photos that have not been safely uploaded or intentionally removed.
7. A successful upload changes only that photo's queue state.
8. Once Drive success is confirmed and local bookkeeping is safely committed, the temporary image may be removed automatically.

## 7. Photo naming and duplicate protection

1. Uploaded photo filenames must be unique within normal app operation without depending only on the visible job name.
2. A timestamp may be part of the filename, but timestamp alone must not be the only uniqueness protection when collisions are possible.
3. Internal photo identity is separate from the visible Drive filename.
4. Renaming a job folder does not change the identity of photos already bound to that folder.
5. Duplicate prevention must favor preserving a recoverable temporary photo over silently losing a photo.

## 8. Local data and deletion

1. Local app state should remain lightweight and may store master-folder ID, recent job-folder names and IDs, queued-photo records, upload status, remote file identity needed for duplicate protection, and settings.
2. Local job-folder records are a convenience cache, not the authoritative inventory of Drive folders.
3. The app must be able to refresh its folder list from Google Drive rather than assuming its local folder list is complete.
4. Successfully uploaded image data does not need to remain in Field Photo Prep.
5. Google authentication credentials or refresh tokens must not be written into ordinary app backups, logs, or exported job records.
6. Deleting a local remembered-folder entry must not automatically delete its Drive folder or Drive photos.
7. Deleting a Drive photo or Drive folder from inside the app is not part of the initial approved scope.

## 9. Initial user interface scope

The first working app needs only the surfaces required for the core workflow:

- choose or confirm the Google account;
- select the approved master Drive folder;
- refresh and see existing Drive job folders under that master folder;
- choose an existing job folder;
- create a job folder only when the needed folder does not already exist;
- open a job folder;
- take photos for that folder;
- see temporary upload status; and
- retry failed or waiting uploads.

The app should keep this workflow direct and field-friendly rather than presenting one long configuration page.

## 10. Initial non-requirements

The following are not required for the first working version unless separately approved:

- permanent in-app photo library;
- workbook import;
- Free Map Router integration;
- automatic route creation;
- photo labels or watermarks;
- before/after categories;
- automatic Drive sharing changes;
- video capture;
- background location tracking;
- OCR;
- AI photo classification; or
- complicated nested folder rules beyond the approved master folder → job folder structure.

## 11. Safety priority

When two behaviors conflict, preserve any photo not yet confirmed in Drive and preserve its exact Drive destination identity before optimizing convenience, speed, cleanup, or storage use. After confirmed Drive storage, the app should favor removing unnecessary local image copies rather than becoming a second photo archive.
