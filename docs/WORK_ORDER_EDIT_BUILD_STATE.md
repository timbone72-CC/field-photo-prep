# Work order editing and discovery repair

- Goal: expose non-destructive name/date editing and explicit discovery status on Work Orders.
- Level: 3; user request authorizes implementation, not merge.
- Authoritative line: PR #95 / fix/address-workorder-fresh-refresh, expanded from deferred read-only investigation because the reported empty list recurs on the same screen. No competing line.
- Base/rollback: governed main c9e6965dc934d697b18e7b62e5f4ebbc0dfb9656; prior branch d50b52396730f6d19547a8f52c129d7a4a5a4699.
- Rules: AGENTS, GOVERNANCE, PROJECT_PROFILE, RULE_INDEX, CHANGE_CONTROL_CONTRACT, CONTRACT, INTEGRATION_CONTRACT, TESTING_CONTRACT, rules/testing/DRIVE_PROVIDER, Regression A/D/E/H.
- Root cause confirmed: no existing-work-order edit control or handler; contracts only permitted rename during destructive reuse. No regression checklist asserted non-destructive editing. New-order inputs cannot edit existing folders.
- Empty-list cause unproven: screenshot has no selected WO or visible rows. Existing provider-staleness investigation remains relevant; fresh settled reads cannot independently guarantee Google cloud freshness.
- Approved behavior: explicit Edit Work Order dialog for selected folder, prefilled name/date, save/cancel, exact original ID and parent, collision and stale-name rejection; keep photos and sequence. No deletion, move, queue rewrite, schema, permissions or backend change.
- Required data: selected address/WO IDs, original name, requested name/date, usable persisted write grant, fresh direct sibling listing. Undated legacy folders are editable using original name and explicit date.
- Offline/failure: no write on unverifiable state; uncertain rename locks further writes and clears capture selection until refresh.
- External boundary: Android SAF rename on operator-selected folder only; no assistant changes to customer Drive data.
- Verification: focused naming/collision tests, emulator edit controls, complete CI final head, physical disposable WO rename with photos preserved, restart, collision/cancel and failed listing. Real Drive gate pending.
- Recovery: refresh and inspect actual exact folder after uncertain response; never blindly rename again. Revert scoped commit without touching protected originals or queued destinations.
- Merge approval: not granted. Implementation/test results will be appended here.

- Additional protected boundary: read-only PendingPhotoStore guard blocks editing an unfinished Clear & Reuse reset. No ledger writes/schema changes. Loaded rules/testing/UPLOAD_QUEUE_RETRY.md for this guard; targeted tests assert waiting-original/destination/sequence preservation and pending-reset refusal.

- Async authorization callbacks cannot unlock the UI during a work-order rename; instrumented coverage asserts the edit-operation latch.

- First CI d8eeeca: unit tests/app build/signer checks passed; instrumentation compilation failed because Espresso is not a repository dependency. Corrected the test to existing Android accessibility automation; final complete suite pending. No failed candidate delivered.

## Verified candidate handoff — October 6

- Runtime branch head: afd29c654e448d6ad853fb0a51d8b6b6c8a692ed. Final full Android CI 37508637427 SUCCESS; governance 37508632536 SUCCESS. Unit policy tests 4/4 and storage continuity tests 2/2 passed. Full instrumentation and launch smoke passed, including edit dialog prefill/cancel and async-lock test.
- Actions PR merge source built: d2c2af4ccabecc3d960503488e3de05fbcdc9350. Candidate 0.28.6-internal, code 43, existing internal package and signer verified. APK SHA256: 453681172bda80223631a39e19f8f186ae7fcda43f8c5b3dbfdb33175c1ed76b.
- Candidate delivered as Field-Photo-Prep-0.28.6-internal-WO-Edit.apk. No production release, customer Drive mutation or merge performed.
- Remaining gate: install over existing Internal package without clearing data; verify on Samsung using disposable WO that name/date correction preserves photos/provider identity/sequence and survives restart, collision/cancel stops writes; examine reported blank existing-WO list under the correct company/address. Google-provider cache causality remains unproven.
- Explicit merge approval remains pending. This is a verified automated candidate, not completed physical Drive proof.

## Clear & Reuse field regression — October 6

User reports date picker followed by changed displayed WO date without deletion confirmation. Physical screenshot: 560 NW REBECCA TER LAWTON / GRASS CUT - 2026-10-06 still lists 33 Uploaded entries dated September 29. Support: 0.28.6-internal, VALIDATED/MEMBER, Drive USABLE, unresolved/protected/corrupt queue counts zero. This proves stale old history is visible; it does not independently prove remote Drive photos still exist or which control caused the date change.

Confirmed design/code defects: Clear & Reuse rejects same-date reset, so a date corrected to today cannot be cleared for today. Its existing-target branch silently changes selected WO instead of reaching deletion confirmation. Earlier checks did not cover correction followed by same-date clearing. Status can be overwritten by unrelated create validation; shown screenshot's name prompt is not deletion evidence.

Expanded authorized scope under original whole-problem repair: explicit same-date or later-date clear confirmation; exact selected folder only; reject other-folder collisions without changing selection; preserve folders/non-images and all unresolved originals; after verified remote zero-photo state and exact ID/name verification, retire only that WO's confirmed history and reset next sequence to 001. Other WOs/addresses remain untouched. No automatic Drive deletion by assistant.

Level 3, same authoritative PR #95. Required packs include Drive, queue/storage and existing universal rules. Rollback: afd29c654e448d6ad853fb0a51d8b6b6c8a692ed (0.28.6 runtime). No schema, grants, backend or signing change. Physical gate pending on disposable WO; merge approval pending. Partial remote/local failure must retain pending reuse state and report incomplete, never claim success. Tests cover same-date target, collision, history retirement, protected refusal, unrelated WO preservation, restart and sequence. Latest candidate will replace 0.28.6 only after final full CI.

Recovery boundary: new UI reuse stores an optional per-WO explicit-completion marker in the existing capture ledger before deletion. Existing ledgers remain readable without migration. New incomplete clears cannot auto-complete on camera open merely because the requested date equals the visible date; only verified zero-remote-photo and local-history completion removes the marker. Legacy pending transitions retain their existing recovery semantics. Do not downgrade while a new clear remains incomplete. Async auth callbacks cannot unlock UI during confirmed cleanup.

### 0.28.7 delivery evidence (2026-10-06)

Runtime head ac9a5359111f8074d6f16266f421c7dcdc6f1311 passed full Android CI run 37513661955, including unit tests, instrumented tests, launch smoke, identity and signer checks. Built PR merge source: 65b792a2a64268888ce2ca26cd008d888c434a63. APK: Field-Photo-Prep-0.28.7-internal-Clear-Reuse.apk, version code 44, package com.inandout.fieldphotoprep.internal. SHA256 f63ddf66936aec9a2dc9bdd8862588c12d755d5acfe8e0dd41b56ba007d1d277; stable signer SHA256 2c0a9616fd819333ed98b33593fbe597e120e95936103c6b7a76270e985d3fba. Saved downloadable APK libfile_cde028bb10a08191898b4f53b492076d.

No live Drive photos modified by assistant. Physical reality gate remains pending: explicit same-date deletion confirmation, selected WO Photos 0 after completion, nonphoto children preserved, next capture 001, restart recovery. PR remains unmerged; explicit merge approval still required after physical evidence. Install over internal app without uninstall or clearing app data; do not downgrade during incomplete Clear & Reuse.

## Process correction — 2026-10-06 operator review

Authorized documentation-only amendment on the same PR #95; Level 1 amendment within the still-Level-3 runtime scope. Owners: GOVERNANCE.md (planning/claims/closeout), CHANGE_CONTROL_CONTRACT.md (coverage record), TESTING_CONTRACT.md (connected sequences), docs/ROADMAP.md (current status). No APK, runtime, Drive, schema or permission change. Rollback: prior branch head 285d3fbb553f9cf417570b61c35156045d232d51. Verification: documentation diff and consistency review; runtime evidence remains valid. Merge authorization unchanged.

At governed pre-fix revision c9e6965: CONTRACT.md §2.21 and §4.14 limited WO rename to reuse; ROADMAP Phase 2 did not include non-destructive editing. This was a planning omission, not a proven ignored edit requirement. CONTRACT §2.26–31 already required confirmation and clearing/renaming the same selected folder; §7.15 and Regression E already required excluding prior history. Claims that those checks were missing were incorrect. §2.24/31 explicitly required a later date; same-date refusal was a design restriction. Field screenshot proves 33 old Uploaded entries visible, not independently whether Drive deletion occurred. The exact original planning decision/person and specific route used on-device are not established by this audit.

| Requirement and source | Owning implementation | Verification and evidence | Status/remaining gap |
| --- | --- | --- | --- |
| Non-destructive WO correction; approved October 6, current CONTRACT correction section | MainActivity edit flow / WorkOrderEditPolicy | 0.28.6 full CI 37508637427; edit/collision/storage tests | Automated passed; exact physical preservation/restart checks pending |
| Exact selected WO confirmation and photo-only deletion; pre-fix §2.26–31 | MainActivity confirmed reuse / Drive child snapshot | 0.28.7 CI 37513661955; confirmation/cancel instrumentation | Automated passed; exact real Drive deletion/nonphoto proof pending |
| Prior history absent and next capture001; pre-fix §7.11–15 / Regression E | PendingPhotoStore verified completion | ClearReuseHistoryTest in CI 37513661955 | Automated passed; physical Photos0/001 pending |
| Same-date reuse; October 6 amendment | WorkOrderFolderName / MainActivity / explicit ledger completion | Same-date policy/history/restart tests in CI 37513661955 | Automated passed; connected physical sequence pending |
| Preserve other WO/protected work; CONTRACT identity/queue guards | WorkOrderEditPolicy / PendingPhotoStore | Collision, unrelated-history and unresolved-record refusal tests | Automated passed; provider-specific observations only as required |

Operator says candidate works “so far”; retain that limited feedback without inventing the exact actions tested. Process amendment establishes mandatory coverage and evidence distinctions; it does not declare the runtime scope complete or prove future compliance automatically.

## October 8, 2026 — final-head automated gate + operator field observation + connected-sequence regression

- **Operator field report:** "the photos disappear when i use clear and reuse". This is **accepted observed behavior** and should **not** be retested just to get the same observation. This report does not itself establish whether the absent photos were verified separately in the Google Drive folder, whether the phone was on 0.28.7, or whether all other physical gates were exercised; no such extrapolation is authorized.
- Full reconciled PR #95 head `f04f5d894f398ec83ad7b677a3ba1f51927f5841` passed [Android CI 37874275589](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37874275589) and [Governance 37874272399](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37874272399). The Android CI included unit tests, debug build, production-signing fail-closed, APK identity/signer verification, emulator instrumented/UI/launch checks, and artifacts. All were PASS; these results are **automated/emulator**, not Samsung/real Google Drive proof.
- **Additional test-only coverage:** `WorkOrderCorrectionStorageTest.editThenSameDateClearRetiresOnlyThatOccurrenceAcrossRestart` now exercises the connected correction→same-date reuse sequence, including exact selected WO provider identity, complete-upload record preservation before reuse, unrelated WAITING photo bytes/destination, prepared same-date reset/restart interlock, selected-history retirement, new selected sequence 001, and unrelated WO sequence continuation. The simulated folder correction is **not** proof of Drive rename/delete. This is tests/documentation only; no app behavior changed.
- **New exact-head CI:** pending at the moment of this note; record result before claiming it passed. Reuse prior physical evidence, and ask only for distinct not-yet-proven exact real-provider observations if needed.
- PR remains **DRAFT / unmerged** until required exact-head CI/physical evidence and explicit Level-3 approval. No customer Drive data were touched.

### Final test-only regression result — 2026-10-08

- Final runtime/test head **`3b704ec2b02ef60ae713a1498d8aefa8efa738b9`** passed [Android CI 37875531355](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37875531355): unit tests and production identity verification, Android internal build, fail-closed production-signing check, stable APK signer, instrumented/emulator and launch-smoke tests, internal APK and test-evidence artifact uploads all **SUCCESS**. [Governance 37875529270](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37875529270) **SUCCESS**.
- New regression tests: (1) `WorkOrderCorrectionStorageTest.editThenSameDateClearRetiresOnlyThatOccurrenceAcrossRestart` exercises name/date correction followed by explicit same-date Clear & Reuse after a restart and verifies selected history/sequence reset and unrelated protected-photo preservation; (2) `Concept3UiStructureInstrumentedTest` checks Edit Work Order remains alongside existing Work Orders controls, their expected labels, sizing and navigation. Both were included in the successful final run. Neither simulates successful Google Drive provider deletion or proves a real Samsung screen cannot overlap in every configuration.
- Operator expressly confirms: **photos disappear after using Clear & Reuse**. This direct field observation is retained as **PASS for the reported disappearance behavior**; it must not be requested again without a distinct unproven question. The report did not separately specify the app vs Google Drive view, build version or the rest of the full provider/phone test sequence, so those are not silently marked PASS.
- **Only test code and this record** changed since the previously reconciled runtime head `f04f5d894f398ec83ad7b677a3ba1f51927f5841`. No production Android/Drive/auth source, queue/Drive content, deployed backend or release behavior changed. This subsequent documentation-only record does not change the tested runtime tree.
- The PR remains draft and **unmerged**. Before Level-3 merge approval, reconcile already-observed user evidence for the distinct real-provider safeguards (edit retains source photos/identity through restart, zero stale local history/001 after confirmed reuse, no unrelated folder/file damage, required confirmation) rather than asking to repeat the disappearing-photos observation. Any genuinely unproven provider-specific gate must be identified explicitly. Explicit PR-specific pre-merge approval is still required.
