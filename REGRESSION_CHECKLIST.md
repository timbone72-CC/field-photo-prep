# Field Photo Prep Regression Checklist

Use only the sections affected by the change. This checklist is not a requirement to retest every feature after every edit.

## A. App launch and folder state

- [ ] App opens without losing stored master-folder identity or useful recent folder mappings.
- [ ] App can refresh the current job-folder list from Google Drive.
- [ ] Previously linked folders still resolve to their existing Drive destination IDs.
- [ ] Reopening a linked job does not create a second Drive folder.
- [ ] Switching between jobs does not change already queued photos' destinations.

## B. Google account and master folder

- [ ] Approved Google account can authenticate.
- [ ] Approved master folder can be selected/confirmed.
- [ ] Stored master-folder Drive ID survives app restart.
- [ ] Renaming the master folder does not break identity when its Drive ID is unchanged.
- [ ] Loss of access stops affected Drive work clearly rather than silently choosing another folder.

## C. Existing folder discovery and creation

- [ ] Refresh shows the actual usable job folders under the approved master folder.
- [ ] Existing folder names and Drive IDs come from Drive rather than only local app memory.
- [ ] Requesting a folder name that has exactly one existing match reuses that folder.
- [ ] Reusing an existing folder stores/retains that folder's Drive ID.
- [ ] No duplicate folder is created when one usable match already exists.
- [ ] Multiple same-named matches require operator choice and are never guessed.
- [ ] No-match creation creates exactly one Drive folder under the approved master folder.
- [ ] Failure or ambiguous create result does not blindly create repeated folders.

## D. Camera capture

- [ ] Camera preview opens for the selected job folder.
- [ ] Photo capture succeeds on the supported device/emulator path.
- [ ] Captured orientation is correct.
- [ ] A newly captured photo remains recoverable until Drive upload is confirmed.
- [ ] Camera works while offline.
- [ ] Leaving the camera does not discard an unconfirmed accepted photo unexpectedly.

## E. Photo preparation

- [ ] Prepared upload copy is created without losing the unconfirmed recoverable photo.
- [ ] Prepared copy is visually usable for field documentation.
- [ ] Orientation remains correct after preparation.
- [ ] Preparation failure leaves enough recoverable temporary data to retry or recapture safely.

## F. Upload destination

- [ ] Photo uploads to the exact Drive folder ID bound to that photo.
- [ ] Changing the currently open job before upload completes does not redirect the photo.
- [ ] Uploaded photo does not land in the master folder root by mistake.
- [ ] Uploaded photo does not land in another same-named job folder.
- [ ] Unrelated Drive files/folders remain unchanged.

## G. Upload status and local cleanup

- [ ] Waiting state is distinguishable from uploaded state.
- [ ] Uploading state is distinguishable from uploaded state.
- [ ] Failed state is distinguishable from uploaded state.
- [ ] App marks Uploaded only after confirmed remote creation.
- [ ] Confirmed remote file identity is persisted where required for duplicate protection.
- [ ] Temporary local image data is not removed before remote success and local bookkeeping are both secure.
- [ ] Confirmed uploaded images do not remain indefinitely as an unnecessary in-app photo library.

## H. Offline and retry

- [ ] Capture while offline creates a persistent temporary waiting item.
- [ ] Waiting item survives app/process restart.
- [ ] Reconnecting allows retry to the original destination folder.
- [ ] One failed photo does not corrupt other queued photos.
- [ ] Retry does not knowingly create a second copy after confirmed upload.
- [ ] Ambiguous upload result is reconciled or surfaced rather than blindly retried.

## I. Unconfirmed-photo protection

- [ ] Sign-in failure does not delete an unconfirmed photo.
- [ ] Drive failure does not delete an unconfirmed photo.
- [ ] Compression/preparation failure does not delete an unconfirmed photo.
- [ ] App restart does not delete waiting unconfirmed photos.
- [ ] Successful cleanup after confirmed upload does not delete the Drive copy.

## J. Permissions and destructive behavior

- [ ] Folder discovery reads only what the approved workflow requires.
- [ ] App does not create public Drive links automatically.
- [ ] App does not alter Drive sharing permissions automatically.
- [ ] App does not move/rename/delete existing Drive content during ordinary discover/create/upload flow.
- [ ] App does not delete Drive photos or job folders through any unapproved path.
- [ ] Authentication material is not exposed in logs or exported app data.

## K. Minimal field workflow

Run this as the primary end-to-end smoke check once the first working version exists:

1. Open app.
2. Confirm Google account and master Drive folder.
3. Refresh existing job folders from Drive.
4. Choose one existing test job folder and confirm the app records/reuses its Drive ID without creating another folder.
5. Create one different test job whose folder does not already exist and confirm exactly one new Drive folder appears.
6. Take two photos inside one selected test job folder.
7. Switch away from the job and back while one photo is still waiting/uploading if practical.
8. Confirm both photos arrive in the original selected Drive folder.
9. Confirm no unconfirmed photo is lost during upload/retry.
10. After confirmed upload and local bookkeeping, confirm temporary image data can be cleaned up and the Drive copies remain intact.
11. Restart the app, refresh Drive folders, and confirm both existing test folders are found without duplicates.

## L. Initial non-requirements guard

For unrelated changes, confirm the change did not accidentally introduce or require:

- permanent in-app photo library;
- workbook integration;
- Free Map Router integration;
- automatic sharing changes;
- video capture;
- background location tracking;
- OCR/AI processing; or
- additional nested Drive folder rules.
