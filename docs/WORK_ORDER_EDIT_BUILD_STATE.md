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
