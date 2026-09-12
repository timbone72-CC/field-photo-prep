# Phase 9 — App Shell & Field UI Redesign

Date: 2026-09-11

Status: **APPROVED FOR IMPLEMENTATION**

Branch: `feat/phase-9-app-shell-redesign`

## Goal

Make Field Photo Prep look and feel like a finished Android field app without changing the proven Drive, identity, queue, preparation, upload, retry, reconciliation, cleanup, or camera-capture safety behavior.

The approved visual direction is a modern native Android shell inspired by Material 3 and Samsung One UI: large clear surfaces, card/list organization, one obvious primary action per screen, quieter secondary actions, compact status chips, and technical details hidden from the normal field workflow unless needed.

## Change classification

**Level 2 — normal UI/navigation redesign.**

This phase changes presentation, layout, navigation affordances, status presentation, and component styling. It does not authorize changes to persisted schemas, Drive permissions, provider identity, folder creation/reuse semantics, upload/retry rules, photo cleanup rules, or deployment/signing behavior.

If implementation would alter any Level 3 behavior, stop and split that work into a separate governed change.

## Camera design lock

The current in-app camera layout is **locked for Phase 9**.

Protected camera surface:
- dominant preview;
- compact Flash/Torch controls;
- large round shutter;
- obvious **Done** action;
- photo count;
- quick zoom/lens presets;
- pinch-to-zoom and temporary slider;
- portrait/landscape reflow;
- current CameraX capture/identity behavior.

Phase 9 must not redesign or restyle the camera screen. Camera changes require a separate approved camera-specific change unless a blocking defect is discovered.

## Approved screen model

### 1. Addresses / Home

Primary purpose: get the operator into the correct property quickly.

Approved presentation:
- top app bar with `Field Photo Prep`;
- Drive/master-folder status card;
- clean address list using card/list-row presentation;
- obvious **New Address** / address-folder action;
- refresh/reconnect actions presented as secondary controls rather than equal-weight full-width buttons;
- status/error messages shown clearly without exposing provider IDs by default.

Search may be added only as a local filtering convenience over the already-loaded address list. It must not redefine Drive discovery or creation semantics.

### 2. Work Orders

Primary purpose: choose the existing work occurrence or create/reuse the correct dated work order.

Approved presentation:
- selected address as the screen title/context;
- existing work orders as clear rows/cards;
- selected work order visibly highlighted;
- one obvious **New Work Order** / use-create action;
- date and work-order-name entry grouped together;
- ordinary folder selection separated from maintenance/reuse tools;
- **Reuse Empty Folder** and **Clear & Reuse** moved into a clearly secondary `Manage`/maintenance area;
- destructive **Clear & Reuse** remains visually distinct and keeps its existing confirmation and safety semantics;
- **Photos** becomes the primary continuation action once a work order is selected.

### 3. Photos

Primary purpose: take photos, see what is ready, select a subset, and upload safely.

Approved presentation:
- selected work order and address as compact context at the top;
- large obvious **Open Camera** action;
- photo queue rendered as clean rows/cards with checkbox, user-facing status, and useful summary information;
- status labels such as `Preparing`, `Ready`, `Uploading`, `Uploaded`, `Failed`, and `Needs attention` mapped from existing queue state without changing that state;
- **Upload Selected (N)** remains the single batch action and stays prominent;
- `Select All Ready` and `Clear Selection` remain available but visually secondary;
- technical photo IDs, remote IDs, hashes, provider IDs, and deep troubleshooting text are hidden behind a details/diagnostic affordance during normal use;
- individual fallback actions such as manual prepare, individual upload, reconcile, and discard remain available when their existing state rules permit them, but they are not all shown as equal-weight primary buttons.

## Visual system

Use a small consistent native Android design system rather than hand-styling each screen independently:
- Material 3 compatible theme and components where practical;
- system light/dark support where it can be added without destabilizing the current app;
- consistent typography hierarchy;
- consistent 8/12/16/24 dp spacing rhythm;
- rounded cards/surfaces;
- one primary accent treatment;
- restrained status colors;
- minimum Android touch target sizing;
- standard back navigation and top-app-bar behavior;
- icons only where they improve recognition; text remains available for field clarity.

Do not add Compose solely for this redesign. Preserve the current Java app architecture and introduce only the minimum Material/AppCompat support needed for a maintainable finished UI.

## Ownership / expected files

Primary runtime owners:
- `MainActivity.java` — Addresses and Work Orders shell/presentation only;
- `PhotoCaptureActivity.java` — photo queue/review shell/presentation only;
- Android theme/resource files for colors, typography, dimensions, shapes, and component styles;
- `app/build.gradle` only for the minimal UI dependency needed for Material components;
- `AndroidManifest.xml` only for application theme assignment if needed.

Explicitly protected from normal Phase 9 edits:
- `CameraCaptureActivity.java` layout/interaction;
- `PendingPhotoStore` and queue record schema;
- Drive client/uploader/reconciler behavior;
- upload batch runner semantics;
- preparation pipeline;
- document-provider permissions and persisted folder identity;
- signing/build identity.

## Read surfaces

The redesigned screens may read exactly the same state the current screens already read:
- master-folder availability/access state;
- loaded Drive address/work-order folder lists;
- selected address/work order;
- current work-order date/name entry;
- current pending-photo records;
- current prepared/upload/cleanup state;
- existing user-facing status/error strings.

## Write surfaces

UI actions must call the same existing owning methods/actions. Phase 9 does not create a second implementation of Drive, queue, photo preparation, or upload behavior.

## Implementation slices

### 9A — Design system and app shell
- add the minimal Material/AppCompat dependency and theme resources;
- establish app background, surfaces, primary action style, secondary action style, status badges/chips, card shape, typography, and spacing;
- apply system bars/window treatment appropriate to the new shell;
- do not touch camera layout.

### 9B — Addresses + Work Orders
- redesign `MainActivity` around the approved screen model;
- preserve every existing Drive action and safety guard;
- keep exact duplicate handling, freshness rules, folder reuse logic, and selected provider identity untouched.

### 9C — Photos
- redesign `PhotoCaptureActivity` around the approved photo-queue model;
- preserve batch eligibility, sequencing, retry, reconciliation, and cleanup behavior exactly;
- keep technical details available but de-emphasized.

### 9D — Field polish and accessibility
- verify long addresses/work-order names remain readable;
- verify portrait layout and common font scaling do not hide primary actions;
- verify enabled/disabled/error states are visually clear;
- verify no new visual affordance implies an action the underlying state does not allow.

## Focused verification

Automated/focused checks should prove the redesign did not alter owning logic:
- existing folder create/reuse/duplicate-prevention tests remain unchanged and green;
- existing queue, batch, preparation, upload, retry, reconciliation, and cleanup tests remain unchanged and green;
- any new pure UI-state formatter/helper gets focused unit coverage;
- launch/instrumentation smoke verifies the redesigned Activities inflate/render and primary navigation still opens the existing photo and camera flows.

Final gate: complete Android CI once on the exact final Phase 9 runtime head.

## Physical smoke gate

One small primary-phone UI gate after automated PASS:
- app launches into the redesigned Addresses screen;
- existing safe address opens without creating anything;
- existing safe work order opens without creating anything;
- Photos screen shows existing queue state correctly;
- Open Camera enters the already-approved locked camera UI unchanged;
- Done returns to the redesigned Photos screen;
- one non-destructive selection/upload-control state is visually correct.

No Drive write is required solely to prove a presentation redesign unless the runtime diff unexpectedly touches Drive behavior.

## Rollback

Rollback the Phase 9 PR to the main commit immediately before the redesign. No data migration is required because Phase 9 adds no persisted schema or Drive identity changes.

## Acceptance standard

Phase 9 is complete when the non-camera app feels like one coherent Android product rather than a set of stacked development controls, while the previously proven workflow and all safety contracts remain behaviorally unchanged.
