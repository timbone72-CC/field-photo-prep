# Field Photo Prep Identity Model v1

Status: **APPROVED DESIGN — IMPLEMENTATION NOT STARTED**

This document records the settled Phase 12 identity model. It governs later authentication/account implementation together with `CONTRACT.md`, `CHANGE_CONTROL_CONTRACT.md`, and `INTEGRATION_CONTRACT.md`.

## Purpose

Add user and business identity without turning Field Photo Prep (FPP) into a second job database and without weakening the existing Google Drive destination model.

The model must work with:
- the current personal-Google-Drive production workspace;
- a later business-controlled Google Workspace Shared Drive;
- multiple client-company folders such as HNP, Tresmolino, and Tasre;
- multiple people working for one field-service business;
- offline field use;
- a future second device without silently porting provider-bound Drive identity.

## Identity boundary

FPP account identity and Google Drive identity are separate systems.

```text
FPP ACCOUNT IDENTITY                    GOOGLE DRIVE IDENTITY

User                                    Android Drive provider/account
  │                                               │
Membership                                      SAF grant
  │                                               │
Organization                              Approved Workspace
                                                  │
                                      Client Company Folder
                                                  │
                                             Property
                                                  │
                                            Work Order
                                                  │
                                              Photos
```

The FPP account system answers:

**Who is using FPP, and which field-service business are they authorized to operate for?**

Google Drive answers:

**Where do that business's client companies, properties, work orders, and photos actually live?**

Neither identity system may silently substitute for the other.

## Entities

### User

A User represents one human.

Required identity:
- permanent opaque `user_id`;
- active/inactive account status.

User-facing/profile data may include:
- display name;
- one or more login identities.

An email address is not the permanent User identity. Changing login email must not create a different person automatically.

### Authentication Identity

An Authentication Identity binds a login provider/account to one User.

Identity v1 may initially use **Continue with Google**, but the permanent FPP identity remains `user_id`, not the Google email address.

Using Google for FPP sign-in does not authorize Google Drive access for FPP's field-work workflow.

### Organization

An Organization represents the actual field-service business using FPP.

For the current operator, that Organization is:

**In And Out Cleaner Inspections LLC**

Required identity:
- permanent opaque `organization_id`;
- organization name;
- active/closed status.

Organization identity must not depend on:
- a Google email address;
- a Drive folder;
- a Drive provider ID;
- one Android device;
- one client company;
- a Google Workspace subscription.

A future migration from the current personal Drive workspace to a business Shared Drive changes the local Drive binding, not the Organization identity.

### Client Company

A Client Company is a customer/client folder inside the approved Drive workspace.

Current examples:
- HNP Jobs;
- Tresmolino Jobs;
- Tasre Jobs.

Client Companies are not FPP Organizations.

Google Drive remains authoritative for Client Company folders. Identity v1 does not require duplicate cloud account records for HNP, Tresmolino, Tasre, properties, work orders, or photos.

### Membership

A Membership connects one User to one Organization.

Required identity/state:
- permanent opaque `membership_id`;
- `user_id`;
- `organization_id`;
- role;
- status.

Initial status values:
- `invited`;
- `active`;
- `revoked`.

A revoked Membership does not delete the User and must never automatically delete protected local photos or Drive data.

### Roles

Identity v1 has only two roles:

- **Owner** — normal field workflow plus organization/member administration.
- **Member** — normal field workflow.

Multiple Owners are allowed.

Do not add Admin, Manager, Supervisor, Dispatcher, Technician, or other role layers until demonstrated workflow evidence requires them.

### Invitation

An Owner may invite a person into an Organization.

An invitation may contain:
- permanent opaque `invite_id`;
- target organization;
- invite email;
- intended role;
- status;
- expiration.

Accepting an invitation creates/activates FPP Membership. It does not grant Google Drive permission and must not silently change Drive sharing.

### Local Drive Binding

Each Android installation keeps its own local binding between the active FPP Organization and the operator-approved Drive workspace.

Conceptually the device may retain:
- `organization_id`;
- persisted SAF tree URI/grant;
- workspace provider document ID and display name;
- selected Client Company provider ID and display name;
- existing property/work-order provider identities as already governed.

The following are provider/device-context identity and must not become portable FPP cloud account identity:
- SAF/content tree URI;
- workspace provider document ID;
- Client Company provider document ID;
- property provider document ID;
- work-order provider document ID;
- photo provider document ID.

## Current and future Drive location

Identity v1 deliberately supports both the current and future storage locations.

### Current production

```text
FPP Organization:
In And Out Cleaner Inspections LLC
        │
        │ local device binding
        ▼
Current personal Google Drive workspace
Photos
├── HNP Jobs
├── Tresmolino Jobs
└── Tasre Jobs (when added through governed workflow)
```

### Future business Shared Drive

```text
SAME FPP Organization:
In And Out Cleaner Inspections LLC
        │
        │ new local device binding
        ▼
Business-controlled Shared Drive
Photos
├── HNP Jobs
├── Tresmolino Jobs
└── Tasre Jobs
```

The migration must not change:
- `user_id`;
- `organization_id`;
- `membership_id`;
- role semantics;
- invitation semantics;
- authentication identity semantics.

The new Drive location receives its own provider identities. FPP must select/rediscover them through the governed Drive workflow rather than copying old provider IDs into the new provider/account context.

## Active Organization rule

A User may eventually belong to more than one Organization, but Identity v1 allows only one active Organization per app installation at a time.

Switching Organizations must be blocked while local unresolved/protected work exists that could be confused with another Organization.

This rule is different from switching Client Companies inside one Organization. HNP ↔ Tresmolino ↔ Tasre switching remains governed by the existing immutable queued destination rules.

## Offline identity behavior

FPP is a field app. Loss of connectivity must not make normal field capture unusable.

After successful authentication and Membership validation, the app may retain enough validated identity/session state to continue safe normal field work offline for the same active Organization.

Temporary inability to reach the account service must not by itself:
- delete protected photos;
- clear queued work;
- alter Drive destinations;
- force the operator out in the middle of ordinary offline capture.

Account-administration operations may require connectivity.

The exact offline revalidation interval and credential/session technology are Phase 12 authentication-architecture decisions and are not fixed by this model.

## Membership revocation

When an active Membership becomes revoked:
- FPP must stop authorizing new ordinary Organization work once revocation is successfully learned;
- protected local originals and recovery evidence must remain protected;
- FPP must not silently redirect unfinished work;
- FPP must not rewrite stored Drive destinations;
- FPP must not blindly continue uploads after authorization is known to be revoked.

Any unfinished local work becomes an explicit recovery case.

## Sign-out

Sign-out changes FPP identity/session state. It does not delete business records.

Normal sign-out should require protected/unresolved local work to be resolved before completing unless a separately governed emergency recovery path is approved.

Sign-out may clear active navigation/session state, but must not automatically:
- delete protected originals;
- delete queue/reconciliation evidence needed for safety;
- delete or move Drive content;
- rewrite provider IDs;
- change Drive sharing.

A persisted Android SAF grant alone does not authorize a different FPP Organization to use that workspace.

## New-device behavior

A new phone deliberately does not inherit provider-bound Drive identity from another phone.

Normal sequence:

```text
Install FPP
→ Sign in
→ Identify User
→ Load/choose Organization Membership
→ No local Drive Binding exists
→ Connect/select approved Drive workspace on this device
→ Select Client Company
→ Continue field workflow
```

This preserves the existing Android backup/restore policy: provider-bound operational state is not silently transferred to another phone.

## First Owner setup

For a new FPP Organization:

```text
Sign in
→ Create Organization
→ creator becomes Owner
→ Connect Drive separately
→ select approved workspace
→ discover/create Client Companies through governed Drive workflow
```

Organization creation happens once. Drive approval remains device/platform specific.

## Invited Member setup

```text
Install/sign in
→ match/accept pending invitation
→ activate Membership
→ connect Drive separately on that device
→ normal field workflow
```

FPP Membership never silently grants Drive sharing.

## Backend data boundary

The identity/account service should remain intentionally small.

Identity v1 may store:
- Users;
- Authentication Identities;
- Organizations;
- Memberships;
- Invitations;
- authentication/session state needed to secure those records.

Identity v1 does not need to store a second cloud copy of:
- client-company folders;
- customer addresses;
- properties;
- work orders;
- inspection notes;
- photos;
- routes;
- Drive folder/file provider IDs;
- SAF URIs.

Google Drive remains authoritative for field-work content.

## Account lifecycle

A normal Member may leave or be removed from an Organization.

An Owner may revoke another Membership.

The final active Owner must not be allowed to leave in a way that strands the Organization without ownership. Ownership must first be transferred or the Organization must be explicitly closed under a separately governed account-close flow.

Closing an FPP Organization affects the FPP account relationship. It must not delete Google Drive business content.

Deleting or closing an FPP User identity likewise must not imply deletion of Google Drive records.

## Privacy boundary

The account system should know as little field-work information as practical.

Identity/account service data may include:
- User identity;
- login identity/email;
- Organization identity;
- Membership role/status;
- invitation information;
- session/security state.

It should not require customer addresses, photos, work orders, inspection notes, or Drive provider IDs merely to authenticate a User.

## Diagnostics boundary

A future read-only App Status/Diagnostics surface may locally show:
- signed-in state;
- active Organization;
- Drive connected/not connected;
- current Client Company;
- queue counts;
- FAILED/UNCERTAIN counts;
- protected-original count;
- app version;
- camera permission.

A copied support status should exclude by default:
- customer addresses;
- photos;
- Google account email;
- Drive provider IDs;
- SAF URIs;
- customer/client-sensitive content.

## Non-negotiable identity rules

1. User identity is a permanent FPP identity, not an email address.
2. Organization identity is a permanent FPP identity, not a Drive folder.
3. Client Company folders are not FPP Organizations.
4. FPP authentication never silently authorizes Google Drive.
5. Google Drive access never automatically grants FPP Organization Membership.
6. Existing stable remote/provider identities remain authoritative for Drive destinations.
7. Provider-bound Drive identities remain local/platform-context information rather than portable FPP account identity.
8. Switching Client Companies never rewrites queued-photo destinations.
9. Switching Organizations is blocked while unresolved local work could cross Organization boundaries.
10. Sign-out, revocation, and account closure never automatically delete protected photos or Drive business records.
11. Moving from the current personal Drive to a future business Shared Drive changes Drive Binding, not User/Organization identity.
12. The FPP identity backend must not become a duplicate property/work-order/photo database.
13. Google login, if used, authenticates the FPP User; Android SAF separately authorizes the Drive workspace.
14. A new device must deliberately establish its own Drive Binding rather than restoring another device's provider identity.
15. Identity implementation must preserve the existing photo-protection, destination, retry, reconciliation, and cleanup contracts.

## Deferred implementation choices

This model does not yet choose:
- authentication/backend vendor;
- exact Google sign-in technology;
- token/session duration;
- offline revalidation duration;
- invitation delivery mechanism;
- account recovery mechanism;
- organization-close UX;
- release/distribution channel;
- subscription/licensing model.

Those decisions must be designed and recorded under Phase 12 before implementation reaches them.
