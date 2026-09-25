# Field Photo Prep Lean Testing Contract

> **Routing scope:** Test selection, verification timing, failure-stop behavior, and feature-specific regression boundaries. Runtime work must use the applicable portions selected by `RULE_INDEX.md`; unrelated feature sections are reference material, not mandatory reading.

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

- workspace/company/job identity and stored remote/provider folder identity;
- company/address/work-order folder creation or rename request and returned/preserved provider folder identity;
- capture-to-job binding;
- repeated multi-shot capture with a unique protected record/file per shutter press;
- camera flash-mode state/cycling independent of photo identity and persistence;
- camera zoom range/clamp behavior and effective lens-ratio math independent of photo identity and persistence;
- protected-original persistence;
- capture callback failure with non-empty image preservation;
- automatic prepared-copy generation after durable waiting state;
- serialized preparation ownership so automatic/manual preparation cannot race;
- restart recovery for waiting photos missing prepared copies;
- prepared-copy generation;
- queue persistence across app restart;
- upload state transitions;
- selectable batch membership validation and deduplication;
- deterministic one-at-a-time batch attempt ordering;
- confirmed batch continuation and retry-safe failure continuation;
- immediate batch stop on UNCERTAIN/unverified remote result;
- preservation of later unattempted batch items;
- confirmed remote success handling;
- failed/unknown upload handling;
- retry idempotency and destination preservation;
- duplicate-folder prevention at company, address, and work-order levels;
- document-provider access/permission failure behavior; and
- destructive-action guards.

## Camera and preparation regression boundary

For the current Android in-app camera workflow, focused coverage should preserve these behaviors when the camera or preparation path changes:

- one camera session may save multiple photos before **Done**;
- each shot receives a fresh protected identity and cannot overwrite another shot;
- all shots retain the same immutable selected work-order destination for that session;
- an unused empty reservation may be cleaned up, but non-empty captured data is preserved;
- Flash mode cycles **Auto → On → Off → Auto**, and a fresh camera session defaults to **Auto**;
- Torch is a separate continuous-light control, defaults **Off** on a fresh session, and must not remain intentionally enabled after leaving the camera;
- devices without a usable flash unit disable flash/torch controls instead of breaking ordinary capture;
- lighting-state changes must not mutate capture identity, queue state, destination identity, preparation state, or Drive state;
- zoom slider and pinch requests remain clamped to the active camera's reported range;
- **1×** means the normal default rear-camera framing rather than merely minimum linear zoom;
- an ultra-wide shortcut is exposed only from a real CameraX-reported sub-1× rear-camera path and must fail back to ordinary capture if that path cannot bind safely;
- effective zoom readout for a physical wide camera accounts for that camera's intrinsic zoom ratio;
- portrait/landscape reflow and target rotation must not mutate capture identity or destination identity;
- zoom/lens controls cannot race an active shutter write;
- finishing one shot places it into durable waiting state before background preparation is notified;
- automatic preparation failure cannot invalidate a durable capture;
- only one full-image preparation operation owns the process-wide preparation gate at a time; and
- restart recovery can requeue eligible waiting photos whose prepared derivative is missing.

These are state/identity boundaries first. UI-only tests are not a substitute for them.

## Selectable batch upload regression boundary

For any change to selectable batch upload, focused automated coverage must prove at least:

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

The UI selected count and checkbox rendering are useful smoke surfaces, but they are not substitutes for these sequencing and remote-safety tests.

## Realistic Drive tests

Drive-related unit tests may mock provider/API responses while developing, but a Level 3 Drive change is not considered fully verified from mocks alone.

For the current Android implementation, use the affected parts of the safe SAF/DocumentsProvider reality gate in `INTEGRATION_CONTRACT.md`. The real path must prove the operator-selected workspace tree, actual company/address/work-order provider identities, the changed company create/rename/switch or address/work-order create/reuse/upload operation, returned or preserved remote identity where applicable, and preservation of unrelated Drive content.

For selectable batch upload, the safe Drive gate must use disposable prepared photos only. Select a proper subset first, prove exactly that subset is created under the correct stored work-order parent while the unselected photos remain local/unattempted, then send the remaining subset and verify no duplicates or wrong-parent files.

Do not inject fake folder IDs and call that a completed Drive reality gate. Synthetic IDs remain useful unit coverage only.

Never use a live customer/job folder when a dedicated test folder can prove the behavior.

## Multi-company regression boundary

For any workspace/company change, focused coverage must prove at least:

- legacy single-company preferences remain readable until explicit workspace migration;
- selecting a workspace does not rewrite queued photo destination IDs;
- an exact legacy company provider ID may be restored only by ID when it is a direct workspace child;
- company switching clears current address/work-order navigation state but preserves queued-photo records;
- company create reuses one exact match, requires operator choice for duplicates, and creates exactly one folder only after authoritative-enough absence;
- company rename preserves provider identity and is blocked on an exact sibling-name collision;
- address discovery/creation uses the exact selected company provider ID, never the workspace root or a sibling company;
- restart restores the workspace and selected company without inventing a new folder; and
- upload/reconciliation continues to use each photo's immutable stored work-order provider ID regardless of later company switching.

These tests may use fake providers for focused development, but the broader-tree Android provider behavior still requires the safe real-device Drive gate before merge approval.

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
- success being persisted only after confirmed remote success;
- repeated retry not knowingly duplicating an already confirmed upload;
- a batch containing multiple photos where one known-safe failure does not corrupt the others; and
- a batch that stops on an uncertain/unverified active photo without starting any later selected photo.

## Failure gate

- A required test failure stops verification, merge, publication, and deployment for that change.
- Any automated script that tests and then commits, pushes, merges, publishes, or deploys must fail fast.
- A failing run may not be reported as passed or verified.
- Fix the failure, rerun the focused test, then run the final complete suite once when the branch is ready.

## Device and emulator testing

Camera behavior cannot be proven entirely by JVM/unit tests. When capture behavior changes, perform the smallest affected device or emulator check that can prove the changed surface.

Synthetic bitmap/EXIF transformation can be proven with Android emulator instrumentation. Actual field-camera capture, device-specific camera behavior, repeated in-app shutter use, control placement around system navigation, and final visual/orientation confidence require a physical supported Android device before those behaviors are called field-proven.

For camera-lighting changes, the physical-device gate must confirm the real device exposes the controls correctly: fresh session starts at **Flash Auto / Torch Off**, Flash Auto/On/Off can be selected, Torch can be turned On and Off, and repeated capture still works without lighting controls racing the shutter. A device without flash capability may satisfy the alternative path by proving the controls fail closed/disabled without breaking capture.

For camera zoom/orientation/lens changes, the physical-device gate must confirm normal **1×** framing, slider and pinch behavior, correct portrait/landscape reflow and saved orientation, and any quick preset actually exposed by the device. A wide preset counts as proven only when the real phone visibly produces wider-than-1× framing through the app; if the device does not expose ultra-wide through the current CameraX path, record that capability boundary instead of inventing a pass. Existing protected multi-shot, Flash/Torch, and **Done** behavior should be checked only once as part of the same combined camera gate.

When automatic preparation is changed, automated instrumentation should prove the real Android bitmap/EXIF path and protected-original preservation. The physical-device gate should then be limited to behavior automation cannot establish honestly, such as camera responsiveness while preparation runs and the operator-visible no-extra-tap workflow.

Selectable batch upload is a Level 3 Drive behavior. Its physical test must use the real Android/Google Drive document-provider path in a disposable safe fixture. A passing emulator or mocked runner test is not enough to claim the batch feature Drive-proven.

Device checks must not substitute for automated identity, queue, photo-preservation, and Drive-boundary tests.

## Reporting

- Label focused runs as focused or targeted.
- Report full-suite counts only from an actual complete-suite run.
- Distinguish mocked/synthetic provider tests from real safe-folder Android document-provider checks.
- Distinguish emulator image-transformation evidence from physical-camera evidence.
- Record a physical-device observation once when it proves the required behavior; do not repeat it merely for confidence.
- For batch testing, report selected count, confirmed count, retry-safe failures, whether the batch stopped early, and whether any later selected photo remained unattempted after a stop.
- Do not require unrelated tests or repeated complete suites merely as paperwork.

## Relationship to other contracts

- `CONTRACT.md` owns approved behavior and platform-neutral identity rules.
- `CHANGE_CONTROL_CONTRACT.md` owns change classification, approval, and rollback.
- `REGRESSION_CHECKLIST.md` owns available workflow smoke checks.
- `INTEGRATION_CONTRACT.md` owns the current Android Google Drive/SAF reality gate, including batch-upload provider checks.
- This contract owns test selection, timing, reuse of valid results, and failure-stop behavior.
