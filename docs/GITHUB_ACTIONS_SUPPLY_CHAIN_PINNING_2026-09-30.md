# GitHub Actions Supply-Chain Pinning — 2026-09-30

## Classification

- Goal: replace mutable GitHub Actions version tags used by FPP CI/release workflows with the exact reviewed commits currently resolved by those tags.
- Change level: **Level 3** because this touches the production release-candidate trust path as well as CI supply-chain execution.
- Authoritative line: `hardening/pin-github-actions` / its single PR.
- Governed base / rollback point: `c9e6965dc934d697b18e7b62e5f4ebbc0dfb9656` (current governed `main` before this scope).
- Required rules: `AGENTS.md`, `GOVERNANCE.md`, `PROJECT_PROFILE.md`, `RULE_INDEX.md`, `CHANGE_CONTROL_CONTRACT.md`, `TESTING_CONTRACT.md`, and the existing release/signing design records.
- External systems changed: none by implementation itself. GitHub-hosted workflow execution is exercised by verification; no Supabase, Drive, Android app data, signing secret, or production deployment is changed.

## Scope

Expected changed runtime/repository surfaces:

- `.github/workflows/android-ci.yml`
- `.github/workflows/production-release-candidate.yml`
- this durable record

No Android source, Gradle dependency, signing identity, package ID, Supabase configuration, Drive behavior, queue state, photo state, or user workflow is in scope.

## Reviewed action identities

The existing mutable tags were resolved immediately before implementation through GitHub and are pinned to these exact commits:

- `actions/checkout@v4` → `11d5960a326750d5838078e36cf38b85af677262`
- `actions/setup-java@v5` → `b6effb05e454b25005698d916606bdc6ffcbf961`
- `gradle/actions/setup-gradle@v4` → `ed408507eac070d1f99cc633dbcf757c94c7933a`
- `ReactiveCircus/android-emulator-runner@v2` → `a421e43855164a8197daf9d8d40fe71c6996bb0d`
- `actions/upload-artifact@v4` → `ea165f8d65b6e75b540449e92b4886f43607fa02`

Pin comments retain the human-readable major tag for maintenance context.

## Protected behavior

- Android CI must execute the same logical steps and conditions as before.
- Production Release Candidate must preserve its current signing-secret validation, fail-closed identity rules, version/package/callback verification, signer/hash verification, artifact retention, and private artifact behavior.
- No secrets are added to source.
- No production candidate is dispatched merely to prove the pinning change unless explicitly required and authorized.
- Existing branch protection/governance behavior remains unchanged.

## Risks

Primary risk: a mistaken action commit could make CI or the release workflow unusable, or subtly change the trusted implementation used for checkout, Java/Gradle setup, emulator execution, or artifact upload.

Mitigation:
- pin only the commits currently resolved by the already-used major tags;
- inspect every workflow diff;
- run the normal PR CI on the exact final head;
- verify required checks and artifacts behave normally;
- do not merge on a required failure.

## Verification boundary

Required before merge consideration:

1. Diff proves only action references/comments and this record changed.
2. Android CI succeeds on the exact final PR runtime/workflow head.
3. Governance/required checks succeed.
4. No signing/package/runtime source changes are present.
5. Explicit Level 3 operator approval is required before merge.

Production release-candidate dispatch is **not automatically required** because the action pins do not modify release inputs, signing secrets, package construction, or release verification logic. If CI or review reveals a release-specific uncertainty that normal static review cannot close, stop and classify the additional gate before dispatching anything.

## Rollback

Revert this narrow PR or reset the two workflow files to governed base `c9e6965dc934d697b18e7b62e5f4ebbc0dfb9656`. No app data, Drive content, Supabase state, or signing material requires rollback.

## Status

Implementation authorized by the operator's instruction to proceed as far as safely possible with current single-user hardening.

Pre-merge Level 3 approval: **NOT YET GRANTED**.
