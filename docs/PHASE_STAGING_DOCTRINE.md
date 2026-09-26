# Field Photo Prep Phase Staging Doctrine

> **Routing scope:** Permanent staging philosophy for deciding how far approved work should proceed before a genuine physical-device or external reality gate.

Date adopted: 2026-09-10
Last simplified: 2026-09-25

Status: **GOVERNED PROJECT OPERATING RULE**

Historical Phase 6 → Phase 7 wording is preserved at:
`docs/history/PHASE_STAGING_DOCTRINE_2026-09-10_ORIGINAL.md`

## Purpose

Field Photo Prep is developed in large, evidence-based stages rather than alternating constantly between small code changes and repeated phone checks.

Default pattern:

**build everything that can be honestly proven without the real device/provider → stage at the next genuine external boundary → run the smallest required reality gate → accept the evidence → adjust only where reality requires it → continue**

A reality gate is an evidence checkpoint, not a reason to fragment otherwise safe work.

## Largest-safe-stage rule

Within approved scope:

- settled decisions stay settled unless new evidence contradicts them;
- continue through the largest coherent batch whose ownership, rollback, and verification are understood;
- use unit/JVM, instrumentation/emulator, fake-provider, contract, and CI evidence for claims those tools can honestly prove;
- do not stop merely because a physical test will eventually be needed;
- do not invent device/provider behavior that only reality can prove;
- stop before the next design or safety decision would require guessing about that real behavior.

This doctrine and `GOVERNANCE.md` use the same batch-sizing rule.

## No-loop rule

Do not:
- repeatedly ask for authorization already granted by the approved scope;
- rerun complete suites as paperwork when exact relevant evidence remains valid;
- require one-command-at-a-time workstation interaction when safe commands can be batched;
- repeat physical observations merely for confidence;
- refresh/retry an uncertain remote operation until it appears to work.

Passing evidence closes that observation.

## Decisions that may stay inside the batch

An implementation decision may be made without stopping when it:

- remains inside approved behavior/scope;
- uses an existing responsibility/state owner;
- does not weaken protected photos, destination identity, duplicate control, uncertainty handling, authorization, or Drive safety;
- is covered by the selected rule packs and planned tests;
- does not create a new unverified external/device assumption.

Examples include a narrow helper shape, internal naming, test seam selection, or bounded cleanup already inside scope.

## Genuine stop/replan boundaries

Stop the affected path when:

- real device/provider evidence contradicts an assumption required by the next step;
- a required remote result becomes ambiguous;
- continuing requires guessing about external behavior;
- scope materially expands;
- risk classification must increase;
- a required focused/final verification fails;
- external live state disagrees with the authoritative branch;
- protected originals, immutable destinations, duplicate evidence, or fail-closed uncertainty can no longer be preserved;
- explicit Level 3 merge approval is the only remaining gate.

A stop is targeted. Independent work may continue when it does not depend on the failed assumption and cannot hide or worsen it.

## Staging standard at a genuine reality boundary

Record only what is needed to resume safely:

1. authoritative branch/PR;
2. exact runtime/configuration head;
3. governed rollback point;
4. final applicable automated evidence;
5. artifact identity when an installable/deployable artifact matters;
6. active impact/build-state record;
7. external state already changed;
8. exact remaining physical/external claim;
9. smallest safe reality-gate procedure;
10. pass/block/fail and stop conditions.

Freeze the affected runtime except for a separately governed fix while the reality gate is pending.

## Current-plan rule

Use the current active phase/build-state record to determine today's device/external gate.

Do not treat an old phase-specific device plan as current merely because it still exists in the repository.

Historical plans remain evidence of what was proven at the time.

## Governing principle

**Build to evidence, not to fear. Stop where reality is required, not where another safety prompt could be invented.**
