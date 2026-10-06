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
