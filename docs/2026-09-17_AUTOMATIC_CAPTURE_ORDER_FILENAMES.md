# Automatic Capture-Order Drive Filenames — 2026-09-17

## Problem

Field Photo Prep currently uploads each prepared JPEG with a deterministic UUID-only Drive name such as `field-photo-<uuid>.jpg`. Google Drive therefore cannot present a job in the operator's real capture order by sorting by name. A real 111-photo job had to be reconciled and manually reorganized after the fact.

## Approved behavior

For photos captured after this change, FPP will reserve and persist an immutable per-work-order capture sequence when the protected capture record is created. The Drive filename will use that sequence from the first remote create:

- `001_field-photo-<uuid>.jpg`
- `002_field-photo-<uuid>.jpg`
- ...
- `999_field-photo-<uuid>.jpg`
- `1000_field-photo-<uuid>.jpg` when a work order ever exceeds three digits.

The UUID remains part of the filename and remains the local photo identity. The sequence is ordering metadata, not destination identity.

Sequence numbers are consumed when a capture reservation is made. They are never reused or renumbered merely because a capture fails, an empty reservation is removed, or an eligible local photo is explicitly discarded. A gap is safer than changing or reusing a previously assigned position.

## Change classification

Level 3.

This changes persisted pending-photo metadata and the deterministic remote filename used by upload and UNCERTAIN reconciliation. It does not change master-tree permissions, address/work-order destination identities, sharing, deletion, folder creation/reuse, prepared JPEG bytes, or retry authority.

## Owning files

Expected runtime owners:

- `PendingPhotoRecord.java` — persisted optional capture sequence, schema v4 compatibility.
- `PendingPhotoStore.java` — reserve the next sequence for the exact stored work-order identity and durably retain the last consumed sequence.
- `DrivePhotoUploader.java` — derive the remote display name from the persisted record.
- `DrivePhotoReconciler.java` — derive the exact same expected remote name from the persisted record.
- `CaptureOrderManifest.java` — report stored sequence when available while preserving legacy diagnostic behavior.

Tests will cover the same ownership boundaries.

## Read/write surfaces

Reads:

- pending-photo metadata under app-private storage;
- the app-private capture-sequence ledger under the same pending-photo root;
- exact stored work-order provider identity already bound to each photo;
- Google Drive/DocumentsProvider metadata already used by upload/reconciliation.

Writes:

- new `captureSequence` value in pending-photo metadata for new captures;
- the app-private sequence ledger before accepting a new capture reservation;
- new remote JPEG display names for new sequenced photos only.

No existing Drive file is renamed, moved, deleted, copied, or rewritten by this feature.

## Persisted schema and compatibility

`PendingPhotoRecord` advances from schema version 3 to version 4 with an optional non-negative `captureSequence` field.

- New captures receive a positive sequence.
- Existing schema 1–3 records load with sequence `0` / legacy-unsequenced semantics.
- Schema 3 remains explicitly readable after the schema 4 upgrade.
- Existing records keep the UUID-only remote filename rule so old FAILED/UNCERTAIN reconciliation is not broken.
- Copy/state-transition methods preserve the stored sequence unchanged.

The sequence ledger is a separate app-private properties file, not a Drive file and not a second photo database. It stores only the last sequence consumed for a stable work-order provider identity. The ledger is written durably before the new photo reservation is accepted. If a later local reservation step fails, that number remains consumed and the next capture advances, leaving a safe gap.

If the ledger is absent during upgrade, FPP bootstraps conservatively from retained pending-photo history for that exact work-order provider identity. The baseline is the greater of the retained-record count and any stored capture sequence. After the ledger exists, its consumed value remains authoritative even if an eligible local photo record is later discarded.

## Identity and duplicate/idempotency behavior

- Local UUID identity is unchanged.
- Work-order provider document identity is unchanged and remains authoritative destination identity.
- New remote filename is a pure deterministic function of the persisted `(captureSequence, UUID)` pair.
- Retry uses the same stored sequence and therefore the same deterministic remote name.
- Reconciliation calculates the same deterministic name and retains exact ID/name/MIME/size/hash proof requirements.
- Legacy records use the legacy UUID-only name and retain their existing reconciliation semantics.
- Sequence allocation never authorizes upload or retry.
- The ledger is keyed by stable work-order provider identity, not visible Drive folder name, so a display rename cannot cause filename positions to restart and collide inside the same folder.

## Offline and stale-state behavior

Sequence reservation uses only durable app-private state for the same stable work-order identity and therefore does not require network access. Camera capture remains offline-capable.

Sequence reservation is serialized process-wide. If the ledger cannot be read, parsed, or durably updated, capture reservation fails closed before a shot is accepted rather than guessing the next number. Integer overflow also fails closed locally. A remote/provider failure cannot change a sequence already bound to a photo.

## Safe fixture plan

Use a disposable Google Drive work order, never a live customer job.

1. Capture at least four photos in one fresh test work order.
2. Confirm local records carry `001` through `004` in capture order.
3. Upload a subset first, then the remaining photos.
4. Verify Drive contains exactly one file per photo under the exact stored work-order parent, with names `001_...` through `004_...` matching capture order.
5. Verify no duplicates and no wrong-parent files.
6. In a disposable local/test context, consume a sequence and remove that eligible local photo or empty reservation, then prove the next capture advances rather than reusing the number.
7. Exercise a retry-safe failure in automated coverage and prove the deterministic filename does not change across retry.
8. Exercise UNCERTAIN reconciliation in automated coverage and prove it searches for the prefixed deterministic filename.
9. Verify unrelated disposable Drive content remains unchanged.

Do not manufacture an ambiguous live/provider create merely for testing.

## Focused tests

Required focused coverage:

- schema v1–3 records load as legacy/unsequenced;
- schema v4 round-trip preserves capture sequence;
- new captures allocate increasing sequence within one work order;
- separate work orders each start at `001`;
- restart / store re-open continues from the durable ledger;
- removal of the highest consumed local reservation/photo does not reuse its sequence;
- a missing ledger bootstraps conservatively from retained legacy records;
- a corrupt/invalid ledger fails capture closed rather than guessing;
- sequence values above 999 widen naturally, for example `1000_...`;
- uploader creates capture-prefixed `field-photo-<uuid>.jpg` names for sequenced records and the old UUID-only name for legacy records;
- write/verify checks the same deterministic name;
- reconciler expects the same sequenced name while legacy reconciliation remains unchanged;
- capture-order manifest uses persisted sequence where available.

## Protected behavior

Must remain unchanged:

- exact address/work-order provider identity binding;
- one protected local original per shutter press;
- prepared-copy behavior and bytes;
- provisional remote-ID barrier before write;
- one-at-a-time batch upload;
- confirmed-success bookkeeping;
- fail-closed UNCERTAIN semantics;
- no blind retry or duplicate create;
- local cleanup only after confirmed remote success;
- no Drive rename/move/delete/share operation from ordinary photo upload.

## Regression areas

Affected checklist sections: F, H, I, J, K, M.

## Rollback

Rollback point: branch base `c8c28faebe5db954759cb6014260dbe3bb34bf7f` (0.16 capture-order export build).

If the new naming path misbehaves, stop new affected uploads and revert the narrow sequence/naming change. Existing protected originals and queue metadata must not be deleted during rollback. Existing sequenced remote files are never automatically renamed back.

## Pre-merge approval

Implementation/testing is authorized by the operator request. Because this is Level 3, explicit operator approval is still required immediately before merge.
