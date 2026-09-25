# Project Governance

## Purpose

This file defines the universal work-control rules for this repository.

Detailed product, testing, integration, and feature rules are loaded only when the current work touches those surfaces.

The goal is to make the **largest coherent change that can be understood, reversed, and verified safely**.

## Operating cycle

For work that may change the repository or a project-defined live system:

1. **Discover** — inspect `main`, open PRs, relevant branches, the active build-state record, and affected live systems.
2. **Classify** — use `RULE_INDEX.md`, the PR governance fields, and `CHANGE_CONTROL_CONTRACT.md`.
3. **Continue the authoritative line** — do not create a competing branch for the same or overlapping scope.
4. **Build the largest safe batch** — keep related work together when ownership, rollback, and verification are understood.
5. **Use existing owners** — do not create a second state machine, persistence path, remote-write path, or user workflow for an existing responsibility without an approved design change.
6. **Verify proportionally** — use the routed rule packs; a required failure stops the affected path.
7. **Reconcile and hand off** — keep source control, live external state, build-state records, roadmap/status, and merge/approval state aligned.

## One authoritative implementation line

Only one branch/PR may be authoritative for the same or materially overlapping implementation scope.

Before creating a branch, inspect open PRs, relevant branches, and the active build-state record.

If an authoritative line exists, continue it.

A replacement line is allowed only after explicitly superseding the old one. Record:
- why the old line cannot safely continue;
- what work/state is carried forward;
- which line becomes authoritative;
- what happens to the old PR/branch.

Preserve history. Do not silently restart the same work from `main`.

Unrelated work may use a separate branch when its ownership and affected surfaces do not create competing state or source-of-truth.

## Source of truth

For completed work, `main` is the governed code baseline.

For in-progress work, source of truth is the combination of:
- governed `main`;
- the single authoritative active branch/PR;
- its durable impact/build-state record;
- any project-defined live external systems legitimately changed by that active work.

Do not compare a live system only to `main` and call it drift while ignoring an existing active branch.

If these sources materially disagree, reconcile them before creating replacement code, migrations, services, or state.

## External-state parity

Persistent live changes to databases, hosted functions, cloud configuration, deployments, permissions, schemas, or other project-defined external systems must not become an undocumented second source of truth.

When source control can represent the change:
- capture it on the authoritative branch under the applicable version/history;
- verify source and live state agree before moving beyond that checkpoint.

When source control cannot represent the setting:
- record its purpose, environment, non-secret value class, verification evidence, and rollback/recovery instruction in the active project record.

Never create a second backend or replacement external path merely because `main` is behind active work.

## Batch sizing and consistency

Prefer the largest coherent batch whose scope, dependencies, ownership, rollback, and verification are understood.

Do not stop merely because the batch is large.

Unless the approved scope intentionally changes them:
- preserve established terminology, navigation, status meanings, and action order;
- extend the canonical flow instead of adding an alternate flow for the same job;
- reuse existing behavior/state owners;
- keep one authoritative representation for each identity, destination, queue state, authorization decision, and remote-write decision.

App and workflow consistency are regression concerns, not optional polish.

## Genuine stop boundaries

Stop the affected path for:
- an unverified structural assumption required by the next step;
- a required user/product decision;
- a real device/provider/external gate that cannot be proven otherwise;
- a required test or verification failure;
- material external-state disagreement;
- scope expansion requiring reclassification;
- explicit Level 3 merge approval.

A stop is targeted. Independent work may continue when it does not depend on the failed assumption and cannot hide or worsen the problem.

## Failure rule

Do not report failed or incomplete evidence as passed.

Fix the bounded defect, rerun the focused evidence first, then run the required final verification when the branch is ready.

## Handoff

Every active Level 2 or Level 3 scope must have one durable handoff record containing enough state for a fresh agent to resume without reconstructing the project from chat history.

At minimum record:
- governed base / rollback commit;
- authoritative branch/PR and current implementation head;
- completed work;
- live external state touched;
- evidence already passed;
- known failures/limitations;
- remaining gates and exact next checkpoint;
- approval/merge status.

The handoff records status. It does not replace product contracts or design documents.

## Closeout

A phase/scope is complete only when:
- intended code/configuration is on the approved line;
- required tests/reality gates pass;
- external state matches recorded/source-controlled state;
- disposable fixtures are cleaned or deliberately retained/documented;
- roadmap/status documents match reality;
- superseded competing lines are closed or quarantined;
- required approval and merge/deployment state are unambiguous.

Do not leave status documents saying work is “NEXT” after it has merged.

## GitHub enforcement boundary

GitHub should mechanically enforce the facts it can observe:
- changes to `main` arrive through pull requests;
- required status checks pass;
- force pushes and branch deletion are blocked;
- unresolved review conversations block merge where configured;
- PRs contain the repository governance classification.

GitHub cannot independently determine whether every semantic rule pack was chosen correctly, whether external systems truly match source, or whether an approval statement was genuinely spoken by the operator when automation shares the operator's GitHub identity.

Machine enforcement supplements these rules; it does not replace judgment.

## Rule ownership and precedence

- This file owns work-control/process rules.
- `PROJECT_PROFILE.md` owns project-specific systems and permanent boundaries.
- Product/domain contracts own approved behavior.
- Approved phase/design/impact records own change-specific behavior.
- Build-state records own current continuation status.

When documents materially conflict, reconcile the conflict. A lower layer may add detail but may not silently override a higher layer.

Change-level definitions, approval requirements, impact-record contents, and rollback details remain authoritative in `CHANGE_CONTROL_CONTRACT.md`.
