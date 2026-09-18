# Phase 10A — Authoritative Main Reconciliation Impact Record

Date: 2026-09-17

Status: **LEVEL 3 — READY FOR FINAL PR/CI REVIEW; EXPLICIT PRE-MERGE APPROVAL PENDING**

Reconciliation branch: `phase-10a-authoritative-main-reconciliation-20260917`

Current canonical base:
- `main`: `80847b2c270d023daaf5ccda3a5313f3883162aa`

Current field-tested development line:
- branch: `feat/capture-order-photo-organizer-20260916`
- current documentation head before this record: `c3ca087bc537e6b8179f658fc31d3cd3d436ef79`
- last runtime-changing merge head: `e52765fd5b02266244d9101c5fd5429d5aa4e6c4`
- internal runtime: versionCode 25 / `0.20-reuse-occurrence-history-isolation`

## Purpose

Make the already-tested 0.20 development lineage the single authoritative repository baseline before new unrelated runtime work begins.

This phase does **not** design or implement new runtime behavior. It promotes an existing tested lineage intact so future work does not accidentally start from the older default `main` branch and omit already-proven fixes.

## Change classification

**Level 3.**

Although Phase 10A itself adds no runtime behavior, the lineage being promoted contains previously governed Level 3 changes involving:
- upload confirmation / UNCERTAIN / reconciliation behavior;
- persisted photo metadata schema compatibility;
- durable capture-order sequence bookkeeping;
- work-order-reuse sequence reset and old-occurrence isolation.

Therefore the canonical-main promotion is handled as Level 3 and requires explicit operator approval immediately before merge.

## Structural verification

GitHub compare evidence:
- `main` → current field-tested branch: **71 commits ahead, 0 behind**;
- merge base is exactly current `main` at `80847b2c270d023daaf5ccda3a5313f3883162aa`;
- no rebase, cherry-pick reconstruction, or conflict-resolution runtime edit is required;
- the tested lineage can be promoted intact.

The compare currently spans 31 changed paths, grouped into:
- contracts/version metadata;
- upload verification + reconciliation;
- capture-order filename / sequence persistence;
- work-order reuse sequence-reset / occurrence isolation;
- focused unit/instrumentation tests;
- supporting impact/test records and roadmap documentation.

## Runtime identity and post-test documentation

Exact runtime-changing head:
- `e52765fd5b02266244d9101c5fd5429d5aa4e6c4`

Exact Android CI:
- run `35219155472`
- result: **SUCCESS**
- event: push
- exact head SHA: `e52765fd5b02266244d9101c5fd5429d5aa4e6c4`

Comparison from `e52765f...` to pre-10A development head `c3ca087...` shows only:
- `docs/ROADMAP.md`

No runtime, test, build, manifest, resource, queue, Drive, camera, or persistence file changed after the exact tested runtime head.

## Included governed changes and evidence

### Upload reliability + safe bulk reconciliation

Original repair head:
- PR #42 head `e5ffb42fd4d6ea785ada79097bb7cbf28150ba85`
- exact head is an ancestor of the current 0.20 lineage;
- compare from PR #42 head to current field-tested branch: ahead-only, 0 behind.

Automated evidence:
- Android CI run `35151330156`: PASS on PR #42 head.

Real Android / Google Drive evidence already produced on the lineage:
- after the bounded provider-settle repair, a normal new Samsung upload reached confirmed `UPLOADED` on attempt 1 without reconciliation;
- a real bulk `Reconcile All Uncertain` run processed 53 persisted UNCERTAIN records and resolved all 53 as exact confirmed Drive matches, with 0 missing/retry-safe and 0 unresolved;
- this was operational recovery evidence, not a manufactured risky live-data experiment;
- later disposable PR #43/#44 Samsung + Google Drive gates exercised the descendant upload lineage successfully.

Protected behavior remains:
- no blind retry;
- unresolved remote truth remains `UNCERTAIN`;
- reconciliation itself performs no Drive create/upload/delete/rename/move/permission write;
- exact stored destination identity and provisional remote identity remain authoritative.

### Automatic capture-order filenames

PR #43:
- head `12c9bcce8c0db443fc1ff87277c20a5e3bcf526c`
- Android CI run `35179166425`: PASS;
- Samsung Galaxy S21 + Google Drive reality gate: PASS;
- verified 001/002 sequence, consumed discard gap, restart persistence, reverse upload order retaining capture identity, and in-place app-data survival;
- explicit Level 3 merge approval was obtained before PR #43 merged into the tested development line.

Persisted compatibility:
- schema v4 adds optional capture sequence;
- legacy schema v1–v3 remains readable;
- legacy uploaded UUID-only remote filenames are not renamed.

### Reused work-order sequence reset / occurrence-history isolation

PR #44:
- final head `7bb0a769ccf74c3e31128173410c6e47332de922`
- Android CI run `35216690680`: PASS;
- merged runtime head `e52765fd5b02266244d9101c5fd5429d5aa4e6c4`;
- exact merged runtime Android CI run `35219155472`: PASS.

Disposable Samsung + Google Drive gate:
- same provider work-order folder identity retained through approved reuse;
- new occurrence opened with Photos (0);
- captures uploaded as 001, 002, then 003 after full app restart;
- Copy Capture Order contained exactly current-occurrence 001/002/003;
- no prior-occurrence rows leaked into the active occurrence;
- no live customer Drive data was used for this gate.

Explicit Level 3 approval was obtained before PR #44 merged into the tested development line.

## Required and optional data

Required existing app-private data that must remain readable:
- persisted master-tree / provider context;
- selected address/work-order provider identities;
- pending-photo metadata;
- protected original / prepared-copy paths for active records;
- provisional and confirmed remote photo identities;
- capture-created timestamps;
- capture sequence where present;
- capture-sequence ledger and reuse occurrence markers.

Optional / legacy-compatible data:
- capture sequence absent on schema v1–v3 records;
- confirmed historical records from earlier reused occurrences.

No data conversion is introduced by Phase 10A itself.

## Schema / identity / permission / platform changes

Phase 10A adds none.

The promoted tested lineage already contains the governed schema/ledger changes described above.

Unchanged:
- Android SAF / DocumentsProvider integration;
- approved master-tree grant;
- stable provider document ID as remote destination identity;
- no app-managed Google OAuth;
- no sharing-permission changes;
- folder hierarchy remains master → address → dated work order → photos.

## Master-folder and destination assumptions

Unchanged:
- provider document ID is authoritative;
- names are display/discovery information;
- ambiguous same-name folders are never guessed;
- stored inaccessible destination stops affected upload rather than silently redirecting;
- queued photo destination remains immutable after capture acceptance.

## Duplicate / idempotency behavior

Unchanged safety rules:
- no second remote create after unresolved upload state;
- legacy + sequence-prefixed photo names remain deterministic for their record type;
- capture sequence reservation is durable and consumed numbers are not reused within an occurrence;
- approved folder reuse may intentionally reset the new occurrence to 001 while retaining the same provider folder ID;
- unconfirmed prior-occurrence photos block folder reuse.

## Offline / stale-state behavior

Unchanged:
- camera capture remains offline-capable;
- waiting photos survive restart;
- stale/provider-loading state fails closed where authoritative absence/emptiness is required;
- UNCERTAIN remote state blocks blind retry;
- pending reuse reset blocks guessed sequence assignment and self-recovers only when the exact intended reuse target is verifiable.

## Read/write surfaces of Phase 10A itself

Reads:
- repository history, branch ancestry, compare metadata, prior impact records, CI evidence, PR evidence, and recorded physical-gate evidence.

Writes:
- repository documentation;
- one reconciliation PR to `main`;
- after explicit approval, Git merge metadata only.

Phase 10A itself performs no Android runtime write, Drive write, schema migration, file upload, delete, rename, permission change, or device install.

## Affected regression surfaces inherited from the promoted runtime

Relevant existing checklist areas:
- A. App launch and folder state
- D/E. Work-order discovery and reuse
- F/G. Capture and preparation
- H. Upload destination
- I. Upload status and cleanup
- J. Offline and retry
- K. Unconfirmed-photo protection
- L. Permissions/destructive behavior
- M. Minimal field workflow

No new regression behavior is added by the reconciliation merge.

## Final verification plan

1. Open one PR from this reconciliation branch to `main`.
2. Confirm GitHub still reports an ahead-only lineage with no merge conflict.
3. Run/accept the repository's complete Android CI on the final PR head.
4. Confirm any commits after exact tested runtime `e52765f...` are documentation-only unless a new runtime change is explicitly introduced.
5. Do **not** perform another physical phone/Drive gate merely to repeat already-valid exact-lineage evidence when no runtime integration delta exists.
6. Obtain explicit operator pre-merge approval.
7. Merge without squash/rebase reconstruction so the tested commit ancestry is preserved.
8. Verify `main` now contains the exact tested runtime lineage.
9. Only after canonical promotion, close/supersede stale open PRs that are already incorporated or replaced by the authoritative main history.

## Rollback

Pre-merge known-good canonical `main`:
- `80847b2c270d023daaf5ccda3a5313f3883162aa`

If the repository promotion itself must be rolled back:
- revert the Phase 10A merge on `main`;
- do not rewrite or delete installed app-private queue/photo state;
- do not delete Drive content;
- do not downgrade a device merely as a repository bookkeeping rollback unless a separately evidenced runtime defect requires a governed device rollback.

Because Phase 10A adds no new runtime delta beyond the already-tested 0.20 lineage, rollback should target the promotion merge rather than inventing patch commits.

## Stale PR handling after promotion

After `main` contains the authoritative tested lineage:
- PR #42 can be closed as incorporated by the promoted lineage rather than merged separately;
- PR #39 must **remain open for separate governed review**. Code comparison shows it contains two reconciliation-hardening behaviors not present in 0.20: treating `ContentResolver.refresh(...) == false` as a best-effort provider hint rather than a hard block, and allowing exact id/name/MIME + SHA-256 content proof to override stale provider size metadata such as temporary `0`. Phase 10A does not absorb or reject that Level 3 fix; it moves to Phase 10C evaluation rather than being silently lost;
- PR #34 should be closed as superseded by the completed Phase 9 Concept 3 UI line already present in main history.

Do not merge or close stale PRs simply to make the open list empty. In particular, PR #39 is excluded from 10A promotion and retained as a live candidate because it contains unique reconciliation logic.

## Approval status

Implementation / reconciliation preparation: **AUTHORIZED** by the operator's 2026-09-17 instruction to begin work.

Explicit Level 3 pre-merge approval to update `main`: **PENDING**.
