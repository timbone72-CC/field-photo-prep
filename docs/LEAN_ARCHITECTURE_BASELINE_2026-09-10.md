# Field Photo Prep Lean Architecture Baseline — 2026-09-10

## Purpose

Record the accepted lean-architecture audit baseline for Field Photo Prep before the physical Phase 6B-H4 Android + Google Drive reality gate and before Phase 7B reconciliation/cleanup design.

This is a documentation-only baseline. It does not authorize or implement runtime cleanup.

## Audit target

Repository: `timbone72-CC/field-photo-prep`

Audited branch line: `feat/phase-6b-h2-create-persist-write-barrier`

Documented H2 head audited: `cbdba0ba89f9b302b496360ff97e9808b5d1a7bc`

Exact H2 runtime/test head beneath that documentation commit: `8d23b061307725589aef69a31a00249744523406`

The audit was performed read-only against an isolated detached worktree after fetching the complete remote history. No runtime, test, Drive, or app data was changed by the audit.

## Executive verdict

**Field Photo Prep is lean.**

The app is near the appropriate minimum architecture for its current field workflow. The audit found only a few low-value cleanup opportunities and no evidence of material architectural bloat.

The governing standard remains:

> The smallest architecture that safely performs the real field workflow.

Safety-critical complexity is not treated as bloat merely because it adds code or tests.

## Measured baseline

At the audited H2 head:

- 17 production Java classes;
- approximately 3,929 production LOC;
- 16 test classes total;
- approximately 2,451 test LOC;
- 80 JVM test methods plus 4 instrumentation test methods;
- 2 activities;
- 1 AndroidX `FileProvider`;
- 0 services;
- 0 workers;
- 0 receivers;
- 0 declared Android permissions;
- 2 meaningful runtime dependencies: `androidx.core` and `androidx.exifinterface`;
- no Room/SQLite persistence layer;
- no Hilt/Dagger dependency injection;
- no WorkManager;
- no Compose;
- no RxJava;
- no Retrofit;
- no Google Drive REST SDK or OAuth SDK;
- no automatic retry scheduler.

Largest production classes at the audited head:

- `PhotoCaptureActivity` — approximately 774 LOC;
- `MainActivity` — approximately 757 LOC;
- `PendingPhotoRecord` — approximately 557 LOC;
- `DrivePhotoUploader` — approximately 460 LOC;
- `PendingPhotoStore` — approximately 431 LOC;
- `PhotoPreparer` — approximately 217 LOC;
- `PhotoUploadCoordinator` — approximately 172 LOC.

The two large activities are recognized maintainability pressure, but broad extraction into view models, presenters, fragments, repositories, use-case layers, or other framework architecture is not justified merely to reduce file size.

## Architecture decisions confirmed by the audit

### Keep the lightweight persistence model

`PendingPhotoStore` remains an appropriate persistence boundary for the current queue size and workflow. File/`.properties` records, explicit schema handling, atomic temporary-file replacement, durable sync, and restart recovery provide the required safety without introducing a database.

**Decision:** do not add Room, SQLite, repository frameworks, or a new persistence layer without a demonstrated requirement.

### Keep the narrow Android/SAF integration

`DriveClient` and `DrivePhotoUploader` have distinct responsibilities: folder/provider identity operations versus staged photo upload/verification.

**Decision:** keep that separation. Do not replace it with a generic Drive abstraction or Google Drive REST/OAuth stack without a real workflow need.

### Keep the upload safety boundaries

The following are deliberate protections and are classified **DO NOT TOUCH** during lean-up work:

- immutable address/work-order provider destination identity;
- protected local originals;
- separate prepared JPEG copies;
- persistent queue state;
- `CAPTURING`, `WAITING`, `UPLOADING`, `FAILED`, `UNCERTAIN`, and `UPLOADED` distinctions;
- `provisionalRemoteFileId`;
- create → persist provisional identity → write barrier;
- staged upload binding to local photo UUID, destination, remote identity, filename, and expected byte count;
- process-start recovery of interrupted uploads;
- fail-closed uncertainty handling;
- verification before confirmed `UPLOADED`;
- deterministic UUID-based remote filenames;
- duplicate-prevention safeguards;
- provider/failure-path tests that prove these boundaries.

These protections are the main reason the app remains reliable without heavier frameworks.

## Cleanup candidates identified

The audit found only small candidates:

1. `QueueStartupRecovery` is a thin semantic wrapper around process-start upload recovery.
2. `PhotoCaptureActivity` contains a small amount of duplicated scan-warning rendering logic.
3. `PendingPhotoStore.unusableWaitingPhotoIds()` is a compatibility alias retained from earlier phase callers.
4. `PendingPhotoStore.recordsForWorkOrder()` is currently not used by production UI code and may either become useful for Phase 7B or later become removable.
5. Documentation phase status can become unclear when runtime implementation is complete but physical device gates and merge approval are still pending.

## Accepted disposition

No runtime cleanup phase will be started now.

Current disposition:

- `QueueStartupRecovery` — **KEEP for now**. The small class makes process-start recovery ownership explicit; deleting it has negligible benefit.
- duplicate warning-rendering logic — **DEFER until after H4**. It is real duplication but not harmful enough to justify churn before physical workflow testing.
- `unusableWaitingPhotoIds()` — **DEFER until Phase 7B design**. Do not remove a compatibility surface until future queue/reconciliation needs are known.
- `recordsForWorkOrder()` — **DEFER until Phase 7B design**. It may be a natural reconciliation query and should not be deleted prematurely.
- large activities — **KEEP / targeted extraction only if a concrete maintenance problem appears**. Do not create framework layers simply to reduce activity LOC.
- runtime dependencies — **KEEP exactly as currently justified**.
- manifest/component footprint — **KEEP**.
- queue/state/upload safety implementation — **DO NOT TOUCH** for lean-up purposes.

## Why cleanup is intentionally deferred

The remaining Phase 6B-H4 gate requires the real Android Google Drive `DocumentsProvider`. Runtime churn immediately before that gate would add variables without meaningful lean-architecture benefit.

After H4, Phase 7B will define remote retry, reconciliation, and cleanup behavior. That design may establish whether the compatibility/query APIs identified by the audit are still required.

Therefore the next architecture review point is:

1. complete Phase 6B-H4 on a physical Android device using a safe Google Drive test hierarchy;
2. record the H4 evidence and outcome;
3. design Phase 7B reconciliation/cleanup against the proven provider behavior;
4. only then revisit the deferred tiny cleanup candidates;
5. do not introduce cleanup that replaces one small class with multiple abstractions.

## Documentation status correction

The roadmap should distinguish four separate states where relevant:

- runtime implementation complete;
- automated verification complete;
- physical Android/Google Drive reality gate pending;
- merge/release approval pending.

A phase must still satisfy its governed real-device gates and merge requirements before being marked `COMPLETE`.

## Protected behavior

This baseline changes no approved behavior. In particular it does not alter:

- camera capture;
- protected local photo storage;
- photo preparation/compression;
- queue schema or state transitions;
- retry eligibility;
- `UNCERTAIN` semantics;
- provisional or confirmed remote identity;
- Drive destination selection;
- SAF permissions;
- remote create/write ordering;
- cleanup/deletion;
- Android manifest or dependencies;
- tests.

## Verification for this record

Change class: **Level 1 — documentation only**.

Required verification is contract and diff review only. Runtime tests are not required because no runtime/test/configuration behavior is changed.

## Baseline conclusion

Field Photo Prep has not drifted into an architecture showcase. Its complexity is concentrated around photo protection, stable provider identity, durable queue state, and preventing ambiguous remote operations from becoming lost or duplicate work.

Until H4 and the Phase 7B design establish a concrete reason to change that structure, the runtime should remain unchanged.
