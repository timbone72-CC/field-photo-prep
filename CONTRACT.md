# Field Photo Prep Contract

This contract protects behavior the user has approved. It does not require features that have not yet been built.

## 1. App purpose

Field Photo Prep is a field-work app for taking work photos and sending prepared copies to the correct work-order folder in the user's Google Drive.

The current implementation target is Android. A future iOS implementation may use different platform APIs, but it must preserve the same identity, photo-protection, destination, retry, and cleanup rules in this contract.

Google Drive is the long-term photo store. Field Photo Prep is not a second photo library.

The core operator flow is:

**Open app → choose an address → choose or create a dated work order → open the in-app camera → take one or more photos → Done → prepared upload copies are created automatically → select the photos to send → Upload Selected → send those photos to their exact stored Drive work-order folder.**

The initial Drive hierarchy is:

**Approved master folder → Address folder → Work Order + Date folder → Photos**

Example:

```text
HNP
└── 1607 Crestview Drive
    ├── Cut Grass - 2026-09-06
    ├── Remove Trash - 2026-09-06
    └── Winterization - 2026-11-15
```

### Identity terminology

In this core contract, **remote folder identity** means the stable identity returned by the active platform's approved Drive integration after a folder is selected or created. Visible folder names are never permanent identity.

For the current Android implementation, `INTEGRATION_CONTRACT.md` maps remote folder identity to the Android document provider's stable document ID inside the persisted Storage Access Framework (SAF) master-tree grant. The app does not manage Google OAuth tokens for that Android workflow.

## 2. Property, work-order, and folder identity

1. One address folder represents one property under the approved master Drive folder.
2. One work-order folder represents one specific work occurrence for that property.
3. Initial work-order folder names use `Work Order - YYYY-MM-DD`, for example `Cut Grass - 2026-09-06`.
4. The date is the local calendar date for that work occurrence.
5. The same property may have different work-order folders on the same date.
6. The same work-order name may recur on different dates and must remain separated by date.
7. The user selects one approved master Drive folder for the app's field-work folders.
8. Google Drive, as exposed through the approved platform integration, is the source of truth for which address folders and work-order folders currently exist within the approved hierarchy.
9. The app may read folder metadata needed to show existing address and work-order folders, including folder name and stable remote folder identity.
10. Before creating an address folder, the app must check the approved master folder for an existing usable address folder with the requested name.
11. Before creating a work-order folder, the app must check the selected address folder for an existing usable folder with the exact requested `Work Order - YYYY-MM-DD` name.
12. If exactly one matching existing folder is found at the relevant level, the app reuses it instead of creating a duplicate.
13. If more than one matching folder exists at the relevant level, the app must require the operator to choose the intended folder. It must not guess.
14. If no matching folder exists, the app may create one under the correct parent folder.
15. Stable remote folder identity is permanent destination identity after a folder is selected or created. Folder name is display and discovery information only.
16. A renamed Drive folder remains the same destination when its stable remote folder identity is unchanged.
17. Two folders with the same visible name are not the same destination.
18. The app must not guess a destination from a folder name when a stored remote folder identity exists.
19. Reopening the app, address, or work order must not create a second Drive folder merely because the app restarted.
20. If a stored destination folder can no longer be accessed, the app must stop the affected upload and report the problem rather than silently creating or choosing another folder.
21. Except for the approved folder-reuse controls below, the app must not move, rename, delete, or change sharing permissions on an existing Drive folder.
22. No additional permanent work-order number or app-generated business identifier is required in the initial model. If same-property, same-work-order, same-date occurrences become a real ambiguity, that case must be designed explicitly rather than guessed around.
23. When the requested dated work-order folder does not exist, the app may offer an older folder for the same address and same work-order name as a reuse candidate.
24. An old work-order folder that is truly empty may be renamed to the new `Work Order - YYYY-MM-DD` name and reused while retaining the same remote folder identity.
25. Empty-folder reuse may never change the address-folder parent.
26. A non-empty old work-order folder is never cleared, renamed, or reused automatically.
27. The operator may explicitly choose **Clear & Reuse** for a non-empty old work-order folder under the same address. Before destructive action, the app must show the old folder name and the number of child items that will be removed and require confirmation.
28. **Clear & Reuse** removes only the selected work-order folder's child items, confirms that the folder is empty, renames that same folder to the new dated work-order name, and reuses its existing remote folder identity.
29. Failure to remove every child item, failure to confirm emptiness, or failure to rename must stop the reuse workflow. The app must not begin sending new-work photos into a partially cleared folder.
30. Address folders are not eligible for automatic or **Clear & Reuse** recycling in the initial model.

## 3. Photo capture and temporary protection

1. Photos are taken inside the app using the in-app camera flow on supported Android devices.
2. A camera session may capture multiple still photos without returning to the work-order screen after every shutter press. The operator ends that session with **Done**.
3. Every shutter press must reserve a new unique protected-photo identity and app-private original destination before camera bytes are written. A later shot must never overwrite an earlier shot's protected original.
4. Every successfully captured shot is finalized independently into recoverable waiting state before the next shot is accepted.
5. Every shot in one camera session inherits the exact address and work-order remote-folder identity bound to that session. Navigation or UI state must not silently redirect an already captured photo.
6. If the camera callback reports an error but non-empty image data exists in the reserved protected-original file, the app must preserve that data rather than deleting it merely because the callback reported failure.
7. Closing or cancelling a camera session may remove only an unused empty capture reservation. Non-empty captured data must be preserved for inspection/recovery.
8. The in-app camera provides a flash mode control with **Auto**, **On**, and **Off** states. A fresh camera session starts at **Flash Auto**.
9. The in-app camera provides a separate continuous-light **Torch On/Off** control. A fresh camera session starts with **Torch Off**.
10. Flash mode and torch state are session camera controls only; changing them must never alter photo identity, destination identity, queue state, preparation state, or Drive state.
11. A device that does not expose a usable flash unit must disable flash/torch controls rather than failing ordinary camera capture.
12. Leaving the camera must not intentionally leave the torch enabled, and lighting controls must not race an active shutter write.
13. The camera must support both portrait and landscape field use. Rotating the device must keep the live preview usable, reflow camera controls for the available orientation, and preserve the same protected camera session and destination identity.
14. The live preview is the dominant camera surface. Flash/Torch remain compact near the preview edge, **Done** remains obvious, and the primary shutter is presented as a large camera-style control rather than a form button.
15. The camera provides pinch-to-zoom plus a fine zoom slider. The slider is normally hidden and appears while zoom is being adjusted so it does not permanently consume preview space.
16. Quick zoom/lens presets may be shown near the shutter when supported. **1×** always returns to the normal default rear-camera framing; it must not be implemented merely as “minimum zoom.” A **3×** shortcut may appear only when that zoom is supported.
17. An ultra-wide shortcut may appear only when the active Android/CameraX camera stack exposes a real rear-camera path below 1×, either through the default logical camera's reported range or a CameraX-exposed rear camera with a genuine sub-1× intrinsic ratio. The app must not label an ordinary digital view as ultra-wide.
18. Ultra-wide labeling should use the device-reported effective ratio rather than inventing a lens value. If a real ultra-wide camera cannot be selected safely, the app must hide/suppress that shortcut and keep ordinary capture available.
19. CameraX-reported zoom state is authoritative for the live zoom readout and slider position. Zoom requests must remain inside the active camera's supported range.
20. Zoom, lens choice, orientation, flash, and torch are camera-session controls only. They must never alter protected-photo identity, destination identity, queue state, preparation state, Drive state, or the exact work order bound to a captured photo.
21. Zoom/lens controls must not be actively changed by the operator during an active shutter write.
22. Still-capture target rotation must follow the active display orientation so portrait and landscape photos remain visually usable after preparation/upload.
23. The app does not keep successfully uploaded photos as a permanent local photo library.
24. A newly captured photo must be retained temporarily until its Drive upload is confirmed or the operator explicitly discards it.
25. A failed preparation, platform/provider access, network request, or Drive upload must not destroy a photo that has not yet been confirmed in Drive.
26. The exact work-order remote folder identity is bound to the photo when the photo is accepted for that work occurrence. Later navigation to another address or work order must not redirect an already captured photo.
27. The app must clearly distinguish photos that are waiting, uploading, uploaded, failed, or uncertain while local temporary state still exists.
28. A photo may not be shown as uploaded until the approved Drive integration has confirmed creation of the destination file.
29. After confirmed Drive upload and successful local status update, the app may automatically remove the temporary local image data for that photo.
30. The app may retain only lightweight upload history or remote identity needed for duplicate protection; it need not retain the image itself.
31. Camera capture must not depend on an active internet connection.
32. Initial implementation is still-photo only. Video capture is outside the current approved scope.

## 4. Prepared upload copy

1. The app creates a smaller prepared copy for Drive upload to reduce field data usage and upload time while leaving the protected original unchanged until remote success is confirmed.
2. After a captured photo reaches durable waiting state, preparation should begin automatically in the background without requiring a per-photo **Prepare** tap during the normal field workflow.
3. Automatic preparation must not delay or invalidate durable capture. If background preparation cannot start or fails, the protected original and waiting queue state remain recoverable.
4. Automatic preparation runs in a controlled serialized path so multiple full-image transforms do not race each other or the manual preparation fallback.
5. On app/process restart, valid waiting photos that still have protected originals but are missing prepared copies may be queued for preparation again without changing their bound destination identity.
6. Preparation may use temporary local storage only for as long as needed to safely complete or retry the upload.
7. Changing compression or resize settings later must not alter photos already confirmed in Drive.
8. The prepared copy must remain visually usable for field-service documentation.
9. Upload preparation must preserve correct photo orientation.
10. A failed prepared-copy creation must stop that photo's upload and leave enough recoverable temporary data to retry or recapture safely.
11. Preparation must never overwrite the protected original.

## 5. Google Drive behavior and platform access

1. The app uses the platform's approved Drive-access mechanism rather than assuming one authentication implementation across Android and future platforms.
2. On Android, the operator chooses the Google Drive-backed master folder through the system document picker. The app keeps the persisted SAF tree grant and provider document identities; it does not store or manage Google OAuth access/refresh tokens for this workflow.
3. The app must use the least remote-storage access that supports the approved workflow. Broader access may not be added without a documented need and user approval.
4. The app may read the metadata needed to discover existing address folders under the approved master folder and existing work-order folders under a selected address folder.
5. The app may create address folders under the approved master folder.
6. The app may create dated work-order folders under the exact selected address folder.
7. The app may upload prepared photo files only into the exact selected or stored work-order folder.
8. Every Drive write must target an explicit stable parent-folder identity; visible parent names alone are insufficient.
9. The app must store or otherwise retain the returned stable remote identity for each address folder and work-order folder it creates or the operator selects for reuse.
10. The app must not infer upload success from a local queue update alone.
11. The app must not automatically share uploaded photos or folders, change inherited permissions, or create public links.
12. Files created under a shared parent may inherit that parent's Drive permissions; the app does not independently broaden sharing.
13. Loss of platform/document-provider access must not delete temporary photos that have not yet been confirmed in Drive.
14. Work-order-folder rename and child deletion are permitted only through the approved empty-folder reuse or confirmed **Clear & Reuse** workflow in Section 2.
15. A folder reuse operation must operate by exact stable remote folder identity; visible folder names alone may never authorize deletion or rename.

## 6. Upload queue, selection, batch execution, and retry

1. Photos may remain temporarily queued locally when the device is offline or Drive is unavailable.
2. Each queued photo keeps its own immutable local identity and its bound work-order remote folder identity.
3. Retry uses the original queued destination. It must not switch to whichever address or work order is currently open.
4. A retry must not knowingly create a second Drive copy after the app has already confirmed the first upload.
5. If upload result is uncertain, the app must resolve the uncertainty before creating another copy or clearly require operator action rather than guessing.
6. Restarting the app must not discard queued photos that have not been safely uploaded or intentionally removed.
7. A successful upload changes only that photo's queue state.
8. Once Drive success is confirmed and local bookkeeping is safely committed, the temporary image may be removed automatically.
9. The operator may build a manual upload batch by selecting any subset of currently upload-eligible prepared photos for the open work order.
10. Batch selection is UI/session convenience only. Selection state is not durable queue authority and must not change a photo's stored destination or upload state merely because a box is checked or unchecked.
11. **Select All Ready** may select all currently eligible prepared photos for the open work order, and **Clear Selection** may remove that selection without changing photo, queue, or Drive state.
12. A selected normal-upload batch may contain only photos that have a usable prepared copy and are eligible to begin an upload attempt. `CAPTURING`, `UPLOADING`, `UNCERTAIN`, already `UPLOADED`, missing-image, or unprepared photos are not eligible for normal batch upload.
13. One **Upload Selected (N)** action snapshots the selected photo IDs. Each selected photo ID may be attempted at most once during that batch run.
14. A batch must perform at most one Drive upload attempt at a time. “Upload Selected” is one operator action, not permission to issue simultaneous remote creates.
15. Every photo in a batch must use that photo's own already-stored immutable work-order provider identity. The currently visible address/work-order state may not redirect any selected item.
16. A confirmed upload may continue to the next selected photo. A confirmed upload with incomplete local cleanup may also continue because remote success is already durable.
17. A known retry-safe failure that leaves the photo in `FAILED` or pre-attempt `WAITING` state may be reported and the batch may continue to later selected photos.
18. If an attempted photo becomes `UNCERTAIN`, remains `UPLOADING`, disappears from readable queue state, or otherwise has an unverified remote outcome, the batch must stop immediately. No later selected photo may be intentionally attempted.
19. Stopping a batch must leave all later unattempted selected photos in their prior local/queue state.
20. Process death during an active batch does not make the batch selection authoritative after restart. Existing per-photo restart recovery governs the active photo; photos not yet attempted remain unchanged and may be selected again later.
21. The existing individual-photo upload and UNCERTAIN reconciliation paths remain available; batch upload does not weaken or replace them.

## 7. Photo naming and duplicate protection

1. Uploaded photo filenames must be unique within normal app operation without depending only on the visible address, work-order name, or date.
2. A timestamp may be part of the filename, but timestamp alone must not be the only uniqueness protection when collisions are possible.
3. Internal photo identity is separate from the visible Drive filename.
4. Renaming an address folder or work-order folder does not change the identity of photos already bound to that work-order's remote folder identity.
5. Duplicate prevention must favor preserving a recoverable temporary photo over silently losing a photo.
6. A batch must not intentionally include the same local photo identity more than once in one run.
7. Batch convenience must never bypass the per-photo remote identity, provisional identity, confirmation, retry, or uncertainty protections.

## 8. Local data and deletion

1. Local app state should remain lightweight and may store master-tree access/identity, recent address-folder names and identities, recent work-order-folder names and identities, queued-photo records, upload status, remote file identity needed for duplicate protection, and settings.
2. Local folder records are a convenience cache, not the authoritative inventory of Drive folders.
3. The app must be able to refresh its address and work-order folder lists from Google Drive through the approved platform integration rather than assuming local folder memory is complete.
4. Successfully uploaded image data does not need to remain in Field Photo Prep.
5. Platform/provider access material must not be exposed in ordinary logs or exported records. On Android, the app must not invent or persist Google OAuth credentials because the SAF workflow does not use app-managed OAuth tokens.
6. Deleting a local remembered-folder entry must not automatically delete its Drive folder or Drive photos.
7. General-purpose Drive photo, work-order-folder, or address-folder deletion from inside the app is not part of the initial approved scope.
8. The only initial app-driven deletion of existing Drive content is the explicit, confirmed child-item removal required by **Clear & Reuse** for one selected work-order folder.

## 9. Initial user interface scope

The first working app needs only the surfaces required for the core workflow:

- choose or confirm the approved remote-storage context using the platform's supported picker/access flow;
- select the approved master Drive folder;
- refresh and see existing address folders under that master folder;
- choose or create an address folder;
- refresh and see existing work-order folders under that address;
- choose or create a `Work Order - YYYY-MM-DD` folder;
- optionally reuse an old same-work-order folder, including explicit **Clear & Reuse** when the operator chooses a non-empty folder;
- open an in-app camera for the selected work occurrence;
- use a phone-camera-style preview-first layout with a large shutter, obvious **Done**, compact Flash/Torch, and session photo count;
- use portrait or landscape camera orientation without losing the protected work-order session;
- use quick device-supported wide/1×/3× presets where genuinely available, with **1×** as the normal-camera reset;
- pinch to zoom or use the temporary fine zoom slider and live effective zoom readout;
- take multiple photos in one camera session and use **Done** to return once;
- see each captured photo retained separately under the same exact work-order destination identity;
- allow prepared upload copies to be created automatically in the background;
- use a per-photo **Send** checkbox for upload-eligible photos;
- use **Select All Ready**, **Clear Selection**, and a visible selected count;
- use **Upload Selected (N)** to send the chosen subset sequentially while preserving per-photo destination and retry safety;
- keep individual-photo upload/reconciliation controls available; and
- see temporary upload/preparation status.

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
- general-purpose Drive deletion or folder cleanup outside the approved work-order-folder reuse flow;
- video capture;
- background location tracking;
- OCR;
- AI photo classification;
- a separate numeric work-order ID; or
- folder nesting beyond approved master folder → address folder → dated work-order folder → photos.

## 11. Safety priority

When two behaviors conflict, preserve any photo not yet confirmed in Drive and preserve its exact work-order-folder destination identity before optimizing convenience, speed, cleanup, or storage use. Destructive Drive reuse must remain operator-initiated, narrowly scoped, and fail closed. A selected upload batch must stop rather than continue through an uncertain or unverified remote result. After confirmed Drive storage, the app should favor removing unnecessary local image copies rather than becoming a second photo archive.
