# Field Photo Prep Contract

> **Routing scope:** Product and runtime invariants. Read the sections selected by `RULE_INDEX.md` when work touches app behavior, identity, photo protection, queue state, destination identity, deletion, or related product boundaries. This file is not mandatory for unrelated documentation/process work.

This contract protects behavior the user has approved. It does not require features that have not yet been built.

## 1. App purpose

Field Photo Prep is a field-work app for taking work photos and sending prepared copies to the correct work-order folder in the user's Google Drive.

The current implementation target is Android. A future iOS implementation may use different platform APIs, but it must preserve the same identity, photo-protection, destination, retry, and cleanup rules in this contract.

Google Drive is the long-term photo store. Field Photo Prep is not a second photo library.

The core operator flow is:

**Open app → choose a company → choose an address → choose or create a dated work order → open the in-app camera → take one or more photos → Done → prepared upload copies are created automatically → select the photos to send → Upload Selected → send those photos to their exact stored Drive work-order folder.**

The initial Drive hierarchy is:

**Approved field-work workspace → Company folder → Address folder → Work Order + Date folder → Photos**

Example:

```text
Photos
├── HNP Jobs
│   └── 1607 Crestview Drive
│       ├── Cut Grass - 2026-09-06
│       └── Winterization - 2026-11-15
└── Tresmolino Jobs
    └── 213 E 9TH ST VICI OK
        └── Initial Secure - 2026-09-21
```

### Identity terminology

In this core contract, **remote folder identity** means the stable identity returned by the active platform's approved Drive integration after a folder is selected or created. Visible folder names are never permanent identity.

For the current Android implementation, `INTEGRATION_CONTRACT.md` maps remote folder identity to the Android document provider's stable document ID inside the persisted Storage Access Framework (SAF) workspace-tree grant. The app does not manage Google OAuth tokens for that Android workflow.

### FPP account identity

The approved account identity model is recorded in `docs/IDENTITY_MODEL_V1.md`.

For account/authentication work, the following rules are contractual:

1. A **User** is a permanent FPP human identity. Email/login address is authentication/profile data and is not the permanent User identity.
2. An **Organization** is the field-service business using FPP. Organization identity is not a Google account, Drive folder, provider document ID, Android device, client company, or Google Workspace subscription.
3. Existing Drive **Company folders** represent client companies under the approved workspace. They are not FPP Organizations. HNP, Tresmolino, Tasre, and future client folders remain Drive-side business data.
4. A **Membership** explicitly joins one User to one Organization. Identity v1 roles are only **Owner** and **Member** unless later field evidence justifies more.
5. FPP authentication and Google Drive authorization are separate. Signing into FPP, including through Google, must not silently grant, select, change, or infer the Android Drive provider/account/workspace.
6. Google Drive access likewise does not automatically grant FPP Organization Membership.
7. The current Android SAF tree grant and workspace/company/property/work-order provider IDs remain platform/provider-context identity. They must not be treated as portable FPP account identity or silently restored/copied into another device/account/provider context.
8. Moving an Organization from the current personal Drive workspace to a later business-controlled Shared Drive changes the local Drive binding/provider identities, not the User, Organization, Membership, or authentication identity.
9. A User may eventually hold Membership in more than one Organization, but Identity v1 permits only one active Organization per app installation at a time. Organization switching must be blocked while unresolved/protected local work could cross Organization boundaries.
10. Client-Company switching inside one active Organization continues to use the existing immutable queued-photo destination rules and must never rewrite queued destination identity.
11. Temporary loss of account-service connectivity must not by itself destroy protected work or make ordinary safe offline capture impossible for a previously authenticated/validated active Membership. Exact session/revalidation intervals require a separately recorded authentication design.
12. Once Membership revocation is successfully learned, the app must not authorize new ordinary Organization work under that Membership. Revocation must not automatically delete protected originals, delete Drive data, rewrite destinations, or blindly continue unresolved uploads.
13. Sign-out and account closure are identity/account operations. They must not automatically delete protected photos or business Drive records, change Drive sharing, or rewrite provider identities.
14. A new device must deliberately establish its own Drive binding through the supported platform access flow rather than inheriting another device's provider-bound identity.
15. The FPP identity backend must remain an identity/account service, not a second property/work-order/photo database. Customer addresses, photos, work orders, SAF URIs, and Drive provider IDs are not required merely to authenticate a User.
16. Identity implementation must preserve all existing photo-protection, exact-destination, upload, retry, reconciliation, cleanup, and Drive-access contracts.
17. **Supabase** is the approved identity/account backend for original FPP, using a **dedicated Supabase project that is separate from Field Photo Prep Team** in Auth users, database, keys, functions, secrets, and migration history.
18. Initial FPP sign-in uses **Supabase Auth email + password**. V1 is invitation-only after a controlled first-Owner bootstrap; open public self-signup is outside Phase 12B.
19. Supabase Auth `auth.users.id` is the permanent FPP User ID. Email remains a mutable login/contact field and is never authorization identity.
20. Original FPP stores only the minimum account model in Supabase: Organizations, Memberships, and Invitations. It must not mirror client-company folders, addresses, work orders, photos, Drive provider IDs, SAF URIs, routes, or inspection data into the identity backend.
21. FPP roles remain `OWNER` and `MEMBER`. Organization authorization is controlled by exact RLS-backed Membership records, not editable user metadata, email equality, Team roles, or Drive account identity.
22. Every FPP table exposed through Supabase's Data API must use explicit least-privilege grants and RLS. Privileged Owner/bootstrap/invitation actions remain server-side and must never expose a secret/service-role key in Android.
23. Android stores only the required Supabase session and active identity snapshot. Access/refresh credentials must be encrypted at rest with Android Keystore-backed protection, excluded from backup/device transfer, and excluded from logs/diagnostics.
24. When network is available, FPP revalidates the Auth session and authoritative Membership. A previously validated `ACTIVE` Membership may continue normal field work for the same active Organization for up to **72 hours** from the last successful validation.
25. During the 72-hour offline grace, Organization switching and Membership/Invitation administration are unavailable. After 72 hours without successful revalidation, existing protected work remains preserved/recoverable, but new capture and new remote Drive mutations require successful Membership revalidation.
26. Once an authoritative `REVOKED` Membership is learned, new ordinary Organization work and new remote Drive writes under that Membership stop immediately. Revocation must not delete protected originals, delete Drive data, rewrite destinations, or silently transfer unfinished work.
27. FPP sign-out remains subject to the protected-work guard. When allowed, it clears local FPP Auth/session state and must not delete protected photos, Drive records, sharing, or provider identities.
28. FPP Supabase identity never selects, infers, authorizes, or rewrites the Android SAF Drive account/workspace. FPP Auth email and Drive account email may differ.
29. Field Photo Prep Team's Supabase project, Auth users, tables, keys, functions, secrets, Organization IDs, work orders, photos, and sessions are never original-FPP runtime identity/data.
30. Supabase Storage, Realtime, job/photo synchronization, Google Drive OAuth, Google/social sign-in, subscriptions/licensing, and public self-signup remain outside Phase 12B.

## 2. Company, property, work-order, and folder identity

### Company/workspace identity

1. The operator approves one field-work workspace through the platform integration. On Android, that workspace is the one persisted SAF tree grant used for the multi-company workflow.
2. Company folders live directly under that approved workspace. One selected company folder is the parent for property/address discovery and creation.
3. The app may discover, select, create, and rename company folders only under the exact approved workspace and only by stable remote/provider identity after selection or creation.
4. Company names are display/discovery information. A company rename does not change company identity when the stable provider document ID is unchanged.
5. Before creating a company, the app must use authoritative-enough provider state to check the workspace for an exact visible-name match. One exact match is reused, multiple exact matches require operator choice, and no exact match may create exactly one folder.
6. Editing a company may rename only the exact selected company folder. A requested rename that would collide with another exact company name must be blocked rather than guessed.
7. Switching companies clears the current address/work-order navigation binding but must never rewrite any queued photo's stored work-order destination identity.
8. Company deletion, moving a company to another parent, automatic company merging, and automatic sharing changes are outside the initial multi-company scope.
9. A legacy single-company master may remain usable until the operator explicitly selects the broader workspace. Migration to the workspace must not rewrite queued photo destination IDs or guess a company from name when exact provider identity is unavailable.

### Property and work-order identity

1. One address folder represents one property under the selected company folder.
2. One work-order folder represents one specific work occurrence for that property.
3. Initial work-order folder names use `Work Order - YYYY-MM-DD`, for example `Cut Grass - 2026-09-06`.
4. The date is the local calendar date for that work occurrence.
5. The same property may have different work-order folders on the same date.
6. The same work-order name may recur on different dates and must remain separated by date.
7. The user selects one approved field-work workspace and then one active company folder under that workspace.
8. Google Drive, as exposed through the approved platform integration, is the source of truth for which company, address, and work-order folders currently exist within the approved hierarchy.
9. The app may read folder metadata needed to show existing company, address, and work-order folders, including folder name and stable remote folder identity.
10. Before creating an address folder, the app must check the exact selected company folder for an existing usable address folder with the requested name.
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
31. When **Add Work Order** is invoked while an older occurrence of that same work-order name is selected, the app must route the request through the approved reuse workflow instead of creating a parallel new dated folder. An empty selected folder may be renamed/reused after verification; a non-empty selected folder must still require the explicit **Clear & Reuse** confirmation before any child deletion.
32. A successfully completed empty-folder reuse or **Clear & Reuse** creates a new capture-order occurrence even though the stable remote folder identity is retained. The first new photo captured for that reused occurrence must start at sequence `001`.
33. Work-order reuse must fail closed while any local photo bound to that work-order remote folder identity is still unconfirmed (`CAPTURING`, `WAITING`, `UPLOADING`, `FAILED`, or `UNCERTAIN`). A retained confirmed `UPLOADED` history record may remain but must not raise the new occurrence's capture-sequence baseline.
34. After a successful work-order reuse, confirmed upload-history records from an earlier occurrence of that reused provider folder may remain locally as lightweight evidence, but they must not appear in the new occurrence's Photos list, photo count, selected batch, or **Copy Capture Order** output. Current-occurrence UI and capture-order history must begin clean for the new work occurrence while exact provider identity remains unchanged.
35. An ordinary later visible-folder rename, outside an approved FPP reuse transition, does not by itself create another capture-order reset. Stable remote folder identity remains authoritative after the reuse transition is complete.

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
2. On Android, the operator chooses the Google Drive-backed field-work workspace through the system document picker. The app keeps that persisted SAF tree grant plus stable workspace/company/address/work-order provider document identities; it does not store or manage Google OAuth access/refresh tokens for this workflow.
3. The app must use the least remote-storage access that supports the approved workflow. Broader access may not be added without a documented need and user approval.
4. The app may read the metadata needed to discover company folders under the approved workspace, address folders under the selected company, and work-order folders under a selected address.
5. The app may create company folders under the approved workspace and address folders under the exact selected company folder.
6. The app may create dated work-order folders under the exact selected address folder.
7. The app may upload prepared photo files only into the exact selected or stored work-order folder.
8. Every Drive write must target an explicit stable parent-folder identity; visible parent names alone are insufficient.
9. The app must store or otherwise retain the returned stable remote identity for each address folder and work-order folder it creates or the operator selects for reuse.
10. The app must not infer upload success from a local queue update alone.
11. The app must not automatically share uploaded photos or folders, change inherited permissions, or create public links.
12. Files created under a shared parent may inherit that parent's Drive permissions; the app does not independently broaden sharing.
13. Loss of platform/document-provider access must not delete temporary photos that have not yet been confirmed in Drive.
14. Company-folder rename is permitted only through the explicit company-edit workflow and must preserve exact provider identity. Work-order-folder rename and child deletion remain permitted only through the approved empty-folder reuse or confirmed **Clear & Reuse** workflow in Section 2.
15. A folder reuse operation must operate by exact stable remote folder identity; visible folder names alone may never authorize deletion or rename.

## 6. Upload queue, selection, batch execution, local discard, and retry

1. Photos may remain temporarily queued locally when the device is offline or Drive is unavailable.
2. Each queued photo keeps its own immutable local identity and its bound work-order remote folder identity.
3. Retry uses the original queued destination. It must not switch to whichever address or work order is currently open.
4. A retry must not knowingly create a second Drive copy after the app has already confirmed the first upload.
5. If upload result is uncertain, the app must resolve the uncertainty before creating another copy or clearly require operator action rather than guessing.
6. Restarting the app must not discard queued photos that have not been safely uploaded or intentionally removed.
7. A successful upload changes only that photo's queue state.
8. Once Drive success is confirmed and local bookkeeping is safely committed, the temporary image may be removed automatically.
9. The operator may build a temporary UI selection from photos in the open work order that are eligible for at least one approved selected action: normal upload or explicit local discard.
10. Selection is UI/session convenience only. Selection state is not durable queue authority and must not change a photo's stored destination, upload state, or local files merely because a box is checked or unchecked.
11. **Select All Ready** retains its upload meaning: it selects all currently upload-eligible prepared photos for the open work order. **Clear Selection** removes the current selection without changing photo, queue, or Drive state.
12. A selected normal-upload batch may contain only photos that have a usable prepared copy and are eligible to begin an upload attempt. `CAPTURING`, `UPLOADING`, `UNCERTAIN`, already `UPLOADED`, missing-image, or unprepared photos are not eligible for normal batch upload.
13. **Upload Selected (N)** may run only when every currently selected photo is normal-upload eligible. It snapshots the selected photo IDs, and each selected photo ID may be attempted at most once during that batch run.
14. A batch must perform at most one Drive upload attempt at a time. “Upload Selected” is one operator action, not permission to issue simultaneous remote creates.
15. Every photo in an upload batch must use that photo's own already-stored immutable work-order provider identity. The currently visible address/work-order state may not redirect any selected item.
16. A confirmed upload may continue to the next selected photo. A confirmed upload with incomplete local cleanup may also continue because remote success is already durable.
17. A known retry-safe failure that leaves the photo in `FAILED` or pre-attempt `WAITING` state may be reported and the upload batch may continue to later selected photos.
18. If an attempted photo becomes `UNCERTAIN`, remains `UPLOADING`, disappears from readable queue state, or otherwise has an unverified remote outcome, the upload batch must stop immediately. No later selected photo may be intentionally attempted.
19. Stopping an upload batch must leave all later unattempted selected photos in their prior local/queue state.
20. Process death during an active upload batch does not make the selection authoritative after restart. Existing per-photo restart recovery governs the active photo; photos not yet attempted remain unchanged and may be selected again later.
21. The existing individual-photo upload and UNCERTAIN reconciliation paths remain available; batch upload does not weaken or replace them.
22. A `WAITING` or retry-safe `FAILED` photo may be selected for explicit local discard even when it is not prepared or otherwise not eligible for upload.
23. **Discard Selected (N)** may run only when every currently selected photo still belongs to the exact open address/work-order identities and `canDiscardLocally()` remains true for every selected photo.
24. Batch local discard requires one explicit confirmation showing the work order and exact selected count and stating that only app-private local photo data will be removed and Drive will not be changed.
25. Before any selected local photo is deleted, the entire selected ID snapshot must be re-read and validated. A missing photo, wrong stored address/work-order identity, duplicate selection, or unsafe queue state aborts the whole batch before deletion starts.
26. Each selected photo must be re-read again before its local deletion. If it has become unsafe, that item and all later items remain untouched.
27. Local discard removes only the selected photo's app-private prepared derivative, protected local original, and pending local metadata. It must not perform a Drive/provider delete, upload, move, rename, permission change, retry, or reconciliation operation.
28. If a local filesystem deletion fails after earlier selected photos were already removed, the batch stops, reports the completed count, and leaves all later photos untouched rather than pretending the whole batch succeeded.
29. `CAPTURING`, `UPLOADING`, `UNCERTAIN`, and `UPLOADED` photos are never eligible for explicit local discard because capture/upload/duplicate-protection evidence must be preserved.
30. Individual-photo discard remains available and must enforce the same exact stored-identity and `canDiscardLocally()` safety rules as batch local discard.

## 7. Photo naming and duplicate protection

1. Uploaded photo filenames must be unique within normal app operation without depending only on the visible address, work-order name, or date.
2. A timestamp may be part of the filename, but timestamp alone must not be the only uniqueness protection when collisions are possible.
3. Internal photo identity is separate from the visible Drive filename.
4. Renaming an address folder or work-order folder does not change the identity of photos already bound to that work-order's remote folder identity.
5. Duplicate prevention must favor preserving a recoverable temporary photo over silently losing a photo.
6. A batch must not intentionally include the same local photo identity more than once in one run.
7. Batch convenience must never bypass the per-photo remote identity, provisional identity, confirmation, retry, or uncertainty protections.
8. New capture-order-enabled photo uploads use a sequence prefix followed by the UUID-based photo identity, for example `001_field-photo-<UUID>.jpg`. The sequence is zero-padded to at least three digits and widens naturally above `999`.
9. Capture sequence is reserved when the app reserves the protected capture, not when the photo later uploads. A consumed reservation number is not reused after discard, failed/empty capture, restart, or out-of-order upload.
10. A newly created work-order remote folder starts its capture sequence at `001`; different work-order remote folder identities maintain independent sequences.
11. After an approved empty-folder reuse or **Clear & Reuse**, the reused folder keeps its same stable remote identity but the new dated work occurrence starts a fresh capture sequence at `001`.
12. Retained confirmed metadata from the prior occurrence must not raise the new reused occurrence's sequence baseline. Unconfirmed prior-occurrence photos must block reuse rather than risk later upload into the new occurrence.
13. Legacy photo records and already-uploaded UUID-only filenames remain valid and are not renamed merely to adopt capture-order prefixes.
14. Once an approved reuse reset is complete, an ordinary later visible-folder rename does not reset the capture sequence again; stable remote folder identity remains authoritative.
15. Confirmed photo-history rows from an earlier occurrence of a reused provider folder must be excluded from the active occurrence's Photos list and capture-order manifest even though their lightweight metadata may remain locally for exact-history or duplicate-protection evidence.

## 8. Local data and deletion

1. Local app state should remain lightweight and may store workspace-tree access/identity, active company identity, recent address-folder names and identities, recent work-order-folder names and identities, queued-photo records, upload status, remote file identity needed for duplicate protection, and settings.
2. Local folder records are a convenience cache, not the authoritative inventory of Drive folders.
3. The app must be able to refresh its company, address, and work-order folder lists from Google Drive through the approved platform integration rather than assuming local folder memory is complete.
4. Successfully uploaded image data does not need to remain in Field Photo Prep.
5. Platform/provider access material must not be exposed in ordinary logs or exported records. On Android, the app must not invent or persist Google OAuth credentials because the SAF workflow does not use app-managed OAuth tokens.
6. Deleting a local remembered-folder entry must not automatically delete its Drive folder or Drive photos.
7. General-purpose Drive photo, work-order-folder, or address-folder deletion from inside the app is not part of the initial approved scope.
8. The only initial app-driven deletion of existing Drive content is the explicit, confirmed child-item removal required by **Clear & Reuse** for one selected work-order folder.
9. Explicit operator discard of a temporary local photo is separate from Drive deletion. It may remove only app-private local photo data that is still in a discard-safe queue state.
10. A local photo discard must fail closed if the persisted record cannot be read, no longer matches the intended address/work-order identity, or is no longer `WAITING`/retry-safe `FAILED`.
11. Local discard must never be implemented by deleting the remote Drive photo or its work-order folder.

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
- use per-photo selection checkboxes for the approved selected actions available to that local photo;
- use **Select All Ready**, **Clear Selection**, and a visible selected count;
- use **Upload Selected (N)** only when the complete selection is upload-ready;
- use **Discard Selected (N)** with one explicit confirmation when the complete selection is locally discard-safe;
- keep individual-photo upload/reconciliation/discard controls available; and
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

When two behaviors conflict, preserve any photo not yet confirmed in Drive and preserve its exact work-order-folder destination identity before optimizing convenience, speed, cleanup, or storage use. The only exception for an unconfirmed local photo is an explicit operator discard that passes the approved stored-identity and discard-safe queue-state guards immediately before local deletion. Destructive Drive reuse must remain operator-initiated, narrowly scoped, and fail closed. A selected upload batch must stop rather than continue through an uncertain or unverified remote result. After confirmed Drive storage, the app should favor removing unnecessary local image copies rather than becoming a second photo archive.
