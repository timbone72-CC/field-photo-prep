# Phase 12 Identity Model v1 — Level 3 Impact Record

Date: 2026-09-21

Status: **DESIGN APPROVED — IMPLEMENTATION NOT STARTED**

## Exact user-facing problem

FPP currently has Drive destination identity but no governed model for identifying the human operator or the field-service business they operate for.

The app is expected to support more than one user and potentially more than one business over time, while continuing to use Google Drive as the authoritative store for client companies, properties, work orders, and photos.

Without a recorded identity boundary, later sign-in work could drift into:
- treating email as permanent identity;
- conflating FPP login with Android Drive authorization;
- duplicating client/work-order data in an account backend;
- making provider-bound Drive IDs portable across devices/accounts;
- weakening the existing immutable-destination and protected-photo model.

## Approved design scope

Record Identity Model v1 only.

This change authorizes no runtime account implementation yet.

The model defines:
- User;
- Authentication Identity;
- Organization;
- Membership;
- Owner/Member roles;
- Invitation;
- device-local Drive Binding;
- separation of FPP identity from Google Drive identity;
- current personal-Drive and future Shared-Drive compatibility;
- one active Organization per installation;
- offline identity principle;
- revocation/sign-out/account-lifecycle safety;
- backend/privacy boundaries.

## Owning files

Design/governance only:
- `docs/IDENTITY_MODEL_V1.md`;
- `docs/PHASE_12_IDENTITY_MODEL_IMPACT_2026-09-21.md`;
- `CONTRACT.md`;
- `docs/ROADMAP.md`.

No runtime source file is authorized to change in this record.

## Read surfaces

Current contracts and design inputs:
- `AGENTS.md`;
- `CONTRACT.md`;
- `CHANGE_CONTROL_CONTRACT.md`;
- `INTEGRATION_CONTRACT.md`;
- `docs/PHASE_STAGING_DOCTRINE.md`;
- `docs/ROADMAP.md`;
- current multi-company Drive behavior;
- current Android backup/restore policy;
- the recorded business-Drive migration audit.

## Write surfaces

Repository documentation/contracts only.

No:
- Android preferences;
- local photo records;
- Drive folders/files;
- Drive permissions;
- account backend;
- authentication provider;
- schema migration;
- package/signing configuration.

## Required data for later implementation

The model requires an implementation to be capable of representing:
- permanent User identity;
- permanent Organization identity;
- Membership joining User ↔ Organization;
- Membership role/status;
- Authentication Identity joining a login provider/account ↔ User;
- invitation identity/status;
- secure authentication/session state.

## Optional data

May be added only when needed:
- User display name;
- invitation expiration;
- last successful Membership validation timestamp;
- account recovery metadata;
- Organization display preferences that are truly Organization-scoped.

Customer addresses, photos, work orders, Drive IDs, and SAF URIs are not identity-backend requirements.

## Schema / identity / permission / platform-access changes

This design introduces future FPP account identities conceptually, but this documentation change does not alter runtime schema or permissions.

Future implementation must preserve the distinction:
- FPP account authorization controls User/Organization Membership;
- Android SAF/Drive provider access controls Drive workspace access.

FPP sign-in must not replace the current Android Drive access model without a separate Level 3 integration change.

## Master-folder and destination assumptions

- Google Drive remains authoritative for Client Company, property, work-order, and photo content.
- Current production may remain on the personal Drive while Identity v1 is designed/implemented.
- A later business Shared Drive migration changes local Drive Binding/provider identity, not FPP User/Organization identity.
- Existing queued photo destinations remain immutable.
- Provider IDs from one account/provider context must not be copied into another as if portable.

## Duplicate / idempotency behavior

Identity v1 requires permanent opaque IDs for User, Organization, Membership, and Invitation so repeated login/setup operations do not depend on mutable display names or email addresses.

Later implementation must explicitly prevent:
- duplicate Organization creation on ordinary returning-user login;
- duplicate Membership activation from repeated invitation acceptance;
- treating the same email string as sufficient proof that two FPP Users are identical.

Exact implementation mechanics are deferred to the authentication architecture.

## Offline and stale-state behavior

The settled principle is:
- temporary loss of account-service connectivity must not break safe ordinary field capture for a previously authenticated/validated active Membership;
- stale identity state must not authorize cross-Organization switching or destructive account changes indefinitely;
- once revocation is successfully learned, new ordinary Organization work must stop;
- revocation/sign-out must not destroy protected local photos or rewrite destinations.

Exact revalidation/grace intervals are deferred.

## Safe Drive fixture plan

Identity implementation must not use live HNP/Tresmolino/Tasre customer work to prove account/Drive-boundary behavior.

Use:
- disposable local/emulator account fixtures for provider-independent identity logic;
- the existing disposable FPP multi-company Drive fixture or a new explicitly disposable business-side fixture for any later real Android/Drive reality gate.

Do not alter production workspace/account selection during design-only work.

## Baseline and expected verification

For this documentation/contract change:
- mandatory contract read completed;
- documentation diff/contract review required;
- no runtime test is required because runtime code is unchanged.

For future implementation:
- focused User/Organization/Membership/invitation/session tests;
- account-vs-Drive identity separation tests;
- offline/stale/revocation tests;
- existing complete Android suite on the final runtime head;
- proportional real-device gate for any behavior that crosses Android sign-in/Drive provider boundaries.

## Primary risks

- identity terminology colliding with existing Drive "company" terminology;
- treating Google email as permanent User identity;
- treating FPP Google sign-in as Drive authorization;
- silently porting SAF/provider IDs between accounts or devices;
- sign-out/revocation destroying or stranding protected work;
- growing the identity backend into a second operational job database.

## Failure recovery

This design changes no runtime behavior, so rollback is repository-only: revert the governance commit before any implementation depends on it.

Once implementation begins, each subphase must define its own data/session migration and rollback behavior.

Drive/photo safety rules remain authoritative throughout.

## Rollback point

Base commit before this design branch:

`d72b62dcc1eb47437b4e1be11a8b778b858cd7ba`

## Affected smoke checks

None for this documentation-only design record.

Future identity implementation must preserve all existing field-flow smoke checks and add only proportional identity/account checks.

## Explicit pre-merge approval status

**APPROVED — 2026-09-23.**

The operator gave explicit pre-merge approval for PR #61 after review of the exact governed diff. This satisfies the Level 3 pre-merge approval gate.
