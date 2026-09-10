# Phase 6B-H1 Implementation Record — Queue Schema + Provisional Remote Identity

Date: 2026-09-10

## Goal

Implement only the first runtime slice of the Phase 6B upload-safety hardening plan: extend the durable pending-photo queue so it can remember the exact provider document identity returned by a future remote create operation before that identity becomes confirmed upload success.

This slice deliberately does **not** change Android SAF/Google Drive write sequencing. Its purpose is to make the local state model capable of safely receiving and preserving provisional remote identity before Phase 6B-H2 connects that state to `createDocument()`.

## Governing design

This implementation follows:

`docs/PHASE_6B_UPLOAD_SAFETY_HARDENING_2026-09-09.md`

The relevant design rule is:

`UPLOADING → remote create returns identity → durably persist provisional identity → only later may H2 stream bytes`

H1 implements only the durable queue/schema side of that rule.

## Exact baseline

Repository:

`timbone72-CC/field-photo-prep`

Base branch:

`feat/phase-6b-drive-upload`

Exact base head:

`3f52712597cac1d986ea7131cb8b02305f223763`

H1 branch:

`feat/phase-6b-h1-provisional-remote-identity`

Draft pull request:

`#16 — Phase 6B-H1: queue schema + provisional remote identity`

Exact final H1 runtime/test head:

`2bc2e598f1298f562f561f6e16ffc0f2c6057f7d`

The documentation commit that adds this record is not runtime evidence. All automated runtime evidence below is tied specifically to `2bc2e598f1298f562f561f6e16ffc0f2c6057f7d`.

## Change level

Level 3.

Reason: H1 changes persisted pending-photo metadata schema and duplicate-protection identity semantics. A bad migration or state transition could hide remote-side-effect evidence or make an unsafe retry appear valid.

Implementation authorization was supplied by the operator's explicit instruction to proceed with **6B-H1 — queue schema + provisional remote identity**.

Pre-merge approval is **not granted** by that implementation instruction. PR #16 remains draft/unmerged.

## Approved scope implemented

H1 changes only local pending-photo queue persistence and its automated tests:

- bump pending-photo metadata schema from version 2 to version 3;
- add nullable persisted `provisionalRemoteFileId`;
- keep schema versions 1 and 2 readable;
- load all valid schema-v2 records with `provisionalRemoteFileId = null`;
- write schema version 3 on the next normal record write;
- allow provisional remote identity only where remote creation may already have occurred: `UPLOADING` and `UNCERTAIN`;
- allow `UPLOADING` to exist before provisional identity is known;
- preserve provisional identity when `UPLOADING` becomes `UNCERTAIN`, including process-start interrupted-upload recovery;
- make provisional identity immutable within one upload attempt;
- allow an idempotent repeat write of the same provisional identity;
- reject replacement with a different provisional identity;
- prevent an upload carrying provisional remote evidence from becoming retryable `FAILED`;
- require confirmed remote identity to equal the recorded provisional identity when one exists;
- on confirmed success, clear the provisional field and persist the exact matching identity as confirmed `remoteFileId` in `UPLOADED`;
- preserve existing immutable photo/address/work-order identities and upload attempt bookkeeping;
- preserve protected local image data through H1 state changes.

## Persisted schema version 3

Schema version 3 retains all version-2 fields and adds:

`provisionalRemoteFileId`

Semantics:

- `null`/empty before a remote create identity has been durably recorded;
- nonblank only when an exact provider-created document identity is known for the active upload attempt;
- duplicate-protection/reconciliation evidence only;
- never equivalent to confirmed upload success;
- never manufactured from display name, path, timestamp, current UI selection, or deterministic filename;
- retained across app/process restart while the result remains in-flight or uncertain;
- cleared when the same exact identity is promoted to confirmed `remoteFileId` on `UPLOADED`.

`remoteFileId` remains confirmed-success identity only.

## Backward compatibility and migration

### Schema version 1

Existing schema-v1 `CAPTURING` and `WAITING` records remain readable with their existing safe queue defaults. Their next persisted write uses schema version 3 and `provisionalRemoteFileId = null`.

### Schema version 2

Existing valid schema-v2 queue records remain readable without rewriting their meaning.

On load:

- all existing immutable capture/address/work-order identity fields are preserved;
- state is preserved;
- upload attempt count and last-attempt timestamp are preserved;
- failure/uncertainty detail is preserved where valid;
- existing confirmed `remoteFileId` is preserved for `UPLOADED` records;
- `provisionalRemoteFileId` defaults to `null` because version 2 did not contain that evidence.

The next record write serializes schema version 3.

### Unknown future versions

Unknown schema versions continue to fail closed. Paired image data is not deleted merely because metadata cannot be interpreted.

## State invariants

H1 makes the remote-identity state distinction explicit:

### `CAPTURING` / `WAITING`

Must contain neither provisional nor confirmed remote identity.

### `UPLOADING`

Must represent an active upload attempt. `provisionalRemoteFileId` may be null before create identity is recorded or non-null after it is recorded. Confirmed `remoteFileId` must remain null.

### `FAILED`

Remains the retryable deterministic-failure state. It may contain neither provisional nor confirmed remote identity. Once provisional remote identity exists, normal `markUploadFailed(...)` is rejected so a possible remote side effect cannot be made blindly retryable.

### `UNCERTAIN`

Must preserve upload-attempt detail and may carry a provisional remote identity. It remains non-retryable until a later reconciliation phase proves a safe outcome.

### `UPLOADED`

Must contain confirmed `remoteFileId` and no provisional identity. If a provisional identity existed, confirmation must use the exact same identity.

## New local queue API

`PendingPhotoStore.recordProvisionalRemoteFileId(photoId, providerDocumentId)`

Behavior:

1. reads the exact persisted photo record;
2. requires current state `UPLOADING`;
3. requires a nonblank provider identity;
4. records the first provisional identity atomically using the existing durable metadata replacement path;
5. permits an idempotent repeat of that same identity;
6. rejects a different replacement identity;
7. performs no remote operation.

The corresponding `PendingPhotoRecord` transition owns validation of these invariants.

## Ownership

Only the queue/persistence layer changes behavior in H1:

- `PendingPhotoRecord` owns schema version, field semantics, and valid state transitions;
- `PendingPhotoStore` owns durable atomic persistence of that state;
- queue/schema tests own migration and transition verification.

The following owners remain unchanged in H1:

- `DrivePhotoUploader` still owns the existing pre-hardening create/write/verify flow;
- `PhotoUploadCoordinator` is not yet wired to persist provisional identity between create and write;
- `PhotoCaptureActivity` remains UI/initiation only;
- `PhotoPreparer` remains unchanged.

That create/persist/write sequencing change belongs exclusively to Phase 6B-H2.

## Exact changed runtime/test files

At the final H1 runtime/test head `2bc2e598f1298f562f561f6e16ffc0f2c6057f7d`, the changes relative to the H1 base are limited to:

- `app/src/main/java/com/inandout/fieldphotoprep/PendingPhotoRecord.java`
- `app/src/main/java/com/inandout/fieldphotoprep/PendingPhotoStore.java`
- `app/src/test/java/com/inandout/fieldphotoprep/PendingPhotoProvisionalIdentityTest.java`
- `app/src/test/java/com/inandout/fieldphotoprep/PendingPhotoQueueStateTest.java`
- `app/src/test/java/com/inandout/fieldphotoprep/PendingPhotoSchema3MigrationTest.java`

No Drive uploader, upload coordinator, camera, photo-preparation, UI, manifest, Gradle, workflow, folder, permission, OAuth, or remote-storage code is changed by H1.

## Automated coverage

H1-focused coverage proves:

- schema-v1 waiting records still load with safe defaults and upgrade on next write;
- schema-v1 interrupted capture still reconciles safely and upgrades on write;
- schema-v2 `UPLOADING` loads with null provisional identity and upgrades to schema 3 when provisional identity is persisted;
- schema-v2 `UNCERTAIN` preserves state, attempt metadata, status detail, immutable destination identity, and null provisional identity;
- schema-v2 `UPLOADED` preserves confirmed remote identity with null provisional identity;
- unknown schema versions still fail closed without deleting paired protected image data;
- provisional identity persists across a new store/process instance;
- process-start `UPLOADING → UNCERTAIN` recovery preserves provisional identity and destination;
- provisional identity prevents transition to retryable `FAILED`;
- provisional identity is immutable within one attempt;
- repeated persistence of the same provisional identity is idempotent;
- confirmed identity mismatch is rejected while the record and provisional evidence remain intact;
- matching confirmation promotes provisional identity to confirmed `remoteFileId` and clears the provisional field;
- invalid `WAITING` + provisional identity metadata fails closed;
- one photo's provisional identity does not alter another photo's state or destination;
- existing queue/capture/preparation/upload tests remain part of the complete suite.

## Final automated verification

Exact tested runtime/test head:

`2bc2e598f1298f562f561f6e16ffc0f2c6057f7d`

Android CI run:

`34466189190`

Job:

`102835223549`

Result: **PASS**.

All workflow steps completed successfully on that exact head:

- complete JVM/unit test suite;
- debug APK build;
- KVM setup;
- Android connected instrumentation tests;
- app launch smoke test;
- debug APK artifact packaging.

Artifact:

- ID: `10147756160`
- name: `field-photo-prep-phase-1-debug-apk`
- size: `2439644` bytes
- digest: `sha256:af66fdabb630860f01d53e4dacdf7becc63684f7d4d2acc26dbbe17b53e1d4d7`

The artifact name is inherited from the existing workflow and is not evidence that this runtime is Phase 1.

## Device / Drive reality-gate status

No physical Android/Google Drive provider gate is required to prove H1 itself because H1 performs no SAF call, no `DocumentsProvider` operation, no remote read, and no remote write. Its changed behavior is app-private queue serialization and state validation.

This does **not** waive the Android Google Drive reality gate for the complete Phase 6B hardening. H2 will change the provider/coordinator sequencing around remote create/write and therefore the hardened upload path still requires the affected physical-device SAF/Google Drive gate before Level 3 merge approval.

## Failure posture

H1 deliberately chooses preservation over convenience:

- a provisional identity never means success;
- a provisional identity blocks retryable failure;
- interrupted in-flight state becomes `UNCERTAIN` and preserves the provisional ID if one was durably stored;
- confirmation cannot silently switch to a different remote document identity;
- invalid schema/state data fails closed;
- no H1 failure deletes protected local image data or remote content.

## Rollback

Pre-H1 branch/runtime baseline:

`3f52712597cac1d986ea7131cb8b02305f223763`

While H1 remains an unmerged development branch, rollback is simply to abandon/revert this isolated H1 branch. No user Drive data or installed app data has been modified by repository implementation alone.

Important future downgrade rule:

Once a schema-v3-capable runtime is actually installed and has written version-3 queue records, an older version-2-only runtime will intentionally reject those records as an unknown future schema. Therefore a future production downgrade over active schema-v3 pending-photo records must not be treated as a routine binary rollback. Preserve the records/photos and use a compatible forward fix or explicitly designed downgrade/migration path rather than deleting or rewriting queue evidence.

Rollback must never delete a protected original, prepared photo, provisional identity evidence, confirmed remote identity, or remote Drive photo merely to make an older runtime accept the queue.

## Explicit non-scope

H1 does not implement or authorize:

- any change to `DrivePhotoUploader`;
- any change to SAF `createDocument()` behavior;
- streaming prepared bytes after provisional persistence;
- create/persist/write barrier wiring;
- Google Drive uncertainty reconciliation;
- filename changes;
- automatic/background retry;
- remote delete or overwrite;
- local original/prepared-copy cleanup;
- camera changes;
- photo preparation changes;
- folder create/reuse/rename/delete changes;
- sharing/permission changes;
- app-managed Google OAuth;
- Room/SQLite migration;
- workbook or Free Map Router integration.

## Approval / merge status

H1 implementation was authorized by the operator.

PR #16 remains **draft and unmerged**. No Level 3 pre-merge approval has been granted.

The next permitted hardening slice after H1 is:

**Phase 6B-H2 — Create/Persist/Write Barrier**

H2 must build on the verified H1 schema/runtime and change only the provider/coordinator boundary necessary to persist the created provider identity before byte streaming begins.
