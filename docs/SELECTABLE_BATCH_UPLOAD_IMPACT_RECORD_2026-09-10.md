# Selectable Batch Upload — Level 3 Impact Record

Date: 2026-09-10

Status: STAGED — AUTOMATED PASS, PHYSICAL CAMERA + SAFE DRIVE GATES PENDING

Branch: `feat/selectable-batch-upload`

Parent camera-lighting branch: `feat/camera-flash-torch-controls`

Main rollback baseline before the combined staged work: `2a66e80378e5c3853a111121104e532df1bf6a03`

Exact automated-tested runtime head: `57d260b1956c0bf4e9f347eb3c1be625fabde68e`

Android CI run: `34554491273` — PASS

APK artifact: `10182109209` (`field-photo-prep-internal-apk`)

Artifact digest: `sha256:d2505ceb4aff7a218a4be471e2badac05ca5ed8233d49885a6913b783242c1c9`

Staged APK SHA-256: `99fb351133ea16f43b254e7b309a2ec246bca2db53ffa52027b337dad4e50dcb`

Later commits after the runtime head are documentation/contract lineage only and do not change the executable tree.

## User-facing problem

Multi-shot capture and automatic preparation now make it practical to create many field photos quickly, but Drive upload still requires selecting and sending one photo at a time. Large jobs can contain more than ten photos, and the operator wants to control network/provider load manually rather than being forced to upload every ready photo in one batch.

## Approved behavior

Add explicit per-photo batch-selection checkboxes to the current work-order photo list.

Controls:

- **Select All Ready** selects every currently upload-eligible prepared photo for the open work order.
- **Clear Selection** clears the batch selection without changing any photo or Drive state.
- a visible selected-count shows the intended batch size;
- **Upload Selected (N)** starts one operator-approved batch containing only those selected photo IDs.

The operator can therefore split a large job manually, for example 5 photos now and 7 photos later.

From the operator's perspective one tap sends the selected batch. Internally the app performs exactly one Drive upload attempt at a time, in deterministic selected-photo order.

The existing individual-photo upload and UNCERTAIN reconciliation controls remain available.

## Change classification

Level 3 — this changes upload orchestration and can initiate multiple remote file creations from one operator action.

It does **not** change the per-photo upload protocol, destination construction, remote filename/idempotency rules, SAF tree grant, queue schema, UNCERTAIN reconciliation algorithm, or confirmed-success cleanup policy.

Implementation is authorized by the operator's **Do it** instruction. Because this is Level 3, explicit operator approval is still required again before merge after automated and real-device safe-Drive evidence is complete.

## Required and optional data

Required for every selected photo:

- existing immutable local photo ID;
- existing stored address provider identity and work-order provider identity;
- existing prepared derivative with usable image data;
- queue state eligible for a new attempt (`WAITING` or retry-safe `FAILED`);
- current persisted master-tree access required by the unchanged per-photo uploader.

Optional/session-only data:

- whether the photo's **Send** checkbox is checked;
- current selected-count;
- current batch progress display.

Selection state is UI/session convenience only. It is not durable upload authority and is not written into queue metadata or a new schema.

## Schema, identity, permission, and platform-access impact

- Queue schema: unchanged.
- Local photo identity: unchanged.
- Address/work-order provider identity: unchanged and immutable per photo.
- SAF persisted master-tree permission: unchanged.
- Provider/account selection: unchanged.
- Per-photo remote create/write/verify protocol: unchanged.
- UNCERTAIN reconciliation semantics: unchanged.
- Confirmed local cleanup policy: unchanged.

The batch layer may decide **which existing photo ID is attempted next**, but it may not construct or substitute a Drive destination.

## Owning files

- `PhotoCaptureActivity.java` — renders selection controls, snapshots the selected IDs, owns the one batch worker, and reports progress/result.
- `PhotoBatchUploadRunner.java` — deterministic sequential batch execution and stop policy independent of Android UI.
- `PhotoBatchUploadRunnerTest.java` — focused sequencing, duplicate-input, safe-failure continuation, and UNCERTAIN/unverified-stop tests.
- existing `PhotoUploadCoordinator` remains the owner of each individual upload attempt and is intentionally unchanged.

## Read surfaces

The batch layer reads:

- current work-order-scoped pending photo records;
- each selected photo's queue state and immutable destination identity;
- existence of that photo's prepared derivative and protected original state;
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

## Master-folder and destination assumptions

- The approved persisted SAF master tree remains the only remote boundary.
- Every selected photo already carries the exact work-order provider document ID captured for that photo's work occurrence.
- The currently visible/open work order may be used for UI filtering only; it must never replace a selected photo's stored destination identity.
- No same-named-folder inference is allowed once a stored provider identity exists.

## Duplicate and idempotency behavior

- the batch snapshot may contain each local photo ID at most once;
- already `UPLOADED`, `UPLOADING`, `UNCERTAIN`, `CAPTURING`, missing-image, or unprepared photos cannot be selected for a normal batch attempt;
- the existing coordinator still owns remote duplicate prevention and provisional/confirmed identity bookkeeping;
- the batch never retries the same photo twice within one run;
- the batch never changes a photo's stored work-order provider ID;
- one operator tap never authorizes simultaneous remote creates.

## Failure, uncertainty, and recovery behavior

- confirmed upload: continue to the next selected photo;
- confirmed upload with local cleanup failure: remote success remains confirmed; continue and report cleanup pending;
- retry-safe `FAILED`/pre-attempt `WAITING` after a known error: preserve local data, count the failure, and continue to the next selected photo;
- `UNCERTAIN` or still-`UPLOADING` after an error: stop the batch immediately; no later selected photo is attempted;
- inability to reread the failed photo's queue state: stop immediately because remote safety cannot be proven;
- missing/unexpected queue state after an attempted upload: stop immediately;
- process death during the active photo remains governed by existing restart recovery (`UPLOADING → UNCERTAIN` where appropriate); photos later in the selected batch were never started and remain unchanged;
- after restart, the old UI selection is not treated as permission to resume remote writes automatically.

## Offline and stale-state behavior

A batch does not assume connectivity. Each photo uses the same existing provider path as a single upload. Known-safe failures remain recoverable. Ambiguous provider outcomes stop the batch.

Batch selection may be lost on process death without losing photos because selection is not queue state.

Provider freshness rules that govern folder create/reuse remain unchanged. Batch upload does not authorize absence-based folder creation and does not bypass provider identity checks.

## Safe Drive fixture plan

Use only the disposable Field Photo Prep test hierarchy. Do not use a live job.

Physical batch gate:

1. create/capture at least four disposable photos under one test work order and allow automatic preparation to complete;
2. select only a proper subset, for example 2 of 4, and tap **Upload Selected (2)** once;
3. prove exactly those two photos appear in the exact stored Drive work-order folder and the other two remain local/unattempted;
4. select the remaining two and upload them with one tap;
5. verify no duplicate remote files and no wrong-parent file;
6. verify unrelated test Drive content is unchanged;
7. if a safe deterministic retry-safe failure can be induced without ambiguous remote state, verify that one failed photo does not corrupt later selected photos;
8. do not deliberately manufacture an ambiguous remote create merely to test UNCERTAIN; automated coverage plus any naturally observed uncertainty remains fail-closed.

If a real provider result becomes ambiguous naturally, stop immediately, preserve the evidence, do not retry that photo, and confirm later selected photos were not attempted.

## Focused automated verification

PASS on exact runtime `57d260b1956c0bf4e9f347eb3c1be625fabde68e`.

Android CI `34554491273` passed:

- complete JVM/unit tests;
- internal debug build;
- stable test-signer verification;
- Android emulator instrumentation;
- internal launch smoke; and
- APK artifact packaging.

Focused batch coverage proves:

- selected IDs are attempted exactly once in deterministic order;
- duplicate IDs are rejected before any attempt;
- no parallel batch attempt is started by the runner;
- a safe failure can be recorded while the runner proceeds to the next selected ID;
- confirmed success with local cleanup pending may continue safely;
- an UNCERTAIN/unverified result stops the batch and leaves all later IDs unattempted.

Existing per-photo coordinator behavior remains the safety boundary for each remote create/write/verify operation.

## Camera-lighting dependency gate

The combined APK also includes the parent Flash/Torch feature. Before using the same build for the Drive batch gate on the operator's Samsung phone, confirm the parent camera-lighting behavior:

1. fresh camera session shows **Flash: Auto** and **Torch: Off**;
2. Torch can be turned On and Off;
3. Flash On fires for a capture and Flash Off does not;
4. returning Flash to Auto preserves repeated capture;
5. leaving with **Done** does not intentionally leave the torch enabled.

The parent camera feature is Level 2; this dependency check must not be conflated with the Level 3 Drive approval.

## Protected behavior

- protected originals are never removed before confirmed remote success;
- every photo retains its immutable stored destination identity;
- no batch action guesses a destination from the currently visible folder name;
- `UNCERTAIN` never becomes an implicit retry;
- one photo's result changes only that photo's queue state;
- confirmed cleanup never deletes the Drive copy;
- manual batch size controls network workload without weakening per-photo safety;
- later selected photos remain untouched after an uncertain/unverified stop.

## Rollback

Remove the batch-selection/orchestration UI and runner, returning to the parent camera-lighting branch. Existing queue records, protected originals, prepared copies, confirmed remote identities, and Drive content remain valid because no new queue schema is introduced.

## Approval state

Implementation is authorized. Automated verification is PASS. Physical camera-lighting and real-Drive subset gates remain pending.

PR #27 must remain draft/unmerged until the applicable device evidence is recorded and the operator gives explicit Level 3 pre-merge approval.
