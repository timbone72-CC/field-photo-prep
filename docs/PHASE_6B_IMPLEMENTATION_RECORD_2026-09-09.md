# Phase 6B Implementation Record — Drive Upload

Date: 2026-09-09

## Goal

Connect one prepared Phase 6A photo to the exact immutable work-order provider document identity already stored on its Phase 7A queue record, create/write the JPEG through the persisted Android SAF tree, and persist confirmed remote file identity only after the provider operation returns a verifiable result.

The priority order is: exact destination identity → preserve unconfirmed local photo → avoid blind duplicate upload → convenience.

## Approved scope

Phase 6B only:

- add a deterministic Drive filename derived from immutable local photo UUID;
- require a non-empty prepared JPEG before any upload attempt begins;
- use the queued photo's stored `workOrderId` as the only upload parent identity;
- resolve that exact provider document through the persisted master tree and require it to be a folder before remote creation;
- persist `UPLOADING` before the first remote write-capable operation;
- create one `image/jpeg` document under that exact work-order provider document ID;
- write the prepared-copy bytes through Android SAF / `ContentResolver`;
- verify the returned provider document identity and readable created-document metadata before treating the result as confirmed;
- persist the returned provider document ID through Phase 7A `markUploadConfirmed(...)`;
- classify deterministic pre-create failures as `FAILED` and therefore retryable;
- classify create/write/post-create verification failures as `UNCERTAIN` because a remote side effect may already exist;
- keep protected original and prepared copy intact on `FAILED`, `UNCERTAIN`, and `UPLOADED` in this phase;
- expose an explicit upload action and result state in the existing photo screen;
- perform provider I/O off the Android UI thread;
- prevent two concurrent upload attempts for the same process/UI workflow.

Explicitly excluded:

- automatic/background retry scheduling;
- resolving an `UNCERTAIN` result by searching Drive;
- automatic deletion of local original/prepared copies after success;
- bulk upload of all waiting photos;
- upload destination fallback by current UI selection or visible folder name;
- folder creation/reuse/rename/delete changes;
- sharing/permission changes;
- camera or compression policy changes;
- general Drive file management.

## Dependency / branch

Branch: `feat/phase-6b-drive-upload`

Stack base: Phase 7A documentation head `055807c184ce5eb6be2077f4d886ae1c3ba7b358`.

Exact tested Phase 7A runtime base: `95ae6a0c46e48c9f77ca85412ad86f650a4d9459`.

Phase 6B must not merge ahead of the Phase 5 → Phase 6A → Phase 7A dependency chain.

## Change level

Level 3.

Reason: this is the first real photo write to the selected Google Drive-backed document tree and directly affects upload destination, duplicate prevention, remote identity, and unconfirmed-photo safety.

Explicit operator approval is required before merge after automated verification and the real Android Google Drive reality gate.

## Ownership

- `PendingPhotoRecord` / `PendingPhotoStore`: queue truth, immutable destination binding, attempt/result persistence.
- `PhotoPreparer`: non-destructive prepared JPEG production only; unchanged by this phase except where existing API is consumed.
- `DrivePhotoUploader`: exact provider destination validation, create/write/verify flow, and remote certainty classification.
- `PhotoUploadCoordinator`: translates Drive result certainty into Phase 7A queue transitions.
- `PhotoCaptureActivity`: user initiation, background execution, state display; it does not construct or override the destination identity.

## Remote filename / duplicate key

Initial Drive filename:

`field-photo-<local-photo-uuid>.jpg`

The UUID is already immutable and unique per local capture. The filename is deterministic so later uncertainty reconciliation has a stable discovery key; visible address/work-order names are not part of identity.

A retry after a true `FAILED` result uses the same filename and same stored work-order provider ID. `UNCERTAIN` and `UPLOADED` are not retryable.

## Exact destination rule

Upload uses only the `workOrderId` stored on the photo when capture began.

The currently open address/work order is display context only and may never override that stored ID.

Before create, the Drive layer builds/resolves the exact document URI under the persisted master tree and confirms that the stored work-order document exists and is a folder. A missing/revoked/inaccessible destination fails before create and is retryable because no remote photo create has begun.

Visible folder name mismatch/rename does not redirect the upload and does not replace stable provider identity.

## Certainty boundary

There is a strict boundary around the first remote write-capable operation.

Before `DocumentsContract.createDocument(...)` is invoked:

- local prepared-file failure;
- missing tree grant/context;
- exact destination cannot be resolved;
- exact destination is not a folder;

are deterministic pre-create failures. Queue result becomes `FAILED` and remains retryable.

At or after the call to create the remote document:

- provider exception;
- null/invalid create result;
- write/open/close failure;
- byte-count mismatch;
- created-document verification failure;

are `UNCERTAIN`, not `FAILED`, because Drive/provider state may already contain a file or partial file. Automatic retry is blocked.

The app does not attempt to delete a possibly-created remote document as error cleanup.

## Confirmed success

A result may become `UPLOADED` only when:

1. queue state was durably changed to `UPLOADING`;
2. exact destination preflight passed;
3. provider returned a nonblank created document identity;
4. all prepared JPEG bytes were written and the stream closed without error;
5. the exact returned document can be re-read as an `image/jpeg` document with matching identity and nonzero size when the provider reports size; and
6. the local queue successfully commits `UPLOADED` with that returned remote document ID.

If remote work succeeds but local confirmation bookkeeping cannot be safely committed, the record must not be presented as uploaded. It remains in/returns to an uncertain protected state, with blind retry blocked.

## Local-photo protection

Phase 6B never deletes:

- the protected original;
- the prepared JPEG;
- uncertain remote content.

Post-confirmation local cleanup belongs to the later retry/cleanup phase.

## Background/UI behavior

Upload runs off the main UI thread.

The selected photo shows queue state and attempt count. The upload control is enabled only when:

- state is `WAITING` or `FAILED`;
- a non-empty prepared copy exists;
- no upload is already running in the process workflow.

`UNCERTAIN` explicitly states that remote reconciliation is required. `UPLOADED` shows confirmed remote identity context. No automatic upload is added.

## Safe automated fixture

Automated provider-flow tests use a fake provider-operations boundary, not live Google Drive. They must prove:

- exact stored work-order provider ID is the create parent;
- deterministic filename is stable and unique across local photo IDs;
- full prepared byte count is passed to the provider writer;
- successful create/write/verify returns the provider document ID;
- pre-create destination failure is classified retryable / no remote side effect;
- create exception is uncertain;
- write failure after create is uncertain;
- post-create verification failure is uncertain;
- no remote delete cleanup occurs;
- confirmed result is persisted as `UPLOADED` with remote identity;
- uncertain result is non-retryable;
- retry after deterministic `FAILED` preserves destination identity and increments attempt count;
- one photo's failure does not alter another photo;
- protected original and prepared copy survive all Phase 6B queue results.

Existing camera, preparation, queue, and emulator image tests must remain passing.

## Real Android Google Drive gate — deferred

A physical Android phone is unavailable now, so Phase 6B may be implemented and heavily tested but may not be called ready for Level 3 merge approval yet.

When a phone is available, use the dedicated safe test hierarchy and prove:

1. selected test master SAF grant is still valid;
2. capture/prepare one disposable photo under the intended test work-order folder;
3. upload from the app;
4. inspect Drive and confirm exactly one JPEG exists under the exact intended work-order folder, not master/address/sibling folders;
5. confirm app persists the actual returned provider document ID and shows `UPLOADED`;
6. confirm protected local image remains present because cleanup is not part of Phase 6B;
7. exercise a safe deterministic pre-create failure and confirm `FAILED` retains local data/destination;
8. if an ambiguous provider interruption can be produced safely, confirm `UNCERTAIN` blocks retry; otherwise document that real-provider ambiguity could not be induced safely;
9. confirm unrelated Drive content is unchanged.

## Rollback

Rollback runtime is the exact tested Phase 7A runtime `95ae6a0c46e48c9f77ca85412ad86f650a4d9459`.

Rollback must not delete any queue metadata, protected original, prepared copy, or remote photo created during testing. If a Phase 6B test result is uncertain, leave the remote/local evidence in place until manually reconciled.

## Approval status

Implementation authorized by the operator's instruction to proceed with **Phase 6B — Drive Upload**.

Pre-merge approval: **not granted**. No merge is authorized until the deferred physical Android/Google Drive gate is completed and explicit Level 3 approval is given.
