# Phase 10F — Explicit Android Backup / Restore Rules

Date: 2026-09-17

Status: **LEVEL 3 — COMPLETE**

Branch: `phase-10f-explicit-backup-rules-20260917`

## Problem

Field Photo Prep currently declares `android:allowBackup="true"` without explicit backup rules.

Android Auto Backup includes most app-private files by default, and Android 12+ treats cloud backup and device-to-device transfer as separate policy surfaces. FPP stores provider/account-bound Google Drive identity and protected operational photo state in locations that would otherwise be eligible for backup/transfer.

Current provider-bound state:
- `shared_prefs/field_photo_prep.xml`
  - persisted SAF master-tree URI;
  - master provider document ID/name;
  - selected property provider document ID/name;
  - selected work-order provider document ID/name;
  - work-order parent binding.

Current protected operational files:
- `files/pending_photos/`
  - protected originals;
  - queue metadata;
  - provisional/confirmed remote identity evidence;
  - capture-sequence ledger and reuse-occurrence markers.
- `files/prepared_photos/`
  - upload derivatives tied to the same local queue identity.

These values are safe only in the device/provider/account context in which they were created.

## Android platform evidence

Android's current backup guidance says:
- Auto Backup is enabled by default for eligible apps and includes most app-private files unless rules exclude them;
- restored URIs can be unstable and should not be backed up when they identify device/provider resources;
- Android 12+ requires `android:dataExtractionRules` to control cloud backup and device-to-device transfer separately;
- Android 11 and lower still require the legacy `android:fullBackupContent` rules.

FPP targets API 36 and supports API 26+, so both rule formats are required.

## Decision

FPP currently has **no device-independent user preference that is worth restoring**.

Therefore the safest rule is:
- keep `android:allowBackup="true"` so Android uses the declared policy machinery;
- explicitly exclude all app-owned credential-protected and device-protected file, database, preference, root, and external domains from:
  - Android 12+ cloud backup;
  - Android 12+ device-to-device transfer;
  - Android 11-and-lower Auto Backup.

This intentionally means a new/restored phone starts without FPP's prior Drive binding or local protected queue.

The operator must deliberately reconnect/select the approved Drive master on the new device. This prevents provider IDs, SAF URIs, pending queue state, capture numbering state, or local photo copies from being silently treated as portable.

## Why not transfer pending photos

Pending/prepared photo data is operational safety state, not a portable photo library.

Moving it to another device without proving the exact provider/account context could:
- bind a queued photo to a provider ID that has different meaning or is inaccessible on the destination device;
- surface stale provisional remote evidence;
- risk incorrect retry/reconciliation assumptions;
- exceed normal cloud-backup size expectations because protected originals can be large;
- create a false impression that migration itself proves cross-device provider identity.

The safe rule is to finish/upload/reconcile protected work on the source phone before migration, or deliberately keep the source phone until that work is resolved.

## Read/write surfaces

Phase 10F reads:
- Android backup policy;
- manifest configuration;
- existing app-private storage ownership.

Phase 10F writes:
- manifest backup-rule references;
- Android backup policy XML resources;
- focused instrumentation coverage;
- internal build version metadata;
- documentation.

It performs no Drive create/write/delete/rename, no queue transition, no photo cleanup, no provider permission mutation, and no migration of existing app-private data.

## Schema / identity / permission impact

No FPP data schema changes.

No Drive provider identity changes.

No SAF permission request changes.

Android may independently restore OS-level granted permissions, but FPP's own saved URI/provider references are excluded. Without those app-owned references, restored OS permission state is not accepted as a selected Drive destination.

## Runtime behavior after restore / transfer

Expected safe behavior:
- no saved master-tree URI or provider IDs are restored by FPP backup;
- no pending/protected/prepared photo queue is restored;
- app opens as not connected and requires deliberate Drive selection;
- normal provider discovery/revalidation begins from the destination device's own current provider context;
- Phase 8C remains required before claiming cross-device/account portability.

## Protected behavior

Unchanged:
- local originals on the current device remain protected until confirmed upload or explicit safe discard;
- queue/upload/retry/reconciliation semantics on the current installation;
- exact Drive destination identity;
- capture sequence behavior;
- Clear & Reuse;
- normal in-place app update behavior.

These rules affect OS backup/restore and D2D migration only; they do not clear the current installation.

## Tests

Required:
- Android resources compile;
- manifest retains `android:allowBackup="true"` and points to both explicit rule files;
- Android 12+ rule resource excludes every supported app-owned backup domain from both cloud and D2D transfer;
- legacy full-backup resource excludes the same app-owned domains;
- complete Android CI on the exact final runtime head.

No customer content is used.

## Rollback

Rollback is the pre-10F `main` commit.

Reverting 10F restores the prior implicit Android backup behavior; it does not restore data that Android never transferred and it must not be used as a mechanism to copy provider-bound state between devices.

## Approval

Implementation preparation is authorized by the operator's instruction to continue the roadmap.

Because this changes OS backup/restore and device-transfer handling of provider-bound operational state, explicit Level 3 operator approval was required immediately before merge and was **APPROVED** by the operator.


## Completion evidence

- versionCode 29 / `0.24-explicit-backup-rules`;
- exact runtime head `190b3c0a541518052127698f811da330ef8c0cf8`;
- Android CI run `35300920973`: PASS;
- unit tests, internal debug build, stable signer verification, explicit backup-rule instrumentation, full instrumentation, launch smoke, and artifacts passed;
- explicit Level 3 operator approval was received immediately before merge;
- PR #55 merged to `main` at `4ec3f00dc169f0ebb5c5bfe22e80e8528a1059ac`.

No physical-phone install was required for this gate because the change governs future OS backup/device-transfer behavior only and does not mutate the current installation's data or Drive state.

Phase 8C remains the physical second-device/provider reality gate.
