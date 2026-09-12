# Phase 9 — App Shell & Field UI Redesign — Implementation Record

Date: 2026-09-11

Status: **CORRECTION STAGED — REPLACEMENT PHYSICAL UI SMOKE PENDING**

Branch: `feat/phase-9-app-shell-redesign`

PR: #32 — `Phase 9 app shell and field UI redesign`

Rollback baseline: `e0e46321893b44e2755d4cc842edca4ccd7c2974`

Exact thumbnail-correction runtime head: `bb8dd1d4ec02a05de8fdfce1ac4a027bb79271e2`

Android CI: run `34695881103` — **PASS**

Artifact: `field-photo-prep-internal-apk` — ID `10298781352`

Artifact digest: `sha256:5088e933ffd4df262ce7c80e1f75858ee6a90b6e01f0b8a464374e14b88a88e8`

Staged APK SHA-256: `4b60155d9c45d76f2d3e9e4c7fff62a8030cf3e1ae6c88e739ba36fc2ea2d469`

## Problem

The proven Field Photo Prep workflow worked correctly but the non-camera screens still looked and behaved like stacked development controls. The operator approved a finished native Android presentation and then selected **Concept 3 — Hybrid Field App** as the final Phase 9 direction. The already-approved CameraX camera layout remains locked.

The first physical Concept 3 build was rejected because the Photos screen was still too close to the old control layout and, specifically, did not show the real photo thumbnails that were part of the approved concept. That device feedback is treated as a failed Phase 9 presentation observation, not as a new feature request.

## Approved Concept 3 result

Concept 3 uses the polished hierarchy of a normal Android app without inventing features the current product does not have:

- a field-green app accent for normal workflow actions;
- blue reserved for the camera-specific primary action;
- compact headers, rounded cards, clear status hierarchy, and less technical clutter;
- Addresses/Home focused on Drive readiness, properties, and one strong **New Address** action;
- Work Orders focused on the selected property, selected work order, creation, existing work orders, and quieter maintenance tools;
- Photos focused on **Open Camera**, real local photo thumbnails, compact photo/status cards, selection, and a persistent green **Upload Selected (N)** action at the bottom;
- technical destination identity remains available but visually de-emphasized;
- light/dark palettes share the same hierarchy;
- no fake Maps, route, notes, schedule, or other dead navigation was added from the visual concept;
- CameraX camera layout and controls remain unchanged.

## Classification

Level 2 — presentation/navigation feature.

No persisted-data schema, queue state machine, Drive permission, provider/document identity, folder create/reuse decision, upload/retry/reconciliation rule, cleanup rule, signing identity, or deployment behavior is intentionally changed.

## Runtime ownership

### `MainActivity.java`

The underlying Addresses and Work Orders actions remain owned by the same existing methods, including:

- master-folder picker/access;
- address refresh/create/reuse;
- exact-duplicate handling;
- work-order selection/create/reuse;
- empty-folder reuse;
- destructive Clear & Reuse confirmation/revalidation;
- transition to the selected work order's photo screen.

Drive operation bodies and fail-closed checks were not moved into the presentation layer.

### `MainScreenDecorator.java`

Concept 3 adds a narrow presentation-only decorator for `MainActivity`. It styles the already-built owned view hierarchy after the activity is resumed, including the compact header, Drive/status card, selected-work-order emphasis, card spacing, and field-green visual hierarchy.

It does not receive `DriveClient`, queue objects, or upload coordinators and performs no Drive operation.

### `PhotoCaptureActivity.java`

No queue/upload method was rewritten for Phase 9. The activity still owns the same controls/listeners and remains the source of truth for:

- CameraX launch after durable capture reservation;
- automatic/manual preparation eligibility;
- batch selection eligibility;
- batch ordering and stop rules;
- individual upload;
- UNCERTAIN reconciliation;
- confirmed cleanup;
- local discard eligibility.

### `PhotoScreenDecorator.java`

The Concept 3 presentation adapter reuses the existing already-wired photo workflow views. It builds the Photos header/context surface, blue **Open Camera** action, compact selection area, selected-photo actions, and persistent green **Upload Selected (N)** bottom bar.

It does not own upload or queue state.

### `Concept3PhotoEnhancer.java` / `PhotoThumbnailLoader.java`

The physical-device correction adds the missing real photo-card treatment:

- scans only the app's existing local pending-photo records for the selected work order;
- reads the protected original first and the prepared derivative second only for display;
- decodes a small in-memory thumbnail;
- applies EXIF orientation for display only;
- adds a compact thumbnail, photo label/time, and operator-facing state badge to the existing photo row;
- preserves the existing checkbox and photo-selection button/listener as workflow owners;
- never writes to, deletes, renames, compresses, or replaces a protected or prepared photo.

If a confirmed upload has already completed local cleanup, no full local image remains by design. In that case the row shows a neutral photo placeholder rather than fetching or recreating a Drive copy.

### `FieldPhotoPrepApplication.java`

Existing startup queue recovery/automatic preparation remains unchanged. Presentation decoration runs from `onActivityResumed`, after the owned activity content is ready. `CameraCaptureActivity` is explicitly excluded.

## Camera design lock verification

`CameraCaptureActivity.java` is not part of the Phase 9 runtime changes. Phase 9 does not modify its preview, Flash/Torch controls, shutter, Done control, zoom/lens controls, orientation handling, or protected-capture semantics.

## Protected behavior

The following remain behaviorally identical:

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

## Automated verification

The exact thumbnail-correction runtime `bb8dd1d4ec02a05de8fdfce1ac4a027bb79271e2` passed Android CI run `34695881103`, including:

- unit tests;
- internal debug build;
- stable test APK signer verification;
- existing instrumented image tests;
- Concept 3 Photos launch/UI smoke;
- internal app launch smoke;
- APK artifact upload.

A later instrumentation-only commit added an assertion that a real local field photo produces a thumbnail view. Subsequent GitHub Actions dispatches failed before job steps started; those infrastructure-level dispatch failures are not runtime evidence. The executable runtime itself did not change after the successful exact-runtime run above.

## Physical smoke gate

Install the replacement internal APK on the primary Samsung phone and verify only the changed visual surface:

1. Addresses/Home and Work Orders still present the Concept 3 field-green card hierarchy;
2. Photos shows actual thumbnails for local protected/prepared photos;
3. thumbnail rows show compact state badges and selection checkboxes;
4. **Open Camera** enters the already-approved locked camera UI with no layout change;
5. **Done** returns to the redesigned Photos screen;
6. the sticky **Upload Selected (N)** bar remains visible and follows existing enabled/disabled selection state.

No Drive write is required solely for this Level 2 presentation gate.

## Rollback

Revert the Phase 9 PR to `e0e46321893b44e2755d4cc842edca4ccd7c2974`. No migration or Drive repair is required because Phase 9 introduces no persisted schema or remote identity change.
