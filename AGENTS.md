# Field Photo Prep — Agent Entry Point

This is the mandatory starting point for human or automated work in this repository.

Do **not** read every contract by default. Route the work first.

## Mandatory first read

Before planning, branching, changing code/configuration, or changing a live project system, read:

1. `GOVERNANCE.md`
2. `PROJECT_PROFILE.md`
3. `RULE_INDEX.md`

Then load only the detailed rule packs selected by `RULE_INDEX.md`.

## Before creating or replacing a branch

Perform the takeover preflight required by `GOVERNANCE.md`:
- inspect `main`;
- inspect open PRs/relevant active branches;
- locate the active build-state/impact record;
- inspect any live external system touched by the work;
- determine whether an authoritative implementation line already exists.

If it does, continue it. Do not create a competing implementation line from `main`.

## Before implementation

Use the PR governance classification format to record the scope, affected surfaces, change level, authoritative branch, required rule packs, protected behavior, external systems, rollback point, and verification boundary.

If scope expands into another rule category, reclassify before continuing.

## Working rule

Work in the **largest coherent safe batch** allowed by the approved scope, ownership boundaries, rollback plan, and available verification.

Do not fragment work merely for caution. Do not combine unrelated work merely for size.

App consistency and operator workflow consistency are protected behavior unless the approved change intentionally modifies them.

## Stop/merge rules

A required failure or unresolved structural/external-state contradiction stops the affected path.

Level 3 work requires explicit operator approval before merge. Design or implementation approval is not merge approval.

## Detailed rules

`RULE_INDEX.md` routes to the authoritative domain/testing/integration/phase rules.

Do not rely on chat memory when the repository rule source is available.
