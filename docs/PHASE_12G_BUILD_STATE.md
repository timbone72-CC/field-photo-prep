# Phase 12G Build State

Date: 2026-09-26

Status: **IMPLEMENTATION & REQUIRED EVIDENCE COMPLETE — GOVERNED CLOSEOUT ON PR #81**

## Governed base

- main/rollback commit: `7b4c2017e9fa013929247751039db08cdae9418f`;
- this is the governed merge commit for completed Phase 12H PR #80;
- Phase 12H runtime/provider evidence is complete;
- stale 12H status text is reconciled in the first 12G documentation checkpoint.

## Authoritative implementation line

- branch: `phase-12g/first-run-new-device-flow`;
- PR: #81 — `Phase 12G: first-run and new-device flow` (Draft);
- competing 12G branch/PR at takeover: none;
- unrelated draft PR #39 does not overlap this scope.

## Classification

**Level 2**

12G is limited to first-run/new-device navigation and orchestration over the existing Phase 12E authorization owner and Phase 12H Drive-binding owner.

Level 3 is not authorized inside this scope.

## Completed preflight

- current protected `main` inspected;
- open PRs/branches checked for overlapping 12G ownership;
- Phase 12 master plan and complete design reread;
- Phase 12H completion evidence reconciled;
- governance/change-control/testing/integration/identity rules loaded;
- current runtime owners inventoried:
  - `AuthActivity`;
  - `RuntimeAuthorizationManager` / authorization policy/guard;
  - `OrganizationDriveBindingGuard`;
  - `FolderPrefs`;
  - `MainActivity`;
- implementation boundary recorded in `docs/PHASE_12G_FIRST_RUN_NEW_DEVICE_IMPACT_2026-09-26.md`.

## External systems

No live external system change is authorized at this checkpoint.

- Supabase: read/consume existing identity state only;
- Google Drive: no mutation performed by preflight;
- Android phone: no install/reset required;
- GitHub: branch/PR documentation only.

## Protected behavior

Must remain unchanged:
- photo protection;
- queue/reconciliation identity;
- immutable photo destinations;
- upload/retry semantics;
- Phase 12E authorization state machine;
- Phase 12H Organization↔Drive binding semantics;
- existing SAF/provider identity;
- Home / Work Orders / Photos field workflow after setup.

## First runtime slice — required authentication entry

Implemented on this branch after the documentation preflight:

- fresh `MainActivity` entry checks the authoritative Phase 12E decision before restoring Drive UI;
- `SIGN_IN_REQUIRED` opens the existing `AuthActivity` with a narrow, in-memory required-entry marker;
- `AuthActivity` uses its existing sign-in/invitation/recovery owner; it does not create a second auth path;
- after that required entry reaches authoritative `VALIDATED`, AuthActivity finishes back to the existing MainActivity;
- closing AuthActivity without authentication leaves existing read/recovery Home reachable; 12G does not create an on-resume auth loop;
- MainActivity then resumes its existing Phase 12E revalidation and Phase 12H binding reconciliation;
- no Drive binding, `FolderPrefs`, Supabase, queue, photo, or provider identity semantics changed.

Focused automated coverage:
- `SIGN_IN_REQUIRED` requires the existing auth route;
- `VALIDATED` completes a required entry only when that entry marker is present;
- GRACE, RECHECK_REQUIRED, NO_MEMBERSHIP, REVOKED, and DRIVE_DISCONNECTED do not create a second 12G recovery state machine;
- emulator no-session launcher coverage proves MainActivity opens the real AuthActivity;
- unrelated pre-existing MainActivity UI tests explicitly dismiss the real auth Activity rather than using a production authentication bypass.

### Reconciliation note

During this slice, overlapping commit `6d2cd275eb4cd5e0a1922d46e88a618036f0af1b` advanced the same authoritative branch while another edit was in progress. Its first CI run `36284696569` failed because existing UI tests were left STOPPED behind the newly expected AuthActivity. The branch was reconciled in place rather than creating a second line. The retained design uses one required first-entry gate and preserves read/recovery access after the gate is closed.

## Invited-user continuation

The same Level 2 orchestration now covers the cold-start invitation path:

- an accepted/validated invitation is marked as an onboarding entry;
- after authoritative `VALIDATED` Membership, AuthActivity explicitly opens/reuses `MainActivity` with `CLEAR_TOP | SINGLE_TOP` and finishes;
- this does not infer or grant Drive access;
- on a new device, MainActivity reaches the existing `NO_WORKSPACE` state and presents the existing deliberate Connect Drive path;
- recovery links are not automatically treated as invitation onboarding unless they originated from an already-required auth entry;
- no Supabase, Drive-binding, SAF, queue, or photo semantics changed.

Existing code already owns the next transition:
- `OrganizationDriveBindingGuard.NO_WORKSPACE` renders **Connect Drive**;
- `chooseMasterFolder()` requires an online `VALIDATED` Organization before opening Android SAF;
- `NextActionGuide` already guides `workspaceConnected=false` to **Next: Connect Google Drive**;
- 12H tests already prove only `VALIDATED` may create/rebind a workspace.

Therefore 12G does not add another Drive setup implementation.

## Final runtime evidence

Authoritative final runtime head:

`4930c4b06a51012ada9cc07c503abeae93e97b27`

Exact Android CI:
- run `36286296646`;
- unit tests: **PASS**;
- internal APK build: **PASS**;
- stable test signer verification: **PASS**;
- instrumented UI/image tests: **PASS**;
- internal launch smoke: **PASS**;
- artifacts/evidence upload: **PASS**.

Focused Phase 12G evidence now proves:
- a fresh no-session launcher entry reaches the existing AuthActivity;
- closing that one-time entry leaves protected/read-only Home recovery reachable;
- Android Activity recreation does not force an authentication loop back over read/recovery Home;
- required authentication returns to Main only after authoritative VALIDATED identity;
- an INVITE redirect continues first-run onboarding after successful invitation activation/validation;
- a cold-start invitation does not infer or grant Drive permission;
- a saved same-Organization binding that was authorization-blocked before sign-in is preserved as the prior binding state so the existing 12H resume policy reloads it after validation;
- NO_WORKSPACE continues into the existing deliberate Connect Drive / Android SAF path;
- the existing 12H guard still requires online VALIDATED Organization identity before creating/rebinding a workspace;
- no provider ID, SAF URI, queue destination, photo identity, or Supabase backend semantics changed.

## Failure/reconciliation history

Required failures were not reported as passes:

- run `36284696569` exposed that pre-existing MainActivity UI tests were stopped behind the newly expected AuthActivity; tests were reconciled by dismissing the real gate in test-only code rather than adding a production bypass;
- run `36285261268` exposed checked-exception handling in that test-only helper; the helper was corrected;
- run `36286017638` exposed that Android Activity recreation re-opened the auth gate and blocked read/recovery Home; production routing was corrected so the gate is offered only on a fresh MainActivity entry;
- final exact runtime run `36286296646` passed all required checks.

Superseded in-flight CI runs on earlier heads are not final evidence.

## External-state / reality boundary

- Supabase schema/RLS/RPC/Edge Functions: **unchanged**;
- Google Drive data/permissions: **unchanged by 12G implementation/testing**;
- Drive binding persistence/schema: **unchanged**;
- operator Samsung state: **not wiped or rebuilt for 12G**;
- final clean-install/new-user physical reality claim remains deliberately deferred to **Phase 12L** under the approved Phase 12 design;
- valid Phase 12H real-provider evidence remains reusable because 12G did not alter its binding semantics.

## Closeout

- classification remained **Level 2**;
- final diff review confirms runtime ownership is limited to Main/Auth navigation plus the small 12G navigation policy and tests;
- no Level 3 surface was introduced;
- no additional physical/provider gate is required for the claims made by this slice;
- PR #81 is the governed closeout line;
- Level 3 merge approval is **not applicable**;
- after governed merge, the next Phase 12 runtime slice is **12I — App Status & Diagnostics**.
