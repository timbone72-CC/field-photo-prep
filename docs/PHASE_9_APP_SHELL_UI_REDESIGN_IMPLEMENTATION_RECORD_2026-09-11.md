# Phase 9 — App Shell & Field UI Redesign — Implementation Record

Date: 2026-09-11

Status: **STAGED — AUTOMATED PASS, PHYSICAL UI SMOKE PENDING**

Branch: `feat/phase-9-app-shell-redesign`

PR: #32 — `Phase 9 app shell and field UI redesign`

Rollback baseline: `e0e46321893b44e2755d4cc842edca4ccd7c2974`

Exact tested runtime head: `25e20943d3fd00f804de94a082fc8b7b24858c49`

Android CI: run `34695114644` — **PASS**

Artifact: `field-photo-prep-internal-apk` — ID `10298048411`

Artifact digest: `sha256:0ccc469d067849449ed2d38e1b24fbf09a2185c8718bf5de6719c310262f1f70`

Staged APK SHA-256: `848a7e557441fb98b30f694007b03fc35448b8d8079dac1b9372c1c7aa80977e`

## Problem

The proven Field Photo Prep workflow worked correctly but the non-camera screens still looked and behaved like stacked development controls. The operator approved a finished native Android presentation and then selected **Concept 3 — Hybrid Field App** as the final Phase 9 direction. The already-approved CameraX camera layout remains locked.

## Approved Concept 3 result

Concept 3 uses the polished hierarchy of a normal Android app without inventing features the current product does not have:

- a field-green app accent for normal workflow actions;
- blue reserved for the camera-specific primary action;
- compact headers, rounded cards, clear status hierarchy, and less technical clutter;
- Addresses/Home focused on Drive readiness, properties, and one strong **New Address** action;
- Work Orders focused on the selected property, selected work order, creation, existing work orders, and quieter maintenance tools;
- Photos focused on **Open Camera**, compact photo/status rows, selection, and a persistent green **Upload Selected (N)** action at the bottom;
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

It does not receive `DriveClient`, `FolderPrefs`, queue objects, or upload coordinators and performs no Drive operation.

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

The Concept 3 presentation adapter reuses the exact already-wired views created by `PhotoCaptureActivity` and changes only their hierarchy/presentation:

- `Photos` header with current address/work-order context;
- blue **Open Camera** action;
- compact user-facing photo-state rows;
- existing selection controls;
- existing `uploadBatchButton` reparented into a persistent bottom green action bar while retaining the same listener and enabled-state ownership;
- individual fallback actions grouped below the normal batch workflow.

It does not receive `PendingPhotoStore`, `DriveClient`, `PhotoUploadCoordinator`, or queue ownership and cannot perform a Drive write.

User-facing queue labels remain presentation mappings only, such as `Ready to upload`, `Uploading`, `Uploaded`, and `Needs attention`; the persisted enum/state is unchanged.

### `FieldPhotoPrepApplication.java`

Existing startup queue recovery and automatic preparation remain unchanged.

Phase 9 registers presentation callbacks for only `MainActivity` and `PhotoCaptureActivity`. Decoration now runs from `onActivityResumed`, after each owned activity has completed building its content view. Each decorator validates its expected view shape and fails closed rather than guessing.

`CameraCaptureActivity` is deliberately excluded from all Phase 9 decoration.

### Resources / theme

- Material/AppCompat dependencies provide the non-camera design system.
- `Theme.FieldPhotoPrep` is assigned only to `MainActivity` and `PhotoCaptureActivity`.
- the application-level theme remains the pre-Phase-9 theme so locked `CameraCaptureActivity` keeps its existing presentation;
- light/dark Concept 3 palettes are resource-based;
- theme selection changes no queue or Drive state.

## Camera design lock verification

`CameraCaptureActivity.java` is not part of the Phase 9 PR diff. Phase 9 does not modify its preview, Flash/Torch controls, shutter, Done control, zoom/lens controls, portrait/landscape behavior, or protected-capture semantics.

## Protected behavior

The following remain behaviorally owned by the previously proven runtime:

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

## Automated evidence

Exact runtime `25e20943d3fd00f804de94a082fc8b7b24858c49` passed Android CI run `34695114644`:

- unit tests — PASS;
- internal debug build — PASS;
- stable test APK signer verification — PASS;
- emulator KVM setup — PASS;
- instrumented image tests and internal launch/UI smoke — PASS;
- internal APK artifact upload — PASS.

`PhotoScreenDecoratorTest` verifies the friendly operator labels for prepared, UNCERTAIN, reconciliation, uploaded, and retry-safe failed states.

`Phase9UiInstrumentedTest` launches the real `PhotoCaptureActivity` with a disposable test address/work-order preference and verifies the Concept 3 Photos surface still contains the selected work order, **Open Camera**, and the existing **Upload Selected (N)** action.

An earlier Phase 9 instrumentation attempt exposed that activity lifecycle decoration was running before the owned content view was ready. The final runtime moves presentation decoration to `onActivityResumed`; the exact resulting runtime is the one that passed the full CI suite above.

## Known implementation boundary

The presentation decorators intentionally validate the expected owned screen structure before reorganizing it. If a future screen refactor changes that structure, decoration fails closed rather than guessing about unrelated views. A later UI architecture rewrite can replace this adapter approach in a separately reviewed phase.

## Physical smoke gate

Install the staged internal APK on the primary Samsung phone and verify the changed presentation once:

1. Addresses/Home launches with the Concept 3 field-app hierarchy and readable controls;
2. an existing safe address/work order can be opened without creating new Drive data;
3. Photos shows the blue **Open Camera** action, compact photo state/list presentation, and bottom green **Upload Selected (N)** action;
4. **Open Camera** enters the already-approved locked camera UI with no camera layout change;
5. **Done** returns to the redesigned Photos screen;
6. the changed non-camera layout has no clipped, overlapping, or inaccessible controls on the Samsung phone.

No Drive write is required solely for this Level 2 presentation gate.

## Rollback

Revert the Phase 9 PR to `e0e46321893b44e2755d4cc842edca4ccd7c2974`. No migration or Drive repair is required because Phase 9 introduces no persisted schema or remote identity change.
