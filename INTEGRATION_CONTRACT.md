# Field Photo Prep Google Drive Integration Contract

> **Routing scope:** Google Drive, Android SAF/DocumentsProvider, provider identity, folder operations, upload/retry, permissions, and Drive reality gates. Read when `RULE_INDEX.md` routes the current work to Drive/provider surfaces.

## Connected systems

- Current local app: Android Field Photo Prep
- Current Android Drive access: Android Storage Access Framework (SAF) and the system DocumentsProvider selected by the operator
- Remote storage: the operator-selected Google Drive-backed document tree
- Primary folder flow: approved field-work workspace → company folder → address folder → dated work-order folder
- Primary photo flow: captured photo → protected temporary local state → prepared copy → exact Drive work-order folder → confirmed remote file

Google Drive is the long-term source of truth for company folders, address folders, work-order folders, and uploaded photos. The app may keep lightweight folder mappings and temporary upload state, but it is not a second photo archive.

This contract covers the Drive boundary only. Future workbook, Free Map Router, or other-system integrations require their own documented handoff rules before runtime coupling is added.

## Current Android access model

1. The Android app opens the system folder picker with `ACTION_OPEN_DOCUMENT_TREE`.
2. The operator chooses the intended Google Drive provider/account context and the approved field-work workspace in that system UI.
3. The app requests and persists the returned read/write tree URI permission.
4. The persisted tree URI defines the app's approved remote-document boundary for the current Android workflow.
5. Provider document ID is authoritative folder/file identity inside that persisted tree grant. Display name is context only.
6. The app does not manage Google OAuth access tokens, refresh tokens, OAuth client configuration, or a service-account credential for this Android workflow.
7. Provider/account choice remains owned by Android's system document picker and installed document provider. The app must not silently switch to another provider, account, or same-named workspace.
8. Any future Android change away from this SAF model is a Level 3 integration change and must be explicitly reviewed before merge.

A future iOS or other-platform implementation may use a different platform access mechanism, but it must preserve the stable-identity, destination, duplicate, retry, sharing, and photo-protection behavior in `CONTRACT.md`.

## Workspace and company contract

1. The operator selects or approves one field-work workspace for the multi-company workflow.
2. On Android, the app stores the persisted tree URI plus the workspace provider document ID and display name.
3. The workspace provider document ID inside the persisted tree grant is authoritative identity; name is display context.
4. Company folders live directly under the approved workspace. The app stores the selected company's provider document ID and display name separately from the workspace identity.
5. The selected company provider document ID is the authoritative parent for address discovery and address creation.
6. A rename of the same workspace or company folder does not invalidate its identity when the provider document ID remains unchanged.
7. Before company creation or rename collision decisions, the app requires provider state safe enough for absence/collision decisions using the same fail-closed freshness rules used for address/work-order writes.
8. One exact existing company-name match is reused; multiple exact matches require operator choice; no exact match may create one company under the exact workspace.
9. Company rename operates only on the exact selected company provider document ID and is blocked if a different company already has the requested exact name.
10. Switching companies clears current address/work-order navigation binding but never changes a queued photo's stored work-order destination ID.
11. Company deletion, moving, automatic merging, and sharing changes are outside this initial multi-company scope.
12. Loss of the persisted workspace grant or inability to resolve the selected company must stop affected discovery and new child-folder creation until the operator resolves the destination.
13. A legacy single-company tree may remain usable until the operator explicitly selects the broader workspace. Migration must not rewrite queued work-order destination IDs or guess the legacy company from name alone.

## Address-folder discovery contract

1. Google Drive as exposed through the approved document tree is authoritative for which address folders currently exist under the exact selected company folder.
2. The app may query the document provider for folder metadata needed to present those address folders, including provider document ID and display name.
3. A locally remembered address-folder list is a cache only. The app must be able to refresh from the provider.
4. A provider document ID already linked to a remembered address remains authoritative even if its visible name changes.
5. Before address-folder creation, the app checks the exact selected company folder for an exact usable name match using provider state safe enough to make an absence/create decision.
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

The Phase 3B and Phase 4 development branches implement repeated matching settled snapshots at those boundaries. They remain subject to their required real-device gates before merge. Future implementations may improve the mechanism, but may not weaken the fail-closed requirement without explicit Level 3 review.

## Work-order folder recycling contract

1. Folder recycling is limited to the exact work-order folder selected under the currently selected address folder.
2. The selected dated work-order folder supplies the work-order name. Clear & Reuse changes only the date portion after the operator selects a later date; the operator does not retype the work-order name for this action.
3. The app must obtain provider state safe enough to enumerate the selected folder's direct children and classify which direct files are photos/images before any destructive decision.
4. The user-facing flow is **select work order → Clear & Reuse → choose the same or later date → confirm**. A zero-photo folder uses this same path; a separate empty-folder reuse control is not required.
5. A non-empty old folder may never be cleared, renamed, or reused automatically.
6. Before confirmation, the app must identify the exact selected folder by provider document ID and show the selected property/work order, direct Drive photo count, and requested new date.
7. The confirmation authorizes only the exact direct-photo provider-identity snapshot shown for that selected work-order folder.
8. After confirmation, the app must re-read the selected folder identity/name, requested-name collision state, and direct-photo identities. If the selected identity/name changes or the photo identity set changes, removal must not begin.
9. The app may remove only confirmed direct child files whose provider MIME type identifies them as images/photos. Child folders and non-image files are never Clear & Reuse removal targets.
10. After removal, the app must obtain authoritative-enough provider state and verify zero direct photo files. Preserved child folders and non-image files may remain.
11. Only after the photo-clear condition is verified may the app rename that same folder to the same work-order name with the requested same or later date and retain its existing provider document ID as the new occurrence destination.
12. If any approved photo removal fails, provider state is uncertain, zero-photo state cannot be confirmed, or rename fails, the workflow stops and no new-work photo may be uploaded into that folder until the operator resolves the failure.
13. Reuse never moves the selected work-order folder to another address and never deletes the work-order folder itself.
14. Ordinary **Add Work Order** remains non-destructive and must not silently invoke Clear & Reuse merely because an older work-order occurrence is selected.
15. General bulk cleanup, address-folder deletion, child-folder deletion, non-photo-file deletion, and deletion of arbitrary Drive content remain outside the reuse scope.

## Phase 13 address archive/delete cleanup contract

1. Phase 13 cleanup is not general Drive cleanup. It is limited to one exact address under the currently selected exact company provider identity.
2. The app must enumerate the exact direct work-order folder identities under that address from authoritative-enough provider state before presenting destructive cleanup.
3. For each direct work-order folder, only direct child files classified by provider MIME type as images/photos are eligible for removal. Work-order folders, child folders, and non-image files are preserved.
4. Before confirmation, the app must show the exact property plus a bounded summary of work orders/photos that will be affected. Confirmation authorizes only the exact address/work-order/photo provider identity snapshot collected for that operation.
5. Immediately before the first deletion, the app must re-read the exact address's direct work-order folder identity set and the direct-photo identity set for each approved work-order folder. A changed folder set or changed photo set stops cleanup before deletion.
6. The existing persisted tree grant and exact provider IDs remain the only Android Drive authority. Visible names, search text, lifecycle age, or archive state alone never authorize a Drive deletion.
7. Any affected local photo in `CAPTURING`, `WAITING`, `UPLOADING`, `FAILED`, `UNCERTAIN`, or otherwise unresolved/protected state blocks the destructive cleanup before remote deletion begins.
8. If deletion partially succeeds, provider state becomes uncertain, or a refreshed snapshot cannot be proven, stop. Do not continue to later work orders and do not blindly retry. Require refresh/inspection and preserve a visible incomplete result.
9. Archive/reactivate/manual local suppression must not rename, move, recreate, or delete the address/work-order folders themselves.
10. Inactivity suggestions never perform a Drive write. They only surface properties for operator review.


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

## Selectable batch upload contract

1. Batch upload is an orchestration layer over the existing per-photo upload contract. It does not define a second Drive write path.
2. The operator may select any subset of currently upload-eligible prepared photos for the open work order and may intentionally keep the batch small to control network/provider load.
3. Selection state is local UI/session state only. Checking or unchecking a photo does not create, retry, confirm, reconcile, delete, or redirect any Drive object.
4. **Select All Ready** selects only photos currently eligible for a normal upload attempt. **Clear Selection** clears only the UI selection.
5. Starting **Upload Selected (N)** snapshots the selected local photo IDs. A photo ID may appear at most once in one batch snapshot.
6. The batch performs exactly one per-photo upload attempt at a time. It must not issue simultaneous remote creates merely because several photos were selected by one operator action.
7. Before each attempt, the existing per-photo coordinator remains responsible for validating the photo's queue state, prepared copy, immutable destination, create barrier, write/verify path, remote identity, and final queue result.
8. Each selected photo must target its own stored work-order provider document ID. The currently displayed work order must not override any photo's stored destination.
9. `CAPTURING`, `UPLOADING`, `UNCERTAIN`, already `UPLOADED`, missing-image, or unprepared photos are not eligible for a normal batch attempt.
10. A confirmed upload may proceed to the next selected photo. Incomplete local cleanup after confirmed remote success may be reported without changing that remote success.
11. A known retry-safe failure that leaves the active photo in `FAILED` or pre-attempt `WAITING` state may be preserved locally while the batch proceeds to a later selected photo.
12. If the active photo becomes `UNCERTAIN`, remains `UPLOADING`, cannot be reread, or otherwise has an unverified remote outcome, the batch must stop immediately. No later selected photo may be intentionally attempted.
13. When a batch stops early, later selected photos remain in their prior queue state and are not implicitly retried or marked failed.
14. Process death during a batch is resolved per photo: the active in-flight photo follows existing restart recovery, while later unattempted photos remain unchanged. The UI batch selection itself is not durable upload authority.
15. The individual-photo upload and UNCERTAIN reconciliation controls remain available and retain the same identity and retry rules.

## Access and permissions

1. Android remote access is granted through the operator-selected persisted SAF tree URI, not through app-managed Google OAuth credentials.
2. Use the least tree/document access that can support selecting the field-work workspace, discovering/creating/renaming company folders, discovering address/work-order folders, creating folders, uploading photos, the explicitly approved work-order-folder rename/photo-deletion reuse flow, and the explicitly approved Phase 13 address-scoped photo-cleanup flow.
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
8. Batch orchestration must not retry the same selected photo twice in one run or bypass the existing per-photo provisional/confirmed remote identity barriers.
9. An `UNCERTAIN` batch item blocks later batch attempts until that item's remote state is resolved or the operator ends the affected workflow; it is never interpreted as permission for a blind retry.

## Safe-environment rule

Drive integration development and verification uses a dedicated test workspace/company fixture or equivalent safe fixture whenever possible. Live customer/job folders are not test targets.

## Android Google Drive Reality Gate

Any Android runtime change that alters or depends on workspace-tree access, company-folder discovery/creation/rename/switching, address-folder discovery, work-order-folder discovery, folder creation, folder identity, provider freshness, folder recycling, child deletion, rename, upload destination, upload confirmation, retry, batch upload orchestration, or remote file identity must complete the affected parts of this gate before being called ready for Level 3 merge approval:

1. Write the real operator sequence from the initiating tap to the final visible result.
2. Map the boundary as `app state → persisted workspace tree URI → company provider document ID → address provider document ID → work-order provider document ID → DocumentsProvider operation/result → persisted app state`.
3. Verify the safe test folder, system document provider, persisted permission, and expected read/write access actually exist before the smoke check.
4. Focused coverage must exercise the real state-building path; directly injecting fake document IDs or success state is useful unit coverage but is not sufficient by itself.
5. In the safe Drive fixture, prove workspace selection persists, company discovery returns the real direct company children, company add/reuse/rename/switch behavior preserves exact provider IDs, and address discovery returns only the real children of the selected company.
6. Under a selected address folder, prove dated work-order discovery returns the real work-order children expected there.
7. Prove one exact existing folder is reused without creating a duplicate and that multiple same-named folders require operator choice at each relevant level.
8. Prove an empty old same-work-order folder can be renamed/reused without changing its provider document ID or parent.
9. For **Clear & Reuse**, use a disposable test work order containing photos plus at least one preserved non-photo item or child folder. Prove the operator flow is select work order → Clear & Reuse → choose the same or later date → confirm; confirmation shows the correct property/work order and Drive photo count; only the confirmed image/photo files are removed; preserved non-photo content remains; zero-photo state is verified; and the same provider identity is renamed/reused.
10. For Phase 13 archive/delete cleanup, use one disposable address with multiple direct work-order folders. Include FPP-uploaded photos and at least one preserved non-photo item. Prove archive removes only the approved photos, preserves folder structure/non-photo content, hides the property from Home, search can find the archived property, and Reactivate reuses the same address provider identity without creating/renaming/moving it.
11. For the Phase 13 negative gate, leave one affected local photo in an unresolved/protected state and prove archive/delete cleanup stops before any Drive photo is removed.
12. Attempt a safe deterministic child-deletion or rename failure when it can be produced without a production destructive testing backdoor or risk to unrelated content. If it cannot be produced safely, document that limitation and do not claim the real-provider failure path was tested.
13. For newly created folders/files, inspect the actual Drive item and confirm it is under the expected parent and has the expected type/name.
14. Confirm the app persisted the returned/selected workspace, company, address, work-order, and remote-file provider identities as applicable and can reopen/retry without creating duplicates.
15. Confirm unrelated test Drive content is unchanged.
16. For upload changes, force or simulate one interrupted/failed attempt and prove the temporary recoverable photo and exact work-order destination identity survive.
17. After confirmed upload, prove the app can remove unnecessary temporary image data without deleting the Drive copy.
18. For selectable batch upload, use at least four disposable prepared photos under one safe test work order. Select only a subset, upload it with one action, and prove exactly that subset is created under the stored work-order parent while unselected photos remain local/unattempted.
19. After the first subset is confirmed, upload the remaining disposable photos and verify no duplicate remote files were created and no selected photo was sent to a wrong parent.
20. Verify the batch executes sequentially from the operator's perspective and that the UI/result identifies which photos confirmed, failed retry-safely, or stopped the batch.
21. Do not deliberately manufacture an ambiguous remote create solely to satisfy an `UNCERTAIN` batch test. If a safe deterministic ambiguous condition cannot be induced, rely on focused automated stop-on-uncertainty coverage and record the real-provider limitation.
22. If a real provider result naturally becomes uncertain during batch testing, stop immediately, preserve evidence, do not retry the uncertain photo, and confirm later selected photos were not attempted.
23. If rollout steps are ordered, do not perform the later step until the required earlier evidence exists.

## Future platform implementation

A future iOS implementation does not have to reproduce Android SAF APIs. Before iOS runtime work, create an iOS integration record mapping its platform-specific folder-access token/bookmark/identifier to the same core stable remote-folder identity contract. It must not weaken destination immutability, duplicate protection, confirmation, retry, sharing, or unconfirmed-photo protection merely because the platform APIs differ.

## Future cross-project integration

No workbook or Free Map Router handoff is part of the initial build. If one is later approved, add the actual producer → handoff artifact/state → consumer contract before implementation and keep remote folder identity separate from external work-order identity.

## When extra work is not required

A change that cannot affect remote document-provider access, folder discovery, folder identity, folder creation, folder recycling, deletion, rename, upload, retry, remote state, or permissions needs only: `No Google Drive integration impact.` It does not require a real Drive smoke test.

## Non-destructive work-order correction

Explicit **Edit Work Order** authorizes correcting the selected existing folder's name and date while keeping all photos, child content, capture sequence and immutable queued destinations. It is the same work occurrence, not Clear & Reuse. Use the exact selected provider ID and address parent; fresh sibling state must verify the original identity/name, reject another folder with the requested exact name, and verify the returned unchanged identity/name after rename. Cancel writes nothing. Uncertain rename results require refresh/inspection before further writes or capture into that selection. No queue or occurrence-reset state is rewritten.

### Same-date Clear & Reuse

Clear & Reuse may explicitly clear the exact selected folder for its current date after the same deletion confirmation. A sibling collision blocks the operation without changing selection. It never silently selects another folder. Once remote photo absence and unchanged folder identity/name are verified, remove only the selected WO's confirmed uploaded history, keep all unrelated/protected records, and reset its next capture to 001. Failed retirement is an incomplete reuse, not success. Edit Work Order continues to keep photos and numbering.


## Phase 14 coordinated two-phone work orders — approved architecture, disabled until proven

The existing per-device Android SAF Drive binding, selected remote folder identity, and photo/document authority remain unchanged. For a work order explicitly enrolled as a managed shared job, a **separate original-FPP Supabase coordination control** may track only its opaque shared link, generation, lead, participating phones, and durable upload/clear/recovery permissions. No Supabase photo bytes, Google OAuth, cross-device copying of SAF IDs, client-folder indexing, or Field Work Hub backend is allowed.

A managed work-order mutation must hold a fresh authorized generation-bound permit **before** remote file creation, deletion, move or rename. Existing local checks/confirmation/fresh provider reads also remain mandatory. Coordinated Clear & Reuse must quiesce both devices, prove no unconfirmed in-flight work, delete only authorized old-cycle photos, verify provider result, and advance cycle only after operation settle. Archive/Delete Address must acquire compatible protection for all managed child work orders **before any photo removal**. Interrupted/uncertain SAF mutations **remain blocked** until verified-safe recovery; no automatic timeout may release new-cycle writes. An old unguarded client cannot be assumed safe: explicit compatible two-phone enrollment is required.

**Reality gate before enabling:** independently prove two devices' *actual shared Drive folder identity* using a disposable fixture and an acceptable, nonpersistent pairing method; prove an in-flight/delayed SAF operation cannot later affect a new-cycle photo. Provider enumeration twice is not a distributed lock. If the gate fails, do not enable managed destructive workflows or claim two-phone safety. This exception is designed but **not yet connected or deployed**.
