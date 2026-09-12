# Phase 9 — App Makeover Reset Plan

Date: 2026-09-12

Status: **PLANNING — SCREEN SPECS FIRST**

Planning branch: `docs/phase-9-ui-reset-spec-20260912`

Baseline: `main` at `e0e46321893b44e2755d4cc842edca4ccd7c2974`

Superseded experiment: PR #32 `feat/phase-9-app-shell-redesign` — closed, not merged, branch preserved for evidence/reference.

## Why Phase 9 is being reset

Three physical-device attempts did not faithfully implement the selected Concept 3 direction. The main failure pattern was architectural: the implementation kept decorating/reparenting the existing development-style view hierarchy instead of replacing the non-camera screen structure with purpose-built app layouts.

The operator's device screenshot made the failure concrete:

- the Home title crowded the Android status bar;
- the title/subtitle/status occupied too much vertical space;
- the Google Drive panel dominated the screen;
- `New Address` was an oversized full-width control;
- property rows were tall and card-heavy;
- raw underscore-heavy Drive folder names were shown as the primary address label;
- only a small number of properties fit on screen;
- the result still looked like the previous utility UI with new colors.

Phase 9 will not continue by stacking more presentation decorators onto PR #32.

## Product direction

Use the approved **Concept 3 — Hybrid Field App** direction:

- Samsung/Material-style Android familiarity;
- compact field-first density;
- clear hierarchy instead of stacked developer controls;
- one obvious primary action per screen;
- common field actions visible;
- maintenance/destructive/technical actions quieter or tucked away;
- real content, no fake navigation or features;
- dark/light support;
- accessibility-sized touch targets without oversized screen furniture.

This plan is textual by design. No additional mockup drawing is required to authorize implementation once a screen specification is approved.

## Hard lock — camera

`CameraCaptureActivity` is a protected design surface for Phase 9.

Do not change:

- preview-first camera layout;
- shutter / Done placement;
- Flash / Torch controls;
- zoom slider / pinch / quick zoom behavior;
- portrait / landscape camera layouts;
- protected capture semantics.

A camera change requires a separately scoped defect or feature.

## Protected runtime behavior

The makeover must not change:

- persisted Drive master-tree permission semantics;
- address/work-order provider identities;
- address or work-order duplicate/create/reuse decisions;
- Clear & Reuse safety semantics;
- protected original photo behavior;
- photo preparation policy;
- queue states or state transitions;
- upload destination binding;
- sequential batch upload rules;
- `UNCERTAIN` behavior or reconciliation;
- confirmed-success cleanup rules;
- signing/deployment identity.

UI code may call existing owner methods and render their state; it must not duplicate or reimplement Drive/queue/upload persistence logic.

## Implementation rule — replace structure, do not decorate it

The replacement Phase 9 runtime must use purpose-built Android layouts/components for the non-camera screens rather than `ActivityLifecycleCallbacks` decorators that restyle the old hierarchy after creation.

Preferred implementation direction:

- Material 3 / Material Components;
- Java remains the runtime language;
- XML layouts are preferred for screen structure;
- list screens use a dedicated adapter/row layout (RecyclerView is acceptable/preferred);
- system-bar insets are handled deliberately;
- reusable visual tokens live in theme/resources rather than ad-hoc per-view mutations;
- existing business/Drive/queue owners remain the behavior source of truth.

No Compose rewrite is required.

## Screen-by-screen delivery

Phase 9 is split into three physical UI gates so a failed visual direction cannot spread across the whole app.

### Phase 9A — Home / Properties

Build only the Home/Properties makeover from its approved written spec.

Physical gate:

1. launch app;
2. verify Home layout and system-bar spacing;
3. verify Drive connected/disconnected presentation;
4. verify property list density/readability;
5. tap one existing property and confirm navigation still reaches the existing Work Orders flow.

Do not create a Drive address merely to prove appearance.

After PASS, lock Home/Properties and do not redesign it during 9B unless a real defect is discovered.

### Phase 9B — Work Orders

Replace the Work Orders UI after 9A is locked.

Physical gate will cover:

- property context;
- current work-order selection;
- existing work-order list;
- new dated work-order entry/action;
- maintenance tools placement;
- opening Photos from the selected work order.

Do not change the underlying Drive create/reuse/clear rules.

### Phase 9C — Photos

Replace the non-camera Photos UI after 9B is locked.

Physical gate will cover:

- work-order context;
- blue `Open Camera` action;
- real local photo thumbnails where bytes remain locally;
- compact operator-facing photo statuses;
- explicit checkbox selection;
- green sticky `Upload Selected (N)` action;
- return from the locked camera via Done.

No new Drive-write gate is required solely for layout if the runtime diff does not touch upload semantics.

## Verification model

Each runtime subphase is Level 2.

For each of 9A, 9B, and 9C:

- use a fresh implementation branch from the latest governed main line;
- record exact scope and rollback baseline;
- add focused UI/state tests for the changed screen;
- run focused tests during development;
- run the complete Android automated suite once on the exact final runtime head;
- stage one internal APK only after that complete suite passes;
- run one physical smoke for the changed screen;
- accept a passing observation once and move forward;
- do not repeat previously passed camera/Drive gates when those owners are untouched.

If the phone gate fails visually, keep the PR unmerged, correct that same screen, rerun the affected automated coverage plus the final complete suite on the new runtime, and retest only the failed observation.

## Merge discipline

- PR #32 remains closed and unmerged.
- Its branch is preserved; do not delete it.
- New Phase 9 runtime work starts from main, not from PR #32.
- 9A must pass and be locked before 9B starts.
- 9B must pass and be locked before 9C starts.
- No combined 'hope the whole makeover looks right' phone build.

## Current next step

Review and approve `docs/PHASE_9A_HOME_PROPERTIES_UI_SPEC_2026-09-12.md`.

Once approved, create the fresh Phase 9A runtime branch from `main` and build only Home/Properties to that specification.
