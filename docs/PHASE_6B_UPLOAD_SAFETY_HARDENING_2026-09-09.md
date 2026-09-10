# Phase 6B Upload Safety Hardening Plan

Date: 2026-09-09

## Purpose

This record documents the approved design hardening for the existing Phase 6B Drive upload work before any additional runtime changes are made.

The hardening exists for one narrow reason: after Android SAF `createDocument()` returns a provider document identity, the current Phase 6B runtime can begin writing bytes before that newly created remote identity has been durably recorded in the local queue. If the process is interrupted in that interval, the queue correctly becomes `UNCERTAIN`, but the exact provisional remote document identity may be lost from local bookkeeping.

This record closes that design gap without broadening Field Photo Prep into a general Drive client, changing its capture model, changing its deterministic filename scheme, adding automatic retry, or adding cleanup behavior to Phase 6B.

This is a documentation/governance change only. No app runtime code, tests, APK, Drive data, or persisted app data are changed by this record.

Where this record conflicts with `docs/PHASE_6B_IMPLEMENTATION_RECORD_2026-09-09.md`, this hardening record governs the next Phase 6B runtime revision. Existing verified behavior remains valid historical evidence for the pre-hardening runtime but is not final evidence for the future hardened runtime.

## Exact baseline

Repository:

`timbone72-CC/field-photo-prep`

Working branch being hardened:

`feat/phase-6b-drive-upload`

Branch head before this documentation reconciliation:

`5c09771e097b2e5578a4528886654463754d2d29`

Branch tree before this documentation reconciliation:

`4a8cf299fd947b2370f313fbc865b00ad8ba1ed1`

Newer governance source:

`main` at `ffcbfdbb0a971f207924a975af595a7547d8d85e`

Common merge base observed during reconciliation:

`8467381b8dac628c6afbb7ea141ba94f0b298417`

At comparison time the branches were diverged. `main` contained eight commits after the common base that were not on the Phase 6B branch, while the Phase 6B stack contained fifty-three commits after the common base that were not on `main`. Therefore this record does not merge or rebase runtime history. It carries the newer governance documents forward onto the Phase 6B branch while preserving the Phase 5 → Phase 6A → Phase 7A → Phase 6B runtime stack unchanged.

## Governance reconciliation

The Phase 6B branch had older governance language that still described Drive identity and authorization in generic Google API/OAuth terms. The current Android implementation actually uses Android Storage Access Framework (SAF), the system `DocumentsProvider`, an operator-selected persisted tree URI, and provider-assigned document IDs.

The following governing files on the Phase 6B branch are reconciled to the exact versions from `main` at `ffcbfdbb0a971f207924a975af595a7547d8d85e`:

- `AGENTS.md`
- `CHANGE_CONTROL_CONTRACT.md`
- `CONTRACT.md`
- `INTEGRATION_CONTRACT.md`
- `REGRESSION_CHECKLIST.md`
- `TESTING_CONTRACT.md`

No runtime source file is part of this reconciliation.

The newer governance establishes these rules for all later Phase 6B/7B work:

1. The persisted Android master tree URI defines the approved remote-document boundary.
2. Provider document IDs are authoritative stable identities within that persisted tree context; display names are discovery/display information only.
3. Provider document IDs are treated as opaque provider-owned values. The app does not infer backend Google Drive ID structure from them.
4. The Android workflow does not invent, persist, or manage Google OAuth access/refresh tokens for the approved SAF path.
5. Provider loading, stale, inconsistent, or otherwise uncertain state cannot prove absence or emptiness for a risky write decision.
6. `DocumentsContract.EXTRA_LOADING = true` is non-authoritative. Its absence or false value is not by itself proof of real-time cloud freshness.
7. Risky absence/emptiness decisions use settled-state verification and fail closed when authoritative-enough provider state cannot be established.
8. Mock/emulator evidence is not represented as proof of real Google Drive `DocumentsProvider` freshness or synchronization behavior.
9. Level 3 Drive changes require the affected Android SAF/DocumentsProvider reality-gate checks on a safe test destination before merge approval.

These rules strengthen Phase 6B. They do not conflict with its existing immutable-destination, fail-closed uncertainty, photo-protection, or deterministic-naming behavior.

## Existing Phase 6B behavior preserved

The following existing Phase 6B decisions remain approved and should not be rewritten merely because this hardening is being added:

- the local photo UUID remains the immutable photo identity;
- the exact `workOrderId` stored on the queued photo remains the only upload parent identity;
- current UI address/work-order selection cannot redirect a queued photo;
- remote photo naming remains deterministic and UUID-based using the existing `field-photo-<local-photo-uuid>.jpg` form;
- `UPLOADING` is durably entered before the first remote write-capable operation;
- deterministic failures before remote create remain `FAILED`/retryable;
- create/write/post-create ambiguity remains `UNCERTAIN` and blocks blind retry;
- `remoteFileId` remains confirmed-success identity only;
- Phase 6B does not delete the protected original, prepared JPEG, or uncertain remote content;
- Phase 6B does not add automatic/background retry;
- provider I/O remains off the Android UI thread;
- one photo's upload result must not rewrite another photo's queue state.

The current filename prefix is intentionally retained. Replacing `field-photo-` with `FPP_` would provide no additional identity or idempotency protection and would create unnecessary churn.

## The hardening gap

The pre-hardening runtime performs the remote steps conceptually as:

`UPLOADING → createDocument() → receive created provider ID in memory → stream bytes → re-read metadata → commit remoteFileId + UPLOADED`

This correctly prevents a successful upload from being reported before post-create verification and local confirmation. It also correctly classifies create/write/post-create failures as `UNCERTAIN`.

The remaining gap is process interruption after `createDocument()` has produced a valid document identity but before confirmed-success bookkeeping. The created identity currently lives inside the active upload call until success. A process death can therefore leave:

- local state: `UPLOADING`, which Phase 7A later converts to `UNCERTAIN` on process startup;
- deterministic remote filename: still known from the immutable photo UUID;
- exact work-order parent identity: still known;
- exact newly created remote document identity: potentially lost locally.

The deterministic filename provides a recovery fallback, but preserving the exact provisional remote identity is safer and cheaper to reconcile than searching by name first.

## Required future persisted state

The Phase 6B hardening will introduce a persisted nullable **provisional remote provider identity** for a photo after remote document creation but before confirmed upload.

Preferred field name:

`provisionalRemoteFileId`

Semantics:

- `null` before a remote create has returned a usable identity;
- set only from the exact provider identity returned by the upload's `createDocument()` path;
- never manufactured from a display name, timestamp, UI selection, or guessed Drive path;
- does not mean upload success;
- survives process/app restart when present;
- is preserved in `UNCERTAIN` state;
- cannot make an `UNCERTAIN` record retryable by itself;
- `remoteFileId` remains reserved for confirmed successful remote upload.

Because Phase 7A queue metadata currently uses schema version 2, adding this persisted field is a Level 3 stored-schema change. The implementation must use an explicit backward-compatible schema migration rather than silently changing the meaning of version 2. Existing valid version-2 records must load with `provisionalRemoteFileId = null`, preserving every existing immutable identity, state, attempt counter, timestamp, status detail, protected image reference, prepared image relationship, and confirmed remote identity.

The implementation record for the runtime change must state the exact next schema version before code is merged.

## Required hardened upload ordering

The future hardened upload sequence is:

1. Read the exact queued photo and its immutable stored work-order provider document ID.
2. Confirm the record is eligible to begin from `WAITING` or `FAILED`.
3. Durably transition that exact photo to `UPLOADING` and increment its attempt bookkeeping.
4. Validate the prepared JPEG and exact stored destination before remote creation.
5. Call the SAF provider create operation using the exact stored work-order provider ID and the deterministic UUID filename.
6. If create returns a valid provider identity, durably persist that identity as `provisionalRemoteFileId` **before streaming prepared JPEG bytes**.
7. Only after that local persistence succeeds, open/write/close the created document and perform the normal post-write provider verification.
8. If provider-confirmed success is established and local confirmation bookkeeping succeeds, atomically record `UPLOADED` with the confirmed `remoteFileId` equal to the exact created provider identity. The provisional field may be cleared in that same confirmed transition because `remoteFileId` then owns confirmed identity.
9. Any ambiguous result after remote creation begins remains `UNCERTAIN`; blind create/retry remains blocked.

If durable persistence of `provisionalRemoteFileId` fails after remote creation has already returned, the upload must stop before streaming bytes when possible and the result must be treated as `UNCERTAIN`. The app must not pretend the create did not happen and must not create another remote file automatically.

There remains an unavoidable very small crash window between the provider returning a created identity and the app durably persisting it. If the process dies in that window, the queue can be `UNCERTAIN` with no provisional ID. That is why deterministic UUID naming and parent-folder reconciliation remain required fallbacks in Phase 7B.

## Provider confirmation terminology

A successful SAF output-stream close proves that the app completed its write interaction with the provider without receiving an error. It does **not** independently prove that Google's backend has finished cloud synchronization.

For this app, `UPLOADED` means the approved Android document-provider integration has supplied a created remote identity, accepted the complete write without error, and the app has passed its required provider-side verification and durable local confirmation bookkeeping.

The app and documentation must not describe this as proof of a lower-level Google backend synchronization guarantee that SAF does not expose.

If provider state after the write remains loading, stale, inconsistent, unavailable, or otherwise insufficient to satisfy the required confirmation boundary, the result remains `UNCERTAIN` rather than being upgraded to `UPLOADED` or downgraded to `FAILED`.

## Phase 7B reconciliation ladder

Remote reconciliation and retry remain owned by Phase 7B, not by this Phase 6B hardening implementation.

For an `UNCERTAIN` photo, Phase 7B should reconcile pessimistically in this order:

1. **Exact provisional identity first.** If `provisionalRemoteFileId` exists, resolve that exact document inside the persisted tree boundary before searching by name.
2. **Settled parent discovery fallback.** If no provisional identity exists or it cannot establish the result, query the exact stored work-order parent for the deterministic `field-photo-<photoUUID>.jpg` child using provider state authoritative enough for the decision.
3. **Identity/cardinality check.** Exactly one plausible deterministic-name match may be examined further. Multiple same-named matches remain ambiguous and must not be guessed between.
4. **Metadata check.** MIME type, name, and provider-reported byte size may support reconciliation. Unknown size or stale metadata cannot prove success or failure.
5. **Content proof when needed.** If a candidate remote file exists but metadata is inconclusive, Phase 7B may read the candidate through the approved provider and compare a SHA-256 digest with the exact prepared local JPEG. Hashing is an uncertainty-resolution tool, not a requirement to download every normal successful upload.
6. **Confirmed identical remote content.** If the exact candidate is proven to contain the prepared photo, persist its provider identity as confirmed `remoteFileId` and transition the photo to `UPLOADED` without creating another copy.
7. **Confirmed settled absence.** Only when authoritative-enough settled provider state proves that the expected remote object is absent may the record become `FAILED`/retryable.
8. **Existing but mismatched/partial/duplicate content.** Do not automatically classify this as safe failure and create another copy. Keep the record `UNCERTAIN` and require an explicitly designed repair/operator-resolution path. Automatic remote delete, overwrite, or duplicate creation is not authorized by this record.
9. **Still inconclusive.** Remain `UNCERTAIN`. Preservation and duplicate avoidance take priority over convenience.

A single empty child query is never sufficient evidence for safe retry.

## Cleanup remains outside Phase 6B hardening

This hardening does not delete either local image copy.

Future cleanup remains ordered as:

`provider-confirmed upload → durable local UPLOADED bookkeeping → local file pruning`

Filesystem deletion is not treated as transactionally atomic with queue metadata. If post-confirmation local deletion fails, the safe result is temporary storage leakage, not rollback of the confirmed remote upload and not deletion of remote evidence.

A later maintenance/garbage-collection mechanism may address confirmed orphaned temporary files, but that is outside this hardening scope.

## Ownership boundaries for implementation

The future runtime change must preserve current separation of responsibilities:

- `PendingPhotoRecord` / `PendingPhotoStore` own persisted queue truth, schema migration, immutable destination binding, provisional identity, confirmed identity, and state transitions.
- `DrivePhotoUploader` owns SAF provider destination resolution, remote create/write/read operations, and provider result classification. It must not silently choose a destination from current UI state.
- `PhotoUploadCoordinator` owns the sequencing boundary between provider results and durable queue transitions. The implementation may split the current monolithic create/write flow or use a narrow callback/result boundary, but the created remote identity must cross into durable queue storage before byte streaming continues.
- `PhotoCaptureActivity` owns user initiation and display only. It must not become the source of persisted destination identity or remote identity.

This plan does not require Room, SQLite, or another persistence framework. The existing lightweight app-private persistence strategy remains acceptable if it can provide the required atomic record replacement and migration behavior.

## Future implementation slices

To prevent this hardening from turning into a broad rewrite, runtime work should be performed and verified in narrow slices:

### 6B-H1 — Queue schema and provisional identity

Add the explicit schema migration and persisted nullable `provisionalRemoteFileId` semantics without changing Drive writes yet.

### 6B-H2 — Create/persist/write barrier

Refactor only the provider/coordinator boundary needed to persist the returned create identity before byte streaming. Preserve deterministic filename, destination identity, certainty classification, and existing UI scope.

### 6B-H3 — Focused automated verification

Prove at minimum:

- legacy/current queue records migrate with null provisional identity;
- returned created identity is durably stored before the writer is allowed to run;
- failure to persist the provisional identity after create stops writing and results in protected uncertainty;
- write interruption preserves provisional identity and exact destination;
- process-start recovery preserves provisional identity when changing stale `UPLOADING` to `UNCERTAIN`;
- deterministic pre-create `FAILED` records do not invent a provisional identity;
- confirmed success promotes the exact created identity to `remoteFileId` and `UPLOADED`;
- `UNCERTAIN` remains non-retryable;
- one photo's provisional/confirmed identity changes do not affect another photo;
- original and prepared local image data survive all Phase 6B hardening failure/uncertainty paths.

After focused coverage passes, run the complete automated repository suite once on the final runtime head. Existing pre-hardening CI evidence must remain labeled historical and must not be reused as proof of the changed runtime.

### 6B-H4 — Android Google Drive reality gate

On a physical Android device and dedicated safe Drive-backed test hierarchy, prove the affected path through the actual persisted SAF tree grant and Google Drive `DocumentsProvider` before Level 3 merge approval.

The gate must verify the exact created file lands under the exact stored work-order provider identity, provider identity is retained, no unrelated content changes, and the app does not claim stronger cloud synchronization certainty than the provider can establish.

If a safe real-provider interruption cannot be induced, document that limitation rather than adding a dangerous production fault-injection backdoor.

### Phase 7B — Remote reconciliation/retry/cleanup

Only after the hardened Phase 6B identity barrier exists should Phase 7B implement remote uncertainty reconciliation, safe retry release, and post-confirmation local cleanup.

## Explicit non-scope

This record does not authorize or require:

- changing the current `field-photo-<UUID>.jpg` naming prefix;
- adding Room or a relational database framework;
- Google Drive REST API or app-managed OAuth migration;
- automatic/background upload retry;
- remote deletion or overwrite of uncertain photos;
- folder creation/reuse/rename/delete changes;
- photo compression or orientation-policy changes;
- camera behavior changes;
- SHA-256 verification on every successful upload;
- local photo cleanup inside Phase 6B hardening;
- general-purpose Drive browsing or file management;
- workbook or Free Map Router integration.

## Risks and failure posture

The primary risk is duplicate creation after a remote side effect whose local confirmation was lost. The secondary risk is falsely reporting a partial or provider-stale object as uploaded. Both are handled by preserving unconfirmed local data, persisting provisional identity as early as safely possible, and keeping uncertainty non-retryable until reconciliation.

If evidence conflicts, the order of priority remains:

1. preserve the unconfirmed protected photo;
2. preserve its exact immutable work-order destination identity;
3. preserve known provisional/confirmed remote identity evidence;
4. avoid blind duplicate remote creation;
5. only then optimize convenience, cleanup, or speed.

## Rollback / baseline safety

Because this record changes documentation only, runtime rollback remains the existing Phase 6B pre-hardening runtime baseline.

Pre-hardening branch head:

`5c09771e097b2e5578a4528886654463754d2d29`

Exact previously verified Phase 6B runtime head recorded in the implementation record:

`e0f6e44c42e04443182ce10954766b734749e015`

No existing queue schema, local photo, Drive file, or app binary is changed by this documentation commit.

## Verification for this documentation change

Per the reconciled testing/change-control contracts, this documentation-only change requires:

- confirm all six governance files on this branch exactly match the intended `main` versions;
- inspect the documentation diff;
- confirm no `app/`, Gradle, manifest, workflow, or test source is changed;
- confirm this record does not claim that the hardening runtime has already been implemented or tested.

No runtime test, emulator run, APK build, or Drive smoke test is required for this documentation-only reconciliation.

## Approval status

The operator explicitly authorized documenting the Phase 6B hardening against `feat/phase-6b-drive-upload` and reconciling it with the newer governance on `main`, with **no app code yet**.

That authorization covers this documentation/governance change only.

The Phase 6B hardening runtime described here has **not** been implemented by this change. Its future Level 3 runtime branch/head will require focused tests, one final complete automated verification, the affected Android SAF/Google Drive reality gate, and explicit pre-merge approval under the governing contracts.
