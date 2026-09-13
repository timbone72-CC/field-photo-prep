# Phase 9D Step 4 — Focused UI Interaction Test Record

Date: 2026-09-13
Status: **IMPLEMENTED — AUTOMATED VERIFICATION PENDING**
Branch: `feat/concept-3-ui-makeover-20260912`
PR: #35
Rollback baseline: `5b88fa28d0b3f06860e61a0c630fdc73f471cb03`

## Purpose

Close the remaining high-value Concept 3 interaction-test gaps without creating an exhaustive UI automation framework.

Steps 1–3 already added direct interaction coverage for:

- photo-row `⋯` actions;
- selected-photo action placement/ownership;
- Home `⋯` / `Change Drive` behavior.

The existing rendered-screen test already follows the real `Open Photos` action and Photos → Home navigation.

## Step 4 scope

Add one focused instrumentation class with two tests only.

### 1. Property and context navigation

Verify production handlers for:

- tapping a property row opens Work Orders for that exact property;
- the visible Work Orders Back control returns Home;
- Home → Work Orders bottom navigation uses the saved/current property context;
- Work Orders → Home bottom navigation returns Home;
- Work Orders → Photos reaches the real Photos activity when exact property/work-order context exists;
- Photos → Work Orders returns to the Work Orders screen.

No real Drive provider is read or written. Provider refresh failure in the isolated test environment is not treated as the subject of this navigation test.

### 2. Photos batch selection

Using isolated local photo fixtures, verify:

- `Select All Ready` selects only eligible prepared WAITING photos;
- `UNCERTAIN` photos remain unselected and disabled;
- confirmed `UPLOADED` photos remain unselected with their checkbox hidden;
- the sticky upload button count reflects the selected ready photos;
- `Clear` removes the batch selection and returns the count to zero.

The test does not press `Upload Selected` and performs no Drive upload.

## Files changed

- `app/src/androidTest/java/com/inandout/fieldphotoprep/FieldUiInteractionInstrumentedTest.java`
- `docs/PHASE_9D_STEP4_FOCUSED_UI_INTERACTION_TEST_RECORD_2026-09-13.md`

## Explicitly not added

Step 4 does **not** add:

- Espresso/page-object infrastructure;
- a general UI test framework;
- duplicate tests for every button;
- real Drive tests;
- camera hardware tests;
- destructive folder tests;
- runtime instrumentation hooks or test-only production code.

## Production behavior

No production runtime file is changed in Step 4.

Therefore unchanged:

- SAF/provider identity and permissions;
- property/work-order persistence;
- camera behavior;
- protected originals;
- photo preparation;
- queue states;
- upload destination and sequencing;
- FAILED / UNCERTAIN rules;
- reconciliation;
- confirmed-success cleanup.

## Rollback

Revert the Step 4 commit to return to exact Step 3 baseline `5b88fa28d0b3f06860e61a0c630fdc73f471cb03`.

## Physical-device impact

None by itself. These tests reduce what must be rechecked manually on Samsung later; the physical gate remains limited to the repaired field behaviors that cannot be proven by emulator interaction alone.
