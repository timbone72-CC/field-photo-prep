# Field Photo Prep Regression Checklist

Use only the sections affected by the change. This checklist is not a requirement to retest every feature after every edit.

## A. App launch and folder state

- [ ] App opens without losing stored master-folder identity or useful recent folder mappings.
- [ ] App can refresh the current address-folder list from the approved Google Drive-backed document tree.
- [ ] Previously linked address and work-order folders still resolve to their stored provider/remote destination identities.
- [ ] Reopening a linked address or work order does not create a second Drive folder.
- [ ] Switching between addresses or work orders does not change already queued photos' stored destinations.

## B. Android provider access and master folder

- [ ] The system document picker can access the intended Google Drive provider/account context.
- [ ] The approved master folder can be selected/confirmed and persisted tree access is retained.
- [ ] Stored master-tree URI and provider document identity survive app restart.
- [ ] Renaming the master folder does not break identity when its provider document ID is unchanged.
- [ ] Loss or revocation of persisted tree access stops affected Drive work clearly rather than silently choosing another provider, account, or same-named folder.
- [ ] The Android workflow does not depend on app-managed Google OAuth access or refresh tokens.

## C. Address-folder discovery and creation

- [ ] Refresh shows the actual usable address folders under the approved master folder.
- [ ] Existing address-folder names and provider document IDs come from the document provider rather than only local app memory.
- [ ] Requesting an address name that has exactly one existing match reuses that folder.
- [ ] Reusing an existing address folder stores/retains that folder's provider document ID.
- [ ] No duplicate address folder is created when one usable exact match already exists.
- [ ] Multiple same-named address matches require operator choice and are never guessed.
- [ ] No-match creation creates exactly one address folder under the approved master folder.
- [ ] Absence-based creation fails closed if provider state is loading, stale, inconsistent, or otherwise not authoritative enough.
- [ ] Failure or ambiguous create result does not blindly create repeated address folders.
- [ ] Address folders are never recycled by the work-order reuse feature.

## D. Work-order folder discovery and creation

- [ ] Selecting an address shows the actual usable work-order folders directly under that address folder.
- [ ] Work-order folders use `Work Order - YYYY-MM-DD` names, for example `Cut Grass - 2026-09-06`.
- [ ] The same work-order name on different dates remains separated into different folders unless the operator intentionally reuses an old one.
- [ ] Different work-order names on the same date remain separate folders.
- [ ] Requesting a dated work-order name with exactly one existing match reuses that folder.
- [ ] Reusing an existing work-order folder stores/retains that folder's provider document ID.
- [ ] No duplicate work-order folder is created when one usable exact match already exists.
- [ ] Multiple same-named work-order matches require operator choice and are never guessed.
- [ ] No-match creation creates exactly one work-order folder under the selected address folder.
- [ ] A work-order folder is never accidentally created at the master-folder root.

## E. Work-order folder reuse

- [ ] When the new dated folder does not exist, the app may offer older folders only under the selected address.
- [ ] Simple reuse candidates match the requested work-order name, for example old `Cut Grass` for new `Cut Grass`.
- [ ] The app obtains provider state safe enough to determine the actual selected old folder contents before deciding whether it is empty.
- [ ] A truly empty old work-order folder can be renamed to the new date without changing its provider document ID.
- [ ] Empty-folder reuse does not change the folder's address parent.
- [ ] A non-empty old folder is never cleared or renamed automatically.
- [ ] Provider loading/stale/uncertain state is never treated as proof that a folder is empty.
- [ ] **Clear & Reuse** shows the full selected hierarchy, exact old folder name, direct child-item count, requested new folder, and child-folder warning where applicable before confirmation.
- [ ] **Clear & Reuse** confirmation is bound to the exact direct-child identity snapshot and stops if that snapshot changes before deletion.
- [ ] Cancelling confirmation changes nothing in Drive.
- [ ] Confirmed **Clear & Reuse** removes only the selected work-order folder's confirmed direct child items.
- [ ] The app verifies authoritative-enough empty state before renaming the selected folder.
- [ ] The renamed folder keeps the same provider document ID.
- [ ] A child-deletion failure stops reuse and leaves the folder visibly incomplete rather than pretending success.
- [ ] A rename failure stops reuse and no new-work photos are sent into that folder.
- [ ] No unrelated Drive file, sibling work-order folder, address folder, or other property is changed.

## F. Camera capture

- [ ] The camera flow opens for the exact selected work occurrence.
- [ ] Real still-photo capture succeeds on a supported physical Android device before camera behavior is called field-proven.
- [ ] Captured orientation is visually usable on the physical-device path.
- [ ] A newly captured photo remains recoverable until Drive upload is confirmed.
- [ ] Camera capture works without an active internet connection.
- [ ] Leaving/cancelling the camera does not discard non-empty captured image data unexpectedly.
- [ ] App/process restart does not discard a non-empty interrupted capture.
- [ ] The photo's stored address/work-order provider identities are fixed before camera launch and do not change with later navigation.

## G. Photo preparation

- [ ] Prepared upload copy is created without losing or modifying the unconfirmed protected original.
- [ ] Automated preparation verification confirms the protected original remains byte-for-byte unchanged.
- [ ] Prepared copy is visually usable for field documentation.
- [ ] Orientation is applied correctly to the prepared output.
- [ ] Large images respect the approved maximum prepared dimensions and smaller images are not upscaled.
- [ ] Repeated preparation for one local photo uses the same derivative identity instead of creating uncontrolled copies.
- [ ] Preparation failure leaves the protected original and its destination binding recoverable.

## H. Upload destination

- [ ] Photo uploads to the exact work-order provider document identity stored on that photo.
- [ ] Changing the currently open address before upload completes does not redirect the photo.
- [ ] Changing the currently open work order before upload completes does not redirect the photo.
- [ ] Uploaded photo does not land in the master folder root by mistake.
- [ ] Uploaded photo does not land directly in the address folder by mistake.
- [ ] Uploaded photo does not land in another same-named work-order folder.
- [ ] Unrelated Drive files/folders remain unchanged.

## I. Upload status and local cleanup

- [ ] Waiting state is distinguishable from uploaded state.
- [ ] Uploading state is distinguishable from uploaded state.
- [ ] Failed state is distinguishable from uploaded state.
- [ ] App marks Uploaded only after confirmed remote creation.
- [ ] Confirmed remote file identity is persisted where required for duplicate protection.
- [ ] Temporary local image data is not removed before remote success and local bookkeeping are both secure.
- [ ] Confirmed uploaded images do not remain indefinitely as an unnecessary in-app photo library.

## J. Offline and retry

- [ ] Capture while offline creates a persistent temporary waiting item.
- [ ] Waiting item survives app/process restart.
- [ ] Reconnecting allows retry to the original stored work-order provider destination identity.
- [ ] One failed photo does not corrupt other queued photos.
- [ ] Retry does not knowingly create a second copy after confirmed upload.
- [ ] Ambiguous upload result is reconciled or surfaced rather than blindly retried.

## K. Unconfirmed-photo protection

- [ ] Document-provider/Drive access failure does not delete an unconfirmed photo.
- [ ] Loss or revocation of the persisted master-tree grant does not delete an unconfirmed photo.
- [ ] Compression/preparation failure does not delete or modify an unconfirmed protected original.
- [ ] App restart does not delete waiting unconfirmed photos.
- [ ] Successful cleanup after confirmed upload does not delete the Drive copy.
- [ ] Checking or unchecking a photo does not itself delete local photo data or change queue state.
- [ ] **Discard Selected** requires explicit confirmation and removes only selected local photos that still pass exact identity and `canDiscardLocally()` guards.
- [ ] `UPLOADING`, `UNCERTAIN`, and `UPLOADED` photos cannot be locally discarded through batch or individual discard.
- [ ] If one selected photo fails whole-batch discard preflight, no selected photo is deleted.
- [ ] A partial local filesystem failure stops discard and leaves later selected photos untouched.

## L. Permissions and destructive behavior

- [ ] Folder discovery reads only what the approved persisted tree workflow requires.
- [ ] App does not create public Drive links automatically.
- [ ] App does not alter Drive sharing permissions automatically.
- [ ] Ordinary discover/create/upload flow does not move/rename/delete existing Drive content.
- [ ] **Drive content deletion** occurs only inside confirmed **Clear & Reuse** for one exact selected work-order folder.
- [ ] Explicit local-photo discard never calls Drive/provider deletion and cannot delete a Drive photo or folder.
- [ ] App does not delete the selected work-order folder itself during **Clear & Reuse**.
- [ ] App does not delete address folders or arbitrary Drive content through the reuse feature.
- [ ] Persisted provider access data and remote identities are not exposed unnecessarily in logs or exported app data.
- [ ] Android runtime does not invent or persist Google OAuth credentials for the SAF workflow.

## M. Minimal field workflow

Run this as the primary end-to-end smoke check once the first working version exists:

1. Open app.
2. Through the Android system document picker, confirm the intended Google Drive provider/account context and approved master folder, then retain the persisted tree grant.
3. Refresh existing address folders from the selected Drive-backed document tree.
4. Choose one existing test address and confirm the app records/reuses its provider document ID without creating another folder.
5. Under that address, refresh existing work-order folders.
6. Choose or create `Cut Grass - 2026-09-06` and confirm exactly one work-order folder exists under the address with its provider identity retained.
7. Take two photos inside that work occurrence.
8. Switch to another address or work order and back while one photo is still waiting/uploading if practical.
9. Confirm both photos arrive in the original `Cut Grass - 2026-09-06` work-order folder identified by the stored provider document ID.
10. Confirm no unconfirmed photo is lost during preparation/upload/retry.
11. After confirmed upload and local bookkeeping, confirm temporary image data can be cleaned up and the Drive copies remain intact.
12. Restart the app, refresh Drive folders, and confirm the existing address and work-order provider identities are found without duplicates.
13. Delete the disposable photos from the test `Cut Grass - 2026-09-06` folder so it is empty, then reuse it as `Cut Grass - 2026-09-13`; confirm the provider document ID stays the same.
14. Add disposable content to that folder, choose **Clear & Reuse** for a later grass-cut date, confirm the hierarchy/warning/count, and verify only that folder's confirmed disposable children are removed before rename.

## N. Initial non-requirements guard

For unrelated changes, confirm the change did not accidentally introduce or require:

- permanent in-app photo library;
- workbook integration;
- Free Map Router integration;
- automatic sharing changes;
- general-purpose Drive deletion outside approved **Clear & Reuse**;
- app-managed Google OAuth tokens for the Android SAF workflow;
- video capture;
- background location tracking;
- OCR/AI processing;
- a separate numeric work-order ID; or
- additional folder nesting beyond master → address → dated work order → photos.