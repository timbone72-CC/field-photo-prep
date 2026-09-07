# Phase 3A Implementation Record — Empty Work-Order Folder Reuse

Date: 2026-09-06

## Approved scope

Phase 3A only:

- keep the merged Phase 2 master-folder, address, work-order discovery, exact-match reuse, and new-folder creation behavior;
- let the operator explicitly select one existing dated work-order folder under the currently selected address;
- let the operator request a newer occurrence for the same exact work-order text;
- before any rename, re-read the selected address from the persisted master tree and re-resolve the selected candidate by stable document-provider folder ID;
- confirm no exact target `Work Order - YYYY-MM-DD` folder already exists;
- confirm the selected candidate is an older occurrence of the same exact work-order text;
- query the selected candidate's actual children and require zero child items of any type;
- rename that exact empty folder to the requested newer `Work Order - YYYY-MM-DD` name;
- re-read the parent after rename and verify the original folder ID still resolves with the new name;
- persist that same folder ID as the selected work occurrence.

Explicitly excluded from Phase 3A:

- deleting any Drive item;
- clearing a non-empty folder;
- **Clear & Reuse**;
- automatic folder recycling without an operator-selected candidate;
- address-folder rename/recycling;
- camera/photo preparation/upload;
- workbook or Free Map Router integration;
- general Drive cleanup or management.

## Governed base and rollback

Exact base / rollback commit:

`4736d5f80859a51375dd1df29143caecbc585bf4`

Phase 3A branch:

`feat/phase-3a-empty-work-order-reuse`

## Change level

Level 3 because this phase renames an existing Drive folder and changes the dated work occurrence represented by that folder while preserving destination identity.

Explicit operator approval is required before merge after automated verification and the real-device Drive gate.

## Architecture and identity

The app continues using Android Storage Access Framework (SAF) inside the operator-selected persisted master-folder tree. No OAuth/REST token, account-wide Drive scope, INTERNET permission, dependency, or signing behavior changes in Phase 3A.

For this runtime, the contract term Drive folder ID maps to the stable document-provider ID inside the persisted tree grant.

Identity boundary:

`persisted master tree URI → selected address document ID → operator-selected old work-order document ID → emptiness query → rename result → parent re-read → same work-order document ID with new name → persisted selected work-order state`

Visible folder names are discovery/context only. The selected old folder must be re-resolved by exact document ID before emptiness or rename.

## Operator flow

1. Open the intended address.
2. Tap the older dated work-order folder to select that exact folder.
3. Enter the same work-order text and choose the newer local date.
4. Tap **Reuse Selected Empty Folder**.
5. App re-reads the address child folders.
6. If the requested dated folder already exists, the old folder is not renamed; the existing requested folder is selected or duplicates require operator choice.
7. App verifies the selected old folder is still a direct child of the current address and is an older occurrence of the same exact work-order text.
8. App queries all direct children of the selected old folder, including ordinary files and subfolders.
9. If any child exists, stop with no rename and no deletion.
10. If zero children exist, rename the exact selected folder ID to the new dated name.
11. Re-read the address, verify the original folder ID still exists with the new name, and only then report success/persist the renamed occurrence.

## Read surfaces

- persisted master tree permission state;
- direct child folders of the selected address;
- all direct child item presence for the selected old work-order folder;
- local selected address/work-order identity.

## Write surfaces

Remote:

- one rename of one operator-selected, verified-empty work-order folder under the selected address.

Local:

- selected work-order ID/name after verified rename.

No file/folder deletion, move, sharing change, address-folder write, photo write, or permission change is performed.

## Eligibility and duplicate rules

A selected old folder is eligible only when:

- it is still directly under the selected address by stable ID;
- its visible name parses as `<work order> - YYYY-MM-DD`;
- parsed work-order text exactly equals the requested trimmed work-order text, including case;
- its parsed date is earlier than the requested date;
- no exact target dated folder currently exists; and
- its actual child query returns zero items.

If one exact target folder already exists, reuse/select that existing target and do not rename the old folder.

If multiple exact target folders exist, stop and require operator choice. Do not rename or create anything.

A same-name/different-case work order is not guessed as the same work type.

A malformed old folder name is not guessed as a reuse candidate.

## Non-empty behavior

A non-empty selected old folder must fail closed:

- no child is deleted;
- no folder is renamed;
- no new folder is created as a side effect of the reuse button;
- status clearly says that the folder is not empty and nothing was renamed or deleted.

**Clear & Reuse** is intentionally deferred to a later separately gated Level 3 phase.

## Rename verification and uncertain state

After rename, the app requires all of the following before success:

- Android/Drive returns a rename result;
- the returned document ID equals the pre-rename selected folder ID;
- re-reading the selected address resolves that same original ID;
- that same ID now has the exact requested dated name; and
- exactly one folder with the requested name exists and it is that same ID.

Any exception or uncertain rename result blocks further create/reuse writes until the operator refreshes the actual work-order folder list. This reuses the existing Phase 2 write-uncertainty guard so a possibly completed rename cannot be followed by a blind duplicate create or repeat rename.

A normal pre-write rejection such as non-empty, ineligible candidate, missing candidate, or existing target does not enter uncertain-write state because no rename was attempted.

## Offline/stale behavior

- If Drive/provider cannot re-read the selected address, no rename is attempted.
- If the selected candidate ID no longer exists under the address, no rename is attempted.
- If emptiness cannot be confirmed, no rename is attempted.
- Rename is never automatically retried after an exception.
- Operator must refresh Drive state before another write after an uncertain rename result.

## Focused automated coverage

Coverage includes:

- existing folder naming rules remain valid;
- older same-work-order/date candidate is eligible;
- same-date or newer candidate is rejected;
- different work-order text is rejected;
- case-changed work-order text is rejected rather than guessed;
- malformed old folder names are rejected;
- work-order text containing ` - ` still parses by the final date separator;
- exact folder identity lookup uses document ID rather than visible name;
- existing exact-name duplicate behavior remains operator-choice only.

Android document-provider emptiness/rename behavior is verified through the real-device safe Drive gate rather than pretending a JVM mock proves Google Drive behavior.

## Safe Drive fixture / reality gate

Use the already disposable fixture:

`HNP Jobs → FIELD PHOTO PREP TEST`

Expected initial reusable folder from Phase 2 testing:

`Cut Grass - 2026-09-06`

### Empty-folder positive check

1. Confirm `Cut Grass - 2026-09-06` is empty in Drive.
2. In the app, open `FIELD PHOTO PREP TEST` and tap that exact folder.
3. Enter `Cut Grass` and choose `2026-09-13`.
4. Tap **Reuse Selected Empty Folder**.
5. App must report successful empty-folder reuse only after same-ID verification.
6. Inspect Drive: old name is gone, exactly one `Cut Grass - 2026-09-13` exists directly under `FIELD PHOTO PREP TEST`, and unrelated content is unchanged.
7. Refresh/reopen the app and confirm the renamed folder resolves normally without a duplicate.

### Non-empty fail-closed check

1. Put one disposable file inside `Cut Grass - 2026-09-13`.
2. Select that folder in the app, request `Cut Grass - 2026-09-20`, and tap **Reuse Selected Empty Folder**.
3. App must say the selected old folder is not empty and that nothing was renamed or deleted.
4. Inspect Drive: folder must still be `Cut Grass - 2026-09-13`, disposable child still exists, and no `Cut Grass - 2026-09-20` was created by the reuse action.
5. Remove the disposable child after the check if desired.

No live customer/job folder is used as the Phase 3A test target.

## Failure recovery

- Read/eligibility/emptiness failure before rename: refresh or correct the selection; no Drive write occurred.
- Rename exception/uncertainty: refresh the selected address before any further create/reuse write. The app must reconcile the actual folder name/ID first.
- Lost master tree access: reselect the intended master through Android's picker.
- Unexpected identity change after rename: do not treat the folder as ready for later photo upload; refresh and inspect the safe fixture before proceeding.

## Pre-merge approval status

Pending explicit operator approval after CI and the Phase 3A real-device reality gate pass.
