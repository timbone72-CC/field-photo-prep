# Field Photo Prep — Rule Index

## How to use this file

Always read:
1. `AGENTS.md`
2. `GOVERNANCE.md`
3. `PROJECT_PROFILE.md`
4. this file

Then classify the work below and read only the applicable detailed rule packs. A task may match more than one category.

For every runtime change, `TESTING_CONTRACT.md` applies at least at its general development/final-gate/failure sections. Read the deeper feature-specific testing sections only when the work touches those features.

## Routing matrix

| Work surface | Required rule packs | Typical risk floor |
| --- | --- | --- |
| Documentation/status only | `CHANGE_CONTROL_CONTRACT.md` Level 1 + document being changed | Level 1 |
| Branching, takeover, superseding work, handoff, phase closeout | `GOVERNANCE.md` + `CHANGE_CONTROL_CONTRACT.md` | Level 1–3 by underlying work |
| Android UI/navigation/presentation | relevant `CONTRACT.md` behavior + `TESTING_CONTRACT.md` + relevant `REGRESSION_CHECKLIST.md` sections | Level 2 unless presentation-only Level 1 |
| Camera/capture | `CONTRACT.md` photo capture + `TESTING_CONTRACT.md` camera boundary + Regression F/K/M | Level 2; higher if persistence/destination semantics change |
| Photo preparation/compression/orientation | `CONTRACT.md` prepared-copy rules + `TESTING_CONTRACT.md` preparation boundary + Regression G/K | Level 2 |
| Protected local state/persistence | `CONTRACT.md` capture/queue/local-data rules + `TESTING_CONTRACT.md` persistence/offline boundaries | Level 2–3 |
| Upload queue/batch/retry/reconciliation/cleanup | `CONTRACT.md` queue/retry sections + `TESTING_CONTRACT.md` batch/offline sections + `INTEGRATION_CONTRACT.md` upload/retry sections + Regression H/I/J/K/L | Level 3 when remote identity/retry/destruction semantics change |
| Drive workspace/company/address/work-order folders | `CONTRACT.md` identity/Drive sections + `INTEGRATION_CONTRACT.md` + `TESTING_CONTRACT.md` provider/Drive rules + Regression A–E/L/M | Level 3 for identity/create/reuse/delete/permission semantics |
| Android SAF/DocumentsProvider permissions/provider identity | `INTEGRATION_CONTRACT.md` + provider-freshness/device rules in `TESTING_CONTRACT.md` | Level 3 |
| Supabase/Auth/session/authorization | `docs/IDENTITY_MODEL_V1.md` + current approved identity design + relevant `CONTRACT.md` identity rules + `TESTING_CONTRACT.md` | Level 2–3 |
| Membership/invitations/roles/RLS/RPC/Edge Functions | identity model + current approved identity design + `CHANGE_CONTROL_CONTRACT.md` Level 3 + backend-focused verification record | Level 3 |
| Schema/migration/persisted-format changes | owning domain contract + `CHANGE_CONTROL_CONTRACT.md` Level 3 + `TESTING_CONTRACT.md` | Level 3 |
| Signing/application ID/release/deployment | current approved release design + `CHANGE_CONTROL_CONTRACT.md` Level 3 + affected install/update tests | Level 3 |
| Physical Android/reality gate | `TESTING_CONTRACT.md` device rules + applicable integration/domain gate + `docs/PHASE_STAGING_DOCTRINE.md` | underlying level |
| New phase or resumed prior work | `GOVERNANCE.md` takeover/source-of-truth rules + active build-state/impact record + applicable domain packs | underlying level |
| Cross-project integration | both projects' profiles/contracts + explicit integration design before implementation | normally Level 3 until boundaries are proven |

## Classification examples

### Change a label on an account screen

Surfaces:
- Android presentation only.

Load:
- Governance/profile;
- Level 1 change rules;
- the affected UI behavior source.

Do not load camera/Drive internals unless the change actually touches them.

### Change upload retry behavior

Surfaces:
- queue;
- remote write;
- destination/idempotency;
- Drive/DocumentsProvider.

Load:
- queue/retry portions of `CONTRACT.md`;
- upload/retry portions of `INTEGRATION_CONTRACT.md`;
- batch/offline/provider rules in `TESTING_CONTRACT.md`;
- relevant regression sections;
- Level 3 change rules.

### Continue an unfinished identity phase

Surfaces:
- governance takeover;
- identity/auth/backend;
- possibly Android UI.

Before creating anything:
- inspect open PRs and build-state records;
- identify the one authoritative line;
- inspect relevant live Supabase state;
- continue that line unless a documented supersession is required.

Then load the identity rules and applicable testing rules.

## Scope expansion

If implementation starts touching a new row in this matrix, add that rule pack before continuing.

If the newly touched surface raises the risk level, reclassify immediately.

Do not rely on memory of a rule pack when the current work materially depends on it.
