# Phase 12H — Organization ↔ Drive Binding Protection Impact

Date: 2026-09-26
Status: ACTIVE — Level 3 / PRE-IMPLEMENTATION
Branch: `phase-12h/organization-drive-binding-protection`
Rollback base: `5e3580d3ab769015b7f3c6168022a5def2f308c2` (merged Phase 12F)

## Exact problem

Phase 12F completed FPP User/Organization membership administration, while the existing Android Drive workspace remains local provider state. Today `FolderPrefs` persists the SAF workspace tree URI and exact provider-root identity, but that saved Drive root is not tagged with the authoritative FPP Organization UUID.

That leaves a cross-identity hazard: a valid FPP account for one Organization could encounter a still-persisted SAF grant/workspace from another Organization and the app currently has no local Organization-binding proof before treating that workspace as usable.

Phase 12H implements the already-approved Phase 12 design for a minimal local Organization ↔ Drive binding. It does not redesign the identity model or Drive hierarchy.

## Approved behavior

The approved Phase 12 design governs:

- FPP account identity and Android SAF/Drive authorization remain separate;
- a usable local Drive binding must match the currently authorized FPP Organization UUID;
- same Organization + valid local binding may reuse the existing workspace;
- a different Organization must see Drive as disconnected even if Android still retains the old SAF permission;
- FPP Auth email and Google Drive account email are never compared as an authorization rule;
- changing FPP Auth identity does not rewrite stored provider IDs;
- unresolved/protected local work blocks unsafe Organization switching;
- queued-photo immutable address/work-order destination IDs are never rewritten;
- a different workspace is selected deliberately through the existing Android SAF picker;
- no Drive provider IDs, SAF URI, company folders, properties, work orders, or photos are mirrored into Supabase;
- v1 still has no automatic Organization switcher.

## Current repository preflight

The merged Phase 12F baseline was inspected before runtime work.

### Existing local Drive owner

`FolderPrefs.java` already owns:
- legacy single-company tree URI / provider folder ID / display name;
- multi-company workspace tree URI / exact provider-root ID / display name;
- current company identity;
- current address identity bound to exact company;
- current work-order identity bound to exact address.

The current multi-company workspace keys are local SharedPreferences under `field_photo_prep`.

### Existing startup behavior

`MainActivity.onCreate()` currently:
1. loads `FolderPrefs`;
2. reads the saved effective Drive tree;
3. treats it as connected when Android still has persisted read permission;
4. restores workspace/company or legacy address navigation.

There is currently no Organization UUID check before that saved tree is reused.

### Existing workspace selection

The existing SAF picker:
1. obtains the persisted read/write tree grant;
2. calls `DriveClient.getTreeFolder(...)` to verify the selected provider root;
3. persists the tree URI plus exact provider-root ID/name through `FolderPrefs.setWorkspaceFolder(...)`.

The selected workspace is not currently tagged with FPP Organization identity.

### Existing upload/reconciliation consumers

`PhotoCaptureActivity` currently reads `folderPrefs.getMasterTreeUri()` directly when creating:
- `DrivePhotoUploader`;
- `DrivePhotoReconciler`;
- batch upload/reconciliation paths.

Therefore a Home-screen-only binding check would be insufficient. The Organization binding must be enforced through a reusable local boundary consumed by both Home and photo/upload/reconciliation flows.

### Existing authorization owner

`RuntimeAuthorizationManager` / `AuthorizationDecision` remain the single FPP authorization owner and already expose the authoritative Organization UUID.

`AuthorizationActionGuard` already protects Drive mutations according to Phase 12E authorization. Phase 12H must consume that existing decision; it must not create a second authentication/session state machine.

`AuthorizationDecision.State.DRIVE_DISCONNECTED` already exists, but Phase 12H must not duplicate or distort the settled 12E policy merely to use that enum. The implementation should keep Organization authorization and Drive-binding verification as separate responsibilities unless an exact existing seam proves otherwise.

### Existing protected-work owner

`ProtectedWorkGuard` already derives unresolved/protected local work from durable queue records and actual local copies. Phase 12H reuses this owner for switch/recovery safety; it does not introduce a second protected-work counter.

### Existing backup boundary

`data_extraction_rules.xml` and `full_backup_content.xml` already exclude all app-owned SharedPreferences, files, databases, root/device domains, and external app-owned state from cloud backup/device transfer.

The 12H binding record therefore remains local/non-portable without adding a second backup mechanism.

## Owning surfaces

Expected runtime owners:
- `app/src/main/java/com/inandout/fieldphotoprep/FolderPrefs.java`
- `app/src/main/java/com/inandout/fieldphotoprep/MainActivity.java`
- `app/src/main/java/com/inandout/fieldphotoprep/PhotoCaptureActivity.java`
- one small reusable Organization/Drive-binding policy/guard class if needed to keep the Activity logic testable and prevent bypasses

Expected focused tests:
- existing `FolderPrefsParentBindingInstrumentedTest.java`, extended for Organization binding;
- focused unit tests for the binding decision/guard seam;
- existing authorization/Drive tests only where the shared boundary changes;
- immutable queued-destination regression coverage;
- existing backup-rule tests remain authoritative unless backup resources themselves change.

Not owned by Phase 12H:
- Supabase schema, RLS, Membership or Invitation RPCs;
- Auth/session persistence format unless contradictory implementation evidence requires a governed scope change;
- Drive folder hierarchy or company/address/work-order naming;
- photo queue record format;
- photo preparation/compression;
- upload retry/reconciliation semantics;
- camera behavior;
- Google Drive sharing;
- Field Photo Prep Team.

## Persisted-data change

Add only the minimum local metadata needed to prove that the existing Drive root belongs to the current FPP Organization context.

Required new local binding metadata:
- authoritative FPP Organization UUID;
- local binding schema/version.

The existing persisted SAF tree URI and provider-root identity remain the Drive identity source. Do not duplicate them into Supabase or a second local database.

The Organization tag/version must be written atomically with a newly selected/confirmed workspace state.

## Legacy / migration behavior

Existing pre-12H Drive selections have no Organization tag.

They are treated as **legacy unbound**, not automatically owned by the first signed-in Organization.

Rules:
- an untagged saved tree is quarantined for new Drive use;
- same-Organization authoritative FPP validation is required before binding;
- the operator must explicitly confirm/reselect the intended workspace;
- the Android provider must confirm the selected tree and exact provider-root identity;
- only then may the Organization tag/version be attached;
- no folder-name fallback is allowed;
- no provider-ID remapping is allowed.

If the explicitly selected tree is the exact previously stored provider root, implementation may preserve the current company/address/work-order navigation state while adding only the Organization tag.

If a different provider root is selected, use the existing workspace-change clearing behavior. Never rewrite queued-photo destinations.

## Binding usability rule

A local workspace is usable only when all required conditions hold:
- FPP runtime state supplies an authorized Organization UUID for the current session;
- the saved binding has a supported binding version;
- saved binding Organization UUID equals the current authorized Organization UUID;
- the saved SAF tree/provider-root identity is internally consistent;
- Android still has the required persisted permission;
- the provider can verify the selected root when the flow requires live confirmation.

A different Organization is treated as Drive disconnected. The old binding is preserved for safe recovery; it is not silently overwritten or reassigned.

## Protected-work behavior

Phase 12H does not add a public Organization switcher.

If an identity transition could expose a different Organization while protected/unresolved work exists:
- preserve all local originals, prepared copies, queue/reconciliation evidence and stored destinations;
- do not reuse the old Organization's binding;
- do not remap queued work;
- block new Drive use until the correct Organization/workspace relationship is explicitly restored.

Existing Phase 12E sign-out protection remains authoritative and should prevent ordinary sign-out from stranding protected work.

## Read / write surfaces

Reads:
- central 12E authorization decision / Organization UUID;
- existing `FolderPrefs` Drive root;
- persisted SAF permission state;
- exact provider-root identity;
- protected-work state when identity-transition safety requires it.

Writes:
- only the local Organization binding UUID/version;
- existing workspace selection/navigation keys through their current owner when the operator deliberately changes workspace.

No Supabase writes are introduced by Phase 12H.

## Failure behavior

Fail closed for new Drive use when:
- Organization tag is missing;
- Organization UUID mismatches;
- binding version is unsupported;
- SAF permission is missing;
- provider-root identity cannot be verified where verification is required.

Failure must not:
- delete local photos;
- delete queue/reconciliation evidence;
- release an UNCERTAIN upload for blind retry;
- clear another Organization's binding merely because the current account differs;
- rewrite provider IDs;
- change Drive sharing;
- create folders automatically.

## Focused automated tests

Before the complete-suite gate, focused coverage must prove at least:

1. no saved workspace => disconnected/unbound;
2. legacy untagged workspace => quarantined, not silently assigned;
3. exact same Organization + supported binding => reusable;
4. different Organization => rejected/disconnected while stored binding remains unchanged;
5. same exact provider root can be explicitly confirmed and tagged without rewriting queued destinations;
6. selecting a different provider root uses existing navigation-clearing semantics while preserving queued-photo destination IDs;
7. unsupported/corrupt binding metadata fails closed;
8. upload/reconciliation cannot obtain a usable Drive tree through the shared boundary for an unbound or wrong-Organization workspace;
9. persisted binding metadata is local SharedPreferences covered by existing backup exclusions;
10. company/address/work-order parent-binding regressions continue to pass.

## Complete regression gate

The complete Android CI suite must pass once on the exact final 12H runtime head.

Existing valid Phase 12F evidence may be reused only where runtime behavior is unchanged.

## Physical / real-provider gate

Use a disposable Google Drive provider context, never a live customer/job folder, to prove:

- same authorized Organization can deliberately confirm/connect the intended workspace;
- restart reuses the correctly tagged local binding;
- a different Organization cannot silently inherit that workspace even while Android still retains the old SAF permission;
- deliberate reconnect uses the Android SAF picker;
- existing queued-photo destination IDs are unchanged;
- unrelated Drive content is untouched.

Do not claim provider-ID portability or second-device portability from this gate.

## Rollback

Prior working runtime: `5e3580d3ab769015b7f3c6168022a5def2f308c2`.

If 12H is unsafe:
1. stop before merge;
2. preserve the working Phase 12F APK/commit;
3. revert only the narrow 12H branch changes;
4. do not delete protected photos or queue state;
5. do not alter Drive content to make the binding problem disappear;
6. do not edit or clear stored provider identities unless the approved rollback specifically requires reverting newly added Organization-tag metadata.

## Merge approval

Level 3 pre-merge approval: **PENDING**.

The approved Phase 12 design authorizes implementation within this documented scope, but explicit operator approval is still required before merging the final 12H runtime change.
