# Field Photo Prep Regression Checklist

> **Routing scope:** Affected-behavior checklist. Read only the sections mapped by `RULE_INDEX.md` to the surfaces being changed; do not treat the entire checklist as mandatory for unrelated work.

Use only the sections affected by the change. This checklist is not a requirement to retest every feature after every edit.

## A. App launch and folder state

- [ ] App opens without losing stored workspace/company identity or useful recent folder mappings.
- [ ] App can refresh the company list from the approved Google Drive-backed workspace and the current address-folder list from the selected company.
- [ ] Previously linked address and work-order folders still resolve to their stored provider/remote destination identities.
- [ ] Reopening a linked address or work order does not create a second Drive folder.
- [ ] Switching between companies, addresses, or work orders does not change already queued photos' stored destinations.

## B. Android provider access, workspace, and company folder

- [ ] The system document picker can access the intended Google Drive provider/account context.
- [ ] The approved field-work workspace can be selected/confirmed and persisted tree access is retained.
- [ ] Stored workspace-tree URI and provider document identity survive app restart.
- [ ] Direct company folders are discovered from Drive, not only local memory.
- [ ] One exact existing company-name match is reused; multiple exact matches require operator choice.
- [ ] Adding a new company creates exactly one direct child under the workspace after fresh/settled absence proof.
- [ ] Editing a company renames only the exact selected provider ID and keeps that ID unchanged.
- [ ] Company rename is blocked when a different sibling already has the requested exact name.
- [ ] Switching companies clears current address/work-order navigation selection without rewriting queued photo destinations.
- [ ] Renaming the workspace or selected company does not break identity when its provider document ID is unchanged.
- [ ] Loss or revocation of persisted tree access stops affected Drive work clearly rather than silently choosing another provider, account, or same-named folder.
- [ ] The Android workflow does not depend on app-managed Google OAuth access or refresh tokens.

## C. Address-folder discovery and creation

- [ ] Refresh shows the actual usable address folders under the exact selected company folder.
- [ ] Existing address-folder names and provider document IDs come from the document provider rather than only local app memory.
- [ ] Requesting an address name that has exactly one existing match reuses that folder.
- [ ] Reusing an existing address folder stores/retains that folder's provider document ID.
- [ ] No duplicate address folder is created when one usable exact match already exists.
- [ ] Multiple same-named address matches require operator choice and are never guessed.
- [ ] No-match creation creates exactly one address folder under the exact selected company folder.
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
- [ ] A work-order folder is never accidentally created at the workspace root or company-folder root.

## Work-order correction

- [ ] Edit Work Order is visible on the Work Orders screen, enabled for a selected writable folder.
- [ ] Edit prefills the existing name/date; either or both can change without clearing photos.
- [ ] Cancel leaves folder and selection unchanged; invalid name/date stays in the dialog.
- [ ] Sibling name/date collision, changed source name, missing ID and unsettled provider block rename.
- [ ] Save verifies the original folder ID and parent; photos, queued destinations and capture sequence remain unchanged.
- [ ] Reopening the app shows the corrected folder name/date under the same identity.
- [ ] Empty/error folder discovery has a visible explanation; no silent blank list.

## E. Work-order folder reuse

- [ ] The operator can select an existing dated work order and start **Clear & Reuse** directly without retyping the work-order name.
- [ ] **Clear & Reuse** asks for the new date after the old work order is selected.
- [ ] The new reuse name keeps the exact selected work-order name and uses the same or a later date.
- [ ] Ordinary **Add Work Order** remains non-destructive and does not silently route into Clear & Reuse.
- [ ] Provider loading/stale/uncertain state is never treated as authority for photo removal or rename.
- [ ] Confirmation shows the exact selected property/work order, Drive photo count, and new date.
- [ ] Cancelling confirmation changes nothing in Drive.
- [ ] Confirmation is bound to the exact direct-photo provider-identity snapshot and stops if that photo set changes before removal.
- [ ] Confirmed **Clear & Reuse** removes only direct image/photo files from the selected work-order folder.
- [ ] Child folders and non-image files in the selected work-order folder remain unchanged.
- [ ] The app verifies authoritative-enough zero-photo state before renaming the selected folder.
- [ ] A zero-photo selected folder uses the same Clear & Reuse flow and can be date-reset without a separate empty-reuse control.
- [ ] The renamed folder keeps the same provider document ID and address parent.
- [ ] The first photo captured in the reused occurrence starts at `001`.
- [ ] Confirmed upload history from the prior occurrence does not appear in the new occurrence's Photos list.
- [ ] A photo-removal failure stops reuse and leaves the folder visibly incomplete rather than pretending success.
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
- [ ] **Drive photo deletion** occurs only inside confirmed **Clear & Reuse** for one exact selected work-order folder or inside the explicit operator-confirmed Phase 13 archive/delete cleanup for one exact address.
- [ ] Clear & Reuse deletion is limited to direct image/photo files; child folders and non-image files are preserved.
- [ ] Explicit local-photo discard never calls Drive/provider deletion and cannot delete a Drive photo or folder.
- [ ] App does not delete the selected work-order folder itself during **Clear & Reuse**.
- [ ] App does not delete address folders, work-order folders, child folders, non-photo files, or arbitrary Drive content through Clear & Reuse or Phase 13 lifecycle cleanup.
- [ ] Persisted provider access data and remote identities are not exposed unnecessarily in logs or exported app data.
- [ ] Android runtime does not invent or persist Google OAuth credentials for the SAF workflow.

## L2. Phase 13 property lifecycle / archive cleanup

- [ ] Home shows active properties only and sorts them by most recent real work activity.
- [ ] Searching, browsing, opening, refreshing, archiving, and reactivating do not falsely update `Last used`.
- [ ] Search can find both active and archived properties by address text.
- [ ] Archived properties are absent from normal Home and can be reactivated using the same provider address identity.
- [ ] Manual Delete Address suppresses the property from normal Home/search state on the installation and does not recursively delete Drive folder structure.
- [ ] Deliberately re-adding/choosing an exact locally deleted Drive address may clear suppression and reuse the same provider identity instead of creating a duplicate.
- [ ] Archive/Delete cleanup enumerates the exact direct work-order folders under the exact target address.
- [ ] Cleanup removes only confirmed direct image/photo files from those work-order folders.
- [ ] Address/work-order/child folders and non-image files remain unchanged.
- [ ] Confirmation is bound to the exact address/work-order/photo provider-identity snapshot and deletion stops if the work-order set or any photo set changes before mutation.
- [ ] Any affected `CAPTURING`, `WAITING`, `UPLOADING`, `FAILED`, `UNCERTAIN`, or otherwise protected/unresolved photo blocks cleanup before Drive deletion begins.
- [ ] Partial/ambiguous remote deletion stops further affected mutation and requires refresh/inspection rather than blind retry.
- [ ] Inactivity threshold is configurable as 30 days / 60 days / 90 days / 6 months / 1 year / Never, defaults to 90 days, and never auto-archives or auto-deletes.
- [ ] Lifecycle state is device-local/provider-context-bound and remains excluded from Android backup/transfer under the existing Phase 10F rules.
- [ ] No company/address/work-order provider ID or queued-photo destination is rewritten by archive, reactivate, search, review, or cleanup.
- [ ] After a fully verified Archive/Delete, old confirmed local photo records and residual copies for exactly the cleaned dated work-order provider IDs are retired; after Reactivate or deliberate re-add, each cleared work order starts at capture number `001`.
- [ ] Failed or blocked Archive/Delete, including a target WAITING photo or partial local-history retirement, never resets active capture numbering or deletes protected/unrelated local records.

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
13. Select the disposable `Cut Grass - 2026-09-06` work order, choose **Clear & Reuse**, select `2026-09-13`, and confirm the same provider document ID is retained and the next captured photo would start at `001`.
14. For the next reuse, place disposable photos plus one non-photo file or child folder in that test work order. Choose **Clear & Reuse** for a later date and verify only the confirmed photos are removed from Drive, the non-photo item remains, and the same folder identity is renamed.

## N. Initial non-requirements guard

For unrelated changes, confirm the change did not accidentally introduce or require:

- permanent in-app photo library;
- workbook integration;
- Free Map Router integration;
- automatic sharing changes;
- general-purpose Drive deletion outside approved **Clear & Reuse** or Phase 13 address-scoped photo cleanup;
- app-managed Google OAuth tokens for the Android SAF workflow;
- video capture;
- background location tracking;
- OCR/AI processing;
- a separate numeric work-order ID; or
- additional folder nesting beyond workspace → company → address → dated work order → photos.
### Same-date Clear & Reuse

Clear & Reuse may explicitly clear the exact selected folder for its current date after the same deletion confirmation. A sibling collision blocks the operation without changing selection. It never silently selects another folder. Once remote photo absence and unchanged folder identity/name are verified, remove only the selected WO's confirmed uploaded history, keep all unrelated/protected records, and reset its next capture to 001. Failed retirement is an incomplete reuse, not success. Edit Work Order continues to keep photos and numbering.
