# Field Photo Prep — Agent Entry Point

This file is the mandatory starting point for human or automated work in this repository.

Do **not** begin by reading every contract. Route the work first.

## Mandatory first read

Before planning, creating a branch, changing code/configuration, or changing a live project system, read:

1. `GOVERNANCE.md`
2. `PROJECT_PROFILE.md`
3. `RULE_INDEX.md`

Then load only the detailed rule packs selected by `RULE_INDEX.md`.

## Mandatory takeover preflight

Before creating a new branch or restarting prior work:

- inspect current `main`;
- inspect open pull requests;
- inspect relevant active branches;
- locate the active build-state/impact record;
- inspect any project-defined live external system touched by the work;
- determine whether an authoritative implementation line already exists.

If an existing branch/PR owns the same or overlapping scope, continue it.

Do not create a competing implementation line from `main` merely because active work is unmerged.

If an old line must be replaced, explicitly supersede it under `GOVERNANCE.md`.

## Classify before implementation

Record:

- goal;
- affected surfaces;
- change level;
- authoritative branch/PR;
- required rule packs;
- protected behavior;
- external systems touched;
- rollback point;
- verification boundary.

If scope expands into another rule category, load that rule pack and reclassify before continuing.

## Batch-size rule

Work in the **largest coherent safe batch**.

Do not artificially fragment work into tiny steps when the scope, ownership, rollback, and verification are understood.

Do not combine unrelated work merely to make a batch larger.

Stop at genuine boundaries defined in `GOVERNANCE.md`, such as a failed required gate, unresolved structural assumption, external-state disagreement, physical reality gate, material scope expansion, or Level 3 merge approval.

## Consistency

Unless the approved scope intentionally changes them:

- preserve the established app workflow;
- preserve established terminology/navigation/status meanings;
- extend canonical behavior instead of creating alternate implementations;
- keep one authoritative owner for each state, identity, destination, authorization decision, queue transition, and remote-write path.

App consistency and workflow consistency are protected behavior.

## Required failure behavior

A required failure stops the affected merge/publication/deployment path.

Do not call incomplete or failed evidence a pass.

## Level 3 merge boundary

Level 3 work requires explicit operator approval before merge.

Implementation/design approval is not merge approval.

## Detailed rule packs

The existing detailed contracts remain authoritative for their domains:

- `CONTRACT.md` — product/photo/queue behavior
- `CHANGE_CONTROL_CONTRACT.md` — change levels, records, approval, rollback
- `TESTING_CONTRACT.md` — test selection, final gates, failure behavior
- `INTEGRATION_CONTRACT.md` — Google Drive / Android DocumentsProvider
- `REGRESSION_CHECKLIST.md` — affected behavior checklist
- `docs/PHASE_STAGING_DOCTRINE.md` — when development should stop for genuine device/external evidence
- `docs/IDENTITY_MODEL_V1.md` — FPP identity model
- current approved phase/design records — scope-specific behavior

Use `RULE_INDEX.md` to decide which ones apply. Do not treat every file above as mandatory reading for unrelated work.
