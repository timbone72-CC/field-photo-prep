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

- `MainActivity.onResume()` now routes `SIGN_IN_REQUIRED` through the existing `AuthActivity` before Drive/Home resume work continues;
- the same routing check runs again after the existing serialized account revalidation, so a stale stored session that becomes `SIGN_IN_REQUIRED` cannot remain in ordinary Home flow;
- `AuthActivity` receives a narrow required-gate intent marker only for this launcher route;
- after that required route reaches authoritative `VALIDATED` identity, `AuthActivity` finishes and returns to the existing `MainActivity`;
- non-sign-in blocked/degraded states remain owned by the existing authorization/recovery behavior and are not redefined by 12G;
- no Drive binding, `FolderPrefs`, Supabase, queue, photo, or provider identity semantics changed.

Focused automated coverage:
- no decision / `SIGN_IN_REQUIRED` requires the existing auth route;
- `VALIDATED` completes the required gate;
- GRACE, RECHECK_REQUIRED, NO_MEMBERSHIP, REVOKED, and DRIVE_DISCONNECTED do not create a second 12G recovery state machine.

Verification: **PENDING CI** on the exact runtime head.

### Next checkpoint after this slice passes

Inspect and implement only the next 12G transition:

`VALIDATED identity + NO_WORKSPACE → existing deliberate Connect Drive action`

Do not change 12H binding persistence or SAF/provider semantics.

## Exact next checkpoint

After the required-authentication slice passes, continue with only:

`VALIDATED identity + NO_WORKSPACE → existing deliberate Connect Drive action`

Use the existing 12H binding guard and existing SAF picker. Do not add new persistence or provider-selection semantics.

## Verification status

- documentation/preflight diff: PASS — exactly five intended documentation files changed from governed main;
- runtime focused tests: not run — no runtime code changed;
- complete Android CI: not required for the preflight documentation commit except the repository's normal Markdown-only CI path;
- physical Samsung/provider gate: not required for this checkpoint;
- Level 3 merge approval: not applicable while scope remains Level 2.
