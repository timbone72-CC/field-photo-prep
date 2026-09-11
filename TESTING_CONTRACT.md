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

- job identity and stored remote/provider folder identity;
- folder creation request and returned provider folder identity;
- capture-to-job binding;
- repeated multi-shot capture with a unique protected record/file per shutter press;
- protected-original persistence;
- capture callback failure with non-empty image preservation;
- automatic prepared-copy generation after durable waiting state;
- serialized preparation ownership so automatic/manual preparation cannot race;
- restart recovery for waiting photos missing prepared copies;
- prepared-copy generation;
- queue persistence across app restart;
- upload state transitions;
- confirmed remote success handling;
- failed/unknown upload handling;
- retry idempotency and destination preservation;
- duplicate-folder prevention;
- document-provider access/permission failure behavior; and
- destructive-action guards.

## Camera and preparation regression boundary

For the current Android in-app camera workflow, focused coverage should preserve these behaviors when the camera or preparation path changes:

- one camera session may save multiple photos before **Done**;
- each shot receives a fresh protected identity and cannot overwrite another shot;
- all shots retain the same immutable selected work-order destination for that session;
- an unused empty reservation may be cleaned up, but non-empty captured data is preserved;
- finishing one shot places it into durable waiting state before background preparation is notified;
- automatic preparation failure cannot invalidate a durable capture;
- only one full-image preparation operation owns the process-wide preparation gate at a time; and
- restart recovery can requeue eligible waiting photos whose prepared derivative is missing.

These are state/identity boundaries first. UI-only tests are not a substitute for them.

## Realistic Drive tests

Drive-related unit tests may mock provider/API responses while developing, but a Level 3 Drive change is not considered fully verified from mocks alone.

For the current Android implementation, use the affected parts of the safe SAF/DocumentsProvider reality gate in `INTEGRATION_CONTRACT.md`. The real path must prove the operator-selected master tree, actual address/work-order provider identities, the changed create/reuse/upload operation, returned remote identity where applicable, and preservation of unrelated Drive content.

Do not inject fake folder IDs and call that a completed Drive reality gate. Synthetic IDs remain useful unit coverage only.

Never use a live customer/job folder when a dedicated test folder can prove the behavior.

## Provider freshness tests

When a change relies on cloud-backed folder absence or emptiness before a write:

- automated tests must cover loading/stale/inconsistent provider state where practical;
- the implementation must fail closed when state is not authoritative enough for the decision;
- a real-device Drive gate must exercise the actual Android provider path for Level 3 folder create/reuse/deletion behavior;
- do not represent emulator or mocked provider state as proof of Google Drive provider freshness.

## Offline and retry coverage

Changes affecting queue or retry behavior must test at least:

- capture while no upload is possible;
- app/process restart with waiting work;
- retry to the original stored destination identity;
- one photo failing without corrupting other queue items;
- success being persisted only after confirmed remote success; and
- repeated retry not knowingly duplicating an already confirmed upload.

## Failure gate

- A required test failure stops verification, merge, publication, and deployment for that change.
- Any automated script that tests and then commits, pushes, merges, publishes, or deploys must fail fast.
- A failing run may not be reported as passed or verified.
- Fix the failure, rerun the focused test, then run the final complete suite once when the branch is ready.

## Device and emulator testing

Camera behavior cannot be proven entirely by JVM/unit tests. When capture behavior changes, perform the smallest affected device or emulator check that can prove the changed surface.

Synthetic bitmap/EXIF transformation can be proven with Android emulator instrumentation. Actual field-camera capture, device-specific camera behavior, repeated in-app shutter use, control placement around system navigation, and final visual/orientation confidence require a physical supported Android device before those behaviors are called field-proven.

When automatic preparation is changed, automated instrumentation should prove the real Android bitmap/EXIF path and protected-original preservation. The physical-device gate should then be limited to behavior automation cannot establish honestly, such as camera responsiveness while preparation runs and the operator-visible no-extra-tap workflow.

Device checks must not substitute for automated identity, queue, photo-preservation, and Drive-boundary tests.

## Reporting

- Label focused runs as focused or targeted.
- Report full-suite counts only from an actual complete-suite run.
- Distinguish mocked/synthetic provider tests from real safe-folder Android document-provider checks.
- Distinguish emulator image-transformation evidence from physical-camera evidence.
- Record a physical-device observation once when it proves the required behavior; do not repeat it merely for confidence.
- Do not require unrelated tests or repeated complete suites merely as paperwork.

## Relationship to other contracts

- `CONTRACT.md` owns approved behavior and platform-neutral identity rules.
- `CHANGE_CONTROL_CONTRACT.md` owns change classification, approval, and rollback.
- `REGRESSION_CHECKLIST.md` owns available workflow smoke checks.
- `INTEGRATION_CONTRACT.md` owns the current Android Google Drive/SAF reality gate.
- This contract owns test selection, timing, reuse of valid results, and failure-stop behavior.
