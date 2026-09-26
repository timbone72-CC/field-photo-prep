# Project Governance

## Purpose

These are the universal work-control rules. Detailed product and feature rules are loaded through `RULE_INDEX.md`.

Goal: make the largest coherent change that can be understood, reversed, and verified safely.

## Operating cycle

For repository or live-system work:

1. **Discover** — inspect `main`, open PRs/active branches, active build-state, and touched live systems.
2. **Classify** — record scope, surfaces, risk level, rule packs, protected behavior, rollback, and verification.
3. **Continue** — use the one authoritative branch/PR for overlapping scope.
4. **Batch safely** — group all related work that can be understood and verified together.
5. **Implement in the owning path** — do not create duplicate state machines, persistence paths, remote-write paths, or user workflows without an approved design change.
6. **Verify proportionally** — a required failure stops the affected path.
7. **Reconcile external state** — persistent live changes must match source control or be precisely recorded when source control cannot represent them.
8. **Handoff** — keep one durable status record sufficient for a fresh agent to resume.
9. **Close atomically** — implementation, evidence, external state, roadmap/status, approval, and merge/deployment state must agree.

## One authoritative implementation line

Only one branch/PR may be authoritative for the same or materially overlapping scope.

Before creating a branch, check open PRs, relevant branches, and the active build-state record.

If a line already owns the scope, continue it. A replacement line must explicitly record:
- why the old line is superseded;
- what state carries forward;
- the new authoritative line;
- disposition of the old PR/branch.

Preserve history. Do not silently restart active work from `main`.

Independent branches are allowed only for genuinely non-overlapping ownership/surfaces.

## Source of truth

Completed work: governed `main`.

In-progress work: governed `main` + the one authoritative branch/PR + its durable build-state/impact record + any legitimate live external state for that work.

Do not compare a live system only with `main` while ignoring an active branch.

Material disagreement among these sources stops implementation until reconciled.

## External-state parity

Databases, hosted functions, cloud configuration, permissions, schemas, deployments, and other project-defined live systems must not become undocumented second sources of truth.

If representable in source control, capture the exact live change on the authoritative branch and verify parity before proceeding.

If not representable in source control, record purpose, environment, non-secret value/class, evidence, and rollback/recovery in the active project record.

Do not create a replacement backend/path merely because `main` is behind active work.

## Classification and risk

Use `RULE_INDEX.md` and `CHANGE_CONTROL_CONTRACT.md`.

Every implementation classification includes:
- goal;
- affected surfaces;
- Level 1/2/3;
- authoritative branch/PR;
- rule packs;
- protected behavior;
- external systems;
- rollback;
- verification boundary.

If scope expands, reclassify first. When uncertain between levels, use the higher level.

Level 3 requires explicit operator approval before merge.

## Largest-safe-batch rule

Prefer the largest coherent batch with stable scope, understood dependencies/ownership, known rollback, and a verification boundary that can prove the whole batch.

Do not stop merely because work is large.

Stop at a genuine boundary: unresolved structural assumption, required product decision, physical/external reality gate, required failure, external-state disagreement, material scope expansion, or Level 3 merge approval.

## Consistency

App behavior and operator workflow are protected behavior unless intentionally changed.

Preserve established terminology, navigation, status meanings, action order, and canonical flows. Extend existing owners rather than creating alternate paths for the same job.

Keep one authoritative representation for each identity, destination, queue state, authorization state, persistence decision, and remote-write decision.

Treat workflow and visual consistency as regression concerns.

## Failure and verification

A required test, verification gate, or structural precondition failure stops the affected merge/publication/deployment path.

Do not report failed or incomplete evidence as passed. Fix the bounded defect, rerun focused evidence first, then required final verification.

## Durable handoff

Each active Level 2/3 scope has one handoff record containing:
- governed base/rollback;
- authoritative branch/PR and current implementation head;
- completed work;
- live external state touched;
- passed evidence;
- known failures/limitations;
- remaining gates and exact next checkpoint;
- approval/merge status.

The handoff records status; it does not replace behavior/design contracts.

## Closeout

A phase/scope is complete only when:
- intended implementation/configuration is on the approved line;
- required automated/reality evidence passes;
- external state is reconciled;
- disposable fixtures are cleaned or deliberately retained/documented;
- roadmap/status reflects reality;
- competing lines are closed/quarantined;
- required approval is recorded;
- merge/deployment state is unambiguous.

## GitHub enforcement boundary

GitHub should enforce what it can observe: PR-based changes to `main`, required checks, no force-push/delete, conversation resolution where configured, and machine-readable PR classification.

GitHub cannot prove semantic choices such as correct rule-pack selection, workflow quality, live-system parity, or that a recorded operator approval was genuinely spoken when automation shares the operator identity. Contracts and operator review remain authoritative for those decisions.

## Precedence

When documents disagree, stop and reconcile. Precedence:
1. `GOVERNANCE.md` — work-control/process;
2. `PROJECT_PROFILE.md` — project systems/boundaries;
3. product/domain contracts — approved behavior;
4. approved phase/design/impact records — current change design;
5. active build-state record — current status/continuation point.

Lower layers may add detail but may not silently override higher layers.
