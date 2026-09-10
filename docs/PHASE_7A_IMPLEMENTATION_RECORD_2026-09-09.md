# Phase 7A Implementation Record — Persistent Upload Queue State

Date: 2026-09-09

## Goal

Add durable, fail-closed local upload-queue bookkeeping before any real Google Drive upload code is introduced.

A captured photo must retain its immutable local identity and exact bound address/work-order remote identities while moving through local upload states, survive app/process restart, isolate one photo's failure from other photos, and never become automatically retryable after an interrupted/ambiguous in-flight result.

## Approved scope

Phase 7A only:

- extend the persisted pending-photo metadata model with queue-state bookkeeping;
- preserve existing `CAPTURING` and `WAITING` capture behavior;
- add `UPLOADING`, `FAILED`, `UNCERTAIN`, and `UPLOADED` local states;
- persist upload-attempt count and last-attempt timestamp;
- persist a failure/uncertainty detail when applicable;
- persist confirmed remote file identity only when a future caller explicitly supplies a confirmed remote identity;
- allow upload attempts to begin only from `WAITING` or `FAILED`;
- make `UPLOADED` terminal for automatic retry;
- convert a persisted `UPLOADING` record found after actual process restart into `UNCERTAIN`, not `WAITING` or `FAILED`;
- block automatic retry of `UNCERTAIN` until a later Drive-reconciliation phase resolves whether remote creation occurred;
- preserve exact address/work-order provider IDs through every queue transition;
- keep protected originals intact throughout all Phase 7A transitions, including simulated confirmed success;
- surface queue states through the existing temporary-photo list;
- keep explicit local discard unavailable for in-flight, uncertain, or confirmed-upload records;
- read valid legacy capture metadata written before Phase 7A and treat it as queue-schema version 1 with safe defaults; all subsequent record writes use schema version 2.

Explicitly excluded:

- any Google Drive file creation or upload;
- any DocumentsProvider write for photos;
- remote existence/reconciliation queries;
- automatic retry scheduling/background networking;
- deletion of protected originals after confirmed success;
- deletion of prepared copies after confirmed success;
- changing the Phase 6A compression/resize/orientation policy;
- folder create/reuse/delete/rename changes;
- camera behavior changes;
- general upload-history/gallery features.

## Dependency and governed base

Phase 7A is intentionally stacked on Phase 6A because the future upload queue consumes Phase 5 protected originals and Phase 6A prepared copies.

Branch: `feat/phase-7a-persistent-upload-queue`

Stack base: Phase 6A branch documentation head `170065f91ddaecdedc563d6ee949abd7c2b1ac94`.

Exact tested Phase 6A runtime base / rollback runtime: `484f6df2ac8aa11762f4696b275f198391f21bf1`.

Phase 7A must not merge to `main` ahead of its Phase 5/Phase 6A dependency chain.

## Change level

Level 3.

Reason: this changes persisted photo metadata schema and upload/retry semantics. A bad transition could cause duplicate uploads, erase duplicate-protection evidence, or make a photo appear safe when it is not.

Explicit operator approval is required before merge after verification and after the dependency chain is safe to merge.

## Persisted schema

Phase 7A introduces queue metadata schema version 2.

Existing immutable fields remain:

- local photo UUID;
- protected image filename derived from UUID;
- capture creation time;
- address provider identity and display name;
- work-order provider identity and display name.

New queue fields:

- schema version;
- state;
- upload attempt count;
- last upload-attempt timestamp;
- failure/uncertainty detail when applicable;
- confirmed remote file identity when and only when confirmed.

Legacy records without a schema-version field are accepted as schema version 1 only when their existing capture state/data is otherwise valid. They load with attempt count 0, no last-attempt time, no failure detail, and no remote identity. Their next atomic metadata write persists schema version 2.

Unknown future schema versions fail closed rather than being guessed.

## State contract

Allowed high-level flow:

`CAPTURING → WAITING → UPLOADING → UPLOADED`

Retryable failure flow:

`WAITING/FAILED → UPLOADING → FAILED`

Ambiguous/interrupted flow:

`UPLOADING → UNCERTAIN`

Restart recovery rule:

- a persisted `UPLOADING` state means the previous process ended before local confirmation bookkeeping completed;
- Phase 7A therefore changes it to `UNCERTAIN` on actual Android process startup;
- it is never silently changed back to `WAITING` or `FAILED`;
- Phase 7A provides no automatic path from `UNCERTAIN` back to retryable state because remote reconciliation does not exist yet.

The process-start boundary is deliberate. `PhotoCaptureActivity` recreation/navigation does **not** classify a currently active `UPLOADING` record as interrupted. `FieldPhotoPrepApplication` owns the once-per-process recovery call, while the photo screen continues to own interrupted-camera reconciliation. This avoids baking in a future race with a live upload worker.

Confirmed-success rule:

- `UPLOADED` may be written only after a future integration caller explicitly supplies a nonblank confirmed remote file identity;
- Phase 7A itself performs no remote operation and cannot manufacture confirmation;
- `UPLOADED` is terminal for automatic upload attempts.

## Identity and idempotency

Every queue transition must copy forward exactly the same:

- local photo UUID;
- address provider identity;
- work-order provider identity;
- protected image filename;
- capture timestamp.

Current UI selection never becomes a source for retry destination identity.

`UNCERTAIN` is intentionally non-retryable so an interrupted provider call cannot automatically create a duplicate before the later Drive phase determines whether a remote file already exists.

## Offline / restart behavior

All Phase 7A state is app-private local metadata and requires no network.

- `WAITING` survives restart unchanged.
- `FAILED` survives restart unchanged and remains retryable.
- `UPLOADED` survives restart as terminal bookkeeping.
- stale persisted `UPLOADING` is converted to `UNCERTAIN` at actual process startup.
- normal screen recreation does not rewrite `UPLOADING`.
- protected image data is not deleted by reconciliation.
- one record's state transition must not rewrite another record.

## Local deletion behavior

Phase 7A does not add automatic cleanup.

Explicit local discard remains available only for safe pre-confirmation states where no upload is in flight or ambiguous. It refuses `UPLOADING`, `UNCERTAIN`, and `UPLOADED` records so the app does not erase evidence needed to resolve or prevent duplicates.

The photo screen also disables unsafe Prepare/Discard actions based on the persisted queue state and visibly identifies `UNCERTAIN` results as requiring later remote reconciliation.

Future post-confirmation cleanup will be a separate guarded change.

## Failure recovery

- metadata write failure leaves the prior atomic record as the authoritative local state;
- invalid transition fails without changing the record;
- invalid/unknown schema fails closed and leaves paired image data untouched;
- missing protected image is surfaced and never converted into successful upload state by reconciliation;
- interrupted `UPLOADING` is preserved as `UNCERTAIN` with its exact destination binding;
- if process-start reconciliation itself cannot complete, it fails closed without manufacturing a retryable or successful state;
- no queue failure in Phase 7A deletes remote or local photos.

## Safe test fixture

No Drive fixture is required for this local-only phase because no Drive/provider operation is performed.

Temporary JVM/app storage with disposable metadata/image files was used. Persistence tests create one store instance, change state, then construct a new store instance over the same files.

## Automated coverage completed

Focused and complete-suite coverage includes:

- legacy schema-v1 `CAPTURING` and `WAITING` records load safely with queue defaults;
- new writes use schema version 2;
- unknown schema version is rejected without deleting paired image data;
- `WAITING → UPLOADING` persists attempt count/time;
- `FAILED → UPLOADING` increments attempt count and preserves destination identities;
- `UPLOADING → FAILED` persists failure detail while retaining image and destination;
- explicit ambiguous transition produces `UNCERTAIN`;
- actual process-start recovery converts stale `UPLOADING` to `UNCERTAIN` and preserves protected image;
- photo-screen capture reconciliation does not misclassify a live `UPLOADING` state as interrupted;
- `UNCERTAIN` cannot begin another upload attempt;
- `UPLOADED` requires explicit confirmed remote identity and cannot begin another upload attempt;
- confirmed-success bookkeeping preserves the protected original in Phase 7A;
- one photo failure does not alter another photo;
- address/work-order provider identities never change across transitions;
- explicit local discard refuses `UPLOADING`, `UNCERTAIN`, and `UPLOADED`;
- existing capture/restart and Phase 6A preparation/image tests remain passing.

## Final automated verification

Exact tested Phase 7A runtime head:

`95ae6a0c46e48c9f77ca85412ad86f650a4d9459`

Android CI run:

`34434158082`

Job:

`102735664934`

Result: **PASS**.

Passed on the exact runtime head:

- complete JVM/unit suite;
- debug APK build;
- KVM/emulator setup;
- Android emulator instrumented image/preparation tests;
- app launch smoke test with the registered `FieldPhotoPrepApplication` process-start recovery path;
- APK artifact packaging.

Artifact:

- ID: `10135640611`
- digest: `sha256:a26baa39daf27b4e28b452e4bc63fb6acf87a45bd8cd374212d98b71d590fa34`

Test build:

- versionCode: `13`
- versionName: `0.8-phase7a-persistent-upload-queue`

Any later branch commit that changes only this implementation record is documentation-only; the runtime evidence above remains tied specifically to `95ae6a0c46e48c9f77ca85412ad86f650a4d9459`.

## Real-device status

No physical Android device is required to prove this purely local persisted state machine. Phase 7A performs no remote write and does not change camera behavior.

Its later integration with actual Drive upload/reconciliation will require the Android safe-folder reality gate before that Drive phase can merge.

## Merge status

Implementation authorized by the operator's explicit instruction to build **Phase 7A — Persistent Upload Queue State**.

Pre-merge status: **not approved**. This Level 3 stacked branch remains unmerged until its Phase 5/Phase 6A dependency chain is resolved and explicit pre-merge approval is received.
