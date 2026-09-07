# Phase 1 Implementation Record — Master Folder Connection

Date: 2026-09-06

## Approved scope

Phase 1 only:

- create the Android app shell;
- let the operator choose one master folder through Android's system folder picker;
- persist access to that selected folder;
- persist its provider document identity and display name locally;
- list and refresh the address folders directly beneath that master folder.

Explicitly excluded: camera, photo preparation, photo upload, work-order folder creation/reuse/deletion, Free Map Router, workbook integration, and route behavior.

## Architecture change from the first Phase 1 attempt

The initial build used Google OAuth plus the Drive REST API. Real-device testing showed that this added account/OAuth configuration that was not needed for the intended Android-only field workflow.

The operator approved replacing that approach with Android's Storage Access Framework (SAF) folder picker. Android/Google Drive owns account selection and folder browsing. Field Photo Prep receives a persisted URI permission only for the operator-selected master folder tree.

This removes:

- Google OAuth client configuration;
- Drive REST access tokens;
- account-wide Drive metadata permission;
- the Google auth dependency;
- the app's own Drive browser; and
- the app INTERNET permission for Phase 1.

## Change level

Level 3 because master-folder authorization and identity semantics changed.

## Permission decision

The picker requests persisted read/write tree permission for the folder the operator explicitly selects. Phase 1 performs reads only; no Drive create, rename, move, delete, upload, or permission write exists in this phase.

The tree grant is narrower than account-wide Drive authorization because Field Photo Prep cannot browse outside the selected tree through this grant. Write capability is requested with the tree so later already-approved folder/photo operations can remain inside the same selected master tree without introducing broader Drive account access.

Each Android phone grants its own access. Different operators may use different Google accounts as long as each account can access the same shared master folder.

## Identity

- The persisted tree URI identifies the approved master tree on that device.
- The document provider's stable document ID is the folder identity used by the app inside that tree.
- Display name is context only and may change without changing the saved tree identity.
- Same-named folders remain distinct because their document identities differ.

## Ownership

- `MainActivity.java`: Phase 1 UI, system folder picker launch/result, master selection, refresh.
- `DriveClient.java`: SAF document-provider reads for selected master and direct child folders.
- `FolderPrefs.java`: local persisted master tree URI, document ID, and display name.
- `DriveFolder.java`: folder document ID/name value object.

## Read/write surfaces

Reads:

- selected tree folder identity/name;
- direct child folder identities/names;
- local saved tree URI/document ID/name.

Writes:

- local saved tree URI/document ID/name only.

No remote Drive content writes occur in Phase 1.

## Protected behavior

- Android's system picker is the only general folder browser.
- Field Photo Prep is scoped to the operator-selected master tree.
- The app lists folders only; ordinary files/photos are not presented as address folders.
- Reopening the app reuses persisted master-tree access when Android still grants it.
- Expired/lost access stops refresh and requires the operator to choose the master again.
- No camera or upload behavior is introduced.

## Verification

Focused automated coverage verifies that only directory MIME types are treated as address folders.

Final gate before merge:

1. `gradle test`;
2. `gradle assembleDebug`;
3. Android emulator launch smoke test;
4. inspect the branch diff;
5. on the operator's Android phone, choose the real safe/shared master folder through the system picker, verify its direct address folders appear, restart the app, and verify persisted access still works.

Phase 1 must not be merged until the real-device master-folder reality check succeeds and the operator explicitly approves the Level 3 merge.

## Rollback

Governed base / rollback commit: `fd992ac4cb44b725d3eae1681622531d74fec651`.
