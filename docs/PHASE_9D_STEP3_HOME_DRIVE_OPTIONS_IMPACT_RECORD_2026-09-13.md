# Phase 9D Step 3 — Home Drive Options Impact Record

Date: 2026-09-13
Status: **IMPLEMENTED — AUTOMATED VERIFICATION PENDING**
Branch: `feat/concept-3-ui-makeover-20260912`
PR: #35
Rollback baseline: `5e588f862ce197b13779f4ebfe4f3c9fa9782ff9`

## Problem

The Concept 3 Home `⋯` control was wired to a one-item Android `PopupMenu`, but physical Samsung use reported that the control did not reliably expose its action. The only intended action is `Change Drive`; this is not a Settings menu.

The audit also found that the overflow remained enabled while another Drive operation was busy, unlike the other Drive-changing controls.

## Step 3 scope

Keep the Home `⋯` as a secondary Drive-options surface and make its single action deterministic:

1. tapping `⋯` opens a standard `Drive options` dialog;
2. the dialog contains only `Change Drive` plus `Cancel`;
3. selecting `Change Drive` calls the existing `chooseMasterFolder()` owner;
4. `chooseMasterFolder()` still launches the same `ACTION_OPEN_DOCUMENT_TREE` request with the existing read/write/persistable/prefix flags;
5. the Home overflow is disabled while the app is busy with a Drive operation and re-enabled only when a saved master exists and the app is not busy.

No Settings entry was added.

## Runtime file changed

- `app/src/main/java/com/inandout/fieldphotoprep/MainActivity.java`
  - replaces the one-item `PopupMenu` with a standard one-item `AlertDialog`;
  - keeps `chooseMasterFolder()` unchanged apart from formatting;
  - disables Home Drive options during busy Drive work;
  - restores enabled state from the existing saved-master/busy state.

## Focused test

- `app/src/androidTest/java/com/inandout/fieldphotoprep/HomeDriveOptionsInstrumentedTest.java`
  - presses the production Home overflow;
  - verifies `Change Drive` is exposed and `Settings` is absent;
  - verifies the dialog remains dismissible without touching Android's external document picker;
  - verifies the overflow is disabled by the existing busy-state owner.

The first test draft attempted to intercept the external `ACTION_OPEN_DOCUMENT_TREE` activity from instrumentation. That made the emulator run hang instead of testing the app-local UI deterministically, so that external-system dependency was removed. The runtime `chooseMasterFolder()` implementation and its existing SAF flags were not changed; actual picker launch remains part of the focused Samsung reality check below.

## Protected behavior

Unchanged:

- SAF result handling;
- persistable tree permission ownership;
- master folder `DocumentId` identity;
- Drive reads/writes after a folder is selected;
- address/work-order behavior;
- photo destination identity;
- protected originals;
- queue/upload/retry/UNCERTAIN/reconciliation/cleanup behavior;
- camera behavior.

No Settings screen or new Drive capability is introduced.

## Rollback

Revert the Step 3 commits to return to exact Step 2 baseline `5e588f862ce197b13779f4ebfe4f3c9fa9782ff9`.

## Physical check after the repair set

On Samsung:

1. tap Home `⋯` and confirm the `Drive options` dialog appears every time;
2. confirm it contains `Change Drive` and no Settings entry;
3. tap `Change Drive` and confirm the normal Android folder picker opens;
4. cancel the picker without changing the current Drive connection if only verifying the doorway.

No destructive Drive test is required for this UI repair.
