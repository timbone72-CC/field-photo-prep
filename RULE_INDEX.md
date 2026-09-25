# Field Photo Prep — Rule Index

## Use

Always read:
1. `AGENTS.md`
2. `GOVERNANCE.md`
3. `PROJECT_PROFILE.md`
4. this file

Then read only the detailed rule packs matching the work surfaces below.

Every runtime change also uses `TESTING_CONTRACT.md` for universal test selection/final-gate/failure rules.

## Routing matrix

| Work surface | Required rule packs | Typical risk floor |
| --- | --- | --- |
| GitHub Actions / PR governance / repository enforcement | `GOVERNANCE.md` + `CHANGE_CONTROL_CONTRACT.md` + affected workflow/config | Level 2; Level 3 if deployment/signing/runtime trust changes |
| Documentation/status only | `CHANGE_CONTROL_CONTRACT.md` Level 1 + document being changed | Level 1 |
| Branching/takeover/supersession/handoff/closeout | `GOVERNANCE.md` + `CHANGE_CONTROL_CONTRACT.md` | underlying work |
| Android UI/navigation/presentation | relevant `CONTRACT.md` behavior + `TESTING_CONTRACT.md` + relevant regression sections | Level 2 unless truly presentation-only |
| Camera/capture | `CONTRACT.md` capture rules + `TESTING_CONTRACT.md` + `rules/testing/CAMERA_PREPARATION.md` + Regression F/K/M | Level 2+ |
| Photo preparation/compression/orientation | prepared-copy rules + `TESTING_CONTRACT.md` + `rules/testing/CAMERA_PREPARATION.md` + Regression G/K | Level 2 |
| Protected local state/persistence | local-state rules + `TESTING_CONTRACT.md` + applicable camera or upload/retry testing pack | Level 2–3 |
| Upload queue/batch/retry/reconciliation/cleanup | queue/retry rules + `TESTING_CONTRACT.md` + `rules/testing/UPLOAD_QUEUE_RETRY.md` + `INTEGRATION_CONTRACT.md` + Regression H/I/J/K/L | Level 3 when remote/retry/destructive semantics change |
| Drive workspace/company/address/work-order folders | Drive/product rules + `INTEGRATION_CONTRACT.md` + `TESTING_CONTRACT.md` + `rules/testing/DRIVE_PROVIDER.md` + Regression A–E/L/M | Level 3 for identity/create/reuse/delete/permission semantics |
| SAF/DocumentsProvider/provider identity | `INTEGRATION_CONTRACT.md` + `TESTING_CONTRACT.md` + `rules/testing/DRIVE_PROVIDER.md` | Level 3 |
| Supabase/Auth/session/authorization | `docs/IDENTITY_MODEL_V1.md` + current approved identity design + relevant product rules + `TESTING_CONTRACT.md` | Level 2–3 |
| Membership/invitations/roles/RLS/RPC/Edge Functions | identity model + current approved identity design + Level 3 change rules + backend verification record | Level 3 |
| Schema/migration/persisted-format changes | owning domain rules + Level 3 change rules + `TESTING_CONTRACT.md` | Level 3 |
| Signing/application ID/release/deployment | approved release design + Level 3 change rules + affected install/update tests | Level 3 |
| Physical Android/reality gate | `TESTING_CONTRACT.md` + applicable testing pack + integration/domain gate + staging doctrine | underlying level |
| New phase/resumed prior work | takeover/source-of-truth rules + active build-state/impact record + applicable domain packs | underlying level |
| Cross-project integration | both projects' profiles/contracts + explicit integration design | normally Level 3 until boundaries are proven |

## Scope expansion

If implementation begins touching another row, load that rule pack before continuing.

If the new surface raises risk, reclassify the change immediately.

Do not rely on memory when the repository rule source is available.

Optional routing examples: `docs/GOVERNANCE_ROUTING_EXAMPLES.md`.
