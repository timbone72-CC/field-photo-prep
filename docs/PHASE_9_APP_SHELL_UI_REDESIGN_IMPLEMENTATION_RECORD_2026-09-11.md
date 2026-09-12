# Phase 9 — App Shell & Field UI Redesign — Implementation Record

Date: 2026-09-11

Status: **IMPLEMENTED — FINAL AUTOMATION AND PHYSICAL UI SMOKE PENDING**

Branch: `feat/phase-9-app-shell-redesign`

Rollback baseline: `e0e46321893b44e2755d4cc842edca4ccd7c2974`

## Problem

The proven Field Photo Prep workflow worked correctly but the non-camera screens still looked and behaved like stacked development controls. The operator approved a more finished native Android presentation while explicitly locking the already-approved CameraX camera layout.

## Approved result

Phase 9 keeps the same workflow ownership and actions but presents them through a Material 3 / Samsung One UI-inspired shell:

- Addresses/Home is a property-focused screen with a Drive connection card, clear address rows, a secondary refresh action, and one primary **New Address** action.
- Work Orders presents the selected address as context, existing work orders as rows/cards, the selected work order clearly, one grouped new-work-order surface, and maintenance/reuse tools in a secondary area.
- Photos presents the immutable upload destination as context, one prominent **Open Camera** action, clean photo rows with user-facing states, explicit selection controls, and one prominent **Upload Selected (N)** action.
- Individual photo fallback actions remain available without being given the same visual weight as the normal batch workflow.
- Light/dark palettes use the same component hierarchy.
- The CameraX camera layout is not restyled or reorganized by Phase 9.

## Classification

Level 2 — presentation/navigation feature.

No persisted-data schema, queue state machine, Drive permission, provider/document identity, folder create/reuse decision, upload/retry/reconciliation rule, cleanup rule, signing identity, or deployment behavior is intentionally changed.

## Runtime ownership

### `MainActivity.java`

Presentation was reorganized around task-focused Addresses and Work Orders surfaces. Existing methods continue to own the same actions:

- master-folder picker/access;
- address refresh/create/reuse;
- exact-duplicate handling;
- work-order selection/create/reuse;
- empty-folder reuse;
- destructive Clear & Reuse confirmation/revalidation;
- transition to the selected work order's photo screen.

The Drive operation bodies and fail-closed checks remain in the same activity methods. The redesign changes surrounding view construction and presentation only.

### `PhotoCaptureActivity.java`

No queue/upload method was rewritten for Phase 9. The existing photo screen still creates and owns the same controls/listeners and remains the source of truth for:

- CameraX launch after durable capture reservation;
- automatic/manual preparation eligibility;
- batch selection eligibility;
- batch ordering and stop rules;
- individual upload;
- UNCERTAIN reconciliation;
- confirmed cleanup;
- local discard eligibility.

### `PhotoScreenDecorator.java`

A narrow presentation adapter runs only for `PhotoCaptureActivity` after its existing `onCreate` has completed. It reuses the exact already-wired View instances and reorganizes/styles them. It does not receive `PendingPhotoStore`, `DriveClient`, `PhotoUploadCoordinator`, or queue ownership and cannot perform a Drive write.

It also maps the existing operator-visible queue labels to shorter presentation labels such as `Ready to upload`, `Uploading`, `Uploaded`, and `Needs attention`; the underlying persisted enum/state remains unchanged.

Dynamic photo rows are styled as they are recreated by the existing `renderPhotoList` path.

### `FieldPhotoPrepApplication.java`

Existing startup queue recovery/automatic preparation remains unchanged. Phase 9 additionally registers an activity lifecycle presentation callback that invokes `PhotoScreenDecorator` only for `PhotoCaptureActivity`.

`CameraCaptureActivity` is deliberately excluded.

### Resources / theme

- Material/AppCompat dependencies provide the non-camera design system.
- `Theme.FieldPhotoPrep` is assigned only to `MainActivity` and `PhotoCaptureActivity`.
- The application-level theme remains the pre-Phase-9 theme so the locked `CameraCaptureActivity` inherits its existing presentation.
- Light and dark Phase 9 palettes are resource-based; no theme choice changes queue or Drive state.

## Camera design lock verification

`CameraCaptureActivity.java` is not part of the Phase 9 PR diff. Phase 9 does not modify its preview, Flash/Torch controls, shutter, Done control, zoom/lens controls, orientation handling, or protected-capture semantics.

## Protected behavior

The following must remain behaviorally identical:

- provider freshness/absence checks before Drive creation;
- duplicate folder choice rather than guessing;
- immutable selected work-order identity;
- protected original before camera bytes;
- multi-shot session destination binding;
- automatic preparation after durable WAITING;
- selectable batch snapshot/eligibility rules;
- one-at-a-time Drive writes;
- no later batch attempt after UNCERTAIN/unverified stop;
- no blind retry of UNCERTAIN;
- reconciliation against the original stored destination;
- local cleanup only after confirmed remote success;
- destructive Clear & Reuse revalidation/confirmation rules.

## Focused verification

Added `PhotoScreenDecoratorTest` to prove that presentation labels remove the technical local ID from normal photo rows while preserving operator-significant state meaning for:

- prepared WAITING → `Ready to upload`;
- UNCERTAIN → `Needs attention`;
- active reconciliation → `Checking upload`;
- UPLOADED → `Uploaded`;
- FAILED → `Upload failed — safe to retry`.

Existing repository tests remain the authority for Drive, identity, queue, preparation, batch, retry, reconciliation, cleanup, and camera behavior.

## Known implementation boundary

`PhotoScreenDecorator` intentionally validates the expected legacy photo-screen view shape before reorganizing it. If that owned screen structure changes later, decoration fails closed rather than guessing about unrelated Views. A future photo-screen refactor should either update the expected presentation contract or move the visual hierarchy directly into the activity in a separately reviewed change.

## Physical smoke gate

After one final successful CI run on the exact runtime head, install one internal APK on the primary Samsung phone and verify only the changed visual surface:

1. redesigned Addresses screen launches and existing safe address opens;
2. redesigned Work Orders screen opens an existing safe work order without a Drive create;
3. redesigned Photos screen renders current queue state and selection controls;
4. **Open Camera** enters the already-approved locked camera UI with no layout change;
5. **Done** returns to the redesigned Photos screen;
6. enabled/disabled selection/upload controls look correct for one existing non-destructive state.

No Drive write is required solely for this Level 2 presentation gate.

## Rollback

Revert the Phase 9 PR to `e0e46321893b44e2755d4cc842edca4ccd7c2974`. No migration or Drive repair is required because Phase 9 introduces no persisted schema or remote identity change.
