# Field Photo Prep Google Drive Integration Contract

## Connected systems

- Current local app: Android Field Photo Prep
- Current Android Drive access: Android Storage Access Framework (SAF) and the system DocumentsProvider selected by the operator
- Remote storage: the operator-selected Google Drive-backed document tree
- Primary folder flow: approved master Drive folder → address folder → dated work-order folder
- Primary photo flow: captured photo → protected temporary local state → prepared copy → exact Drive work-order folder → confirmed remote file

Google Drive is the long-term source of truth for address folders, work-order folders, and uploaded photos. The app may keep lightweight folder mappings and temporary upload state, but it is not a second photo archive.

This contract covers the Drive boundary only. Future workbook, Free Map Router, or other-system integrations require their own documented handoff rules before runtime coupling is added.

## Current Android access model

1. The Android app opens the system folder picker with `ACTION_OPEN_DOCUMENT_TREE`.
2. The operator chooses the intended Google Drive provider/account context and the approved master folder in that system UI.
3. The app requests and persists the returned read/write tree URI permission.
4. The persisted tree URI defines the app's approved remote-document boundary for the current Android workflow.
5. Provider document ID is authoritative folder/file identity inside that persisted tree grant. Display name is context only.
6. The app does not manage Google OAuth access tokens, refresh tokens, OAuth client configuration, or a service-account credential for this Android workflow.
7. Provider/account choice remains owned by Android's system document picker and installed document provider. The app must not silently switch to another provider, account, or same-named master folder.
8. Any future Android change away from this SAF model is a Level 3 integration change and must be explicitly reviewed before merge.

A future iOS or other-platform implementation may use a different platform access mechanism, but it must preserve the stable-identity, destination, duplicate, retry, sharing, and photo-protection behavior in `CONTRACT.md`.

## Master folder contract

1. The operator selects or approves one master Drive folder for the initial workflow.
2. On Android, the app stores the persisted tree URI plus the master provider document ID and display name.
3. The master provider document ID inside the persisted tree grant is authoritative identity; name is display context.
4. A rename of the same remote folder does not invalidate its identity when the provider identity remains the same.
5. Loss of the persisted tree grant or inability to resolve the stored master must stop affected discovery and new child-folder creation until the operator resolves or replaces the destination.
6. The app must not silently substitute another same-named master folder.

## Address-folder discovery contract

1. Google Drive as exposed through the approved document tree is authoritative for which address folders currently exist under the approved master folder.
2. The app may query the document provider for folder metadata needed to present those address folders, including provider document ID and display name.
3. A locally remembered address-folder list is a cache only. The app must be able to refresh from the provider.
4. A provider document ID already linked to a remembered address remains authoritative even if its visible name changes.
5. Before address-folder creation, the app checks the approved master folder for an exact usable name match using provider state safe enough to make an absence/create decision.
6. One exact match is reused, multiple exact matches require operator choice, and no exact match may be created as one new address folder.
7. Discovery must not modify, rename, move, delete, or change permissions on address folders it reads.
8. Address folders are not eligible for automatic recycling or **Clear & Reuse** in the initial model.

## Work-order folder contract

1. A work-order folder lives directly under one selected address folder.
2. Initial work-order folder names use `Work Order - YYYY-MM-DD`, for example `Cut Grass - 2026-09-06`.
3. The work-order name is descriptive text such as `Cut Grass`, `Remove Trash`, `Winterization`, or `Full Property Inspection`.
4. The date is the local calendar date for that specific work occurrence.
5. Repeated work of the same type on different dates must use separate dated work-order folders unless the operator intentionally recycles an older folder under the approved reuse rules below.
6. Before creating a dated work-order folder, the app checks the selected address folder for an exact usable folder-name match.
7. If exactly one matching work-order folder exists, the app reuses it and stores its provider document ID.
8. If multiple exact matches exist, the app requires operator choice and must not guess by timestamp, ordering, or other inference.
9. If no exact match exists, the app may create one under the selected address folder or offer an eligible older same-work-order folder for reuse.
10. Once selected or created, the work-order folder's provider document ID is authoritative destination identity on Android. Its visible name is not identity.
11. Reopening the same work occurrence uses its stored provider identity and must not create a duplicate merely from its visible name.
12. Same-property, same-work-order, same-date ambiguity is not automatically solved by inventing another identifier. If that becomes a real workflow need, it requires an explicit design change.

## Provider freshness and uncertainty contract

Android document providers may temporarily expose cached or loading state. A single child query is therefore not automatically proof that a cloud-backed folder is empty or that a requested child does not exist.

For a decision that could create a duplicate, authorize deletion, or authorize rename based on emptiness/absence:

1. request provider refresh for the relevant document/list when the provider supports refresh;
2. treat `DocumentsContract.EXTRA_LOADING` as non-authoritative;
3. require the implementation's documented settled-state verification before acting;
4. if provider state remains stale, loading, inconsistent, or otherwise uncertain, fail closed with no destructive action and no absence-based create;
5. ordinary read-only browsing may show currently available provider state, but a stronger verification boundary is required immediately before risky write decisions.

The current hardened Android implementation uses repeated matching settled snapshots at those boundaries. A future implementation may improve the mechanism, but it may not weaken the fail-closed requirement without explicit Level 3 review.

## Work-order folder recycling contract

1. Folder recycling is limited to work-order folders under the currently selected address folder.
2. An automatic or simple reuse candidate must have the same work-order name as the new occurrence; the date may differ.
3. The app must obtain provider state safe enough to determine the selected old folder's direct children before deciding whether it is empty.
4. A truly empty old folder may be renamed to the requested new `Work Order - YYYY-MM-DD` name and reused with the same provider document ID.
5. A non-empty old folder may never be cleared, renamed, or reused automatically.
6. The operator may explicitly choose **Clear & Reuse** for a non-empty old work-order folder.
7. Before **Clear & Reuse**, the app must identify the folder by exact provider document ID, enumerate its direct child items from authoritative-enough provider state, show the full selected hierarchy, old folder name, child-item count, requested new name, and child-folder warning where applicable, then obtain operator confirmation.
8. The confirmation authorizes only the exact direct-child identity snapshot shown/approved for that selected work-order folder.
9. After confirmation, the app must re-read the selected hierarchy and direct-child identities. If the set or selected identity changed, deletion must not begin.
10. The app may remove only the confirmed direct child identities whose direct parent is that selected work-order folder.
11. The app must verify authoritative-enough zero-child state after removal and before rename.
12. Only after confirmed emptiness may the app rename that same folder to the requested new dated work-order name and retain its existing provider document ID as the new occurrence destination.
13. If any child removal fails, if state is uncertain, if emptiness cannot be confirmed, or if rename fails, the workflow stops and no new-work photo may be uploaded into that folder until the operator resolves the failure.
14. Reuse never moves the selected work-order folder to another address and never deletes the work-order folder itself.
15. General bulk cleanup, address-folder deletion, and deletion of arbitrary Drive content remain outside the initial scope.

## Photo upload contract

1. Every queued photo carries the exact destination work-order provider document ID captured when that photo entered the work occurrence.
2. Every Android upload request targets that stored work-order document identity explicitly through the approved persisted tree grant.
3. The currently visible/open address or work order must not override a queued photo's stored destination.
4. Remote success is recorded only after the document provider confirms creation of the destination file and the app retains enough returned remote identity to support duplicate control.
5. The app records enough remote identity to recognize a confirmed upload and avoid knowingly duplicating it on retry.
6. An interrupted provider call, timeout-like failure, or ambiguous response is not equivalent to failure and not equivalent to confirmed success. The app must reconcile uncertainty before creating another copy or require operator action.
7. A photo not yet confirmed in Drive must remain recoverable locally for retry or recapture.
8. After confirmed remote creation and successful local bookkeeping, temporary local image data may be removed automatically.
9. The app does not need to retain confirmed uploaded image data as a permanent local copy.

## Access and permissions

1. Android remote access is granted through the operator-selected persisted SAF tree URI, not through app-managed Google OAuth credentials.
2. Use the least tree/document access that can support selecting the master folder, discovering address/work-order folders, creating folders, uploading photos, and the explicitly approved work-order-folder rename/child-deletion reuse flow.
3. Any requested platform permission or provider access that can expose metadata or content outside the approved workflow must be documented with its exact purpose before merge.
4. If pre-existing-folder discovery or approved recycling cannot be implemented within the currently approved persisted tree grant, any broader access mechanism must be explicitly reviewed rather than silently added.
5. Persisted URI/access data and remote identities must not be exposed unnecessarily in logs, exported job data, image metadata added by the app, or repository files.
6. Loss/revocation of the persisted provider grant may pause Drive work but must not destroy temporary photos that have not yet been confirmed remotely.

## Sharing and ownership

1. The app does not automatically create public links.
2. The app does not automatically add or remove Drive permissions.
3. Child folders/files may inherit permissions from their Drive parent according to Google Drive behavior; the app does not broaden those permissions on its own.
4. A future requirement to share folders with additional people is a separate feature and must define who owns permission changes.

## Retry and duplicate control

1. Retry preserves immutable local photo identity and exact destination work-order provider document ID.
2. Confirmed uploaded state is terminal for automatic retry unless the user explicitly requests a new copy.
3. A failed local status update after remote success must be treated as an uncertain state to reconcile, not as permission to blindly re-upload.
4. Address-folder and work-order-folder create retries must not knowingly create duplicates after a confirmed successful create.
5. Duplicate control may use stored provider/remote file and folder identities plus app-generated stable photo identities; visible names alone are insufficient identity after selection.
6. Folder-name matching is only a discovery step. Once a folder is selected or linked, its stored provider identity owns Android destination identity.
7. A failed or interrupted **Clear & Reuse** operation must be treated as incomplete destructive state and must not be retried blindly without first re-reading the actual selected folder contents and name from the provider.

## Safe-environment rule

Drive integration development and verification uses a dedicated test master folder or equivalent safe fixture whenever possible. Live customer/job folders are not test targets.

## Android Google Drive Reality Gate

Any Android runtime change that alters or depends on master-tree access, address-folder discovery, work-order-folder discovery, folder creation, folder identity, provider freshness, folder recycling, child deletion, rename, upload destination, upload confirmation, retry, or remote file identity must complete the affected parts of this gate before being called ready for Level 3 merge approval:

1. Write the real operator sequence from the initiating tap to the final visible result.
2. Map the boundary as `app state → persisted master tree URI → address provider document ID → work-order provider document ID → DocumentsProvider operation/result → persisted app state`.
3. Verify the safe test folder, system document provider, persisted permission, and expected read/write access actually exist before the smoke check.
4. Focused coverage must exercise the real state-building path; directly injecting fake document IDs or success state is useful unit coverage but is not sufficient by itself.
5. In the safe Drive fixture, prove existing address-folder discovery returns the real children expected for the selected master folder.
6. Under a selected address folder, prove dated work-order discovery returns the real work-order children expected there.
7. Prove one exact existing folder is reused without creating a duplicate and that multiple same-named folders require operator choice at each relevant level.
8. Prove an empty old same-work-order folder can be renamed/reused without changing its provider document ID or parent.
9. For **Clear & Reuse**, use disposable test content and prove the app shows the correct full hierarchy and child count before confirmation, removes only that folder's confirmed children, verifies emptiness, then renames/reuses the same provider identity.
10. Attempt a safe deterministic child-deletion or rename failure when it can be produced without a production destructive testing backdoor or risk to unrelated content. If it cannot be produced safely, document that limitation and do not claim the real-provider failure path was tested.
11. For newly created folders/files, inspect the actual Drive item and confirm it is under the expected parent and has the expected type/name.
12. Confirm the app persisted the returned/selected provider identities and can reopen/retry without creating duplicates.
13. Confirm unrelated test Drive content is unchanged.
14. For upload changes, force or simulate one interrupted/failed attempt and prove the temporary recoverable photo and exact work-order destination identity survive.
15. After confirmed upload, prove the app can remove unnecessary temporary image data without deleting the Drive copy.
16. If rollout steps are ordered, do not perform the later step until the required earlier evidence exists.

## Future platform implementation

A future iOS implementation does not have to reproduce Android SAF APIs. Before iOS runtime work, create an iOS integration record mapping its platform-specific folder-access token/bookmark/identifier to the same core stable remote-folder identity contract. It must not weaken destination immutability, duplicate protection, confirmation, retry, sharing, or unconfirmed-photo protection merely because the platform APIs differ.

## Future cross-project integration

No workbook or Free Map Router handoff is part of the initial build. If one is later approved, add the actual producer → handoff artifact/state → consumer contract before implementation and keep remote folder identity separate from external work-order identity.

## When extra work is not required

A change that cannot affect remote document-provider access, folder discovery, folder identity, folder creation, folder recycling, deletion, rename, upload, retry, remote state, or permissions needs only: `No Google Drive integration impact.` It does not require a real Drive smoke test.
