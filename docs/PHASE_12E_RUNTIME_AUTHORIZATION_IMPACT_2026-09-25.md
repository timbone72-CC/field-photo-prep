# Phase 12E — Runtime Authorization Enforcement: Level 3 Impact Record

Date: 2026-09-25  
Status: design/impact record ready; runtime implementation not started  
Base: Phase 12D merged runtime and complete Phase 12 design.

## Problem and approved scope

Phase 12D proves sign-in, recovery, membership lookup, encrypted session restore, and account recheck, but ordinary field actions are not yet gated. 12E makes the existing validated FPP Organization identity authoritative for startup, capture, and remote Drive mutations, with a 72-hour same-Organization grace window for temporary account-service outages. Sign-out is guarded by unresolved protected work. Owner/member administration, local Organization-to-Drive binding migration, new-device onboarding, diagnostics, and production signing belong to later slices.

## Change class and ownership

Level 3 because this changes capture and Drive write authority while preserving protected work. One central authorization policy consumes `AuthSessionState`, `SecureAuthStore`, and `SupabaseAuthClient`; startup/resume and account navigation consume it in `MainActivity`/`AuthActivity`; camera and every underlying Drive mutation entry point enforce it immediately before starting actions. Upload batches recheck before each remote attempt. Existing photo store owns protected-work truth and immutable destinations. UI must never be the sole security gate. Exact affected methods and all remote-write entry points must be inventoried before implementation; unverified entry points stop runtime work.

## Required and optional data

Required: encrypted Auth session; User, Membership and Organization UUID snapshot; ACTIVE status; last successful complete validation timestamp; current authoritative validation result; local protected/unresolved queue state. Optional: provider connectivity and screen display state. No new Supabase job/photo data. A monotonic elapsed-time anchor may exist in process only; persisted wall-clock inconsistency fails closed after restart. No provider IDs or SAF URIs enter Supabase.

## Reads and writes

Reads: app-private encrypted session; Auth User and exact RLS-scoped Membership/Organization; pending-photo store states; local current destination and SAF permission when starting Drive work; system time. Writes: atomically rotated Auth token pair after refresh; validated snapshot/time only after complete successful validation; locally sticky authoritative revocation indication if required to prevent offline grace resurrection. Sign-out clears FPP credentials/navigation when permitted. No photo, destination, queue, Drive, Membership or Organization record is rewritten merely because account state changes.

## Identity, schema, permissions, platform access

No planned backend schema/RLS/grant change and no new Android permission or Drive tree selection. Existing encrypted session schema may need a versioned minimal addition to distinguish an authoritative revocation across restart; migration must preserve the validated identity and never corrupt encrypted token rotation. No separate permission boolean is persisted. No service-role key in Android. The existing SAF grant alone never establishes FPP authorization. Organization-to-Drive binding itself is deferred to 12H; 12E must not treat an unrelated current SAF grant as authority to switch Organization.

## Policy and stale/offline behavior

At online startup/foreground resume: restore, serialize token refresh, validate Auth User, require exactly one ACTIVE Membership and exact Organization, then update validation time. An indeterminate transport/5xx failure permits the same previously validated User/Organization to work only while elapsed time is strictly under 72 hours. At exactly 72 hours block new capture and all new remote Drive mutations. Missing timestamp, clock rollback, unreadable session, authoritative inactive/mismatch/revocation, or changed User fails closed. Authoritative revocation ends grace immediately and remains effective across restart until authoritative reinstatement. A successful complete validation resets the window. Organization switching and administration are unavailable in grace. Existing protected originals, local preparation, inspection, and reconciliation evidence remain safe in every state.

Capture authorization is checked before each shutter reservation. Remote authorization is checked immediately before each folder create/rename/clear, upload, cleanup mutation, retry, and each batch item. An in-flight attempt is reconciled to its actual provider result; later attempts stop. No queued destination is recalculated. UNCERTAIN uploads never receive blind retries. Sign-out is blocked for any unresolved CAPTURING, WAITING, UPLOADING, FAILED, UNCERTAIN or unresolved cleanup/original state that would be stranded. An allowed sign-out does not remove photos, queue evidence, provider IDs, SAF grant, or Drive content.

## Duplicate and idempotency behavior

Serialize refresh/revalidation to avoid reuse of rotating refresh tokens. Persist the new token pair before later membership requests can fail. Coalesce concurrent resume/recheck requests. Repeated successful validation updates only the timestamp/snapshot for the same authoritative identity. A network timeout cannot manufacture revocation. A lost response from a remote upload remains UNCERTAIN under the existing coordinator; account policy does not create a second retry path.

## Safe fixtures and tests

Focused tests inject a controllable clock: first-use offline refusal, boundary minus an instant, exact 72-hour boundary, beyond boundary, revalidation reset, clock rollback, same/different User and Organization, service outage versus authoritative revocation, persistence across restart, and serialized refresh. Gate tests cover every mutation entry point including a mid-camera shot and a batch whose next item must remain unattempted. Sign-out tests use disposable local protected records and confirm no data deletion. Preserve established queue and destination tests. Run the complete Android suite once on exact final runtime head. On Samsung, use disposable photos/work orders: online startup, airplane-mode grace, return-online revalidation, guarded sign-out, and existing capture/Drive smoke. Exact 72-hour boundary is automated; do not wait three days. Use real SAF/Drive fixture only for affected provider action smoke, never customer folders.

## Baseline, expected result, rollback

Baseline: 12D merged; Account is optional and field workflow continues without auth gating. Expected: signed-out startup cannot start new work; validated owner/member can; temporary outage inside grace retains safe work; expiry and revocation block new capture and Drive writes without deleting protected work; online validation restores permissions automatically when ACTIVE. Record exact baseline runtime commit and artifact before implementation. Keep prior working APK available. If the gate risks misrouting, loss, or incorrect upload state, stop affected uploads and revert the narrow 12E runtime commit/PR; never roll back by deleting protected work or replacing queued destinations. Session schema changes need an explicit read/migration/rollback test before implementation.

## Dependency and approval gate

Before code: enumerate actual camera/Drive entry points, confirm queue states and any cleanup-pending representation, and decide how durable revocation is represented without a duplicate state machine. Any mismatch with the complete design or contracts requires explicit reconciliation. Implementation uses a dedicated branch and PR, focused and final full tests, affected data-preservation and provider checks, and **explicit operator approval before Level 3 merge**. This record approves no merge or deployment by itself.
