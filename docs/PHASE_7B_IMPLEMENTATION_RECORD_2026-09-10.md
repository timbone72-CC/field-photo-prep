# Phase 7B Implementation Record — Remote Reconciliation, Retry & Cleanup

Date: 2026-09-10

Status: **RUNTIME + AUTOMATED VERIFICATION COMPLETE — PHYSICAL DEVICE GATE STAGED**

## Purpose

This record captures the exact Phase 7B runtime that was built after the Phase 6B-H4 Samsung Galaxy A16 + real Google Drive evidence was accepted.

Phase 7B now implements the conservative `UNCERTAIN` reconciliation, guarded retry release, confirmed-match promotion, and post-confirmation local cleanup defined by `docs/PHASE_7B_IMPACT_AND_IMPLEMENTATION_PLAN_2026-09-10.md`.

This record does **not** grant Level 3 merge approval. The runtime is frozen at the next genuine Android/Google Drive reality boundary pending the staged device gate.

## Repository / branch / base

Repository: `timbone72-CC/field-photo-prep`

Branch: `feat/phase-7b-remote-reconciliation-cleanup`

Exact governed base / rollback head:

`d1e2e2c4ea60e0ebad57b097d29abc555dd2c4f3`

Exact Phase 7B runtime/test head:

`f25868dd768dcdddb11ac4d6ab3879a7cde85d2c`

Version:

- `versionCode 15`
- `versionName 0.10-phase7b-reconciliation-cleanup`

## Implemented behavior

### Conservative `UNCERTAIN` reconciliation

A new narrow `DrivePhotoReconciler` handles only one persisted `UNCERTAIN` photo at a time.

It:

- uses the photo's immutable local UUID and stored work-order provider DocumentId;
- prefers an exact persisted `provisionalRemoteFileId` when present;
- otherwise inspects the exact stored work-order parent for the deterministic `field-photo-<photoUUID>.jpg` name;
- requests bounded provider refresh and rejects `DocumentsContract.EXTRA_LOADING` state;
- requires two matching settled child snapshots before absence/cardinality can affect retry safety;
- never guesses between multiple same-name candidates;
- uses provider metadata as a cheap screen and SHA-256 of the exact prepared JPEG only when uncertainty resolution requires content proof;
- returns only `CONFIRMED_MATCH`, `CONFIRMED_ABSENT_RETRY_SAFE`, or `REMAIN_UNCERTAIN`;
- performs no remote create, write, delete, rename, or overwrite operation during reconciliation.

### Guarded retry release

`PendingPhotoRecord` and `PendingPhotoStore` now have a dedicated reconciliation-only transition for `UNCERTAIN → FAILED`.

That transition:

- is unavailable from any other queue state;
- refuses retry release while unresolved provisional remote identity remains;
- preserves local photo identity, address/work-order identities, attempt count, last-attempt time, original image, and prepared JPEG;
- clears only uncertainty/provisional bookkeeping that the reconciliation result has explicitly proven safe to release;
- reuses the existing `FAILED → UPLOADING` user-initiated retry path; no automatic retry scheduler was added.

### Confirmed reconciliation

When one exact candidate is proven identical to the prepared JPEG, `PhotoUploadCoordinator` promotes the existing uncertain record directly to `UPLOADED` with that exact provider identity.

No second remote object is created.

### Confirmed local cleanup

A narrow `ConfirmedPhotoCleanup` helper coordinates local cleanup only after durable `UPLOADED + remoteFileId` exists.

Cleanup:

- removes the protected original through `PendingPhotoStore`;
- removes the prepared derivative through `PhotoPreparer`;
- retains queue metadata and confirmed `remoteFileId`;
- never deletes or rewrites the Drive object;
- does not roll a confirmed upload backward when a local deletion fails;
- treats leftover local files beside an `UPLOADED` record as cleanup-pending evidence that can be retried safely.

`PhotoCaptureActivity` attempts cleanup for previously confirmed records when the photo screen opens and attempts cleanup immediately after a newly confirmed upload or reconciliation result.

### UI behavior

The photo screen now identifies the build as:

`Phase 7B · Reconciliation + local cleanup`

One explicit `Reconcile Uncertain Upload` control is enabled only for `UNCERTAIN` records.

While reconciliation runs, the screen states that it is checking the stored destination and that no remote write is allowed.

Cleanup failure is reported separately from upload success so a local filesystem problem cannot masquerade as a failed Drive upload.

## Lean-architecture result

The accepted lean baseline was preserved.

Phase 7B added only two production classes:

- `DrivePhotoReconciler`
- `ConfirmedPhotoCleanup`

No Room/SQLite, WorkManager, service, receiver, Hilt/Dagger, Compose, RxJava, Retrofit, Google Drive REST/OAuth stack, new Android permission, or new runtime dependency was added.

The queue remains schema version 3. No persisted cleanup state machine was introduced.

The previously deferred lean-audit candidates were revisited as planned:

- `QueueStartupRecovery` — kept;
- `unusableWaitingPhotoIds()` compatibility alias — kept;
- `recordsForWorkOrder()` — kept;
- activity architecture — not split into framework layers;
- unrelated warning-rendering duplication — still deferred.

## Exact changed surfaces from Phase 7B base

Runtime/config:

- `app/build.gradle`
- `app/src/main/java/com/inandout/fieldphotoprep/ConfirmedPhotoCleanup.java`
- `app/src/main/java/com/inandout/fieldphotoprep/DrivePhotoReconciler.java`
- `app/src/main/java/com/inandout/fieldphotoprep/PendingPhotoRecord.java`
- `app/src/main/java/com/inandout/fieldphotoprep/PendingPhotoStore.java`
- `app/src/main/java/com/inandout/fieldphotoprep/PhotoCaptureActivity.java`
- `app/src/main/java/com/inandout/fieldphotoprep/PhotoPreparer.java`
- `app/src/main/java/com/inandout/fieldphotoprep/PhotoUploadCoordinator.java`

Focused tests:

- `ConfirmedPhotoCleanupTest.java`
- `DrivePhotoReconcilerTest.java`
- `PendingPhotoReconciliationTest.java`
- `PhotoUploadReconciliationCoordinatorTest.java`

Governance:

- `docs/PHASE_7B_IMPACT_AND_IMPLEMENTATION_PLAN_2026-09-10.md`
- this implementation record
- `docs/PHASE_7B_DEVICE_REALITY_GATE_PLAN_2026-09-10.md`

No manifest or dependency-set expansion occurred.

## Automated verification

Exact final runtime/test head:

`f25868dd768dcdddb11ac4d6ab3879a7cde85d2c`

GitHub Actions:

- workflow: `Android CI`
- run: `34512362110`
- job: `102989361359`
- result: **PASS**

The exact runtime passed:

1. complete JVM/unit test suite;
2. debug APK build;
3. Android connected instrumentation tests on API 35 emulator;
4. debug APK install;
5. app launch smoke test;
6. APK artifact packaging.

Exact CI artifact:

- artifact ID: `10166488011`
- artifact name: `field-photo-prep-phase-1-debug-apk` (legacy workflow artifact label; contents are the Phase 7B build)
- artifact SHA-256: `544a834c701e16f650cd2738d2ccd76be4a9aa031900974f0027f65a265689c5`
- artifact size: `2451856` bytes

The artifact label remains inherited from the existing CI workflow and was not renamed because that is unrelated to Phase 7B runtime correctness.

## Automated evidence proven

Focused and complete-suite coverage now proves at minimum:

- ordinary `UNCERTAIN` remains non-retryable;
- reconciliation-specific retry release is the only `UNCERTAIN → FAILED` path;
- unresolved provisional identity blocks retry release;
- exact provisional identity is preferred;
- matching remote content confirms the same provider identity without remote create;
- mismatch remains uncertain;
- no-provisional + two settled zero-match snapshots can become retry-safe absence;
- unresolved provisional + zero parent matches remains uncertain;
- one exact deterministic-name match can be SHA-256 proven;
- multiple same-name candidates remain uncertain;
- loading, refresh rejection, inconsistent snapshots, candidate read/hash failure, and inconclusive provider evidence fail closed;
- reconciliation does not call provider create/write/delete operations;
- one photo's reconciliation does not mutate another;
- cleanup refuses non-`UPLOADED` records;
- confirmed cleanup can remove both local image copies while retaining `UPLOADED` metadata and confirmed remote identity;
- cleanup failure does not roll back confirmed success or initiate another upload;
- existing H2 create → persist provisional identity → write/verify barrier behavior remains passing;
- existing capture, preparation, queue migration, restart, and upload tests remain passing.

## Physical evidence still required

Automation cannot prove real Google Drive `DocumentsProvider` behavior for the new Phase 7B read-only reconciliation and cleanup boundary.

The next device session is intentionally small and is governed by `docs/PHASE_7B_DEVICE_REALITY_GATE_PLAN_2026-09-10.md`.

Required real-device checks are:

1. install/update the exact Phase 7B APK over the existing H2 app without clearing data;
2. prove the H4 confirmed photo's retained local image copies are removed while its `UPLOADED` metadata and Drive JPEG remain intact;
3. perform one fresh safe test capture → prepare → upload and prove immediate post-confirmation local cleanup while the new Drive JPEG remains present and usable;
4. close/reopen and prove confirmed metadata survives without local-image resurrection;
5. do not manufacture a real ambiguous upload solely to obtain `UNCERTAIN`; if no safe real `UNCERTAIN` fixture exists, record reconciliation's physical path as **NOT SAFELY INDUCIBLE** rather than adding a fault-injection backdoor or risking duplicate Drive content.

If a natural `UNCERTAIN` result occurs during the ordinary safe test, do not start another upload. Capture the evidence and use the explicit reconciliation control once under the gate plan.

## Merge boundary

Phase 7B remains **unmerged**.

Passing CI does not grant Level 3 pre-merge approval. The exact runtime is frozen for the physical reality gate. No additional runtime change should be stacked on this branch unless the device gate exposes a real defect or assumption failure.

After the device gate is recorded, the next step is explicit Level 3 review/merge authorization—not more feature development.