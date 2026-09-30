# Clear & Reuse Simple Workflow — Build State

Date: 2026-09-29
Status: LEVEL 3 — MERGE APPROVED
Branch: `fix/clear-reuse-photo-only-20260929`
PR: #92 `Fix Clear & Reuse workflow and limit cleanup to photos`
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
- complete Android CI on the final branch/PR head;
- disposable physical-device Google Drive check proving photo-only cleanup, preserved non-photo content, same provider identity/parent, new date, and next sequence `001`;
- explicit operator Level-3 merge approval after all required evidence passes.

## Physical-device Google Drive reality gate — PASS

Completed on 2026-09-29 through the actual Android SAF/Google Drive provider path using the disposable property `99999 FPP UNIQUE CREATE TEST`.

Fixture and evidence:
- selected work order: `TREE TRIM 3 TEST - 2026-09-18`;
- fixture contained three FPP-uploaded photos plus one non-photo Google document (`Test 2`);
- confirmation correctly reported **3** Drive photos to remove and **1** other non-photo item to keep;
- confirmed Clear & Reuse removed the three photos and preserved `Test 2`;
- the work-order folder was renamed to `TREE TRIM 3 TEST - 2026-09-30`;
- Drive metadata verified that the renamed work order retained the same provider folder identity and same address parent;
- the next captured/uploaded photo in the reused occurrence was visibly named beginning `001_field-photo-`, proving the new occurrence restarted at sequence `001`.

Result: **PASS**. The required real-provider photo-only cleanup, preserved non-photo content, stable folder identity/parent, date reset, and sequence reset were all observed.

## Current progress

- Branch created from governed main.
- Selected work-order name/date derivation implemented.
- Direct image/photo classification implemented.
- Clear & Reuse UI moved out of hidden maintenance actions.
- Separate empty-folder control removed from the user-facing flow.
- Main Clear & Reuse path now uses selected WO → date → confirmation.
- Product, integration, and regression contracts reconciled.
- Focused unit/UI coverage added for the changed behavior.
- Runtime identification bumped to version `0.28.1` / versionCode `38`.
- Draft PR #92 opened on the authoritative branch.
- Physical-device Google Drive reality gate passed and recorded above.

## Remaining

- pass the required GitHub Android CI on the exact final PR head;
- merge PR #92 after the exact final head passes required GitHub checks.

Explicit Level-3 merge approval: **GRANTED 2026-09-29**.
