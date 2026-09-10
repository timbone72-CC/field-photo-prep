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
- convert a persisted `UPLOADING` record found after restart into `UNCERTAIN`, not `WAITING` or `FAILED`;
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
- Phase 7A therefore changes it to `UNCERTAIN` on restart/reconciliation;
- it is never silently changed back to `WAITING` or `FAILED`;
- Phase 7A provides no automatic path from `UNCERTAIN` back to retryable state because remote reconciliation does not exist yet.

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
- stale persisted `UPLOADING` is converted to `UNCERTAIN` during restart reconciliation.
- protected image data is not deleted by reconciliation.
- one record's state transition must not rewrite another record.

## Local deletion behavior

Phase 7A does not add automatic cleanup.

Explicit local discard remains available only for safe pre-confirmation states where no upload is in flight or ambiguous. It must refuse `UPLOADING`, `UNCERTAIN`, and `UPLOADED` records so the app does not erase evidence needed to resolve or prevent duplicates.

Future post-confirmation cleanup will be a separate guarded change.

## Failure recovery

- metadata write failure leaves the prior atomic record as the authoritative local state;
- invalid transition fails without changing the record;
- invalid/unknown schema fails closed and leaves paired image data untouched;
- missing protected image is surfaced and never converted into successful upload state by reconciliation;
- interrupted `UPLOADING` is preserved as `UNCERTAIN` with its exact destination binding;
- no queue failure in Phase 7A deletes remote or local photos.

## Safe test fixture

No Drive fixture is required for this local-only phase because no Drive/provider operation is performed.

Use temporary app/JVM storage containing disposable metadata/image files. Automated tests must prove persistence by creating one store instance, changing state, then constructing a new store instance over the same files.

## Required automated coverage

Focused coverage must include:

- legacy schema-v1 `CAPTURING` and `WAITING` records load safely with queue defaults;
- new writes use schema version 2;
- unknown schema version is rejected without deleting paired image data;
- `WAITING → UPLOADING` persists attempt count/time;
- `FAILED → UPLOADING` increments attempt count and preserves destination identities;
- `UPLOADING → FAILED` persists failure detail while retaining image and destination;
- explicit ambiguous transition produces `UNCERTAIN`;
- restart reconciliation converts stale `UPLOADING` to `UNCERTAIN` and preserves protected image;
- `UNCERTAIN` cannot begin another upload attempt;
- `UPLOADED` requires explicit confirmed remote identity and cannot begin another upload attempt;
- confirmed-success bookkeeping preserves the protected original in Phase 7A;
- one photo failure does not alter another photo;
- address/work-order provider identities never change across transitions;
- explicit local discard refuses `UPLOADING`, `UNCERTAIN`, and `UPLOADED`;
- existing capture/restart and Phase 6A preparation tests remain passing.

Final runtime head must pass the complete JVM suite, debug build, existing Android emulator image instrumentation/launch smoke, and APK packaging.

## Real-device status

No physical Android device is required to prove this purely local persisted state machine. Phase 7A performs no remote write and does not change camera behavior.

Its later integration with actual Drive upload/reconciliation will require the Android safe-folder reality gate before that Drive phase can merge.

## Merge status

Implementation authorized by the operator's explicit instruction to build **Phase 7A — Persistent Upload Queue State**.

Pre-merge status: **not approved**. This Level 3 stacked branch remains unmerged until its dependency chain and explicit pre-merge approval requirements are satisfied.
