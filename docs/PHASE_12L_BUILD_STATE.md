# Phase 12L Build State

Date: 2026-09-27

Status: **IN PROGRESS — AUTHORITATIVE REALITY-GATE LINE ESTABLISHED**

## Governed base

- `main`: `c7970238e00139415a417f669d86915c4260154e`;
- Phase 12K merge: PR #84;
- Phase 12K post-merge release activation: PASS;
- post-merge Android CI `36316289354`: PASS;
- Production Release Candidate `36316384072`: PASS.

## Authoritative line

- branch: `phase-12l/clean-install-new-user-reality-gates`;
- PR: #85 — `Phase 12L: clean-install and new-user reality gates` (Draft);
- competing 12L branch/PR at takeover: none;
- unrelated Draft PR #39 remains outside Phase 12L.

## Classification

**Level 1 — documentation/evidence gate only.**

No runtime, schema, Drive-provider, authorization, signing, or backend mutation is authorized on this branch. A discovered defect must use a separately governed implementation line.

## Contract/source-of-truth audit

Reconciliation performed before physical testing:

- `CONTRACT.md`: add approved permanent production Android identity/signing/update rules from completed 12K;
- `TESTING_CONTRACT.md`: add release/clean-install evidence rules;
- `CHANGE_CONTROL_CONTRACT.md`: current; no edit required;
- `INTEGRATION_CONTRACT.md`: current; no edit required;
- `GOVERNANCE.md`: current; no edit required;
- `PROJECT_PROFILE.md`: current; no edit required;
- `RULE_INDEX.md`: current; no edit required;
- `REGRESSION_CHECKLIST.md`: no new runtime behavior in 12L, so no checklist expansion required.

## 12K closeout reconciled

- PR #84 merged to main at `c7970238e00139415a417f669d86915c4260154e`;
- GitHub Production Release Candidate run `36316384072`: PASS;
- GitHub-built v37 APK signer/package/callback/Supabase evidence: PASS;
- GitHub-built APK SHA-256: `49a02f6fe4d1a8becacae305966e46145bdaa9cb63cea105947763d3e24ad0dd`;
- artifact ID `10931335308`;
- main post-merge Android CI `36316289354`: PASS.

## Exact next gate

Before making the production package fresh, inspect the installed **production** app and prove it contains no protected local photos, unresolved upload/queue work, or production-only Drive binding/state that must be preserved.

Do **not** clear/uninstall `com.inandout.fieldphotoprep.internal`.

Once the production-package precondition is clean, begin Gate A from `docs/PHASE_12L_REALITY_GATE_PLAN_2026-09-27.md`.

## Production pre-clear safety evidence

Production pre-clear safety gate: **PASS**.

Observed production support status before any clear/uninstall:
- app version: `0.28.0`;
- account state: `VALIDATED`;
- role: `OWNER`;
- Drive state: `NO_WORKSPACE`;
- camera permission: `NOT_GRANTED`;
- queue CAPTURING / WAITING / UPLOADING / FAILED / UNCERTAIN / UPLOADED: all `0`;
- protected originals: `0`;
- cleanup pending: `0`;
- unreadable local records: `0`.

Conclusion: the production package contains no protected local work, unresolved upload state, or production-only Drive workspace binding that needs preservation. It is safe to clear **only** `com.inandout.fieldphotoprep` for Gate A. The working `com.inandout.fieldphotoprep.internal` package remains protected and must not be cleared or uninstalled.

## Gate A — clean/no-session production launch

Gate A clean/no-session launch: **PARTIAL PASS**.
- production package was cleared after the pre-clear safety gate passed;
- fresh production launch displayed `Field Photo Prep Account` with empty Email/Password fields and Sign In / Forgot Password actions;
- no prior OWNER session was silently restored;
- ordinary authenticated field use therefore remains gated behind FPP sign-in as designed.

Remaining Gate A sub-check: physically confirm `com.inandout.fieldphotoprep.internal` is still installed/unchanged after clearing production. That separation proof may also be reused for Gate F.

## Gate B — returning Owner identity

Gate B returning Owner identity: **PARTIAL PASS**.
- fresh production package accepted the controlled Owner sign-in;
- app reached Home after authentication;
- Home shows Google Drive `Not connected` with explicit `Connect Drive` action;
- no Drive workspace was silently restored or inferred by FPP authentication;
- property state is fresh/empty on this production install.

Remaining Gate B proof: capture App Status after fresh sign-in to confirm account `VALIDATED`, expected Organization, and role `OWNER` on this cleared production package.
