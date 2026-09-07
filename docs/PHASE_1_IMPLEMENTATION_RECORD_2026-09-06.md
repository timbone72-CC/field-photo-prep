# Phase 1 Implementation Record — Drive Folder Connection

Date: 2026-09-06

## Approved scope

Phase 1 only:

- create the Android app shell;
- authorize access to Google Drive;
- let the operator browse folders and choose one master folder;
- persist the master folder Drive ID and name locally;
- list and refresh the address folders directly beneath that master folder.

Explicitly excluded: camera, photo preparation, photo upload, work-order folder creation/reuse/deletion, Free Map Router, workbook integration, and route behavior.

## Change level

Level 3 because the phase introduces Google Drive authorization. No Drive write permission is requested in Phase 1.

## Permission decision

Phase 1 requests only:

`https://www.googleapis.com/auth/drive.metadata.readonly`

Reason: the app must discover pre-existing address folders beneath a selected master folder. `drive.file` can authorize a folder selected through Google Picker but does not grant the app access to enumerate that folder's existing children. Metadata-readonly is therefore the narrowest Drive scope that satisfies the approved existing-folder discovery requirement.

The app does not request photo/file content access and performs no Drive create, rename, move, delete, upload, or permission writes in Phase 1.

## Ownership

- `MainActivity.java`: Phase 1 UI, authorization flow, folder browsing, master selection, refresh.
- `DriveClient.java`: read-only Drive folder listing request.
- `FolderPrefs.java`: local master-folder ID/name persistence.
- `DriveFolder.java`: folder ID/name value object.

## Read/write surfaces

Reads:

- Drive folder IDs and names only;
- local saved master-folder ID/name.

Writes:

- local saved master-folder ID/name only.

No Google Drive writes occur.

## Protected behavior

- Drive folder ID is authoritative identity once selected.
- Folder browsing is folder-only and exists only to select the master or display its direct address-folder children.
- The app does not become a photo library or general Drive file manager.
- No camera or upload behavior is introduced.

## Verification

Focused automated coverage checks that the Drive list query is parent-scoped, folder-only, excludes trashed items, and safely escapes parent IDs.

Final gate before merge:

1. `gradle test`
2. `gradle assembleDebug`
3. inspect the branch diff;
4. perform the Drive Reality Gate on an Android device/test account before merge if OAuth credentials are available.

## Google Drive Reality Gate status

Runtime real-Drive verification requires Google Cloud configuration that is not stored in this repository:

- Google Drive API enabled;
- OAuth consent configuration including `drive.metadata.readonly`;
- Android OAuth client for package `com.inandout.fieldphotoprep` and the signing certificate used for the test build.

Until those prerequisites are configured and the real-device check succeeds, Phase 1 may be code-complete and build-tested but must not be described as live-verified.

## Rollback

Base / rollback commit: `fd992ac4cb44b725d3eae1681622531d74fec651`.
