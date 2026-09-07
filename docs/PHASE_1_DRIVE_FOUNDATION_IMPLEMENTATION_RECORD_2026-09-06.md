# Phase 1 Drive Folder Foundation — Implementation Record — 2026-09-06

## Change classification

**Level 3 — high risk.** Phase 1 introduces Google authorization and master-folder identity. It is intentionally read-only against Google Drive.

Explicit pre-merge operator approval: **PENDING**.

## User-facing problem

Field Photo Prep needs a lean first foundation that can connect to the operator's Google Drive, select one master folder, remember its exact Drive folder ID, and show the address folders directly beneath it. Phase 1 must not add camera capture, work-order creation, photo upload, folder rename, deletion, or recycling.

## Approved Phase 1 behavior

1. Open a minimal Android app shell.
2. Explicitly connect/choose a Google account for Drive authorization.
3. Request metadata-read-only Drive access only.
4. Browse folders under **My Drive** and choose one master folder.
5. Persist only the selected master folder's exact Drive ID and display name.
6. Refresh and display direct child folders of that exact master folder as address folders.
7. Preserve the stored master-folder identity across app restart.
8. Keep the OAuth access token in memory only; reconnect when a token is unavailable or expired.

## Explicitly outside Phase 1

- camera preview or capture;
- photo preparation or local photo queue;
- work-order folder browsing, creation, rename, recycling, or deletion;
- photo upload;
- Drive writes of any kind;
- workbook or Free Map Router integration;
- Shared Drive navigation;
- background synchronization;
- permanent access/refresh-token storage.

## Owning files and responsibilities

- `MainActivity.kt`: Phase 1 operator UI and state rendering only.
- `DriveAuthorizationManager.kt`: Google Identity Services authorization request and result parsing.
- `DriveApiClient.kt`: read-only Drive `files.list` requests and response parsing.
- `DriveQuery.kt`: exact direct-child-folder query construction.
- `DriveFolderPickerDialog.kt`: folder-only navigation under My Drive for master-folder selection.
- `MasterFolderStore.kt`: lightweight persistent master-folder ID/name cache.
- `DriveModels.kt`: folder identity value objects.

UI code does not construct Drive queries or persist tokens. Drive code does not own UI state.

## Read and write surfaces

### Google Drive reads

- folder metadata only: `id`, `name`, and folder MIME type;
- direct folder children for the folder currently being browsed;
- direct address-folder children of the selected master folder.

### Google Drive writes

**None in Phase 1.** No create, rename, move, delete, permission, upload, or content-read request is implemented.

### Local writes

- exact master-folder Drive ID;
- master-folder display name.

OAuth access tokens are not written to SharedPreferences, logs, backups, or repository files.

## Authorization scope

Phase 1 requests only:

`https://www.googleapis.com/auth/drive.metadata.readonly`

The request opts out of automatically including previously granted scopes and asks the operator to select a Google account. This scope is used because the approved workflow must discover existing address folders created outside Field Photo Prep. A `drive.file`-only token does not reliably provide metadata access to arbitrary pre-existing descendants merely because a parent folder is known.

This is a restricted Google OAuth scope. Production distribution may therefore require Google OAuth verification. No broader Drive-content scope is requested.

## Required and optional data

Required:

- a Google account authorized for Drive metadata read access;
- an accessible master folder in the account's My Drive hierarchy;
- stable Drive folder IDs returned by Google.

Optional:

- locally remembered master-folder display name for user context.

No photo data exists in Phase 1.

## Master-folder and identity assumptions

- Drive folder ID is authoritative identity; visible name is context only.
- A renamed master folder remains the same master when its ID is unchanged.
- Phase 1 folder navigation starts at My Drive root and traverses folders only.
- Shared Drive navigation is intentionally deferred unless the real operator workflow requires it.
- Address refresh is an exact direct-child query using the stored master folder ID.

## Duplicate and idempotency behavior

Phase 1 performs no Drive create/update/delete operations, so remote write idempotency is not applicable. Duplicate visible address-folder names may be displayed because Drive IDs, not names, are identity; Phase 1 does not select or merge address folders.

## Offline and stale-state behavior

- The stored master-folder ID/name remains available locally while offline.
- Authorization and folder refresh require network access.
- A failed refresh does not clear or substitute the stored master folder.
- HTTP 401 clears only the in-memory token and asks the operator to reconnect.
- The app never silently chooses another master folder after a read failure.

## Safe Drive fixture plan

Before merge, use a disposable test folder in the operator's Google Drive, not a live field job:

1. Create a safe test master folder with two test address subfolders.
2. Install the Phase 1 build on a supported Android device/emulator with Google Play services.
3. Connect the intended Google account.
4. Navigate to and select the safe master folder.
5. Confirm both real test address folders appear.
6. Restart the app and confirm the exact master folder ID/name remains stored.
7. Reconnect and refresh; confirm no Drive item was created, renamed, moved, deleted, or shared.
8. Rename the test master in Drive, then confirm the stored ID still targets that same folder after reconnect/refresh; the cached display name may remain stale until a future metadata-refresh feature because Phase 1 does not refresh master-name metadata independently.

Reality-gate status: **PENDING real-device safe-folder execution.** Mock/unit tests do not satisfy this gate by themselves.

## Focused automated tests

- exact master-folder Drive ID/name persistence;
- same ID surviving display-name replacement in local cache;
- incomplete local master identity rejected;
- direct-child-folder query contains exact parent ID + folder MIME type + non-trash guard;
- query literal escaping;
- Drive folder-page metadata parsing and malformed-row rejection.

## Final automated gate

On the final runtime head:

- `gradle testDebugUnitTest`
- `gradle assembleDebug`

CI may satisfy the complete automated-suite gate when it runs against that exact head.

## Affected regression checks

Use only:

- `REGRESSION_CHECKLIST.md` section A: app launch/folder state, where implemented in Phase 1;
- section B: Google account/master folder;
- section C: address-folder discovery portions implemented in Phase 1;
- section L: permissions/destructive behavior, specifically read-only behavior.

Do not run camera, work-order, upload, retry, photo-retention, or Clear & Reuse checks for Phase 1 because those runtime features do not exist.

## Primary risks

1. Requesting more Google Drive access than the workflow needs.
2. Persisting a name instead of the exact Drive folder ID as identity.
3. Reading the wrong parent when refreshing addresses.
4. Treating authorization failure as permission to substitute another folder/account.
5. Accidentally introducing Drive writes while building folder discovery.

## Failure recovery

- Authorization/read failure: keep stored master identity, clear only in-memory token when unauthorized, reconnect explicitly.
- Wrong master selection: operator chooses a different master; saving the new choice replaces only the local master ID/name.
- Phase 1 regression before merge: abandon/revert the branch. No Drive cleanup is required because Phase 1 performs no Drive writes.

## Rollback point

Known governed base before Phase 1 runtime code:

`fd992ac4cb44b725d3eae1681622531d74fec651`

## Approval gate

Implementation is authorized by the operator's instruction to build Phase 1. Because this is Level 3, **do not merge the Phase 1 pull request until the safe Drive reality gate is completed and the operator gives explicit pre-merge approval.**
