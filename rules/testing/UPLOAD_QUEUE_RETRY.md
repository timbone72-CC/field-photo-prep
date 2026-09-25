# Testing Rule Pack — Upload Queue, Batch, Retry & Reconciliation

## Applies when

Read this file when work touches:
- queue state or restart recovery;
- upload selection/batching;
- retry eligibility or retry execution;
- `FAILED`, `UPLOADING`, `UNCERTAIN`, or confirmed-success handling;
- reconciliation;
- local cleanup after confirmed upload;
- remote-write sequencing or idempotency.

Use together with `TESTING_CONTRACT.md`, the queue/retry rules in `CONTRACT.md`, and the applicable upload/provider sections of `INTEGRATION_CONTRACT.md`.

## Selectable batch regression boundary

Focused automated coverage must prove:

- only upload-eligible prepared photos can enter a normal batch snapshot;
- duplicate selected photo IDs are rejected before any remote attempt;
- selected IDs are attempted in deterministic order and exactly once per batch run;
- the runner never overlaps two Drive attempts;
- every attempt delegates to the existing per-photo upload coordinator rather than creating a second remote-write implementation;
- each photo retains its immutable stored work-order destination;
- confirmed success may continue to the next selected photo;
- confirmed success with local cleanup still pending may continue while reporting that cleanup state accurately;
- a retry-safe failure may be kept locally while later selected photos continue;
- `UNCERTAIN`, still-`UPLOADING`, missing/unreadable queue state, or another unverified outcome stops the batch immediately;
- no later selected photo is attempted after that stop; and
- process interruption cannot convert UI batch selection into implicit retry authority.

UI selected counts and checkbox rendering are useful smoke surfaces, not substitutes for sequencing and remote-safety tests.

## Offline and retry coverage

Changes affecting queue or retry behavior must test at least:

- capture while no upload is possible;
- app/process restart with waiting work;
- retry to the original stored destination identity;
- one photo failing without corrupting other queue items;
- success persisted only after confirmed remote success;
- repeated retry not knowingly duplicating an already confirmed upload;
- a batch containing multiple photos where one known-safe failure does not corrupt the others; and
- a batch that stops on an uncertain/unverified active photo without starting any later selected photo.

## Physical Drive gate

Selectable batch upload is Level 3 Drive behavior when remote write/retry semantics change.

Its physical test must use the real Android/Google Drive DocumentsProvider path in a disposable safe fixture.

For a batch gate:
1. select a proper subset first;
2. prove exactly that subset is created under the correct stored work-order parent;
3. prove unselected photos remain local/unattempted;
4. then send the remaining subset;
5. verify no duplicate or wrong-parent files.

A passing emulator or mocked runner test is not enough to call the real Drive behavior proven.

Do not inject fake folder IDs and call that a completed Drive reality gate.

## Reporting

For batch testing, report:
- selected count;
- confirmed count;
- retry-safe failure count;
- whether the batch stopped early;
- whether later selected photos remained unattempted after a stop.

Distinguish mocked provider evidence from real Android/Google Drive provider evidence.
