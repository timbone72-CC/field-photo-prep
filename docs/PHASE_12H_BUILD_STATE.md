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


## Samsung reality gate — same-Organization restart reuse PASS

Physical Samsung evidence on 2026-09-26 using the exact green 12H runtime build:

- FPP was fully closed after explicit Drive confirmation;
- app was reopened without reconnecting or changing Drive state;
- the saved Organization-tagged binding was reused automatically for the same authorized Organization;
- Home reopened connected to **HNP Jobs** under **Workspace: Photos**;
- existing property list restored with 25 properties;
- no **Confirm Drive** prompt reappeared.

This satisfies the same-Organization restart/reuse gate.

Remaining physical 12H evidence:
- prove a different Organization cannot silently inherit this workspace while Android still retains the old SAF permission;
- verify deliberate reconnect still uses Android SAF;
- preserve/verify queued-photo destination identities and unrelated Drive content.


## Samsung reality gate — different-Organization isolation FAIL (caught before merge)

Physical Samsung evidence on 2026-09-26 exposed a real lifecycle gap:

- the existing Drive binding was tagged to the real Organization and Android retained the SAF permission;
- `timbone72@gmail.com` was temporarily moved to one disposable ACTIVE Organization for the isolation test;
- Auth UI correctly showed the new `Phase 12H Isolation Fixture` Organization;
- after closing AuthActivity, Home incorrectly still displayed **HNP Jobs / Workspace: Photos** and the prior 25-property list.

Backend fixture cleanup/restoration was completed immediately afterward:
- the real `timbone72@gmail.com` Membership was restored to `MEMBER · ACTIVE`;
- disposable fixture Membership and Organization were deleted;
- real Organization remained ACTIVE with one ACTIVE OWNER.

Root cause from source reconciliation:
- `RuntimeAuthorizationManager.replaceAuthenticatedSession(...)` correctly stores and observes the new Organization;
- `OrganizationDriveBindingGuard` correctly rejects a wrong-Organization binding when queried;
- however, returning from `AuthActivity` resumes the existing `MainActivity`;
- `MainActivity.onResume()` revalidated authorization but did not re-evaluate Drive binding state or clear the prior in-memory company/property/work-order lists;
- therefore stale Home UI leaked the previous Organization's Drive navigation state until another render/action path re-evaluated the binding.

Required fix:
- on every MainActivity resume, re-evaluate the Organization/Drive binding;
- when unusable/wrong-Organization, immediately clear in-memory Drive navigation lists/selections and render disconnected/quarantined state;
- preserve persisted binding/provider IDs and queued destinations;
- do not clear the saved old-Organization binding merely because another Organization is signed in.

The different-Organization physical gate remains **FAIL** until rerun on a fixed green APK.


## Cross-Organization resume fix — automated PASS

The first lifecycle fix at `74646e89241636946a55ea0f8ab26c0e12d70591` correctly added binding reconciliation on `MainActivity.onResume()`, but Android CI exposed an over-broad regression: ordinary `NO_WORKSPACE` resumes were forcing Work Orders/Photos navigation back to Home.

That regression was corrected at runtime commit `79e505e1703624b81d56b1810063a1904df9fd76`:
- `NO_WORKSPACE` preserves the current local screen/navigation state;
- saved-but-unusable binding states (including wrong Organization, legacy unbound, invalid binding, permission missing, and authorization-required cases) still clear only in-memory Drive navigation and render the safe disconnected/quarantined Home state;
- persisted provider IDs, Organization binding metadata, SAF grants, and queued-photo destinations remain untouched.

Exact corrected Android CI:
- run `36245357592`
- runtime head `79e505e1703624b81d56b1810063a1904df9fd76`
- governance: PASS
- unit tests: PASS
- internal debug build: PASS
- stable test APK signer verification: PASS
- instrumented image/UI tests: PASS
- internal launch smoke test: PASS
- APK/test evidence artifact upload: PASS

Next checkpoint: rerun only the previously failed physical different-Organization isolation gate on the corrected APK.


## Samsung reality gate — cross-Organization stale-UI regression fixed

Physical Samsung retest on 2026-09-26 using corrected runtime commit
`79e505e1703624b81d56b1810063a1904df9fd76`:

- disposable `Phase 12H Isolation Fixture` was recreated for `timbone72@gmail.com`;
- real Organization retained one ACTIVE OWNER;
- app returned to Home under the different Organization;
- prior **HNP Jobs / Workspace: Photos** UI was no longer visible;
- property count was 0;
- Home rendered Drive disconnected with **Connect Drive**;
- message required Field Photo Prep account recheck before Drive use;
- no prior property/navigation list leaked from the owning Organization.

This confirms the lifecycle stale-UI bug caught in the first reality attempt is fixed.

The different-Organization isolation gate is not yet marked fully PASS because this screenshot is in an authorization-recheck-required state. Final proof still requires:
- revalidate the disposable Organization;
- return to Home;
- verify the old HNP/Photos binding remains unavailable after fresh authorization.


## Samsung reality gate — different-Organization isolation PASS

Final physical Samsung proof on 2026-09-26 using corrected runtime head
`e62a911ab8c73165fbc9aac50eca485ef6894830`:

- existing saved Drive binding remained tagged to the real FPP Organization;
- Android retained the original SAF permission;
- `timbone72@gmail.com` was temporarily assigned one disposable ACTIVE Organization;
- account recheck completed successfully under `Phase 12H Isolation Fixture`;
- after returning to Home, the old **HNP Jobs / Workspace: Photos** binding remained unavailable;
- Home displayed **0 properties**;
- UI explicitly reported that the saved Drive workspace belongs to a different Field Photo Prep Organization;
- UI required connecting that Organization's own workspace;
- old provider/company/property state did not leak across Organizations.

The disposable fixture was immediately cleaned up afterward:
- `timbone72@gmail.com` restored to `MEMBER · ACTIVE` in the real Organization;
- fixture Membership deleted;
- fixture Organization deleted and verified absent;
- real Organization retained exactly one ACTIVE OWNER.

This satisfies the different-Organization isolation reality gate.

The runtime lifecycle/UI regressions discovered during this gate were fixed before final PASS:
- stale Home navigation on return from AuthActivity;
- ordinary NO_WORKSPACE resume regression;
- Account/recheck path hidden while Drive was blocked;
- stale company/workspace actions exposed by an outdated test assumption.

Exact current green runtime CI:
- runtime head `e62a911ab8c73165fbc9aac50eca485ef6894830`;
- Android CI run `36246719902`;
- governance PASS;
- unit tests PASS;
- internal APK build PASS;
- signer verification PASS;
- instrumented UI/image tests PASS;
- launch smoke test PASS.
