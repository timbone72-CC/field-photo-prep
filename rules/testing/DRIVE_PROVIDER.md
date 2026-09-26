# Testing Rule Pack — Drive Provider, Workspace & Folder Identity

## Applies when

Read this file when work touches:
- Android SAF/DocumentsProvider behavior;
- workspace/company/address/work-order discovery or creation;
- provider freshness/loading;
- provider folder identity;
- company switching or rename;
- absence/emptiness decisions before Drive writes;
- real Google Drive validation.

Use together with `TESTING_CONTRACT.md`, `INTEGRATION_CONTRACT.md`, and the applicable Drive/product rules in `CONTRACT.md`.

## Realistic Drive tests

Drive-related unit tests may mock provider/API responses while developing, but a Level 3 Drive change is not fully verified from mocks alone.

Use the affected parts of the safe SAF/DocumentsProvider reality gate in `INTEGRATION_CONTRACT.md`.

The real path must prove, as applicable:
- the operator-selected workspace tree;
- actual company/address/work-order provider identities;
- the changed create/rename/switch/reuse/upload operation;
- returned or preserved remote identity;
- preservation of unrelated Drive content.

Never use a live customer/job folder when a dedicated test folder can prove the behavior.

Synthetic IDs remain useful unit coverage only; they are not real-provider evidence.

## Multi-company regression boundary

For any workspace/company change, focused coverage must prove at least:

- legacy single-company preferences remain readable until explicit workspace migration;
- selecting a workspace does not rewrite queued photo destination IDs;
- an exact legacy company provider ID may be restored only by ID when it is a direct workspace child;
- company switching clears current address/work-order navigation state but preserves queued-photo records;
- company create reuses one exact match, requires operator choice for duplicates, and creates exactly one folder only after authoritative-enough absence;
- company rename preserves provider identity and is blocked on an exact sibling-name collision;
- address discovery/creation uses the exact selected company provider ID, never the workspace root or a sibling company;
- restart restores the workspace and selected company without inventing a new folder; and
- upload/reconciliation continues to use each photo's immutable stored work-order provider ID regardless of later company switching.

Fake providers may cover focused logic, but broader-tree Android provider behavior still requires the safe real-device Drive gate before merge approval when that behavior changes.

## Provider freshness boundary

When a change relies on cloud-backed folder absence or emptiness before a write:

- automated tests must cover loading/stale/inconsistent provider state where practical;
- implementation must fail closed when state is not authoritative enough for the decision;
- a real-device Drive gate must exercise the actual Android provider path for Level 3 folder create/reuse/deletion behavior;
- emulator or mocked provider state must not be represented as proof of Google Drive provider freshness.

## Provider evidence rule

Record what the evidence actually proves.

Distinguish:
- fake/mock provider behavior;
- emulator/provider simulation;
- real Android DocumentsProvider behavior;
- real Google Drive content/result.

Do not upgrade one category into another merely because the test passed.
