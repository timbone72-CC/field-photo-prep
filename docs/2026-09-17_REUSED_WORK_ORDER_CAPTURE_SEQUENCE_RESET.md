# Reused Work-Order Capture Sequence Reset — Impact Record

Date: 2026-09-17

Status: **LEVEL 3 — IMPLEMENTATION AUTHORIZED; PRE-MERGE APPROVAL PENDING**

Branch: `fix/reused-work-order-capture-sequence-reset-20260917`

Rollback/base commit: `07a8b8928aae4e022bc8209266bdf8db227a1061`

## Problem

Automatic capture-order filenames currently reserve sequence numbers by stable work-order provider document ID. That is correct while one dated work occurrence remains in that folder, but Field Photo Prep intentionally supports reusing the same provider folder identity for a later dated occurrence through **Reuse Selected Empty Folder** and **Clear & Reuse**.

Because reuse keeps the provider ID, a newly reused occurrence would currently continue the prior occurrence's capture sequence instead of starting at `001`.

Example of the defect:

- old occurrence ends at `086_field-photo-...jpg`;
- the same Drive folder is cleared/renamed and reused for a new dated occurrence;
- first new capture would become `087_field-photo-...jpg` instead of `001_field-photo-...jpg`.

## Approved behavior

A successfully reused work-order folder represents a **new work occurrence** for capture-order numbering even though its Android/Drive provider document ID remains the same remote destination identity.

After either approved reuse path successfully renames the exact same provider folder to the new dated work-order name:

- the new occurrence's first capture is `001_field-photo-<UUID>.jpg`;
- later captures advance `002`, `003`, and so on;
- discarded/failed reservations within that new occurrence still consume their numbers and are never reused;
- restart persistence still continues the new occurrence's sequence;
- the provider document ID remains unchanged and remains the authoritative upload destination;
- existing old occurrence photo metadata is not rewritten or renumbered.

## Change classification

**Level 3.** The change alters persisted capture-sequence bookkeeping and is coupled to the existing destructive Drive folder-reuse workflow. It must fail closed rather than allow a stale sequence or an old unconfirmed photo to cross into a newly reused occurrence.

## Owning files/functions

Expected runtime owners:

- `PendingPhotoStore.java`
  - durable capture-sequence ledger;
  - reuse reset preparation/completion;
  - capture reservation sequence assignment;
  - local preflight that blocks reuse when old unconfirmed photos still exist.
- `MainActivity.java`
  - existing empty-folder reuse and **Clear & Reuse** success path;
  - request/reset capture-order occurrence only after the exact reuse transition is verified.
- `PhotoCaptureActivity.java`
  - current-occurrence local photo filtering so uploaded history from the prior renamed occurrence is not presented as the new occurrence's photo list.

Expected tests:

- `AutomaticCaptureOrderFilenameTest.java` plus focused reuse/identity tests as needed.

## Read/write surfaces

Reads:

- existing app-private pending-photo metadata;
- existing app-private `capture-sequences.properties` ledger;
- existing selected work-order provider ID/name;
- existing Drive/provider reuse verification already owned by `MainActivity`/`DriveClient`.

Writes:

- app-private capture-sequence ledger only for the new reset bookkeeping;
- existing Drive child deletion/rename behavior is not broadened or duplicated.

No new Drive permission, OAuth, folder hierarchy, file type, or remote write path is introduced.

## Persisted-data design

The existing sequence ledger remains the durable source for consumed capture numbers. Reuse adds a narrow reset state for one exact work-order provider ID and requested new dated folder name.

Design requirements:

1. The reset is prepared only at the existing verified reuse boundary immediately before the folder rename that creates the new occurrence.
2. The reset becomes active only after the same provider ID is verified under the requested new dated name.
3. If the local completion write is interrupted after the Drive rename, the pending reset remains recoverable and the first capture for the verified target occurrence completes the reset before reserving `001`.
4. If a reset is pending but the currently selected folder does not match the requested new dated name, capture fails closed rather than guessing.
5. Once a reset is active for that provider ID, old occurrence records do not raise the new occurrence's sequence baseline.
6. Existing installations without reset metadata retain the current sequence behavior until an approved reuse transition occurs.

This avoids using a visible folder name as remote destination identity. Provider document ID remains authoritative for Drive writes; the requested dated name is used only to verify/recover the explicit FPP-managed reuse transition.

## Old queued-photo safety

A reused provider ID points to a different dated work occurrence after rename. Therefore reuse must not proceed while any old local record for that provider ID remains unconfirmed (`CAPTURING`, `WAITING`, `UPLOADING`, `FAILED`, or `UNCERTAIN`). Otherwise a later retry could send an old-occurrence photo into the newly reused folder.

Before either reuse rename begins, the app will require that every retained local record bound to the candidate provider ID is already `UPLOADED`. Confirmed-upload metadata may remain as lightweight duplicate/history evidence; it does not authorize a new upload.

## Current-occurrence UI filtering

After successful reuse, prior `UPLOADED` metadata may still exist locally under the same provider ID. The Photos surface must treat the exact captured `workOrderName` snapshot together with the provider ID as local occurrence context for display/selection only, so prior occurrence history is not mixed into the newly renamed occurrence.

This local display/filter rule does not change upload destination identity: each photo still uploads only to its immutable stored provider document ID.

## Duplicate/idempotency behavior

- UUID identity remains unchanged.
- A reset does not rename, delete, copy, or rewrite prior photo metadata or remote files.
- A reset never reuses a consumed sequence inside the same occurrence.
- Re-running reset completion for the same pending reuse target is idempotent.
- Existing confirmed remote identities remain untouched.

## Offline/stale-state behavior

- Capture remains offline-capable after the reuse transition is locally committed.
- A pending reuse reset that cannot be reconciled safely blocks new capture for that provider ID rather than assigning a guessed number.
- Existing Drive/provider freshness and destructive-action guards remain unchanged.

## Safe Drive fixture plan

Use only a disposable test address/work-order folder.

Physical Android + real Google Drive gate:

1. Create/capture/upload at least two photos so the old occurrence has a sequence above `001`.
2. Confirm no old local unconfirmed photo remains.
3. Use the existing **Clear & Reuse** flow to recycle that exact disposable work-order folder to a later date.
4. Verify the provider folder identity stays the same and old disposable Drive children are removed only through the approved confirmation flow.
5. Capture one photo in the newly reused occurrence and upload it.
6. Verify its remote name begins with `001_`, not the old occurrence's next number.
7. Capture a second photo and verify `002_`.
8. Restart FPP, capture another photo, and verify continuation rather than reset.
9. Confirm unrelated Drive content is unchanged and no old-occurrence local record appears as selectable new-occurrence work.

Do not use a live customer/job folder.

## Focused automated tests

At minimum prove:

- existing same-occurrence numbering still continues normally;
- restart continues normally;
- discarded reservation gaps remain consumed;
- reuse reset makes the next capture `001` even when old records/ledger show a higher sequence;
- second capture after reset becomes `002`;
- reset state survives store reopen;
- pending reset self-recovers when the selected provider ID has the verified requested new name;
- pending reset blocks capture when the selected folder still has the old/unexpected name;
- reuse preflight blocks any non-`UPLOADED` old local photo state;
- confirmed old metadata does not block reuse;
- current-occurrence filtering excludes prior renamed-occurrence history.

Final requirement: complete repository automated suite once on the exact final runtime head.

## Protected behavior

Must remain unchanged:

- Drive master-tree permission model;
- stable provider document ID as remote destination identity;
- destructive **Clear & Reuse** confirmation/snapshot/freshness/deletion/rename guards;
- duplicate remote-create protection;
- protected-original retention;
- upload/retry/UNCERTAIN/reconciliation semantics;
- automatic preparation and batch upload sequencing;
- no automatic Drive cleanup outside approved **Clear & Reuse**;
- no renaming of already-uploaded photo files.

## Failure recovery

- Any focused or complete automated test failure stops publication/merge.
- If Drive reuse succeeds but local reset completion fails, keep the reset pending and do not guess a sequence. A later capture may complete the reset only when the selected provider ID is verified under the exact requested new dated name.
- If Drive reuse itself is incomplete, existing reuse failure handling remains authoritative and no new-work photo should be sent into that folder.
- Rollback is a narrow revert to base `07a8b8928aae4e022bc8209266bdf8db227a1061`; rollback must not delete queued photos or Drive content.

## Approval status

Implementation authorization: **APPROVED** by the operator in chat on 2026-09-17 (`Let's fix it.`).

Level 3 explicit pre-merge approval: **PENDING** until focused tests, final complete CI, and the required disposable real-device/Drive gate pass.
