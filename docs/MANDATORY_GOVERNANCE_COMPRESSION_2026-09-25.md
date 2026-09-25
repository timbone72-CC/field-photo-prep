# Mandatory Governance Compression — 2026-09-25

## Governance Classification

- Scope key: docs-compress-mandatory-governance
- Goal: Reduce the mandatory first-read governance path without removing work-control or project-safety meaning.
- Affected surfaces: Markdown governance documentation only.
- Change level: Level 1
- Authoritative branch: docs/compress-mandatory-governance
- Required rule packs: GOVERNANCE.md; PROJECT_PROFILE.md; RULE_INDEX.md; CHANGE_CONTROL_CONTRACT.md
- Protected behavior: Source-of-truth, takeover, external-state parity, largest-safe-batch, consistency, failure-stop, handoff, closeout, GitHub enforcement, project invariants, and Level 3 merge rules remain intact.
- External systems changed: None.
- Rollback point: 6a6009106564c82520e1081ba12a7741a3acb48b
- Verification boundary: Documentation semantic/diff review; Governance Check; Markdown-only Android CI.
- Level 3 merge approval: N/A

## Strategy

The mandatory layer previously repeated the same ideas across `AGENTS.md`, `GOVERNANCE.md`, `PROJECT_PROFILE.md`, and `RULE_INDEX.md`.

This pass:
- turns `AGENTS.md` into a true entry point;
- keeps process rules in `GOVERNANCE.md`;
- keeps FPP-specific boundaries in `PROJECT_PROFILE.md`;
- keeps routing only in `RULE_INDEX.md`;
- moves routing examples to optional `docs/GOVERNANCE_ROUTING_EXAMPLES.md`.

Detailed product/testing/integration rules remain untouched.
