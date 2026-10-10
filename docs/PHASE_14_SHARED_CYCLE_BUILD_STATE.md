# Phase 14 — Shared Cycle Foundation, Level-3 Build State

**Authoritative branch:** `feat/phase14-shared-cycle-foundation` (single owner for this scope).  
**Proposed PR:** draft Level 3, no merge permission yet.  
**Governed rollback base:** `main` `1772db779576f8e42f7bc73b6c4b7b3a61c794ce` (PR #101, operator limited Supabase exception recorded).  
**Operator authorizations:** 2026-10-09 local: “yes. i approve” limited **original-FPP Supabase coordination ledger**, and “Continue Phase 14 with the approved Supabase coordination design and its two-phone safety protections.” This authorizes governed implementation work, **not deployment of unverified deletion/upload paths or final Level-3 merge**.

## Goal and affected path

Build a safe, small two-phone shared-work-order cycle controller while keeping currently working single-phone Field Photo Prep stable. Per individual work order (not per app/company/address), the eventual UI will offer One/Two photographers. Uploads from both devices must use one authoritative sequence, current cycle identity and lead; stale-phone writes and duplicate UUIDs must be blocked. Clear & Reuse, Archive/Delete and finish need coordinated cross-phone fencing and cannot touch the next cycle's photo set.

**Affected contracts:** `PROJECT_PROFILE.md`, `CONTRACT.md`, `INTEGRATION_CONTRACT.md`, `docs/IDENTITY_MODEL_V1.md`: add exact, minimal operator-approved backend exception; earlier identity-only language still applies outside the exception.

**Staged code:** `SharedCycleGate.java` is an **inactive in-memory reference policy**, not connected to Android or actual Supabase. `SharedCycleGateTest.java` proves the policy logic with deterministic and fault-edge tests. There is no new UI button and no remote-write code path changed.

**Staged backend:** `supabase/proposals/phase14_coordination_sealed_foundation.sql` defines a sealed original-FPP-only schema with opaque job/cycle IDs, participant IDs and photo UUID→sequence, plus serialized reservation/confirmation/uncertain RPC drafts. **It is not in `supabase/migrations/`, not applied to production, and grants NO client execution on its RPCs or direct table access.** No pairing, destructive RPC, or recovery API is enabled; a schema draft is not a completed synchronized service.

## Risk/authorization and data classification

Level 3 because the target eventually changes local schema, upload sequencing, remote authorization and deletion. Existing local protected originals, exact captured destination IDs, offline capture, old single-person naming, and PR95 same-date Clear & Reuse must not be modified just to support mode Two. The only allowed backend data exception is opaque coordination state—not photo bytes, addresses, client-company names, Drive file/folder/provider IDs, SAF URIs, general work-order content, FWH identity or cloud storage.

FPP backend project (read-only verified 2026-10-09 local): `vtyiktvqhbgabawotkrj` ("Field Photo Prep"), **not** `vyocaujuwrivoqynvitm` ("Field Photo Prep Team" / now separate FWH). Original project reported ACTIVE_HEALTHY with `fpp_organizations`, `fpp_memberships`, `fpp_invitations`, `fpp_admin_audit`; migration list ends in `20261008193859_fpp_pending_invitation_recovery`. **No live database changes made.**

## Gate inventory and no-skip evidence

| Approved requirement and source | Owning implementation | Evidence | Current status |
| --- | --- | --- | --- |
| 1/2 mode per selected work order; One default; no displaced actions (Phase14 plan §2) | Future Work Orders UI + persistent shared coordinator | Source plan approved, not active UI | PENDING implementation and two-phone UI proof |
| Same actual Drive folder on two independent SAF grants (Phase14 §3; Integration boundary) | Future disposable pairing protocol | Source-only feasible challenge design, not real device | **STOP GATE** — unverified |
| Two phones numbered 001... without collisions (§5) | Inactive `SharedCycleGate`, sealed SQL proposal | Pure Java tests for shared seq and duplicate UUID; SQL not executed | Policy coverage staged; transactional/backend and Drive behavior PENDING |
| First *confirmed* uploader becomes lead; optional preferred lead (§2) | Reference policy `SharedCycleGate` | Unit tests | Policy staged, not wired |
| Stale phone cannot clear new-cycle photos (§4) | Current-generation guard in reference policy; future backend RPC and Drive owner | Stale phone test; real delayed-provider result still unproven | **STOP GATE** — backend+real SAF required |
| In-flight/uncertain remote write blocks rollover (§4) | Reference model / sealed RPC mark uncertain | Unit tests including blocked/no timeout | Policy covered, durable interrupted SAF proof PENDING |
| Clear only old photos, preserve folders/non-images (§4) | Future `MainActivity` integration of existing photo-ID snapshot + coordinator | Reuse historical single-device PR95 tests; no two-device proof | PENDING |
| Address Archive/Delete must guard **all** enrolled child WOs (§4) | Future `MainActivity.performConfirmedPropertyCleanup` coordinator integration | Source paths audited; no code changed | PENDING |
| Offline queued originals survive, cannot wrong-cycle upload (§6) | Future `PendingPhotoRecord` cycle binding + `PhotoUploadCoordinator` | Existing single-device retention evidence; no cross-device tests | PENDING |
| Supabase original-FPP membership/secret isolation (Checkpoint A §2) | Sealed SQL proposal and contract exception | Original live project read-only inspected; no RPC grants in staged SQL | SQL NOT deployed/executed; security review PENDING |
| No regression to current app | Existing unchanged running Android classes | New policy class unused; Android final CI when available | PENDING CI |

## Important limitations, errors to avoid and recovery

- **The reference Java gate does not coordinate Android devices**: it is an executable contract/unit-test model, not a shared in-process Android singleton to be used for actual authority. Never wire it as if it were the server.
- **The SQL is a sealed proposal**: no client grants, no work-order enrollment RPC, no safe shared destructive completion, no provider attestation, no migration. No live backend parity claim.
- A Supabase DB transaction cannot revoke an Android SAF `DocumentsContract.deleteDocument` already in flight. Holding a DB fence and waiting for all in-flight operations to settle **without lease expiry** is mandatory; crash or unknown remote result stays `RECOVERY_BLOCKED` until provably safe recovery. Manual Drive edits and old unguarded APKs remain outside fully enforceable app guarantees; Two-mode enrollment must require two updated compatible phones.
- On same-device offline capture, old immutable destination and local protected bytes stay safe. Do not mark new generation just because a screen restarted or a provider cache looks empty.
- The test folder `TEST FOLDER 99999` from PR95 is disposable but **no permissions to mutate it here are inferred**; no Drive mutation is performed.
- *Rollback*: discard this branch if no deployment has occurred. If future live migration ever occurs, only use recorded separate recovery/compatibility plan; do not uninstall or downgrade a phone with protected queued photos.

## Checkpoint plan

1. Contract exception and isolated shared-cycle unit-test policy; sealed SQL draft with transaction/authorization principles — **IN PROGRESS**. Run exact-head Android CI and governance on draft PR.
2. Prove SAF two-phone work-order pairing using disposable data, decide whether transient markers visible to a client are acceptable, and prove delayed/crashed provider-write fencing. **Required before enabling any production shared remote writer.**
3. Complete genuine Supabase schema/RPC, RLS, device challenge attestation and recovery on the one branch, with automated server-side concurrency tests and test-project/transaction rollback. Only then apply governed live migration with explicit verification.
4. Integrate versioned/cycle-bound Android queue and network coordinator using current upload owner, not a second upload system. Add per-WO One/Two control, shared Drive count, optional lead; guard edit/archive/delete/clear.
5. Full CI + smallest necessary two-physical-phone disposable Google Drive gate. Existing passed PR95 single-phone evidence reused; no gratuitous repeats. Explicit Level-3 **pre-merge approval** before merging or publishing APK.

**No claim of completion of Phase14 or real shared two-phone protection is made at this point.**
