# Phase 8C — Second Android Phone / Shared-Master Reality Gate

Date: 2026-09-28  
Result: **PASS / COMPLETE**  
Runtime: Field Photo Prep Internal `0.28.0-internal`

## Purpose

Prove the existing Field Photo Prep workflow on a second physical Android phone using a deliberately established local Android SAF / Google Drive binding rather than assuming the primary phone's local provider state is portable.

This is a physical reality-gate record only. No Android runtime, backend, Drive permission model, queue semantics, or destination construction was changed.

## Pre-gate cleanup

The second phone contained 20 old local `WAITING` photos from earlier testing. The operator explicitly confirmed they were obsolete and disposable.

The app's data was cleared on that phone, which removed only the internal app-local queue/protected-photo/session/binding state. Already-uploaded Google Drive files were not deleted.

Clean baseline after sign-in:

- app version: `0.28.0-internal`;
- account state: `VALIDATED`;
- role: `MEMBER`;
- Drive state: `NO_WORKSPACE`;
- queue states: all zero;
- protected originals: 0;
- cleanup pending: 0;
- unreadable local records: 0.

## Second-phone Drive binding

Using the Android system folder picker, the installation deliberately selected the existing `Photos` workspace.

After the grant:

- workspace: `Photos`;
- company: `HNP Jobs`;
- existing property count discovered: 25;
- existing property selected: `99999 FPP UNIQUE CREATE TEST`;
- existing work order selected: `test - 2026-09-16`.

No duplicate property or work-order folder was created as part of the gate.

## Capture and preparation

One disposable photo was captured with the in-app camera.

Observed local state:

- state: `WAITING`;
- prepared copy: 618 KB;
- photo was selectable as `Ready to upload`.

## Upload result

The disposable photo was selected and uploaded.

Observed result:

- batch: 1 of 1 confirmed in Drive;
- durable state: `UPLOADED`;
- upload attempt: 1;
- local copies: cleaned up after confirmed remote success;
- no `FAILED` or `UNCERTAIN` state occurred.

## Independent Drive verification

Read-only Google Drive inspection independently verified the newly uploaded file in:

`Photos → HNP Jobs → 99999 FPP UNIQUE CREATE TEST → test - 2026-09-16`

Verified file:

- name: `001_field-photo-73df4444-8205-4436-a777-d401e7da91a5.jpg`;
- MIME type: `image/jpeg`;
- size: 632,714 bytes;
- created: `2026-09-29T02:19:56.569Z`;
- exact parent folder: existing `test - 2026-09-16` work-order folder;
- fetched image rendered successfully and was visually usable.

The same work-order folder also retained its earlier historical test image; the new upload did not create a replacement work-order folder.

## Restart persistence

Field Photo Prep Internal was removed from Recent Apps and reopened.

The app reopened with:

- `HNP Jobs`;
- workspace `Photos`;
- 25 properties still discoverable.

Final App Status after restart:

- app version: `0.28.0-internal`;
- account state: `VALIDATED`;
- role: `MEMBER`;
- Drive state: `USABLE`;
- camera permission: `GRANTED`;
- `CAPTURING: 0`;
- `WAITING: 0`;
- `UPLOADING: 0`;
- `FAILED: 0`;
- `UNCERTAIN: 0`;
- `UPLOADED: 1`;
- protected originals: 0;
- cleanup pending: 0;
- unreadable local records: 0.

## Conclusion

Phase 8C passes for its intended second-device/shared-master boundary.

The second phone established its own local provider/SAF access, rediscovered existing Drive hierarchy, reopened existing identities without duplication, captured/prepared/uploaded one disposable photo to the exact existing destination, confirmed the remote image, cleaned local copies only after success, and preserved a usable binding and confirmed state across restart.

This evidence supports second-device portability of the governed workflow without copying primary-phone provider state.

It does **not** prove or claim:

- client-company Google Drive ACL isolation;
- Drive access for an account that has not independently been granted access;
- portability of SAF URIs or device-local provider state themselves.

Those remain separate boundaries.
