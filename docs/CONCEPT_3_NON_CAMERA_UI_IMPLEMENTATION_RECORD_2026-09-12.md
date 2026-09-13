# Concept 3 Non-Camera UI Implementation Record

## Current three-tab correction — 2026-09-13

Status: **IMPLEMENTED — AUTOMATED AND RENDERED REVIEW PASSED; SAMSUNG VISUAL GATE PENDING**.

Branch: `feat/concept-3-ui-makeover-20260912`; draft PR [#35](https://github.com/timbone72-CC/field-photo-prep/pull/35).

Exact tested runtime / APK source: `801e05f84e1bbc2cd0f308dc77bae1cd43d0ef87`.
Last application-runtime change: `c9de04d212f9df2062f47d8b639f8b2ef4d06f7f`; the subsequent tested commit corrects verification capture only. Final records are a later documentation-only commit; runtime has not changed after verification.

The operator's correction scope explicitly covers the three separate Home, Work Orders and Photos tabs. The supplied **Concept 3 — Hybrid Field App** image governs visual interpretation. Continue the existing implementation rather than restarting from main. The original automated pass below did not establish visual fidelity; this pass includes inspection of the actual rendered activities.

### Coordinated result

- Shared 48dp app bars / 17sp titles; calmer 14sp medium property and work-order text; 10dp card corners; consistent light/dark proportions.
- Home: secondary 48dp pale storage/status strip, contrasting green connected dot/refresh icon, Properties/count on one line, compact blue New Address visual within a 48dp touch target, property cards starting at 60dp with compact vector icons.
- Work Orders: real property and selected-order context, compact selectable orders, Photos action, dated creation and quiet maintenance/reuse controls. Content-sized list replaces the fixed 260dp blank reservation; existing actions and safety owners remain unchanged.
- Photos: real context, blue 48dp Open Camera, 52dp local thumbnails, lighter 12sp statuses, explicit checkboxes/count, Select All Ready/Clear, sticky green 48dp Upload Selected. Individual/retry/UNCERTAIN controls retain their existing behavior.
- One shared 56dp bottom navigation uses 20dp icons and 11sp single-line labels, a flat selected state, and existing system-bar inset handling. Photos → Home now explicitly returns to Home; Photos → Work Orders uses the existing saved-property selection path.
- Property-only display cleanup removes the reported terminal PRESSURE TEST metadata from numeric property labels. Work-order PRESSURE TEST labels remain intact. Duplicate display labels retain short provider-ID disambiguators. Stored names and IDs are untouched. Free-form address naming has no general metadata grammar, so ambiguous address tokens are preserved rather than guessed away.

`CameraCaptureActivity`, camera controls/resources, Drive/provider identity, SAF permissions, folder persistence, protected originals, preparation, queue/attempt sequencing, retry/reconciliation, duplicate/create/reuse/Clear & Reuse, exact stored destination binding and confirmed-success cleanup owners are unchanged. No fake runtime data/features or Google Drive integration impact.

### Final verification and actual rendered review

[Android CI run 34732932338](https://github.com/timbone72-CC/field-photo-prep/actions/runs/34732932338) — **PASS**, exact head `801e05f84e1bbc2cd0f308dc77bae1cd43d0ef87`.

- Unit tests: **139 passed**, zero failures/errors/skips.
- Internal debug build: **PASS**.
- Stable signer verification: **PASS**; certificate SHA-256 `2c0a9616fd819333ed98b33593fbe597e120e95936103c6b7a76270e985d3fba`.
- Full instrumentation: **8 passed**, zero failures/skips; includes real-activity rendering/selection/navigation and Concept 3 structure alongside existing photo preparation/upload coverage.
- Additional dark and 1.3-font UI runs: **2 passed each**, explicitly executing both UI tests and requiring all three newly named screenshots.
- Internal launch smoke: **PASS**.

Rendered evidence artifact: `10309898268`, `concept-3-rendered-verification`; archive SHA-256 `c7e4280430c3ea5314e0cb8c26f3d3ba7988c52c5701a8b83e120de7b148935c`.

Visually inspected all **nine** final PNGs: Home / Work Orders / Photos in light, dark, and light at 1.3 font scale, API 35 at 1080×2400 / 440dpi (approximately 393dp wide). Review accepted the smaller hierarchy, quiet storage strip, dense property/order rows, consistent green/blue actions, local thumbnail/check/count/upload presentation, one-line icon navigation and clear system-bar spacing. Larger font preserves readable wrapping and compact navigation. Initial rendered review led to lighter Photos statuses, flat navigation and a contrasting connected dot before this final gate.

These are actual production activities/adapters/image decoding with isolated AndroidTest-only folder/queue/JPEG fixtures. No provider was read/written; fixtures are not evidence of real Samsung/Google Drive behavior. Maintenance/creation controls below a long list remain scrollable and use the existing owners.

### One final internal APK / remaining Samsung gate

APK artifact: `10310476852`, `field-photo-prep-internal-apk`; archive SHA-256 `916b1a317a9ad187890eca0234568fa296a208982622e0691bb29e0cc01cfa22`.

Staged filename: `Field-Photo-Prep-Concept-3-Correction.apk`.
APK SHA-256: `4d009ad04e539bc054e8b8513780b9f2f94a939d2ac72424e843284a54fb1ea2`.
Downloaded archive digests and the APK certificate were independently verified.

Install over the existing signed internal app; do not clear app data. Check compact Home with real properties, existing property/order selection and Photos context, real thumbnails/checkbox/count/sticky upload, existing Open Camera handoff, and all three tabs against Samsung status/navigation bars at normal phone settings. No destructive Drive experiment or camera redesign regression gate is required solely for this presentation correction. **Do not merge until operator Samsung approval.**

## Original implementation record — 2026-09-12 (historical)

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
