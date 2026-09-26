# Governance Core Compaction — 2026-09-25

Level 1 documentation-only refactor.

Purpose: reduce the always-read governance layer without changing any safety rule, project invariant, risk level, approval gate, or runtime behavior.

Changed:
- `AGENTS.md`
- `GOVERNANCE.md`
- `PROJECT_PROFILE.md`
- `RULE_INDEX.md`

Approach:
- remove repeated explanations;
- keep takeover/source-of-truth/external-parity/batch-size/consistency/failure/handoff/closeout rules explicit;
- keep detailed product, integration, testing, and regression material in routed packs rather than duplicating it here.

Protected:
- no Android/Supabase/Drive/runtime/configuration behavior changes;
- no detailed domain rule is deleted;
- Level 3 operator merge approval remains unchanged;
- current Phase 12F scope remains separate.

Rollback: `407eaf9c5c93c24a206cd75e47e58013334ec26a`.
