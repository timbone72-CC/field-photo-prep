# Field Photo Prep Regression Checklist

Use only the sections affected by the change. This checklist is not a requirement to retest every feature after every edit.

## A. App launch and job state

- [ ] App opens without losing existing job records.
- [ ] Previously linked jobs still show their existing Drive destination.
- [ ] Reopening a job does not create a second Drive folder.
- [ ] Switching between jobs does not change already queued photos' destinations.

## B. Google account and master folder

- [ ] Approved Google account can authenticate.
- [ ] Approved master folder can be selected/confirmed.
- [ ] Stored master-folder Drive ID survives app restart.
- [ ] Renaming the master folder does not break identity when its Drive ID is unchanged.
- [ ] Loss of access stops affected Drive work clearly rather than silently choosing another folder.

## C. Create job folder

- [ ] Creating a new job creates exactly one Drive folder under the approved master folder.
- [ ] App stores the Drive folder ID returned for that job.
- [ ] Opening that job again reuses the same folder ID.
- [ ] Same/similar visible names do not cause the app to guess between different folders.
- [ ] Failure or ambiguous result does not blindly create repeated folders.

## D. Camera capture

- [ ] Camera preview opens for the selected job.
- [ ] Photo capture succeeds on the supported device/emulator path.
- [ ] Captured orientation is correct.
- [ ] Protected local original exists before upload is considered complete.
- [ ] Camera works while offline.
- [ ] Leaving the camera does not discard an accepted photo unexpectedly.

## E. Photo preparation

- [ ] Prepared upload copy is created without changing the protected original.
- [ ] Prepared copy is visually usable for field documentation.
- [ ] Orientation remains correct after preparation.
- [ ] Preparation failure leaves the original intact and reports failure.

## F. Upload destination

- [ ] Photo uploads to the exact Drive folder ID bound to that photo.
- [ ] Changing the currently open job before upload completes does not redirect the photo.
- [ ] Uploaded photo does not land in the master folder root by mistake.
- [ ] Uploaded photo does not land in another same-named job folder.
- [ ] Unrelated Drive files/folders remain unchanged.

## G. Upload status

- [ ] Waiting state is distinguishable from uploaded state.
- [ ] Uploading state is distinguishable from uploaded state.
- [ ] Failed state is distinguishable from uploaded state.
- [ ] App marks Uploaded only after confirmed remote creation.
- [ ] Confirmed remote file identity is persisted where required for duplicate protection.

## H. Offline and retry

- [ ] Capture while offline creates a persistent waiting item.
- [ ] Waiting item survives app/process restart.
- [ ] Reconnecting allows retry to the original destination folder.
- [ ] One failed photo does not corrupt other queued photos.
- [ ] Retry does not knowingly create a second copy after confirmed upload.
- [ ] Ambiguous upload result is reconciled or surfaced rather than blindly retried.

## I. Original-photo protection

- [ ] Sign-in failure does not delete the original.
- [ ] Drive failure does not delete the original.
- [ ] Compression/preparation failure does not delete the original.
- [ ] App restart does not delete waiting originals.
- [ ] Queue clearing, when implemented, does not silently delete originals.

## J. Permissions and destructive behavior

- [ ] App does not create public Drive links automatically.
- [ ] App does not alter Drive sharing permissions automatically.
- [ ] App does not move/rename/delete existing Drive content during ordinary create/upload flow.
- [ ] App does not delete Drive photos or job folders through any unapproved path.
- [ ] Authentication material is not exposed in logs or exported app data.

## K. Minimal field workflow

Run this as the primary end-to-end smoke check once the first working version exists:

1. Open app.
2. Confirm Google account and master Drive folder.
3. Create one test job.
4. Confirm exactly one job folder appears under the test master folder.
5. Take two photos inside the job.
6. Switch away from the job and back while one photo is still waiting/uploading if practical.
7. Confirm both photos arrive in the original job folder.
8. Confirm both protected originals still exist locally according to the app's retention model.
9. Restart the app.
10. Reopen the job and confirm it still points to the same Drive folder and creates no duplicate.

## L. Initial non-requirements guard

For unrelated changes, confirm the change did not accidentally introduce or require:

- workbook integration;
- Free Map Router integration;
- automatic sharing changes;
- phone-photo deletion after upload;
- video capture;
- background location tracking;
- OCR/AI processing; or
- additional nested Drive folder rules.
