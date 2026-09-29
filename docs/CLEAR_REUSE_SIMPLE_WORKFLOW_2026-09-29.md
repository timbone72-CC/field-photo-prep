# Clear & Reuse Simple Workflow — Build State

Date: 2026-09-29
Status: LEVEL 3 — IN PROGRESS
Branch: `fix/clear-reuse-photo-only-20260929`
Rollback: `26d4f8fc7a25469ac6f1f10e4ed67fff339cf4ba`

## Goal

Make Clear & Reuse a direct field workflow:

**Select work order → Clear & Reuse → choose later date → confirm.**

The selected dated work order supplies its own work-order name. The app keeps the same provider folder identity and address parent, clears only direct Drive image/photo files for that work order, preserves non-photo items, changes the date portion, and starts the next capture at `001`.

## Scope and owners

- `MainActivity`: user flow and existing reuse orchestration.
- `DriveClient`: direct-child image classification.
- `WorkOrderFolderName`: safe selected-name/date derivation.
- `screen_work_orders.xml`: direct Clear & Reuse entry point.
- Existing `PendingPhotoStore` occurrence reset remains authoritative.

No queue schema, upload destination, camera, Supabase, auth, workspace, company, or address behavior changes.

## Safety / failure behavior

Provider freshness, exact folder identity, requested-name collision checks, mutation authorization, and protected local-photo checks remain required. Confirmation is bound to the direct-photo identity set. Changed photo state stops the operation. A partial remote result blocks further Drive writes until refresh/inspection.

## Verification boundary

Required before merge:
- focused unit/UI coverage for selected-name dating, image classification, photo-snapshot stability, and simplified entry point;
- complete Android CI on final runtime head;
- disposable physical-device Google Drive check proving photo-only cleanup, preserved non-photo content, same provider identity/parent, new date, and next sequence `001`.

## Current progress

- Branch created from governed main.
- Selected work-order name/date derivation implemented.
- Direct image/photo classification implemented.
- Clear & Reuse UI moved out of hidden maintenance actions.
- Separate empty-folder control removed from the user-facing flow.
- Main Clear & Reuse path now uses selected WO → date → confirmation.
- Product, integration, and regression contracts reconciled.

## Remaining

- finish focused automated coverage;
- bump internal/runtime version for install identification;
- open PR and pass CI;
- physical-device disposable Drive reality check;
- explicit operator approval before merge.

Explicit Level-3 merge approval: **NOT YET GRANTED**.
