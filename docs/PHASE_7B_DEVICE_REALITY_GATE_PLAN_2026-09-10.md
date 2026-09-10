# Phase 7B Device Reality Gate Plan — Reconciliation + Confirmed Local Cleanup

Date: 2026-09-10

Status: **STAGED — WAITING FOR SAMSUNG GALAXY A16 EXECUTION**

## Purpose

This is the straight-line physical-device gate for the exact Phase 7B runtime after all automatable work passed.

The session exists to prove only the parts automation cannot honestly prove against the real Android Google Drive `DocumentsProvider`: upgrade-safe cleanup of already confirmed local image copies, immediate cleanup after a fresh confirmed upload, preservation of Drive content and confirmed metadata, and—only if a safe natural fixture appears—the new read-only reconciliation path.

Do not broaden this session into feature testing, Drive cleanup, repeated fault injection, or branch integration.

## Exact frozen build

Repository: `timbone72-CC/field-photo-prep`

Branch: `feat/phase-7b-remote-reconciliation-cleanup`

Exact runtime/test head:

`f25868dd768dcdddb11ac4d6ab3879a7cde85d2c`

Version:

- `versionCode 15`
- `versionName 0.10-phase7b-reconciliation-cleanup`

CI evidence:

- workflow run: `34512362110`
- job: `102989361359`
- result: **PASS**
- artifact ID: `10166488011`
- artifact name: `field-photo-prep-phase-1-debug-apk` (legacy workflow label)
- SHA-256: `544a834c701e16f650cd2738d2ccd76be4a9aa031900974f0027f65a265689c5`
- artifact size: `2451856` bytes

Do not substitute a locally rebuilt APK or another workflow artifact during this gate.

## Device / provider target

- Device: Samsung Galaxy A16
- Provider: Android Storage Access Framework + Google Drive document provider
- Approved master: `HNP Jobs`
- Safe address: `FIELD PHOTO PREP TEST`
- Existing H4 work order: `Cut Grass - 2026-09-21`
- Existing H4 confirmed photo UUID: `88ba77d6-2af9-4a3d-ad07-0bdefe30232c`
- Existing H4 Drive JPEG: `field-photo-88ba77d6-2af9-4a3d-ad07-0bdefe30232c.jpg`
- Existing H4 Drive backend file ID observed previously: `1wEEcTKq6hdOjzfR-1079F7Kh_D0opIsB`

The app's persisted provider DocumentId remains the governed app identity. Do not replace it with the backend Drive file ID.

## Fixed rails

The following are not operator-discretion items during this gate:

1. **Do not uninstall the existing app before installing Phase 7B.** Install/update over the existing app so H4 queue metadata and retained local files survive into the upgrade test.
2. **Do not clear app storage/data.**
3. **Do not delete, rename, move, or overwrite the H4 Drive JPEG.**
4. **Do not intentionally interrupt a Drive upload to manufacture `UNCERTAIN`.**
5. **Do not press Upload again for a record that is `UNCERTAIN`.**
6. **Do not create duplicate remote files as a reconciliation test.**
7. **Do not alter provider IDs or destination identity.**
8. **Do not treat a cleanup failure as an upload failure.** If the queue remains `UPLOADED`, remote success remains confirmed.
9. **Do not merge any PR from this test session.** Merge approval is a separate Level 3 decision after evidence is recorded.

## Allowed spot decisions

Harmless operator discretion is allowed for:

- taking an extra screenshot when a message disappears quickly;
- waiting briefly for Drive UI/backend visibility after the app reports provider-confirmed success;
- one normal Drive/app refresh if the provider is visibly still loading;
- choosing a harmless subject for the fresh test photo;
- stopping early when evidence is contradictory or unexpectedly ambiguous.

Do not use spot discretion to change the test objective, retry an ambiguous upload, or modify Drive content to make a test pass.

## Preflight — before opening the Phase 7B photo screen

1. Confirm the existing app still has access to `HNP Jobs`.
2. In Google Drive, confirm the H4 JPEG still exists under:

   `HNP Jobs → FIELD PHOTO PREP TEST → Cut Grass - 2026-09-21`

3. Open the H4 JPEG once and confirm it is still usable.
4. If practical, capture one screenshot showing the H4 Drive JPEG before the update.
5. Install the exact Phase 7B artifact **over** the existing app. Do not uninstall first.
6. Open the app and confirm the selected master remains `HNP Jobs` or can still be read from the persisted grant.

If the update installation unexpectedly clears app data or loses the persisted master grant, **STOP** and record that result. Do not rebuild the queue by hand and continue as though the upgrade passed.

## Gate 7B-A — upgrade cleanup of the existing H4 confirmed photo

### Objective

Prove that a previously confirmed `UPLOADED` photo can shed its retained local image copies after upgrading to Phase 7B without losing confirmed metadata or changing Drive.

### Steps

1. Navigate to:

   `FIELD PHOTO PREP TEST → Cut Grass - 2026-09-21`

2. Open the photo screen.
3. Confirm the screen label reads:

   `Phase 7B · Reconciliation + local cleanup`

4. Observe the existing H4 photo record with UUID suffix `…fe30232c`.
5. The expected final local state is:
   - queue record remains `UPLOADED · attempt 1`;
   - confirmed remote identity remains displayed/retained;
   - local original is gone;
   - prepared copy is gone;
   - screen reports `Local copies: cleaned up` or the one-time startup message reports that previously confirmed local copies were removed.
6. Return to Google Drive and confirm the exact H4 JPEG still exists in the same work-order folder.
7. Open the JPEG again and confirm it remains usable.

### PASS

PASS when:

- existing queue metadata remains `UPLOADED`;
- confirmed remote identity remains present;
- local image copies are no longer retained;
- the Drive JPEG remains unchanged/present/usable;
- no second deterministic-name JPEG appears.

### FAIL / STOP

STOP if:

- the queue record disappears;
- `UPLOADED` rolls backward;
- remote identity is lost;
- the Drive JPEG is deleted/moved/renamed;
- a duplicate JPEG is created;
- the app reports upload failure merely because local cleanup could not finish.

A local deletion failure by itself is not a remote failure. Record it as cleanup incomplete and stop the cleanup path for investigation.

## Gate 7B-B — one fresh upload with immediate cleanup

### Objective

Prove the normal Phase 7B path:

`capture → prepare → upload → durable UPLOADED → local cleanup`

against the real provider.

### Steps

1. Stay in the safe test hierarchy. Reuse `FIELD PHOTO PREP TEST → Cut Grass - 2026-09-21` unless the operator has a clear reason to use another disposable test work order.
2. Take **one** new test photo.
3. Confirm it appears as one new `WAITING` queue record bound to the selected work-order identity.
4. Prepare the selected photo once.
5. Confirm the prepared JPEG is created and the protected original is still retained before upload.
6. Press `Upload Selected Prepared Photo` **once**.
7. Wait for the queue result. Do not navigate away or force-stop the app to create an artificial interruption.
8. Expected successful result:
   - `UPLOADED · attempt 1`;
   - confirmed provider remote identity displayed;
   - status reports that the local original and prepared copy were removed while upload metadata was kept;
   - selected-photo detail reports `Local copies: cleaned up`.
9. In Google Drive, confirm one new deterministic JPEG exists under the exact intended work-order folder.
10. Confirm its UUID in the filename matches the local photo UUID shown by the app.
11. Open the new Drive JPEG and confirm orientation/content are usable.
12. Confirm the pre-existing H4 JPEG and unrelated sibling content remain present.
13. Fully close the app and reopen it.
14. Return to the same work order and confirm the new record remains `UPLOADED` with confirmed remote identity and no regenerated local image copies.

### PASS

PASS when one and only one new remote JPEG is created for the new photo, the queue reaches durable `UPLOADED`, both app-private image copies are removed after confirmation, restart preserves confirmed metadata, and all Drive content remains intact.

### Unexpected `UNCERTAIN` branch

If the ordinary single upload naturally becomes `UNCERTAIN`:

1. **Do not press Upload again.**
2. Capture the screen showing `UNCERTAIN`, attempt count, and any provisional remote suffix.
3. Inspect Drive without modifying anything and note whether the deterministic JPEG appears zero, one, or multiple times.
4. Select the uncertain queue record.
5. Press `Reconcile Uncertain Upload` **once**.
6. Observe one of the governed outcomes:
   - `UPLOADED` if an exact remote candidate is proven identical;
   - `FAILED` only if the provider settles and proves retry-safe absence with no unresolved provisional identity;
   - `UNCERTAIN` if provider evidence remains ambiguous.
7. Do not perform a retry in this gate after a reconciliation result unless a later reviewed plan explicitly authorizes it.

The reconcile action itself must not create, write, delete, rename, or overwrite a Drive file.

## Gate 7B-C — reconciliation reality evidence

### Normal disposition

A physical `UNCERTAIN` upload is **not required to be manufactured**.

If no natural `UNCERTAIN` result occurs and no already-existing safe uncertain queue fixture is available, record:

`NOT SAFELY INDUCIBLE — automated/fake-provider reconciliation coverage retained; no production fault-injection backdoor added.`

This is an allowed limitation, matching the earlier H4 ambiguity-gate treatment.

### PASS-equivalent acceptance

The physical reconciliation portion is accepted without manufacturing an ambiguous upload when:

- Gate 7B-A passes;
- Gate 7B-B passes;
- the exact runtime's complete automated reconciliation suite is already PASS;
- no safe real uncertain fixture existed;
- no contradictory provider behavior was observed during the normal device flow.

If a natural `UNCERTAIN` case occurs, its real reconciliation result becomes the stronger evidence and must be recorded exactly.

## Evidence to capture

Minimum evidence ledger:

- device: Samsung Galaxy A16;
- date/time of test;
- exact Phase 7B runtime SHA;
- artifact ID and SHA-256;
- app version shown/known as versionCode 15 / versionName `0.10-phase7b-reconciliation-cleanup`;
- screenshot or observation of the Phase 7B photo screen;
- H4 record before/after cleanup where available;
- H4 Drive JPEG still present after cleanup;
- new test photo local UUID;
- new remote deterministic filename;
- new provider remote identity suffix displayed by app;
- new Drive JPEG present/openable;
- post-restart `UPLOADED` metadata and no local-copy resurrection;
- reconciliation disposition: natural result details or `NOT SAFELY INDUCIBLE`.

## Final decision rule

- **PASS:** Gate 7B-A and 7B-B pass; Gate 7B-C either passes on a natural safe fixture or is recorded as `NOT SAFELY INDUCIBLE` with no contradictory evidence.
- **FAIL:** wrong destination, duplicate creation, confirmed metadata loss, remote content deletion/change caused by local cleanup, unsafe retry release, or any reconciliation remote write.
- **STOP / INVESTIGATE:** provider/app evidence conflicts or an unexpected ambiguous state appears that the plan does not safely classify.

## After the phone gate

If PASS:

1. create `docs/PHASE_7B_DEVICE_REALITY_GATE_RECORD_2026-09-10.md` with the observed evidence;
2. update the Phase 7B PR with exact device-gate status;
3. perform final Level 3 pre-merge review;
4. request/receive explicit merge authorization;
5. only then merge;
6. proceed to Phase 8 field workflow/release hardening.

If FAIL, do not merge and do not stack Phase 8 work. Fix only the demonstrated defect on the existing Phase 7B branch, rerun proportionate automated verification, and restage only the affected physical check.