# Field Photo Prep Testing Contract

This contract defines how Field Photo Prep changes are tested. Testing must match the risk of the change while protecting the app's highest-priority rule: original field photos remain unchanged.

## 1. Testing principles

1. Test the smallest relevant surface while developing.
2. Do not run broad test suites after every small edit when a focused test can prove the changed behavior.
3. After a focused test fails, fix that failure and rerun the same focused test before moving on.
4. Run the complete required automated verification once on the final runtime head before merge.
5. A required test failure stops commit, push, merge, APK release, or installation instructions for that change until the failure is resolved.
6. Documentation-only changes do not require Android runtime tests.
7. A passing test does not override a contract violation.

## 2. Protected test fixtures

Runtime tests involving photos must use disposable test fixtures or copies created specifically for testing.

Real field-job originals must not be used as destructive test targets.

Test fixtures should include, when practical:

- ordinary JPEG photos;
- representative Galaxy S21 field photos;
- photos with EXIF date/time metadata;
- photos with GPS metadata;
- photos without GPS metadata;
- duplicate filenames;
- portrait and landscape orientation;
- large photo batches;
- unsupported or unreadable files;
- low-storage or failed-output conditions where they can be simulated safely.

## 3. Original-photo preservation tests

Every runtime change that can touch photo reading, resizing, metadata, output storage, filenames, cleanup, or sharing must prove that source photos remain unchanged.

At minimum, the affected tests must verify that processing does not change the source photo's:

- file contents;
- filename;
- location;
- size;
- modification state when Android permits that to be checked;
- metadata.

A failed resize, failed metadata write, failed save, cancellation, or processing exception must leave the original unchanged.

Any regression that alters, moves, renames, replaces, or deletes an original photo is release-blocking.

## 4. Resize behavior tests

When resize behavior is affected, tests must verify:

1. the default resize selection is 60% unless the governing contract is changed;
2. output width and height are approximately the selected percentage of the original pixel dimensions, subject only to required integer rounding and orientation handling;
3. the resized copy is a new file;
4. the source image remains unchanged;
5. multiple selected photos can be processed as one batch;
6. one failed photo does not silently corrupt another photo's output;
7. successful and failed items are reported accurately to the operator;
8. on representative Galaxy S21 field photos, 60% output files are materially smaller than their source files while retaining visually acceptable field-photo detail.

The first usable release must record representative before-and-after file sizes during validation. No fixed megabyte target or compression ratio is required unless later evidence supports one.

## 5. Filename and overwrite tests

When output naming or storage behavior is affected, tests must verify:

1. the original filename is retained when possible;
2. an existing output filename is never overwritten silently;
3. a collision creates a unique new filename;
4. multiple collisions remain unique;
5. output naming never changes the source filename.

## 6. Metadata tests

When metadata handling is affected, tests must verify supported preservation of:

- original date/time metadata when available;
- GPS/location metadata when available;
- orientation information needed for correct visual display.

If some metadata cannot be copied, the resized image may still succeed, but the app must clearly report that metadata was not fully preserved.

The app must not invent GPS or date/time metadata that was not present in the source.

## 7. Android permission tests

When photo or storage permissions are affected, verify that:

1. the app requests only the narrow access needed for the approved workflow;
2. denying access fails safely and does not modify photos;
3. granting access does not give the app permission to alter unrelated user files beyond what the approved workflow requires;
4. permission changes are reviewed as the risk level required by `CHANGE_CONTROL_CONTRACT.md`.

## 8. Sharing tests

When Android sharing or Google Drive handoff behavior is affected, verify that:

1. sharing is operator-initiated;
2. only resized copies from the current batch, or another batch explicitly selected by the operator, are handed off;
3. older prepared photos are not silently mixed into a current share action;
4. originals are not substituted for resized copies;
5. cancelling the share action does not delete or modify originals or completed resized copies;
6. a share failure is reported without damaging local files;
7. a completed batch can be shared again without requiring the source photos to be resized again.

Direct Google Drive API integration is outside the initial release unless separately approved and contracted.

## 9. Batch and stability tests

Before the first usable release, verify realistic batches representative of field work.

The test plan must include at least:

- 1 photo;
- 10 photos;
- 30 photos;
- a larger batch chosen to expose memory or stability problems on the target Android device class.

Batch validation must also verify that:

- each processing run is identifiable separately from older prepared batches;
- starting a new batch does not silently add older prepared photos to it;
- cancelling after some photos finish keeps completed resized copies available;
- unprocessed originals remain unchanged after cancellation;
- a cancelled or incomplete batch is not reported as fully successful;
- a large batch does not cause source-photo loss or silent partial overwrites.

If device limits are reached, the app must stop safely and report the incomplete work.

## 10. Change-level verification

### Level 1

Documentation-only changes require:

- diff inspection;
- contract consistency review.

A Level 1 runtime or appearance-only change requires one focused check of the affected surface.

### Level 2

Level 2 runtime work requires:

- focused tests for the changed behavior during development;
- affected regression checks from `REGRESSION_CHECKLIST.md`;
- the complete automated test suite once on the final runtime head;
- Android/Kotlin build and static checks required by the project configuration;
- one affected-device smoke test before release.

### Level 3

Level 3 work requires all Level 2 checks plus:

- realistic disposable photo fixtures;
- explicit original-preservation verification;
- permission/storage impact verification;
- rollback verification;
- explicit operator approval before merge;
- post-installation validation on the target device before the release is considered complete.

## 11. Final runtime verification

Once the Android project exists, the exact final verification commands must be recorded in this contract or the project build documentation rather than guessed by an agent.

Until those commands are established, no agent may claim that the complete Android test suite or release build has passed.

The final runtime verification must cover, at minimum:

- automated unit tests;
- Android instrumentation or device tests where required by the changed behavior;
- successful debug/release build as applicable;
- static or lint checks adopted by the project;
- contract and regression checks.

## 12. Failure-stop rule

A failed required check stops the release path for that change.

Do not continue automatically to commit, push, merge, APK release, or installation after a required failure.

Fix the failure or abandon the change, then rerun the required affected verification.

No test shortcut is allowed when the shortcut could conceal original-photo damage.
