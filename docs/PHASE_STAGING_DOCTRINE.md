# Field Photo Prep Phase Staging Doctrine

Date adopted: 2026-09-10

Status: **GOVERNED PROJECT OPERATING RULE**

## Purpose

Field Photo Prep is developed in large, evidence-based stages rather than alternating constantly between small code changes and repeated phone checks.

The default development pattern is:

`build everything that can be honestly proven without the phone → stage at the next genuine physical-device boundary → run the smallest required phone reality gate → accept the evidence → adjust only where reality requires it → continue the next phase as far as possible → stage again`

A phone gate is an evidence checkpoint, not an automatic reason to stop development permanently or to begin another round of fragmented one-step work.

## Current transition: Phase 6 into Phase 7

The current app is staged at the Phase 6 physical-device boundary.

After the outstanding Phase 6 device gates are completed:

1. consolidate the device evidence once;
2. classify Phase 6 as PASS, BLOCKED, or FAIL under the master device-gate plan;
3. if Phase 6 passes, use the actual Android/Google Drive provider evidence to finalize Phase 7 assumptions;
4. adjust the Phase 7 plan only where the Phase 6 evidence shows a real need;
5. proceed through Phase 7 in the largest safe, testable chunks practical;
6. use JVM/unit, emulator/instrumentation, fake-provider, contract, and CI evidence for behavior that does not require a real phone;
7. do not stop Phase 7 early merely because a later phone check will eventually be required;
8. continue until further progress would require guessing about real Android, camera, Google Drive DocumentsProvider, device lifecycle, provider freshness, or another physical-device behavior;
9. at that point, freeze the tested runtime, record the exact runtime SHA and CI evidence, perform the appropriate lean/checkpoint review, stage the APK and evidence, and create the next straight-line device reality gate;
10. stop only at that genuine device-dependent boundary.

This rule intentionally allows the Phase 7 staging point to move. The exact Phase 7 implementation and next device gate must be based on what the Phase 6 phone evidence actually proves.

## Straight-line development rule

Within an approved phase:

- settled decisions stay settled unless new evidence contradicts them;
- do not repeatedly ask for authorization that the current approved scope already grants;
- do not rerun complete test suites as paperwork when exact valid CI evidence already exists and relevant runtime behavior has not changed;
- do not require one-command-at-a-time Bash interaction when commands can be safely batched or repository facts are already established;
- do not stop after each small implementation slice merely to perform a phone test that is not yet required to make the next safe implementation decision;
- do not invent provider behavior that only a phone can prove;
- do not continue past a boundary where the next design decision materially depends on unverified real-provider/device behavior;
- veer from the planned line only for a concrete unexpected result, failed assumption, new safety risk, or evidence that the planned design is wrong.

## What may be decided on the spot

An agent may make narrow implementation decisions inside the approved phase when they:

- stay within the documented behavior and contracts;
- do not expand product scope;
- do not weaken photo protection, destination identity, duplicate control, uncertainty handling, or Drive safety;
- are covered by the planned testing boundary; and
- do not create a new physical-device assumption.

Examples include choosing a small helper shape, selecting the narrowest existing test seam, naming a local implementation detail, or consolidating harmless duplication when already authorized.

## What requires stopping or replanning

Stop the affected path and record the evidence when:

- a real phone/provider result contradicts an assumption used by the next phase;
- a required remote result becomes ambiguous;
- continuing would require guessing whether Google Drive or Android behaves a certain way;
- the approved scope would need to expand;
- a new Level 3 behavior is discovered that is not covered by the current impact plan;
- a required focused or final verification fails;
- the runtime can no longer preserve the protected original, immutable destination, duplicate-prevention evidence, or fail-closed uncertainty rules.

A stop should be targeted. Independent work may continue when it does not depend on the failed assumption and cannot hide or worsen the problem.

## Staging standard at each genuine device boundary

When a phase reaches the next legitimate phone-dependent boundary, stage it to the same standard used before the Phase 6 gate:

1. exact branch and runtime SHA;
2. exact final automated-tested runtime head;
3. final CI result and artifact identity/digest;
4. implementation/impact records current;
5. roadmap status current;
6. lean/checkpoint review proportional to the amount of new architecture added;
7. runtime frozen except for a separately governed fix;
8. smallest straight-line device test plan written before the phone session;
9. pass/block/fail and stop conditions defined;
10. no-loop rules applied;
11. device plan made a required read at the relevant execution and decision checkpoints.

Do not create a large audit ritual when the phase added little architecture. The review should be proportional and should answer whether the implementation is still the smallest safe architecture for the workflow.

## Relationship to the current master device plan

The current physical-device execution plan is:

`docs/MASTER_DEVICE_REALITY_GATE_PLAN_2026-09-10.md`

That file governs the outstanding Phase 3B, Phase 4, Phase 5, Phase 6A, and Phase 6B-H4 phone session.

This staging doctrine governs what happens **after that evidence is collected**, especially the Phase 6 → Phase 7 transition and future phase/device-gate cycles.

If Phase 6 phone evidence requires changes, update the Phase 7 design and staging point to match the evidence. Do not preserve an outdated plan merely because it was written earlier.

## Phase 7 intent under this doctrine

Phase 7 should be pushed as far as can be safely and honestly proven before the next phone gate. Candidate provider-independent work includes, subject to the actual Phase 6 evidence and approved Phase 7 design:

- retry eligibility and state transitions;
- reconciliation decision logic;
- preservation of provisional/confirmed remote identity evidence;
- deterministic-name matching rules;
- exact-destination preservation;
- per-photo isolation;
- restart recovery;
- cleanup eligibility after confirmed success;
- cleanup-failure bookkeeping;
- focused fake-provider tests;
- JVM/instrumentation verification;
- documentation and migration records.

Provider-dependent claims remain behind the next physical-device gate where appropriate.

## Governing principle

**Build to evidence, not to fear. Stop where reality is required, not where another safety prompt could be invented.**

The project should move in a straight line by default, with enough controlled flexibility to respond when actual device/provider evidence proves that the line needs to change.
