# Concept 3 Three-Tab UI Correction

Date: 2026-09-13

Classification: Level 2 presentation/navigation correction.

Branch: `feat/concept-3-ui-makeover-20260912`; existing PR #35.

Rollback point: `cd3d0eebda2be9021d3553756d08cddceb6672c9` (previous tested runtime: `6708c351173874b14abfd5fb8cba90b91b9e4a59`).

## Problem and authorized scope

The operator rejected excessive scale and inconsistent presentation across the three separate Home, Work Orders and Photos tabs. They are not historical versions of Home. Continue the existing implementation, using the supplied Concept 3 Hybrid Field App image as visual authority and substituting only real app content/actions. The older Phase 9A staged-screen restrictions are superseded by the operator's explicit coordinated three-tab authorization; behavioral contracts remain unchanged.

## Owning surfaces and changed blocks

- Screen/row XML: compact app titles, quieter Home storage strip, property heading/count on one line, shorter cards, consistent explicit action text scale, secondary maintenance controls, camera and sticky upload actions.
- `ContentHeightListView`: work-order list measures its actual rows within the existing scroll container, eliminating the fixed blank reservation for short lists.
- Shared bottom-navigation XML, icon/color/style resources: one common 56dp component, 20dp vector icons, 11sp one-line labels and selected state.
- `MainActivity` / `PhotoCaptureActivity`: bind shared navigation IDs and selected state. A UI-only intent destination ensures Photos → Home returns to Home rather than leaving Work Orders visible; Photos → Work Orders delegates to the existing saved-property opener when Main is on Home. Existing lookup/identity checks remain authoritative.
- `PropertyDisplayName`: separate property presentation from ordinary work-order folder text. Strip only the operator-reported terminal `PRESSURE TEST` metadata from numeric property names. The repo accepts free-form address names; it contains no general address/metadata grammar. Do not guess away ambiguous address tokens such as the second `RD` in the reported Bluestem example.
- Work-order adapter: duplicate display labels show a short provider-ID disambiguator without changing selection identity.
- CI evidence: preserve full instrumentation reports and export actual activity screenshots in light/dark/1.3-font variants on a Samsung-sized viewport.
- Unit/UI instrumentation: display regressions, shared navigation structure/dimensions, actual activity screenshots with adapter-bound fixtures and real test JPEG bytes, explicit checkbox/count, uncertainty exclusion and navigation.

## Read/write surfaces and protected behavior

UI reads existing folder/queue state and protected/prepared image bytes for thumbnails. Navigation changes only which screen is visible; existing selection actions still call their existing owners. No changes to stored schemas, Drive folder names, provider IDs, permissions, original protection, preparation, queue/attempt sequencing, upload destination binding, duplicate/create/reuse/Clear & Reuse, UNCERTAIN reconciliation, confirmed-success cleanup or signing.

`CameraCaptureActivity` and camera resources are unchanged. No fake runtime data or fake external features. Instrumentation fixtures are isolated emulator-only evidence, not proof of real Google Drive/provider behavior.

No Google Drive integration impact.

## Verification and remaining gate

Focused: `PropertyDisplayNameTest`, Concept 3 structural and rendered-activity instrumentation. Final required Android CI: all unit tests, internal debug build, signer verification, complete instrumentation and launch smoke on exact final runtime head. Render and inspect all three real activities before staging an APK, in light/dark themes and a larger font setting. Record actual outcomes, not inferred passes.

Primary risks: default Android button styling/font scale, duplicate labels after presentation cleanup, navigation returning to the wrong tab, and text/system-bar overlap. Rollback by reverting this narrow correction; never clear application data or queued photos as rollback.

Samsung gate: inspect compact Home with real properties, select one existing work order, inspect Photos/selection/sticky upload, confirm existing camera handoff and all three-tab/system-bar spacing. No merge before operator approval; no destructive Drive experiment solely for this presentation correction.

Status: **IMPLEMENTED — AUTOMATED AND RENDERED REVIEW PASSED; SAMSUNG VISUAL GATE PENDING**.

Exact tested runtime/APK source: `801e05f84e1bbc2cd0f308dc77bae1cd43d0ef87`; [CI 34732932338](https://github.com/timbone72-CC/field-photo-prep/actions/runs/34732932338) PASS: 139 unit tests, internal build, stable signer, 8 full instrumentation tests, 2 explicit UI tests each in dark/1.3-font variants, and launch smoke. All nine final actual-activity images were visually inspected and accepted. The initial visual review was corrected before this gate: lighter Photos statuses, flat navigation and green connected dot against the quiet strip.

APK artifact `10310476852`; APK SHA-256 `4d009ad04e539bc054e8b8513780b9f2f94a939d2ac72424e843284a54fb1ea2`. Rendered evidence artifact `10309898268`. See the updated implementation record for archive digests, viewport, fixture limitations and Samsung checklist. Final evidence records are documentation-only; no runtime change follows verification. No merge before Samsung approval.
