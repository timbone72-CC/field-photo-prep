# Phase 12H — Build State

Last updated: 2026-09-26

This file is the durable handoff point for Phase 12H — Organization ↔ Drive Binding Protection.

## Authoritative line

- merged Phase 12F / rollback base: `5e3580d3ab769015b7f3c6168022a5def2f308c2`
- active branch: `phase-12h/organization-drive-binding-protection`
- change level: **Level 3**
- merge approval: **PENDING**
- runtime implementation: **NOT STARTED**

Do not implement 12H on the old Phase 12F branch and do not modify `main` directly.

## Governing design

Phase 12H is already designed by:
- `docs/PHASE_12_MASTER_PLAN_2026-09-25.md`
- `docs/PHASE_12_COMPLETE_DESIGN_2026-09-25.md`
- `docs/IDENTITY_MODEL_V1.md`

This slice is implementation preparation, not a redesign.

## Preflight source-of-truth map

Confirmed on merged Phase 12F baseline:

- `FolderPrefs` owns the persisted local Drive tree/provider-root identity and company/address/work-order parent bindings.
- `MainActivity` currently restores any saved tree with a surviving SAF read grant without an Organization tag.
- the SAF picker verifies the selected provider root before persisting it.
- `PhotoCaptureActivity` reads the effective saved tree directly for upload and reconciliation construction.
- `DriveClient` already gates Drive mutations through `AuthorizationActionGuard`.
- `RuntimeAuthorizationManager` / `AuthorizationDecision` own the authoritative current Organization UUID.
- `ProtectedWorkGuard` owns durable unresolved/protected-work detection.
- all app SharedPreferences/provider-bound state are already excluded from cloud backup/device transfer.

Therefore 12H must create one reusable Organization-binding boundary consumed by both Home and Photos. A Home-only check is insufficient.

## Settled implementation boundary

The 12H change may:
- add local Organization UUID + binding-version metadata beside the existing Drive root;
- add a small pure binding policy/guard to keep checks testable and shared;
- update workspace selection/confirmation to bind only after exact provider verification;
- make saved-workspace restoration fail closed for unbound/wrong-Organization state;
- route upload/reconciliation tree access through the same Organization-binding boundary;
- add/extend focused tests.

The 12H change may not:
- add Supabase tables or Drive metadata;
- change queue destination identities;
- change upload retry/reconciliation semantics;
- change company/address/work-order hierarchy;
- infer Drive account from Auth email;
- auto-assign a legacy untagged Drive tree to the first signed-in Organization;
- introduce an Organization switcher;
- create a second auth state machine.

## Required rule packs

- `GOVERNANCE.md`
- `PROJECT_PROFILE.md`
- `RULE_INDEX.md`
- `CHANGE_CONTROL_CONTRACT.md`
- `TESTING_CONTRACT.md`
- `INTEGRATION_CONTRACT.md`
- `rules/testing/DRIVE_PROVIDER.md`
- relevant Drive/identity sections of `CONTRACT.md`
- `docs/PHASE_STAGING_DOCTRINE.md`

## Planned focused evidence

Automated:
- local binding state/migration decisions;
- legacy-unbound quarantine;
- same-Organization reuse;
- wrong-Organization rejection;
- corrupt/unsupported binding fail-closed;
- explicit same-provider confirmation;
- upload/reconciliation shared-boundary rejection when unbound/mismatched;
- queued destination immutability;
- existing FolderPrefs parent-binding regression;
- existing backup exclusion regression.

Final:
- complete Android CI once on the exact final runtime head.

Physical:
- smallest real Samsung + disposable Google Drive provider gate after automated work is complete.

## Exact next checkpoint

Before runtime coding:
1. create the draft 12H PR with the Level 3 governance classification;
2. reconcile the impact/build-state docs into that PR;
3. inspect the exact current `INTEGRATION_CONTRACT.md` provider gate and the affected Drive/product contract clauses;
4. then implement the largest coherent automated batch, starting with the local binding store/policy and focused tests.

Stop if:
- a second persisted Drive-binding system appears necessary;
- queue/photo destination schema would need to change;
- Supabase would need Drive provider identity;
- current provider/live state contradicts the approved design;
- scope expands beyond the documented 12H boundary.
