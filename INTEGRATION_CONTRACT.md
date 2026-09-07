# Field Photo Prep Google Drive Integration Contract

## Connected systems

- Local Android app: Field Photo Prep
- Remote storage: the authenticated user's Google Drive
- Primary folder flow: approved master Drive folder → existing or newly created job folder
- Primary photo flow: captured photo → temporary local queue → exact Drive job folder → confirmed remote file

Google Drive is the long-term source of truth for job folders and uploaded photos. The app may keep lightweight folder mappings and temporary upload state, but it is not a second photo archive.

This contract covers the Drive boundary only. Future workbook, Free Map Router, or other-system integrations require their own documented handoff rules before runtime coupling is added.

## Master folder contract

1. The user selects or approves one master Drive folder for the initial workflow.
2. The app stores that folder's Drive ID and display name.
3. Drive ID is authoritative identity; name is display context.
4. A rename of the same Drive folder does not invalidate its identity.
5. Loss of access to the stored master folder must stop folder discovery and new child-folder creation until the operator resolves or replaces the destination.
6. The app must not silently substitute another same-named master folder.

## Existing job-folder discovery contract

1. Google Drive is authoritative for which job folders currently exist under the approved master folder.
2. The app may query Drive for folder metadata needed to present those existing job folders, including folder ID and display name.
3. Folder discovery is scoped to the approved workflow and must not become a general-purpose Drive browser unless separately approved.
4. A locally remembered folder list is a cache only. The app must be able to refresh from Drive.
5. A Drive folder ID already linked to a remembered job remains authoritative even if the folder name changes.
6. When creating by requested folder name, the app first checks the approved master folder for matching existing folders.
7. If exactly one usable matching folder exists, the app reuses that folder and stores its Drive ID.
8. If multiple matching folders exist, the app requires the operator to choose the intended folder. It must not select by ordering, timestamp, or guesswork.
9. If no matching folder exists, the app may create a new child folder.
10. Discovery must not modify, rename, move, delete, or change permissions on folders it reads.

## Job-folder creation contract

1. New job folders are created with the approved master folder ID as their parent.
2. The app records the Drive folder ID returned by Google after successful creation.
3. A job is not considered remotely linked until a valid existing or newly created Drive folder ID has been confirmed and persisted.
4. Reopening an already linked job uses its stored folder ID and does not create a new folder merely from its name.
5. If folder creation result is uncertain, the app must reconcile the result before blindly creating another same-named folder.
6. Existing unrelated Drive folders must not be renamed, moved, deleted, or repurposed during job creation.

## Photo upload contract

1. Every queued photo carries the exact destination job-folder ID captured when that photo entered the job workflow.
2. Every upload request targets that folder ID explicitly.
3. The currently visible/open job must not override a queued photo's stored destination.
4. Remote success is recorded only after Google confirms file creation.
5. The app records enough remote identity to recognize a confirmed upload and avoid knowingly duplicating it on retry.
6. A network timeout or ambiguous response is not equivalent to failure and not equivalent to confirmed success. The app must reconcile uncertainty before creating another copy or require operator action.
7. A photo not yet confirmed in Drive must remain recoverable locally for retry or recapture.
8. After confirmed remote creation and successful local bookkeeping, temporary local image data may be removed automatically.
9. The app does not need to retain confirmed uploaded image data as a permanent local copy.

## Authentication and permissions

1. The app authenticates with the user's Google identity rather than using a service account as the normal personal Drive owner.
2. Use the least authorization that can support selecting the master folder, discovering the approved job folders, creating folders, and uploading photos.
3. Any requested permission that can expose metadata or content outside the approved workflow must be documented with its exact purpose before merge.
4. If pre-existing-folder discovery cannot be implemented with the currently approved authorization, the required additional metadata access must be explicitly reviewed rather than silently broadening permissions.
5. Authentication material must not be exposed in logs, exported job data, image metadata added by the app, or repository files.
6. Sign-out or expired authorization may pause Drive work but must not destroy temporary photos that have not yet been confirmed remotely.

## Sharing and ownership

1. The app does not automatically create public links.
2. The app does not automatically add or remove Drive permissions.
3. Child folders/files may inherit permissions from their Drive parent according to Google Drive behavior; the app does not broaden those permissions on its own.
4. A future requirement to share folders with additional people is a separate feature and must define who owns permission changes.

## Retry and duplicate control

1. Retry preserves immutable local photo identity and exact destination folder ID.
2. Confirmed uploaded state is terminal for automatic retry unless the user explicitly requests a new copy.
3. A failed local status update after remote success must be treated as an uncertain state to reconcile, not as permission to blindly re-upload.
4. A folder-create retry likewise must not knowingly create duplicate job folders after a confirmed successful create.
5. Duplicate control may use stored remote file/folder IDs and app-generated stable identities; visible names alone are insufficient identity.
6. Folder-name matching is only a discovery step. Once a folder is selected or linked, its Drive ID owns identity.

## Safe-environment rule

Drive integration development and verification uses a dedicated test master folder or equivalent safe fixture whenever possible. Live customer/job folders are not test targets.

## Google Drive Reality Gate

Any runtime change that alters or depends on Drive authentication, master-folder selection, folder discovery, folder creation, folder identity, upload destination, upload confirmation, retry, or remote file identity must complete this gate before being called ready, mergeable, or publishable:

1. Write the real operator sequence from the initiating tap to the final visible result.
2. Map the boundary as `app state → actual Drive folder/file ID → Drive API result → persisted app state`.
3. Verify the test account/folder and required permissions actually exist before the smoke check.
4. Focused coverage must exercise the real state-building path; directly injecting a fake final folder ID or success state is useful unit coverage but is not sufficient by itself.
5. In the safe Drive fixture, prove existing folder discovery returns the real child folders expected for the test master folder.
6. Prove an existing folder is reused without creating a duplicate and that multiple same-named folders require operator choice.
7. For newly created folders/files, inspect the actual Drive item and confirm it is under the expected parent and has the expected type/name.
8. Confirm the app persisted the returned/selected Drive identity and can reopen/retry without creating a duplicate.
9. Confirm unrelated test Drive content is unchanged.
10. For upload changes, force or simulate one interrupted/failed attempt and prove the temporary recoverable photo and destination identity survive.
11. After confirmed upload, prove the app can remove unnecessary temporary image data without deleting the Drive copy.
12. If rollout steps are ordered, do not perform the later step until the required earlier evidence exists.

## Future cross-project integration

No workbook or Free Map Router handoff is part of the initial build. If one is later approved, add the actual producer → handoff artifact/state → consumer contract before implementation and keep Drive folder identity separate from external work-order identity.

## When extra work is not required

A change that cannot affect Drive authentication, folder discovery, folder identity, folder creation, upload, retry, remote state, or permissions needs only: `No Google Drive integration impact.` It does not require a real Drive smoke test.
