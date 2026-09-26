# Field Photo Prep — Agent Entry Point

This is the mandatory starting point for repository work.

## First read

Before planning, branching, changing code/configuration, or changing a live project system, read:

1. `GOVERNANCE.md`
2. `PROJECT_PROFILE.md`
3. `RULE_INDEX.md`

Then load only the detailed rule packs selected by `RULE_INDEX.md`.

## Before creating or resuming work

Identify:
- current `main`;
- open PRs and relevant active branches;
- the active build-state/impact record;
- live external systems touched by the work;
- the single authoritative branch/PR for the scope.

If the same or overlapping scope already has an authoritative line, continue it. Do not restart from `main`. Superseding a line must be explicit under `GOVERNANCE.md`.

## Before implementation

Record:
- goal and affected surfaces;
- change level;
- authoritative branch/PR;
- required rule packs;
- protected behavior;
- external systems touched;
- rollback point;
- verification boundary.

If scope expands, load the new rule pack and reclassify before continuing.

## Working rule

Build the **largest coherent safe batch**. Do not fragment work for ceremony and do not combine unrelated work for size.

Unless the approved scope intentionally changes them, preserve established workflow, terminology, navigation, status meanings, and the existing owner of each state/identity/destination/write path.

A required failure stops the affected merge/publication/deployment path. Never call incomplete evidence a pass.

Level 3 requires explicit operator approval before merge; design or implementation approval is not merge approval.

Detailed behavior remains owned by the contracts and rule packs routed through `RULE_INDEX.md`.
