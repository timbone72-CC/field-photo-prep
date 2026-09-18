# Phase 10C — Large-Batch Upload Observation Record

Date opened: 2026-09-17

Status: **OBSERVING — NO RUNTIME CHANGE AUTHORIZED YET**

Canonical baseline:
- `main` after Phase 10B;
- versionCode 26 / `0.21-photo-list-scale`;
- Phase 10B merge `110477c286bace05bb7fa56ee22199cf2de7fa92`.

## Purpose

Collect one fresh realistic large-batch upload result under the current canonical behavior before deciding whether any further upload/reconciliation change is justified.

This observation phase intentionally changes no Drive, queue, upload, retry, reconciliation, destination, or deletion behavior.

## Record for the next natural large batch

Capture:
- selected photo count;
- immediately confirmed uploads;
- retry-safe failures;
- UNCERTAIN results;
- reconciliation-confirmed results;
- unresolved items after safe reconciliation;
- whether manual recovery was materially disruptive.

## Decision rule

- If false UNCERTAIN is rare and bulk recovery is practical: make no runtime change.
- If false UNCERTAIN remains materially disruptive: review a narrow read-only pre-surface reconciliation refinement.
- PR #39 remains an input to that review, not an automatic merge target.

## PR #39 evidence retained for later review

PR #39 contains two ideas not present in 0.21:
1. treat provider `ContentResolver.refresh(...) == false` as a best-effort hint rather than a hard reconciliation blocker when two matching settled non-loading snapshots can still be proven;
2. allow exact id/name/MIME + SHA-256 content proof to override stale provider size metadata such as temporary `0`.

Those ideas must be re-evaluated against the canonical 0.21 line only if fresh field evidence justifies a Level 3 change.

## Protected rules

Do not weaken:
- no blind retry;
- no second remote create while remote state is unresolved;
- exact stored work-order destination;
- provisional remote identity barrier;
- strict exact-match reconciliation;
- sequential selected-batch Drive writes;
- fail-closed behavior when remote truth cannot be proven.

No synthetic ambiguous remote-create test is required merely to force an UNCERTAIN result.
