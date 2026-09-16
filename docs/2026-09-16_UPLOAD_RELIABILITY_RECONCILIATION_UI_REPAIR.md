# Upload Reliability + Reconciliation UI Repair — 2026-09-16

## Change class

Level 3 for upload confirmation/retry and bulk reconciliation behavior, plus a Level 2 Photos-screen layout/status fix.

## Baseline and rollback

- Governed base: `main` at `80847b2c270d023daaf5ccda3a5313f3883162aa`.
- Repair branch: `fix/upload-reliability-reconcile-ui-20260916`.
- Rollback point: revert this repair branch/PR back to the governed base above. Rollback must not delete queued photos or protected originals.

## Exact user-facing problems

1. Too many otherwise normal uploads are ending in `UNCERTAIN` and requiring manual reconciliation.
2. The selected-photo reconciliation card sits outside the photo-list ScrollView and consumes most of the remaining list height. After reconciliation the operator has to leave and reopen Photos to comfortably reach the next photo.
3. A backlog of many UNCERTAIN records currently requires one reconciliation tap per photo even though each check is read-only and independently bound to that photo's stored destination.
4. The Photos screen hides the stored uncertainty reason, making a large backlog difficult to diagnose and making it harder to preserve accurate capture-to-remote identity for later ordering.

## Approved behavior

- Preserve the existing fail-closed duplicate protection and exact stored work-order destination identity.
- Do not infer upload success from local state, Drive upload timestamp, visible folder name, or list order.
- After all prepared bytes are written to the already-created remote document, allow a bounded provider-settle window before classifying remote verification as uncertain. Temporary readback delay, zero-size metadata, or stale byte-size metadata during that bounded window must not immediately force operator reconciliation.
- A hard identity/name/MIME mismatch remains immediately uncertain.
- If verification still cannot be proven after the bounded settle window, the existing `UNCERTAIN` path remains unchanged and blind retry remains blocked.
- Keep the selected-photo/reconciliation card inside the same ScrollView as the photo rows so it no longer permanently steals list viewport height.
- Show the persisted uncertainty detail for an UNCERTAIN photo instead of only a generic label.
- Add one operator action to reconcile all currently persisted UNCERTAIN records sequentially. Every item must delegate to the existing strict per-photo reconciliation path; bulk orchestration may not create a second proof algorithm.
- Bulk reconciliation may confirm an exact remote match, release a photo as retry-safe only after confirmed settled absence, or leave it UNCERTAIN. One unresolved item does not authorize a retry and does not prevent independent read-only reconciliation of later items.
- A bulk run must not create, upload, delete, rename, move, or change permissions on any Drive item.
- Preserve each record's immutable photo ID, capture-created timestamp, work-order provider ID, provisional remote ID when present, and confirmed remote ID when proven. This retained mapping is the authoritative basis for any later capture-order organization; Drive upload timestamps are not.

## Owning files/functions

- `DrivePhotoUploader.writeAndVerify(...)`: remote post-write verification and uncertainty boundary.
- `DrivePhotoUploader.AndroidProviderOps.readDocument(...)`: provider read used by upload verification.
- `DrivePhotoReconciler.reconcile(...)`: existing strict read-only proof for one UNCERTAIN record.
- `PhotoUploadCoordinator.reconcileUncertain(...)`: existing local queue transition after one reconciliation result.
- `PhotoReconciliationBatchRunner`: sequential orchestration/result accounting only; no Drive implementation.
- `PhotoCaptureActivity`: bulk-reconcile UI ownership, progress, summary, and display of stored uncertainty detail.
- `app/src/main/res/layout/screen_photos.xml`: reconciliation controls/layout.

## Read/write surfaces

Reads:
- immutable queued photo ID and capture-created timestamp;
- stored address/work-order provider IDs;
- persisted provisional remote file ID when present;
- persisted uncertainty detail;
- prepared JPEG bytes/length/hash;
- exact remote provider document identity/name/MIME/size/content;
- settled contents of the stored work-order parent when exact-document proof is unavailable.

Writes:
- no new Drive write operation is added;
- the same existing upload create/write path remains authoritative;
- the same existing reconciliation path may update local queue state from `UNCERTAIN` to confirmed `UPLOADED` or retry-safe `FAILED` only after its existing provider proof succeeds;
- bulk orchestration writes only those existing per-photo local reconciliation results and confirmed-upload local cleanup;
- UI layout/status changes write no Drive state.

## Protected behavior

- No upload destination guessing.
- No ordering guess from Drive upload timestamps.
- No second remote create after an ambiguous result.
- No loss or overwrite of the protected original before confirmed Drive success.
- Persisted provisional remote identity remains the write barrier.
- Confirmed uploaded state still requires provider evidence.
- Batch upload still stops on an unresolved `UNCERTAIN` result.
- Reconciliation remains read-only with respect to Drive and cannot create, upload, delete, rename, move, redirect, or change sharing of Drive content.
- Bulk reconciliation processes one record at a time and delegates to the existing per-photo coordinator/reconciler.
- A result that remains ambiguous stays `UNCERTAIN`; it is never converted to success or retry-safe state merely to reduce the backlog count.
- Local confirmed-photo cleanup may occur only after exact remote success has already been committed, and lightweight metadata including capture timestamp and confirmed remote ID remains available.

## Verification policy

The upload repair adds a bounded verification settle window after the prepared byte write:

- retry provider readback a limited number of times;
- transient read failure may settle;
- zero or temporarily wrong remote size may settle;
- exact provider identity/name/MIME must always match;
- unknown size (`-1`) remains acceptable as before when identity/name/MIME are exact;
- after the final attempt, unresolved readback/metadata remains `UNCERTAIN`.

Bulk reconciliation adds only sequential orchestration around the existing strict proof:

- snapshot persisted UNCERTAIN IDs before starting;
- reject duplicate/blank IDs in the orchestration snapshot;
- attempt each snapshot ID at most once in that run;
- run only one reconciliation at a time;
- continue to later independent records after confirmed, retry-safe-absent, or remain-uncertain results because reconciliation is remote read-only;
- summarize checked, confirmed, retry-safe, still-uncertain/error, and local-cleanup-pending counts;
- never turn an exception or ambiguous result into success.

This does **not** weaken the fail-closed rule. It removes repetitive operator taps while retaining the same proof requirement for every photo.

## Focused tests

Upload verification:
- transient post-write provider read failure eventually verifies and returns success;
- transient zero/wrong size eventually verifies and returns success;
- persistent post-write read failure still returns `remoteStateUncertain=true`;
- hard identity/name/MIME mismatch remains uncertain and does not become confirmed;
- existing upload destination, provisional-ID, short-write, changed-prepared-file and duplicate-protection tests remain passing.

Bulk reconciliation:
- IDs are reconciled in deterministic snapshot order and at most once;
- duplicate IDs fail before any reconciliation attempt;
- confirmed/retry-safe/still-uncertain results are counted accurately;
- an unresolved item does not become retry authority and later independent read-only checks may continue;
- the UI exposes one bulk action only when UNCERTAIN records exist and shows stored per-photo uncertainty detail;
- existing strict `DrivePhotoReconciler` identity/name/MIME/size/hash and settled-parent tests remain passing.

## Affected smoke checks

From `REGRESSION_CHECKLIST.md`:

- H. Upload destination
- I. Upload status and local cleanup
- J. Offline and retry
- K. Unconfirmed-photo protection
- M. Minimal field workflow, limited to disposable test photos and the affected upload/reconciliation path

## Safe Drive fixture / real-device gate

Use a disposable address/work-order folder under the approved test master Drive tree, never a live customer folder as an experiment surface.

1. Capture/prepare several disposable photos.
2. Upload a selected subset through the real Android Google Drive provider.
3. Confirm each successful photo lands under its stored work-order provider ID with exactly one remote file.
4. Confirm ordinary successful uploads no longer require reconciliation merely because immediate readback is delayed.
5. Restart/interrupt one disposable upload only if a safe natural test can be produced; confirm the protected local photo and exact destination survive and no blind duplicate retry occurs.
6. Reconcile any naturally uncertain test photo and confirm no second remote file is created.
7. If two or more naturally uncertain disposable records are available, run the bulk control and confirm each uses its own stored work-order identity, runs sequentially, and creates no Drive writes. If a multi-item uncertain condition cannot be produced safely, use focused automated bulk tests plus one natural real-provider reconciliation and record that limitation.
8. Confirm the Photos screen can scroll between the selected reconciliation card and remaining photo rows without leaving/reopening the screen.
9. Confirm stored capture-created time and confirmed remote identity remain readable after reconciliation/confirmed local-image cleanup.

A naturally ambiguous remote create must not be manufactured solely to force an `UNCERTAIN` result.

## Failure recovery

- Any automated test failure stops merge/publication.
- Any wrong-parent file, duplicate remote file, lost local original, false confirmed-success result, or reconciliation-triggered Drive write stops the device gate immediately.
- Preserve all local queue evidence and remote test content for inspection; do not retry an unresolved uncertain item blindly.
- Bulk reconciliation may be interrupted without granting retry authority to items that were not individually proven.

## Approval status

The operator explicitly requested the original upload/reconciliation repair and later requested that the many UNCERTAIN photos be resolved without one-by-one reconciliation, specifically to preserve accurate capture order rather than guess from Drive timestamps. Implementation of sequential bulk reconciliation within the existing strict proof boundary is authorized on this repair branch. Final Level 3 readiness still requires the affected automated and real Android/Drive reality gates before merge approval.
