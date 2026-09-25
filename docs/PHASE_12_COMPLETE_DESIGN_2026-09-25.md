# Field Photo Prep Phase 12 — Complete Design

Date: 2026-09-25  
Status: design complete; implementation remains 12E–12M  
Supersedes the provisional implementation details in `PHASE_12_MASTER_PLAN_2026-09-25.md` where this document is more specific. Contracts and approved identity architecture still govern.

## Outcome and fixed boundaries

The original Field Photo Prep app uses the dedicated FPP Supabase project for human and Organization authorization and Android SAF for access to a locally selected Google Drive workspace. A signed-in FPP account never chooses a Google account. Drive folders, work orders, and photos are never mirrored into Supabase. FPP Team remains separate. One active Organization per installation, invitation-only accounts, OWNER and MEMBER roles, and existing immutable photo destinations are fixed v1 boundaries. No public store release, subscriptions, cloud photo sync, or extra framework is implied.

12A–12D are complete. 12D is an optional Account screen and a proven encrypted session/recovery implementation; the mandatory gate begins in 12E. 12E–12M are designed below, but none is claimed implemented by this document.

## 1. One authorization decision

One application-level policy owner returns an immutable decision for the active User and Organization. Every UI action and every underlying mutation entry point checks that decision immediately before starting. UI button disabling alone is insufficient. A camera session additionally checks before each new shutter reservation; already reserved nonempty originals remain protected. Queue preparation that only transforms existing protected local files may finish. An upload runner checks before each new remote attempt, including each member of a selected batch. Already-started provider calls cannot be canceled retroactively; their results must be reconciled and never marked failed without evidence. Any later selected photos remain unattempted after authority is lost.

Decision inputs: readable encrypted session, matching User/Organization/Membership snapshot from a successful authoritative validation, validation time, currently observed authoritative response, clock, local Drive binding and permission, and protected-work guard. Persist only the session and last validated snapshot, not a second independent permission flag. State names:

| State | Cause | New capture | New Drive mutations | Read/recovery | Member admin |
| --- | --- | --- | --- | --- | --- |
| VALIDATED | Auth User, exactly one ACTIVE Membership, exact Organization successfully checked | Yes | Yes, if local SAF binding valid | Yes | OWNER only, online |
| GRACE | Temporary service/transport failure; matching last ACTIVE snapshot younger than 72 hours | Yes | Yes, if SAF/provider works | Yes | No |
| RECHECK_REQUIRED | Snapshot age at least 72 hours, missing trustworthy time, or session cannot establish the same identity | No | No | Yes | No |
| SIGN_IN_REQUIRED | No readable session or no previously validated identity | No | No | Yes | No |
| NO_MEMBERSHIP | Authoritative zero or multiple usable memberships, inactive status, or Organization mismatch | No | No | Yes | No |
| REVOKED | Authoritative revocation of the current Membership | No | No | Yes | No |
| DRIVE_DISCONNECTED | Otherwise permitted but no same-Organization verified SAF workspace | Local capture only when an exact existing work-order destination is safely bound; otherwise no | No | Yes | Account admin according to account state |

Network availability is not itself proof of permission or revocation. Validate on startup and on every return to foreground; serialize refresh/revalidation and coalesce concurrent requests. Recheck Account explicitly repeats that same operation. At a pending validation, retain the prior state only according to the grace clock; never flash unrestricted access from stale UI. A timeout, 5xx, connectivity failure, or other indeterminate response can enter GRACE. An authoritative Auth invalid/revoked result or Membership status/identity mismatch fails closed. A 401 caused by expired access token triggers one serialized refresh/retry before classification; a failed refresh with no recoverable session requires sign-in, not an invented ACTIVE result. No successful Membership validation means no first-use offline grace.

Persist the rotated access/refresh pair immediately after refresh, before Membership requests. Update the identity snapshot and validation timestamp only after Auth User, ACTIVE Membership, and exact Organization all pass. The same User and Organization must match the stored snapshot before any fallback to grace. A different authenticated User cannot borrow another User's grace. Revocation learned authoritatively is sticky locally until a new authoritative ACTIVE validation of that same Membership; restarting or going offline cannot resurrect old grace.

Clock: record wall-clock validation instant and an in-process monotonic elapsed reference. Grace applies only when elapsed time is nonnegative and strictly less than 72 hours; the exact 72:00:00 boundary blocks. Within a process, use monotonic elapsed time to resist manual wall-clock rollback. Across restart, if wall time precedes the saved validation time or time evidence is inconsistent, fail closed into RECHECK_REQUIRED until online validation; no user clock override exists. A successful online validation resets the window. Background time counts. Recheck just before each protected action so a screen left open cannot extend grace. Tests inject a clock and prove boundary minus one instant, boundary, plus one instant, rollback, reboot, refresh-token rotation, and revocation override. No real three-day wait.

## 2. Action and protected-work rules (12E)

Read-only account status, local protected-original inspection, queue review, and safe recovery guidance remain available in every account state. No auth state change deletes originals, prepared copies, queue records, Drive data, or provider identities. Ordinary Drive mutations include create, rename, reuse/clear, upload, cleanup that deletes remote content, and any remote retry. UNCERTAIN remains subject to its existing reconciliation rules, independent of auth permission. No automatic upload starts simply because authorization returns.

Capture requires a valid Organization and an exact local work-order destination previously selected through a verified same-Organization SAF binding; if the provider is temporarily unreachable, offline capture may use an already stored immutable destination. New folder creation requires a live usable provider. When authorization drops mid-camera, preserve the current in-flight shot and disable the next shutter. When it drops mid-batch, reconcile the in-flight attempt, then stop before the next. A confirmed remote result is recorded even if authority changed while the call was in flight; never retry it to compensate.

Sign-out is refused whenever there is unresolved work that could be stranded: nonempty CAPTURING reservation, WAITING, UPLOADING, FAILED, UNCERTAIN, or any original requiring reconciliation/cleanup under the active identity. A confirmed UPLOADED history record with no unresolved local cleanup does not block. Compute the guard from existing store truth, not a UI count. On allowed sign-out, attempt server sign-out online, clear local encrypted session and navigation state regardless of transient server failure, preserve the Organization-tagged local Drive binding but disable its use until a matching Organization signs in again. Never clear SAF permissions as a side effect. If a session is unreadable or membership revoked while protected work remains, expose local read-only preservation and status; do not offer a bypass that switches ownership or blindly retries uploads. An explicit future recovery procedure for stranded protected work needs its own governed design.

## 3. Membership and invitation lifecycle (12F)

All mutations run through narrowly scoped server operations. Authenticate the current caller, re-read exact ACTIVE OWNER Membership inside the operation, scope every query to Organization UUID, and apply a database transaction or equivalent concurrency control. Android holds only its publishable key. Read-only Owner roster and pending invitations expose only same-Organization minimal identity/contact/status. MEMBER receives no administration permissions, including through direct API calls.

Invite accepts normalized email, role OWNER or MEMBER, and exact Organization. One active/pending invitation per Organization plus normalized email; resend reuses the pending record and issues a fresh invite delivery safely rather than creating duplicate Memberships. A repeat request with the same intended role is idempotent. A different role requires an explicit Owner update, not silent replacement. Invitation expiry or cancellation makes the link unusable for Organization activation. Acceptance must prove the current Auth User controls the invited email via the Auth flow, then atomically create/activate exactly one Membership for the invited Organization; a callback token alone never grants Membership. Duplicate acceptance returns the same Membership. Existing User UUID is stable even if email later changes. A cross-Organization invitation cannot expose the other Organization's roster or grant Drive access. If acceptance would yield multiple active Organizations on an installation, the client blocks ordinary work pending a separately designed selector; it never guesses.

Role change, revocation, and reactivation target Membership UUID in the exact Organization. Repeat operations are idempotent. The final ACTIVE OWNER cannot demote, revoke, or remove themself or be changed concurrently with another Owner action that would leave zero active Owners. Two concurrent last-Owner operations must be serialized/guarded in the database. Owner self-revocation is rejected when it would strand unresolved work locally, but server-side revocation by another Owner cannot depend on device state; that phone becomes read-only once it learns revocation. Reinstatement requires authoritative ACTIVE revalidation; a stale local snapshot cannot restore permission. Audit minimal actor, target, action, time, and result server-side without storing photos, addresses, provider IDs, passwords, or tokens. Invite email delivery failure leaves a retryable, visible pending state; never claim delivery merely because a record exists.

## 4. First run and local Organization/Drive binding (12G–12H)

First run: no session -> sign in or accept invitation -> validate exactly one ACTIVE Membership and Organization -> show Drive not connected -> user deliberately selects the approved workspace with Android SAF -> verify persisted tree permission and exact provider root -> record the local binding -> select client company -> choose property/work order -> capture. FPP auth and Drive account emails may differ. A returning same-Organization User can reuse a locally verified binding; an invited person on another phone must independently obtain Drive sharing outside FPP and select the workspace there. A no-session install never imports prior phone's SAF URI/provider IDs; backup exclusions remain in force.

Binding record is app-private and minimal: Organization UUID, existing workspace tree URI/provider-root identity already used by the app, and a local binding version. It is written atomically only after the user selects and the provider confirms the tree; it is excluded from device backup. Do not put this record in Supabase. Existing legacy workspace selection with no Organization tag is quarantined on first gated launch; after same-Organization online validation, require explicit user confirmation/reselection and provider identity check before attaching a tag. Never silently assign an untagged tree to the first signed-in Organization. If unresolved photos exist during migration, preserve their stored destinations and require a safe same-provider verification before new writes; no automatic remapping.

A binding is usable only when its Organization UUID equals the currently authorized Organization and the SAF grant/provider still validates. A different User with the same Organization may reuse it only after authoritative Membership validation and a local check that no unresolved work crosses a User ownership boundary; otherwise read-only and deliberate recovery. A different Organization sees disconnected status and cannot use the previous binding, even if Android still grants access. Switching Organization is blocked while unresolved/protected work exists, and v1 has no automatic organization switcher. Connecting another workspace does not rewrite existing queued destinations. Existing work under an old binding is reconciled only through its stored identity and explicit governed recovery; no folder-name fallback. No silent grant migration, provider-ID portability, or automatic sharing changes.

12G navigation depends on 12H binding rules; implement the binding guard before enabling the complete first-run Drive flow. The earlier master plan's G-then-H ordering is a documentation/dependency sequence, not permission to ship unguarded first-run writes.

## 5. Diagnostics and recovery experience (12I–12J)

One read-only App Status screen consumes the authorization decision, Drive-binding verifier, and existing queue store. Show FPP account/Organization name and role, last successful validation, current state and rounded grace remaining, connected/disconnected Drive status, current company, protected/unresolved counts by existing queue state, app version, and relevant permission status. Recheck Account is the single explicit authorization refresh action; Connect Drive opens the existing SAF picker only when current authorization allows binding. The copied support summary is a strict allowlist of state labels, counts, app version, and coarse timestamps. Exclude tokens, email by default, passwords, addresses, notes, photos, SAF URI, provider IDs, Google account identity, and raw errors containing these.

| Situation | Message/action | Safety result |
| --- | --- | --- |
| Temporary account outage within grace | Work remains available until displayed deadline; Recheck Account | Existing destinations only; no admin |
| Grace expired | Sign in/connect and Recheck Account | Preserve and show protected work; no new capture/write |
| Revoked/no ACTIVE Membership | Contact an Organization Owner; Recheck after access restored | No grace; no new work/write |
| Unreadable/expired session | Sign in again | Never clear protected work; guard cross-identity access |
| Drive unavailable/wrong binding | Connect or restore correct Drive workspace | No guessed destination; no remote write |
| Sign-out blocked | Resolve the listed pending/uncertain photos safely | No force sign-out shortcut |
| UNCERTAIN upload | Follow existing reconciliation path | No blind retry |

Messages identify what remains protected and one practical next action. They never invite deleting photos to resolve an account problem. Status screens own no separate permission logic.

## 6. Release identity path (12K)

Internal and production application IDs and callbacks remain distinct: `com.inandout.fieldphotoprep.internal://auth-callback` and `com.inandout.fieldphotoprep://auth-callback`. Reject wrong scheme/host/type; recovery performs fresh normal sign-in after password change. A release build contains only the dedicated project's URL and publishable key. Secret/service-role keys and production signing key material never enter the APK source or repository. Use a separately controlled production signer; retain its certificate and key securely so updates preserve Android package/signature continuity. Internal builds keep their established test signing and never replace a production installation by accident. Record versionCode monotonicity, artifact hash, signer fingerprint, rollback APK/commit, and install-over-existing behavior before distributing an update.

Current internal controlled use may use Supabase's hosted Auth email. Before invitations to outside users or public distribution, configure and verify production-suitable email delivery and recovery, including rate/abuse behavior, from an address the business controls. Custom URI callback is accepted for controlled current use; a broadly distributed app needs a separate verified HTTPS App Link decision when an owned domain is available. Domain, Play Store, and monetization are not prerequisites for current Phase 12 work. Release readiness means a documented controlled path, not a claim that a public launch has happened.

## 7. Ordered delivery and evidence

| Slice | Build boundary | Required evidence and stop gate |
| --- | --- | --- |
| 12E | Central decision, runtime gates, sign-out guard | Deterministic clock and action tests; serialized refresh; full suite on final head; Samsung online/offline/recheck and protected photo smoke; Level 3 approval before merge |
| 12F | Server Owner API and narrow Android UI | RLS/role/cross-Organization, duplicate, concurrency and last-Owner tests; invitation email/acceptance on disposable users; full suite; Level 3 approval |
| 12H | Organization-tagged local SAF binding and legacy migration | Store/migration tests, wrong-Organization rejection, immutable queue checks, disposable real provider fixture; full suite; Level 3 approval |
| 12G | First-run and invited-user navigation consuming 12H | No-session/new-device flow tests; no silent Drive selection; full suite; physical clean-install evidence deferred to 12L |
| 12I | Read-only diagnostics | State-source and redacted-copy tests; full suite; affected UI smoke |
| 12J | Recovery screens and routes | All blocked-state transitions, no destructive shortcuts; full suite; affected UI smoke |
| 12K | Signing, callbacks, email/update path | Exact package/callback, signer/update, publishable-only secrets and email tests; full suite on runtime changes; Level 3 approval and install check |
| 12L | Integrated device reality gate | Clean/no-session, invited MEMBER and OWNER, Drive reconnect, sign-out/identity boundary, protected-work safety on disposable fixture; no fabricated second-phone portability claim |
| 12M | Closeout | Diff/source-of-truth, RLS/grants/security advisor, backup/privacy, evidence, deferred items and final exact-head regression review |

The implementation order is 12E -> 12F -> 12H -> 12G -> 12I -> 12J -> 12K -> 12L -> 12M. This corrects the provisional G/H sequencing so new-device onboarding never enables an unbound Drive path. Documentation/design may proceed independently, but overlapping auth and binding runtime branches must not diverge. Each runtime slice gets its own impact record, exact owner files and rollback commit before code changes. Level 3 merges require the operator's explicit approval under `CHANGE_CONTROL_CONTRACT.md`; this design approval does not preapprove their merge. On a gate failure stop that slice, preserve photos and current working APK, and revert the narrow change rather than guessing in production.

12L may combine a suitable second phone observation with Phase 8C; otherwise Phase 8C portability remains unproven. Do not wipe the operator's working phone merely to make a clean-install test; use an isolated disposable installation/test device when available, and distinguish automated evidence from physical evidence. No 72-hour physical wait: the exact boundary is deterministic test evidence. 12M declares Phase 12 complete only after all implemented claims and the final regression pass are recorded.

## Open product decisions deliberately outside this design

Public distribution method, paid access, domain purchase, extra Organization switching UI, public self-signup, cloud photo sync, and a recovery mechanism that transfers unresolved photos across identities are separate future decisions. Their absence does not block the specified internal Phase 12 implementation. A safe physical test device and production signing material are execution inputs for later gates; no undocumented fallback may be treated as passing evidence.
