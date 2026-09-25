# Rule-Pack Deduplication — 2026-09-25

## Governance Classification

- Scope key: docs-deduplicate-rule-packs
- Goal: Reduce buried/repeated testing and staging rules without changing their meaning.
- Affected surfaces: Markdown governance/testing documentation only.
- Change level: Level 1
- Authoritative branch: docs/deduplicate-rule-packs
- Required rule packs: GOVERNANCE.md; PROJECT_PROFILE.md; RULE_INDEX.md; CHANGE_CONTROL_CONTRACT.md
- Protected behavior: All runtime safety, testing, device/provider, photo, Drive, queue, identity, and approval requirements retain their existing meaning.
- External systems changed: None.
- Rollback point: 4c184fe0bb0f5d956fc0a0151d65d4ec70f399bf
- Verification boundary: Documentation diff/routing review; Governance Check; Markdown-only Android CI lightweight path.
- Level 3 merge approval: N/A

## Changes

- Move detailed camera/preparation testing rules to `rules/testing/CAMERA_PREPARATION.md`.
- Move detailed queue/batch/retry testing rules to `rules/testing/UPLOAD_QUEUE_RETRY.md`.
- Move detailed Drive/provider/workspace testing rules to `rules/testing/DRIVE_PROVIDER.md`.
- Reduce `TESTING_CONTRACT.md` to universal testing policy plus routing.
- Replace the active Phase 6 → Phase 7 staging text with a permanent staging doctrine.
- Preserve the original staging doctrine under `docs/history/`.

## Non-goals

This pass does not:
- rewrite `CONTRACT.md`;
- rewrite `INTEGRATION_CONTRACT.md`;
- change Android/Supabase/Drive behavior;
- alter Phase 12F implementation;
- remove a testing requirement merely to shorten a file.

## Verification rule

Every moved requirement must remain represented either in the universal testing contract or in exactly one routed feature pack.

The history archive exists for provenance, not as a current rule source.
