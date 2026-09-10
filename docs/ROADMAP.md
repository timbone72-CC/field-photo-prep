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
- launch the system camera for the exact selected work occurrence;
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

## Phase 8 — Field Workflow / Release Hardening — IN PROGRESS

Goal: turn the proven core into a practical first field release without expanding the product unnecessarily.

Detailed plan:

`docs/PHASE_8_FIELD_WORKFLOW_RELEASE_HARDENING_PLAN_2026-09-10.md`

### Phase 8A — Field workflow polish — IN PROGRESS

Use observed A16 friction only:
- remove stale developer phase labels from normal screens;
- clearly separate selecting an existing work order from creating/reusing a dated work order;
- make the work-order name field explicitly name-only so it does not look like a search field;
- simplify implementation-heavy photo-screen top copy;
- keep all Drive, identity, queue, camera, retry, and cleanup semantics unchanged.

The external Samsung camera OK/Retake screen remains accepted for the first release. Removing it reliably would likely require an in-app camera subsystem such as CameraX and is deferred unless field use proves that cost worthwhile.

### Phase 8B — Normal install/update/release path — PENDING

- keep the stable test key non-production;
- define a secure release-signing path without committing private release key material;
- make version progression intentional;
- build one release-candidate APK with recorded identity/checksum;
- prove update behavior on the primary phone without unexpected state loss;
- document rollback.

No Play Store publication is assumed.

### Phase 8C — Second Android phone/shared-master reality check — PENDING

On another supported Android phone using that operator's own Google Drive access to the shared approved master:
- install normally;
- select the intended Drive provider/account and approved shared master;
- reopen a safe existing address/work order without duplicates;
- capture/prepare/upload one disposable photo;
- verify the Drive destination and visual result;
- restart and confirm safe confirmed state;
- prove the app does not assume provider IDs are portable between phones/accounts.

## Phase development staging rule

The governed process is recorded in:

`docs/PHASE_STAGING_DOCTRINE.md`

Default cycle:

**Build everything that can be honestly proven without the phone → stage at the next genuine device-dependent boundary → run the smallest required phone reality gate → accept the evidence → adjust only where reality requires it → continue the next phase as far as possible → stage again.**

## Lean architecture baseline

The accepted lean-architecture audit baseline is recorded in:

`docs/LEAN_ARCHITECTURE_BASELINE_2026-09-10.md`

The app remains intentionally small: no database framework, DI framework, reactive stack, background worker framework, in-app camera subsystem, or permanent photo gallery has been added without a demonstrated need.

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
