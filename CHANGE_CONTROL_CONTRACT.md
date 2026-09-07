# Field Photo Prep Change Control Contract

## Purpose

This contract controls how changes are approved, implemented, tested, reviewed, merged, and rolled back. The process must match the actual risk: enough protection to prevent lost or misfiled photos without turning every small improvement into a large project.

## Change levels

### Level 1 — low risk

Typical examples:

- documentation and comments;
- test descriptions or fixtures that do not redefine approved behavior;
- noninteractive wording; and
- appearance-only changes that cannot alter capture, local storage, job identity, Drive destination, upload, retry, deletion, or permissions.

Required process:

- use a branch;
- record the changed surface and protected behavior;
- inspect the diff; and
- run only checks appropriate to the changed surface.

### Level 2 — normal feature or fix

Typical examples:

- ordinary screens and controls;
- camera preview or shutter interaction;
- job-list presentation;
- upload-status presentation;
- retry controls that do not change retry identity or storage semantics; and
- normal feature additions that do not alter stored-data schemas, Drive permissions, or destination identity.

Required process:

- use a branch and pull request;
- record the exact problem, approved scope, owning files, read/write surfaces, protected behavior, focused tests, rollback point, and affected smoke checks;
- change only the owning behavior and its tests;
- pass focused coverage during development; and
- pass the complete automated suite once on the final runtime head before merge.

The user's approved request is implementation authorization. Do not ask for the same approval again unless scope changes.

### Level 3 — high risk

Typical examples:

- local database or persisted-schema changes;
- migrations;
- deletion or cleanup automation;
- Google account or Drive scope changes;
- master-folder selection semantics;
- job-folder identity changes;
- upload destination construction;
- upload idempotency or retry semantics;
- changes that could lose an original photo or send a photo to the wrong folder; and
- deployment or signing changes.

Required process:

- use a dedicated branch and pull request;
- create the full impact record described below;
- use realistic fixtures and a safe Drive test folder/account context;
- identify exact rollback steps before implementation;
- pass focused and complete automated tests;
- pass affected data-preservation and Drive smoke checks; and
- obtain explicit operator approval before merge.

## Classification rules

- Choose the smallest level that honestly covers the risk.
- Importance alone does not make a change Level 3.
- If uncertain between two levels, use the higher level.
- If scope expands during implementation, reclassify before continuing the expanded work.
- Split unrelated work instead of hiding it inside a larger change.

## Authorization

- The user's request and approval authorize the documented scope.
- No duplicate approval is required for Level 1 or Level 2 work when scope remains unchanged.
- Level 3 requires explicit pre-merge approval.
- An audit finding or adjacent defect is not automatic implementation authorization.
- No intentional feature work is performed directly on `main`.

## Required change records

### Level 1 record

State:

- what is changing;
- why it is low risk;
- files expected to change;
- protected behavior; and
- the check that proves the changed surface remains valid.

### Level 2 record

State:

- exact user-facing problem;
- approved behavior;
- owning files/functions;
- read and write surfaces;
- protected behavior;
- focused tests;
- primary risks;
- rollback commit; and
- affected smoke checks.

### Level 3 impact record

In addition to the Level 2 record, state:

- required and optional data;
- schema, identity, permission, or authentication changes;
- master-folder and destination assumptions;
- duplicate/idempotency behavior;
- offline and stale-state behavior;
- safe Drive fixture plan;
- baseline and expected test results;
- failure recovery steps; and
- explicit pre-merge approval status.

Implementation stops when required structural assumptions remain unverified.

## Protected ownership boundaries

The exact code modules may evolve, but responsibilities must remain explicit:

- camera code owns capture and camera lifecycle;
- local storage owns protected originals and persisted queue/job state;
- photo preparation owns non-destructive resize/compression/orientation work;
- Drive integration owns authentication, folder creation, Drive IDs, upload, and confirmed remote results;
- UI code may request those actions and render state but must not duplicate their persistence or Drive logic.

A helper may not silently take ownership from another module.

## Diff control

- Inspect every changed file and function.
- Explain every changed block for Level 2 and Level 3 work.
- Remove unrelated changes.
- A display-only request may not alter photo destination, capture persistence, Drive writes, retry semantics, or authentication.
- A compression-quality request may not alter folder identity or deletion behavior.

## Verification matrix

### Documentation-only change

- contract/diff review only.

### Level 1 runtime change

- focused automated check where applicable;
- quick affected-surface smoke check.

### Level 2 change

- focused regression coverage;
- final complete automated suite once;
- affected workflow smoke checks from `REGRESSION_CHECKLIST.md`.

### Level 3 change

- focused and complete regression coverage;
- safe Drive fixture validation;
- all affected data-preservation checks;
- `INTEGRATION_CONTRACT.md` reality gate;
- explicit pre-merge approval; and
- post-publication/install verification when runtime delivery changed.

A required failure stops merge or publication for that change.

## Rollback

- Identify the prior working commit before merging runtime changes.
- Prefer reverting the narrow change over stacking additional guesses onto a broken branch.
- A rollback must not intentionally delete queued photos or protected originals.
- If a live version misroutes photos, stop further affected uploads until the known-good behavior is restored or the destination defect is understood.

## Maintenance rule

A documented imperfection may remain when changing it is riskier than its current impact. Working behavior is not rewritten merely because a cleaner implementation exists.
