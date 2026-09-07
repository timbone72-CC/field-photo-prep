# Field Photo Prep Contract

This contract protects behavior the user has approved. It does not require features that have not yet been built.

## 1. App purpose

Field Photo Prep is an Android field-work app for taking work photos and sending prepared copies to the correct work-order folder in the user's Google Drive.

Google Drive is the long-term photo store. Field Photo Prep is not a second photo library.

The core operator flow is:

**Open app → choose an address → choose or create a dated work order → take photos → send them to that exact Drive work-order folder.**

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

## 2. Property, work-order, and folder identity

1. One address folder represents one property under the approved master Drive folder.
2. One work-order folder represents one specific work occurrence for that property.
3. Initial work-order folder names use `Work Order - YYYY-MM-DD`, for example `Cut Grass - 2026-09-06`.
4. The date is the local calendar date for that work occurrence.
5. The same property may have different work-order folders on the same date.
6. The same work-order name may recur on different dates and must remain separated by date.
7. The user selects one approved master Drive folder for the app's field-work folders.
8. Google Drive is the source of truth for which address folders and work-order folders currently exist within the approved hierarchy.
9. The app may read folder metadata needed to show existing address and work-order folders, including folder name and Drive folder ID.
10. Before creating an address folder, the app must check the approved master folder for an existing usable address folder with the requested name.
11. Before creating a work-order folder, the app must check the selected address folder for an existing usable folder with the exact requested `Work Order - YYYY-MM-DD` name.
12. If exactly one matching existing folder is found at the relevant level, the app reuses it instead of creating a duplicate.
13. If more than one matching folder exists at the relevant level, the app must require the operator to choose the intended folder. It must not guess.
14. If no matching folder exists, the app may create one under the correct parent folder.
15. Google Drive folder ID is permanent destination identity after a folder is selected or created. Folder name is display and discovery information only.
16. A renamed Drive folder remains the same destination when its Drive folder ID is unchanged.
17. Two folders with the same visible name are not the same destination.
18. The app must not guess a destination from a folder name when a stored folder ID exists.
19. Reopening the app, address, or work order must not create a second Drive folder merely because the app restarted.
20. If a stored destination folder can no longer be accessed, the app must stop the affected upload and report the problem rather than silently creating or choosing another folder.
21. Except for the approved folder-reuse controls below, the app must not move, rename, delete, or change sharing permissions on an existing Drive folder.
22. No additional permanent work-order number or app-generated business identifier is required in the initial model. If same-property, same-work-order, same-date occurrences become a real ambiguity, that case must be designed explicitly rather than guessed around.
23. When the requested dated work-order folder does not exist, the app may offer an older folder for the same address and same work-order name as a reuse candidate.
24. An old work-order folder that is truly empty may be renamed to the new `Work Order - YYYY-MM-DD` name and reused while retaining the same Drive folder ID.
25. Empty-folder reuse may never change the address-folder parent.
26. A non-empty old work-order folder is never cleared, renamed, or reused automatically.
27. The operator may explicitly choose **Clear & Reuse** for a non-empty old work-order folder under the same address. Before destructive action, the app must show the old folder name and the number of child items that will be removed and require confirmation.
28. **Clear & Reuse** removes only the selected work-order folder's child items, confirms that the folder is empty, renames that same folder to the new dated work-order name, and reuses its existing Drive folder ID.
29. Failure to remove every child item, failure to confirm emptiness, or failure to rename must stop the reuse workflow. The app must not begin sending new-work photos into a partially cleared folder.
30. Address folders are not eligible for automatic or **Clear & Reuse** recycling in the initial model.

## 3. Photo capture and temporary protection

1. Photos may be taken inside the app using the device camera.
2. The app does not keep successfully uploaded photos as a permanent local photo library.
3. A newly captured photo must be retained temporarily until its Drive upload is confirmed or the operator explicitly discards it.
4. A failed preparation, authentication, network request, or Drive upload must not destroy a photo that has not yet been confirmed in Drive.
5. The exact work-order folder ID is bound to the photo when the photo is accepted for that work occurrence. Later navigation to another address or work order must not redirect an already captured photo.
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
3. The app may read the metadata needed to discover existing address folders under the approved master folder and existing work-order folders under a selected address folder.
4. The app may create address folders under the approved master folder.
5. The app may create dated work-order folders under the exact selected address folder.
6. The app may upload prepared photo files only into the exact selected or stored work-order folder.
7. Every Drive write must target an explicit parent folder ID.
8. The app must store or otherwise retain the returned Drive ID for each address folder and work-order folder it creates or the operator selects for reuse.
9. The app must not infer upload success from a local queue update alone.
10. The app must not automatically share uploaded photos or folders, change inherited permissions, or create public links.
11. Files created under a shared parent may inherit that parent's Drive permissions; the app does not independently broaden sharing.
12. Sign-in or Drive authorization failure must not delete temporary photos that have not yet been confirmed in Drive.
13. Work-order-folder rename and child deletion are permitted only through the approved empty-folder reuse or confirmed **Clear & Reuse** workflow in Section 2.
14. A folder reuse operation must operate by exact Drive folder ID; visible folder names alone may never authorize deletion or rename.

## 6. Upload queue and retry

1. Photos may remain temporarily queued locally when the device is offline or Drive is unavailable.
2. Each queued photo keeps its own immutable local identity and its bound work-order folder ID.
3. Retry uses the original queued destination. It must not switch to whichever address or work order is currently open.
4. A retry must not knowingly create a second Drive copy after the app has already confirmed the first upload.
5. If upload result is uncertain, the app must resolve the uncertainty before creating another copy or clearly require operator action rather than guessing.
6. Restarting the app must not discard queued photos that have not been safely uploaded or intentionally removed.
7. A successful upload changes only that photo's queue state.
8. Once Drive success is confirmed and local bookkeeping is safely committed, the temporary image may be removed automatically.

## 7. Photo naming and duplicate protection

1. Uploaded photo filenames must be unique within normal app operation without depending only on the visible address, work-order name, or date.
2. A timestamp may be part of the filename, but timestamp alone must not be the only uniqueness protection when collisions are possible.
3. Internal photo identity is separate from the visible Drive filename.
4. Renaming an address folder or work-order folder does not change the identity of photos already bound to that work-order folder ID.
5. Duplicate prevention must favor preserving a recoverable temporary photo over silently losing a photo.

## 8. Local data and deletion

1. Local app state should remain lightweight and may store master-folder ID, recent address-folder names and IDs, recent work-order-folder names and IDs, queued-photo records, upload status, remote file identity needed for duplicate protection, and settings.
2. Local folder records are a convenience cache, not the authoritative inventory of Drive folders.
3. The app must be able to refresh its address and work-order folder lists from Google Drive rather than assuming local folder memory is complete.
4. Successfully uploaded image data does not need to remain in Field Photo Prep.
5. Google authentication credentials or refresh tokens must not be written into ordinary app backups, logs, or exported records.
6. Deleting a local remembered-folder entry must not automatically delete its Drive folder or Drive photos.
7. General-purpose Drive photo, work-order-folder, or address-folder deletion from inside the app is not part of the initial approved scope.
8. The only initial app-driven deletion of existing Drive content is the explicit, confirmed child-item removal required by **Clear & Reuse** for one selected work-order folder.

## 9. Initial user interface scope

The first working app needs only the surfaces required for the core workflow:

- choose or confirm the Google account;
- select the approved master Drive folder;
- refresh and see existing address folders under that master folder;
- choose or create an address folder;
- refresh and see existing work-order folders under that address;
- choose or create a `Work Order - YYYY-MM-DD` folder;
- optionally reuse an old same-work-order folder, including explicit **Clear & Reuse** when the operator chooses a non-empty folder;
- take photos for that work occurrence;
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
- general-purpose Drive deletion or folder cleanup outside the approved work-order-folder reuse flow;
- video capture;
- background location tracking;
- OCR;
- AI photo classification;
- a separate numeric work-order ID; or
- folder nesting beyond approved master folder → address folder → dated work-order folder → photos.

## 11. Safety priority

When two behaviors conflict, preserve any photo not yet confirmed in Drive and preserve its exact work-order-folder destination identity before optimizing convenience, speed, cleanup, or storage use. Destructive Drive reuse must remain operator-initiated, narrowly scoped, and fail closed. After confirmed Drive storage, the app should favor removing unnecessary local image copies rather than becoming a second photo archive.
