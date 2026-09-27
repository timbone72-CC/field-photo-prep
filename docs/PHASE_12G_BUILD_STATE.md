# Phase 12G Build State

Date: 2026-09-26

Status: **PRE-IMPLEMENTATION — AUTHORITATIVE LINE ESTABLISHED**

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

### Next checkpoint after this slice passes

`VALIDATED identity + NO_WORKSPACE → existing deliberate Connect Drive action`

Do not change 12H binding persistence or SAF/provider semantics.

## Verification status

- documentation/preflight diff: PASS — exactly five intended documentation files changed from governed main;
- runtime focused tests: not run — no runtime code changed;
- complete Android CI: not required for the preflight documentation commit except the repository's normal Markdown-only CI path;
- physical Samsung/provider gate: not required for this checkpoint;
- Level 3 merge approval: not applicable while scope remains Level 2.
