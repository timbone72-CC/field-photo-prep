# Phase 9D — Field UI Repair Plan

Date: 2026-09-12
Status: **LOCKED — AUDIT COMPLETE ENOUGH TO PLAN; NO RUNTIME FIXES IN THIS RECORD**
Branch: `feat/concept-3-ui-makeover-20260912`
PR: #35
Audit baseline before this record: `a3e868d7d33488cdb3cb3e1b5305703275d26a9b`

## Goal

Repair the confirmed Concept 3 field-UI defects without reopening or redesigning the proven camera, Drive, photo identity, queue, upload, retry, reconciliation, or cleanup core.

The implementation order below is locked unless a newly discovered defect makes a later step unsafe to perform as written.

## Confirmed findings driving this plan

1. **Photos row three-dot affordance is dead/misleading.** Every photo row shows `⋯`, but it is only display text and has no menu or click handler.
2. **Per-photo actions are poorly placed.** Selecting a photo exposes its existing Prepare / Upload / Reconcile / Discard actions after the full photo list, so the response can be far from the row the operator touched.
3. **Home overflow is field-broken on Samsung.** Source wiring exists and currently contains only `Change Drive`, but the operator reported that the three-dot control does not work reliably on the physical device.
4. **Visible-control interaction testing is incomplete.** Current Concept 3 tests prove rendering and some navigation/state behavior, but do not exercise enough visible controls to catch dead or non-responsive UI.
5. **Small cleanup debt exists.** Photo checkbox/touch sizing is below the intended 48dp target, and one old hidden work-order selector remains wired internally.
6. **Settings is absent by design history, not a regression.** Do not add a Settings screen in this repair phase merely to make the shell look complete.
7. **Photo-list scaling is a risk only.** Current list rebuilding/thumbnail decoding may become slow at high photo counts, but no field defect has been reported. Do not optimize it speculatively.

## Repair order

### 1. Repair Photos row actions

Make the visible `⋯` affordance perform a real, immediate action for that exact photo.

Preferred implementation:
- `⋯` opens a small contextual menu for the selected photo;
- menu items invoke the existing state-safe Prepare / Upload / Reconcile / Discard owners;
- unavailable actions are hidden or clearly disabled according to the existing queue state;
- no new photo state, persistence, destination, retry, or Drive logic is introduced.

If a simpler implementation provides the same immediate and clear access to the existing actions with less code, use the simpler implementation.

### 2. Remove the long-list action-placement problem

Do not require the operator to scroll below the entire photo list to discover what happened after selecting a row.

After the row action repair:
- remove the existing bottom action panel from normal use if it is redundant; or
- retain it only where it remains genuinely useful as a fallback/details surface.

Do not duplicate the underlying action logic.

### 3. Repair Home overflow

Keep the scope narrow:
- the overflow remains a secondary Drive action surface;
- `Change Drive` must reliably open the existing master-folder picker on Samsung;
- do not convert it into a broad settings or feature menu during this phase.

### 4. Add focused interaction tests

Add only the interaction coverage needed to prevent the defects found in this audit from returning.

At minimum exercise:
- Home overflow / `Change Drive` launch path;
- property-row navigation;
- Work Orders navigation and Open Photos;
- Photos row action / overflow behavior;
- batch selection control behavior;
- bottom-navigation paths and their existing context guards.

Do not create an exhaustive UI automation framework.

### 5. Small low-risk cleanup

- bring the photo selection touch target to the intended minimum size;
- remove the hidden legacy work-order selector and its obsolete wiring if no current runtime path requires it;
- make no unrelated layout cleanup.

### 6. Do not build Settings in Phase 9D

Settings is deferred unless a separately approved need establishes enough real operator-adjustable values to justify it.

Do not add placeholder settings.
Do not make safety behavior optional.
Do not expose switches that weaken protected originals, immutable destination binding, UNCERTAIN reconciliation, retry safety, or confirmed-success cleanup.

### 7. Do not optimize the photo list without evidence

The current Photos list implementation may be inefficient at large counts. Treat this as a recorded risk only.

Only start a separate performance repair if real Samsung use demonstrates material lag, delayed taps, memory pressure, or unusable scrolling at realistic field photo counts.

## Protected core — do not touch for this repair

Unless a repair cannot be completed safely without a core change, Phase 9D must not change:

- `CameraCaptureActivity` behavior;
- CameraX capture/session behavior;
- SAF master-tree identity or persisted permission model;
- provider `DocumentId` ownership rules;
- protected-original storage semantics;
- automatic photo preparation ownership;
- immutable address/work-order destination stored on each photo;
- queue states and transition rules;
- upload sequencing;
- FAILED / UNCERTAIN retry rules;
- remote reconciliation;
- duplicate protection;
- confirmed-success cleanup.

A required core change stops this repair phase and requires a separate impact review before implementation continues.

## Samsung verification after implementation

The operator should test only the repaired UI surfaces:

1. Home `⋯` reliably exposes `Change Drive`.
2. Photo-row `⋯` immediately exposes the correct actions for that photo.
3. Per-photo actions are obvious without scrolling to the bottom of a long list.
4. Bottom navigation still behaves normally, including existing context requirements.
5. Home, Work Orders, and Photos have no obvious visual regression from the repair.

Do not require a full destructive Drive retest or camera regression pass solely for these UI repairs unless implementation unexpectedly touches those owners.

## Completion order

`Photos ⋯` → per-photo action placement → Home `⋯` → focused interaction tests → small cleanup → focused Samsung gate → version/docs reconciliation → merge review.

## Anti-bloat rule

Phase 9D fixes confirmed field-UI defects and the exact test gaps that allowed them through. It does not become a general redesign, Settings project, performance rewrite, accessibility overhaul, architecture refactor, or new-feature phase.
