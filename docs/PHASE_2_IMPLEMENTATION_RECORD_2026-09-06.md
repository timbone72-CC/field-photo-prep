# Phase 2 Implementation Record — Work-Order Folders

Date: 2026-09-06

## Approved scope

Phase 2 only:

- tap an existing address folder from the Phase 1 list;
- list and refresh the direct child work-order folders under that exact address folder;
- enter a human-readable work-order name and choose a local calendar date;
- build the exact initial folder name `Work Order - YYYY-MM-DD`;
- if exactly one folder with that exact name already exists, select/reuse it without creating another;
- if multiple exact same-named folders exist, require the operator to tap the intended folder and do not create another;
- if no exact match exists, create exactly one folder directly under the selected address;
- persist the selected address and work-order provider document identities/names locally for later workflow phases.

Explicitly excluded: address-folder creation, old-folder recycling, rename, deletion, Clear & Reuse, camera, photo preparation, photo upload, Free Map Router, workbook integration, route behavior, and general Drive management.

## Governed base and rollback

Exact base / rollback commit:

`46076c7e4c2aa256d0881b5ff758d1290a1a7dfd`

Phase 2 branch:

`feat/phase-2-work-order-folders`

## Change level

Level 3 because this phase introduces a real remote Drive folder write and persists work-order destination identity.

Explicit operator approval is required before merge, not before implementation while this approved scope remains unchanged.

## Architecture and identity mapping

The merged Phase 1 runtime uses Android Storage Access Framework (SAF), not app-held Google OAuth/REST tokens.

For this runtime, the governing contract term “Drive folder ID” maps to the stable document-provider folder ID inside the persisted master tree grant. The selected master tree URI scopes access. Display names are discovery/context only; stable provider document IDs own identity once selected or created.

Real boundary:

`persisted master tree URI → selected address document ID → work-order document ID → Android/Drive document-provider result → local selected-folder state`

## Required and optional data

Required to list/select:

- persisted readable master tree URI;
- selected address folder document ID and display name.

Required to create:

- persisted write permission for the same master tree;
- work-order descriptive text;
- valid local date;
- no exact existing work-order folder match.

Optional/convenience local state:

- current address ID/name;
- current work-order ID/name.

The new SharedPreferences keys are additive and optional. Existing Phase 1 preferences remain valid without migration.

## Read surfaces

- persisted tree permission state;
- direct child folders of the selected address;
- local selected address/work-order identities.

## Write surfaces

Remote:

- at most one new directory directly under the exact selected address folder, and only after an exact-name discovery check returns zero matches.

Local:

- current selected address ID/name;
- current selected work-order ID/name.

No existing Drive item is renamed, moved, deleted, overwritten, or shared by Phase 2.

## Duplicate and idempotency behavior

1. Refresh the selected address’s actual direct child folders before create/reuse decision.
2. Exact requested name means exact case-sensitive `Work Order - YYYY-MM-DD` string equality.
3. One exact match is reused by its existing stable document ID.
4. Multiple exact matches stop automatic selection/creation. The UI shows duplicate IDs only as a disambiguation suffix and requires the operator to tap one.
5. Zero exact matches permits one create request under the selected address document ID.
6. A confirmed create returns the new document identity, which is persisted as the selected work order.
7. Any create exception blocks another create attempt until the operator refreshes the work-order list. This prevents blind repeated creates after an uncertain provider/network result.
8. Restarting the app cannot create a duplicate by itself because creation always begins with a fresh exact-name check.

## Offline and stale-state behavior

- Read/create behavior depends on what the selected Android document provider can currently serve.
- A read failure changes no Drive content.
- A create failure is not automatically retried.
- A remembered work-order ID is revalidated against the selected address’s current child list before being treated as current in that screen.
- If the remembered work-order ID is no longer present, the app clears that remembered selection rather than guessing from a name.
- Changing the master folder clears remembered address/work-order selections.

## Focused tests

Automated coverage must prove:

- ordinary files are still excluded from folder lists;
- exact-name matching does not guess by case or partial text;
- multiple exact same-named folders remain multiple and require operator choice;
- work-order naming produces `Work Order - YYYY-MM-DD`;
- blank work-order names fail;
- invalid calendar dates fail.

## Safe Drive fixture / reality gate plan

Before merge, use a disposable test address folder under a safe master/test context rather than a live customer job when possible.

Real-device checks for this phase:

1. Open Phase 2 with persisted master-folder access.
2. Tap the disposable address folder and confirm its actual direct child folders are listed.
3. Create `Cut Grass - <test date>` and inspect Drive to confirm exactly one new folder exists directly under that address, not at master root.
4. Run the same Use/Create action again and confirm the existing exact folder is reused with no second folder created.
5. Refresh/reopen the address and confirm the same work-order document identity resolves.
6. If a disposable duplicate-name fixture is available, confirm multiple exact matches are visibly disambiguated and require a tap; no automatic create occurs.
7. Confirm unrelated sibling/address/master Drive content is unchanged.
8. Phase 2 does not test recycling, deletion, rename, camera, or uploads.

## Failure recovery

- Read failure: refresh when Drive/provider access is restored; no write occurred.
- Create error/uncertain result: the app disables another create until work-order folders are refreshed. The operator must inspect the refreshed real state before another create decision.
- Lost master-tree access: reselect the intended master through Android’s system folder picker.
- Wrong master selection: reselect the correct master; changing master clears current address/work-order memory.

## Pre-merge approval status

Pending explicit operator approval after automated checks and the Phase 2 real-device reality gate pass.
