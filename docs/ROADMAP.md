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
- Phase 9 completed at versionCode 18 / `0.13-field-ui-internal` with the existing internal package identity and stable non-production test signer.

Phase 8C remains independently deferred; completing Phase 9 does not claim cross-device/account provider-ID portability.

## Current canonical field-tested baseline — 2026-09-17

Field work after Phase 9 produced additional reliability and photo-ordering changes that are now reconciled into the repository's canonical `main` branch.

Canonical field-tested baseline:
- canonical branch: `main`;
- Phase 10A merge: `a5689e67a91d5dbbe57f6c8b1fc80dbece243449`;
- exact last runtime-changing tested head preserved in ancestry: `e52765fd5b02266244d9101c5fd5429d5aa4e6c4`;
- current internal build after Phase 10D: versionCode 27 / `0.22-folder-screen-state-separation-internal`;
- includes the bounded Drive verification-settle behavior and safe bulk reconciliation path;
- includes durable automatic capture-order filenames;
- includes work-order-reuse capture-sequence reset and old-occurrence history isolation;
- exact head passed the complete Android CI suite, and the reuse/occurrence behavior passed the disposable physical Android + Google Drive gate.

The source-of-truth split is resolved. Future runtime work must start from canonical `main` (or a branch created from it) so already-proven 0.20 behavior is not lost.

## Phase 10 — Stabilization & Scale — IN PROGRESS

Goal: stabilize the proven single-phone field workflow for realistic large inspection batches, reduce avoidable operator recovery work, and make repository/device-recovery behavior explicit without weakening the existing photo and Drive safety model.

Phase 10 is driven by actual field evidence and code review. It is not a general feature-expansion phase.

### Phase 10A — Repository/source-of-truth reconciliation — COMPLETE

Problem:
- the field-tested 0.20 development line is ahead of default `main`;
- beginning future work from the older default branch could silently omit already-tested fixes and create parallel behavior.

Plan:
- compare the exact tested 0.20 head against default `main`;
- reconcile the proven runtime, tests, contracts, and roadmap into one authoritative development baseline;
- preserve the tested runtime behavior rather than reimplementing it;
- reconcile stale/open PR status only after the authoritative branch contains the intended tested changes;
- run the complete Android CI suite on the final reconciled runtime head;
- perform only the smallest proportional Samsung smoke required by any actual integration delta.

Completion gate:
- one authoritative branch contains the full intended 0.20 behavior;
- no unreviewed runtime drift is introduced during reconciliation;
- complete Android CI passes;
- roadmap/version status matches the repository state.

No new product behavior is authorized by 10A.

Completion evidence:
- PR #47 merged to `main` at `a5689e67a91d5dbbe57f6c8b1fc80dbece243449`;
- reconciliation branch was ahead-only from the prior `main` merge base, with no runtime conflict-resolution edits;
- exact final PR head `4389c1d29cc0f9231692e324ad87589f7f570a1f` passed Android CI run `35293174512`;
- unit tests, internal debug build, stable signer verification, instrumented image tests, internal launch smoke, and artifact uploads all passed;
- explicit Level 3 operator pre-merge approval was given immediately before merge;
- PR #42 was closed after its exact head was confirmed in canonical ancestry;
- obsolete Phase 9A PR #34 was closed as superseded;
- PR #39 remains open intentionally for separate Phase 10C stale-provider reconciliation review.

### Phase 10B — Large photo-list scalability — COMPLETE

Field basis:
- realistic work orders now reach at least roughly 100 photo records;
- the current Photos screen rebuilds all photo rows and decodes thumbnails while rendering;
- this was acceptable at small counts but is now a real scaling boundary worth hardening.

Plan:
- replace only the Photos list presentation with a virtualized list such as `RecyclerView`;
- move thumbnail decode/loading off the main UI thread;
- preserve exact photo ordering, checkbox selection, row `⋯` actions, statuses, selected-photo details, batch upload, batch discard, reconciliation, and capture-order behavior;
- do not move queue, Drive, or persistence ownership into the list adapter;
- test with a realistic 100–150-photo local fixture;
- run a focused Samsung scroll/tap/selection smoke before completion.

Protected behavior:
- no queue-state change;
- no upload/retry change;
- no destination change;
- no prepared-image policy change;
- no permanent in-app photo library.

Completion evidence:
- versionCode 26 / `0.21-photo-list-scale`;
- exact runtime head `8140dcfa8701d0b4e37cbda7af4f0713f376b449`;
- Android CI run `35295211103`: PASS;
- 150-photo virtualization and asynchronous thumbnail instrumentation passed;
- focused Samsung large-list smoke passed;
- PR #49 merged to `main` at `110477c286bace05bb7fa56ee22199cf2de7fa92`.

### Phase 10C — Large-batch upload / UNCERTAIN stabilization — OBSERVING

Field basis:
- a prior large upload produced a substantial UNCERTAIN backlog even though later reconciliation proved the Drive copies existed;
- the current development line already adds a bounded provider-settle verification window and safe bulk reconciliation.

First step:
- use the current canonical 0.21 behavior on a fresh realistic large batch and record:
  - selected photo count;
  - immediately confirmed uploads;
  - retry-safe failures;
  - UNCERTAIN results;
  - reconciliation-confirmed results;
  - any unresolved items.

Decision rule:
- if false UNCERTAIN results are now rare and recovery is practical, make no additional upload change;
- if false UNCERTAIN remains materially disruptive, design one narrow automatic **read-only reconciliation** pass for the affected photo before surfacing manual operator recovery.

Any later implementation must preserve:
- no blind retry;
- no second remote create when remote state is unresolved;
- exact stored destination identity;
- provisional remote identity evidence;
- strict exact-match reconciliation;
- strictly sequential selected-batch Drive writes;
- manual fail-closed state when remote truth still cannot be proven.

Do not add a background retry scheduler merely to hide provider uncertainty.

Existing open PR #39 (`Fix UNCERTAIN reconciliation on stale Drive provider metadata`) is a Phase 10C input, not a Phase 10A merge target. A code comparison found two unique hardening ideas not present in 0.20: provider `refresh() == false` should not by itself block otherwise-settled reconciliation, and stale provider size metadata should not override a stronger exact SHA-256 content proof. Keep PR #39 open until those behaviors are deliberately re-evaluated against the canonical 0.20 baseline with current tests and a governed Level 3 decision.

### Phase 10D — Separate Properties and Work Orders UI state — COMPLETE

Field basis:
- `MainActivity` currently reuses one visible-folder collection across Properties and Work Orders;
- that coupling previously contributed to root-level property rows appearing as work orders;
- current guards prevent the known failure, but the shared state remains an unnecessary maintenance hazard.

Plan:
- give Properties and Work Orders separate visible collections/adapters or equivalent narrowly owned screen state;
- preserve the existing `DriveClient` provider queries and stable document-ID rules;
- preserve current selection/reconnect/create/reuse behavior;
- add focused regression coverage proving property rows cannot leak into the Work Orders list and work-order rows cannot leak into Properties;
- do not use this as a reason for a broad MVVM, Compose, repository-layer, or navigation rewrite.

This is a targeted maintainability repair justified by a real prior defect.

Completion evidence:
- versionCode 27 / `0.22-folder-screen-state-separation`;
- separate `propertyFolders` and `workOrderFolders` now back the two adapters;
- focused regression proves opening Work Orders clears stale work-order/self rows without mutating the property list;
- exact runtime head `291bd3b5df607ff2bcee75081bc0504dc255be18`;
- Android CI run `35296448276`: PASS;
- PR #51 merged to `main` at `aec5697f2d2d51894ed1dcb59ec6d51cb4f0c6e4`.

### Phase 10E — Guided next-action workflow

Goal:
make the routine field workflow self-explanatory so the user does not need to guess what action follows the one they just completed.

Design rule:
- the app should present one obvious **primary next action** based on already-known app state;
- secondary controls may remain available, but routine progress should not depend on the operator interpreting status labels or remembering the workflow;
- safe routine transitions should advance naturally when the prior action completes;
- destructive, ambiguous, account/provider, or unresolved-remote-state decisions must still stop and require deliberate operator input.

Expected state-driven guidance examples:
- no property selected → **Choose Property**;
- property selected with no active work order → **Choose or Add Work Order**;
- active work order with no current photos → **Take Photos**;
- photos captured and still preparing → show preparation progress without asking for another action;
- prepared photos ready → **Upload Ready Photos (N)**;
- confirmed upload complete → **Done — Return to Work Orders**;
- one or more uploads unresolved → **Check N Uploads** and route into the existing safe reconciliation path;
- after exact reconciliation succeeds, return the operator to the normal next action rather than leaving them in a recovery dead end.

UI approach:
- prefer a small persistent **Next Action** surface or equivalent field-readable primary button rather than a tutorial, wizard, or extra settings system;
- explain successful state changes in plain language, including the exact work order/destination when useful;
- avoid generic labels such as `WAITING` when a field-oriented explanation such as **23 photos ready — next: upload to [work order]** is clearer;
- keep Home / Work Orders / Photos as normal Android screens rather than forcing a rigid step-by-step wizard.

Implementation boundaries:
- derive guidance from existing selected-property, selected-work-order, photo-preparation, queue, upload, and reconciliation state;
- do not create a second workflow state machine that can drift away from the authoritative app state;
- UI guidance may request existing actions but must not duplicate persistence, Drive, queue, or reconciliation ownership;
- automatic navigation is allowed only where the destination is unambiguous and the action is non-destructive.

Safety stops that remain explicit:
- Clear & Reuse;
- local photo discard;
- Change Drive / provider or master-folder selection;
- ambiguous property/work-order identity;
- unresolved `UNCERTAIN` uploads;
- any action that could change destination identity, delete protected local content, or create a second remote copy.

Completion gate:
- focused tests prove each major normal state exposes the correct primary next action;
- impossible or unsafe actions are not presented as routine continuation;
- a Samsung field smoke proves the normal Property → Work Order → Capture → Prepare → Upload → Done path can be followed without prior knowledge of the app;
- existing Drive, photo-safety, queue, retry, reconciliation, and deletion semantics remain unchanged.

This phase is a usability layer over proven state, not a new workflow engine.

### Phase 10F — Android backup/restore and second-phone readiness

Goal:
make app-private recovery behavior explicit before claiming portability across Android devices/accounts.

Plan:
- define explicit Android backup/data-extraction rules instead of relying on implicit default backup behavior;
- classify app-private data into:
  - safe to restore as ordinary local data;
  - provider/account-bound data that must be revalidated;
  - temporary/protected photo data whose restore behavior must not risk duplication, misrouting, or false success;
- include persisted SAF master-tree access, provider document identities, pending-photo metadata, queue state, capture-sequence state, and local image copies in the review;
- if a restored provider-bound identity cannot be proven valid in the active device/account context, fail closed and require deliberate reconnection/reselection rather than guessing;
- test backup/restore rules without using live customer content.

Phase 8C remains the final physical portability gate:
- when a second suitable Android phone is available, run the existing shared-master test on that phone;
- do not claim provider-ID portability until that gate passes.

### Phase 10 deferred release trigger — production signing

Production signing is not active Phase 10 work unless normal distribution beyond the current internal/test workflow becomes a real requirement.

When that trigger occurs:
- create a separately secured production signing path;
- do not commit production private key material;
- preserve install/update continuity intentionally;
- perform the governed release/install reality gate before calling the build production-ready.

### Phase 10 protected core

Do not change during stabilization unless a separately evidenced defect requires it:
- protected local originals;
- immutable address/work-order destination identity;
- conservative address ambiguity handling;
- parent-bound work-order identity;
- automatic serialized photo preparation;
- 2048px long-edge / JPEG 85 preparation policy without contrary image-quality evidence;
- sequential Drive uploads;
- provisional remote identity barrier;
- `FAILED` versus `UNCERTAIN` distinction;
- exact reconciliation before retry release;
- cleanup only after confirmed remote success;
- explicit destructive confirmation for Clear & Reuse.

Do not add Room/SQLite, WorkManager, parallel Drive uploads, automatic destructive Drive cleanup, placeholder Settings, OCR, AI classification, or a permanent in-app photo library without separate field evidence and approval.

### Phase 10 sequencing

Default order:

**10A source-of-truth reconciliation → 10B photo-list scaling → 10C large-batch observation/refinement → 10D screen-state separation → 10E guided next-action workflow → 10F backup/restore readiness → Phase 8C when a second suitable phone exists.**

A later step may move earlier only when field evidence makes it more urgent and the change remains independently testable.

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
