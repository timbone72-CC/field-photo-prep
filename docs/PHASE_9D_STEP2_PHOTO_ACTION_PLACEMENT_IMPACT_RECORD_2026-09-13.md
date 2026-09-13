# Phase 9D Step 2 — Photo Action Placement Impact Record

Date: 2026-09-13
Status: **IMPLEMENTED — AUTOMATED VERIFICATION PENDING**
Branch: `feat/concept-3-ui-makeover-20260912`
PR: #35
Rollback baseline: `901c12a97266636057159ce7637669db3f2ee98e`

## Problem

After Step 1, each photo row has a working `⋯` action menu, but the old selected-photo panel still placed Prepare / Upload / Reconcile / Discard controls after the full photo list. On a long field batch that duplicated the new action path and could require unnecessary scrolling.

## Step 2 scope

Retain the selected-photo panel only as a compact details surface and remove its duplicate action stack from normal use.

The panel now:

- sits above the scrollable photo list instead of after it;
- shows the existing selected-photo identity/state and preparation/upload detail text;
- keeps the four existing action buttons visually hidden;
- leaves those buttons wired as the existing action owners used by the row `⋯` popup.

`PhotoRowActionsView` now reads the enabled state of those existing hidden action owners rather than requiring them to be visually visible. Choosing a popup item still delegates to the same button handler as before.

## Runtime files changed

- `app/src/main/res/layout/screen_photos.xml`
  - moves `photos_selected_panel` above the scrollable list;
  - keeps only selected-photo details visible;
  - hides the duplicate Prepare / Upload / Reconcile / Discard buttons.
- `app/src/main/java/com/inandout/fieldphotoprep/PhotoRowActionsView.java`
  - treats enabled hidden owner buttons as valid menu actions.
- `app/src/androidTest/java/com/inandout/fieldphotoprep/PhotoRowActionsViewInstrumentedTest.java`
  - verifies the exact photo is still selected;
  - verifies the details panel is outside the long list;
  - verifies duplicate buttons are hidden while the existing upload owner remains enabled for a prepared waiting photo.

## Protected behavior

Unchanged:

- `PhotoCaptureActivity` action methods and queue-state decisions;
- camera behavior;
- SAF master-tree identity and persisted permissions;
- protected originals and preparation ownership;
- immutable photo destination identity;
- upload sequencing;
- FAILED / UNCERTAIN retry rules;
- reconciliation and duplicate protection;
- confirmed-success cleanup.

No Drive, queue, persistence, schema, camera, or action-owner logic is changed in Step 2.

## Field result

The operator no longer needs to scroll below the full photo list to find per-photo actions. The row `⋯` is the normal action doorway, while the compact selected-photo panel remains immediately visible as status/detail context.

## Rollback

Revert the Step 2 commit to return to exact Step 1 baseline `901c12a97266636057159ce7637669db3f2ee98e`.

## Physical check after the repair set

On Samsung:

1. tap a photo-row `⋯` and confirm the contextual actions appear at that row;
2. confirm the selected-photo detail card appears above the list, not after the final photo;
3. confirm the old full-width per-photo action buttons are no longer visible;
4. confirm batch selection and `Upload Selected` remain unchanged.

No destructive Drive or camera retest is required solely for this placement repair.
