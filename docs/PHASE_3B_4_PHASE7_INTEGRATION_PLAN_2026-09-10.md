# Phase 3B + Phase 4 Integration onto Phase 7 Main — 2026-09-10

Status: IMPLEMENTATION AUTHORIZED — pre-merge authorization already granted by operator, merge remains verification-gated.

## Purpose

Integrate the already implemented and physically validated Phase 3B Clear & Reuse behavior from PR #8 and Phase 4 Address Folder Creation behavior from PR #10 onto the current Phase-7 `main` without regressing the merged Phase 5 → 7B photo, queue, upload, reconciliation, cleanup, or stable test-signing behavior.

## Change level

Level 3.

Reasons:
- Phase 3B intentionally deletes direct child content and renames one selected work-order folder after explicit confirmation;
- Phase 4 creates a persistent Drive address folder and establishes destination identity;
- integration touches Drive/provider and destination-selection code shared with the current runtime.

## Governed base / rollback

Repository: `timbone72-CC/field-photo-prep`

Integration branch: `integrate/phase-3b-4-onto-phase7-main`

Exact current-main base / rollback point:

`b68ae04d90ab1ed1b414d2ed3136bc5b7672055c`

That base contains the merged Phase 5 → 7B runtime plus stable test signing and passed the complete Android CI gate after integration to `main`.

## Source evidence to preserve

Phase 3B source branch / PR:
- `feat/phase-3b-clear-reuse`
- PR #8
- tested runtime `0a7071744791b470f1c0bd795d78934c8f28c87e`
- Android CI `34302110486` PASS
- Samsung Galaxy A16 real-provider gate PASS on 2026-09-10 as recorded in `docs/DEVICE_REALITY_GATE_RECORD_2026-09-10.md`.

Phase 4 source branch / PR:
- `feat/phase-4-address-folder-creation`
- PR #10
- tested runtime `a1ca4f5e1ecd871f24d0724b6c1239bfd9666570`
- Android CI `34425740379` PASS
- Samsung Galaxy A16 real-provider create/reuse/restart gate PASS on 2026-09-10 as recorded in `docs/DEVICE_REALITY_GATE_RECORD_2026-09-10.md`.

## Approved integration behavior

### Phase 4 address creation
- operator enters an address folder name;
- outer whitespace is trimmed and blank names are rejected;
- immediately before a no-match create decision, obtain provider state fresh/settled enough for an absence decision;
- one exact case-sensitive name match reuses that exact provider document ID;
- multiple exact matches create nothing and require operator choice;
- no exact match creates exactly one folder directly under the approved master provider document ID;
- verify the returned created identity against refreshed provider state before treating it as selected;
- persist the exact selected/created address provider ID and display name through existing `FolderPrefs` state;
- never rename, recycle, clear, delete, or permission-modify address folders.

### Phase 3B Clear & Reuse
- only an explicitly selected non-empty older same-work-order folder may enter Clear & Reuse;
- re-resolve address/work-order by exact provider identity before destructive work;
- confirm the requested new dated target does not already exist;
- obtain a provider state authoritative enough to enumerate the selected old folder's direct children;
- show master, address, old folder, direct child count, requested new folder, and child-folder warning where applicable;
- require explicit confirmation;
- re-read/revalidate exact child identities after confirmation and before deletion;
- delete only the confirmed direct children;
- stop on first deletion failure and do not rename a partial result;
- verify authoritative-enough zero-child state before rename;
- rename/reuse the same work-order provider identity under the same address parent;
- verify and persist that same identity after rename;
- block further risky writes until refresh after partial/uncertain destructive results.

### Shared provider freshness rule
- request provider refresh where supported;
- `DocumentsContract.EXTRA_LOADING=true` is non-authoritative;
- one cloud-provider child query is not proof of absence/emptiness;
- risky absence/emptiness decisions require the already-tested matching settled snapshot behavior;
- stale/loading/inconsistent/unverifiable state fails closed.

## Owning files expected to change

Runtime:
- `app/src/main/java/com/inandout/fieldphotoprep/DriveClient.java`
- `app/src/main/java/com/inandout/fieldphotoprep/MainActivity.java`
- `app/src/main/java/com/inandout/fieldphotoprep/AddressFolderName.java` if absent from current main.

Tests:
- `app/src/test/java/com/inandout/fieldphotoprep/DriveClientTest.java`
- `app/src/test/java/com/inandout/fieldphotoprep/AddressFolderNameTest.java` if absent from current main.

Documentation/status:
- this integration record;
- `docs/ROADMAP.md` after verification/merge status is known.

Obsolete historical build-version edits from PR #8/#10 are explicitly not ported. Current Phase 7B versioning and stable test signing remain authoritative.

## Read/write surfaces

Reads:
- persisted SAF master tree URI and exact master provider identity;
- direct child address folders under the approved master;
- direct work-order folders under the selected address;
- exact direct-child identities under one selected old work-order folder;
- existing current address/work-order identity state.

Drive writes:
- Phase 4: at most one new address directory directly under the exact approved master after settled no-match evidence;
- Phase 3B: only after explicit confirmation, delete the exact confirmed direct-child identities of one selected old work-order folder and rename that same folder after verified emptiness.

Local writes:
- existing selected address/work-order identity fields;
- existing fail-closed UI/write-block state where required by the approved implementations.

No photo image, queue schema, upload remote identity, signing identity, manifest permission, sharing, or OAuth write is introduced.

## Protected current-main behavior

Must remain unchanged:
- exact photo-to-work-order binding before camera launch;
- protected original persistence until confirmed upload or explicit safe discard;
- prepared JPEG behavior and orientation;
- queue schema v3 and `WAITING/UPLOADING/FAILED/UNCERTAIN/UPLOADED` semantics;
- H2 create → durable provisional identity → write/verify barrier;
- deterministic remote filename;
- Phase 7B uncertainty reconciliation and retry-release rules;
- post-confirmed local cleanup;
- stable non-production debug/test signer and CI signer verification;
- `applicationId com.inandout.fieldphotoprep`;
- no automatic retry scheduler or new architecture framework.

## Duplicate/idempotency behavior

Address create:
- exact existing match => reuse;
- duplicate exact matches => no create/guess;
- no settled proof of absence => no create;
- ambiguous/unverified create result => no blind repeated create until refresh/inspection.

Clear & Reuse:
- existing requested new dated target => do not clear old folder;
- changed child snapshot => no deletion;
- partial/uncertain deletion or rename => no blind retry and no new-work upload into the affected folder until refreshed/resolved.

Photo upload duplicate/reconciliation behavior remains owned by Phase 7B and must not be changed by this integration.

## Offline / stale-state behavior

- ordinary camera capture remains offline-capable;
- Drive create/reuse/destructive actions require accessible provider state;
- stale/loading/inconsistent provider state stops absence/emptiness-based writes;
- provider/access failure must not delete any unconfirmed photo or alter queued photo destination identity.

## Safe Drive reality fixture

Use only the established disposable hierarchy under:

`HNP Jobs → FIELD PHOTO PREP TEST`

and the previously created disposable Phase 4 address fixture where useful.

Do not use a live customer/job folder.

## Verification plan

Focused automated verification:
- port and run Phase 3B DriveClient tests covering refresh/loading/settled child snapshots, exact identity, child-count/folder-count, snapshot-change rejection, and existing Phase 3A eligibility;
- port and run Phase 4 tests covering trim/blank name validation, exact case-sensitive match/reuse, duplicate detection, no-match creation decision, provider freshness, returned-ID verification, and duplicate prevention;
- preserve current Phase 5–7 tests unchanged and compiling.

Final automated gate:
- complete repository Android CI exactly once on the final integration runtime head;
- require unit tests, debug build, stable signer verification, Android instrumentation/install/launch smoke, and artifact packaging to pass.

Physical Android / real Google Drive smoke after integration:
- verify existing Phase 4 exact-match reuse on the safe master without duplication;
- verify one safe no-match/create-or-existing-reuse fixture path as practical without adding clutter;
- verify Phase 3B can still obtain settled real-provider child state and show the correct hierarchy/count;
- use cancel for the destructive confirmation unless a fresh destructive fixture is needed; prior full destructive success evidence remains valid, but the integrated owning path must at minimum prove it reaches the correct real-provider confirmation safely;
- confirm unrelated Drive content unchanged;
- verify the current Phase 7B photo screen/confirmed-upload records still resolve after the integrated build.

If current integration behavior materially differs from the previously tested Phase 3B/4 path, stop and expand the physical gate rather than relying on historical evidence.

## Failure recovery

- focused test failure: stop, fix only the owning port, rerun focused test first;
- full CI failure: stop merge;
- real-provider ambiguity/loading: fail closed; do not create/delete/rename around it;
- destructive integration uncertainty: do not retry blindly; inspect the exact safe fixture;
- photo/queue/upload regression: revert the integration branch to base `b68ae04d90ab1ed1b414d2ed3136bc5b7672055c` and stop affected testing.

## Approval

Implementation authorization: GRANTED by the operator's instruction to continue the controlled integration.

Explicit Level-3 pre-merge authorization: GRANTED by the operator's prior instruction, “You may merge anything that needs merged.”

That authorization does not waive required verification. Merge remains blocked by any required automated or device failure.
