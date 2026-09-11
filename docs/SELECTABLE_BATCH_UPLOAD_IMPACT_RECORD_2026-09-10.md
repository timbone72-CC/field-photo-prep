# Selectable Batch Upload — Level 3 Impact Record

Date: 2026-09-10

Status: IN PROGRESS — EXPLICIT PRE-MERGE APPROVAL REQUIRED

Branch: `feat/selectable-batch-upload`

Parent camera-lighting branch: `feat/camera-flash-torch-controls`

Main rollback baseline before the combined staged work: `2a66e80378e5c3853a111121104e532df1bf6a03`

## User-facing problem

Multi-shot capture and automatic preparation now make it practical to create many field photos quickly, but Drive upload still requires selecting and sending one photo at a time. Large jobs can contain more than ten photos, and the operator wants to control the network load manually rather than being forced to upload every ready photo in one batch.

## Approved behavior

Add explicit per-photo batch-selection checkboxes to the current work-order photo list.

Controls:

- **Select All Ready** selects every currently upload-eligible prepared photo for the open work order.
- **Clear Selection** clears the batch selection without changing any photo or Drive state.
- a visible selected-count shows the intended batch size;
- **Upload Selected (N)** starts one operator-approved batch containing only those selected photo IDs.

The operator can therefore split a large job manually, for example 5 photos now and 7 photos later.

From the operator's perspective one tap sends the selected batch. Internally the app performs exactly one Drive upload attempt at a time, in deterministic selected-photo order.

## Change classification

Level 3 — this changes upload orchestration and can initiate multiple remote file creations from one operator action.

It does **not** change the per-photo upload protocol, destination construction, remote filename/idempotency rules, SAF tree grant, queue schema, UNCERTAIN reconciliation algorithm, or confirmed-success cleanup policy.

Explicit operator approval is required again before merge after automated and real-device safe-Drive verification.

## Required and optional data

Required for every selected photo:

- existing immutable local photo ID;
- existing stored address/work-order provider identities;
- a valid prepared derivative;
- queue state eligible for a new attempt (`WAITING` or retry-safe `FAILED`).

Selection state is UI/session convenience only. It is not durable upload authority and is not written into queue metadata or a new schema.

## Owning files

- `PhotoCaptureActivity.java` — renders selection controls, snapshots the selected IDs, owns the one batch worker, and reports progress/result.
- `PhotoBatchUploadRunner.java` — deterministic sequential batch execution and stop policy independent of Android UI.
- `PhotoBatchUploadRunnerTest.java` — focused sequencing, duplicate-input, safe-failure continuation, and UNCERTAIN-stop tests.
- existing `PhotoUploadCoordinator` remains the owner of each individual upload attempt and is intentionally unchanged.

## Read surfaces

The batch layer reads:

- current work-order-scoped pending photo records;
- each selected photo's queue state and immutable destination identity;
- existence of that photo's prepared derivative;
- per-photo result state after an upload error to decide whether continuing is safe.

## Write surfaces

For each selected photo, one at a time, the batch invokes the existing `PhotoUploadCoordinator.upload(photoId)` path. That existing path remains responsible for:

- `WAITING/FAILED → UPLOADING`;
- remote create;
- durable provisional remote identity barrier;
- byte write and verification;
- `FAILED`, `UNCERTAIN`, or confirmed `UPLOADED` bookkeeping;
- exact stored destination identity;
- duplicate/retry protection.

After confirmed success, the existing confirmed-local-cleanup path may run for that photo.

Batch selection itself writes no Drive content and no queue metadata.

## Duplicate/idempotency behavior

- the batch snapshot may contain each photo ID at most once;
- already `UPLOADED`, `UPLOADING`, `UNCERTAIN`, `CAPTURING`, missing-image, or unprepared photos cannot be selected for a normal batch attempt;
- the existing coordinator still owns remote duplicate prevention and provisional/confirmed identity bookkeeping;
- the batch never retries the same photo twice within one run;
- the batch never changes a photo's stored work-order provider ID.

## Failure and uncertainty behavior

- confirmed upload: continue to the next selected photo;
- confirmed upload with local cleanup failure: remote success remains confirmed; continue and report cleanup pending;
- retry-safe `FAILED`/pre-attempt `WAITING` after a known error: preserve local data, count the failure, and continue to the next selected photo;
- `UNCERTAIN` or still-`UPLOADING` after an error: stop the batch immediately; no later selected photo is attempted;
- inability to reread the failed photo's queue state: stop immediately because remote safety cannot be proven;
- process death during the active photo remains governed by existing restart recovery (`UPLOADING → UNCERTAIN`); photos later in the selected batch were never started and remain unchanged.

## Offline/stale-state behavior

A batch does not assume connectivity. Each photo uses the same existing provider path as a single upload. Known-safe failures remain recoverable. Ambiguous provider outcomes stop the batch. Selection state may be lost on process death without losing photos because selection is not queue state.

## Safe Drive fixture plan

Use only the disposable Field Photo Prep test hierarchy. Do not use a live job.

Physical gate:

1. create/capture at least four disposable photos under one test work order and allow automatic preparation to complete;
2. select only a subset (for example 2 of 4) and tap **Upload Selected (2)** once;
3. prove exactly those two photos appear in the exact stored Drive work-order folder and the other two remain local/unattempted;
4. select the remaining two and upload them with one tap;
5. verify no duplicate remote files and no wrong-parent file;
6. if a safe deterministic retry-safe failure can be induced without ambiguous remote state, verify that one failed photo does not corrupt later selected photos;
7. do not deliberately manufacture an ambiguous remote create merely to test UNCERTAIN; automated coverage plus any naturally observed uncertainty remains fail-closed.

## Focused automated verification

Must prove:

- selected IDs are attempted exactly once in deterministic order;
- no parallel batch attempt is started by the runner;
- duplicate IDs are rejected before any attempt;
- a safe failure can be recorded while the runner proceeds to the next selected ID;
- an UNCERTAIN/unverified result stops the batch and leaves all later IDs unattempted;
- existing per-photo coordinator tests remain green;
- complete Android CI passes once on the final runtime head.

## Protected behavior

- protected originals are never removed before confirmed remote success;
- every photo retains its immutable stored destination identity;
- no batch action guesses a destination from the currently visible folder name;
- `UNCERTAIN` never becomes an implicit retry;
- one photo's result changes only that photo's queue state;
- confirmed cleanup never deletes the Drive copy;
- manual batch size controls network workload without weakening per-photo safety.

## Rollback

Remove the batch-selection/orchestration UI and runner, returning to the parent camera-lighting branch. Existing queue records, protected originals, prepared copies, confirmed remote identities, and Drive content remain valid because no new queue schema is introduced.

## Approval state

Implementation is authorized by the operator's **Do it** instruction. Because this is Level 3, merge remains blocked until the final evidence is reviewed and the operator gives explicit pre-merge approval.
