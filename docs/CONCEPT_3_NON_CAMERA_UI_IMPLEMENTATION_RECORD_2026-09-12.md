# Concept 3 Non-Camera UI Implementation Record

Date: 2026-09-12

Status: **IMPLEMENTED — AUTOMATED GATE PASSED; SAMSUNG VISUAL GATE PENDING**

Branch: `feat/concept-3-ui-makeover-20260912`

PR: #35 — `Implement Concept 3 non-camera UI makeover`

Rollback baseline: `e77b83bf07505cc586fb8766cb8623db51e35b7`

Exact final tested branch head / APK source: `6708c351173874b14abfd5fb8cba90b91b9e4a59`

Last runtime-code change before final verification: `7fc6faee634ce3da1fa91050a2f8673182efd1da`

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

The supplied Concept 3 screenshot is the visual authority. Fake route, map, schedule, or work-order data from the mockup was not copied.

- Home uses the Concept 3 compact app bar, strong green real-storage/status card, dense property cards, blue New Address action, and persistent bottom navigation.
- Work Orders uses a compact property context card, real selectable work-order cards, prominent blue Photos action, compact new-dated-work-order controls, and maintenance actions collapsed behind a quieter control.
- Photos uses a compact work-order context card, prominent blue Open Camera action, real local thumbnails when bytes exist, explicit checkbox selection, compact human-facing states, and a sticky green Upload Selected action.
- Bottom navigation contains only real Field Photo Prep destinations: Home, Work Orders, Photos.
- Light and dark resource palettes and system-bar icon handling are provided.

## Read/write surfaces

This makeover introduces no new persisted schema and no new Drive write implementation. UI actions continue to call the existing `MainActivity` / `DriveClient` and `PhotoCaptureActivity` queue/upload owners.

Thumbnail decoding reads only existing local protected/prepared files for display and does not modify them.

The exact stored provider identities remain authoritative. Address normalization is display-only and never renames Drive folders.

## Focused coverage

- `Concept3UiStructureInstrumentedTest` verifies that all three purpose-built screens expose their real field actions and that Photos retains a full-width Upload Selected action.
- `PropertyDisplayNameTest` verifies display-only folder-name normalization.
- Existing repository unit and instrumented coverage continues to protect photo identity, queue, Drive, upload, retry, reconciliation, and camera behavior.

## Final automated verification

GitHub Actions run: `34730010336`

Result: **PASS**

Exact tested head: `6708c351173874b14abfd5fb8cba90b91b9e4a59`

Passed stages:

- full unit test suite;
- internal debug APK build;
- stable test APK signer verification;
- Android emulator instrumentation suite, including Concept 3 structural coverage;
- internal app launch smoke test;
- internal APK artifact upload.

Artifact ID: `10308258702`

Artifact archive digest: `sha256:aa47298119c14572c9454daa2318d7ebdfc67414e74b3d90015b71048e3a4b26`

Staged APK filename: `Field-Photo-Prep-Concept-3-Test.apk`

Staged APK SHA-256: `3e0dcb697aef55dc6de8705c62f6544bd8ee7b5db2fe39766190727ab77d8e50`

The final diff does not include `CameraCaptureActivity`.

## Remaining physical gate

One Samsung visual/workflow smoke remains. No destructive Drive create/delete/rename test and no camera-behavior regression gate are required solely for this non-camera presentation change.

Check only:

1. Home visually follows the Concept 3 direction and real properties remain readable/dense.
2. Open one existing property; Work Orders follows Concept 3 and selecting an existing work order still works.
3. Open Photos; thumbnails/statuses/selection and the sticky Upload Selected action render correctly with real queued photos where available.
4. Confirm Open Camera still reaches the already-approved camera surface; do not retest camera controls unless a defect is visible.
5. Confirm normal Back/bottom navigation feels correct and no screen overlaps the Samsung status/navigation areas.

If those observations pass, the UI branch is ready for final merge review. If a visual defect appears, correct only the affected non-camera surface and repeat only that failed observation after required automated verification.
