# Field Photo Prep Lean Testing Contract

## Purpose

This contract controls which tests run, when they run, and what happens after a failure. It keeps verification proportional to the change while protecting the photo and Drive workflows that matter most.

## Development loop

- Documentation-only changes require diff and contract review only. They do not require runtime tests, device tests, or Drive smoke tests.
- For runtime changes, run the smallest focused test or tests that directly cover the changed behavior while developing.
- After correcting a focused-test failure, rerun that focused test first.
- Do not run the complete suite after every small edit unless the change is broad enough that no meaningful focused boundary exists.

## Final runtime gate

Before merging a runtime change:

1. focused tests for the changed behavior must pass;
2. the complete repository suite must pass once on the final runtime head; and
3. only the smoke checks for the affected surface and risk level must be completed.

A successful CI run on the exact final runtime head satisfies the final complete-suite requirement. Do not duplicate it locally without a specific reason.

## Required test boundaries

As features are implemented, automated coverage should be organized around these behavior boundaries rather than UI snapshots alone:

- job identity and stored Drive folder ID;
- folder creation request and returned Drive folder ID;
- capture-to-job binding;
- protected-original persistence;
- prepared-copy generation;
- queue persistence across app restart;
- upload state transitions;
- confirmed Drive success handling;
- failed/unknown upload handling;
- retry idempotency and destination preservation;
- duplicate-folder prevention;
- authentication/authorization failure behavior; and
- destructive-action guards.

## Realistic Drive tests

Drive-related unit tests may mock API responses while developing, but a Level 3 Drive change is not considered fully verified from mocks alone.

Before merge, use the safe reality gate in `INTEGRATION_CONTRACT.md` to prove that the actual app path can create or use the intended test folder, upload a prepared photo to the exact stored folder ID, receive confirmation, and leave unrelated Drive content unchanged.

Never use a live customer/job folder when a dedicated test folder can prove the behavior.

## Offline and retry coverage

Changes affecting queue or retry behavior must test at least:

- capture while no upload is possible;
- app/process restart with waiting work;
- retry to the original destination;
- one photo failing without corrupting other queue items;
- success being persisted only after confirmed remote success; and
- repeated retry not knowingly duplicating an already confirmed upload.

## Failure gate

- A required test failure stops verification, merge, publication, and deployment for that change.
- Any automated script that tests and then commits, pushes, merges, publishes, or deploys must fail fast.
- A failing run may not be reported as passed or verified.
- Fix the failure, rerun the focused test, then run the final complete suite once when the branch is ready.

## Device testing

Camera behavior cannot be proven entirely by JVM/unit tests. When capture behavior changes, perform the smallest affected device or emulator check that can prove preview/capture behavior, orientation, persistence, and return to the job workflow.

Device checks must not substitute for automated identity, queue, and Drive-boundary tests.

## Reporting

- Label focused runs as focused or targeted.
- Report full-suite counts only from an actual complete-suite run.
- Distinguish mocked Drive tests from real safe-folder integration checks.
- Do not require unrelated tests or repeated complete suites merely as paperwork.

## Relationship to other contracts

- `CONTRACT.md` owns approved behavior.
- `CHANGE_CONTROL_CONTRACT.md` owns change classification, approval, and rollback.
- `REGRESSION_CHECKLIST.md` owns available workflow smoke checks.
- `INTEGRATION_CONTRACT.md` owns the Google Drive reality gate.
- This contract owns test selection, timing, reuse of valid results, and failure-stop behavior.
