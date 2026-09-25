# Field Photo Prep Source-of-Truth Reconciliation — 2026-09-24

Status: **SOURCE-OF-TRUTH RECONCILIATION — DOCUMENTATION ONLY**

## Purpose

Reconcile the original Field Photo Prep repository with the actual merged GitHub history, the separate Field Photo Prep Team repository, and the operator's past project conversations so later Phase 12 work does not inherit architecture from the wrong product.

This record changes no Android runtime code, Drive data, permissions, schema, authentication backend, or photo behavior.

## Audited sources

Original FPP:
- `timbone72-CC/field-photo-prep`
- current main at audit start: `0f2c38c8599edba733c8d9f0a06d12f796e9cbca`
- `AGENTS.md`
- `CONTRACT.md`
- `CHANGE_CONTROL_CONTRACT.md`
- `INTEGRATION_CONTRACT.md`
- `TESTING_CONTRACT.md`
- `docs/PHASE_STAGING_DOCTRINE.md`
- `docs/ROADMAP.md`
- Phase 11A/11B records and PR evidence
- Phase 12A identity record and PR evidence
- open PRs #39 and #62

Separate Team product:
- `timbone72-CC/field-photo-prep-team`
- Team `AGENTS.md`
- Team `docs/ROADMAP.md`
- merged Team PR history
- current open Team draft PRs

Conversation history:
- original FPP build and governance discussions;
- Team separation/backend discussions;
- Phase 11 phone-gate and merge approvals;
- Phase 12 identity discussions and explicit 2026-09-24 approval/reconfirmation.

## Reconciled findings

### 1. Original FPP and Team are separate products

The Team repository explicitly governs a hard boundary:

- Team uses a separate repository;
- Team uses a separate Android package/app data;
- Team has its own backend/auth/storage architecture;
- Team uses original FPP only as a proven behavioral/development reference;
- Team work must not modify or use original FPP runtime/data as a Team experiment surface.

Team Phase 0 records a **separate Supabase project** as part of that separation.

Therefore a Team backend choice is not automatically an original-FPP backend choice.

### 2. Supabase was not selected for original FPP

The original FPP repository contains no Supabase/Firebase/auth runtime dependency on main.

The approved Phase 12A identity model deliberately deferred:
- authentication/backend vendor;
- sign-in implementation;
- token/session mechanics;
- invitation delivery;
- account recovery.

Past conversation review found Supabase selected and implemented for **Field Photo Prep Team**, not an explicit operator decision selecting it for original FPP.

PR #62 incorrectly attempted to promote the Team Supabase architecture into original FPP. It was closed unmerged during this reconciliation.

### 3. Google/Continue-with-Google was discussed, not selected

Original FPP history contains earlier Google-login/OAuth proposals and later abandonment of Drive OAuth in favor of Android SAF for the Drive workflow.

The Phase 12 identity discussion also considered **Continue with Google** as a possible FPP authentication method.

No audited evidence establishes it as the approved Phase 12B authentication implementation.

The correct source of truth is therefore:

**Phase 12A identity semantics are settled; Phase 12B authentication/backend technology remains undecided until separately designed and approved.**

Android SAF remains the approved Drive authorization path regardless of the future FPP sign-in method.

### 4. Phase 11A is complete, but its documentation remained stale

Phase 11A runtime merged through PR #57.

Permanent evidence includes:
- final tested head `ed1990742d403f466151d9399a423a8fdaabbe69`;
- Android CI run `35419341385`: PASS;
- disposable real-Drive multi-company gate;
- exact company add/discovery/rename/switch behavior;
- company-scoped property/work-order creation;
- immutable queued-photo destination across company switch;
- confirmed upload to the original stored work-order destination while another company was active;
- restart/workspace persistence;
- hardened transient work-order draft isolation;
- success-banner phone verification;
- PR #57 merged to main at `66aeed51fc7065f4f18e06677e3931daf4f3977f`;
- post-merge live workspace verification proved HNP and Tresmolino switching inside the shared `Photos` workspace.

The roadmap and Phase 11A record still said `IN PROGRESS`; that is documentation drift.

### 5. Phase 11B is complete, but its documentation remained stale

The compact selector runtime merged through PR #59.

Permanent evidence includes:
- final tested head `3555e5135d659ef887060b5efbe7e3c5e86912ed`;
- Android CI run `35436040473`: PASS;
- phone review of the compact current-company selector;
- one stale/empty startup property read was corrected by the existing Refresh action with no data loss or wrong company identity;
- operator accepted the refreshed behavior and approved merge;
- PR #59 merged to main at `8e9ce51749e3f5919023f5dfc9a98a089ba4f8ed`.

The roadmap and Phase 11B record still said `IN PROGRESS`; that is documentation drift.

### 6. Phase 12A identity model remains approved

PR #61 merged the Identity Model v1 into original FPP.

The approved model remains:
- permanent User identity separate from email;
- permanent Organization identity separate from Drive;
- Membership joins User ↔ Organization;
- Owner/Member only in v1;
- Client Company folders remain Drive-side client data, not FPP Organizations;
- FPP authentication and Drive authorization remain separate;
- provider-bound SAF/Drive identities remain device/provider-context state;
- current personal Drive and later business Shared Drive use the same User/Organization model;
- sign-out/revocation/account closure never automatically destroy protected photos or Drive business data;
- the identity backend must not become a duplicate property/work-order/photo database.

The historical impact record claimed pre-merge approval on 2026-09-23. The cross-conversation audit could not independently substantiate that exact approval timing.

The operator explicitly approved/reconfirmed the Identity Model v1 on **2026-09-24**. This reconciliation records the verified approval date without changing the model.

### 7. Phase 10C and Phase 8C remain correctly open/deferred

Phase 10C remains **OBSERVING**. PR #39 stays open and must not be merged merely because the code exists. It remains dependent on fresh large-batch evidence and governed Level 3 review.

Phase 8C remains the physical second-device portability gate and is deferred until another suitable Android phone is available.

### 8. Business Shared Drive migration remains deferred

The business-Drive migration audit remains valid.

Current production stays on the existing personal-Drive `Photos` workspace until the business can justify the additional Workspace/storage cost and the permanent destination passes its governed migration gate.

No intermediate migration is required.

## Correct source of truth after this reconciliation

### Runtime

Original FPP runtime remains exactly the existing main behavior:
- Java/Android;
- CameraX;
- Android SAF/DocumentsProvider Drive access;
- multi-company workspace;
- protected originals;
- immutable destination identity;
- serialized preparation;
- sequential safe uploads;
- conservative retry/reconciliation;
- explicit destructive reuse guards.

### Identity design

Phase 12A Identity Model v1 remains approved.

### Authentication/backend

**Not yet selected for original FPP.**

Do not infer the answer from:
- Team's Supabase project;
- an old OAuth proposal;
- a Google account already used for Drive;
- an email address;
- a backend used by another project.

Phase 12B must be designed as original-FPP work and recorded before implementation.

## Open work after reconciliation

1. Phase 10C remains observation-only.
2. Phase 8C remains deferred.
3. Shared Drive migration remains deferred.
4. Phase 12B authentication/backend design is the next original-FPP design task.
5. Team development continues only in the separate Team repo under its own roadmap and governance.

## Protected behavior

This reconciliation must not change:
- Android runtime;
- camera/photo handling;
- pending queue state;
- Drive folder/file identity;
- workspace/company provider identity;
- upload/retry/reconciliation;
- Drive sharing;
- backup/restore rules;
- Team repo runtime/backend/data.

## Verification

Documentation-only:
- mandatory governance reread completed;
- GitHub PR/merge history audited;
- Team hard-boundary governance audited;
- cross-conversation decisions reconciled;
- diff review only;
- no Android runtime tests required.
