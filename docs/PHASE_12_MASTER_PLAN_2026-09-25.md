# Phase 12 Master Plan — User Identity & Release Readiness

Date: 2026-09-25

Status: **APPROVED MASTER PLAN — 12E COMPLETE / 12F IN PROGRESS**

## Purpose

Phase 12 adds durable FPP User/Organization identity and release-readiness controls without turning Field Photo Prep into a cloud job/photo system and without conflating FPP sign-in with Google Drive authorization.

This master plan turns the previously loose “later Phase 12 slices” into one ordered program so later work does not invent overlapping account-state, Drive-binding, diagnostics, invitation, or release behavior one subphase at a time.

The plan governs the remaining Phase 12 work after the completed 12A–12E foundation.

## Governing sources

This plan is subordinate to the repository contracts and the already approved identity/auth architecture:

- `CONTRACT.md`
- `CHANGE_CONTROL_CONTRACT.md`
- `TESTING_CONTRACT.md`
- `INTEGRATION_CONTRACT.md`
- `docs/PHASE_STAGING_DOCTRINE.md`
- `docs/IDENTITY_MODEL_V1.md`
- `docs/PHASE_12_IDENTITY_MODEL_IMPACT_2026-09-21.md`
- `docs/PHASE_12B_SUPABASE_AUTH_ARCHITECTURE_2026-09-24.md`
- `docs/PHASE_12D_ANDROID_AUTH_SESSION_2026-09-24.md`

If a later implementation choice conflicts with those sources, the conflict must be reconciled explicitly rather than silently changing the identity model.

## Phase 12 finish line

Phase 12 is complete when the original FPP app can safely support all of the following without weakening the proven field-photo workflow:

1. a real FPP User signs in to the dedicated original-FPP Supabase project;
2. the app validates the User's exact ACTIVE Organization Membership;
3. ordinary startup/resume authorization is enforced;
4. temporary account-service outages allow the same Organization to continue safe field work for at most 72 hours after the last successful Membership validation;
5. successful validation resets that grace window automatically;
6. authoritative revocation stops new Organization work and new Drive mutations without deleting protected local work;
7. sign-out is safe and cannot strand unresolved/protected work;
8. Owners can manage Members/Owners and invitations without exposing a service-role secret to Android;
9. a new install/new user deliberately establishes its own FPP identity and Android SAF Drive binding;
10. an existing Drive binding cannot silently authorize a different FPP Organization;
11. the operator can see useful account/Drive/work status without exposing sensitive provider/token data;
12. common recovery states have clear, safe guidance;
13. the production identity/signing/update/auth-email path is defined and verified to the extent required for the chosen distribution model;
14. clean-install/new-user reality gates pass for the claims actually being made;
15. final account/privacy/security/source-of-truth review is complete.

Phase 12 does **not** require public app-store release. It requires a safe production-capable path, not a forced distribution decision.

## Permanent boundaries for every remaining subphase

The following remain non-negotiable:

- Supabase is the FPP identity/account backend only.
- Google Drive remains authoritative for client-company folders, properties, work orders, and photos.
- Original FPP remains separate from Field Photo Prep Team.
- FPP Auth never selects or authorizes the Android Drive account.
- Android SAF/Drive access never grants FPP Organization Membership.
- Auth email may differ from the Google Drive account email.
- Provider-bound Drive IDs and SAF URIs remain device/platform-context data.
- Protected originals, queue/reconciliation evidence, and immutable photo destinations remain protected.
- Sign-out, revocation, or account closure never automatically delete Drive content or protected photos.
- No Supabase Storage/Realtime/job-photo mirror is added.
- No Room/SQLite, WorkManager, broad Supabase SDK migration, Kotlin/Compose rewrite, or new permanent photo library is introduced merely for identity.
- Invitation-only v1 remains the default; open public self-signup is outside this plan.
- OWNER and MEMBER remain the only v1 roles unless later evidence requires a governed model change.

## Completed foundation

### Phase 12A — Identity Model v1 — COMPLETE

Settled:
- permanent User UUID independent of email;
- permanent Organization UUID independent of Drive;
- explicit Membership;
- OWNER / MEMBER roles;
- invitation model;
- one active Organization per installation in v1;
- FPP identity separate from Drive authorization;
- protected-work-safe sign-out/revocation principles;
- provider-bound Drive identities remain local.

### Phase 12B — Dedicated Supabase Authentication Architecture — COMPLETE

Settled:
- dedicated original-FPP Supabase project;
- email/password authentication;
- invitation-only v1;
- RLS-backed Organization/Membership authorization;
- Keystore-backed encrypted Android session;
- 72-hour same-Organization offline grace;
- server-side privileged member/invitation actions;
- no job/photo mirror.

### Phase 12C — Dedicated FPP Supabase Foundation — COMPLETE

Delivered:
- Organizations, Memberships, Invitations;
- explicit grants + RLS;
- first real Owner and Organization;
- hosted isolation tests;
- Team separation.

### Phase 12D — Android Auth / Session Foundation — COMPLETE

Delivered and physically proven:
- email/password sign-in;
- recovery deep link;
- password reset;
- fresh sign-in after recovery;
- Auth User → ACTIVE Membership → Organization validation;
- rotating refresh-token persistence;
- encrypted session restore;
- Account / Recheck Account;
- Samsung recovery/restart/recheck gate;
- no hard startup authorization gate yet.

## Remaining execution sequence

Default order:

**12F Owner/member administration → 12H Organization/Drive binding protection → 12G First-run/new-device flow → 12I App Status & Diagnostics → 12J Recovery/account-state UX → 12K Production identity/release path → 12L Clean-install/new-user reality gates → 12M Phase 12 closeout**

Design work may overlap only where dependencies are explicit and runtime ownership does not overlap.

---

# Phase 12E — Runtime Authorization Enforcement

Status: **COMPLETE — merged to main 2026-09-25**

## Goal

Turn the proven 12D session layer into the actual authorization gate for routine FPP use while preserving rural/offline field usability.

## Required behavior

Online startup/resume:
1. restore encrypted session;
2. refresh Auth session when required;
3. validate current Auth User;
4. validate authoritative Membership + Organization;
5. persist the successful validation time;
6. allow ordinary same-Organization field work.

A successful Membership validation resets the 72-hour grace clock.

Temporary account-service failure:
- is not revocation;
- may use the last successfully validated ACTIVE Membership for the same Organization;
- allows ordinary work only while the last successful validation is less than 72 hours old.

Within grace:
- normal local capture/preparation remains allowed;
- existing Drive workflow may continue when Android/Drive access itself works;
- Organization switching is blocked;
- membership/invitation administration is blocked.

After 72 hours without successful revalidation:
- protected originals and queue/reconciliation evidence remain available;
- viewing/recovery/diagnostics remain available;
- new capture is blocked;
- new remote Drive mutations are blocked;
- ordinary work resumes automatically after successful Membership revalidation.

Authoritative REVOKED:
- ends grace immediately once learned;
- blocks new ordinary Organization work;
- blocks new Drive writes;
- preserves all protected local/reconciliation evidence;
- never rewrites destinations or deletes Drive content.

Sign-out:
- remains blocked while unresolved/protected work would be stranded;
- when allowed, clears FPP auth/session and active identity navigation state;
- does not delete photos, queue evidence, Drive data, or Drive sharing;
- does not make an existing SAF grant valid for a different FPP Organization.

## Engineering shape

Use one central authorization decision owner derived from:
- current encrypted session;
- last successful Membership validation timestamp;
- authoritative Membership result when available;
- current time;
- protected/unresolved local-work state.

UI, capture, and Drive actions consume that decision; they do not each invent their own 72-hour/revocation logic.

Do not create a second persistent account-state machine when the decision can be derived from existing authoritative/session data.

Refresh/revalidation must remain serialized so rotating refresh tokens cannot be consumed in parallel.

## 72-hour test rule

Do **not** wait three real days.

The policy must be tested with a controllable/injected clock in automated tests, including:
- just inside the grace boundary;
- exact boundary behavior;
- just outside the boundary;
- successful revalidation resetting the timestamp;
- backend/network failure not becoming revocation;
- authoritative revocation overriding remaining grace.

The production app must not expose a user-accessible clock override or test backdoor.

## Provisional change level

**Level 3**

Reason:
authorization now gates capture and remote Drive writes, and sign-out/revocation can affect whether protected work is allowed to progress.

## Verification boundary

Automated:
- focused policy tests;
- action-gate tests;
- session refresh/revalidation tests;
- sign-out protected-work tests;
- complete Android suite once on final runtime head.

Physical Samsung gate:
- normal online startup/revalidation;
- temporary offline use inside a valid grace window;
- return online and automatic successful revalidation;
- sign-out blocked when protected/unresolved work exists, if a disposable safe fixture can prove it without creating ambiguity;
- normal existing Drive/photo workflow remains intact.

The physical gate does not need to wait 72 real hours; the exact time boundary is an automated policy claim.

---

# Phase 12F — Owner / Member Administration

## Goal

Allow ACTIVE Owners to manage Organization access without putting privileged Supabase credentials in Android.

## Scope

Owner capabilities:
- view same-Organization Members/Owners needed for administration;
- view pending invitations;
- invite a person as MEMBER or OWNER;
- resend/reuse an existing valid pending invitation rather than create duplicates;
- cancel a pending invitation;
- change MEMBER ↔ OWNER;
- revoke/reactivate Membership where allowed;
- prevent actions that would leave the Organization with no active Owner.

Member capabilities:
- normal field workflow;
- no Organization membership/invitation administration.

## Backend rules

Privileged mutations remain server-side through narrowly scoped Edge Functions/private database functions.

Every privileged action must:
- authenticate the caller;
- validate exact Organization;
- require ACTIVE OWNER where appropriate;
- be retry-safe/idempotent;
- enforce one Membership per Organization/User;
- avoid duplicate pending invitations;
- preserve Organization isolation.

Android never receives a service-role/secret key.

Supabase's hosted email may remain acceptable for internal low-volume testing. Production-suitable email delivery is handled by 12K before outside-user/public distribution.

## Provisional change level

**Level 3**

Reason:
changes authoritative Membership/Invitation state and can revoke authorization.

## Verification boundary

- disposable Supabase Auth identities;
- Owner/member/wrong-Organization authorization tests;
- duplicate invite/idempotency tests;
- last-active-Owner protection;
- invitation cancellation/expiration behavior;
- complete Android/backend verification for the final runtime/backend head;
- no live customer/job data required.

---

# Phase 12G — First-Run / New-Device Flow

## Goal

Make a new install understandable and safe without assuming any Drive identity is portable from another phone.

## Normal returning/new-device sequence

`Install/open → Sign in → validate ACTIVE Membership → establish active Organization → detect no local Drive binding → connect/select approved Drive workspace through Android SAF → select Client Company → normal field workflow`

## Invited-user sequence

`Install/open → sign in / accept invitation → activate/validate Membership → establish active Organization → connect Drive separately on that device → normal field workflow`

## Rules

- no Drive workspace is selected merely because FPP sign-in succeeded;
- no Google account is inferred from Auth email;
- no provider ID/SAF URI is restored from another device;
- backup exclusions remain authoritative;
- invitation acceptance does not grant Drive sharing;
- multiple active Membership handling must fail closed until an explicit Organization-selection design is needed/approved;
- public Organization self-creation is not introduced here.

## Provisional change level

**Level 2 or Level 3 depending on implementation**

If this slice only orchestrates existing auth and existing SAF selection without changing persisted Drive-binding semantics, Level 2 may be sufficient.

Any new persisted Organization↔Drive binding or provider/account-selection semantic is Level 3 and belongs with 12H.

## Verification boundary

Most flow logic should be automated without wiping the operator's real phone state.

The final clean-install/new-user reality claim belongs to 12L so development does not repeatedly destroy/rebuild device state.

---

# Phase 12H — Organization ↔ Drive Binding Protection

## Goal

Prevent a valid FPP account from accidentally operating against a Drive workspace that belongs to another local FPP Organization context.

## Required behavior

The local Drive binding must be provably associated with the active FPP Organization.

Rules:
- same Organization + valid local binding may reuse it;
- different Organization may not silently reuse an old binding;
- a persisted Android SAF grant alone is insufficient authorization for a different FPP Organization;
- changing FPP Auth identity does not rewrite stored provider IDs;
- unresolved/protected work blocks Organization switching when it could cross identity boundaries;
- queued-photo immutable destinations remain unchanged;
- FPP Auth email and Drive account email may differ and must not be compared as an authorization rule.

If an Organization requires a different workspace, the user deliberately reconnects/selects it through the existing Android SAF workflow.

## Data/minimalism rule

Add only the minimum local Organization binding needed to prove ownership of the existing Drive workspace selection.

Do not mirror Drive provider IDs into Supabase.

## Provisional change level

**Level 3**

Reason:
touches persisted Drive-binding/account-selection safety and could otherwise allow work to reach the wrong business workspace.

## Physical gate

Use only a disposable Drive test context.

Prove:
- valid same-Organization reuse;
- different Organization cannot silently inherit binding;
- reconnect path uses Android SAF deliberately;
- existing queued-photo destinations are unchanged;
- unrelated Drive content is untouched.

---

# Phase 12I — App Status & Diagnostics

## Goal

Give the operator one read-only place to understand account, offline-grace, Drive, and protected-work state without exposing sensitive internals.

## Local display may include

- signed-in / signed-out;
- active Organization;
- Membership role/status;
- last successful Membership validation;
- online validated / offline grace / revalidation required / revoked state;
- approximate offline grace remaining;
- Drive workspace connected/not connected;
- current Client Company where useful locally;
- queue counts;
- FAILED / UNCERTAIN counts;
- protected-original count;
- app version;
- camera permission.

## Copied support status must exclude by default

- access/refresh tokens;
- passwords;
- customer addresses;
- photos;
- Google account email;
- provider document IDs;
- SAF URIs;
- client-sensitive notes/content.

## Ownership rule

Diagnostics consumes state owned by 12E/12H and the existing queue/Drive modules.

It must not create a second authorization or Drive state machine.

## Provisional change level

**Level 2**

Read-only presentation unless implementation discovers a need for a higher-risk action, which must be split out.

## Parallelization

The diagnostics screen can be designed while 12E is being implemented.

Runtime implementation should wait until the 12E authorization-state owner is stable enough to avoid duplicate logic.

---

# Phase 12J — Recovery & Account-State UX

## Goal

Make blocked/degraded states understandable and recoverable without asking the operator to interpret backend terminology.

## Required states/guidance

At minimum:
- temporarily offline but still inside grace;
- grace expired / revalidation required;
- Membership revoked;
- no ACTIVE Membership;
- session unreadable/expired and sign-in required;
- Drive workspace disconnected/unavailable;
- sign-out blocked by protected/unresolved work;
- unresolved protected work after authorization is lost;
- manual Recheck Account path.

Messages should explain:
1. what is currently safe;
2. what is blocked;
3. what data remains protected;
4. the single next action when one exists.

## Safety

Recovery UX must never:
- delete protected work to make an account error disappear;
- silently redirect queued work;
- automatically choose another Organization/workspace;
- encourage blind retry of UNCERTAIN uploads.

## Provisional change level

**Level 2** for presentation and navigation around already-settled behavior.

Any new mutation/recovery action that changes protected data, authorization, or Drive identity must be separately classified.

---

# Phase 12K — Production Identity / Release Path

## Goal

Make the proven identity system capable of a controlled production release without forcing a Play Store or public-release decision.

## Scope

- separately secured production signing path;
- preserve package/update continuity intentionally;
- production application ID remains `com.inandout.fieldphotoprep`;
- production auth callback remains `com.inandout.fieldphotoprep://auth-callback` until a verified HTTPS App Link is deliberately adopted;
- validate production callback configuration;
- keep production private signing material out of the repository;
- define controlled production APK/update delivery;
- confirm release build contains only publishable Supabase credentials;
- production-suitable Auth email/SMTP becomes required before outside-user/public distribution;
- revisit verified Android App Links when an owned HTTPS domain exists and external distribution makes that worthwhile.

## Distribution rule

Phase 12K prepares a safe release path.

It does not decide:
- Play Store vs direct/private distribution;
- subscription/licensing;
- public self-signup.

Those remain separate product decisions.

## Provisional change level

**Level 3**

Signing/deployment/update changes require explicit pre-merge approval and post-install verification.

---

# Phase 12L — Clean-Install / New-User Reality Gates

## Goal

Prove the assembled identity flow on real Android without repeatedly using destructive device resets during earlier subphases.

## Required claims to prove

Using the safest available physical test arrangement:
- clean/no-session launch requires correct FPP authentication;
- new/returning User loads only authorized Organization identity;
- no Drive binding is silently inherited from FPP sign-in;
- Drive workspace connection remains a deliberate Android SAF action;
- invited Member/Owner path works end-to-end when that feature is enabled;
- sign-out/sign-in does not cross Organization/Drive boundaries;
- existing protected work remains safe through tested account transitions;
- production/internal package behavior is consistent with the intended release path.

## Second-phone relationship

Phase 8C's second-Android-phone/shared-master portability gate remains separate evidence.

If a suitable second phone is available, combine overlapping observations so the project does not repeat the same shared-master test twice.

If no suitable second phone is available:
- do not simulate provider-ID portability;
- do not claim cross-device/account portability as field-proven;
- record the remaining Phase 8C limitation explicitly.

Phase 12 may still verify identity/release behavior that does not depend on claiming portable provider IDs.

## Change level

Primarily a device/evidence gate.

Any defect fix discovered here returns to a separately governed runtime branch at the appropriate change level.

---

# Phase 12M — Account / Privacy / Release Closeout

## Goal

Reconcile the final implementation against the identity model, contracts, backend, Android runtime, and actual device evidence before declaring Phase 12 complete.

## Final review

Repository/runtime:
- no service-role/secret credentials in Android or repository runtime;
- no Team project credentials/data reuse;
- no stale localhost callback dependency;
- encrypted auth state still excluded from Android backup/device transfer;
- no duplicate authorization state machines;
- no unnecessary framework/backend expansion.

Supabase:
- RLS enabled and scoped correctly;
- explicit grants remain minimal;
- Owner/member/invitation privileged paths are server-controlled;
- security advisor reviewed;
- disposable test identities/data cleaned where appropriate;
- production email delivery status documented for the actual distribution model.

Drive/photo safety:
- identity changes did not alter immutable queued destinations;
- revocation/sign-out did not delete protected work;
- Organization/Drive binding prevents silent cross-Organization reuse;
- unresolved remote state still follows existing FAILED/UNCERTAIN rules.

Privacy:
- backend contains only identity/membership/invitation data required by the approved model;
- diagnostics/support export excludes sensitive provider/token/customer data by default;
- account close/revocation does not imply Drive business-data deletion.

Documentation:
- roadmap reconciled;
- all Phase 12 records reflect actual merged behavior/evidence;
- known deferred limitations remain explicit.

## Completion rule

Phase 12 becomes **COMPLETE** only after:
- all required 12E–12L implementation/evidence gates for the supported claims pass;
- final automated regression passes on the exact final runtime head;
- required Level 3 approvals are recorded;
- 12M source-of-truth/security/privacy review is complete.

---

# Dependency and parallel-work map

## Must remain sequential

- 12E must establish the authorization-state owner before later screens/actions rely on it.
- 12H must settle Organization↔Drive binding before final first-run/recovery/release claims.
- 12L runs after the relevant runtime slices are assembled.
- 12M is last.

## May overlap safely

While 12E runtime is being built/staged:
- design 12I diagnostics against the planned 12E state outputs;
- design 12F Owner/member administration and its server API;
- design 12K production signing/email/release requirements.

After 12E merges:
- 12F implementation may proceed;
- 12I implementation may proceed once it consumes the single 12E state owner;
- 12G flow design may proceed using settled invitation behavior;
- 12K planning may continue without modifying runtime signing until ready.

Do not run multiple Android runtime branches that independently change startup/auth/Drive-binding ownership and then attempt to reconcile them later.

# Testing strategy across Phase 12

Use the phase-staging doctrine:

**build everything honestly provable without a phone → stage once at the genuine device boundary → run the smallest required phone gate → accept evidence → continue.**

Do not:
- wait 72 real hours to test the grace policy;
- repeat successful recovery/sign-in tests merely for confidence;
- repeatedly install historical APKs when the integrated build contains the behavior;
- use live customer work as an identity/Drive-boundary test surface;
- rerun full CI after documentation-only changes;
- create test backdoors in production runtime to manufacture account states.

Use:
- deterministic clock-based policy tests;
- disposable Supabase users/Organizations/Invitations where needed;
- emulator/instrumentation for Android lifecycle and local-state behavior;
- safe disposable Drive fixtures only where real DocumentsProvider behavior matters;
- one final complete automated suite on each runtime subphase's exact final head;
- proportional physical Samsung gates only for behavior that truly crosses the real Android/email/Drive/signing boundary.

# Scope intentionally excluded from Phase 12

Do not add these merely because identity now exists:

- subscriptions/licensing/paywalls;
- employee scheduling;
- route planning;
- workbook/FMR integration;
- Supabase Storage or Realtime;
- cloud job/photo synchronization;
- Google social login;
- public self-signup;
- permanent photo library;
- OCR/AI classification;
- background location;
- generalized cloud admin portal;
- extra role hierarchy beyond OWNER/MEMBER.

Each requires separate evidence and approval.

# Immediate next step

Create the dedicated **Phase 12E design + Level 3 impact record** from this master plan and the already settled 72-hour/revocation/sign-out rules.

Then implement 12E in the largest safe testable chunk practical, stopping only at the next genuine physical-device boundary.
