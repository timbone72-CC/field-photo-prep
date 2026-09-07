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

Validated on the operator phone with both the empty-folder positive reuse and non-empty fail-closed checks, then merged through PR #6.

### Phase 3B — Clear & Reuse — IN PROGRESS

Goal: allow deliberate recycling of a non-empty old work-order folder when the operator explicitly chooses it.

Scope:
- operator selects one exact old work-order folder;
- app shows the old folder name and direct child-item count;
- explicit confirmation is required;
- remove only that selected folder's child items;
- verify the folder is empty;
- rename/reuse the same folder identity for the new work/date;
- any delete, verification, or rename failure stops the workflow.

No automatic cleanup or bulk Drive management.

### Phase 4 — Address Folder Creation — PENDING

Goal: create a missing property/address folder safely under the approved master folder.

Scope:
- enter/select address text;
- refresh actual master-folder children before create;
- one exact match → reuse;
- multiple exact matches → operator choice;
- no match → create exactly one address folder under the approved master;
- persist returned folder identity;
- never recycle address folders.

### Phase 5 — Camera + Temporary Photo Protection — PENDING

Goal: take still photos inside the selected work occurrence without making the app a second photo library.

Scope:
- launch camera for the selected work order;
- capture still photos only;
- bind each accepted photo to the exact selected work-order folder identity;
- retain an unconfirmed photo locally until Drive success or explicit discard;
- survive app/process restart with unconfirmed photos;
- camera capture must work without internet.

No video, AI classification, OCR, watermarking, or permanent in-app gallery.

### Phase 6 — Photo Preparation + Drive Upload — PENDING

Goal: reduce upload size and send photos to the exact selected Drive destination.

Scope:
- create a smaller prepared upload copy;
- preserve correct orientation and field-documentation usability;
- upload only to the exact bound work-order folder;
- mark uploaded only after confirmed remote creation;
- retain enough remote identity to avoid knowingly duplicating confirmed uploads;
- preserve the recoverable local photo if preparation or upload fails.

### Phase 7 — Offline Queue, Retry & Cleanup — PENDING

Goal: make the app dependable in weak/no-service field conditions.

Scope:
- persistent waiting/failed queue;
- retry to the original immutable destination;
- app/process restart survival;
- one photo failure does not corrupt others;
- uncertain remote result must be reconciled or surfaced rather than blindly duplicated;
- remove temporary local image data only after confirmed Drive success and safe local bookkeeping.

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
