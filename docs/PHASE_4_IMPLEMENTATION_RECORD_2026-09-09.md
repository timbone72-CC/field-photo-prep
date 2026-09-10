# Phase 4 Implementation Record — Address Folder Creation

Date: 2026-09-09

## Goal

Create or reuse one property/address folder directly under the operator-approved master Drive folder without guessing identity or creating avoidable duplicates.

## Approved scope

Phase 4 only:

- keep the existing master-folder selection and address-folder discovery behavior;
- let the operator enter a desired address-folder name;
- before any create, request a fresh/settled listing of the approved master's direct child folders;
- exact case-sensitive one-name match → reuse that existing folder ID;
- multiple exact matches → create nothing and require the operator to choose the intended existing folder;
- no exact match → create exactly one folder directly under the approved master folder ID;
- verify the returned created folder identity against refreshed provider state before treating it as selected;
- persist the exact selected/created address document-provider ID and display name through the existing `FolderPrefs` address fields;
- opening a different address continues clearing stale work-order selection through the existing address identity rule;
- address folders are never renamed, recycled, cleared, or deleted by Phase 4.

Explicitly excluded:

- work-order Clear & Reuse from Phase 3B;
- address normalization/geocoding;
- silently converting spaces/case/abbreviations/underscores;
- duplicate address merging or deletion;
- camera/photo capture;
- photo preparation/upload;
- sharing/permission changes;
- workbook or Free Map Router integration.

## Governed base and rollback

Exact base / rollback commit:

`8467381b8dac628c6afbb7ea141ba94f0b298417`

Phase 4 branch:

`feat/phase-4-address-folder-creation`

Rollback is to the exact base above. Phase 4 introduces no migration or remote cleanup requirement.

## Change level

Level 3.

Reason: Phase 4 performs a new Drive write that creates a persistent destination folder and establishes address-folder identity used by later work-order/photo flows. A wrong parent, stale discovery result, or blind retry could create duplicate or misidentified destinations.

Explicit operator approval is required before merge after automated verification and the real-device Drive reality gate.

## Required and optional data

Required:

- persisted approved master tree URI;
- persisted master document-provider ID and display name;
- persisted read/write SAF permission for that tree;
- operator-entered address-folder name after outer-whitespace trimming.

Returned/persisted on success:

- exact address document-provider ID;
- exact address display name.

No new stored-data keys or schema version are introduced. Existing `FolderPrefs` address fields remain the persistence owner.

## Naming rule

Phase 4 does not invent address formatting.

- Trim outer whitespace.
- Reject blank names.
- Otherwise preserve the operator-entered text exactly.
- Matching is case-sensitive exact visible-name matching for discovery only.
- Once selected/created, the returned provider document ID is authoritative identity.

This intentionally avoids converting existing underscore-style folders or guessing postal normalization.

## Read/write surfaces

Reads:

- persisted master tree URI/identity and SAF permissions;
- direct child folders of the approved master;
- exact provider folder IDs and display names.

Writes:

- at most one new directory directly under the approved master for a no-match request;
- existing local current-address ID/name fields after verified selection/creation;
- existing current-work-order fields may be cleared by `FolderPrefs.setCurrentAddress(...)` when the address identity changes, as already governed.

No existing Drive item is renamed, moved, deleted, or permission-modified.

## Duplicate/idempotency behavior

Before create:

1. Re-read the approved master's direct folder children through the fresh/settled provider path.
2. Find case-sensitive exact name matches.
3. One match → reuse it; no create call.
4. More than one match → create nothing; expose duplicate identity suffixes and require operator selection.
5. No match → issue one create call under the exact master document ID.

After create:

1. retain the returned document ID;
2. refresh/re-read the master children;
3. verify the returned ID is present with the requested name;
4. if the requested name is now ambiguous, do not guess which same-named folder the operator intended; require operator choice;
5. only a verified unambiguous result is persisted/opened automatically.

Any create exception or unverifiable/ambiguous result blocks another address create attempt until the operator refreshes actual address folders. Restart or repeated taps are not permission to blindly create another folder.

## Stale provider behavior

Phase 3B real-device testing proved that Android's Google Drive document provider can temporarily return stale child listings. Phase 4 therefore must not use a single cached child query as proof that an address does not exist.

The Phase 4 create path will request provider refresh, reject `DocumentsContract.EXTRA_LOADING`, and require matching settled folder snapshots before deciding there is no exact match. If fresh provider state cannot be confirmed, create stops with no Drive write.

Normal read-only address browsing may remain a lightweight listing; the stronger freshness requirement is mandatory at the create boundary.

## Safe Drive fixture / reality gate

Use only the existing safe master:

`HNP Jobs`

Never use a live customer/job folder as a create test.

Suggested disposable name for the no-match create path:

`FIELD PHOTO PREP ADDRESS CREATE TEST`

Real-device gate when an Android device is available:

1. Confirm master shows `HNP Jobs` and real address discovery works.
2. Enter existing `FIELD PHOTO PREP TEST`; prove exactly one existing match is reused and no duplicate is created.
3. Enter `FIELD PHOTO PREP ADDRESS CREATE TEST`; prove exactly one folder is created directly under `HNP Jobs`.
4. Inspect Drive and confirm exact parent/type/name.
5. Repeat the same request; prove the existing folder is reused rather than duplicated.
6. Restart the app, refresh, repeat the request, and prove restart does not create another folder.
7. Confirm the selected address provider ID is retained and work-order discovery opens under that exact address.
8. If safe duplicate-name fixtures can be created manually, prove multiple exact address matches require operator choice and do not trigger a create. If that fixture is not safe/practical, document the limitation and do not overclaim real-device coverage.
9. Confirm unrelated address/work-order content is unchanged.
10. After the gate, disposable test address folders may be cleaned up manually in Drive; Phase 4 itself must not add address deletion.

## Automated coverage plan

Focused tests must cover:

- address-name trim/blank validation;
- exact case-sensitive address matching;
- one-match reuse decision;
- duplicate exact-name detection remains multiple for operator choice;
- folder identity lookup by provider ID;
- fresh-folder snapshot comparison including changed names/IDs/order;
- provider freshness authority requirements;
- existing work-order naming/identity tests remain passing.

Final runtime head must pass the complete repository suite, debug build, and Android launch smoke test in CI.

## Failure recovery

- No fresh/settled master listing → stop before create.
- Read-only/lost permission → stop before create.
- Create throws or returns no usable ID → block another create until refresh/inspection.
- Returned ID cannot be verified under the master → block another create until refresh/inspection.
- Same-name ambiguity appears after create → do not guess; show actual folders for operator choice.
- No cleanup/delete is attempted automatically after uncertain create because doing so could remove the wrong same-named address folder.

## Merge status

Implementation authorized by the user's instruction to continue Phase 4 work.

Pre-merge status: **not approved**. Because this is Level 3, merge remains blocked until the automated gate passes, the real Android/Drive gate is completed, and the operator explicitly approves the merge.
