# Phase 7B Device Reality Gate Record — 2026-09-10

Status: **PASS — physical Android / Google Drive reality gate complete**

## Purpose

This record closes the staged Phase 7B physical-device gate for remote reconciliation and confirmed local cleanup. It records only evidence actually observed on the Samsung Galaxy A16 and independently visible Google Drive results. No ambiguous upload was manufactured.

## Governed runtime

Repository: `timbone72-CC/field-photo-prep`

Phase 7B branch: `feat/phase-7b-remote-reconciliation-cleanup`

Exact Phase 7B runtime/test head:

`f25868dd768dcdddb11ac4d6ab3879a7cde85d2c`

Version:

- `versionCode 15`
- `versionName 0.10-phase7b-reconciliation-cleanup`

Original Phase 7B runtime CI evidence:

- run `34512362110`
- job `102989361359`
- PASS: JVM/unit suite, debug build, Android instrumentation, install/launch smoke, APK packaging
- artifact `10166488011`
- artifact digest `sha256:544a834c701e16f650cd2738d2ccd76be4a9aa031900974f0027f65a265689c5`

## Signing blocker and governed recovery

The first physical Phase 7B update attempt was blocked before installation because the prior H2 GitHub Actions debug APK and Phase 7B debug APK were signed by different ephemeral debug keys. Android correctly rejected the update with:

`App not installed as package conflicts with an existing package.`

No Phase 7B runtime behavior ran during that failed install attempt and no app or Drive data was modified by it.

A separate Level-3 stable test-signing change was explicitly authorized and implemented on `feat/stable-test-signing`. It changes only debug/test package signing and CI verification; no Phase 7B Java/runtime, queue, Drive, photo, upload, reconciliation, or cleanup behavior changed.

Stable test signer certificate SHA-256:

`2C:0A:96:16:FD:81:93:33:ED:98:B3:35:93:FB:E5:97:E1:20:E9:59:36:10:3C:6B:7A:76:27:0E:98:5D:3F:BA`

Stable-signed Phase 7B build used for the completed device gate:

- signing/build head `1d8d3acb95f84be4f5fefa0c1c0357265f58730a`
- CI run `34516597291`
- job `103003428037`
- PASS: unit tests, debug build, stable signer verification, Android instrumentation/install/launch smoke, artifact publication
- artifact `10168097825`
- artifact ZIP digest `sha256:b0605953a02aac919e31800bbdb41fdab00969402ee4ab5add36c2a112b0a6d7`
- APK SHA-256 `445a3c88da685f8af299ddea3d593f9c384369691e79fe243f9fc9202cb1d1a8`

Stable-signed H2 compatibility fixture:

- branch `feat/stable-test-signing-h2-fixture`
- exact head `5fcc06b937f9008c1dfa3265ea8d894b8b1f162b`
- CI run `34516638178`
- job `103003565364`
- PASS with the same stable signer and full Android suite
- artifact `10168100773`
- artifact ZIP digest `sha256:07d23b3c09bb269075d0955cba67031d288a8741747d61a220a7a3b18bc36e6f`
- APK SHA-256 `d895177b15fa078133fbccd02844b0f50d389314565f33aac9d555b0da25c85f`
- `versionCode 14`
- `versionName 0.9-phase6b-drive-upload`

Because Android cannot change package signers in place, the old ephemeral-signed test app was uninstalled only after its previously uploaded Drive JPEG was independently re-confirmed. A disposable retained-local-data fixture was then recreated once on the stable-signed H2 build and used for the real stable H2 → stable Phase 7B update test.

## Device / provider context

- test date: 2026-09-10
- physical device: Samsung Galaxy A16
- provider path: Android Storage Access Framework / Google Drive `DocumentsProvider`
- approved master: `HNP Jobs`
- safe address: `FIELD PHOTO PREP TEST`
- work order: `Cut Grass - 2026-09-21`
- app destination provider identity suffix observed: `…Vmh4GuLy`

No live customer/job folder was used for this gate.

## Stable H2 retained-local fixture

A new disposable photo was captured, prepared, and uploaded once on the stable-signed H2 fixture.

Observed H2 result:

- local photo UUID `973f23ec-2f16-418c-b8a4-c703951e3346`
- displayed suffix `…951E3346`
- state `UPLOADED · attempt 1`
- prepared copy approximately `340 KB`
- upload result confirmed with a provider remote identity
- H2 explicitly retained the protected original and prepared copy pending the cleanup phase

Independent Drive evidence:

- deterministic filename `field-photo-973f23ec-2f16-418c-b8a4-c703951e3346.jpg`
- Drive backend file ID `10E0vOo0Yix4Af6m5ubb9_hujm7SexS7S`
- exactly one matching deterministic-name JPEG observed

This established the retained-local-image upgrade fixture needed by Gate 7B-A.

## Stable H2 → Phase 7B update continuity

The stable-signed Phase 7B APK was installed directly over the stable-signed H2 app without uninstalling or clearing data.

Observed result:

- Android accepted the APK as an update;
- persisted `HNP Jobs` access remained available;
- the app immediately listed the same 17 address folders;
- no queue reconstruction or manual destination rebinding was required.

This closes the signing/update continuity blocker that interrupted the first attempt.

## Gate 7B-A — upgrade cleanup of previously confirmed upload

**PASS**

After the H2 → Phase 7B update, the operator navigated to:

`HNP Jobs → FIELD PHOTO PREP TEST → Cut Grass - 2026-09-21 → Photos`

Observed Phase 7B screen:

- label `Phase 7B · Reconciliation + local cleanup`;
- startup message: `Removed local image copies for 1 previously confirmed upload(s). Drive copies and metadata were kept.`;
- H2 fixture record `…951E3346` remained `UPLOADED · ATTEMPT 1`;
- destination identity remained `…Vmh4GuLy`;
- confirmed metadata remained present;
- retained app-private original/prepared image copies were removed.

Independent Drive verification confirmed the H2 fixture JPEG remained present exactly once after local cleanup. No remote delete, move, rename, overwrite, or duplicate was observed.

## Gate 7B-B — fresh upload with immediate cleanup

**PASS**

One new disposable photo was captured on Phase 7B, prepared once, and uploaded once to the same immutable test work-order destination.

Observed result:

- local photo UUID `245bc9a8-6a15-48e1-8c46-ab36ef8a95f7`
- displayed suffix `…EF8A95F7`
- state `UPLOADED · ATTEMPT 1`
- confirmed provider remote identity remained displayed after success
- selected record reported `Local copies: cleaned up`
- no second upload attempt was made

Independent Drive verification found exactly one new deterministic JPEG:

`field-photo-245bc9a8-6a15-48e1-8c46-ab36ef8a95f7.jpg`

Drive backend file ID:

`1f3rABu_QE6g6jDo_YgmKtBHqS_z05A-O`

The pre-existing H2 fixture JPEG and the earlier H4 JPEG both remained present. No unrelated Drive test content was removed by Phase 7B cleanup.

The operator opened the uploaded photos in Google Drive and reported that the pictures looked great / were visually usable and correctly presented for field documentation.

## Restart persistence

**PASS**

The app was fully closed from Recents and reopened. The operator returned to the same work order.

Observed after restart:

- H2-upgraded fixture `…951E3346` remained `UPLOADED · ATTEMPT 1`;
- fresh Phase 7B photo `…EF8A95F7` remained `UPLOADED · ATTEMPT 1`;
- selecting `…EF8A95F7` still showed `Local copies: cleaned up`;
- its confirmed remote identity remained present;
- local image copies did not regenerate after restart.

This proves confirmed upload bookkeeping survives restart independently of removed local image bytes.

## Gate 7B-C — physical UNCERTAIN reconciliation

Disposition:

`NOT SAFELY INDUCIBLE — automated/fake-provider reconciliation coverage retained; no production fault-injection backdoor added.`

No natural `UNCERTAIN` upload occurred during the normal single-upload device path, and no pre-existing safe uncertain fixture was available. Per the staged gate plan, the operator did not intentionally interrupt upload, create duplicate remote files, alter provider identities, or otherwise manufacture remote ambiguity.

The exact Phase 7B runtime's automated reconciliation suite remains the evidence for:

- exact provisional-identity lookup first;
- deterministic-name fallback under the immutable original destination;
- settled-provider requirements before absence/cardinality decisions;
- content proof before confirmed-match promotion;
- retry release only after retry-safe absence is proven with no unresolved provisional identity;
- remaining `UNCERTAIN` when evidence is loading, inconsistent, ambiguous, inaccessible, duplicated, or otherwise inconclusive;
- reconciliation performing no remote create/write/delete/rename/overwrite.

No physical provider behavior observed during Gate 7B-A/B contradicted those assumptions.

## Overall Phase 7B device result

**PASS**

The physical gate demonstrated:

1. stable package update continuity after the separately governed test-signing fix;
2. persisted SAF/Drive access survives the H2 → Phase 7B update;
3. a previously confirmed `UPLOADED` record preserves metadata while Phase 7B removes its retained local image copies;
4. local cleanup leaves the Drive JPEG intact;
5. a fresh Phase 7B capture/preparation/upload reaches durable `UPLOADED` on attempt 1;
6. fresh confirmed upload immediately removes both app-private image copies;
7. deterministic Drive filename and exact destination behavior remain intact;
8. restart preserves confirmed metadata without recreating local image data;
9. resulting Drive photos remain visually usable;
10. no duplicate remote JPEG, wrong-destination upload, metadata rollback, or unrelated remote deletion was observed.

Gate 7B-C was accepted as `NOT SAFELY INDUCIBLE` under the written plan rather than manufacturing an unsafe ambiguous remote state.

## Non-blocking usability findings retained for Phase 8

The device session also confirmed several UI/polish items that are not Phase 7B safety failures:

- the work-order entry field can be mistaken for a search box; future field hardening should make `work-order name only` clearer and keep date entry visibly separate;
- the address-list activity still displays an inherited `Phase 5 · Camera + temporary photo protection` label even when the installed photo workflow is Phase 7B;
- the external Samsung camera may show its own `OK / Retake` confirmation screen; removing that extra step would require a deliberate camera-ownership change rather than a Phase 7B fix.

Do not reopen Phase 7B runtime scope solely for these items; evaluate them in Phase 8 field-workflow/release hardening.

## Merge status

This PASS does **not** itself authorize merge.

Phase 7B and the stable test-signing change remain Level-3 governed work. Perform final pre-merge review and obtain explicit operator merge authorization before merging any PR.
