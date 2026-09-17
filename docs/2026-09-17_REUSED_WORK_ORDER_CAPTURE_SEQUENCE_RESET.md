# Reused Work-Order Capture Sequence Reset — Impact Record

Date: 2026-09-17

Status: **LEVEL 3 — IMPLEMENTED; FOCUSED AUTOMATED GATE PASSED; FULL CI + DEVICE/DRIVE GATE PENDING**

Branch: `fix/reused-work-order-capture-sequence-reset-20260917`

Rollback/base commit: `07a8b8928aae4e022bc8209266bdf8db227a1061`

## Problem

Automatic capture-order filenames reserve sequence numbers durably for each stable work-order provider document ID. That is correct while one dated work occurrence remains in that folder, but Field Photo Prep intentionally supports reusing the same provider folder identity for a later dated occurrence through **Reuse Selected Empty Folder** and **Clear & Reuse**.

Because reuse keeps the provider ID, the previous implementation would continue the prior occurrence's capture sequence instead of starting the newly reused occurrence at `001`.

Example of the defect:

- old occurrence ends at `086_field-photo-...jpg`;
- the same Drive folder is cleared/renamed and reused for a new dated occurrence;
- first new capture would have become `087_field-photo-...jpg` instead of `001_field-photo-...jpg`.

## Approved behavior

A successfully reused work-order folder represents a **new work occurrence for capture-order numbering** even though its Android/Drive provider document ID remains the same remote destination identity.

After either approved reuse path successfully renames the exact same provider folder to the new dated work-order name:

- the new occurrence's first capture is `001_field-photo-<UUID>.jpg`;
- later captures advance `002`, `003`, and so on;
- discarded/failed reservations within that new occurrence still consume their numbers and are never reused;
- restart persistence continues the new occurrence's sequence;
- the provider document ID remains unchanged and remains the authoritative upload destination;
- existing old-occurrence photo metadata is not rewritten or renumbered;
- a later ordinary visible-folder rename does **not** itself reset numbering again.

## Change classification

**Level 3.** The change alters persisted capture-sequence bookkeeping and is coupled to the existing destructive Drive folder-reuse workflow. It must fail closed rather than allow a stale sequence or an old unconfirmed photo to cross into a newly reused occurrence.

## Owning files/functions

Runtime owners:

- `PendingPhotoStore.java`
  - durable capture-sequence ledger;
  - reuse reset preparation/completion;
  - capture reservation sequence assignment;
  - local preflight that blocks reuse when old unconfirmed photos still exist.
- `MainActivity.java`
  - existing empty-folder reuse and **Clear & Reuse** success paths;
  - prepares the local reset immediately before the existing reuse write boundary;
  - completes the reset only after the same provider ID is verified under the requested new dated name.
- `app/build.gradle`
  - internal build progression to versionCode 23 / `0.18-reused-work-order-sequence-reset`.

Focused regression owner:

- `AutomaticCaptureOrderFilenameTest.java`.

`PhotoCaptureActivity.java` is intentionally unchanged by this fix. Existing photo-list/history presentation remains as before.

## Read/write surfaces

Reads:

- existing app-private pending-photo metadata;
- existing app-private `capture-sequences.properties` ledger;
- existing selected work-order provider ID/name;
- existing Drive/provider reuse verification already owned by `MainActivity`/`DriveClient`.

Writes:

- app-private capture-sequence ledger for the new reset bookkeeping;
- existing Drive child deletion/rename behavior is not broadened or duplicated.

No new Drive permission, OAuth, folder hierarchy, file type, or remote write path is introduced.

## Persisted-data design

The existing sequence ledger remains the durable source for consumed capture numbers. Reuse adds a narrow pending-reset target for one exact work-order provider ID and requested new dated folder name, plus a marker that the provider ID has entered an FPP-managed reused occurrence.

Design requirements:

1. The reset is prepared only at the existing verified reuse boundary immediately before the folder rename that creates the new occurrence.
2. The reset becomes active only after the same provider ID is verified under the requested new dated name.
3. If the local completion write is interrupted after the Drive rename, the pending reset remains recoverable and the first capture for the verified target occurrence completes the reset before reserving `001`.
4. If a reset is pending but the currently selected folder does not match the requested new dated name, capture fails closed rather than guessing.
5. Once a reset is active for that provider ID, retained confirmed old-occurrence records do not raise the new occurrence's sequence baseline.
6. Existing installations without reset metadata retain the current sequence behavior until an approved reuse transition occurs.
7. After the reset is completed, the stable provider ID again governs sequence continuation; an ordinary later visible-folder rename does not create another reset.

The requested dated name is used only to verify/recover the explicit FPP-managed transition while the reset is pending. It is **not** promoted into permanent remote destination identity. The stable provider document ID remains authoritative for Drive writes.

## Old queued-photo safety

A reused provider ID points to a different dated work occurrence after rename. Therefore reuse must not proceed while any old local record for that provider ID remains unconfirmed (`CAPTURING`, `WAITING`, `UPLOADING`, `FAILED`, or `UNCERTAIN`). Otherwise a later retry could send an old-occurrence photo into the newly reused folder.

Before either reuse rename begins, the app requires that every retained local record bound to the candidate provider ID is already `UPLOADED`. Confirmed-upload metadata may remain as lightweight history/duplicate-protection evidence. It does not authorize another upload and, after the explicit reuse reset, it does not raise the new occurrence's capture-number baseline.

## Duplicate/idempotency behavior

- UUID identity remains unchanged.
- A reset does not rename, delete, copy, or rewrite prior photo metadata or remote files.
- A reset never reuses a consumed sequence inside the same occurrence.
- Re-running reset completion for the same pending reuse target is idempotent.
- Existing confirmed remote identities remain untouched.
- A later explicit FPP-managed reuse of the same provider folder may intentionally start another new occurrence at `001`.

## Offline/stale-state behavior

- Capture remains offline-capable after the reuse transition is locally committed.
- A pending reuse reset that cannot be reconciled safely blocks new capture for that provider ID rather than assigning a guessed number.
- Existing Drive/provider freshness and destructive-action guards remain unchanged.

## Automated verification

Focused workflow run `35183085393` passed on exact focused runtime/test head `7ae2f10180d6b197a158518e9d15edabadeb2408`.

The focused suite proves:

- existing same-occurrence numbering continues normally;
- restart continues normally;
- discarded reservation gaps remain consumed;
- legacy sequence compatibility and conservative bootstrap remain intact;
- corrupt ledger values still fail closed;
- reuse reset makes the next capture `001` even when old records/ledger show a higher sequence;
- second capture after reset becomes `002`;
- reset survives store reopen;
- pending reset self-recovers when the exact requested reused folder name is selected;
- pending reset blocks capture while the old/unexpected name is still selected;
- reuse preflight blocks a non-`UPLOADED` old local photo;
- confirmed old metadata does not raise the new baseline;
- a second later reuse of the same provider ID starts at `001` again;
- a consumed gap remains consumed inside the newly reused occurrence;
- after completed reuse, an ordinary later visible-folder rename continues by provider identity instead of resetting/blocking the counter.

Final requirement before device testing: the normal complete Android CI suite must pass once on the exact final branch head/PR.

## Safe Drive fixture plan

Use only a disposable test address/work-order folder.

Physical Android + real Google Drive gate:

1. Start from a disposable occurrence whose sequence is already above `001`.
2. Confirm no old local unconfirmed photo remains.
3. Use the existing **Clear & Reuse** flow to recycle that exact disposable work-order folder to a later date.
4. Verify the provider folder identity stays the same and old disposable Drive children are removed only through the approved confirmation flow.
5. Capture one photo in the newly reused occurrence and upload it.
6. Verify its remote name begins with `001_`, not the old occurrence's next number.
7. Capture a second photo and verify `002_`.
8. Restart FPP, capture another photo, and verify continuation rather than another reset.
9. Confirm no duplicate/wrong-parent upload and no unrelated Drive content changed.

Do not use a live customer/job folder.

## Protected behavior

Must remain unchanged:

- Drive master-tree permission model;
- stable provider document ID as remote destination identity;
- destructive **Clear & Reuse** confirmation/snapshot/freshness/deletion/rename guards;
- duplicate remote-create protection;
- protected-original retention;
- upload/retry/UNCERTAIN/reconciliation semantics;
- automatic preparation and batch upload sequencing;
- existing Photos history/list presentation;
- no automatic Drive cleanup outside approved **Clear & Reuse**;
- no renaming of already-uploaded photo files.

## Failure recovery

- Any focused or complete automated test failure stops publication/merge.
- If Drive reuse succeeds but local reset completion fails, keep the reset pending and do not guess a sequence. A later capture may complete the reset only when the selected provider ID is observed under the exact requested new dated name.
- If Drive reuse itself is incomplete, existing reuse failure handling remains authoritative and no new-work photo should be sent into that folder.
- Rollback is a narrow revert to base `07a8b8928aae4e022bc8209266bdf8db227a1061`; rollback must not delete queued photos or Drive content.

## Approval status

Implementation authorization: **APPROVED** by the operator in chat on 2026-09-17 (`Let's fix it.`).

Level 3 explicit pre-merge approval: **PENDING** until final complete CI and the required disposable real-device/Drive gate pass.
