# Phase 14 — Checkpoint A: Source Audit, Identity/Fencing Feasibility, and Implementation Gate

**Status:** CHECKPOINT A SOURCE AUDIT COMPLETE; FPP-ONLY COORDINATION EXCEPTION APPROVED BY OPERATOR; CROSS-PHONE DRIVE/SAF FEASIBILITY STILL OPEN.  
**Source audited:** governed `main` `bce15843eaac975abae31f7312f7505bfc6ce68f` after documentation-only PR #99.  
**Owning design:** `docs/PHASE_14_TWO_PHONE_JOB_CYCLE_PLAN.md` (read this first).  
**Authoritative checkpoint branch:** `phase14/checkpoint-a-identity-and-fencing`.  
**Level:** documentation-only work Level 1; proposed underlying Android/local queue/Drive/remote-coordination change Level 3.  
**Approval scope:** operator said "continue" to begin the technical safety checkpoint. No customer Drive, active phone/app, backend schema, account sharing, or cross-project system changed. **Operator's prior approval of PR #95 is not an approval to merge this separate Phase 14 runtime feature.**

## 1. Source-grounded integration and ownership map

| Actual code and method(s) | Current behavior from source | Phase 14 engineering implication |
| --- | --- | --- |
| `MainActivity.refreshWorkOrderFolders` (around L1812) | Lists folders by the **selected phone's** SAF provider ID and updates the existing `WorkOrderListAdapter` | Add non-destructive **In Drive** photo-count enrichment for Work Orders here (read-only, asynchronous). Do not reinterpret local count. |
| `WorkOrderListAdapter.getView` (around L78–87) | Row "N photos" counts only `protectedPhotoCountsByWorkOrderId` from the on-device store; zero hides the field | Separate `In Drive: N` from `On this phone: N`, and show unknown/stale rather than false zero. Preserve date and selection layout. |
| `PhotoCaptureActivity.refreshPhotoList` (around L458) | Scans `PendingPhotoStore` and filters records by local work-order provider ID; `Photos (N)` is this device's own history | Cross-phone uploaded images are not absent merely because they do not appear in this screen. Do not introduce a duplicate shared gallery. |
| `PendingPhotoStore.beginCapture` / `reserveNextCaptureSequence` (around L94, L250+) | Acquires **process-local** `CAPTURE_SEQUENCE_LOCK`, advances locally persisted sequence at capture | Same-work-order two-phone `001` uniqueness is impossible with current local counter. Preserve One-mode numbering; Two mode needs atomic cross-device reservation plus schema/versioned migration. |
| `PendingPhotoRecord` (schema 4) | Immutable stored provider work-order ID/name and local capture sequence; no shared cycle/generation UUID | Add cycle link and generation safely without rewriting old queued destination. Existing active records need migration/recovery classification before enrollment. |
| `DrivePhotoUploader.create` / `remoteFileNameFor` | Uses exact `uploadingRecord.workOrderId()` and prefix `%03d_field-photo-UUID.jpg`; creates JPEG via SAF provider | Keep per-phone SAF destination identity and UUID idempotency. Add online current-cycle upload permit + shared sequence reservation **before** provider create, and confirmed-upload acknowledgement after verified remote file. |
| `PhotoUploadCoordinator.upload` | One-device state machine that creates, persists provisional remote ID, writes/verifies, commits confirmed upload; no shared cycle permission | Extend one existing owning coordinator, including UNCERTAIN reconciliation, not parallel upload engine. A process-local `PhotoUploadGate` is **not** a shared two-device lock. |
| `DriveClient.listDirectChildren` | A fresh-provider request and **two equal settled reads**, enumerating direct MIME-classified photo/document IDs (not photo bytes) | Reuse the read path for safe shared counts/snapshots, but serial/lazy row refresh to avoid heavy queries. Successively matching provider reads do **not** themselves prove cloud-transactional isolation. |
| `MainActivity.performConfirmedClearReuse` (around L2266–2407) | Checks current local queue/history, compares selected Drive photo-ID set, calls `DriveClient.deleteDocument` by individual provider ID, verifies zero, optionally renames same folder, resets local occurrence | No knowledge of the other phone's unfinished local records, generation, pending uploads or reset. Must be blocked behind shared coordination **before the first remote mutation**, not patched with another dialog. |
| `MainActivity.performConfirmedPropertyCleanup` + `AddressPhotoCleanupMutation` (around L586–730) | Enumerates and removes photo IDs across several WO folders; resets address-local history and archive state | Include **every affected enrolled work order** in shared coordination. A Clear & Reuse-only patch is insufficient. |
| `FolderPrefs` and `PROJECT_PROFILE.md` | SAF tree/document IDs and exact selection are context-local, not portable to second phone | Never compare or copy raw Android SAF IDs between devices as proof they share the same Google Drive folder. |
| `SupabaseAuthClient` + `supabase/migrations/*` | Existing original-FPP Supabase holds user/Organization/membership/invitation identity and owner administration, with RLS/grants | Existing Supabase project is a plausible place for **minimal shared control**, but currently explicitly **forbidden** from job/photo persistence by `PROJECT_PROFILE.md`, `CONTRACT.md` and `docs/IDENTITY_MODEL_V1.md`. A narrow approved contract exception is prerequisite. Do not touch Field Work Hub. |

**Audited scope:** Android Java owners, current Drive SAF integration, queue naming/persistence, FPP identity migration definition and existing contracts. No physical two-phone link/uncertain-provider proof performed in this checkpoint. This is not a claim that a proposed protocol already works.

## 2. Decision: prefer tiny FPP coordination ledger; reject a Drive-only lock

**Proposed architecture:** FPP's existing dedicated Supabase project coordinates **only opaque work-order link identity, current cycle UUID and generation, enrolled device/member IDs, lead/hand-off, active operation fencing/recovery, and one idempotent numbering/reservation record per Two-mode photo**. JPEGs and complete customer work-order trees remain exclusively Google Drive; local pending originals remain on each phone. Do not create a general job/photo database or an extra delivery/storage service.

**Why Google Drive-only locking is not sufficient:** the Android SAF `DocumentsContract` calls used by FPP enumerate, create, rename and delete independent documents. They provide no app-exposed cross-device atomic compare-and-swap for a shared job-generation version or non-expiring exclusive operation lock. A lock represented only by a Drive filename/folder state can be cached on one device and raced by another. Two matching settled directory reads are freshness safeguards, **not a distributed transaction**. A Drive photo count is read-only evidence, **not** delete authority.

**Server contract exception is material:** The existing original-FPP backend contract prohibits job/photo records, even a small ledger. It must be amended explicitly and narrowly **before** schema/code. Proposed allowed metadata excludes address text, client-company names, photo contents, thumbnails, SAF URIs and provider file IDs; any remote-photo-ID ledger, if necessary, is an explicit separately reviewed privacy expansion. Enforce FPP Organization membership inside RLS/RPC, no Android service-role key, and no Field Work Hub access.

## 3. Cross-device binding design — recommended one-time challenge, not folder-name equality

Candidate protocol (NOT IMPLEMENTED):
1. Phone A enrolls one selected work-order folder while online using its current verified SAF selection; backend allocates opaque job-link key and short-lived random pairing challenge.
2. With the operator deliberately joining Phone B, Phone A creates **one short-lived non-image probe document** within the disposable work-order folder via its own Android SAF grant. Phone B, using **its own** SAF-selected folder, performs fresh direct-child enumeration and verifies the exact unpredictable probe challenge. A and B both attest using authenticated FPP membership; backend binds their distinct installation/client identities to the one opaque link.
3. Phones confirm the probe's exact identity is removed/verified and that no unintended files were changed. If the probe cannot be created/read/removed with real Google Drive DocumentsProvider, **do not enroll**; preserve existing one-person behavior. The remote probe is not a secret or permanent authorization token, and the actual generation/operation state lives only in the secure coordinator.
4. Each later session revalidates its **own** stored provider folder ID and live FPP membership. A display-name match, exported phone SAF ID, or the fact both can see HNP does not count as same-folder proof.

**Caveats requiring genuine device/provider validation:** the probe can be visible to the client while it exists; FPP's known customers share Drive folders. This scheme changes the folder temporarily and requires clear acceptable client-visibility and non-photo-cleanup semantics. It is **not approved** for real customer locations. Use one disposable fixture after deciding the visibility policy. If even transient marker visibility is unacceptable, use a different pairing mechanism and prove equal security; never silently plant a permanent metadata file.

No probe, Drive mutation, backend ID or authentication record was created in this checkpoint.

## 4. Serialized operations and safety proof obligations

Required cross-device logical protocol, to be assessed against actual provider behavior in disposable fixtures:

- `OPEN(g)`: current cycle generation g; photos captured bind to (opaque shared WO, cycle UUID g, device-specific stored SAF destination); offline capture is allowed within existing membership rules.
- Before a Two-mode upload calls `createJpeg`, obtain **current-cycle authorization** and atomically reserve the next sequence by (org, shared work order, generation, photo UUID). Record in-flight create/write/reconciliation. A remote uncertain result remains in-flight/blocked; local original is not discarded until confirmed.
- `QUIESCING(g)`: lead requests finish/reuse. Immediately refuse new upload reservations and cross-phone mutating operations for g. Require settled active operations and explicit participant acknowledgement of relevant pending/protected local work, or a tightly scoped owner recovery path that cannot write to a newer cycle.
- `CLEARING(g)`: server grants **one** durable destructive operation with immutable target cycle, selected photo ID set and operation identity. Block any new-cycle upload and competing clear/archive/rename. Each SAF deletion acts by the **exact previously authorized old document ID**; no broad list-then-delete that might sweep new children. On partial/uncertain remote result the shared work order stays **RECOVERY_BLOCKED**, not lease-expired/re-opened.
- `VERIFY(g)`: completed/settled remove and rename results, fresh exact selected SAF folder verification, no unexpected photos/non-photo effects, local queue retirement for initiating device and shared participant acknowledgements. Only then atomically advance to `OPEN(g+1)`, reset Two-mode shared sequence, and allow new-cycle uploads.
- Stale phone B with old `g` or old 30-photo history is refused before *any* new remote action. Its unconfirmed originals remain bound to g for explicit recovery; no automatic re-targeting.
- Address Archive/Delete must protect *all* affected managed work orders under a coordinated multi-work-order shutdown; if any enrolled work order is unsafe, do not begin cleanup of any photos under that address.
- All active operation tokens remain **durably blocked** on crashed/timed-out/ambiguous Drive calls. **Do not** expire a clearing lease and release g+1 solely because a clock expired. If an old provider operation might still complete, require recovery that prevents its late action from affecting new-cycle files (for example explicit isolation/migration before reopening).

**Open structural risk, NOT proven solved:** Supabase can serialize *permissions*, but it cannot atomically cancel an Android `DocumentsContract.deleteDocument` or `renameDocument` already in flight. Two consecutive settled provider queries do not prove that a delayed response cannot mutate later. A correct implementation must demonstrate its precise **fence + no-reuse-until-settled** or stronger **separate remote photo-set isolation** strategy. The latter would alter current same-folder work-order identity, folder naming/hierarchy or cleanup and requires another explicit contract decision. Do not claim a backend lock alone solves races.

**Other unsafe actor:** an old FPP build not honoring the new backend can still delete photos. Enable Two mode only after both participating phones are on a compatible guarded build and acknowledge enrollment; do not claim it protects manual Drive/other-app writes.

## 5. Numbering, lead and UI constraints

- One-person existing capture-time `001` allocation and `0.28.7` Clear & Reuse stays untouched for unmanaged WOs.
- In Two mode, each durable photo UUID receives **one** globally reserved sequence number for its g, before first remote create. Duplicate requests reuse it, independent of retries/offline reconnect. A known-safe failure may leave a gap; never renumber a client-uploaded photo to conceal a gap.
- Serializing remote upload batches in optional chosen "first" order is compatible with the operator's preferred workflow. Default lead is the device/user whose **verified successful upload completion** is first committed for g. Only current confirmed lead can finish or request Clear & Reuse. `Done` remains the camera-session button, not a job finalizer.
- Keep explicit `In Drive: N` counts separate from current local `N photos` display. `DriveClient.listDirectChildren` already returns `photoCount()` without downloading image bytes. Call in foreground with bounded/lazy refresh, avoid thousands of serial provider refreshes on screen load; stale, inaccessible, or unverified display is **Unknown**, not 0.
- Proposed single compact One/Two work-order control must not displace existing buttons; finish/action only visible for managed Two mode. Both phones refresh management state when toggled; correctness never relies on a screen refresh.

## 6. Workflow completeness coverage (approved Phase14 outcomes, not implemented)

| Workflow | Existing owner | Checkpoint A finding | Required future gate |
| --- | --- | --- | --- |
| Create/open a WO | `MainActivity`, `FolderPrefs` | Device-specific folder identities | Pair two distinct Android SAF selections to one demonstrated shared folder; no guessed matches |
| Take photos and Done | `PhotoCaptureActivity`, `PendingPhotoStore` | Current capture reserves local sequence | Two-mode cycle binding and protected originals, offline capture preserved |
| Upload/retry/UNCERTAIN | `PhotoUploadCoordinator`, `DrivePhotoUploader` | No remote-generation guard or shared counter | Idempotent cross-device reservation, fencing for unverified provider results |
| Work Orders photo counts | `WorkOrderListAdapter`, `DriveClient` | Current count is on-device, not shared Drive | Live shared count and explicit unknown state with both phones |
| Correct WO name/date | `MainActivity.renameWorkOrder` | PR95 non-destructive edit by same provider ID | Managed shared-operation guard; existing verified editing remains correct |
| Clear & Reuse same/later date | `MainActivity.performConfirmedClearReuse` | Device-local guard and photo-ID snapshot only | Two-device stale-generation, pending operations, exact old-cycle delete proof |
| Cancel/partial fail/restart | Local queues and provider state | Local recovery, not shared recovery | Durable coordinator RECOVERY_BLOCKED; no auto-expiry, no wrong-cycle upload |
| Archive/Delete Address | `MainActivity.performConfirmedPropertyCleanup` | Loops through WOs; only current phone's pending work known | Coordinated all-WO fence; nothing deleted if any managed child unsafe |
| Reopen/change One↔Two; lead handoff | No current shared manager | New feature | No downgrade/single-user toggle safety bypass |
| Client company/auth separation | `FolderPrefs`, FPP identity RPC | FPP auth independent of Android SAF | Original FPP org-scoped RLS and Drive grant proof; FWH untouched |
| Finish photo work | No FPP work-order finish | New Two-mode-only action | Lead-only, non-destructive, no accidental delivery |

## 7. Verification/impact and handoff

**No runtime/testing claim yet** beyond code-level inspection. No Android APK built or installed. No Drive photos were read, deleted or changed; no probe written; no Supabase schema/permissions modified.

**Protected behavior:** previously passed PR #95 tests stay passed. Keep original exact SAF stored queue destinations, One-mode numbering, photo preservation/retry, same/later Clear & Reuse, Home/Work Orders/Photos controls, company isolation and explicit user confirmations.

**External integrations:** one narrow future FPP Supabase coordinator plus each phone's independently authorized Google Drive DocumentsProvider; no FWH. Backend state schema, grants, migrations and operation endpoints are **PROPOSED ONLY**.

**Rollback baseline:** `bce15843eaac975abae31f7312f7505bfc6ce68f`. Revert this documentation alone to remove proposal. For future runtime work, record actual baseline before first code migration, never remove pending originals, and never revert an enrolled managed cycle to an unguarded app.

**Future implementation tests:** two Samsung/Android phones with different local SAF bindings but the same disposable Drive folder; 15+30 => 45 uniquely numbered photos, new cycle 10 images remain after stale B tries to clear, concurrent uploads, delayed provider responses, one phone offline with waiting originals, managed address archive block, old/unupdated phone incompatible, code restart/fault injection, unknown Drive count, fail-closed revocation; focused tests, one exact-head full CI and governed Level3 operator merge approval.

**Source audit result:** One implementation line per owned code path is feasible; the existing classes are suitable extension points. **The cross-device SAF pairing feasibility and safe distributed write fencing remain unproven assumptions.** No Phase14 runtime work should proceed until those preflight architecture questions and the narrow identity-backend exception have explicit product approval. There is no safe one-line toggle-only implementation that achieves the operator's protection goal.

**Exact next checkpoint:** decide whether to authorize the narrow FPP-only Supabase coordination ledger as a contract exception. Then take the smallest *disposable* Android/Drive pairing and operation-fencing reality proof on both phones; **stop and revise** if temporary marker visibility or deferred SAF operations make guarantees impossible. No repeat of PR95's already-passed single-phone tests.


## 8. Operator decision — Approved FPP-only coordination ledger (2026-10-09 local)

**Operator approval:** On 2026-10-09 local, the operator explicitly responded **"yes. i approve"** to the bounded question: "Do you approve that limited expansion of Supabase for two-phone coordination?" **APPROVED** for using the **existing original-FPP Supabase backend** for only the minimum shared coordination state, not for client-photo cloud storage, a full work-order/address database, or any Field Work Hub integration.

**Clarification, confirmed in the same exchange:** The planned **Photographers: One / Two** selection is **per individual work order**, not per property, company, or entire app. A Grass Cut may be set to Two while a Winterization at the same address remains One. One remains default for unmanaged work orders; switching a previously managed work order back to One must not remove generation/fencing safeguards. Existing UI controls must remain accessible. This requirement is already part of the Phase14 product plan and is reaffirmed here.

**Authorization scope:** This is a product/architecture decision allowing the narrowly scoped Level-3 contract amendment and future implementation design to proceed; **it is not evidence of a deployed or tested runtime feature, permission for live Supabase migration, or Level-3 pre-merge approval**. Explicit migration/runtime release and pre-merge gates remain applicable under `CHANGE_CONTROL_CONTRACT.md`. Contract text must identify the narrow exception before schema/API/Android implementation. Existing account/membership tables and FWH remain unchanged.

**Technical gates NOT yet passed:** (1) definitive matching of independent device SAF folder bindings with the *same actual remote* work order without guessing by name or exposing unsafe client-visible metadata; (2) proof that coordinator fencing + SAF operation recovery prevents late/stale deletes or uploads crossing a new cycle. Code must not implement an unsafe fallback if those cannot be proven.

**Next authorized engineering work:** Design and test narrowly scoped no-customer-data coordinator/fencing logic off-device; prepare explicit amended contract and secure API/schema proposal on the one authoritative Phase14 implementation line. The actual cross-phone real-Drive pairing reality gate waits for a disposable, operator-identified test folder and appropriate approval of any visible probe content. Reuse existing PR95 evidence; do not reopen or repeat its passed checks.
