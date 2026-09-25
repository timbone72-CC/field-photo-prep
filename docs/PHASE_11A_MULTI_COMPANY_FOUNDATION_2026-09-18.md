# Phase 11A Multi-Company Foundation Impact Record — 2026-09-18

Status: **COMPLETE — Level 3**

## User-facing problem

Field Photo Prep currently persists one approved Drive master tree, so the operator must replace the active Drive folder to move between clients. The business now has more than one client/company (including HNP and Tresmolino), and the operator needs to add, select, and edit companies from inside FPP without mixing one company's properties or photo destinations with another.

## Approved behavior

The field hierarchy becomes:

`approved field-work workspace → company → address → dated work order → photos`

The app may:
- persist one operator-approved Android SAF tree grant for the common field-work workspace;
- discover company folders directly under that workspace;
- select one company at a time;
- create a company folder under the exact workspace after fresh/settled absence checking;
- rename the exact selected company folder after collision checking;
- switch companies without altering already-queued photos;
- continue discovering/creating address folders only under the exact selected company;
- keep queued-photo upload destinations bound to their already-stored work-order provider document IDs.

The app will **not** delete companies, move companies between parents, alter sharing, create public links, or add another upload implementation.

## Current real Drive structure

The operator's current common container is `Photos`, with direct company folders including `HNP Jobs` and `Tresmolino Jobs`. This live structure is context only and is **not** a development/test fixture.

## Change classification

Level 3 because this changes:
- persisted Drive-tree selection semantics;
- hierarchy and parent identity;
- company-folder creation/rename behavior;
- restoration binding for selected address/work order;
- the tree URI used by existing upload/reconciliation code.

## Owning files

Expected runtime ownership:
- `FolderPrefs.java` — persisted workspace/company/address/work-order binding;
- `MainActivity.java` — workspace selection, company discovery/add/edit/switch, company-scoped property discovery;
- `DriveClient.java` — existing settled folder discovery/create/rename primitives only; no second write path;
- `screen_home_properties.xml` — company/workspace presentation if needed.

Expected focused tests:
- `FolderPrefsParentBindingInstrumentedTest.java`;
- `HomeDriveOptionsInstrumentedTest.java`;
- `DriveClientTest.java`;
- new company-specific focused tests where the existing seams are insufficient.

Contracts/checklists updated before runtime behavior:
- `CONTRACT.md`;
- `INTEGRATION_CONTRACT.md`;
- `TESTING_CONTRACT.md`;
- `REGRESSION_CHECKLIST.md`;
- `docs/ROADMAP.md`.

## Read surfaces

- persisted SAF workspace tree URI;
- workspace provider document ID/name;
- direct company-folder metadata under the workspace;
- selected company provider document ID/name;
- direct address folders under the selected company;
- direct work-order folders under the selected address;
- existing pending-photo records and their immutable work-order destinations.

## Write surfaces

- local SharedPreferences workspace/company selection state;
- company-folder create under the exact workspace;
- selected company-folder rename by exact provider identity;
- existing address/work-order create/reuse behavior under the selected company;
- existing photo uploads only through the existing upload coordinator/uploader.

## Required and optional data

Required:
- persisted workspace tree URI and workspace provider document ID;
- active company provider document ID before property create/discovery;
- existing exact address/work-order provider identities;
- queued photo destination work-order provider identities.

Optional/display:
- workspace/company/address/work-order visible names.

Visible names are never permanent destination identity.

## Migration

Existing installs may have legacy `master_tree_uri/master_folder_id/master_folder_name` pointing directly at a company such as HNP.

Migration is fail-closed:
1. legacy operation remains readable until the operator explicitly selects the common workspace;
2. selecting a workspace must not rewrite any queued photo destination ID;
3. if the legacy company provider ID is found as a direct child of the newly selected workspace, it may be selected by exact provider ID;
4. otherwise no company is guessed from its visible name;
5. current address/work-order navigation is cleared when the active company identity changes;
6. unconfirmed photos remain recoverable;
7. if the broader tree cannot access an old queued work-order provider ID, upload/reconciliation stops through existing failure/uncertainty behavior rather than redirecting the photo.

## Duplicate/idempotency rules

- Company create uses fresh/settled workspace child state.
- One exact company-name match is reused.
- Multiple exact matches require operator choice.
- No exact match may create exactly one company.
- Company rename is bound to the exact selected company provider ID.
- Rename is blocked when another company already has the requested exact visible name.
- Company switch never rewrites queued-photo destination IDs.

## Offline/stale-state behavior

Company discovery/create/rename requires usable persisted workspace access.
Absence/collision decisions fail closed while provider state is loading, stale, inconsistent, or cannot accept refresh.
Already captured photos remain local/recoverable under existing queue rules.

## Safe Drive fixture plan

Do not use HNP, Tresmolino, or another live client as the feature test surface.

Use a disposable workspace such as:
`FPP MULTI COMPANY TEST → TEST COMPANY A / TEST COMPANY B → TEST ADDRESS → TEST WORK ORDER`.

Prove:
- workspace selection/persistence;
- exact existing company discovery;
- add one company without duplicate;
- rename selected company without changing provider ID;
- switch companies and show only that company's addresses;
- address creation lands under the selected company, never workspace root or a sibling company;
- one prepared photo uploads to its stored test work-order ID;
- switching company before upload does not redirect it;
- restart restores workspace/company safely;
- unrelated test content is unchanged.

## Automated verification

During development, run focused company/workspace/persistence/identity tests.
Before merge, the complete repository suite must pass once on the exact final runtime head.
Real Android + Google Drive provider evidence is required before this Level 3 change is called merge-ready.

## Rollback

Rollback point: canonical main `8d252eb6ae8b680e89cf197d81bc3cf526a6d73b`.

If the new hierarchy misroutes or cannot resolve destinations:
- stop affected uploads;
- revert the Phase 11A runtime change;
- do not delete queued photos or protected originals;
- retain legacy master preferences for rollback compatibility until the new path is proven.

## Protected behavior

This change must not weaken:
- protected originals;
- prepared-copy behavior;
- immutable queued work-order destination;
- upload/retry/reconciliation semantics;
- upload idempotency;
- Clear & Reuse guards;
- photo cleanup rules;
- provider freshness fail-closed behavior;
- account/provider choice remaining in Android's system picker.

## Completion evidence

- final tested runtime head: `ed1990742d403f466151d9399a423a8fdaabbe69`
- Android CI run `35419341385`: PASS
- real Android + Google Drive provider gate: PASS
- company discovery/add/rename/switch: PASS
- selected-company property/work-order scoping: PASS
- queued-photo immutable destination across company switch: PASS
- real upload to original stored work-order while another company was active: PASS
- restart/workspace persistence: PASS
- transient work-order draft isolation after company/property change: PASS
- semantic success-banner spot-check: PASS
- PR #57 merged to main at `66aeed51fc7065f4f18e06677e3931daf4f3977f`
- post-merge live workspace verification proved HNP ↔ Tresmolino switching under the shared `Photos` workspace

The prior `IN PROGRESS` label was stale documentation and is corrected by the 2026-09-24 source-of-truth reconciliation.
