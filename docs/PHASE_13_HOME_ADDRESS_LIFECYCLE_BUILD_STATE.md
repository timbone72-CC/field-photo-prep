# Phase 13 — Home Address Lifecycle & Cleanup — Build State

Date: 2026-09-29  
Status: **LEVEL 3 — PHASE 13A–13E IMPLEMENTED; FINAL AUTOMATED AND PHYSICAL BEHAVIOR GATES PASS / TEST-FIXTURE CLOSEOUT AND EXPLICIT MERGE APPROVAL PENDING**  
Authoritative branch: `phase-13/home-address-lifecycle-cleanup`  
PR: #93 `Phase 13: Home address lifecycle and cleanup` (draft)  
Governed base / rollback: `6d3b77f527ae58c3e3dd026887644ee3c0cb6557`  
Base meaning: current governed `main`, including merged PR #92 Clear & Reuse photo-only cleanup.
Final validated Phase 13 runtime head: `731a231695436da9f355cafaa6c9922d814e82ed`.
Android CI run #1139 / `36702591737`: **PASS** on that exact runtime head.
Physically installed Samsung internal build: `0.28.5-internal` / versionCode `42`.
The historical Phase 13D and intermediate Phase 13E snapshots below remain as evidence of earlier checkpoints; this header and the final Phase 13E acceptance record at the end govern the current status.

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

Archive/Delete may reuse the existing photo MIME classification and identity-set comparison logic. Unlike Clear & Reuse, a successfully completed Archive/Delete also retires old confirmed local photo history for the exact verified dated work-order IDs and independently starts a new capture-order occurrence at 001; it never renames a Drive folder. Local retirement/sequence reset occurs only after complete remote zero-photo verification, and any local I/O failure blocks final lifecycle state.

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

Phase 13A completed on 2026-09-29:
- Home now maintains one authoritative discovered-property set plus a derived visible Home list;
- normal Home shows ACTIVE properties only;
- address search is available directly on Home;
- search can include ACTIVE and ARCHIVED properties while DELETED/SUPPRESSED stays out of normal results;
- Home sorting uses persisted real-work activity, newest first, with unknown activity sorted predictably by address;
- successful protected photo capture records real property work activity;
- browsing/searching/refreshing does not update `Last used`;
- rows show a quiet `Last used MMM d` line only when real activity is known;
- archived search results are prevented from opening until Phase 13B Reactivate exists;
- lifecycle metadata is stored in the app-private `property_lifecycle` SharedPreferences file and remains covered by the existing all-shared-preferences backup/transfer exclusion;
- existing screenshot/navigation fixtures were updated to use the new authoritative discovered-property path rather than mutating the derived display list;
- focused policy/persistence/Home search coverage added.

Phase 13A verification:
- Android CI run `36659787068` / run #1093: **PASS** on exact runtime head `81ed9a39419ffba69888b940582a9beb1cf83b1a`;
- unit tests and production identity checks: PASS;
- internal debug build: PASS;
- production release fail-closed check: PASS;
- stable test APK signer and APK evidence verification: PASS;
- full connected Android instrumentation: PASS;
- rendered light/dark/large-font verification and internal launch smoke: PASS;
- internal APK and rendered evidence artifacts uploaded by CI.

A prior Phase 13A run correctly failed because legacy test fixtures bypassed the new Home list owner and one rendered-row expectation predated the search field. Those fixtures were corrected; runtime safety was not weakened.

Phase 13B completed as a safe staged checkpoint on 2026-09-29:
- each Home property row now uses one compact overflow affordance instead of adding another visible action button;
- ACTIVE property options expose **Archive** and secondary **Delete Address**;
- ARCHIVED search results expose **Reactivate** and secondary **Delete Address**;
- Reactivate changes only device-local lifecycle state, preserves `Last used`, and does not rename/move/recreate any Drive folder;
- deliberately choosing/adding an exact ARCHIVED or DELETED/SUPPRESSED Drive address prompts to restore it and reuses the exact same address provider ID;
- lifecycle APIs now distinguish `reactivate`, `archiveAfterCleanupProven`, and `deleteAfterCleanupProven`;
- Archive/Delete confirmation copy states the final intended cleanup boundary: old FPP photos removed, Drive folder structure and non-photo items retained;
- this intermediate checkpoint intentionally blocks final ARCHIVED/DELETED state after confirmation because Phase 13C photo cleanup has not yet been proven;
- no Phase 13B Drive deletion, rename, folder creation, folder removal, or sharing mutation exists.

Phase 13B verification:
- initial CI run #1102 correctly failed at compile because the new lifecycle methods were inserted inside `buildHomeUi()`; the methods were moved to class scope with no behavior change;
- Android CI run `36662771353` / run #1103: **PASS** on exact runtime head `b88f15142626d1b0697cd8387620cbbd359b2f01`;
- unit tests and production identity checks: PASS;
- internal debug build and production fail-closed/signing/APK identity checks: PASS;
- full connected Android instrumentation: PASS;
- focused lifecycle tests prove Reactivate preserves `Last used`, Archive/Delete remain fail-closed before 13C, and exact removed-address re-add reuses the same provider ID;
- rendered-screen variants and internal launch smoke: PASS;
- internal APK and rendered evidence artifacts uploaded by CI.

Phase 13C completed on 2026-09-29:
- Archive/Delete now resolve the exact current company/address provider identity from fresh Drive state before cleanup;
- the cleanup plan snapshots only direct dated work-order folders under that exact address and classifies only direct child `image/*` documents as photo deletion targets;
- child folders and non-image files are preserved and are never cleanup targets;
- `PendingPhotoStore.requireAddressCleanupSafe` blocks cleanup when the affected address has unresolved/protected local photo state and fails closed when unreadable metadata prevents proving ownership;
- the confirmation summarizes the exact property, work-order count, photo count, and preserved non-photo count before any deletion;
- after confirmation the app revalidates the exact address, work-order identity set, and approved direct-photo identity set before mutation;
- persisted SAF read/write access and runtime Drive-mutation authorization are rechecked immediately before deletion;
- cleanup executes deterministically one work order at a time: remove only the approved photos for that work order, obtain fresh provider state, prove that work order has zero direct photos, then continue to the next work order;
- if deletion or post-delete verification fails, later work orders are not touched, the removed count is reported, the address cleanup path is blocked until refresh/inspection, and lifecycle state is not committed;
- final `ARCHIVED` / `DELETED` state is committed only after all approved photos are removed, every affected work order verifies photo-empty, the work-order folder set still matches, and the original address provider identity/name is still confirmed;
- Archive/Delete do not rename, move, recreate, or remove address/work-order folders and do not alter non-photo content.

Phase 13C verification:
- implementation review found an initial sequencing defect where all work orders could be deleted before post-delete verification; this was corrected before accepting the checkpoint so each work order is verified before any later work order can be mutated;
- focused mutation coverage now proves the call order `delete work-1 photos → verify work-1 → delete work-2 photos → verify work-2` and proves a failed first-work-order verification prevents later-work-order deletion;
- Android CI run #1120 / `36664611383` correctly failed only because an instrumentation assertion expected one specific access-rejection message even though the app had already failed closed at an earlier valid Drive authority guard;
- that test was broadened to accept the existing valid Drive authority/access guard without weakening runtime behavior;
- Android CI run #1121 / `36664624482`: **PASS** on branch head `c11d5aea78a695d2043c0f5bef93b08f05d422cd`, containing runtime implementation head `14ed8883d7527aa13186d1149c96c5e786fae4d9`;
- unit tests and production identity checks: PASS;
- internal debug build: PASS;
- production release fail-closed, stable signer, and APK identity/evidence checks: PASS;
- full connected Android instrumentation: PASS;
- rendered verification and internal launch smoke: PASS;
- internal APK and rendered/test evidence artifacts uploaded by CI;
- internal APK artifact: `field-photo-prep-internal-apk`, artifact id `11075932150`, SHA-256 `9417eb205336a49bbc008d5b13561a29c71f84c8a34ba81558b92b551a4acdf4`;
- rendered/test evidence artifact id `11075966941`, SHA-256 `da0ba134fcd8338eda748ccd2f9d8eace27e4b54c335e82aafc1db9ff2130fd2`;
- no Phase 13 real-provider Drive photo deletion has been performed yet; that external behavior remains reserved for the Phase 13E disposable Samsung/Google Drive reality gate.

Phase 13D completed on 2026-09-29:
- added one app-global `Suggest archive after` preference with the approved choices 30 days, 60 days, 90 days, 6 months, 1 year, and Never;
- default remains 90 days and invalid/missing persisted values fall back safely to 90 days;
- the setting is stored only in app-private `property_archive_review` SharedPreferences and is not company-specific;
- Android cloud backup and device-transfer rules were rechecked and continue excluding all SharedPreferences, so this setting remains device-local/non-portable;
- candidate calculation considers only ACTIVE properties with a known real-work `Last used` timestamp; unknown activity is not fabricated into an inactivity age;
- 30/60/90-day thresholds use exact day cutoffs, while 6 months and 1 year use calendar month/year cutoffs;
- Never returns no review candidates;
- Home exposes one compact review entry only when the current company has addresses actually due for review;
- tapping the review entry filters the existing Home property list to those candidates and preserves the existing per-address Archive/Delete controls; tapping Show all or typing a search leaves review-only mode;
- the setting is exposed as `Suggest Archive After` in the existing App & Drive menu rather than adding a new settings screen;
- review mode has no code path that invokes archive/delete cleanup, performs Drive mutation, or changes lifecycle state automatically;
- no per-company timers, tags, favorites, pinning, analytics, or storage dashboard were introduced;
- a min-SDK compatibility review caught and removed newly introduced runtime `List.of()` usage so Phase 13D does not raise the app's Android API assumptions.

Phase 13D verification:
- focused unit coverage proves 90-day candidate selection, known-vs-unknown activity behavior, exact 6-month calendar cutoff, and Never;
- instrumentation coverage proves the global preference defaults to 90 days and persists a changed selection;
- App & Drive menu policy tests include the setting in both usable and authorization-blocked states without exposing stale provider/company actions;
- Android CI run #1131 / `36666095641`: **PASS** on exact runtime head `e79219684a3b7176e9807bb747e0380dc910dc4f`;
- unit tests and production identity checks: PASS;
- internal debug build: PASS;
- production release fail-closed, stable signer, and APK evidence checks: PASS;
- full connected Android instrumentation and internal launch smoke: PASS;
- rendered verification: PASS;
- internal APK artifact id `11076352100`, SHA-256 `5dc76ee00f0604574c19d1ec50268b747eb4d525f24eb391562b62e145a26d51`;
- rendered/test evidence artifact id `11076213815`, SHA-256 `2f294635b948d339507a0583e1bc05bbd8d44c1dac7cb4a1f3a5cc9bacc53f21`;
- no Phase 13D external/Drive mutation was performed.

Rollback note for Phase 13D:
- the new preference file is `property_archive_review`;
- source rollback may safely leave this app-private preference unused; it has no provider identity or Drive side effect;
- uninstall/clear-app-data removes it, and existing backup/transfer rules prevent it from migrating to another installation.

Completed:
- current `main` inspected;
- no open PR or existing Phase 13 branch owned this scope;
- Phase 13 authoritative branch created from `6d3b77f527ae58c3e3dd026887644ee3c0cb6557`;
- roadmap records Phase 13A–13E;
- product, integration, and regression rules reconciled to authorize the approved address-scoped photo-cleanup behavior;
- existing PR #92 Clear & Reuse photo-only rules selected as the photo classification/deletion baseline;
- Phase 10F non-portable backup boundary confirmed.

Historical checkpoint at Phase 13D: the disposable Samsung/Google Drive reality gate had not yet begun. Its completed evidence and remaining closeout obligations are recorded in the final Phase 13E acceptance section below.

## Phase 13E archive local-history/numbering defect discovered on physical Samsung

After the real-provider positive gate, Reactivate correctly reused the original address and two dated work-order folders, but Photos still displayed the old two confirmed-upload rows from Test B. Those images had been independently proven deleted from Drive. Their retained local metadata and per-work-order capture ledger caused the new test photos to retain old sequence positions instead of starting at 001. The operator rejected this behavior on 2026-09-30 and approved actual completed-archive retirement rather than displaying lightweight prior-archive history.

Scope: on this same authoritative Phase 13 branch/PR #93, retire *only* confirmed old metadata and any remaining local copies bound to the exact verified dated work-order IDs after fresh remote zero-photo proof; reset their capture sequence as a new occurrence; keep the original provider IDs/folders/non-photo files, preserve unrelated local records, and block on any unresolved/corrupt local metadata. A failed local cleanup must not claim Archive succeeded or commit a partial ledger reset. This is an explicit revision to the earlier Phase 13 assumption that confirmed history could remain or that archive should never reset capture numbering. Clear & Reuse's separate history semantics remain unchanged.

Physical reality already proven: original Archive removed exactly 4 disposable Drive photos, preserved both folders and Google Doc; Samsung 0.28.4-internal passed physical address-card navigation and menu checks; negative gate showed WAITING correctly blocks Archive, with no mutation. **Do not treat local-history retirement or capture-number reset as passed yet**. Current fixture has one new confirmed upload and one new WAITING record under Test B; do not delete/renumber either on installation or while WAITING remains. After focused/full CI of the repair, operator resolves that disposable WAITING photo explicitly, then runs one fresh, confirmed Archive and reactivation to prove old rows vanish and new capture starts 001. PR remains draft and Level 3 merge approval is still required. Rollback baseline remains the earlier verified head `31a33093b3067cccbc3906f1d677dc5c10f5a289` (0.28.4-internal).


## Phase 13E — Final physical acceptance evidence (2026-09-30)

**Implementation / full CI:** exact final runtime head `731a231695436da9f355cafaa6c9922d814e82ed`; Android CI #1139 (`36702591737`) **PASS** on this runtime head. Unit tests and production identity, internal debug build, production signing fail-closed, stable test signer/APK identity, complete connected emulator instrumentation, internal launch smoke, and rendered evidence all passed. Internal APK `0.28.5-internal`, versionCode `42`; CI APK artifact ID `11091101345`, workflow artifact digest `sha256:bf0dba7d86cd038f334e25264f6e2a991606adc661eb019a29c2af3458caee84`. The documentation-only evidence update does not alter this validated runtime head.

**Live context / fixture:** physical operator Samsung (validated MEMBER, Drive USABLE, camera GRANTED), selected `HNP Jobs` company inside the existing SAF Google Drive workspace, disposable address `99998 PHASE 13 TEST` with two existing dated work-order folders `PHASE 13 TEST A - 2026-09-29` and `PHASE 13 TEST B - 2026-09-29`, and manually placed non-photo Google Doc `PHASE 13 KEEP ME` under B. No live customer/job folders used. These are device-observed behavior claims, not synthetic provider-ID assertions.

**Original remote photo cleanup and preservation (physical evidence):**
- On Samsung 0.28.3, Archive preflight listed 2 dated work orders, 4 remote direct photos, and 1 direct non-photo item to preserve; the operator completed Archive. Independent Google Drive screenshots showed A empty, B retaining its original Google Doc, both original folder entries still present, and all 4 approved photos absent.
- The test address was found via search and reactivated, retaining its existing two dated work-order entries; exact provider-ID byte comparison was not independently exported from the device.
- The 0.28.4 navigation repair passed full CI #1138 (`36700372692`) and operator physical taps: the address card opened Work Orders and the row's three-dot action opened the Archive confirmation independently.

**Protected/unresolved negative gate (physical evidence):**
- Operator successfully uploaded one new disposable photo under B and left a second as local `WAITING`.
- Attempting Archive displayed `Could not verify address archive. Nothing was changed` and identified the `WAITING` photo; Home showed the target's `1 photo` protected indicator. This proves fail-closed refusal before cleanup for the witnessed WAITING case.
- Operator explicitly discarded only that disposable WAITING photo; Support Status on 0.28.5 then showed CAPTURING, WAITING, UPLOADING, FAILED, UNCERTAIN, Protected originals, and unreadable local records all zero. Do not infer no protected records on another device/context.

**Archive local-history retirement and numbering repair (physical evidence):**
- Under 0.28.5, the second Archive preflight listed **2 work-order folders, 1 Drive photo targeted, 1 non-photo item preserved**. The operator confirmed Archive; Home displayed **24** rather than **25** active properties and no longer displayed the disposable address.
- The operator searched for and reactivated that address, opened existing dated work order B, and provided a physical Photos screenshot of **Photos (0)**: the previously stale uploaded-history rows were absent. This directly verifies the issue the operator reported was repaired.
- The operator then captured and uploaded one new photo in B, copied **FPP_CAPTURE_ORDER_V1**, and supplied the following non-sensitive extracted assertions from its real result: address = `99998 PHASE 13 TEST`, work order = `PHASE 13 TEST B - 2026-09-29`, count = **1**, record prefix = **001**, remote filename prefix = **001_field-photo-**. Thus the next capture numbered **001**, not the old ledger sequence. The full photo ID and exact timestamp are intentionally not copied into this repo.
- This proof covers B's physical capture-number reset. A's 001 reset is covered by exact-head automated tests, not independently retested on the phone. The preserved document remained identified in the second preflight; its post-second-Archive content was not separately screenshotted, while the prior independent Drive preservation proof already passed.

**External-state disposition / stop boundary:**
- The address was intentionally reactivated for the successful numbering proof and currently contains **one newly uploaded disposable numbered test photo under B**, plus the earlier preserved non-photo test document. The prior archived photo history was removed; this final new photo was uploaded after the successful second Archive, so it is not expected to disappear until a **separately confirmed** cleanup. Do not claim the disposable fixture has been completely cleaned.
- Remaining fixture housekeeping: the operator may perform one last deliberate Archive of `99998 PHASE 13 TEST` to remove the newly uploaded test photo and retain the folders/document. The non-photo test document may be cleaned manually from Drive later if desired. No assistant-initiated Drive mutation is authorized or needed to mark the verified implementation behavior.
- **Phase 13E implementation, final full CI, and tested positive/negative/numbering physical behavior: PASS.** Preserve the precise evidence limits above. PR #93 remains draft/unmerged; final fixture disposition and **explicit Level 3 operator pre-merge approval** remain open. Do not interpret acceptance of a phone test or the earlier design approval as permission to merge.
- Runtime rollback point before the scoped archive-history correction: `31a33093b3067cccbc3906f1d677dc5c10f5a289` (0.28.4-internal). Governed full Phase 13 base: `6d3b77f527ae58c3e3dd026887644ee3c0cb6557`.

**Next exact checkpoint:** obtain the operator's preferred disposition for the disposable address's final newly uploaded test photo, then request explicit Level 3 merge approval on this exact validated runtime scope. Do not repeat established provider or instrumentation tests absent new contradictory evidence.
