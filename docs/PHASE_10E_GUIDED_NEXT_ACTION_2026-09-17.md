# Phase 10E — Guided Next-Action Workflow

Date: 2026-09-17

Status: **LEVEL 2 — COMPLETE**

Branch: `feat/phase-10e-guided-next-action-20260917`

## Problem

The app already knows the selected property, selected work order, photo queue states, preparation state, upload state, reconciliation state, and whether Drive is connected. The UI does not always turn that known state into one obvious next step, so the operator still has to interpret controls and remember the workflow.

## Goal

Make routine field progression self-explanatory by deriving one prominent next action from existing authoritative app state.

No persisted workflow state is added.

## Design

Use a stateless presentation policy that maps current existing state into a display action. The policy is not a second workflow engine and does not own any persistence, Drive, queue, camera, upload, retry, reconciliation, or deletion behavior.

Home:
- Drive unavailable → **Next: Connect Google Drive**;
- connected with no properties → **Next: Add a Property**;
- connected with properties but no current valid property → **Next: Choose a Property**;
- current saved property still exists in the property list → **Next: Open Work Orders**.

Work Orders:
- no selected work order → **Next: Choose or Add Work Order**;
- exact work order selected → **Next: Take Photos**.

Photos:
- no current photos → **Next: Take Photos**;
- active local preparation → **Preparing Photo…**;
- active Drive operation → **Uploading…** or **Checking Uploads…**;
- UNCERTAIN backlog → **Next: Check N Uploads**;
- unprepared/retry-preparation photo exists → **Next: Prepare Photo**;
- ready photos exist and no safe upload selection is active → **Next: Select N Ready Photos**;
- safe selected upload batch exists → **Next: Upload Selected (N)**;
- all current photo records are confirmed uploaded → **Done — Return to Work Orders**.

## Delegation rule

The next-action controls call existing action owners:
- Connect Drive → existing folder picker;
- Add Property → existing address entry;
- Open Work Orders → existing saved-property open path;
- Take Photos → existing camera/photo navigation;
- Prepare Photo → existing per-photo preparation owner after selecting one exact eligible record;
- Select Ready Photos → existing Select All Ready behavior;
- Upload Selected → existing strictly sequential batch upload;
- Check Uploads → existing read-only bulk reconciliation;
- Done → existing Work Orders navigation.

The guidance layer may choose which already-valid action to surface. It does not duplicate the action implementation.

## Safety stops

Never turn these into automatic continuation:
- Clear & Reuse;
- local discard;
- Change Drive/provider/master;
- ambiguous property/work-order identity;
- unresolved UNCERTAIN state;
- any destination change;
- any action that could create a second remote copy.

## Read/write surfaces

Reads:
- existing Drive connection state;
- existing property/work-order selections;
- current in-memory property/work-order lists;
- existing photo scan and prepared-copy state;
- existing preparation/upload gates;
- existing batch selection.

Writes:
- presentation text/enabled state;
- ordinary existing action methods only after explicit operator tap.

No new persistence/schema is introduced.

## Verification

Focused tests must prove:
- each major normal state maps to the expected next-action label and enabled/disabled state;
- next-action controls delegate to the existing action owners;
- unsafe/destructive actions are never exposed as automatic next actions;
- existing navigation/photo/Drive safety tests remain passing.

Complete Android CI is required on the final runtime head.

A focused Samsung smoke is required because this phase intentionally changes the routine field interaction surface.


## Completion evidence

- versionCode 28 / `0.23-guided-next-action`;
- exact runtime head `7cfd56d932cdda0941e3952bca5ad8c75ddeb712`;
- Android CI run `35297157236`: PASS;
- unit policy tests passed;
- internal debug build and stable signer verification passed;
- full instrumentation, guided delegation tests, launch smoke, and artifacts passed;
- operator installed the exact green 0.23 internal APK on Samsung and reported the guided flow **Works**;
- PR #53 merged to `main` at `8f1ce719d57564faf79a523bb665a7af165e2b4b`.

No Drive destination, provider identity, queue, upload, retry, reconciliation, local discard, Clear & Reuse, or photo-preparation behavior was changed by this phase.
