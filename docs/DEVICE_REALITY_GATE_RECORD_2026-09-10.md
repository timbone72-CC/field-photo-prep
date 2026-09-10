# Device Reality Gate Record — 2026-09-10

Status: **COMPLETED — Gates A, B, and C PASS**

This record consolidates the physical Android + real Google Drive evidence produced by `docs/MASTER_DEVICE_REALITY_GATE_PLAN_2026-09-10.md`.

No runtime code was changed during the phone session. No Level 3 merge is authorized by this record.

## Device / provider context

- Device: Samsung Galaxy A16
- Android version: not explicitly captured during the session; do not infer it in place of evidence
- Provider: Android system Storage Access Framework with the Google Drive document provider
- Approved master used: `HNP Jobs`
- Primary safe test address: `FIELD PHOTO PREP TEST`
- Test date: 2026-09-10

## Exact staged builds

### Gate A — Phase 3B

- Branch: `feat/phase-3b-clear-reuse`
- Exact tested runtime: `0a7071744791b470f1c0bd795d78934c8f28c87e`
- CI run: `34302110486`
- Artifact: `10085354689`
- SHA-256: `fb62638024d479a17c82d0caa43e3f0ca0410769ae9a4a6ea9fa8e41e41f1032`

### Gate B — Phase 4

- Branch: `feat/phase-4-address-folder-creation`
- Exact tested runtime: `a1ca4f5e1ecd871f24d0724b6c1239bfd9666570`
- CI run: `34425740379`
- Artifact: `10132648386`
- SHA-256: `bccbcd28c822c731d7a51de63d170915c6ddd14ad7ef7ebb5ce95982b56a5b5a`

### Gate C — Integrated Phase 5 + 6A + 7A + 6B-H1/H2

- Branch: `feat/phase-6b-h2-create-persist-write-barrier`
- Exact tested runtime/test head: `8d23b061307725589aef69a31a00249744523406`
- CI run: `34467462173`
- Artifact: `10148261046`
- SHA-256: `0011223e86c2dff7da92f98e030de2160c2c25d76e0dee605006e856c3dd2569`

## Gate A — Phase 3B Clear & Reuse — PASS

Safe hierarchy:

`HNP Jobs → FIELD PHOTO PREP TEST → Cut Grass - 2026-09-20`

Observed:

1. The Google Drive provider initially reported the work-order folder as still loading.
2. The app failed closed: destructive reuse remained unavailable while the provider state was non-authoritative.
3. One deliberate `REFRESH WORK ORDERS` cleared the loading condition; no refresh loop was used.
4. The selected old folder contained exactly two direct disposable items, one of which was a child folder.
5. The confirmation dialog showed the full master/address/old-folder/new-folder hierarchy and the exact direct-child count.
6. `Cancel` was exercised once. Drive inspection confirmed the old folder and both direct items remained unchanged.
7. The same fixture was re-entered and `Clear & Reuse` was confirmed once.
8. The app reported completion with the same folder identity and the new name `Cut Grass - 2026-09-21`.
9. Drive inspection confirmed the resulting folder was empty, the old dated name was gone, the new dated name existed exactly once, and unrelated sibling test content remained present.
10. A deterministic provider delete/rename failure was not safely inducible and was not manufactured.

Important provider evidence:

- Real Google Drive SAF state can be temporarily non-authoritative immediately after navigation/listing.
- One normal provider refresh was sufficient in this session.
- The hardened fail-closed behavior prevented destructive action until the provider settled.

Usability finding:

- The work-order entry field can be misunderstood as either a search field or a full dated-folder-name field. A future UI pass should make the accepted input explicit and preferably prefill the work-order type from a selected historical folder.

## Gate B — Phase 4 Address Folder Creation — PASS

Observed:

1. The real provider listed 16 existing address folders under `HNP Jobs` before the new-address test.
2. Entering existing `FIELD PHOTO PREP TEST` through `Use / Create Address` reused the existing address and opened its real work-order contents; no duplicate address was created.
3. New address `FIELD PHOTO PREP ADDRESS CREATE TEST` was created exactly once directly under `HNP Jobs`.
4. Drive metadata independently confirmed the new folder existed with provider/backend Drive identity `1586X-5CddHqgt0e9tCJm33o4Wx2iH-Uk`.
5. Repeating the same request reused the existing folder rather than creating a duplicate.
6. After fully closing and reopening the app, repeating the same request again reused the existing address; restart did not create another folder.
7. Work-order discovery opened beneath the exact selected address.
8. A duplicate-name fixture was not intentionally manufactured because it was optional and would add unnecessary clutter.

Operator mistake safely observed:

- One attempt submitted an empty address field. The app stopped at local validation with `Address folder name is required.` and no Drive write occurred.

## Gate C1 — Phase 5 Capture / Protection / Destination Binding — PASS

Primary destination:

`HNP Jobs → FIELD PHOTO PREP TEST → Cut Grass - 2026-09-21`

Observed:

1. The integrated H2 build retained the approved master after install over the Phase 4 build and listed the real address folders.
2. The exact address/work-order hierarchy was selected before capture.
3. A real Samsung camera still image was captured and returned to the app.
4. The app reported `Photo protected locally and waiting for preparation/upload.`
5. One local photo record appeared in durable `WAITING` state.
6. Full app close/reopen preserved the same photo record and original destination identity suffix `…Vmh4GuLy`.
7. A separate work order `Photo Binding Test - 2026-09-22` was selected; its photo screen showed zero local photo records.
8. Returning to the original `Cut Grass - 2026-09-21` work order recovered the original waiting photo with its original destination identity.
9. This provided physical-device evidence that current UI selection changes do not retarget an already captured photo.

## Gate C2 — Phase 6A Real Photo Preparation — PASS

Using the same real camera photo:

1. A separate prepared JPEG was created.
2. The app reported the prepared copy as approximately 365 KB at `1536×2048`.
3. The UI explicitly reported `Protected original kept.`
4. The prepared copy remained separately selectable while the original queue record remained present.
5. After upload, the operator opened the actual Drive JPEG and confirmed the image was correctly oriented and usable for field documentation.
6. No additional camera capture was performed solely to repeat evidence already obtained.

## Gate C3 — Phase 6B-H4 Exact Drive Upload — PASS

Using the same prepared photo:

1. Upload was started exactly once.
2. The queue transitioned to `UPLOADED · attempt 1`.
3. The app displayed `Upload result: confirmed` and exposed a returned remote provider identity suffix `…yfQ0MN4P`.
4. The prepared copy remained approximately 365 KB.
5. The app explicitly retained the local original and prepared copy because Phase 7 cleanup is not implemented yet.
6. Google Drive backend metadata independently exposed the newly created JPEG:
   - name: `field-photo-88ba77d6-2af9-4a3d-ad07-0bdefe30232c.jpg`
   - Drive backend file ID: `1wEEcTKq6hdOjzfR-1079F7Kh_D0opIsB`
   - MIME type: `image/jpeg`
   - size: `373617` bytes
   - created at: `2026-09-10T17:19:07.637Z`
7. The filename UUID suffix matched the local photo identity suffix `…fe30232c` shown in the app.
8. The operator opened the actual Drive file from the intended test work-order folder and confirmed it was present and visually usable.

Important identity observation:

- The app's provider document identity and Google Drive backend file ID are distinct representations. Continue treating the Android provider DocumentId as the governed app identity; do not substitute backend Drive file IDs into persisted provider identity fields.

## Gate C4 — Interrupted / Ambiguous Upload — NOT SAFELY INDUCIBLE

No controlled method was available that could reliably create an ambiguous real-provider outcome without risking an uncontrolled duplicate or turning the session into repeated failure experiments.

Per the master plan:

- no interruption was manufactured;
- no blind second upload was attempted;
- no uncertain remote object was deleted;
- this evidence gap is carried into Phase 7B design rather than guessed away.

## Session usability findings

The phone session exposed two related UI problems without invalidating the safety behavior:

1. The work-order field can look like a search box even though it is an input used by `Use / Create Work Order`.
2. The field permits operators to type a full dated folder-style value even though the date is controlled separately.

Preferred future direction, subject to governed design:

- label the field explicitly as work-order type/name only;
- show helper text such as `Work order name only — date is added automatically`;
- when a historical work-order folder is selected for reuse, prefill the work-order type automatically where safe;
- do not silently normalize or guess materially different work-order names.

The session also created harmless disposable test clutter while recovering from operator navigation mistakes:

- `Photo Binding Test - 2026-09-22` under `FIELD PHOTO PREP TEST`;
- an accidental `Cut Grass - 2026-09-21` under `FIELD PHOTO PREP ADDRESS CREATE TEST`;
- an earlier lower/reordered `grass cut - 2026-09-21` test folder was observed.

Do not automatically delete these as part of upload/reconciliation work. Any cleanup is a separate explicit test-data cleanup action.

## Consolidated result

- Gate A / Phase 3B: **PASS**
- Gate B / Phase 4: **PASS**
- Gate C1 / Phase 5: **PASS**
- Gate C2 / Phase 6A: **PASS**
- Gate C3 / Phase 6B-H4: **PASS**
- Gate C4 ambiguity experiment: **NOT SAFELY INDUCIBLE — allowed limitation**

The physical Android/Google Drive blockers targeted by the master device reality-gate plan are satisfied for the exact tested builds above.

## Phase 7B handoff

Phase 7B may now be designed from real evidence rather than assumptions.

Carry forward these facts:

1. Google Drive SAF can temporarily report a folder as loading/non-authoritative; bounded refresh/fail-closed behavior is necessary.
2. A successful real upload can return a provider identity, verify through the provider path, reach durable `UPLOADED`, and subsequently become visible in the Drive backend.
3. Provider document identity must remain distinct from backend Drive file ID.
4. The deterministic filename `field-photo-<local-photo-uuid>.jpg` was preserved end to end and is useful reconciliation evidence.
5. A confirmed uploaded photo currently retains both protected original and prepared copy, providing the safe starting point for Phase 7B cleanup design.
6. No real ambiguous/interrupted provider outcome was safely induced, so Phase 7B must preserve conservative `UNCERTAIN` handling and use deterministic identity/name/parent reconciliation rather than assuming provider behavior not observed here.
7. Phase 7B should be built as far as can be honestly proven with automated/emulated/fake-provider evidence, then staged again only at the next genuine real-device/provider boundary per `docs/PHASE_STAGING_DOCTRINE.md`.

## Merge / next-step boundary

This record closes the staged device evidence collection. It does **not** grant Level 3 merge approval for any open PR.

Next governed work:

1. absorb this evidence into Phase 7B design;
2. revisit only the lean-audit candidates that Phase 7B design makes relevant;
3. define the next large safe Phase 7B implementation slice;
4. build/test straight through to the next genuine phone-dependent boundary;
5. stage that next reality gate when reached.
