# Phase 12H — Build State

Last updated: 2026-09-26

This file is the durable handoff point for Phase 12H — Organization ↔ Drive Binding Protection.

## Authoritative line

- merged Phase 12F / rollback base: `5e3580d3ab769015b7f3c6168022a5def2f308c2`
- active branch: `phase-12h/organization-drive-binding-protection`
- draft PR: **#80 — Phase 12H: organization Drive binding protection**
- change level: **Level 3**
- merge approval: **PENDING**
- runtime implementation: **FIRST AUTOMATED BATCH PASS**

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

## Automated runtime checkpoint — PASS

Exact tested runtime head: `114ccc5747488757dc824435e49713387e13459a`.

Android CI run `36243441754` completed successfully on 2026-09-26.

PASS evidence:
- governance check;
- unit tests;
- internal debug build;
- stable test APK signer verification;
- instrumented image tests;
- internal launch smoke test;
- APK artifact upload;
- rendered/test-evidence artifact upload.

Implemented and covered:
- local Organization UUID + binding-version metadata beside the existing provider-bound Drive root;
- legacy untagged workspace quarantined/fail-closed;
- wrong-Organization binding rejected without rewriting stored Drive identity;
- same exact provider root can be explicitly confirmed and tagged without clearing company/address/work-order navigation;
- different provider root uses existing navigation-clearing behavior while queued destination IDs remain unchanged;
- corrupt/partial workspace state fails closed;
- Home and Photos consume one shared Organization/Drive binding guard;
- Photos has no direct raw `FolderPrefs.getMasterTreeUri()` upload/reconciliation bypass;
- new capture requires a usable Organization/Drive binding because the capture record carries the selected provider destination;
- GRACE may reuse an already-correct binding, but only online `VALIDATED` authorization may create/rebind one;
- Phase 12E `AuthActivity` remains the single protected-work identity-transition guard and blocks cross-User/Organization session replacement while protected work exists.

## Exact next checkpoint

Run the smallest required Samsung + disposable real Google Drive provider gate against the exact green runtime head `114ccc5747488757dc824435e49713387e13459a`.

Prove only:
1. legacy saved workspace is shown as needing explicit confirmation rather than silently reused;
2. current validated Organization can deliberately confirm/reselect the intended workspace through Android SAF;
3. app restart reuses the now-tagged binding for that same Organization;
4. a different Organization cannot silently inherit that workspace even if Android retains the old SAF permission;
5. reconnect uses the Android SAF picker;
6. queued-photo destination IDs remain unchanged;
7. unrelated Drive content is untouched.

Do not broaden this into production-release testing or a second-device portability claim.

Stop if:
- a second persisted Drive-binding system appears necessary;
- queue/photo destination schema would need to change;
- Supabase would need Drive provider identity;
- current provider/live state contradicts the approved design;
- scope expands beyond the documented 12H boundary.


## Samsung reality gate — legacy workspace quarantine PASS

Physical Samsung evidence on 2026-09-26 using the exact green 12H runtime build from commit `114ccc5747488757dc824435e49713387e13459a`:

- app updated over the existing installation;
- existing pre-12H Drive provider state remained present locally;
- app did **not** silently reuse that workspace;
- Home rendered Drive as disconnected/quarantined;
- UI showed **Confirm Drive**;
- UI message: saved Drive workspace must be confirmed for the current Field Photo Prep Organization;
- property list remained unavailable until confirmation.

This satisfies the first physical gate: legacy untagged Drive state is quarantined rather than automatically assigned to the signed-in Organization.

Next physical step: deliberately confirm/reselect the intended exact workspace through Android SAF, then verify same-Organization restart reuse.


## Samsung reality gate — explicit same-Organization confirmation PASS

Physical Samsung evidence on 2026-09-26 using the exact green 12H runtime build:

- operator tapped **Confirm Drive** from the quarantined legacy state;
- Android SAF picker was used to deliberately select/confirm the intended existing top-level workspace;
- app returned to Home with Drive connected;
- workspace displayed as **Photos**;
- active company displayed as **HNP Jobs**;
- existing property list restored (25 properties visible);
- no automatic reassignment occurred before explicit confirmation.

This satisfies the explicit same-Organization confirmation/reconnect gate.

Next physical step: fully close/reopen FPP and verify the same Organization reuses the now-tagged binding without asking for Drive confirmation again.
