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
- current internal build after Phase 10F: versionCode 29 / `0.24-explicit-backup-rules-internal`;
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

### Phase 10E — Guided next-action workflow — COMPLETE

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

Completion evidence:
- versionCode 28 / `0.23-guided-next-action`;
- guidance is derived from existing Drive/property/work-order/photo/queue state through a stateless presentation policy;
- Home, Work Orders, and Photos delegate next-action taps to the existing action owners rather than duplicating workflow logic;
- exact runtime head `7cfd56d932cdda0941e3952bca5ad8c75ddeb712`;
- Android CI run `35297157236`: PASS;
- focused policy and instrumentation coverage passed;
- focused Samsung usability smoke passed;
- PR #53 merged to `main` at `8f1ce719d57564faf79a523bb665a7af165e2b4b`.

### Phase 10F — Android backup/restore and second-phone readiness — COMPLETE

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

Completion evidence:
- versionCode 29 / `0.24-explicit-backup-rules`;
- Android 12+ cloud backup and device-to-device transfer explicitly exclude all app-owned backup domains;
- Android 11-and-lower legacy Auto Backup excludes the same app-owned domains;
- saved SAF URI/provider IDs, pending queue metadata, protected originals, prepared derivatives, capture-sequence state, preferences, databases, and future app-owned external/root state are not silently migrated;
- exact runtime head `190b3c0a541518052127698f811da330ef8c0cf8`;
- Android CI run `35300920973`: PASS;
- explicit Level 3 operator pre-merge approval was given immediately before merge;
- PR #55 merged to `main` at `4ec3f00dc169f0ebb5c5bfe22e80e8528a1059ac`.

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

## Phase 11 — Multi-Company Field Workflow — COMPLETE

Goal:
support multiple client/company Drive folders without forcing the operator through Android's folder picker every time and without weakening exact destination identity.

### Phase 11A — Multi-company foundation — COMPLETE

Delivered and validated:
- one operator-approved SAF workspace tree;
- direct company discovery under that workspace;
- exact selected company provider identity persisted separately from workspace identity;
- Add Company / Edit Company / company switching inside FPP;
- authoritative-enough exact-name duplicate/collision protection;
- properties and work orders scoped to the exact selected company;
- queued-photo work-order destinations remain immutable across company switching;
- legacy single-company state retained as a rollback/migration source;
- no company deletion/move, automatic sharing change, second upload path, or queued destination rewrite.

Completion evidence:
- final tested runtime head `ed1990742d403f466151d9399a423a8fdaabbe69`;
- Android CI run `35419341385`: PASS;
- disposable real Android + Google Drive provider gate: PASS;
- company add/discovery/rename/switch: PASS;
- real immutable-destination upload while another company was active: PASS;
- restart/workspace persistence: PASS;
- hardened transient work-order draft isolation: PASS;
- success-banner phone verification: PASS;
- PR #57 merged at `66aeed51fc7065f4f18e06677e3931daf4f3977f`;
- post-merge live `Photos` workspace verification proved HNP ↔ Tresmolino switching without another Android folder-picker change.

Permanent record:
`docs/PHASE_11A_MULTI_COMPANY_FOUNDATION_2026-09-18.md`

### Phase 11B — Clear Company Switcher — COMPLETE

Delivered and validated:
- Home shows current company with a compact ▼ selector affordance in multi-company workspace mode;
- tapping the company-name area opens the existing company chooser;
- no separate Switch button, company tabs, permanent search bar, or extra management screen;
- Add Company / Edit Company / Change Workspace remain under overflow;
- legacy single-company mode hides the selector affordance;
- no Drive identity, persistence, upload, queue, or permission behavior changed.

Completion evidence:
- final tested runtime head `3555e5135d659ef887060b5efbe7e3c5e86912ed`;
- Android CI run `35436040473`: PASS;
- proportional phone review completed;
- one stale/empty startup property read was corrected through existing Refresh with no evidence of data loss or wrong company identity;
- operator accepted the refreshed behavior and approved merge;
- PR #59 merged at `8e9ce51749e3f5919023f5dfc9a98a089ba4f8ed`.

Permanent record:
`docs/PHASE_11B_COMPANY_SWITCHER_2026-09-18.md`

The prior Phase 11 `IN PROGRESS` labels were stale documentation and were corrected by the 2026-09-24 source-of-truth reconciliation.

## Phase 12 — User Identity & Release Readiness — 12F COMPLETE / 12H IN PROGRESS

Goal:
add durable FPP User/Organization identity and release-readiness controls without conflating FPP sign-in with Google Drive authorization or turning FPP into a second job/photo database.

Governing design:
- `docs/IDENTITY_MODEL_V1.md`;
- `docs/PHASE_12_IDENTITY_MODEL_IMPACT_2026-09-21.md`;
- `docs/SOURCE_OF_TRUTH_RECONCILIATION_2026-09-24.md`;
- `docs/PHASE_12B_SUPABASE_AUTH_ARCHITECTURE_2026-09-24.md`;
- `docs/PHASE_12B_SUPABASE_AUTH_IMPACT_2026-09-24.md`;
- `docs/PHASE_12_MASTER_PLAN_2026-09-25.md`.

Current status:
- Phase 12A Identity Model v1 is complete as the approved design foundation;
- Phase 12B dedicated Supabase authentication architecture is approved and merged;
- Phase 12C dedicated Supabase backend foundation is complete and merged;
- Phase 12D Android auth/session foundation is complete and merged;
- Phase 12E runtime authorization enforcement is complete, Samsung-validated, and merged through the governed Level 3 gate;
- Phase 12F Owner/member administration and invitation lifecycle is complete and merged through PR #74; superseded PR #73 remains closed and must not be used as a continuation point;
- Phase 12H Organization ↔ Drive binding protection is active in pre-implementation on `phase-12h/organization-drive-binding-protection`;
- current personal-Drive production remains valid and unchanged;
- Team remains a separate product/backend;
- later business Shared Drive migration changes local Drive binding, not FPP User/Organization identity.

### Phase 12A — Identity model — COMPLETE (DESIGN PHASE)

Approved model:
- permanent User identity independent of email;
- permanent Organization identity representing the field-service business;
- explicit Membership joining User ↔ Organization;
- only Owner and Member roles in v1;
- invitations grant FPP Membership, never Drive permission;
- FPP authentication is separate from Android SAF/Drive authorization;
- Drive Client Company folders remain Drive-side data, not Organizations;
- provider-bound Drive IDs remain local/platform-context identity;
- one active Organization per installation in v1;
- Organization switching blocked while unresolved/protected local work could cross the boundary;
- offline field capture remains possible for previously authenticated/validated Membership when the account service is temporarily unavailable;
- sign-out/revocation/account closure never automatically delete protected photos or Drive business records;
- identity backend remains small and does not duplicate property/work-order/photo data.

### Phase 12B — Dedicated Supabase authentication architecture — DESIGN APPROVED / BACKEND FOUNDATION IMPLEMENTED IN 12C

Settled design:
- Supabase is the original-FPP identity/account backend;
- create a **dedicated original-FPP Supabase project**, separate from Field Photo Prep Team;
- no shared Auth users, tables, keys, Edge Functions, secrets, Organization IDs, or sessions with Team;
- initial sign-in is Supabase Auth email/password;
- v1 is invitation-only after controlled first-Owner bootstrap;
- `auth.users.id` is permanent FPP User identity;
- minimum backend tables are Organizations, Memberships, and Invitations only;
- roles remain OWNER and MEMBER;
- exposed tables use explicit grants + RLS;
- privileged membership/invitation/bootstrap actions remain server-side;
- Android credentials use Keystore-backed encrypted local storage;
- online startup/resume revalidates authoritative Membership;
- cached ACTIVE Membership supports same-Organization field work for at most 72 hours;
- after 72 hours without revalidation, existing protected work remains safe but new capture/remote Drive mutations wait for revalidation;
- password recovery uses Supabase Auth;
- Android SAF remains fully separate Drive authorization;
- Supabase Storage/Realtime/job-photo mirror/public signup/Google social login are out of scope.

Completion gate for 12B design:
- exact docs/contract diff reviewed;
- explicit Level 3 operator pre-merge approval recorded;
- no runtime/schema implementation begins before merge.

### Phase 12C — Dedicated FPP Supabase foundation — COMPLETE

Completed:
1. dedicated original-FPP Supabase project created: `Field Photo Prep`, ref `vtyiktvqhbgabawotkrj`;
2. minimum Organization/Membership/Invitation schema applied;
3. explicit Data API grants + RLS applied and verified;
4. wrong-user/wrong-Organization hosted isolation tests passed;
5. Owner-vs-Member invitation visibility tests passed;
6. duplicate Membership and pending-Invitation protections passed;
7. database/RLS security checks passed; final Auth advisor shows only the known Pro-only leaked-password-protection warning;
8. disposable test identities rolled back to zero persistent rows;
9. Team remains separate and untouched.

Completed:
- real Auth Owner created/confirmed for `inandoutinspections2026@gmail.com`;
- **In And Out Cleaner Inspections LLC** created as the real FPP Organization;
- exact `ACTIVE OWNER` Membership created;
- hosted RLS verified with the real Owner UUID;
- unrelated authenticated identity verified to read no FPP identity rows;
- default `localhost:3000` invite redirect identified and recorded for the next Android-auth slice;
- post-bootstrap advisor review complete.

Known non-blocking warning:
- leaked-password protection is disabled because Supabase documents that feature as Pro-only; current project is Free.

Merge evidence:
- explicit Level 3 operator approval given on 2026-09-24;
- PR #65 merged at `c76ce157627dd1a0247786f35eb2fb0eba105938`.

No Android runtime auth work is included in Phase 12C.

### Phase 12D — Android auth/session foundation — COMPLETE

Scope:
- package-specific Android auth callback URIs;
- dedicated AuthActivity rather than exposing auth callbacks through MainActivity;
- email/password sign-in;
- password recovery → Android deep link → password update;
- narrow Java HTTPS Supabase client using the publishable key only;
- exact Auth User → ACTIVE Membership → Organization validation;
- Android Keystore/AES-GCM encrypted session persistence;
- Account entry in the existing Home overflow;
- no hard startup gate yet.

Redirects:
- internal: `com.inandout.fieldphotoprep.internal://auth-callback`
- production: `com.inandout.fieldphotoprep://auth-callback`

Reason for staging before enforcement:
the first auth-enabled build must prove recovery/sign-in/session restoration on the physical field phone before authentication can safely gate the existing Drive/photo workflow.

Permanent records:
- `docs/PHASE_12D_ANDROID_AUTH_SESSION_2026-09-24.md`
- `docs/PHASE_12D_ANDROID_AUTH_SESSION_IMPACT_2026-09-24.md`

Current final runtime evidence:
- exact runtime head `df02a34981e601d3009875edc8fe154e2a23c936`;
- Android CI `36122577021`: **PASS**;
- Supabase internal + production callback URLs: configured;
- normal Samsung sign-in + ACTIVE OWNER validation: **PASS**;
- Samsung Recheck Account + restart persistence: **PASS**;
- existing Drive/workspace smoke: **PASS**;
- final session-sequencing hardening: **PASS automated**.

Final Samsung hardening gate:
- exact final APK install: **PASS**;
- fresh recovery link opens Field Photo Prep Internal: **PASS**;
- password update + fresh normal sign-in: **PASS**;
- ACTIVE OWNER Membership/Organization validation: **PASS**;
- restart session restore: **PASS**;
- Recheck Account refresh/revalidation: **PASS**;
- prior Drive/workspace smoke: **PASS / reused valid evidence**.

Phase 12D completion:
- explicit Level 3 operator pre-merge approval: **RECORDED 2026-09-25**;
- PR #67 merged to `main` at `475d351b48daa8eb612cf1b0b745d7c4566dbcd5`.

Phase 12D is **COMPLETE**.

### Phase 12E — Runtime authorization enforcement — COMPLETE

Delivered and validated:
- centralized runtime authorization decision consuming the encrypted 12D session;
- automatic authoritative Membership/Organization revalidation;
- exact 72-hour same-Organization offline grace with deterministic boundary/clock-rollback tests;
- successful validation resets grace;
- authoritative revocation overrides grace;
- new capture and new Drive mutations fail closed when authorization requires recheck/sign-in/revocation handling;
- protected originals and reconciliation evidence remain preserved;
- sign-out is blocked while unresolved/protected work would be stranded;
- Home/work-order presentation shows protected-photo counts beside the affected job so unresolved work is locatable rather than only showing a global count.

Evidence:
- Phase 12E merged to `main` at `31bfaaffa012cadf2da0c9c31c6da64967d9d24f`;
- post-merge Android CI passed;
- Samsung online/offline-within-grace/revalidation, capture/preparation, exact Drive upload, protected-work sign-out block, and sign-out/sign-in gates passed;
- explicit Level 3 merge approval recorded.

### Phase 12F — Owner/member administration — COMPLETE

Canonical implementation:
- PR #74, `phase-12f/membership-invitation-lifecycle`, merged to `main`;
- merge commit: `5e3580d3ab769015b7f3c6168022a5def2f308c2`;
- approved final PR head: `696bda87a236cf3b70ee8ea16e3231fdfe16e6e7`;
- superseded PR #73 is closed and not merged;
- live Phase 12F Supabase migration history is reconciled into source control;
- deployed `fpp-owner-invite` Edge Function source matches the merged Phase 12F source;
- core Owner authorization, Organization isolation, invitation idempotency/acceptance, cancellation/expiry, concurrent last-Owner protection, and failure/retry behavior passed.

Completed verification:
- invitation delivery-failure visibility and safe retry: PASS;
- real disposable invitation email/deep-link acceptance: PASS;
- final backend RLS/grant/catalog/advisor snapshot: PASS;
- focused Android Owner/member administration verification: PASS;
- Samsung invitation/member/role/revoke/reactivate/last-Owner reality gates: PASS;
- final Android regression on current Phase 12F runtime content: Android CI run `36241181047` PASS at branch snapshot `2772ebdcc9165c2b6aae6c30f875a1c87f185a7c`.

Completed cleanup:
- disposable `Phase 12F Race Fixture` removed after preserving verification evidence;
- fixture Organization, Memberships, Invitations, audit rows, and two `.example.invalid` Auth users verified at zero;
- real `In And Out Cleaner Inspections LLC` Organization remained present with one ACTIVE OWNER.

Completion:
- explicit Level 3 operator merge approval recorded on 2026-09-26;
- required governance check passed;
- Android CI run `36241916951` passed on the exact approved PR head;
- PR #74 merged to `main` at `5e3580d3ab769015b7f3c6168022a5def2f308c2`.

Durable handoff:
- `docs/PHASE_12F_BUILD_STATE.md`
- `docs/PHASE_12F_BACKEND_VERIFICATION_2026-09-25.md`

### Phase 12G — First-run/new-device flow — PLANNED

- sign in and validate Membership first;
- establish active Organization;
- separately connect/select Drive workspace through Android SAF;
- invited-user setup;
- no portable provider IDs or silent Drive-account inference.

### Phase 12H — Organization ↔ Drive binding protection — IN PROGRESS (PRE-IMPLEMENTATION)

Active line:
- branch: `phase-12h/organization-drive-binding-protection`;
- rollback base: merged Phase 12F `5e3580d3ab769015b7f3c6168022a5def2f308c2`;
- impact: `docs/PHASE_12H_ORGANIZATION_DRIVE_BINDING_IMPACT_2026-09-26.md`;
- build state: `docs/PHASE_12H_BUILD_STATE.md`;
- Level 3 merge approval remains pending.

Scope:
- bind the local Drive workspace deliberately to the active FPP Organization;
- prevent another Organization from silently reusing that binding;
- quarantine legacy untagged Drive state until explicit same-Organization/provider confirmation;
- preserve queued immutable destinations;
- do not compare Auth email to Drive email as an authorization rule;
- no Supabase Drive-identity mirror and no second auth/binding state machine.

### Phase 12I — App Status & Diagnostics — PLANNED

Read-only operator status for:
- signed-in/Membership state;
- last successful validation and offline-grace state;
- Organization;
- Drive connected/not connected;
- queue/protected-work counts;
- app version and relevant permission state.

Diagnostics must consume the authoritative 12E/12H state rather than create a second state machine, and copied support status excludes tokens, provider IDs, SAF URIs, customer addresses/photos, and other sensitive content.

### Phase 12J — Recovery/account-state UX — PLANNED

Clear guidance for:
- temporary offline grace;
- grace expired / revalidation required;
- revoked/no ACTIVE Membership;
- sign-in required;
- Drive disconnected;
- sign-out blocked by protected work;
- manual Recheck Account.

Recovery guidance must never delete or silently redirect protected work.

### Phase 12K — Production identity/release path — PLANNED

- separately secured production signing;
- production callback/update continuity;
- no production private key in the repository;
- production-suitable Auth email delivery before outside-user/public distribution;
- revisit verified Android App Links if an owned HTTPS domain and external distribution make them worthwhile;
- no forced Play Store/public-distribution decision.

### Phase 12L — Clean-install/new-user reality gates — PLANNED

- prove clean/no-session behavior on real Android;
- prove invited/new user identity flow;
- prove Drive connection remains a separate deliberate SAF action;
- prove account transitions cannot cross Organization/Drive boundaries;
- reuse overlapping Phase 8C second-phone evidence if a suitable second phone becomes available rather than duplicate testing.

### Phase 12M — Account/privacy/release closeout — PLANNED

Final source-of-truth, security, privacy, RLS, backup, diagnostics-data-boundary, release, and protected-work review before Phase 12 is called complete.

Default remaining sequence:

**12F → 12H → 12G → 12I → 12J → 12K → 12L → 12M**

Safe design work may overlap where recorded in the master plan, but multiple runtime branches must not independently take ownership of startup/auth/Drive-binding state.

Settled Phase 12A/12B decisions must not drift without contradictory evidence or an explicit governed design change.

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
- deeper folder nesting beyond workspace → company → address → dated work order → photos.

## Roadmap maintenance rule

- mark a phase `COMPLETE` only after required automated and real-device gates pass and the governed change is merged;
- keep active work `IN PROGRESS`;
- split phases when doing so reduces risk or clarifies ownership;
- update this roadmap intentionally when evidence changes sequencing rather than allowing implementation to drift silently.
