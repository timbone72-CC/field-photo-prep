# Phase 9D Step 1 — Photo Row Actions Impact Record

Date: 2026-09-12
Status: **IMPLEMENTED — AUTOMATED VERIFICATION PENDING**
Branch: `feat/concept-3-ui-makeover-20260912`
PR: #35
Rollback baseline: `785dd873536e40395d10825d7d2248ae55788f2e`

## Problem

Concept 3 photo rows displayed a `⋯` affordance that had no action of its own. The only existing per-photo controls were in the selected-photo panel after the full photo list, so the row affordance could appear dead in normal field use.

## Step 1 scope

Replace the decorative row dots with a real 48dp photo-actions control. Tapping it:

1. invokes the row's existing selection handler so the exact photo becomes the current selected photo;
2. reads the enabled state of the existing Prepare / Upload / Reconcile / Discard buttons;
3. opens a small contextual popup containing only those currently available actions;
4. invokes the existing button/action owner when an item is chosen.

If no action is currently safe/available, the popup shows a disabled `No actions available` item rather than behaving like a dead control.

The existing selected-photo panel remains unchanged in Step 1. Its placement is handled separately by Step 2 of the locked Phase 9D plan.

## Owners and surfaces

Changed runtime surfaces:

- `row_photo.xml` — real 48dp `PhotoRowActionsView` replaces decorative `⋯` text.
- `PhotoRowActionsView.java` — UI-only popup bridge to the existing per-photo buttons.

Focused test:

- `PhotoRowActionsViewInstrumentedTest.java` — verifies the visible action control is clickable, selects the exact photo through the production row handler, and exposes the existing selected-photo action surface without touching Drive.

## Read/write impact

Opening the popup performs no Drive write and introduces no persisted state.

The popup reads only the already-rendered enabled state of the existing action buttons. Choosing a menu item delegates to the same existing Prepare / Upload / Reconcile / Discard button handler that was already present before this step.

## Protected behavior

Unchanged:

- camera behavior;
- SAF permissions and provider identity;
- protected originals;
- prepared-photo ownership;
- immutable stored destination identity;
- queue states/transitions;
- upload sequencing;
- FAILED / UNCERTAIN retry rules;
- reconciliation;
- duplicate protection;
- confirmed-success cleanup.

No Drive, queue, persistence, schema, or camera owner is modified.

## Rollback

Revert this Step 1 commit to return to exact baseline `785dd873536e40395d10825d7d2248ae55788f2e`.

## Physical check after the repair set

On Samsung, tap `⋯` on a normal ready photo and confirm the popup appears beside that row with the actions currently valid for that photo. Also confirm an UNCERTAIN photo exposes reconciliation rather than retry/upload. No destructive Drive test is required for this UI doorway.
