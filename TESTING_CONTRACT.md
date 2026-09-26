# Field Photo Prep Lean Testing Contract

> **Routing scope:** Universal test selection, verification timing, evidence reuse, and failure-stop behavior. Feature-specific regression boundaries live under `rules/testing/` and are loaded only when `RULE_INDEX.md` routes the current work to them.

## Purpose

Use the smallest evidence set that honestly proves the changed behavior while preserving the project's protected invariants.

Testing exists to prove behavior, not to create repeated ceremony.

## Development loop

- Documentation-only changes require diff/contract review only. They do not require runtime, device, or Drive tests.
- For runtime changes, run the smallest focused tests that directly cover the changed behavior while developing.
- After correcting a focused-test failure, rerun that focused test first.
- Do not run the complete suite after every small edit unless no meaningful focused boundary exists.
- Reuse exact valid evidence when relevant runtime behavior has not changed.

## Final runtime gate

Before merging a runtime change:

1. focused tests for the changed behavior must pass;
2. the complete repository suite must pass once on the exact final runtime head; and
3. only the smoke/reality checks required by the affected surface and risk level must pass.

A successful CI run on the exact final runtime head satisfies the complete-suite requirement. Do not duplicate it locally without a specific reason.

## Core test boundaries

Automated coverage should protect behavior/state boundaries rather than UI snapshots alone.

As applicable, prove:
- identity and stored provider/destination identity;
- capture-to-job binding;
- protected-original persistence;
- queue persistence and state transitions;
- non-destructive preparation;
- confirmed remote success handling;
- failed/unknown/uncertain remote handling;
- retry idempotency and destination preservation;
- duplicate-folder prevention;
- provider access/permission failure behavior;
- destructive-action guards.

The detailed boundaries for camera/preparation, upload/retry, and Drive/provider work are routed through the feature packs below.

## Feature rule packs

| Surface | Required testing pack |
| --- | --- |
| Camera / capture / flash / torch / zoom / orientation / preparation | `rules/testing/CAMERA_PREPARATION.md` |
| Queue / batch upload / retry / reconciliation / cleanup | `rules/testing/UPLOAD_QUEUE_RETRY.md` |
| Drive provider / workspace / company / address / work-order / freshness | `rules/testing/DRIVE_PROVIDER.md` |

A task may require more than one pack.

## Device and external evidence

Use automated tests for deterministic logic and state boundaries.

Use a physical device or real external provider only for claims that depend on that real environment.

Do not:
- substitute device checks for automated identity/state tests;
- represent mocks as real-provider proof;
- repeat already-proven physical observations merely for confidence;
- manufacture unsafe failures;
- use live customer work when a disposable fixture can prove the behavior.

The applicable rule pack and `INTEGRATION_CONTRACT.md` define the exact reality gate when Drive/provider behavior is affected.

## Failure gate

- A required test failure stops verification, merge, publication, and deployment for that change.
- Automation that tests and then commits, pushes, merges, publishes, or deploys must fail fast.
- A failing run may not be reported as passed or verified.
- Fix the failure, rerun the focused test, then run the final complete suite once when the branch is ready.

## Reporting

- Label focused runs as focused or targeted.
- Report complete-suite results only from an actual complete-suite run.
- Distinguish mocked/synthetic provider evidence from real provider evidence.
- Distinguish emulator evidence from physical-device evidence.
- Record a physical observation once when it proves the required behavior.
- Do not require unrelated tests or repeated complete suites merely as paperwork.

## Relationship to other rules

- `GOVERNANCE.md` owns work-control and stop/continue decisions.
- `CONTRACT.md` owns approved product/runtime behavior.
- `CHANGE_CONTROL_CONTRACT.md` owns risk classification, approval, and rollback.
- `REGRESSION_CHECKLIST.md` owns available workflow smoke checks.
- `INTEGRATION_CONTRACT.md` owns Android Google Drive/SAF behavior and reality-gate semantics.
- feature packs under `rules/testing/` own detailed regression requirements for their routed surfaces.
- this file owns universal test selection, timing, evidence reuse, and failure-stop behavior.
