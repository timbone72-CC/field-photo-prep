# Phase 13 — Home Address Lifecycle & Cleanup — Build State

Date: 2026-09-29  
Status: **LEVEL 3 — DESIGN APPROVED / READY FOR IMPLEMENTATION**  
Authoritative branch: `phase-13/home-address-lifecycle-cleanup`  
PR: pending at record creation  
Governed base / rollback: `6d3b77f527ae58c3e3dd026887644ee3c0cb6557`  
Base meaning: current governed `main`, including merged PR #92 Clear & Reuse photo-only cleanup.

## Goal

Keep Home useful as the number of known properties grows while preserving the app's lean field workflow.

The operator should normally see current/active properties, be able to find older properties quickly, archive inactive properties, reactivate them when work returns, manually remove junk/test/incorrect addresses from FPP's normal lists, and avoid retaining old Drive photos indefinitely.

FPP remains a capture/preparation/delivery tool, not a permanent photo library or general Drive file manager.

## Approved user-facing behavior

### Home

- Home shows active properties only.
- Active properties sort by most recent real work activity.
- A compact address search is available on Home.
- Search includes active and archived properties.
- A quiet `Last used` value may be shown to support cleanup decisions.
- Searching, opening, refreshing, reviewing, archiving, or reactivating a property does not count as real work activity and must not refresh its inactivity age.

### Archive

- Archive is the normal cleanup path.
- Archived properties leave the normal Home list.
- Archived properties remain searchable and clearly identified as archived.
- Reactivate returns the exact existing property to Home.
- Archive preserves the company/address provider identity and lightweight lifecycle/history state needed for safe reuse.
- Archive does not rename, move, recreate, or delete the Drive address/work-order folder structure.

### Manual Delete Address

- A secondary manual Delete Address action exists for junk/test/incorrect addresses.
- It is deliberately less prominent than Archive.
- Delete removes/suppresses the address from normal FPP Home/search state on the current installation after the same cleanup safety checks.
- Delete is not authority for recursive Drive folder deletion.
- If the operator later deliberately chooses/adds that exact existing Drive address, FPP may clear the local suppression and reuse the same provider identity rather than create a duplicate.
- Do not label the action as remote permanent Drive deletion; confirmation must state that Drive folder structure/non-photo content is preserved.

### Photo cleanup

- Archive/Delete may remove old Drive photos for the affected property after explicit operator confirmation and all safety checks pass.
- Reuse the already-proven PR #92 photo definition: only direct child files whose provider MIME type identifies them as images/photos are deletion targets.
- Apply that definition only to direct work-order folders under the exact target address.
- Preserve address folders, work-order folders, child folders, and non-image files.
- No general Drive cleanup/file-manager behavior is introduced.

### Inactivity suggestions

Global setting: **Suggest archive after**

Choices:
- 30 days
- 60 days
- 90 days
- 6 months
- 1 year
- Never

Default: **90 days**.

The threshold is suggestion-only. It never performs archive or deletion automatically.

Home may expose a compact `Addresses ready for review` entry. Avoid recurring pop-up nagging.

## Phase checkpoints

### 13A — Home scalability

Implement active-only Home presentation, real-work sorting, `Last used`, and active+archived search without destructive Drive behavior.

### 13B — Archive, reactivate, manual delete

Add device-local lifecycle state and the Archive / Reactivate / Delete Address UI behavior.

### 13C — Safe archive/delete photo cleanup

Connect explicit Archive/Delete confirmation to address-scoped photo-only Drive cleanup with exact provider identity, freshness, unresolved-work guards, snapshot revalidation, and fail-closed partial-result behavior.

### 13D — Adjustable inactivity review

Add the global age threshold and small review surface. No automatic destructive action.

### 13E — Verification / reality gate

Complete focused automated coverage, final full CI, and the smallest real Samsung/Google Drive fixture gate required to prove the changed external behavior.

## Change classification

**Level 3.**

Reasons:
- new persisted lifecycle format/state;
- device/provider-bound identity;
- deliberate Drive photo deletion;
- destructive cleanup across multiple work-order folders;
- safety coupling to protected/unresolved local photo state.

Explicit operator design approval to implement this scope: **GRANTED 2026-09-29**.

Explicit Level 3 merge approval: **NOT YET GRANTED**. Obtain only after final required evidence passes on the exact final runtime head.

## Required rule packs / authorities

Read and preserve:
- `AGENTS.md`
- `GOVERNANCE.md`
- `PROJECT_PROFILE.md`
- `RULE_INDEX.md`
- `CHANGE_CONTROL_CONTRACT.md`
- `CONTRACT.md`
- `TESTING_CONTRACT.md`
- `INTEGRATION_CONTRACT.md`
- `REGRESSION_CHECKLIST.md`
- `rules/testing/DRIVE_PROVIDER.md`
- `rules/testing/UPLOAD_QUEUE_RETRY.md`
- `docs/PHASE_STAGING_DOCTRINE.md`
- `docs/PHASE_10F_EXPLICIT_BACKUP_RESTORE_RULES_2026-09-17.md`
- `docs/CLEAR_REUSE_SIMPLE_WORKFLOW_2026-09-29.md`

If implementation expands into another routed surface, load that rule pack and reclassify before continuing.

## Planned ownership

Keep one owner for each responsibility.

- Existing Home/MainActivity/PropertyListAdapter path: display, search request, menu actions, navigation.
- New narrow `PropertyLifecycleStore` (planned): device-local property lifecycle metadata only.
- `DriveClient`: provider enumeration, MIME classification, exact document operations, freshness.
- `PendingPhotoStore`: authoritative protected/unresolved local photo records and queue safety evidence.
- Existing authorization guard: permission to perform Drive mutation.
- Existing FolderPrefs: current workspace/company/address/work-order selection only; do not overload it with lifecycle history.

Do not introduce Room, a database framework, Supabase property storage, a second queue, a second Drive client, or a second workflow state machine.

## Persisted lifecycle data

Minimum required data per property:
- exact company provider document ID;
- exact address provider document ID;
- lifecycle state: `ACTIVE`, `ARCHIVED`, or local `DELETED/SUPPRESSED`;
- last real-work activity time.

Optional only if implementation proves it materially useful:
- archive timestamp;
- last known display name as non-authoritative UI fallback.

Do not use visible address text as identity.

The lifecycle store is provider-context-bound local state. It must remain excluded from cloud backup and device-to-device transfer under the existing Phase 10F all-app-owned-data exclusion. No portability claim is allowed.

No migration of existing addresses is required beyond safe default interpretation:
- existing discovered addresses with no lifecycle record are `ACTIVE`;
- their initial `Last used` state may be unknown until real activity is observed;
- unknown must not be fabricated as a recent date.

## Real-work timestamp rule

`Last used` must represent real field-work activity, not UI browsing.

Implementation must update the timestamp only from an existing authoritative work action under the exact address, such as a confirmed create/reuse work occurrence or successful photo capture acceptance.

Do not update it merely because the operator:
- opens the address;
- searches for it;
- refreshes Drive;
- reviews archive candidates;
- archives/reactivates it;
- changes company and returns.

Prefer the smallest existing authoritative work event rather than adding a parallel activity state machine.

## Archive/Delete preflight

Before any remote deletion:

1. Resolve the exact current workspace tree, company provider ID, and address provider ID.
2. Require current Drive mutation authorization and persisted write permission.
3. Obtain authoritative/fresh direct work-order folder state for the target address.
4. For every direct work-order folder, obtain authoritative/fresh direct-child state and collect only image/photo document IDs.
5. Query authoritative local queue/protected-photo state for all affected work-order provider IDs.
6. If any affected local record is `CAPTURING`, `WAITING`, `UPLOADING`, `FAILED`, `UNCERTAIN`, unreadable/corrupt in a way that prevents proving safety, or otherwise protected/unresolved: **block cleanup before Drive deletion**.
7. Show one clear confirmation summarizing the exact property, work-order count, photo count, and that folders/non-photo items are kept.
8. On confirmation, re-read the direct work-order identity set and each approved direct-photo identity set.
9. If any set changed, stop before deletion and require review again.

Archive/Delete may reuse existing Clear & Reuse photo MIME classification and identity-set comparison logic, but it must not call Clear & Reuse's rename/sequence-reset behavior.

## Remote mutation / failure behavior

- Delete only approved direct photo IDs.
- Never delete address/work-order/child folders or non-photo items.
- Execute deterministically and boundedly; do not create parallel destructive writes merely for speed.
- After each work-order cleanup, re-read provider state as needed to prove the approved photos are gone.
- If deletion fails after some photos were removed, record/report the partial count, stop later affected deletions, set the affected destructive path blocked until refresh/inspection, and do not claim archive/delete complete.
- Never blind-retry an ambiguous remote deletion.
- Local lifecycle state must not be committed to final ARCHIVED/DELETED state until the required cleanup result is proven complete.
- Cancelling confirmation changes nothing.

## Search / Home behavior

- Home rendering filters lifecycle state after authoritative company/address discovery; it does not invent a second Drive directory.
- Search is local over the currently discovered exact property set plus lifecycle metadata.
- Archived search results may be opened/reactivated.
- Deleted/suppressed results are not part of normal search.
- Sorting uses known real-work time. Addresses with unknown activity must sort predictably without pretending they were recently worked.

## External systems

Runtime Phase 13 touches:
- Android local app-private lifecycle state;
- Android SAF/DocumentsProvider;
- Google Drive photos in disposable/user-confirmed target address work-order folders.

It does **not** change:
- Supabase schema/config;
- Auth/session behavior;
- Organization/Membership;
- Drive sharing permissions;
- workspace/company binding;
- camera behavior;
- upload destination construction;
- signing/release identity.

No live external state has been changed by the design-recording work.

## Safe Drive fixture plan

Use a disposable company/address fixture, never a live customer/job folder.

Positive fixture:
- one disposable address;
- at least two direct dated work-order folders;
- FPP-uploaded photos across those work orders;
- at least one preserved non-photo item;
- optionally one child folder if needed to prove preservation.

Positive physical gate:
1. confirm property is active and searchable;
2. create/observe real work activity so `Last used` is known;
3. archive;
4. confirm the displayed preflight counts are correct;
5. confirm only approved Drive photos are removed;
6. confirm address/work-order folders and non-photo content remain;
7. confirm property leaves Home;
8. search finds it as archived;
9. reactivate;
10. confirm the same address provider identity is reused and no duplicate/rename/move occurred.

Negative physical gate:
1. create one disposable affected local photo in a safe unresolved/protected state;
2. attempt archive/delete cleanup;
3. prove cleanup blocks before Drive deletion;
4. confirm remote photos remain unchanged.

Do not manufacture an unsafe ambiguous provider result solely for testing. Use automated fail-closed coverage for deterministic partial/uncertain paths when a safe real-provider trigger does not exist.

## Automated verification boundary

Focused tests must cover, as applicable:
- lifecycle persistence and exact company/address provider keying;
- default ACTIVE behavior for previously known addresses without records;
- active/archive/delete filtering;
- active+archived search;
- deleted/suppressed exclusion;
- reactivate preserves exact provider identity;
- deliberate re-add of exact suppressed address reuses exact provider identity;
- real-work timestamp updates and no-op browsing cases;
- deterministic sort including unknown activity;
- threshold calculations and Never;
- no automatic destructive action from inactivity review;
- multi-work-order preflight aggregation;
- unresolved/protected local-state block;
- changed work-order set before confirmation execution;
- changed photo set before deletion;
- photo-only MIME filtering;
- folder/non-photo preservation;
- partial remote deletion fail-closed behavior;
- no queued-photo destination rewrite;
- backup rules continue excluding lifecycle state.

Final runtime gate:
- focused tests pass;
- complete repository/Android CI passes once on the exact final runtime head;
- required safe Samsung/Google Drive reality gates pass;
- explicit operator Level 3 merge approval is then obtained.

## Protected behavior

Must remain unchanged unless separately approved:
- Home → Work Orders → Photos normal field workflow;
- company switching and exact company/provider binding;
- address/work-order discovery and duplicate prevention;
- queued photo immutable destination;
- capture/protected-original semantics;
- upload/retry/reconciliation;
- Clear & Reuse workflow and its sequence reset;
- Drive sharing;
- Supabase/Auth/Organization boundaries;
- existing backup/restore exclusions.

## Rollback

Rollback point: `6d3b77f527ae58c3e3dd026887644ee3c0cb6557`.

Before merge, the implementation must identify any new lifecycle preference/file name and exact revert behavior.

Rollback of runtime code must:
- never delete protected local photos;
- never rewrite queued destinations;
- never attempt to restore Drive photos already explicitly deleted by a completed archive operation;
- preserve/report incomplete destructive state rather than pretending rollback can recreate removed remote photos.

Because remote deletion is not reversible by source rollback, the preflight/confirmation/revalidation gate is mandatory.

## Current progress

Completed:
- current `main` inspected;
- no open PR or existing Phase 13 branch owned this scope;
- Phase 13 authoritative branch created from `6d3b77f527ae58c3e3dd026887644ee3c0cb6557`;
- roadmap records Phase 13A–13E;
- product, integration, and regression rules reconciled to authorize the approved address-scoped photo-cleanup behavior;
- existing PR #92 Clear & Reuse photo-only rules selected as the photo classification/deletion baseline;
- Phase 10F non-portable backup boundary confirmed.

Not started:
- runtime implementation;
- runtime tests;
- external/Drive mutation for Phase 13;
- Phase 13 physical reality gate.

## Next checkpoint

Begin **Phase 13A — Home scalability** on this authoritative branch.

Build the largest coherent safe batch that can be proven without a physical Drive mutation. Do not stop for a phone test until the next genuine external/destructive boundary under the staging doctrine.
