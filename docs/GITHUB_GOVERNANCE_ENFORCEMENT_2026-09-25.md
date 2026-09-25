# GitHub Governance Enforcement — 2026-09-25

## Classification

- Goal: add machine-checkable PR governance without changing FPP runtime behavior.
- Affected surfaces: GitHub pull-request metadata and GitHub Actions only.
- Change level: Level 2.
- Authoritative branch: `governance/enforce-pr-controls`.
- Required rule packs: `GOVERNANCE.md`, `PROJECT_PROFILE.md`, `RULE_INDEX.md`, `CHANGE_CONTROL_CONTRACT.md`.
- Protected behavior: Android runtime, photo/Drive workflow, Supabase state, signing identity, and Phase 12F implementation remain unchanged.
- External systems changed: GitHub repository workflow metadata only after merge; branch protection itself is not changed by this PR.
- Rollback point: `fb63c273ddc1ce749c18954c1b51b05475a8f603`.
- Verification boundary: workflow/diff review plus GitHub Actions parsing on future PR events.
- Level 3 merge approval: N/A.

## Purpose

GitHub can enforce repository facts it can observe, but it cannot understand all project semantics.

This change adds a machine-readable layer for the observable parts:
- every PR must declare a stable scope key;
- every PR must declare its change level and routed rule packs;
- authoritative branch metadata must match the actual PR branch;
- duplicate open PRs using the same scope key fail;
- `supabase/` and signing-keystore changes cannot be classified below Level 3;
- GitHub Actions workflow changes cannot be classified Level 1;
- a non-draft Level 3 PR cannot pass the governance check while merge approval is still recorded as PENDING.

The check does not claim to prove:
- that the chosen scope key is semantically perfect;
- that all relevant rule packs were selected correctly;
- that live Supabase/Drive state truly matches source;
- that a recorded Level 3 approval was independently authored by a different human identity.

Those remain governed by repository rules and operator process.

## CI cost control

The existing Android CI now treats a pull request as documentation-only only when every changed path ends in `.md`.

For Markdown-only PRs:
- checkout and scope detection still run;
- the Android CI status still completes successfully;
- Gradle, APK build/signing, KVM, emulator, and artifact upload steps are skipped.

Any non-Markdown change runs the full Android pipeline. Pushes to governed runtime branches continue to run the full pipeline.

This preserves one stable Android CI check name while avoiding expensive runtime verification for documentation-only changes.

## Verification evidence

Workflow-changing head `78860e9e08c7d95d29b268fb1ed4f8e7d8c2d8ae`:
- Android CI run `36196885402`: **PASS**
- scope detector correctly selected full runtime verification because YAML workflow files changed;
- unit tests: PASS;
- debug build: PASS;
- stable signer verification: PASS;
- instrumented image tests/internal launch smoke: PASS;
- artifact packaging: PASS.

This evidence record itself is Markdown-only and is used to prove that the next Android CI run takes the lightweight documentation path while keeping the same required `test` check.

## Branch-protection target after this check exists

For `main`, the recommended GitHub protection is:
- require a pull request before merging;
- require the Governance Check status;
- require Android CI for runtime changes once the required-check strategy is finalized;
- require conversation resolution;
- block force pushes;
- block deletion;
- do not require a second approving reviewer;
- do not require signed commits;
- do not require linear history.

Repository administration settings are not modified by this branch.
