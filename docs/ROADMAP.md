# Field Photo Prep Roadmap

## Purpose

Keep Field Photo Prep lean and field-focused. The app exists to get the right photos into the right Google Drive work-order folder with as little friction as possible.

Core finish-line workflow:

**Choose/create address → choose/create/reuse dated work order → take photos → prepare them → send them safely to the exact Drive folder → remove unnecessary local copies after confirmed success.**

This roadmap defines phase boundaries. It does not expand the approved product scope in `CONTRACT.md`. If implementation reveals a real workflow need, update the roadmap intentionally rather than quietly adding features.

## Phase status

### Phase 1 — Master Folder Connection — COMPLETE

Goal: connect the app to one approved master folder without building a general Drive browser.

Delivered:
- Android system folder picker;
- persisted access to the selected master folder;
- master-folder identity retained across restart;
- refresh/list direct address folders.

Validated on the operator phone with `HNP Jobs` and persisted restart behavior.

### Phase 2 — Work-Order Folders — COMPLETE

Goal: select an address and establish the exact dated work occurrence beneath it.

Delivered:
- tap an existing address folder;
- list/refresh its work-order folders;
- enter a human-readable work-order name;
- choose a local calendar date;
- create `Work Order - YYYY-MM-DD` under the exact address;
- reuse one exact existing match instead of creating a duplicate;
- require operator choice when exact duplicates exist;
- persist selected address/work-order identities.

Validated with `HNP Jobs → FIELD PHOTO PREP TEST → Cut Grass - 2026-09-06`.

### Phase 3A — Empty Work-Order Folder Reuse — COMPLETE

Goal: recycle an older same-work-order folder only when it is truly empty.

Delivered:
- operator selects the specific old work-order folder;
- app verifies it is the same work-order type and an older date;
- app revalidates the exact folder identity under the selected address;
- app verifies the folder contains zero child items of any kind;
- app renames that same folder to the new `Work Order - YYYY-MM-DD` name;
- folder identity and parent remain unchanged;
- no deletion is permitted.

Merged after the positive empty-folder reuse and non-empty fail-closed device checks plus explicit Level 3 approval.

### Phase 3B — Clear & Reuse — IN PROGRESS — DEVICE GATE BLOCKED

Goal: allow deliberate recycling of a non-empty old work-order folder when the operator explicitly chooses it.

Scope:
- operator selects one exact old work-order folder;
- app shows the full master/address/work-order hierarchy and direct child-item count;
- explicit confirmation is required;
- remove only that selected folder's child items;
- verify the folder is empty;
- rename/reuse the same folder identity for the new work/date;
- any delete, verification, or rename failure stops the workflow;
- cloud-provider child listings must be fresh/settled before any empty/non-empty decision.

Current status:
- runtime and automated verification are complete on PR #8;
- real-device testing exposed stale Android/Google Drive child listings and the branch was hardened to fail closed unless fresh settled state is confirmed;
- large-display work-order selection was hardened;
- remaining real-device confirmation/cancel/success checks are deferred because the operator's Android phone is temporarily unavailable.

No automatic cleanup or bulk Drive management.

### Phase 4 — Address Folder Creation — IN PROGRESS — DEVICE GATE BLOCKED

Goal: create a missing property/address folder safely under the approved master folder.

Scope:
- enter/select address text;
- refresh actual master-folder children before create;
- one exact match → reuse;
- multiple exact matches → operator choice;
- no match → create exactly one address folder under the approved master;
- persist returned folder identity;
- never recycle address folders.

Current status:
- runtime and automated Android CI verification are complete on PR #10;
- PR remains unmerged pending the real Android/Google Drive create/reuse reality gate.

### Phase 5 — Camera + Temporary Photo Protection — IN PROGRESS — DEVICE GATE BLOCKED

Goal: take still photos inside the selected work occurrence without making the app a second photo library.

Scope:
- launch camera for the selected work order;
- capture still photos only;
- bind each accepted photo to the exact selected work-order folder identity before camera launch;
- retain an unconfirmed photo locally until Drive success or explicit discard;
- survive app/process restart with unconfirmed photos;
- camera capture must work without internet.

Current status:
- runtime and automated Android CI verification are complete on PR #11;
- PR remains unmerged pending a physical Android camera/restart reality check.

No video, AI classification, OCR, watermarking, or permanent in-app gallery.

### Phase 6A — Photo Preparation — IN PROGRESS — DEVICE GATE BLOCKED

Goal: reduce upload size without weakening the protected original.

Scope:
- create a separate smaller prepared JPEG;
- preserve correct orientation and field-documentation usability;
- never overwrite/delete the protected original during preparation;
- deterministic prepared identity from the immutable local photo UUID;
- perform expensive preparation off the UI thread;
- block conflicting local actions while preparation owns a photo.

Current status:
- runtime and complete Android CI/emulator image verification are complete on PR #12;
- PR remains unmerged pending a short real-camera-photo visual/responsiveness check and its Phase 5 dependency.

### Phase 6B — Drive Upload — IN PROGRESS — H4 DEVICE GATE BLOCKED

Goal: send one prepared photo to its exact bound Drive work-order destination and record confirmed remote identity safely.

Scope:
- upload only to the exact provider document identity already stored on the photo;
- mark uploaded only after confirmed remote creation;
- retain enough remote identity to avoid knowingly duplicating confirmed uploads;
- preserve the recoverable local photo if upload fails or remote outcome is uncertain;
- use the Android safe-folder/DocumentsProvider reality gate before merge.

Current status:
- core Phase 6B Drive-upload runtime is implemented on the governed development line;
- Phase 6B-H1 added durable `provisionalRemoteFileId` queue evidence while preserving confirmed `remoteFileId` as success-only;
- Phase 6B-H2 implemented the create → persist provisional identity → write/verify barrier;
- H2 automated verification passed on exact runtime/test head `8d23b061307725589aef69a31a00249744523406`;
- H2 documentation head is `cbdba0ba89f9b302b496360ff97e9808b5d1a7bc`;
- the remaining required Phase 6B-H4 physical Android + real Google Drive `DocumentsProvider` reality gate is blocked until the operator phone is available;
- Level 3 merge approval has not been granted and the H2 pull request remains unmerged;
- Phase 7B reconciliation, automatic retry, and cleanup are not part of Phase 6B and have not been implemented here.

### Phase 7A — Persistent Upload Queue State — IN PROGRESS

Goal: establish durable local upload/retry bookkeeping before real remote upload is connected.

Scope:
- persistent `WAITING`, `UPLOADING`, `FAILED`, `UNCERTAIN`, and `UPLOADED` bookkeeping while preserving capture states;
- immutable local photo identity and exact original destination through every transition;
- attempt count and last-attempt timestamp;
- failed state remains retryable;
- interrupted/ambiguous in-flight state becomes `UNCERTAIN`, not blindly retryable;
- confirmed-success bookkeeping requires explicit confirmed remote identity;
- app/process restart survival;
- one photo's transition cannot corrupt another;
- no Drive upload/write in this subphase;
- no automatic local image cleanup in this subphase.

Detailed implementation record: `docs/PHASE_7A_IMPLEMENTATION_RECORD_2026-09-09.md`.

### Phase 7B — Remote Retry, Reconciliation & Cleanup — PENDING

Goal: finish weak/no-service behavior once real Drive upload exists.

Scope:
- retry failed work to the original immutable destination;
- reconcile uncertain remote results before another create/upload attempt;
- never knowingly duplicate an already confirmed remote photo;
- isolate failures between photos;
- remove temporary local image data only after confirmed Drive success and safe local bookkeeping;
- preserve lightweight remote identity/history needed for duplicate prevention.

Phase 7B implementation details are intentionally not frozen before the Phase 6 physical-device evidence exists. After the Phase 6 gates are completed, use the real Android/Google Drive provider evidence to adjust Phase 7B assumptions as needed, then build Phase 7 as far as can be honestly proven without another phone dependency. Stage again only when the next implementation decision materially requires real-device/provider evidence.

### Phase 8 — Field Workflow / Release Hardening — PENDING

Goal: prove the minimal app works reliably in everyday field use on more than one Android phone.

Scope:
- full operator workflow on the primary phone;
- same workflow on the son's Android phone with his own account access to the shared master folder;
- fix only real friction discovered in field testing;
- simplify/remove unnecessary UI where practical;
- validate restart, weak-network, permission-loss, duplicate prevention, and wrong-destination guards;
- prepare a normal install/update path for continued use.

Feature expansion stays out unless field use proves it is necessary.

## Phase development staging rule

The governed phase-staging process is recorded in:

`docs/PHASE_STAGING_DOCTRINE.md`

Default development cycle:

**Build everything that can be honestly proven without the phone → stage at the next genuine device-dependent boundary → run the smallest required phone reality gate → accept the evidence → adjust only where reality requires it → continue the next phase as far as possible → stage again.**

For the Phase 6 → Phase 7 transition specifically:

- complete and consolidate the outstanding Phase 6 device evidence first;
- use that evidence to finalize or adjust Phase 7B assumptions;
- do not preserve a pre-phone Phase 7 assumption when real provider behavior contradicts it;
- do not stop Phase 7 after every small implementation slice for a phone check that is not yet necessary;
- push Phase 7 to the next point where proceeding further would require guessing about real Android/Google Drive behavior;
- at that point, freeze the tested runtime, record exact CI/artifact evidence, perform a proportional lean/checkpoint review, and stage the next straight-line device gate.

This rule is intended to prevent fragmented implementation, repeated Bash/approval loops, and unnecessary device testing while still stopping before unverified platform behavior becomes architecture.

## Lean architecture baseline

The accepted lean-architecture audit baseline is recorded in:

`docs/LEAN_ARCHITECTURE_BASELINE_2026-09-10.md`

The audit found the current H2 app lean and near the appropriate minimum architecture for its field workflow. No runtime lean-up phase is planned before H4.

Deferred cleanup candidates must be revisited only after the physical H4 evidence is known and during Phase 7B design, when their actual future value can be judged. Runtime safety boundaries are not to be simplified merely to reduce class or line count.

## Product boundary

Keep out unless separately approved:
- permanent in-app photo library;
- workbook integration;
- Free Map Router integration;
- route planning;
- automatic sharing changes;
- general Drive cleanup or file manager behavior;
- video;
- background location tracking;
- OCR;
- AI photo classification;
- separate numeric work-order IDs without a demonstrated ambiguity;
- deeper folder nesting beyond master → address → dated work order → photos.

## Roadmap maintenance rule

- Completed phases should be marked `COMPLETE` only after their required automated and real-device gates pass and the governed change is merged.
- Active work should be marked `IN PROGRESS`.
- New phases or phase splits are allowed when they reduce risk or clarify ownership, but they must be recorded here before the implementation sequence drifts.
- The roadmap guides sequencing; `CONTRACT.md`, `CHANGE_CONTROL_CONTRACT.md`, `TESTING_CONTRACT.md`, and `INTEGRATION_CONTRACT.md` remain authoritative for approved behavior, risk, tests, and Drive safety.
