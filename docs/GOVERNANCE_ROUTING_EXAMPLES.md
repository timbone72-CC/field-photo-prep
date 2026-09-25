# Governance Routing Examples

This file contains examples only. It is not part of the mandatory first-read path.

Use `RULE_INDEX.md` as the authoritative router.

## Presentation-only label change

Surfaces:
- Android presentation only.

Load:
- governance/profile;
- Level 1 change rules when the wording cannot affect interaction/runtime behavior;
- the affected product/UI source.

Do not load camera/Drive internals unless the change touches them.

## Upload retry change

Surfaces:
- queue;
- remote write;
- destination/idempotency;
- Drive/DocumentsProvider.

Load:
- queue/retry portions of `CONTRACT.md`;
- upload/retry portions of `INTEGRATION_CONTRACT.md`;
- `TESTING_CONTRACT.md`;
- `rules/testing/UPLOAD_QUEUE_RETRY.md`;
- `rules/testing/DRIVE_PROVIDER.md` when provider behavior is involved;
- relevant regression checklist sections;
- Level 3 change rules when remote retry/destination/destructive semantics change.

## Continue unfinished identity work

Before creating anything:
- inspect open PRs and active build-state records;
- identify the one authoritative implementation line;
- inspect relevant live Supabase state;
- continue that line unless an explicit supersession is required.

Then load the identity model, current approved identity design, testing rules, and backend verification record for the affected surface.
