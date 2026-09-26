# Field Photo Prep — Rule Index

Always read `AGENTS.md`, `GOVERNANCE.md`, `PROJECT_PROFILE.md`, and this file. Then load only packs matching the work surface.

Every runtime change uses `TESTING_CONTRACT.md` for universal test/failure rules. Add feature testing packs only when those surfaces are touched.

| Work surface | Required packs | Typical floor |
| --- | --- | --- |
| GitHub Actions / PR governance / repository enforcement | `GOVERNANCE.md`, `CHANGE_CONTROL_CONTRACT.md`, affected workflow/config | L2; L3 if deployment/signing/runtime trust changes |
| Documentation/status only | L1 rules + affected document | L1 |
| Branching/takeover/supersession/handoff/closeout | `GOVERNANCE.md`, `CHANGE_CONTROL_CONTRACT.md` | underlying level |
| Android UI/navigation/presentation | relevant `CONTRACT.md`, `TESTING_CONTRACT.md`, affected regression sections | L2; presentation-only may be L1 |
| Camera/capture | capture rules, `TESTING_CONTRACT.md`, `rules/testing/CAMERA_PREPARATION.md`, Regression F/K/M | L2+ |
| Preparation/compression/orientation | prepared-copy rules, `TESTING_CONTRACT.md`, `rules/testing/CAMERA_PREPARATION.md`, Regression G/K | L2 |
| Protected local persistence | relevant `CONTRACT.md`, `TESTING_CONTRACT.md`, owner-specific testing pack | L2–L3 |
| Upload/batch/retry/reconciliation/cleanup | queue/retry rules, `TESTING_CONTRACT.md`, `rules/testing/UPLOAD_QUEUE_RETRY.md`, upload/retry integration rules, Regression H/I/J/K/L | L3 when remote identity/retry/destruction changes |
| Drive workspace/company/address/work-order folders | identity/Drive rules, `INTEGRATION_CONTRACT.md`, `TESTING_CONTRACT.md`, `rules/testing/DRIVE_PROVIDER.md`, Regression A–E/L/M | L3 for identity/create/reuse/delete/permission semantics |
| SAF/DocumentsProvider permission/provider identity | `INTEGRATION_CONTRACT.md`, `TESTING_CONTRACT.md`, `rules/testing/DRIVE_PROVIDER.md` | L3 |
| Supabase/Auth/session/authorization | identity model + current identity design + relevant product rules + `TESTING_CONTRACT.md` | L2–L3 |
| Membership/invitations/roles/RLS/RPC/Edge Functions | identity model + current identity design + L3 change rules + backend verification record | L3 |
| Schema/migration/persisted format | owning domain contract + L3 rules + `TESTING_CONTRACT.md` | L3 |
| Signing/application ID/release/deployment | approved release design + L3 rules + install/update verification | L3 |
| Physical Android/reality gate | universal testing + applicable feature pack + integration/domain gate + staging doctrine | underlying level |
| New phase/resumed work | governance takeover rules + active build-state/impact record + applicable packs | underlying level |
| Cross-project integration | both projects' profiles/contracts + explicit integration design | normally L3 until boundaries are proven |

If implementation touches another row, load that pack before continuing. If risk increases, reclassify immediately. Do not rely on memory when a rule pack materially governs the work.
