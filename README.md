# Field Photo Prep

Lean Android app for getting field-work photos into the correct Google Drive work-order folder.

## Governed Drive workflow

```text
Approved master folder
└── Address
    └── Work Order - YYYY-MM-DD
        └── Photos
```

Example:

```text
HNP
└── 1607 Crestview Drive
    ├── Cut Grass - 2026-09-06
    └── Winterization - 2026-11-15
```

Google Drive is the long-term source of truth. Field Photo Prep may remember folder names/IDs and temporary upload state, but it is not a second photo library.

Old dated work-order folders may be reused under controlled rules. Empty same-work-order folders can be renamed to a new date. A non-empty old folder may be cleared and reused only through an explicit **Clear & Reuse** action with operator confirmation; the app does not perform general automatic Drive cleanup.

## Phase 1 — Drive folder foundation

Phase 1 is intentionally read-only and contains only:

- Android app shell;
- explicit Google Drive authorization;
- master-folder selection under My Drive;
- persistent master-folder Drive ID/name;
- refresh/display of direct address-folder children.

Phase 1 does **not** contain camera capture, work-order creation, photo upload, folder reuse, or any Drive write/delete behavior.

### Android / Google setup

1. Use a Google Cloud project with the Google Drive API enabled.
2. Configure the OAuth consent screen and add the intended test account while the app is in testing.
3. Create an **Android OAuth client** for package `io.github.timbone72cc.fieldphotoprep` using the SHA-1 of the signing certificate used for the installed build.
4. Open the project in Android Studio or build it with JDK 17, Android API 37, and Gradle 9.6.0.

No OAuth client secret or access token belongs in this repository.

See `docs/PHASE_1_DRIVE_FOUNDATION_IMPLEMENTATION_RECORD_2026-09-06.md` for the Level 3 scope, permission boundary, safe test plan, and merge gate.
