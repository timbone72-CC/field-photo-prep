# Phase 3B Implementation Record — Clear & Reuse

Date: 2026-09-07

## Approved scope

Phase 3B only:

- keep merged Phase 3A empty-folder reuse behavior;
- let the operator explicitly choose **Clear & Reuse** for one selected non-empty old work-order folder;
- re-resolve the selected address and work-order by stable document-provider identity before destructive work;
- verify the selected folder is an older occurrence of the same exact work-order text;
- confirm the requested new dated folder does not already exist;
- enumerate every direct child item of the selected old folder;
- show the full destructive hierarchy before confirmation: selected master, selected address, old work-order folder, direct-child count, and requested new work-order folder;
- warn when any direct child is itself a folder because deleting that child folder also removes its contained descendants;
- require explicit operator confirmation;
- after confirmation, re-read the selected folder and child IDs and stop if the confirmed set changed;
- delete only the direct children that were explicitly included in the confirmed snapshot;
- fail fast on the first deletion failure;
- verify the selected work-order folder contains zero direct children after deletion;
- only after confirmed emptiness, rename that same work-order folder to the requested `Work Order - YYYY-MM-DD` name;
- verify the same stable folder ID remains under the same address and is the only exact target after rename;
- persist that same folder identity as the selected work occurrence.

Explicitly excluded:

- automatic clearing or recycling;
- deleting the selected work-order folder itself;
- deleting or recycling address folders;
- deleting sibling work-order folders;
- arbitrary Drive cleanup or file-manager behavior;
- sharing/permission changes;
- camera/photo preparation/upload;
- workbook or Free Map Router integration.

## Governed base and rollback

Exact base / rollback commit:

`44aa0ff71b12c7fd11c0e1820872e9146d9730a7`

Phase 3B branch:

`feat/phase-3b-clear-reuse`

## Change level

Level 3 because this phase intentionally deletes existing remote child content before reusing a work-order folder.

Explicit operator approval is required before merge after automated verification and the real-device Drive gate.

## Architecture and identity

The app continues using Android Storage Access Framework (SAF) inside the operator-selected persisted master-folder tree. No OAuth/REST token, account-wide Drive scope, INTERNET permission, dependency, signing, or sharing behavior changes in Phase 3B.

For this runtime, governed Drive folder identity maps to the document-provider ID inside the persisted tree grant.

Destructive boundary:

`persisted master tree URI → selected address document ID → selected old work-order document ID → direct-child ID snapshot → explicit operator confirmation → snapshot revalidation → child deletions → confirmed empty folder → rename → same work-order document ID with new name → persisted selected work-order state`

Visible folder names are display/context only. Delete and rename authorization comes from the exact selected identities plus explicit confirmation.

## Confirmation contract

Before any deletion, the dialog must show:

- master folder name;
- address folder name;
- old selected work-order folder name;
- direct child-item count;
- requested new work-order folder name;
- a warning if any direct child is itself a folder.

The dialog provides **Cancel** and **Clear & Reuse** actions. Cancelling or dismissing changes nothing.

The confirmation is bound to the exact child-ID snapshot. After confirmation, the app re-reads the current child IDs. If the set differs, no deletion begins and the operator must review Clear & Reuse again.

## Deletion and failure behavior

- Delete only the confirmed direct child IDs of the selected work-order folder.
- Never delete the selected work-order folder itself.
- A direct child folder is treated as one direct item; Android/provider deletion of that folder also removes its descendants, which is why the confirmation warns about child folders.
- Stop on the first deletion failure.
- After any partial deletion failure, the old work-order folder is not renamed.
- After partial/uncertain destructive state, block further create/reuse Drive writes until refresh and operator inspection.
- After all requested deletions report success, re-read the selected folder and require zero children before rename.
- Rename failure after successful clearing leaves the folder empty but not ready for new work; further writes remain blocked until refresh/inspection.

## Duplicate and stale-state rules

- If the requested dated target already exists, do not clear the old folder; select/reuse the existing target or require operator choice if duplicated.
- If the selected old folder ID is no longer under the selected address, stop before deletion.
- If its visible name changed before confirmation or between confirmation and deletion, stop.
- If it is no longer an older same-work-order occurrence, stop.
- If the confirmed child-ID set changed before deletion, stop.
- If the app cannot read contents with certainty, stop.
- No blind retry after a destructive failure or uncertain result.

## Automated coverage

Focused automated coverage includes:

- existing folder MIME/discovery behavior;
- exact-name duplicate handling;
- exact folder identity lookup by document ID;
- child snapshot count and child-folder count;
- stable child-ID comparison independent of enumeration order;
- changed child-ID set rejection;
- existing Phase 3A work-order eligibility/naming coverage.

Real Google Drive SAF deletion/rename behavior is verified through the safe device gate rather than overclaiming from JVM mocks.

## Safe Drive fixture / reality gate

Use only:

`HNP Jobs → FIELD PHOTO PREP TEST`

No live customer/job folder is a test target.

### Pre-destructive hierarchy check

During the first Phase 3B device setup, reinstall/reselection left the Android folder picker positioned inside a nested test folder and the operator accidentally selected a nested folder as the app master. No Phase 3B deletion was executed. The app's visible master/address labels exposed the mistake before destructive confirmation.

The operator then reselected the intended `HNP Jobs` master and reopened `FIELD PHOTO PREP TEST`. The app correctly displayed:

- `Master folder: HNP Jobs`
- `Address: FIELD PHOTO PREP TEST`

As a safety hardening response, the Phase 3B confirmation was changed to display the full hierarchy itself before the destructive action is available. The Android version code was then bumped so this hardened build can install in place over the earlier Phase 3B test build without clearing the corrected master selection.

### Cancellation check

1. Ensure one selected old test work-order folder has disposable direct content.
2. Select that old folder and request a newer date for the same work-order text.
3. Tap **Clear & Reuse Selected Folder**.
4. Confirm the dialog shows correct master, address, old folder, direct-child count, and new folder.
5. Tap **Cancel**.
6. Inspect Drive: old folder and every disposable child remain unchanged, and no new target folder exists.

### Successful Clear & Reuse check

1. Open the same controlled fixture and confirm disposable content only.
2. Open Clear & Reuse and verify the full path/count again.
3. Confirm **Clear & Reuse**.
4. App must delete only the selected old folder's direct children.
5. App must verify zero children before rename.
6. App must rename/reuse that same folder identity for the new date.
7. Inspect Drive: new dated name exists exactly once under `FIELD PHOTO PREP TEST`; old dated name is gone; selected folder identity is preserved; unrelated sibling/address content is unchanged.
8. Refresh/reopen and confirm the renamed folder resolves normally.

### Failure-path reality check

Attempt a safe deterministic provider deletion or rename failure only if it can be produced without adding a production destructive testing backdoor or risking unrelated Drive content. If the provider cannot be safely forced to fail deterministically, document that limitation and rely on fail-fast code review/automated guards plus the successful real-provider path; do not claim a real provider failure test happened when it did not.

## Automated verification

Original Phase 3B runtime commit:

`0024d6c4b8d55593af2edd98f4ced1532f80486f`

passed Android CI run `34116076640`.

Hierarchy-warning runtime commit:

`4a978d4fcdfb9a3c3676494d7273eee46de9e6e5`

passed Android CI run `34244198117`.

Final in-place-update runtime head:

`4d9dcdf08c4020871bf8e736e4e1dbaa576d62de`

passed Android CI run `34244802654`:

- unit tests passed;
- debug APK build passed;
- Android install/launch smoke test passed;
- APK artifact packaged successfully.

## Merge status

Not authorized yet. Real-device cancellation/success evidence and explicit Level 3 operator approval are still required before merge.
