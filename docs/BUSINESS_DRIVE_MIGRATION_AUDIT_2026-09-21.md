# Business Drive Migration Audit — 2026-09-21

Status: planning/audit only. No production Drive content is moved, renamed, copied, deleted, or re-shared by this record.

## Purpose

Move Field Photo Prep (FPP) from the current personal Google Drive ownership model to a durable business-controlled Google Drive location without interrupting the existing HNP field workflow.

The migration must be staged so the current production workspace remains usable until the permanent destination has been built, tested, synchronized, and explicitly selected in the production app.

## Current production source

Current production hierarchy:

```text
personal Google Drive
└── 04 - Media
    └── Photos
        ├── HNP Jobs
        └── Tresmolino Jobs
```

Observed source inventory on 2026-09-21:

- HNP Jobs:
  - 24 direct property folders
  - 41 direct work-order folders below those properties
  - 886 JPEG photos
  - approximately 1,857.1 MiB of photo payload
  - largest observed work order: 144 photos
- Tresmolino Jobs:
  - 1 direct property folder
  - 1 direct work-order folder
  - 0 uploaded photos observed
- Tasre:
  - no production FPP company folder observed yet

The existing HNP folder remains production and must not be relocated or have its FPP provider identity changed during staging.

## Existing test fixture

A separate personal-Drive fixture already exists:

```text
FPP MULTI COMPANY TEST
├── TEST COMPANY A
└── TEST COMPANY B RENAMED
```

Use disposable/test work only for migration and provider reality checks. Do not manufacture tests inside live customer folders when the separate fixture or a new business-side disposable fixture can prove the behavior.

## Business Drive audit

The business Google account is now separately linked and can be inspected without replacing access to the personal Drive.

Observed business hierarchy includes:

```text
In And Out Cleaner Inspections LLC
├── 00 - Incoming Files
├── 01 - Company and Legal
├── 02 - Banking and Finance
├── 03 - Taxes and Accounting
├── 04 - Insurance
├── 05 - Clients and Vendors
├── 06 - Jobs and Operations
├── 07 - Contractors and Personnel
├── 08 - Vehicles Equipment and Supplies
├── 09 - Marketing and Branding
├── 10 - Templates and Forms
├── 11 - Software and Accounts
├── 90 - Archive
└── 98 - Owners Private
```

No existing business-side folder named `Field Photo Prep` was found.

Sampled business folders are ordinary My Drive items and do not currently report a Shared Drive identity.

## Permanent target

Preferred permanent ownership model:

```text
Shared Drive: In And Out Field Operations
└── Photos
    ├── HNP Jobs
    ├── Tresmolino Jobs
    └── Tasre Jobs
        └── property
            └── dated work order
                └── photos
```

This keeps FPP's existing governed hierarchy:

**approved workspace → client company → property/address → dated work order → photos**

The FPP organization is **In And Out Cleaner Inspections LLC**. HNP, Tresmolino, and Tasre are client-company folders inside the approved FPP workspace; they are not FPP organization identities.

## Workspace account requirement

The currently linked business Drive uses a `gmail.com` Google account and does not presently expose a Shared Drive.

The permanent Shared Drive should be created only after the business has a qualifying Google Workspace configuration that supports Shared Drive creation for the organization. Current Google Workspace documentation makes Shared Drive creation/membership dependent on a qualifying Business setup and a business-email/verified-domain context.

Do not migrate production into the business account's ordinary My Drive merely as an intermediate destination if the final target is a Shared Drive. Doing so would create a second provider-identity migration and an unnecessary second FPP cutover.

Fallback only if the operator deliberately abandons the Shared Drive target:

```text
In And Out Cleaner Inspections LLC
└── 06 - Jobs and Operations
    └── Field Photo Prep
        ├── HNP Jobs
        ├── Tresmolino Jobs
        └── Tasre Jobs
```

## Cutover doctrine

There is one production cutover.

Until the permanent destination passes its reality gate:

- keep the existing personal `Photos` workspace active;
- keep HNP working exactly as it works today;
- do not change the production FPP workspace/tree grant;
- do not remove existing collaborators;
- do not rewrite queued photo destinations;
- do not migrate stored SAF/provider IDs between accounts;
- do not delete or archive the old production tree.

The permanent destination receives new provider identities. FPP must rediscover/select those identities through its governed workspace/company flow rather than copying old provider IDs.

## Migration sequence

1. Establish the permanent business Google Workspace/Shared Drive environment.
2. Create the empty permanent FPP workspace and the three client-company folders.
3. Prove the Android SAF/DocumentsProvider path against the business Shared Drive using disposable data.
4. Prove company discovery/switching, property creation, work-order creation, capture, prepare, upload, restart, and exact destination preservation.
5. Copy historical HNP/Tresmolino content into the permanent destination while the old production tree remains intact.
6. Reproduce only the required HNP folder-level access on the new HNP folder. Do not expose Tresmolino or Tasre through that share.
7. Perform a final delta copy for content created in the old tree during staging.
8. Confirm the production phone has no unresolved migration blockers: no active upload, no unresolved UNCERTAIN work requiring the old destination, and no protected original whose destination cannot be safely preserved through the cutover.
9. Use the existing governed FPP **Change Workspace** flow to select the permanent business `Photos` workspace.
10. Run one small live end-to-end proof and physically verify the exact new Drive destination.
11. Keep the old personal tree intact as rollback/archive evidence until the new system has operated reliably for an agreed cooling-off period.
12. Only after stable operation, mark the old tree clearly as legacy/archive. Deletion is not part of this migration plan.

## HNP communication rule

HNP does not own the FPP Drive tree.

No HNP action is required during staging. When the new HNP folder is ready for cutover, send only the communication needed to transition them to the new folder/share. Do not ask HNP to change anything before the new business destination is field-proven.

## Tasre rule

Tasre is the third client company in the future permanent workspace.

If Tasre production work must begin before the migration is complete, create/select the Tasre company through the current governed FPP company workflow rather than manually fabricating a destination outside the app's identity rules. It can then be copied with the final migration.

## Safety / rollback

Rollback before cutover is trivial: continue using the unchanged personal production workspace.

Rollback after cutover means reselecting the last known-good workspace only if doing so does not conflict with newly queued photos whose immutable destination points to the new business workspace. Never rewrite queued destinations to make rollback appear easier.

## Change-control classification

Documentation-only Level 1 record.

Changed surface:
- migration planning documentation only.

Protected behavior:
- no runtime code;
- no schema;
- no Drive write;
- no account permission change;
- no upload/retry/reconciliation change;
- no destination/provider identity change.

Verification:
- source and destination audit results were read from the separately linked personal and business Google Drive accounts;
- diff review only.
