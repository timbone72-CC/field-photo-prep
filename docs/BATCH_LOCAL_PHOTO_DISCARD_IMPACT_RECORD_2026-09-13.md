# Batch Local Photo Discard — Level 3 Impact Record

Date: 2026-09-13
Status: IMPLEMENTATION AUTHORIZED — PRE-MERGE APPROVAL PENDING
Branch: `feat/batch-local-photo-discard-20260913`
Stacked base: `feat/concept-3-ui-makeover-20260912` at `4e8bc4e678d2fbc898b0e78b4b3bfe8a566e1a91`

## Problem

Physical Samsung testing confirmed that a camera session can produce several photos, but the Photos screen can discard only one photo at a time through the per-photo action menu. Field use needs the operator to select multiple bad local photos and discard them with one confirmation.

## Approved behavior

1. The existing Photos checkboxes become general temporary photo selection rather than upload-only selection.
2. A checkbox is available only when the photo is eligible for at least one safe selected action. Current approved selected actions are upload and local discard.
3. `Select All Ready` keeps its existing meaning: it selects currently upload-eligible prepared photos only.
4. `Upload Selected (N)` is enabled only when every currently selected photo is still a valid normal-upload item.
5. A new `Discard Selected (N)` action is enabled only when every currently selected photo is locally discard-safe.
6. Local discard remains limited by the existing `PendingPhotoRecord.canDiscardLocally()` rule: `WAITING` and retry-safe `FAILED` are allowed; `CAPTURING`, `UPLOADING`, `UNCERTAIN`, and `UPLOADED` are refused.
7. One confirmation identifies the selected work order and exact selected count and states that the action removes app-private local original/prepared copies only and never deletes Drive content.
8. Immediately before deletion begins, the complete selected ID snapshot is re-read. Every item must still exist, belong to the currently open exact work order/address, and remain locally discard-safe. Any failed preflight aborts the whole batch before deletion starts.
9. During deletion, each item is re-read again immediately before its local removal. If state unexpectedly becomes unsafe, stop before that item and leave all later items untouched.
10. A filesystem failure stops the batch. Already completed local deletions remain completed; later items remain untouched. The app reports the completed count and failure instead of pretending all selected photos were discarded.
11. Batch discard never performs a Drive/provider write, delete, move, rename, permission change, upload, retry, or reconciliation operation.
12. Existing individual-photo discard remains available and uses the same local deletion helper/safety rule.

## Change classification

Level 3 because the feature intentionally deletes unconfirmed app-private originals after explicit operator confirmation. It does not change Drive deletion scope, queue schema, destination identity, SAF permissions, or remote retry semantics.

## Owning surfaces

Expected runtime owners:

- `PhotoCaptureActivity.java` — selected-action gating, confirmation, preflight, sequential local-discard orchestration, and result reporting.
- `PendingPhotoStore.discard(...)` — remains authoritative for refusing unsafe queue states and deleting protected local original/metadata.
- `PhotoPreparer.preparedFile(...)` — existing prepared derivative path; local prepared copy is removed only as part of an approved local discard.
- `screen_photos.xml` — adds `Discard Selected (N)` beside the existing selected upload action.

No Drive class, SAF owner, queue schema, camera class, or destination identity owner is to change.

## Read surfaces

- current UI/session selected photo IDs;
- exact current address/work-order identities;
- persisted pending-photo records;
- existing `canDiscardLocally()` state guard;
- existence of the local prepared derivative.

## Write/delete surfaces

Only app-private local files/metadata for explicitly selected, preflight-approved photos:

- prepared derivative for the selected photo ID, if present;
- protected local original and its pending metadata through the existing `PendingPhotoStore.discard(...)` owner.

No remote/Drive write surface is involved.

## Schema, identity, permissions, and provider access

- no schema change;
- no migration;
- no folder or photo destination identity change;
- no SAF permission change;
- no master-folder assumption change;
- no provider call is required for batch discard;
- no Drive file is deleted.

## Duplicate/idempotency behavior

- selected IDs are held in a `LinkedHashSet`, so one local photo identity cannot intentionally be discarded twice in one batch;
- preflight requires every selected ID to resolve to its persisted local record before deletion begins;
- a previously discarded/missing ID fails preflight rather than being treated as successful;
- remote duplicate/upload evidence states remain protected because `UNCERTAIN`, `UPLOADING`, and `UPLOADED` cannot pass `canDiscardLocally()`.

## Offline/stale-state behavior

The feature is local-only and may operate offline. It does not rely on Drive/provider freshness. Persisted queue state is re-read before deletion rather than trusting rendered UI state. If queue state changes or cannot be read safely, deletion fails closed.

## Focused automated coverage

Add focused coverage proving:

- general selection permits an unprepared `WAITING` photo to be selected for local discard;
- `Select All Ready` still selects only upload-eligible prepared photos;
- Upload Selected disables when the selection contains a non-upload-ready item;
- Discard Selected enables only when every selected item is locally discard-safe;
- an unsafe `UNCERTAIN`, `UPLOADING`, or `UPLOADED` item cannot be batch-discarded;
- complete preflight aborts before any deletion when one selected ID is unsafe/missing/wrong-work-order;
- a valid multi-photo local batch removes only its selected local originals/metadata/prepared copies;
- unselected photos remain intact;
- no Drive owner is invoked by the local discard path;
- individual discard continues to use the same safety/deletion helper.

Then run the complete Android CI suite once on the final runtime head.

## Physical smoke check

On Samsung, using disposable newly captured local photos only:

1. capture several photos in one work order;
2. select at least two local discard-safe photos;
3. verify `Discard Selected (N)` shows the exact count;
4. confirm once;
5. verify exactly those photos disappear locally and an unselected photo remains;
6. do not use an `UNCERTAIN` or live customer Drive scenario to prove this local-only behavior.

No Drive upload/delete reality gate is required because the implementation must not touch Drive/provider code.

## Primary risks and mitigations

- **Wrong local photo deleted:** selected IDs are snapshot/preflighted against exact persisted address/work-order identity, then re-read before each deletion.
- **Unsafe queue evidence deleted:** existing `canDiscardLocally()` guard remains authoritative and store-level `discard(...)` enforces it again.
- **Partial batch falsely reported complete:** stop on first failure and report completed count.
- **Drive content accidentally affected:** no Drive owner or provider call is in the batch discard path.

## Rollback

Rollback target is the stacked Concept 3 head `4e8bc4e678d2fbc898b0e78b4b3bfe8a566e1a91`. Reverting the batch-discard commits must not remove or rewrite surviving queued-photo records.

## Merge approval

Implementation is authorized by the operator request to continue. Because this is Level 3 deletion behavior, explicit operator approval is still required before this branch is merged.