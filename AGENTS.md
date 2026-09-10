# Field Photo Prep Agent Guardrails

This repository protects a field-photo workflow where the wrong destination, lost photo, duplicate folder, or destructive retry can create real work problems. Every human or automated agent must follow these guardrails before changing approved behavior.

## Mandatory contract read

Before changing runtime code, tests that define runtime behavior, Google Drive behavior, permissions, photo handling, storage, or upload behavior, read:

1. `CONTRACT.md`
2. `CHANGE_CONTROL_CONTRACT.md`
3. relevant sections of `REGRESSION_CHECKLIST.md`
4. `TESTING_CONTRACT.md`
5. `INTEGRATION_CONTRACT.md` when the change touches Google Drive, Android document-provider access, master-tree permissions, folder identity, upload, retry, or future external handoffs

Documentation-only edits must still read the document being changed and the change-class rules below.

## Mandatory master device-gate reread

For any physical Android device-gate work, phone testing, APK install/test transition, device-gate result classification, deviation from the staged device path, merge recommendation based on device evidence, or Phase 7B design that relies on H4 observations, read:

`docs/MASTER_DEVICE_REALITY_GATE_PLAN_2026-09-10.md`

Re-read that file at all of these checkpoints:

1. at the start of every physical-device test session;
2. immediately before installing or switching to the next APK/gate;
3. immediately before declaring a gate PASS, BLOCKED, or FAIL;
4. before deviating from the straight-line path because of an unexpected result; and
5. before recommending merge approval or starting Phase 7B design from device evidence.

Do not replace these rereads with memory, a chat summary, or repeated ad hoc safety prompts. The master plan exists to keep device work straight-line, proportional, and free of unnecessary verification loops while preserving genuine stop conditions.

## Choose the smallest honest change class

### Level 1 — low risk

Examples: documentation, comments, test wording, noninteractive copy, and appearance-only changes that cannot alter photo capture, photo storage, folder selection, upload destination, retry state, Drive writes, or permissions.

Use a branch, inspect the diff, and perform only the verification appropriate to the changed documentation or surface.

### Level 2 — normal feature or fix

Examples: normal screens and controls, camera UI, photo review UI, job-folder display, status labels, non-destructive local workflow behavior, and ordinary feature additions that do not change stored-data schemas, Drive permissions, or destination identity.

Record the problem, scope, owning files, protected behavior, focused tests, rollback point, and affected smoke check. Run focused tests during development, then the complete automated suite once on the final runtime head before merge.

### Level 3 — high risk

Examples: stored-data schema changes or migrations, deletion, automatic writes, document-provider/account-selection or persisted Drive-tree permission changes, folder-identity changes, upload/retry semantics, any change that could send photos to the wrong folder, and deployment changes.

Use a detailed impact record, realistic fixtures or a safe test environment, explicit rollback steps, focused tests, one final complete automated verification, affected smoke checks, and explicit operator approval before merge.

When uncertain between two levels, use the higher level.

## Authorization without repeated permission loops

- The user's request and approval authorize the documented scope.
- Do not ask for the same approval again when the scope has not changed.
- Ask again only when scope expands, assumptions prove false, or Level 3 pre-merge approval is required.
- Contract findings and adjacent defects are not automatic authorization to change them.

## Live-data protection

- Never use a real field job as an experiment surface when a safe test folder or fixture can prove the behavior.
- Never delete, move, rename, overwrite, or change permissions on an existing Drive file or folder unless that exact behavior is approved and tested.
- A failed upload must never be reported as successful.
- A retry must never silently create a duplicate photo or redirect it to a different job folder.
- A captured original must not be destroyed merely because compression, upload, document-provider access, or Drive availability fails.

## Ownership and narrow scope

- Change the module that owns the behavior.
- State which surfaces are read and which are written.
- Preserve unrelated camera, local-photo, queue, folder, Drive, and provider/account-selection behavior.
- Report adjacent defects separately.
- Do not turn a fix into cleanup, redesign, refactoring, renaming, relocation, or feature expansion without approval.

## Verification

`TESTING_CONTRACT.md` owns test selection, timing, reuse, and failure-stop behavior.

- During development, run only focused tests covering the changed behavior.
- After fixing a focused failure, rerun that focused test first.
- For runtime changes, run the complete suite once on the final runtime head before merge.
- Documentation-only changes require diff and contract review, not runtime tests.
- Any required test failure stops commit/push/merge/publication/deployment automation for that change.

Before calling a Drive-related change ready, satisfy the Android document-provider reality gate in `INTEGRATION_CONTRACT.md` using a safe test destination.
