# Upload Reliability + Reconciliation UI Repair — 2026-09-16

## Change class

Level 3 for upload confirmation/retry behavior, plus a Level 2 Photos-screen layout fix.

## Baseline and rollback

- Governed base: `main` at `80847b2c270d023daaf5ccda3a5313f3883162aa`.
- Repair branch: `fix/upload-reliability-reconcile-ui-20260916`.
- Rollback point: revert this repair branch/PR back to the governed base above. Rollback must not delete queued photos or protected originals.

## Exact user-facing problems

1. Too many otherwise normal uploads are ending in `UNCERTAIN` and requiring manual reconciliation.
2. The selected-photo reconciliation card sits outside the photo-list ScrollView and consumes most of the remaining list height. After reconciliation the operator has to leave and reopen Photos to comfortably reach the next photo.

## Approved behavior

- Preserve the existing fail-closed duplicate protection and exact stored work-order destination identity.
- Do not infer upload success from local state alone.
- After all prepared bytes are written to the already-created remote document, allow a bounded provider-settle window before classifying remote verification as uncertain. Temporary readback delay, zero-size metadata, or stale byte-size metadata during that bounded window must not immediately force operator reconciliation.
- A hard identity/name/MIME mismatch remains immediately uncertain.
- If verification still cannot be proven after the bounded settle window, the existing `UNCERTAIN` path remains unchanged and blind retry remains blocked.
- Keep the selected-photo/reconciliation card inside the same ScrollView as the photo rows so it no longer permanently steals list viewport height. No queue, Drive, destination, or deletion behavior is changed by the layout move.

## Owning files/functions

- `DrivePhotoUploader.writeAndVerify(...)`: remote post-write verification and uncertainty boundary.
- `DrivePhotoUploader.AndroidProviderOps.readDocument(...)`: provider read used by upload verification.
- `app/src/main/res/layout/screen_photos.xml`: selected-photo card/list layout only.
- Focused unit coverage for bounded verification retry.

## Read/write surfaces

Reads:
- immutable queued photo ID and stored work-order provider ID;
- prepared JPEG length;
- created remote provider document identity/name/MIME/size.

Writes:
- no new Drive operation type is added;
- the same existing create/write path remains authoritative;
- only the timing of the existing post-write read verification changes;
- UI layout changes write no queue or Drive state.

## Protected behavior

- No upload destination guessing.
- No second remote create after an ambiguous result.
- No loss or overwrite of the protected original before confirmed Drive success.
- Persisted provisional remote identity remains the write barrier.
- Confirmed uploaded state still requires provider evidence.
- Batch upload still stops on an unresolved `UNCERTAIN` result.
- Reconciliation remains read-only and cannot create, delete, rename, move, or redirect Drive content.

## Verification policy

The repair adds a bounded verification settle window after the prepared byte write:

- retry provider readback a limited number of times;
- transient read failure may settle;
- zero or temporarily wrong remote size may settle;
- exact provider identity/name/MIME must always match;
- unknown size (`-1`) remains acceptable as before when identity/name/MIME are exact;
- after the final attempt, unresolved readback/metadata remains `UNCERTAIN`.

This does **not** weaken the fail-closed rule. It delays the uncertainty classification long enough for a cloud-backed Android DocumentsProvider to publish the file state it just accepted.

## Focused tests

- transient post-write provider read failure eventually verifies and returns success;
- transient zero/wrong size eventually verifies and returns success;
- persistent post-write read failure still returns `remoteStateUncertain=true`;
- hard identity/name/MIME mismatch remains uncertain and does not become confirmed;
- existing upload destination, provisional-ID, short-write, changed-prepared-file and duplicate-protection tests remain passing.

## Affected smoke checks

From `REGRESSION_CHECKLIST.md`:

- H. Upload destination
- I. Upload status and local cleanup
- J. Offline and retry
- K. Unconfirmed-photo protection
- M. Minimal field workflow, limited to disposable test photos and the affected upload/reconciliation path

## Safe Drive fixture / real-device gate

Use a disposable address/work-order folder under the approved test master Drive tree, never a live customer folder.

1. Capture/prepare several disposable photos.
2. Upload a selected subset through the real Android Google Drive provider.
3. Confirm each successful photo lands under its stored work-order provider ID with exactly one remote file.
4. Confirm ordinary successful uploads no longer require reconciliation merely because immediate readback is delayed.
5. Restart/interrupt one disposable upload if a safe natural test can be produced; confirm the protected local photo and exact destination survive and no blind duplicate retry occurs.
6. Reconcile any naturally uncertain test photo and confirm no second remote file is created.
7. Confirm the Photos screen can scroll between the selected reconciliation card and remaining photo rows without leaving/reopening the screen.

A naturally ambiguous remote create must not be manufactured solely to force an `UNCERTAIN` result.

## Failure recovery

- Any automated test failure stops merge/publication.
- Any wrong-parent file, duplicate remote file, lost local original, or false confirmed-success result stops the device gate immediately.
- Preserve all local queue evidence and remote test content for inspection; do not retry an unresolved uncertain item blindly.

## Approval status

The operator explicitly requested: "Fix it" for both the excessive reconciliation problem and the reconciliation screen obstruction. Implementation is authorized within this documented scope. Final Level 3 readiness still requires the affected real Android/Drive reality gate before calling the change field-proven.
