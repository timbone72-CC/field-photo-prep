# Selectable Batch Upload — Level 3 Impact Record

Date: 2026-09-10

Status: STAGED — AUTOMATED PASS, PHYSICAL CAMERA + SAFE DRIVE GATES PENDING

Branch: `feat/selectable-batch-upload`
Parent: `feat/camera-flash-torch-controls`
Rollback baseline: `2a66e80378e5c3853a111121104e532df1bf6a03`

Exact tested runtime: `57d260b1956c0bf4e9f347eb3c1be625fabde68e`
Android CI: `34554491273` — PASS
Artifact: `10182109209` (`field-photo-prep-internal-apk`)
Artifact digest: `sha256:d2505ceb4aff7a218a4be471e2badac05ca5ed8233d49885a6913b783242c1c9`
Staged APK SHA-256: `99fb351133ea16f43b254e7b309a2ec246bca2db53ffa52027b337dad4e50dcb`

## Problem and approved behavior

Multi-shot capture and automatic preparation can create many ready photos quickly, but the app previously required one upload tap per photo. The operator wants manual control over batch size for large jobs.

The work-order photo list therefore adds per-photo **Send** checkboxes, **Select All Ready**, **Clear Selection**, a selected count, and **Upload Selected (N)**. One tap starts only the selected snapshot. Internally the photos are processed sequentially, one remote upload attempt at a time.

## Risk classification

Level 3 because one operator action can start more than one remote upload. The existing per-photo upload protocol remains unchanged. No SAF grant, queue schema, destination construction, reconciliation algorithm, or cleanup policy is changed.

Implementation is authorized. Merge still requires explicit operator approval after the device evidence is reviewed.

## Ownership and safety

`PhotoCaptureActivity` owns selection and the batch worker. `PhotoBatchUploadRunner` owns deterministic sequential ordering and stop policy. Existing `PhotoUploadCoordinator` continues to own each individual photo attempt.

Only prepared photos in `WAITING` or retry-safe `FAILED` state may be selected. Every selected photo keeps its already-stored work-order provider identity. Duplicate IDs are rejected before any attempt. Confirmed success may continue to the next photo. A retry-safe failure may remain local while later selected photos continue. An `UNCERTAIN`, still-`UPLOADING`, unreadable, or otherwise unverified result stops the batch immediately; later selected photos are not attempted.

Batch selection is UI/session state, not durable upload authority. If the process dies, the active photo follows the existing restart rules and later photos were never started. Protected originals remain until confirmed remote success.

## Automated verification

PASS on exact runtime `57d260b1956c0bf4e9f347eb3c1be625fabde68e`. CI `34554491273` passed JVM tests, debug build, signer verification, Android instrumentation, launch smoke, and APK packaging.

Focused tests prove deterministic one-at-a-time execution, duplicate-selection rejection before work begins, safe-failure continuation, immediate stop before later photos on uncertain/unverified results, and continued batch progress when remote success is confirmed but local cleanup remains pending.

The runtime is frozen. Later documentation-only commits do not invalidate this evidence.

## Physical gates

Use the combined staged APK only with disposable test content.

### Gate A — lighting

Confirm a fresh camera session shows **Flash: Auto** and **Torch: Off**. Toggle Torch On and Off. Verify Flash On fires for a capture and Flash Off does not. Return Flash to Auto, confirm repeated capture still works, then leave with **Done** and confirm the torch is not left on.

### Gate B — selectable batch

Use at least four disposable prepared photos under one test work order. Select only a subset, such as 2 of 4, and tap **Upload Selected (2)** once. Verify exactly those two reach the exact test work-order folder while the other two remain unattempted. Then select and upload the remaining two. Verify there are no duplicate remote files or wrong-parent files.

If any remote result becomes ambiguous, stop. Do not retry that uncertain photo until reconciliation establishes the remote result.

## Merge state

PR #27 remains draft and unmerged through both physical gates. After PASS evidence is recorded, obtain explicit operator pre-merge approval.
