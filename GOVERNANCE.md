# Project Governance

## Purpose

This file defines the universal operating rules for work in this repository. It is intentionally short. Detailed product, testing, integration, and feature rules are loaded only when the current work touches those surfaces.

The goal is not to force small changes. The goal is to make the largest coherent change that can be understood, reversed, and verified safely.

## Mandatory operating cycle

For every task that may change the repository or an external project system:

1. **Discover** — inspect current `main`, open pull requests, relevant active branches, the active build-state record, and any project-defined live external systems touched by the work.
2. **Classify** — state the requested scope, affected surfaces, change level, required rule packs, protected behavior, and rollback point.
3. **Continue the authoritative line** — if an active branch/PR already owns the same or overlapping scope, continue it. Do not create a competing implementation line.
4. **Plan the largest safe batch** — group as much related work as can be understood and verified together. Do not fragment work merely for caution, and do not combine unrelated concerns merely for size.
5. **Implement within ownership boundaries** — use the existing owner of each behavior/state. Do not create a second state machine, persistence path, remote-write path, or user workflow for the same responsibility without an approved design change.
6. **Verify proportionally** — use the rule packs selected by `RULE_INDEX.md`. A required failure stops the affected work.
7. **Reconcile external state** — any persistent live-system change must be represented in source control or, when source control cannot represent it, recorded precisely in the active build-state/evidence record before proceeding.
8. **Update the handoff** — keep the active build-state record accurate enough that a fresh agent can resume without reconstructing the project from chat history.
9. **Close atomically** — a phase/scope is not complete until code/configuration, tests/evidence, external-state parity, status/roadmap records, and merge/approval state agree.

## One authoritative implementation line

Only one branch/PR may be authoritative for the same or materially overlapping implementation scope.

Before creating a branch:
- inspect open PRs;
- inspect relevant active branches;
- inspect the active build-state record;
- compare the proposed scope with existing work.

If an authoritative line exists, continue it.

A replacement line is allowed only when the existing line is explicitly superseded. Record:
- why the old line cannot safely continue;
- what state is being carried forward;
- which line is now authoritative;
- what happens to the old PR/branch.

Preserve history. Do not silently abandon one implementation and start the same work again from `main`.

Unrelated work may use a separate branch only when ownership and affected surfaces do not overlap in a way that creates competing state or conflicting source-of-truth.

## Source-of-truth rule

For completed work, `main` is the governed code baseline.

For in-progress work, source of truth is the combination of:
- governed `main`;
- the single authoritative active branch/PR;
- its durable build-state/impact record;
- the project-defined live external systems that the active work legitimately changes.

Do not compare a live system only to `main` and call it drift while ignoring an existing authoritative active branch.

If these sources disagree materially, stop implementation and reconcile the disagreement before creating replacement code, migrations, services, or state.

## External-state parity

Persistent changes to databases, hosted functions, cloud configuration, deployments, permissions, schemas, or other project-defined live systems must not become an undocumented second source of truth.

When a live change can be represented in source control:
- capture it on the authoritative branch under the exact applicable version/history;
- verify the source representation matches the live state before continuing beyond that checkpoint.

When a live setting cannot be represented directly in source control:
- record its exact purpose, value class (never secrets), environment, verification evidence, and rollback/recovery instruction in the active project record.

Never build a second backend or replacement external path merely because `main` is behind active work.

## Work classification

Before implementation, record:

- **Goal**
- **Affected surfaces**
- **Change level**
- **Authoritative branch/PR**
- **Required rule packs**
- **Protected behavior**
- **External systems touched**
- **Rollback point**
- **Verification boundary**

Use `RULE_INDEX.md` to choose the applicable rule packs.

If work begins touching a surface not in the classification, reclassify before continuing that expanded work. If the expansion changes risk, raise the change level.

## Change levels

The detailed definitions remain in `CHANGE_CONTROL_CONTRACT.md`.

- **Level 1** — low-risk documentation, comments, noninteractive copy, or equivalent changes that cannot alter protected runtime behavior.
- **Level 2** — normal feature/fix work that does not change high-risk persistence, destination, permission, destructive, retry, or deployment semantics.
- **Level 3** — high-risk changes involving persisted schemas, migrations, destructive behavior, provider/account selection, destination identity, retry/idempotency, protected-data risk, or deployment/signing.

When uncertain, use the higher level.

Level 3 requires explicit operator approval before merge.

## Safe batch sizing

Prefer the largest coherent batch whose:
- scope is stable;
- dependencies are understood;
- ownership is clear;
- rollback is known;
- verification can prove the whole batch.

Do not stop merely because the batch is large.

Stop at a genuine boundary:
- an unverified structural assumption;
- a required user/product decision;
- a physical-device or external reality gate that cannot be proven otherwise;
- a required test failure;
- external-state disagreement;
- scope expansion requiring reclassification;
- explicit Level 3 merge approval.

## Consistency rule

The app and operator workflow are protected behavior.

Unless the approved scope intentionally changes them:
- preserve established terminology, navigation patterns, status meanings, and action order;
- prefer extending the existing canonical flow over adding an alternate flow for the same job;
- reuse existing behavior/state owners instead of duplicating logic;
- keep one authoritative representation for each identity, destination, queue state, authorization state, and remote-write decision;
- treat visual consistency and workflow consistency as regression concerns, not optional polish.

## Failure rule

A required test, verification gate, or structural precondition failure stops the affected merge/publication/deployment path.

Do not report a failed or incomplete gate as passed.

Fix the bounded defect, rerun the focused evidence first, then run the required final verification when the branch is ready.

## Handoff requirement

Every active Level 2 or Level 3 scope must have one durable handoff record. It must identify at least:
- governed base / rollback commit;
- authoritative branch and PR;
- current implementation head;
- completed work;
- live external state touched;
- tests/evidence already passed;
- known failures or limitations;
- exact remaining gates;
- exact next checkpoint;
- approval/merge status.

The handoff is status, not a substitute for product contracts or design documents.

## Closeout rule

A phase/scope is complete only when:
- the intended implementation/configuration is on the approved line;
- required tests and reality gates pass;
- external state matches the recorded/source-controlled state;
- disposable fixtures are cleaned or deliberately documented as retained;
- roadmap/status documents reflect reality;
- superseded competing lines are closed or clearly quarantined;
- required approval is recorded;
- merge/deployment state is unambiguous.

Do not leave the roadmap saying “NEXT” for work that has already merged.

## Rule precedence

When project documents disagree, reconcile the conflict rather than choosing whichever instruction is convenient.

Use this order:
1. this governance file for work-control/process rules;
2. `PROJECT_PROFILE.md` for project-specific systems and permanent project boundaries;
3. product/domain contracts for approved behavior;
4. approved phase/design/impact records for the current change;
5. active build-state records for current status and continuation point.

A lower layer may add detail but may not silently override a higher layer.
