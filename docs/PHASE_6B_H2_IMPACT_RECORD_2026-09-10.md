# Phase 6B-H2 Impact Record — Create/Persist/Write Barrier

Date: 2026-09-10

## Goal

Implement only Phase 6B-H2 from `docs/PHASE_6B_UPLOAD_SAFETY_HARDENING_2026-09-09.md`: split the existing Android SAF upload flow at the remote-create boundary so the exact provider-created document identity is durably persisted to the pending-photo queue before any prepared JPEG bytes are written.

Required ordering:

`UPLOADING → validate exact destination → create remote JPEG → persist provisionalRemoteFileId → write bytes → verify provider result → confirm remoteFileId + UPLOADED`

## Change level

Level 3. This changes upload sequencing and duplicate-protection semantics after a remote side effect may have occurred.

## Exact base

Repository: `timbone72-CC/field-photo-prep`

Base branch: `feat/phase-6b-h1-provisional-remote-identity`

Base head: `0a38ffddcebea70e3430455af49b7308028a9c32`

H1 tested runtime head: `2bc2e598f1298f562f561f6e16ffc0f2c6057f7d`

## Approved scope

- refactor `DrivePhotoUploader` into a pre-write remote creation stage and a post-persistence write/verify stage;
- keep destination validation and deterministic UUID filename behavior unchanged;
- return the exact provider-created document identity to the coordinator before byte streaming;
- have `PhotoUploadCoordinator` durably persist that identity through `PendingPhotoStore.recordProvisionalRemoteFileId(...)` before invoking any byte writer;
- if provisional persistence fails after create, do not write bytes and keep the result fail-closed/non-retryable where local state can be committed;
- preserve provisional identity through write/verification uncertainty;
- confirm only the same identity as the persisted provisional identity;
- add focused automated tests proving call ordering and failure behavior.

## Explicit non-scope

- no Phase 7B remote reconciliation;
- no automatic/background retry;
- no local photo cleanup;
- no remote delete/overwrite repair;
- no folder discovery/create/reuse/rename/delete changes;
- no camera or photo-preparation changes;
- no UI redesign;
- no schema change beyond H1 schema v3;
- no filename change;
- no Google Drive REST/OAuth migration.

## Protected behavior

- exact queued `workOrderId` remains the only upload parent identity;
- current UI selection cannot redirect an existing queued photo;
- `field-photo-<UUID>.jpg` remains deterministic remote naming;
- deterministic failures before remote create remain retryable `FAILED`;
- create ambiguity remains `UNCERTAIN`;
- after a successful create, no code path may begin a second automatic create merely because local confirmation fails;
- any write/verify ambiguity after provisional identity is persisted remains `UNCERTAIN` with that identity preserved;
- `remoteFileId` remains confirmed-success identity only;
- protected original and prepared JPEG are never deleted by H2.

## Owning files

Expected runtime owners:

- `DrivePhotoUploader.java` — exact destination validation, create stage, write/verify stage, provider result classification;
- `PhotoUploadCoordinator.java` — durable ordering barrier between returned create identity and byte streaming.

Expected tests:

- `DrivePhotoUploaderTest.java`;
- `PhotoUploadCoordinatorTest.java`;
- a narrow H2 ordering/failure test if needed.

No other runtime owner should change unless a directly required compile/test adjustment is discovered and documented.

## Read/write surfaces

Reads:

- persisted pending-photo record;
- protected/prepared local JPEG metadata and bytes;
- exact stored work-order provider document identity;
- provider metadata for destination and created photo.

Writes:

- one provider remote JPEG create call;
- local queue `provisionalRemoteFileId` immediately after a successful create;
- prepared JPEG bytes only after that local persistence succeeds;
- final local `UPLOADED` + confirmed `remoteFileId` only after provider verification succeeds.

## Failure recovery

- failure before create: persist `FAILED` when safely classifiable;
- create ambiguity: persist `UNCERTAIN`; no write or retry assumption;
- create succeeds but provisional persistence fails: byte writer must not run; attempt remains protected and must not be made blindly retryable;
- write or post-write verification fails after provisional persistence: persist `UNCERTAIN` while retaining provisional identity;
- local confirmation fails after verified remote success: retain/protect provisional evidence and block retry pending reconciliation.

There is still an unavoidable crash window between provider create returning and local provisional persistence. Deterministic filename + exact parent remain the Phase 7B fallback for that case.

## Focused verification requirements

Prove at minimum:

1. exact stored destination is validated before create;
2. create returns the exact remote identity without writing bytes;
3. coordinator persists that identity before writer invocation;
4. writer observes the same persisted provisional identity;
5. provisional persistence failure prevents writer invocation;
6. write interruption preserves provisional identity in `UNCERTAIN`;
7. verification failure preserves provisional identity in `UNCERTAIN`;
8. pre-create safe failure still becomes `FAILED` with no provisional identity;
9. create ambiguity becomes `UNCERTAIN` with no invented provisional identity;
10. successful write/verify promotes the exact provisional identity into confirmed `remoteFileId`;
11. original and prepared local files survive all H2 paths;
12. one photo's result does not mutate another photo.

After focused tests pass, the complete Android CI suite must pass once on the exact final H2 runtime/test head.

## Reality gate

Because H2 changes the actual SAF create/write sequencing, automated tests are not final field proof. Before Level-3 merge approval, the affected path must later pass the physical Android + Google Drive `DocumentsProvider` reality gate described by `INTEGRATION_CONTRACT.md` and the Phase 6B hardening plan.

## Approval status

The operator explicitly authorized proceeding with **6B-H2 — Create/Persist/Write Barrier**. This authorizes implementation of the narrow scope above. It does not grant pre-merge approval. The H2 PR must remain draft/unmerged until verification is complete and explicit merge approval is given.