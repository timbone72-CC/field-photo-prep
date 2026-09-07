# Field Photo Prep Regression Checklist

Use only the sections affected by the change. This checklist is not a requirement to retest every feature after every edit.

## A. App launch and folder state

- [ ] App opens without losing stored master-folder identity or useful recent folder mappings.
- [ ] App can refresh the current address-folder list from Google Drive.
- [ ] Previously linked address and work-order folders still resolve to their existing Drive destination IDs.
- [ ] Reopening a linked address or work order does not create a second Drive folder.
- [ ] Switching between addresses or work orders does not change already queued photos' destinations.

## B. Google account and master folder

- [ ] Approved Google account can authenticate.
- [ ] Approved master folder can be selected/confirmed.
- [ ] Stored master-folder Drive ID survives app restart.
- [ ] Renaming the master folder does not break identity when its Drive ID is unchanged.
- [ ] Loss of access stops affected Drive work clearly rather than silently choosing another folder.

## C. Address-folder discovery and creation

- [ ] Refresh shows the actual usable address folders under the approved master folder.
- [ ] Existing address-folder names and Drive IDs come from Drive rather than only local app memory.
- [ ] Requesting an address name that has exactly one existing match reuses that folder.
- [ ] Reusing an existing address folder stores/retains that folder's Drive ID.
- [ ] No duplicate address folder is created when one usable exact match already exists.
- [ ] Multiple same-named address matches require operator choice and are never guessed.
- [ ] No-match creation creates exactly one address folder under the approved master folder.
- [ ] Failure or ambiguous create result does not blindly create repeated address folders.

## D. Work-order folder discovery and creation

- [ ] Selecting an address shows the actual usable work-order folders directly under that address folder.
- [ ] Work-order folders use `Work Order - YYYY-MM-DD` names, for example `Cut Grass - 2026-09-06`.
- [ ] The same work-order name on different dates remains separated into different folders.
- [ ] Different work-order names on the same date remain separate folders.
- [ ] Requesting a dated work-order name with exactly one existing match reuses that folder.
- [ ] Reusing an existing work-order folder stores/retains that folder's Drive ID.
- [ ] No duplicate work-order folder is created when one usable exact match already exists.
- [ ] Multiple same-named work-order matches require operator choice and are never guessed.
- [ ] No-match creation creates exactly one work-order folder under the selected address folder.
- [ ] A work-order folder is never accidentally created at the master-folder root.

## E. Camera capture

- [ ] Camera preview opens for the selected work occurrence.
- [ ] Photo capture succeeds on the supported device/emulator path.
- [ ] Captured orientation is correct.
- [ ] A newly captured photo remains recoverable until Drive upload is confirmed.
- [ ] Camera works while offline.
- [ ] Leaving the camera does not discard an unconfirmed accepted photo unexpectedly.

## F. Photo preparation

- [ ] Prepared upload copy is created without losing the unconfirmed recoverable photo.
- [ ] Prepared copy is visually usable for field documentation.
- [ ] Orientation remains correct after preparation.
- [ ] Preparation failure leaves enough recoverable temporary data to retry or recapture safely.

## G. Upload destination

- [ ] Photo uploads to the exact work-order-folder Drive ID bound to that photo.
- [ ] Changing the currently open address before upload completes does not redirect the photo.
- [ ] Changing the currently open work order before upload completes does not redirect the photo.
- [ ] Uploaded photo does not land in the master folder root by mistake.
- [ ] Uploaded photo does not land directly in the address folder by mistake.
- [ ] Uploaded photo does not land in another same-named work-order folder.
- [ ] Unrelated Drive files/folders remain unchanged.

## H. Upload status and local cleanup

- [ ] Waiting state is distinguishable from uploaded state.
- [ ] Uploading state is distinguishable from uploaded state.
- [ ] Failed state is distinguishable from uploaded state.
- [ ] App marks Uploaded only after confirmed remote creation.
- [ ] Confirmed remote file identity is persisted where required for duplicate protection.
- [ ] Temporary local image data is not removed before remote success and local bookkeeping are both secure.
- [ ] Confirmed uploaded images do not remain indefinitely as an unnecessary in-app photo library.

## I. Offline and retry

- [ ] Capture while offline creates a persistent temporary waiting item.
- [ ] Waiting item survives app/process restart.
- [ ] Reconnecting allows retry to the original work-order-folder destination.
- [ ] One failed photo does not corrupt other queued photos.
- [ ] Retry does not knowingly create a second copy after confirmed upload.
- [ ] Ambiguous upload result is reconciled or surfaced rather than blindly retried.

## J. Unconfirmed-photo protection

- [ ] Sign-in failure does not delete an unconfirmed photo.
- [ ] Drive failure does not delete an unconfirmed photo.
- [ ] Compression/preparation failure does not delete an unconfirmed photo.
- [ ] App restart does not delete waiting unconfirmed photos.
- [ ] Successful cleanup after confirmed upload does not delete the Drive copy.

## K. Permissions and destructive behavior

- [ ] Folder discovery reads only what the approved workflow requires.
- [ ] App does not create public Drive links automatically.
- [ ] App does not alter Drive sharing permissions automatically.
- [ ] App does not move/rename/delete existing Drive content during ordinary discover/create/upload flow.
- [ ] App does not delete Drive photos, work-order folders, or address folders through any unapproved path.
- [ ] Authentication material is not exposed in logs or exported app data.

## L. Minimal field workflow

Run this as the primary end-to-end smoke check once the first working version exists:

1. Open app.
2. Confirm Google account and master Drive folder.
3. Refresh existing address folders from Drive.
4. Choose one existing test address and confirm the app records/reuses its Drive ID without creating another folder.
5. Under that address, refresh existing work-order folders.
6. Choose or create `Cut Grass - 2026-09-06` and confirm exactly one work-order folder exists under the address.
7. Take two photos inside that work occurrence.
8. Switch to another address or work order and back while one photo is still waiting/uploading if practical.
9. Confirm both photos arrive in the original `Cut Grass - 2026-09-06` work-order folder.
10. Confirm no unconfirmed photo is lost during upload/retry.
11. After confirmed upload and local bookkeeping, confirm temporary image data can be cleaned up and the Drive copies remain intact.
12. Restart the app, refresh Drive folders, and confirm the existing address and work-order folders are found without duplicates.
13. Create `Cut Grass - 2026-09-13` under the same address and confirm it remains separate from the 2026-09-06 occurrence.

## M. Initial non-requirements guard

For unrelated changes, confirm the change did not accidentally introduce or require:

- permanent in-app photo library;
- workbook integration;
- Free Map Router integration;
- automatic sharing changes;
- video capture;
- background location tracking;
- OCR/AI processing;
- a separate numeric work-order ID; or
- additional folder nesting beyond master → address → dated work order → photos.
