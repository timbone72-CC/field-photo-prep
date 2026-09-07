# Field Photo Prep Google Drive Integration Contract

## Connected systems

- Local Android app: Field Photo Prep
- Remote storage: the authenticated user's Google Drive
- Primary outbound flow: app job → exact Drive job folder → prepared photo file

This contract covers the Drive boundary only. Future workbook, Free Map Router, or other-system integrations require their own documented handoff rules before runtime coupling is added.

## Master folder contract

1. The user selects or approves one master Drive folder for the initial workflow.
2. The app stores that folder's Drive ID and display name.
3. Drive ID is authoritative identity; name is display context.
4. A rename of the same Drive folder does not invalidate its identity.
5. Loss of access to the stored master folder must stop new child-folder creation until the operator resolves or replaces the destination.
6. The app must not silently substitute another same-named folder.

## Job-folder creation contract

1. New job folders are created with the approved master folder ID as their parent.
2. The app records the Drive folder ID returned by Google after successful creation.
3. A local job is not considered remotely linked until a valid Drive folder ID has been confirmed and persisted.
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
7. Upload failure must not remove the protected local original.

## Authentication and permissions

1. The app authenticates with the user's Google identity rather than using a service account as the normal personal Drive owner.
2. Use the least authorization scope that can support the approved workflow.
3. Any proposal to request broader Drive access must document why the current least-privilege approach cannot satisfy the requirement and requires user approval before merge.
4. Authentication material must not be exposed in logs, exported job data, image metadata added by the app, or repository files.
5. Sign-out or expired authorization may pause Drive work but must not destroy local originals or queue state.

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

## Safe-environment rule

Drive integration development and verification uses a dedicated test master folder or equivalent safe fixture whenever possible. Live customer/job folders are not test targets.

## Google Drive Reality Gate

Any runtime change that alters or depends on Drive authentication, master-folder selection, folder creation, folder identity, upload destination, upload confirmation, retry, or remote file identity must complete this gate before being called ready, mergeable, or publishable:

1. Write the real operator sequence from the initiating tap to the final visible result.
2. Map the boundary as `app state → actual Drive folder/file ID → Drive API result → persisted app state`.
3. Verify the test account/folder and required permissions actually exist before the smoke check.
4. Focused coverage must exercise the real state-building path; directly injecting a fake final folder ID or success state is useful unit coverage but is not sufficient by itself.
5. In the safe Drive fixture, inspect the actual created folder/file and confirm it is under the expected parent and has the expected type/name.
6. Confirm the app persisted the returned Drive identity and can reopen/retry without creating a duplicate.
7. Confirm unrelated test Drive content is unchanged.
8. For upload changes, force or simulate one interrupted/failed attempt and prove the protected original and destination identity survive.
9. If rollout steps are ordered, do not perform the later step until the required earlier evidence exists.

## Future cross-project integration

No workbook or Free Map Router handoff is part of the initial build. If one is later approved, add the actual producer → handoff artifact/state → consumer contract before implementation and keep Drive folder identity separate from external work-order identity.

## When extra work is not required

A change that cannot affect Drive authentication, folder identity, folder creation, upload, retry, remote state, or permissions needs only: `No Google Drive integration impact.` It does not require a real Drive smoke test.
