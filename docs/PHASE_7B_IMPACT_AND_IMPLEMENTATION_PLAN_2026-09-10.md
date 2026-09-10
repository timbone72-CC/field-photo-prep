# Phase 7B Impact and Implementation Plan — Remote Reconciliation, Retry & Cleanup

Date: 2026-09-10

Status: **IMPLEMENTATION AUTHORIZED — runtime not yet changed by this record**

## Purpose

Phase 7B finishes the weak/no-service upload lifecycle after the real Phase 6B-H4 Android + Google Drive gate passed. It must reconcile `UNCERTAIN` uploads without blind duplicate creation, release retry only when remote absence is established strongly enough, and remove unnecessary local image copies only after confirmed remote success and durable local bookkeeping.

This plan is based on the completed physical-device evidence in `docs/DEVICE_REALITY_GATE_RECORD_2026-09-10.md`. It does not guess around the provider behavior that was not safely inducible.

## Exact governed base

Repository: `timbone72-CC/field-photo-prep`

Phase 7B branch: `feat/phase-7b-remote-reconciliation-cleanup`

Exact base / rollback documentation head:

`d1e2e2c4ea60e0ebad57b097d29abc555dd2c4f3`

Exact tested H2 runtime beneath that documentation head:

`8d23b061307725589aef69a31a00249744523406`

No Level 3 merge approval is granted by implementation authorization.

## Change level

**Level 3.**

Phase 7B changes retry eligibility after uncertainty, remote reconciliation behavior, and automatic local cleanup after confirmed upload. A bad implementation could duplicate a photo, destroy evidence needed to resolve an ambiguous remote result, release retry too early, or delete the only recoverable local image before remote success is secure.

## Physical-device evidence absorbed

The completed 2026-09-10 Samsung Galaxy A16 gate established:

1. Google Drive SAF child state can temporarily be loading/non-authoritative.
2. The existing bounded/fail-closed freshness posture is necessary.
3. A normal successful photo upload can return a provider document identity, verify through the provider path, reach durable `UPLOADED`, and later be visible in Google Drive backend storage.
4. Provider DocumentId and Google Drive backend file ID are distinct and must not be substituted for each other.
5. The deterministic name `field-photo-<local-photo-uuid>.jpg` survived end to end.
6. The confirmed H4 photo retained both the protected original and prepared copy, so Phase 7B starts from a safe cleanup posture.
7. A real ambiguous/interrupted provider outcome was not safely inducible. Phase 7B therefore keeps `UNCERTAIN` conservative rather than inventing optimistic provider behavior.

## Approved Phase 7B scope

### Remote uncertainty reconciliation

For one persisted `UNCERTAIN` photo:

- use only its immutable local photo UUID and stored work-order provider identity;
- prefer exact `provisionalRemoteFileId` evidence when available;
- otherwise/fallback inspect the exact stored work-order parent for the deterministic filename;
- use provider state authoritative enough for absence/cardinality decisions;
- never use the currently open UI destination as reconciliation identity;
- never guess between multiple same-named remote candidates;
- compare remote content with the exact prepared local JPEG using SHA-256 only when resolving uncertainty;
- confirm an identical remote candidate without creating another copy;
- release retry only when authoritative-enough settled state proves safe absence and no unresolved provisional identity remains;
- keep mismatched, partial, duplicate, inaccessible, loading, or otherwise inconclusive results `UNCERTAIN`.

### Safe retry release

- `UNCERTAIN → FAILED` is allowed only from a reconciliation result that proves retry-safe absence.
- The transition preserves local photo identity, exact address/work-order provider identities, attempt count, last-attempt time, and protected/prepared local data.
- Any unresolved provisional remote identity blocks retry release.
- A later retry uses the existing `FAILED → UPLOADING` path and original immutable destination.
- No automatic/background retry scheduler is added.

### Confirmed reconciliation

- An `UNCERTAIN` photo may transition directly to `UPLOADED` only when the reconciler proves one exact remote candidate contains the prepared photo.
- The confirmed provider document identity becomes `remoteFileId` using the existing confirmed-success semantics.
- No new remote create occurs during confirmed reconciliation.

### Post-confirmation local cleanup

After durable `UPLOADED` bookkeeping exists:

- remove the protected original image file;
- remove the prepared JPEG copy;
- retain the lightweight queue metadata and confirmed `remoteFileId` needed for duplicate protection/history;
- never delete the Drive photo as part of local cleanup;
- never roll a confirmed upload backward because local deletion fails;
- treat leftover local files beside an `UPLOADED` record as cleanup-pending evidence that can be safely retried later.

A new persisted cleanup schema/state is **not planned** unless implementation proves file-presence plus terminal `UPLOADED` metadata is insufficient. The current schema version 3 is expected to remain valid.

## Explicit non-scope

Phase 7B does not authorize:

- Room, SQLite, WorkManager, Hilt/Dagger, RxJava, Compose, Retrofit, Google Drive REST/OAuth, or a new persistence framework;
- automatic/background upload retry;
- remote delete, overwrite, rename, or repair of an uncertain photo;
- choosing a replacement destination;
- treating a single empty provider listing as proof of absence;
- SHA-256 hashing on every normal successful upload;
- cleanup of test Drive folders created during device gates;
- Phase 3B/4 merge or branch integration;
- work-order input/search UI fixes;
- replacing the external Samsung/Android camera flow;
- permanent local gallery/history images;
- workbook or Free Map Router integration.

## Reconciliation decision ladder

The runtime must implement this order conservatively.

### 1. Validate local evidence

Require:

- state `UNCERTAIN`;
- exact stored work-order provider ID;
- deterministic expected remote filename from local photo UUID;
- prepared local JPEG exists and is non-empty before content confirmation can occur.

If the prepared JPEG is missing, do not release retry and do not claim remote confirmation from weak metadata alone. Remain `UNCERTAIN`.

### 2. Exact provisional identity first

When `provisionalRemoteFileId` exists:

- resolve that exact provider document identity inside the persisted tree;
- if it resolves to the expected JPEG/name and SHA-256 matches the prepared JPEG, confirm it as `UPLOADED`;
- if it resolves but name/type/content mismatches, remain `UNCERTAIN`;
- if it cannot be resolved conclusively, parent discovery may still find and confirm an identical deterministic-name candidate, but **zero parent matches must not release retry while unresolved provisional evidence remains**.

This intentionally favors duplicate avoidance over convenience.

### 3. Settled parent discovery fallback

Use the exact stored work-order parent. Request provider refresh when supported, reject `DocumentsContract.EXTRA_LOADING`, and require bounded matching settled snapshots before using cardinality/absence.

The existing Phase 3B device-proven pattern of bounded retries with two matching settled snapshots is the preferred implementation baseline. Do not create an unbounded refresh loop.

Find children whose visible name exactly equals the deterministic filename.

- exactly one candidate: inspect further;
- more than one candidate: remain `UNCERTAIN`;
- zero candidates:
  - if `provisionalRemoteFileId == null`, reconciliation may release to `FAILED`/retryable;
  - if unresolved provisional identity exists, remain `UNCERTAIN`.

### 4. Candidate proof

For the single candidate:

- provider ID must be nonblank;
- exact name must match;
- MIME type must be `image/jpeg`;
- remote content must hash to the same SHA-256 as the exact prepared local JPEG before uncertain state is promoted to confirmed success.

Provider-reported size may be used as a cheap mismatch check, but unknown size or a matching size is not by itself sufficient to prove uncertain content identical.

### 5. Final classification

Possible reconciler results:

- `CONFIRMED_MATCH(remoteProviderId)`;
- `CONFIRMED_ABSENT_RETRY_SAFE`;
- `REMAIN_UNCERTAIN(detail)`.

No reconciler result authorizes remote deletion, overwrite, second-create, or destination change.

## Provider access / freshness implementation

Phase 7B may add the narrow provider operations needed to:

- read one exact document by provider ID;
- enumerate exact parent children with id/name/MIME/size;
- request provider refresh for parent and child-list URIs;
- inspect `EXTRA_LOADING`;
- require bounded matching settled snapshots;
- open one candidate for read-only SHA-256 comparison.

This provider code should remain narrow and photo-specific. Do not generalize it into a full Drive repository/client framework.

## Local cleanup implementation

Ownership remains split:

- `PendingPhotoStore` owns deletion of the protected original after it verifies the record is durably `UPLOADED`;
- `PhotoPreparer` owns deletion of its prepared derivative;
- `PhotoUploadCoordinator` owns ordering and may report whether cleanup completed, but it must not reinterpret a cleanup failure as remote upload failure;
- UI may initiate/display reconciliation or cleanup but must not manufacture queue transitions or remote identity.

Expected cleanup order:

`durable UPLOADED + confirmed remote identity → attempt protected-original removal → attempt prepared-copy removal → retain metadata`

Both local deletions are safe after confirmed success. If either fails, keep `UPLOADED`; do not remove metadata and do not touch Drive. A later cleanup attempt may remove whichever local file remains.

## Lean-architecture decisions revisited

From `docs/LEAN_ARCHITECTURE_BASELINE_2026-09-10.md`:

- `QueueStartupRecovery`: **KEEP**; still useful process-boundary ownership.
- duplicate warning rendering: **DEFER**; unrelated to Phase 7B correctness.
- `unusableWaitingPhotoIds()` compatibility alias: **KEEP for this phase unless direct compilation/use evidence proves removal safe**; no benefit to churn.
- `recordsForWorkOrder()`: **KEEP**; it remains a natural lightweight queue query and should not be removed during reconciliation work.
- large activities: **NO ARCHITECTURAL SPLIT**. Add only the smallest reconciliation/cleanup UI controls needed.

A single narrow new reconciliation helper/class is acceptable if it keeps provider-specific uncertainty logic out of the activity and avoids bloating the normal uploader.

## Expected owning files

Likely runtime surfaces:

- `PendingPhotoRecord.java` — add only the guarded `UNCERTAIN → FAILED` retry-release transition if needed;
- `PendingPhotoStore.java` — persist retry-release transition and confirmed protected-original cleanup;
- `PhotoPreparer.java` — prepared-copy cleanup;
- `DrivePhotoReconciler.java` — narrow provider reconciliation/freshness/hash logic (new, if implementation confirms this is the smallest boundary);
- `PhotoUploadCoordinator.java` — sequence reconciliation results into durable queue transitions and local cleanup;
- `PhotoCaptureActivity.java` — minimal reconcile/cleanup initiation and status rendering only.

No manifest, permission, OAuth, dependency, database, worker/service, or folder-identity change is planned.

## Read/write surfaces

Reads:

- app-private queue metadata;
- protected original presence;
- prepared JPEG bytes for uncertainty hash proof;
- persisted master tree URI;
- exact stored work-order provider identity;
- exact provisional remote provider identity when present;
- exact remote candidate metadata/content through SAF.

Writes:

- local queue transition `UNCERTAIN → UPLOADED` or guarded `UNCERTAIN → FAILED`;
- deletion of local protected original/prepared derivative only after durable `UPLOADED`;
- no remote write during reconciliation;
- a later user-initiated retry from `FAILED` reuses the existing Phase 6B create/write path.

## Failure posture

- provider loading/stale/inconsistent → remain `UNCERTAIN`;
- provider refresh unavailable when absence/cardinality matters → remain `UNCERTAIN`;
- provisional identity resolves mismatched → remain `UNCERTAIN`;
- provisional identity cannot be resolved and no deterministic candidate is found → remain `UNCERTAIN`;
- multiple deterministic-name candidates → remain `UNCERTAIN`;
- candidate hash mismatch/read failure → remain `UNCERTAIN`;
- local transition persistence failure → preserve previous queue record and local image data;
- local cleanup failure after confirmed success → remain `UPLOADED`, retain metadata, leave remaining local file for later cleanup retry;
- no failure path deletes remote content.

## Automated verification plan

Develop with focused tests only, then run the complete repository suite once on the final runtime head.

Focused coverage must prove at minimum:

1. `UNCERTAIN → FAILED` is allowed only through the reconciliation-specific transition and preserves immutable identities/attempt history.
2. Retry release clears provisional identity only when safe absence is explicitly supplied.
3. Existing normal `UNCERTAIN` remains non-retryable.
4. Exact provisional candidate + matching content confirms the same provider identity without create.
5. Exact provisional candidate mismatch remains uncertain.
6. No provisional + two settled parent snapshots + zero deterministic-name matches returns retry-safe absence.
7. Unresolved provisional + zero parent matches remains uncertain.
8. Exactly one deterministic-name candidate + matching SHA-256 confirms it.
9. Multiple same-name candidates remain uncertain.
10. Provider `EXTRA_LOADING`, inconsistent snapshots, refresh rejection, read/hash error, unknown/inconclusive metadata all fail closed.
11. Reconciliation never calls remote create/write/delete.
12. One photo reconciliation does not change another photo.
13. Confirmed cleanup removes protected original and prepared JPEG while retaining `UPLOADED` metadata/confirmed remote ID.
14. Cleanup refuses any non-`UPLOADED` record.
15. Cleanup failure does not roll back `UPLOADED`, remove confirmed remote identity, or trigger a new upload.
16. Existing H2 create → provisional-persist → write barrier tests remain passing.
17. Existing queue restart, capture, and preparation tests remain passing.

## Next physical-device boundary

Do **not** stop implementation merely because Phase 7B will require a phone check later.

Build and automate everything above first. The next phone gate is expected only after the final Phase 7B runtime is frozen and should be limited to real provider behavior that automation cannot prove, especially:

- real Google Drive settled parent discovery used by reconciliation;
- real read-only candidate content access/hash through SAF;
- a safe real-device reconciliation scenario if one can be constructed without a dangerous production fault-injection backdoor;
- automatic local cleanup after a confirmed real upload and proof the Drive copy remains intact;
- cleanup/restart behavior on the physical device where meaningful.

If a real ambiguous upload still cannot be induced safely, do not manufacture one. Stage the smallest honest substitute fixture/test method and document the limitation.

## Straight-line / no-loop rule

This phase follows `docs/PHASE_STAGING_DOCTRINE.md`.

- settled H4 evidence is reused;
- no repeated Phase 6 phone checks;
- no repeated complete test suite during development;
- no one-command-at-a-time Bash ritual;
- no repeated approval prompts while this scope remains unchanged;
- no runtime merge without explicit Level 3 pre-merge approval;
- stop only for a real failed assumption, failed required test, scope expansion, or the next genuine device/provider boundary.
