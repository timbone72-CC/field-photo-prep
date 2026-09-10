# Phase 6B-H2 Implementation Record — Create/Persist/Write Barrier

Date: 2026-09-10

## Goal

Implement only the second Phase 6B upload-safety hardening slice: ensure a provider-created remote photo identity is durably persisted in the local pending-photo queue before any prepared JPEG bytes are written to that remote object.

The hardened ordering is now:

`WAITING/FAILED → UPLOADING → validate exact destination → create remote JPEG → persist provisionalRemoteFileId → write prepared bytes → verify provider result → UPLOADED + confirmed remoteFileId`

This record documents the implemented H2 runtime and its automated verification. It does not claim the later physical Android + Google Drive `DocumentsProvider` reality gate has been completed.

## Governing design

H2 follows:

- `docs/PHASE_6B_UPLOAD_SAFETY_HARDENING_2026-09-09.md`
- `docs/PHASE_6B_H1_IMPLEMENTATION_RECORD_2026-09-10.md`
- `docs/PHASE_6B_H2_IMPACT_RECORD_2026-09-10.md`
- the reconciled `CONTRACT.md`, `INTEGRATION_CONTRACT.md`, `CHANGE_CONTROL_CONTRACT.md`, and `TESTING_CONTRACT.md`.

## Exact baseline

Repository: `timbone72-CC/field-photo-prep`

Base branch: `feat/phase-6b-h1-provisional-remote-identity`

Exact H2 base head: `0a38ffddcebea70e3430455af49b7308028a9c32`

H2 branch: `feat/phase-6b-h2-create-persist-write-barrier`

Draft pull request: `#17 — Phase 6B-H2: create/persist/write barrier`

Exact final H2 runtime/test head:

`8d23b061307725589aef69a31a00249744523406`

The documentation commit adding this implementation record occurs after that tested runtime/test head and is not itself runtime evidence.

## Change level

Level 3.

Reason: H2 changes upload sequencing at the boundary where a remote side effect may already exist. Incorrect ordering could lose the only exact remote identity, permit duplicate creation, or write bytes before local duplicate-protection evidence is durable.

The operator explicitly authorized implementation of **6B-H2 — Create/Persist/Write Barrier**. Pre-merge approval has not been granted. PR #17 remains draft/unmerged.

## Runtime changes

### 1. `DrivePhotoUploader` is now staged

The previous monolithic `upload(...)` operation performed destination validation, remote create, byte write, and verification inside one method. H2 splits that responsibility into two explicit stages.

#### `create(...)`

This stage:

- requires a persisted `UPLOADING` record;
- rejects a record that already contains provisional remote identity so another remote photo is not blindly created;
- validates the prepared JPEG before any remote create;
- validates the exact stored `workOrderId` as the destination provider folder identity;
- preserves deterministic `field-photo-<UUID>.jpg` naming;
- performs the remote JPEG create;
- returns a `CreatedUpload` containing the exact local photo UUID, exact work-order destination identity, exact provider-created remote file identity, deterministic remote display name, and expected byte count;
- performs no prepared-JPEG byte write.

Pre-create deterministic destination/prepared-file failures remain safely classifiable. Create ambiguity remains `UNCERTAIN` because the provider may have performed a remote side effect even when the app did not receive a reliable result.

#### `writeAndVerify(...)`

This stage refuses to invoke the provider writer unless all barrier evidence matches:

- queue state is still `UPLOADING`;
- the staged `CreatedUpload` belongs to the same local photo UUID;
- the staged destination is the same immutable queued `workOrderId`;
- the queued `provisionalRemoteFileId` equals the created provider identity;
- deterministic remote filename still matches that photo UUID;
- the prepared JPEG still exists and its byte count has not changed since remote creation.

Only after those checks pass does the provider writer run.

Write interruption, short write, provider verification failure, metadata mismatch, missing/changed prepared file after create, or staged-token mismatch are treated as uncertain post-create outcomes.

### 2. `PhotoUploadCoordinator` owns the durable barrier sequence

The coordinator now executes:

1. validate the selected queue item can begin upload;
2. require the prepared JPEG before queue state changes;
3. durably transition to `UPLOADING`;
4. call `DrivePhotoUploader.create(...)`;
5. receive the exact provider-created identity;
6. call `PendingPhotoStore.recordProvisionalRemoteFileId(...)` and require that durable local write to succeed;
7. only then call `DrivePhotoUploader.writeAndVerify(...)` with the returned persisted record;
8. on verified success, call `markUploadConfirmed(...)`, which H1 requires to match the provisional identity and promotes it into confirmed `remoteFileId`.

This makes the H1 schema field part of the real upload sequence rather than passive metadata.

## Provisional-persistence failure behavior

A failure to durably record `provisionalRemoteFileId` after remote create is now a hard write barrier.

The coordinator does not intentionally invoke the byte writer after that failure.

It attempts to move a still-readable `UPLOADING` record to `UNCERTAIN`. If queue persistence itself is unavailable, it leaves the record fail-closed in `UPLOADING`; the existing process-start recovery converts a readable interrupted `UPLOADING` record to `UNCERTAIN` on restart.

The app therefore never converts this condition into retryable `FAILED` merely because the local provisional write failed.

There remains an unavoidable very small crash window after the provider returns the created identity but before local persistence completes. H2 does not pretend to eliminate that platform/process window. The deterministic UUID filename plus immutable parent identity remain Phase 7B reconciliation fallbacks.

## Identity isolation hardening

`CreatedUpload` is bound to:

- local photo UUID;
- immutable work-order destination provider identity;
- created remote provider identity;
- deterministic remote filename;
- expected prepared byte count.

A staged token from another photo or destination cannot be used to open the writer even if a different queue record somehow contains the same provisional remote string.

## Failure-state behavior preserved

### Before remote create

Deterministic failures such as unreadable/wrong destination remain `FAILED`/retryable with no provisional identity.

### Create ambiguity

Create failure/ambiguity remains `UNCERTAIN` with no invented provisional identity when the exact provider-created identity was not safely returned.

### After successful create and provisional persistence

Write/verify ambiguity becomes `UNCERTAIN` while preserving the exact `provisionalRemoteFileId`.

### Confirmed success

The exact provisional identity is promoted to confirmed `remoteFileId`; provisional state is cleared by the H1 queue invariant.

### Local files

H2 deletes neither the protected original nor the prepared JPEG. Cleanup remains Phase 7B/post-confirmation work.

## Exact changed runtime/test files

Relative to H2 base `0a38ffddcebea70e3430455af49b7308028a9c32`, H2 runtime/test behavior is limited to:

- `app/src/main/java/com/inandout/fieldphotoprep/DrivePhotoUploader.java`
- `app/src/main/java/com/inandout/fieldphotoprep/PhotoUploadCoordinator.java`
- `app/src/test/java/com/inandout/fieldphotoprep/DrivePhotoUploaderTest.java`
- `app/src/test/java/com/inandout/fieldphotoprep/DrivePhotoUploaderStageBindingTest.java`
- `app/src/test/java/com/inandout/fieldphotoprep/PhotoUploadCoordinatorTest.java`

Documentation added before the final runtime head:

- `docs/PHASE_6B_H2_IMPACT_RECORD_2026-09-10.md`
- `docs/PHASE_6B_H2_TEST_MATRIX_2026-09-10.md`

No queue schema change beyond H1 version 3 was made. No `PendingPhotoRecord` or `PendingPhotoStore` runtime change was required in H2.

No camera, photo-preparation, UI, WorkManager/retry scheduler, folder-management, permission, OAuth, reconciliation, cleanup, manifest, Gradle, or workflow behavior was changed.

## Focused automated coverage

The H2 tests prove:

- deterministic remote naming remains UUID-based;
- exact stored work-order identity is used for create;
- create performs no byte write;
- writer refuses to run without matching persisted provisional identity;
- writer refuses mismatched provisional identity;
- staged token from another photo/destination is rejected before provider I/O;
- prepared-file mutation after create blocks writer invocation;
- pre-create destination failure remains safely retryable and creates no remote photo;
- create ambiguity is uncertain and writes no bytes;
- write interruption preserves provisional identity in `UNCERTAIN`;
- post-write verification failure preserves provisional identity in `UNCERTAIN`;
- short writes remain uncertain;
- successful coordinator flow persists provisional identity before writer invocation;
- the fake provider reads the actual persisted queue record from inside `writeDocument(...)` and observes the matching provisional identity;
- forced provisional-persistence failure after remote create prevents writer invocation;
- after restoring that queue fixture, the surviving `UPLOADING` record remains non-retryable and process-start recovery converts it to `UNCERTAIN`;
- success promotes the exact provisional identity to confirmed `remoteFileId`;
- protected original/prepared files survive H2 success and failure paths;
- one photo's failure does not mutate another queued photo.

## Final automated verification

Exact tested runtime/test head:

`8d23b061307725589aef69a31a00249744523406`

Android CI run:

`34467462173`

Job:

`102839281807`

Result: **PASS**.

All workflow steps completed successfully on that exact head:

- complete JVM/unit test suite;
- debug APK build;
- KVM setup;
- Android connected instrumentation tests;
- app launch smoke test;
- debug APK artifact packaging.

Artifact:

- ID: `10148261046`
- name: `field-photo-prep-phase-1-debug-apk`
- size: `2440839` bytes
- digest: `sha256:0011223e86c2dff7da92f98e030de2160c2c25d76e0dee605006e856c3dd2569`

The artifact name is inherited from the existing workflow and is not evidence that this runtime belongs to Phase 1.

## Physical Android / Google Drive reality gate

Not completed by H2 automated verification.

Because H2 changes the actual SAF create/write sequencing, the complete hardened upload path still requires the affected physical Android + Google Drive `DocumentsProvider` reality gate before Level-3 merge approval.

That gate must prove on a safe test hierarchy that:

- the selected work-order provider identity is the actual parent;
- create returns a usable provider document identity;
- that identity is retained locally before bytes are written;
- the prepared JPEG is written to that exact created document;
- provider verification is sufficient for the app's documented `UPLOADED` boundary;
- unrelated Drive content is unchanged;
- interruption/ambiguous behavior is documented without unsafe duplicate creation.

Automated mocked/emulator evidence is not represented as proof of Google Drive cloud freshness or backend synchronization.

## Explicit non-scope retained

H2 does not implement:

- Phase 7B remote reconciliation;
- SHA-256 remote content reconciliation;
- automatic/background retry;
- local image cleanup;
- uncertain remote delete/overwrite;
- camera changes;
- photo compression/orientation changes;
- folder create/reuse/rename/delete changes;
- general Drive browsing;
- OAuth/Drive REST migration;
- workbook or Free Map Router integration.

## Rollback

H2 rollback point is the exact H1 branch head:

`0a38ffddcebea70e3430455af49b7308028a9c32`

Reverting H2 does not require deleting pending photos or remote data. Any real provider object created during later device testing must be treated according to the uncertainty/reconciliation rules rather than blindly removed or recreated.

## Approval status

Implementation: authorized and completed on the isolated H2 branch.

Automated verification: passed on exact runtime/test head `8d23b061307725589aef69a31a00249744523406`.

Physical Android/Google Drive reality gate: pending.

Merge approval: not granted.

PR #17 must remain draft/unmerged.