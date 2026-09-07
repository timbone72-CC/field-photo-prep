# Field Photo Prep Google Drive Integration Contract

## Connected systems

- Local Android app: Field Photo Prep
- Remote storage: the authenticated user's Google Drive
- Primary folder flow: approved master Drive folder → address folder → dated work-order folder
- Primary photo flow: captured photo → temporary local queue → exact Drive work-order folder → confirmed remote file

Google Drive is the long-term source of truth for address folders, work-order folders, and uploaded photos. The app may keep lightweight folder mappings and temporary upload state, but it is not a second photo archive.

This contract covers the Drive boundary only. Future workbook, Free Map Router, or other-system integrations require their own documented handoff rules before runtime coupling is added.

## Master folder contract

1. The user selects or approves one master Drive folder for the initial workflow.
2. The app stores that folder's Drive ID and display name.
3. Drive ID is authoritative identity; name is display context.
4. A rename of the same Drive folder does not invalidate its identity.
5. Loss of access to the stored master folder must stop folder discovery and new child-folder creation until the operator resolves or replaces the destination.
6. The app must not silently substitute another same-named master folder.

## Address-folder discovery contract

1. Google Drive is authoritative for which address folders currently exist under the approved master folder.
2. The app may query Drive for folder metadata needed to present those address folders, including folder ID and display name.
3. A locally remembered address-folder list is a cache only. The app must be able to refresh from Drive.
4. A Drive folder ID already linked to a remembered address remains authoritative even if its name changes.
5. Before address-folder creation, the app checks the approved master folder for an exact usable name match.
6. One match is reused, multiple matches require operator choice, and no match may be created as one new address folder.
7. Discovery must not modify, rename, move, delete, or change permissions on folders it reads.

## Work-order folder contract

1. A work-order folder lives directly under one selected address folder.
2. Initial work-order folder names use `Work Order - YYYY-MM-DD`, for example `Cut Grass - 2026-09-06`.
3. The work-order name is descriptive text such as `Cut Grass`, `Remove Trash`, `Winterization`, or `Full Property Inspection`.
4. The date is the local calendar date for that specific work occurrence.
5. Repeated work of the same type on different dates must use separate dated work-order folders.
6. Before creating a dated work-order folder, the app checks the selected address folder for an exact usable folder-name match.
7. If exactly one matching work-order folder exists, the app reuses it and stores its Drive ID.
8. If multiple exact matches exist, the app requires operator choice and must not guess by timestamp, ordering, or other inference.
9. If no exact match exists, the app may create one under the selected address folder.
10. Once selected or created, the work-order folder's Drive ID is authoritative destination identity. Its visible name is not identity.
11. Reopening the same work occurrence uses its stored folder ID and must not create a duplicate merely from its visible name.
12. Same-property, same-work-order, same-date ambiguity is not automatically solved by inventing another identifier. If that becomes a real workflow need, it requires an explicit design change.

## Photo upload contract

1. Every queued photo carries the exact destination work-order-folder ID captured when that photo entered the work occurrence.
2. Every upload request targets that work-order folder ID explicitly.
3. The currently visible/open address or work order must not override a queued photo's stored destination.
4. Remote success is recorded only after Google confirms file creation.
5. The app records enough remote identity to recognize a confirmed upload and avoid knowingly duplicating it on retry.
6. A network timeout or ambiguous response is not equivalent to failure and not equivalent to confirmed success. The app must reconcile uncertainty before creating another copy or require operator action.
7. A photo not yet confirmed in Drive must remain recoverable locally for retry or recapture.
8. After confirmed remote creation and successful local bookkeeping, temporary local image data may be removed automatically.
9. The app does not need to retain confirmed uploaded image data as a permanent local copy.

## Authentication and permissions

1. The app authenticates with the user's Google identity rather than using a service account as the normal personal Drive owner.
2. Use the least authorization that can support selecting the master folder, discovering address/work-order folders, creating folders, and uploading photos.
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

1. Retry preserves immutable local photo identity and exact destination work-order-folder ID.
2. Confirmed uploaded state is terminal for automatic retry unless the user explicitly requests a new copy.
3. A failed local status update after remote success must be treated as an uncertain state to reconcile, not as permission to blindly re-upload.
4. Address-folder and work-order-folder create retries must not knowingly create duplicates after a confirmed successful create.
5. Duplicate control may use stored remote file/folder IDs and app-generated stable photo identities; visible names alone are insufficient identity after selection.
6. Folder-name matching is only a discovery step. Once a folder is selected or linked, its Drive ID owns identity.

## Safe-environment rule

Drive integration development and verification uses a dedicated test master folder or equivalent safe fixture whenever possible. Live customer/job folders are not test targets.

## Google Drive Reality Gate

Any runtime change that alters or depends on Drive authentication, master-folder selection, address-folder discovery, work-order-folder discovery, folder creation, folder identity, upload destination, upload confirmation, retry, or remote file identity must complete this gate before being called ready, mergeable, or publishable:

1. Write the real operator sequence from the initiating tap to the final visible result.
2. Map the boundary as `app state → address folder ID → work-order folder ID → Drive API result → persisted app state`.
3. Verify the test account/folder and required permissions actually exist before the smoke check.
4. Focused coverage must exercise the real state-building path; directly injecting fake folder IDs or success state is useful unit coverage but is not sufficient by itself.
5. In the safe Drive fixture, prove existing address-folder discovery returns the real children expected for the test master folder.
6. Under a selected address folder, prove dated work-order discovery returns the real work-order children expected there.
7. Prove one exact existing folder is reused without creating a duplicate and that multiple same-named folders require operator choice at each relevant level.
8. For newly created folders/files, inspect the actual Drive item and confirm it is under the expected parent and has the expected type/name.
9. Confirm the app persisted the returned/selected Drive identities and can reopen/retry without creating duplicates.
10. Confirm unrelated test Drive content is unchanged.
11. For upload changes, force or simulate one interrupted/failed attempt and prove the temporary recoverable photo and exact work-order-folder destination survive.
12. After confirmed upload, prove the app can remove unnecessary temporary image data without deleting the Drive copy.
13. If rollout steps are ordered, do not perform the later step until the required earlier evidence exists.

## Future cross-project integration

No workbook or Free Map Router handoff is part of the initial build. If one is later approved, add the actual producer → handoff artifact/state → consumer contract before implementation and keep Drive folder identity separate from external work-order identity.

## When extra work is not required

A change that cannot affect Drive authentication, folder discovery, folder identity, folder creation, upload, retry, remote state, or permissions needs only: `No Google Drive integration impact.` It does not require a real Drive smoke test.
