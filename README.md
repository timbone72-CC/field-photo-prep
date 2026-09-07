# Field Photo Prep

Android app for preparing field-work photos for faster upload while protecting unconfirmed photos until Google Drive confirms storage.

## Initial Drive workflow

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
