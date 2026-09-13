# Field Photo Prep Roadmap

## Purpose

Keep Field Photo Prep lean and field-focused. The app exists to get the right photos into the right Google Drive work-order folder with as little friction as possible while protecting any photo that is not yet confirmed in Drive.

Core finish-line workflow:

**Choose/create address → choose/create/reuse dated work order → take photos → prepare them → send them safely to the exact Drive folder → remove unnecessary local copies after confirmed success.**

This roadmap defines phase boundaries. `CONTRACT.md`, `CHANGE_CONTROL_CONTRACT.md`, `TESTING_CONTRACT.md`, and `INTEGRATION_CONTRACT.md` remain authoritative for approved behavior and safety.

## Phase status

### Phase 1 — Master Folder Connection — COMPLETE

Delivered and validated:
- Android system folder picker;
- persisted access to the selected master folder;
- master-folder identity retained across restart;
- refresh/list direct address folders.

### Phase 2 — Work-Order Folders — COMPLETE

Delivered and validated:
- select an existing address folder;
- list/refresh its work-order folders;
- enter a human-readable work-order name and date;
- create `Work Order - YYYY-MM-DD` under the exact address;
- reuse one exact existing match instead of creating a duplicate;
- require operator choice when exact duplicates exist;
- persist selected address/work-order identities.

### Phase 3A — Empty Work-Order Folder Reuse — COMPLETE

Delivered and validated:
- operator selects the exact old work-order folder;
- app verifies same work-order type and older date;
- provider identity and address parent are revalidated;
- authoritative-enough zero-child state is required;
- the same folder is renamed to the new dated work order;
- folder identity and parent remain unchanged;
- no deletion occurs.

### Phase 3B — Clear & Reuse — COMPLETE

Delivered and validated:
- explicit operator-selected destructive reuse for one non-empty old work-order folder;
- full master/address/work-order hierarchy and direct-child count shown before confirmation;
- child-folder warning where applicable;
- confirmation bound to the exact child-identity snapshot;
- provider freshness/loading uncertainty fails closed;
- only confirmed direct children may be removed;
- zero-child state is verified before rename;
- same folder identity is retained after rename/reuse;
- partial/uncertain destructive results block further writes until refresh/inspection.

Real Samsung Galaxy A16 + Google Drive provider cancellation and successful Clear & Reuse gates passed on 2026-09-10.

The historical Phase 3B PR #8 was not force-merged after the later Phase 7 line advanced. Its validated behavior was ported onto current main through PR #20, revalidated in CI and with a proportional real-device smoke, and merged. PR #8 is closed as superseded.

### Phase 4 — Address Folder Creation — COMPLETE

Delivered and validated:
- enter an address folder name;
- trim outer whitespace only;
- require fresh/settled provider state before absence-based create;
- one exact match → reuse;
- multiple exact matches → operator choice, no create;
- no exact match → create one folder under the exact approved master;
- verify returned provider identity before automatic selection;
- persist exact address provider identity;
- address folders are never recycled by work-order reuse.

Real Samsung Galaxy A16 + Google Drive provider create/reuse/restart behavior passed on 2026-09-10.

The historical Phase 4 PR #10 was not force-merged after the later Phase 7 line advanced. Its validated behavior was ported onto current main through PR #20, revalidated in CI and with a proportional real-device smoke, and merged. PR #10 is closed as superseded.

### Phase 5 — Camera + Temporary Photo Protection — COMPLETE

Delivered and validated:
- launch camera capture for the exact selected work occurrence;
- bind the photo to the selected work-order provider identity before camera launch;
- protect non-empty captured image data locally;
- retain unconfirmed photos across app/process restart;
- preserve destination binding across later navigation;
- still-photo capture works on the physical Android path.

Samsung Galaxy A16 real-camera capture, restart survival, and immutable destination binding passed on 2026-09-10.

### Phase 6A — Photo Preparation — COMPLETE

Delivered and validated:
- create a separate smaller prepared JPEG;
- preserve the protected original during preparation;
- deterministic derivative identity from the local photo UUID;
- preserve usable orientation/content;
- perform preparation off the UI thread;
- block conflicting local actions while preparation owns the photo.

Samsung Galaxy A16 real-camera preparation and visual usability passed on 2026-09-10.

### Phase 6B — Drive Upload — COMPLETE

Delivered and validated:
- upload only to the immutable work-order provider identity stored on the photo;
- durable provisional remote identity before byte write;
- create → persist provisional identity → write/verify barrier;
- confirmed remote identity recorded only after provider-visible success;
- ambiguous/interrupted outcome becomes non-blind-retry `UNCERTAIN`;
- deterministic remote filename from local photo UUID.

Physical Android + real Google Drive DocumentsProvider upload passed on Samsung Galaxy A16 on 2026-09-10. The unsafe ambiguity experiment was not manufactured when no deterministic safe method was available.

### Phase 7A — Persistent Upload Queue State — COMPLETE

Delivered and validated:
- durable `WAITING`, `UPLOADING`, `FAILED`, `UNCERTAIN`, and `UPLOADED` bookkeeping;
- immutable local photo identity and stored destination through transitions;
- upload attempt count and timestamp;
- stale in-flight work recovers to `UNCERTAIN` rather than blind retry;
- confirmed-success state requires explicit remote identity;
- per-photo state isolation and restart survival.

### Phase 7B — Remote Retry, Reconciliation & Cleanup — COMPLETE

Delivered and validated:
- conservative read-only `UNCERTAIN` reconciliation against the original immutable destination;
- deterministic-name matching and SHA-256 comparison where required;
- retry release only after authoritative-enough remote absence and no unresolved provisional identity;
- confirmed-match promotion to `UPLOADED` without a second remote create;
- automatic local original/prepared cleanup only after confirmed Drive success and durable bookkeeping;
- cleanup failure remains separate from upload success;
- no automatic retry scheduler or remote uncertain-content deletion/overwrite.

Samsung Galaxy A16 reality gate passed on 2026-09-10. Confirmed state and cleanup survived restart, and Drive copies remained intact and visually usable.

Stable test signing was added so test APK updates preserve app-private state across CI builds. The checked-in stable test key is explicitly non-production.

The complete Phase 5 → 7B stack was merged to main, followed by the controlled Phase 3B/4 integration in PR #20. The current main line therefore contains the full proven core workflow.

## Phase 8 — Field Workflow / Release Hardening — IN PROGRESS — 8C DEFERRED

Goal: turn the proven core into a practical first field release without expanding the product unnecessarily.

Detailed plan:

`docs/PHASE_8_FIELD_WORKFLOW_RELEASE_HARDENING_PLAN_2026-09-10.md`

### Phase 8A — Field workflow polish — COMPLETE

Delivered and validated:
- stale developer phase labels were removed from normal screens;
- existing-work-order selection is clearly separated from creating/reusing a dated work order;
- the work-order entry field now states that it accepts the work-order name only;
- implementation-heavy normal photo-screen copy was simplified;
- Drive, identity, queue, camera, retry, reconciliation, and cleanup semantics were unchanged.

Phase 8A passed Android CI and merged through PR #21 on 2026-09-10.

### Phase 8B — Normal install/update/release path — COMPLETE

Delivered and validated:
- internal/test APKs use application ID `com.inandout.fieldphotoprep.internal` and launcher label `Field Photo Prep Internal`;
- the stable checked-in test key remains explicitly non-production and signs only the internal/debug build;
- the future production identity remains `com.inandout.fieldphotoprep` and is reserved for a separately secured production signer;
- no production private key was created or committed;
- version progression was advanced intentionally to versionCode 17 / `0.12-field-release-hardening-internal` for the internal build;
- Android CI passed unit tests, internal build, stable signer verification, instrumentation, internal launch smoke, and artifact packaging;
- Samsung Galaxy A16 physical validation proved `Field Photo Prep` and `Field Photo Prep Internal` install side-by-side as distinct apps;
- the internal app independently selected `HNP Jobs`, reopened the existing safe test work order, and successfully returned a camera capture as a protected `WAITING` record under the `.internal` package/FileProvider identity.

Phase 8B merged through PR #22 on 2026-09-10. Permanent evidence: `docs/PHASE_8B_DEVICE_REALITY_GATE_RECORD_2026-09-10.md`.

No Play Store publication is assumed.

### Post-8B field camera and upload workflow enhancements — COMPLETE

Field use demonstrated that the external-camera flow and one-photo-at-a-time interaction created unnecessary friction. The app now owns still capture in-app with CameraX while preserving the same protected-photo and immutable-destination rules.

Delivered and validated on the current main line:
- CameraX in-app multi-shot camera session with one **Done** action;
- automatic background preparation after durable `WAITING` capture;
- Flash Auto/On/Off and separate Torch On/Off controls;
- selectable prepared-photo upload batches with strictly sequential Drive writes;
- real `UNCERTAIN` fail-closed behavior and explicit reconciliation without blind duplicate retry;
- pinch zoom and a one-handed zoom slider;
- phone-camera-style preview-first controls;
- portrait and landscape camera layouts;
- capability-gated truthful ultra-wide support when CameraX exposes a real sub-1× path;
- exact 1× reset and supported higher-zoom quick controls.

The camera/layout smoke passed on the operator's Samsung phone, and the selectable batch Drive gate passed against a disposable real Google Drive work order. PRs #25, #26, #27, and #30 are merged. The old superseded camera PR #24 is closed.

### Phase 8C — Second Android phone/shared-master reality check — DEFERRED

Reason for deferral: a second suitable Android phone is not currently available.

When another supported Android phone becomes available, use that phone user's own Google Drive access to the shared approved master and complete the original portability gate:
- install the internal build normally;
- select the intended Drive provider/account and approved shared master;
- reopen a safe existing address/work order without duplicates;
- capture/prepare/upload one disposable photo;
- verify the Drive destination and visual result;
- restart and confirm safe confirmed state;
- prove the app does not assume provider IDs are portable between phones/accounts.

This deferral does not invalidate the already proven single-device core workflow and is not a reason to simulate a second device/account. Phase 8C remains required before claiming cross-device/account portability or treating that portability as field-proven.

Development may continue on work that does not depend on second-device provider identity behavior. The next roadmap phase should be defined from actual remaining field needs rather than inventing a substitute for the missing second-phone gate.

## Phase 9 — Concept 3 Field UI — COMPLETE

Goal: replace the non-camera development-style presentation with a compact, field-first Home / Work Orders / Photos interface while preserving the proven camera, Drive, photo-identity, queue, upload, retry, reconciliation, and cleanup core.

Delivered and validated:
- purpose-built compact Home, Work Orders, and Photos layouts with shared bottom navigation;
- field-readable property/work-order presentation and real photo thumbnails/selection controls;
- Photos row `⋯` now provides immediate state-safe actions for that exact photo;
- selected-photo details/actions no longer require scrolling below a long photo list;
- Home `⋯` reliably exposes only the existing **Change Drive** path;
- focused interaction coverage for property/work-order/photo navigation and batch selection;
- 48dp photo-selection touch target and removal of the obsolete hidden work-order selector;
- operator-confirmed **Discard Selected (N)** for locally discard-safe temporary photos only, with complete preflight and no Drive deletion path;
- Work Orders date control repaired for Samsung readability without changing date semantics or folder naming;
- Settings remains deferred; photo-list performance remains an evidence-only risk rather than a speculative rewrite.

Verification and merge evidence:
- Phase 9D UI repair automated gate passed and the focused Samsung Galaxy S21 field gate passed on 2026-09-13;
- Batch Local Photo Discard Level 3 automated gate and Samsung disposable-photo smoke passed on 2026-09-13;
- Work Orders date readability automated gate and Samsung visual/date-picker smoke passed on 2026-09-13;
- final version-reconciled head `64339686ae0be08a2148b9b73390c35575e30584` passed Android CI run `34761274823`;
- PR #35 (Concept 3 + Phase 9D), PR #36 (guarded batch local discard), and PR #37 (date readability + version reconciliation) merged to `main` on 2026-09-13;
- current internal build version is versionCode 18 / `0.13-field-ui-internal` with the existing internal package identity and stable non-production test signer.

Phase 8C remains independently deferred; completing Phase 9 does not claim cross-device/account provider-ID portability.

## Phase development staging rule

The governed process is recorded in:

`docs/PHASE_STAGING_DOCTRINE.md`

Default cycle:

**Build everything that can be honestly proven without the phone → stage at the next genuine device-dependent boundary → run the smallest required phone reality gate → accept the evidence → adjust only where reality requires it → continue the next phase as far as possible → stage again.**

## Lean architecture baseline

The accepted lean-architecture audit baseline is recorded in:

`docs/LEAN_ARCHITECTURE_BASELINE_2026-09-10.md`

The app remains intentionally small. CameraX was added only after field use demonstrated a real need for faster in-app multi-shot capture and camera controls. The app still avoids unnecessary database, DI, reactive, background-worker, and permanent photo-gallery frameworks.

## Product boundary

Keep out unless separately approved:
- permanent in-app photo library;
- workbook integration;
- Free Map Router integration;
- route planning;
- automatic sharing changes;
- general Drive cleanup or file-manager behavior;
- video;
- background location tracking;
- OCR;
- AI photo classification;
- separate numeric work-order IDs without demonstrated ambiguity;
- deeper folder nesting beyond master → address → dated work order → photos.

## Roadmap maintenance rule

- mark a phase `COMPLETE` only after required automated and real-device gates pass and the governed change is merged;
- keep active work `IN PROGRESS`;
- split phases when doing so reduces risk or clarifies ownership;
- update this roadmap intentionally when evidence changes sequencing rather than allowing implementation to drift silently.
