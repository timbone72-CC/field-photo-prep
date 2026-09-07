# Phase 3B Implementation Record — Clear & Reuse

Date: 2026-09-07

## Approved scope

Phase 3B only:

- keep all merged Phase 1, Phase 2, and Phase 3A behavior;
- let the operator explicitly select one non-empty older work-order folder under the currently selected address;
- require the requested new occurrence to use the same exact work-order text and a later local date;
- before destructive action, re-read the selected address and re-resolve the selected old folder by stable document-provider ID;
- enumerate every direct child item of that exact selected work-order folder;
- show the old folder name, direct child-item count, and requested new folder name before confirmation;
- warn when any direct child is itself a folder because deleting that direct child folder also removes its contents;
- require explicit operator confirmation;
- after confirmation, re-read the selected folder and its child IDs again;
- if the selected folder name, target state, or child-ID set changed after confirmation, stop without deleting anything and require a fresh review;
- remove only the direct child items returned from that exact selected work-order folder;
- stop immediately on any child-deletion failure and never rename a partially cleared folder;
- verify the selected work-order folder has zero children after deletion;
- rename that same folder to the new `Work Order - YYYY-MM-DD` name only after confirmed emptiness;
- verify the same stable folder ID still resolves under the same address with the new name;
- persist that same folder identity as the selected work occurrence.

Explicitly excluded:

- automatic clearing or recycling;
- address-folder deletion/recycling;
- deleting sibling work-order folders;
- deleting arbitrary Drive content;
- moving the selected work-order folder;
- sharing/permission changes;
- camera/photo preparation/upload;
- workbook or Free Map Router integration;
- general Drive cleanup or file-manager behavior.

## Governed base and rollback

Exact base / rollback commit:

`44aa0ff71b12c7fd11c0e1820872e9146d9730a7`

Phase 3B branch:

`feat/phase-3b-clear-reuse`

## Change level

Level 3 because Phase 3B intentionally deletes existing Google Drive content before reusing a work-order folder.

Explicit operator approval is required before merge after the final automated suite and real-device Drive reality gate pass.

## Identity boundary

The app continues using Android Storage Access Framework (SAF) inside the persisted master-folder tree.

For this runtime, contract references to Drive folder ID map to the stable document-provider ID inside the persisted tree grant.

Destructive boundary:

`persisted master tree URI → selected address document ID → selected old work-order document ID → direct-child snapshot → operator confirmation → second direct-child snapshot → exact child deletions → verified empty folder → rename → same work-order document ID with new name → persisted selected work-order state`

Visible names remain discovery/context only. No visible name alone authorizes deletion.

## Operator flow

1. Open the intended address.
2. Tap the exact older work-order folder to select it.
3. Enter the same work-order text and choose the newer local date.
4. Tap **Clear & Reuse Selected Folder**.
5. App re-reads the selected address and resolves the selected old folder by exact ID.
6. App verifies that the requested target dated folder does not already exist.
7. App verifies that the selected old folder still has the same name and remains an older occurrence of the same exact work-order text.
8. App enumerates all direct child items of that exact old folder.
9. If the folder is empty, Phase 3B stops and tells the operator to use the non-destructive empty-folder reuse control.
10. If non-empty, app shows the old folder name, direct child-item count, requested new folder name, and any child-folder warning.
11. Operator may cancel; cancellation changes nothing.
12. On confirmation, app re-reads the folder and its direct child IDs again.
13. If the folder name, target state, or child-ID set changed since confirmation, app stops before deletion and requires a fresh review.
14. App deletes only the confirmed direct child IDs from the selected work-order folder.
15. Any child-deletion failure stops the workflow immediately. The folder is not renamed and further Drive writes are blocked until refresh.
16. After all requested deletions return success, app re-enumerates the selected folder and requires zero children.
17. Only then may the app rename the same folder ID to the new dated name.
18. App re-reads the selected address and verifies exactly one requested target name exists and it is the original selected folder ID.
19. Only then is Clear & Reuse reported complete.

## Confirmation snapshot rule

The confirmation is authorization for one exact destructive set, not a general permission to empty whatever happens to be in the folder later.

The app records the direct child document IDs shown by the pre-confirmation enumeration. After confirmation it enumerates again and compares IDs without relying on order.

If an item was added, removed, or replaced between those two reads, no deletion occurs. The operator must run Clear & Reuse again and confirm the new count/state.

## Child deletion rule

- Only document IDs returned as direct children of the selected work-order folder may be passed to delete.
- The selected work-order folder itself is never deleted.
- The address folder is never deleted.
- Sibling work-order folders are never deleted.
- A direct child folder counts as one direct child item; deleting that child folder also removes its own contents, so the confirmation explicitly warns when child folders are present.
- Deletion is sequential and fail-fast.
- A failure after some earlier deletions is treated as incomplete destructive state; no rename follows.

## Failure and uncertain-state handling

### Before the first child deletion

Read, eligibility, stale-selection, existing-target, changed-name, changed-child-set, and cancellation failures make no Drive change and do not enter destructive recovery state.

### After deletion begins

If any deletion fails:

- stop immediately;
- report how many of the confirmed direct items were removed before the failure;
- do not rename the work-order folder;
- block further create/reuse Drive writes until the operator refreshes actual Drive state;
- require a fresh Clear & Reuse review before any later destructive retry.

If deletion calls all return success but the folder cannot be confirmed empty:

- do not rename;
- mark the operation incomplete;
- block further create/reuse Drive writes until refresh.

If rename or post-rename verification fails after successful clearing:

- do not report the folder ready;
- block further create/reuse Drive writes until refresh;
- require the operator to inspect the actual safe-fixture folder name/contents before proceeding.

No photo upload exists yet, so Phase 3B cannot begin new-work uploads. Future upload phases must honor the same incomplete-destructive-state rule before allowing photos into a partially processed folder.

## Read surfaces

- persisted master-tree permission state;
- direct work-order folders under the selected address;
- exact selected work-order folder identity and name;
- all direct child item IDs and MIME types under that selected work-order folder.

## Write surfaces

Remote:

- deletion of confirmed direct child items of one exact selected work-order folder;
- rename of that same selected work-order folder after verified emptiness.

Local:

- selected work-order name/ID after verified successful rename;
- in-memory write-block state after uncertain/incomplete destructive results until refresh.

No auth scope, master-folder permission, account, dependency, INTERNET permission, signing, sharing, or parent-folder behavior changes.

## Focused automated coverage

Phase 3B focused tests cover:

- existing exact-match folder behavior remains unchanged;
- selected folder identity remains ID-based;
- confirmation child-ID snapshots compare independent of provider ordering;
- any added/removed/replaced child causes snapshot mismatch;
- direct-child snapshot count and child-folder count are preserved;
- existing same-work-order older-date eligibility tests from Phase 3A remain in the complete suite.

Provider deletion and rename success/failure cannot be proven by JVM helpers alone and require the real safe-folder gate below.

## Safe Drive fixture / reality gate

Use only:

`HNP Jobs → FIELD PHOTO PREP TEST`

Expected current old occurrence after Phase 3A:

`Cut Grass - 2026-09-13`

Target occurrence for successful Phase 3B test:

`Cut Grass - 2026-09-20`

### A. Confirmation and cancellation check

1. Put disposable direct child content inside `Cut Grass - 2026-09-13`.
2. In the app select that exact folder, enter `Cut Grass`, and choose `2026-09-20`.
3. Tap **Clear & Reuse Selected Folder**.
4. Confirm the warning shows the exact old folder name and correct direct child count.
5. Tap **Cancel**.
6. Inspect Drive and prove the old folder, all children, and absence of `Cut Grass - 2026-09-20` are unchanged.

### B. Successful Clear & Reuse

1. Repeat the same selection/request.
2. Confirm the warning.
3. Tap **Clear & Reuse**.
4. App must not report success until the old folder is confirmed empty and the same folder ID is verified with the new name.
5. Inspect Drive: the disposable direct children are gone, old dated name is gone, exactly one `Cut Grass - 2026-09-20` exists, and unrelated/sibling content is unchanged.
6. Refresh/reopen the app and confirm that same folder identity resolves normally.

### C. Changed-after-confirmation fail-closed check

Using disposable test content only, change the selected folder contents after the warning is prepared but before destructive execution if a practical device/provider path can do so. The app must detect a different child-ID set and stop without deleting the newly changed set.

### D. Provider failure gate

Before merge, force or reproduce one real child-deletion or rename failure in a safe fixture if the provider offers a practical deterministic method. Prove the app does not rename a partially cleared folder and requires refresh before another write.

Do not add a production destructive testing backdoor merely to manufacture this condition. If a deterministic provider-side failure cannot be produced safely, document the exact limitation and do not overclaim that this specific reality-gate item was proven.

## Automated final gate

On the exact final runtime head before merge:

1. focused tests pass;
2. complete repository CI passes once;
3. debug APK builds;
4. Android install/launch smoke passes;
5. APK artifact is packaged;
6. safe real-device confirmation/cancel/success checks pass;
7. any unresolved provider-failure-gate limitation is explicitly reported rather than hidden;
8. explicit operator approval is obtained before merge.

## Rollback

If Phase 3B fails verification or is rejected, leave `main` at:

`44aa0ff71b12c7fd11c0e1820872e9146d9730a7`

That preserves Phase 3A empty-folder reuse and contains no Clear & Reuse child deletion path.

## Pre-merge approval status

Pending final CI, real-device Drive verification, and explicit operator approval.
