# Concept 3 Non-Camera UI Implementation Record

Date: 2026-09-12

Status: IMPLEMENTED ON FEATURE BRANCH — AUTOMATED VERIFICATION REQUIRED

Branch: `feat/concept-3-ui-makeover-20260912`

Rollback baseline: `e77b83bf07505cc586fb8766cb8623db51e35b7`

## Scope

Replace the rejected non-camera development-style presentation with the operator-selected Concept 3 Hybrid Field App direction while preserving existing Field Photo Prep behavior.

Changed presentation surfaces:

- Home / Properties
- Work Orders
- Photos / upload queue

Protected and intentionally untouched:

- `CameraCaptureActivity`
- CameraX capture behavior
- protected-original semantics
- Drive provider identities and master-tree permission model
- address/work-order duplicate, create, reuse, and Clear & Reuse behavior
- queue state machine
- upload/retry/UNCERTAIN/reconciliation behavior
- confirmed-success cleanup semantics

## Concept 3 mapping

The reference image is visual authority, but fake route/map/schedule features are not copied.

- Home uses the Concept 3 compact app bar, strong green status/context card, dense property cards, blue primary action, and persistent bottom navigation.
- Work Orders uses a compact property context card, real selectable work-order cards, blue Photos action, compact new-dated-work-order controls, and collapsed maintenance actions.
- Photos uses a compact work-order context card, prominent blue Open Camera action, real local thumbnails when bytes exist, explicit checkbox selection, compact status rows, and sticky green Upload Selected action.
- Bottom navigation contains only real Field Photo Prep destinations: Home, Work Orders, Photos.

## Read/write surfaces

This makeover introduces no new persisted schema and no new Drive write implementation. UI actions continue to call the existing MainActivity/DriveClient and PhotoCaptureActivity queue/upload owners.

Thumbnail decoding reads only existing local protected/prepared files for display and does not modify them.

## Verification

Focused coverage: `Concept3UiStructureInstrumentedTest` plus the existing property display tests.

Final requirement before staging: one complete Android CI pass on the exact executable runtime head. Physical Samsung acceptance remains the final visual gate.
