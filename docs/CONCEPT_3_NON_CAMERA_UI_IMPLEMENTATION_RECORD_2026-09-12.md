# Concept 3 Non-Camera UI Implementation Record

## Current three-tab correction — 2026-09-13

Status: **COMPLETE — AUTOMATED + SAMSUNG DEVICE PASS — MERGED**.

Branch: `feat/concept-3-ui-makeover-20260912`; PR #35 merged to `main` on 2026-09-13 as `66c307ede21e89ff4d0b7cc6ed00e30adb92582a`.

The original coordinated Concept 3 correction was automated-tested at `801e05f84e1bbc2cd0f308dc77bae1cd43d0ef87`. The later locked Phase 9D repair set corrected the concrete interaction defects found during review without changing the camera/Drive/queue core. Final Phase 9D runtime head `3a635d41cac6bb99802e352ea4ae890cbef499bb` passed Android CI run `34755337225`, then the focused Samsung Galaxy S21 gate passed all six repaired UI observations.

The operator's correction scope explicitly covered the three separate Home, Work Orders and Photos tabs. The supplied **Concept 3 — Hybrid Field App** image governed visual interpretation. The implementation continued the existing app rather than restarting the proven core.

### Coordinated result

- Shared 48dp app bars / 17sp titles; calmer 14sp medium property and work-order text; 10dp card corners; consistent light/dark proportions.
- Home: secondary 48dp pale storage/status strip, contrasting green connected dot/refresh icon, Properties/count on one line, compact blue New Address visual within a 48dp touch target, property cards starting at 60dp with compact vector icons.
- Work Orders: real property and selected-order context, compact selectable orders, Photos action, dated creation and quiet maintenance/reuse controls. Content-sized list replaces the fixed 260dp blank reservation; existing actions and safety owners remain unchanged.
- Photos: real context, blue 48dp Open Camera, 52dp local thumbnails, lighter 12sp statuses, explicit checkboxes/count, Select All Ready/Clear, sticky green Upload Selected. Individual/retry/UNCERTAIN controls retain their existing behavior.
- One shared 56dp bottom navigation uses 20dp icons and 11sp single-line labels, a flat selected state, and existing system-bar inset handling. Photos → Home explicitly returns to Home; Photos → Work Orders uses the existing saved-property selection path.
- Property-only display cleanup removes the reported terminal PRESSURE TEST metadata from numeric property labels. Work-order PRESSURE TEST labels remain intact. Duplicate display labels retain short provider-ID disambiguators. Stored names and IDs are untouched. Free-form address naming has no general metadata grammar, so ambiguous address tokens are preserved rather than guessed away.
- Phase 9D repaired the Photos row `⋯`, moved selected-photo context above the long list, made Home `⋯` reliably expose only **Change Drive**, added focused interaction coverage, expanded the photo checkbox target to 48dp, and removed the obsolete hidden work-order selector.

`CameraCaptureActivity`, camera controls/resources, Drive/provider identity, SAF permissions, folder persistence, protected originals, preparation, queue/attempt sequencing, retry/reconciliation, duplicate/create/reuse/Clear & Reuse, exact stored destination binding and confirmed-success cleanup owners were unchanged by the Concept 3 / Phase 9D UI repair work.

### Verification and rendered review

The coordinated rendered review passed on Android CI run `34732932338`, exact head `801e05f84e1bbc2cd0f308dc77bae1cd43d0ef87`.

- Unit tests: **139 passed**, zero failures/errors/skips at that correction checkpoint.
- Internal debug build: **PASS**.
- Stable signer verification: **PASS**; certificate SHA-256 `2c0a9616fd819333ed98b33593fbe597e120e95936103c6b7a76270e985d3fba`.
- Full instrumentation: **PASS**; included real-activity rendering/selection/navigation and Concept 3 structure alongside existing photo preparation/upload coverage.
- Additional dark and 1.3-font UI runs: **PASS**.
- Internal launch smoke: **PASS**.

Rendered evidence artifact: `10309898268`, `concept-3-rendered-verification`; archive SHA-256 `c7e4280430c3ea5314e0cb8c26f3d3ba7988c52c5701a8b83e120de7b148935c`.

All nine correction PNGs were visually reviewed: Home / Work Orders / Photos in light, dark, and light at 1.3 font scale, API 35 at 1080×2400 / 440dpi. Review accepted the smaller hierarchy, quiet storage strip, dense property/order rows, consistent green/blue actions, local thumbnail/check/count/upload presentation, one-line icon navigation and clear system-bar spacing.

The subsequent Phase 9D repair sequence had its own focused tests and full CI gates. Final Step 5 / Phase 9D runtime head `3a635d41cac6bb99802e352ea4ae890cbef499bb` passed Android CI run `34755337225`. The Samsung Galaxy S21 field gate then passed:

1. Home `⋯` exposed **Change Drive** and the Android folder picker opened.
2. Photo-row `⋯` exposed the correct state-safe action path.
3. Selected-photo details stayed above the scrolling photo list without the old bottom action stack.
4. The 48dp photo checkbox target was usable.
5. Visible Work Orders selection and **Open Photos** worked after removal of the hidden legacy picker.
6. Bottom navigation and overall visual layout showed no new regression.

That device evidence closed the Concept 3 / Phase 9D UI gate. PR #35 was then merged to `main` after operator approval.

Two concrete Samsung follow-ups discovered during that field pass were handled separately rather than being hidden inside PR #35:

- guarded multi-photo local discard — Level 3, automated PASS + Samsung PASS, merged through PR #36;
- Work Orders date readability — Level 2, automated PASS + Samsung PASS, merged through PR #37.

Final version reconciliation advanced the internal app to versionCode 18 / `0.13-field-ui-internal`; exact version-reconciled head `64339686ae0be08a2148b9b73390c35575e30584` passed Android CI run `34761274823` before PR #37 merged.

## Original implementation record — 2026-09-12 (historical)

Date: 2026-09-12

Status at that historical checkpoint: **IMPLEMENTED — AUTOMATED GATE PASSED; SAMSUNG VISUAL GATE PENDING**

Branch: `feat/concept-3-ui-makeover-20260912`

PR: #35 — `Implement Concept 3 non-camera UI makeover`

Rollback baseline: `e77b83bf07505cc586fb8766cb8623db51e35b7`

Exact final tested branch head / APK source at that checkpoint: `6708c351173874b14abfd5fb8cba90b91b9e4a59`

Last runtime-code change before that verification: `7fc6faee634ce3da1fa91050a2f8673182efd1da`

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

- Home uses the Concept 3 compact app bar, real storage/status presentation, dense property cards, blue New Address action, and persistent bottom navigation.
- Work Orders uses a compact property context card, real selectable work-order cards, prominent blue Photos action, compact new-dated-work-order controls, and maintenance actions collapsed behind a quieter control.
- Photos uses a compact work-order context card, prominent blue Open Camera action, real local thumbnails when bytes exist, explicit checkbox selection, compact human-facing states, and a sticky green Upload Selected action.
- Bottom navigation contains only real Field Photo Prep destinations: Home, Work Orders, Photos.
- Light and dark resource palettes and system-bar icon handling are provided.

## Read/write surfaces

This makeover introduced no new persisted schema and no new Drive write implementation. UI actions continued to call the existing `MainActivity` / `DriveClient` and `PhotoCaptureActivity` queue/upload owners.

Thumbnail decoding reads only existing local protected/prepared files for display and does not modify them.

The exact stored provider identities remain authoritative. Address normalization is display-only and never renames Drive folders.

## Focused coverage

- `Concept3UiStructureInstrumentedTest` verifies that all three purpose-built screens expose their real field actions and that Photos retains a full-width Upload Selected action.
- `PropertyDisplayNameTest` verifies display-only folder-name normalization.
- Existing repository unit and instrumented coverage continues to protect photo identity, queue, Drive, upload, retry, reconciliation, and camera behavior.

## Historical automated verification

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

The historical diff did not include `CameraCaptureActivity`.

## Historical physical gate

At the original implementation checkpoint, one Samsung visual/workflow smoke remained. That pending state is preserved here as historical context only. The later Phase 9D repair sequence and Samsung Galaxy S21 gate described at the top of this record completed the real-device validation before merge.