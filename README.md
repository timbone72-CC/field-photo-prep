# Field Photo Prep

Field Photo Prep is a company-neutral Android app for preparing field-work photos for faster upload while protecting the original photos.

## Initial purpose

The first release is intentionally small:

1. select one or more existing photos;
2. create resized copies, with 60% as the default resize target;
3. preserve supported date/time and GPS/location metadata when available;
4. keep original filenames when possible without overwriting existing output files;
5. save resized copies separately from the originals; and
6. let the operator share or upload those copies through Android's normal workflow, including Google Drive.

The app does not require a remote server, subscription, company-specific account, or Play Store distribution for the initial release. Direct APK installation is the approved starting method.

## Non-negotiable photo safety rule

Original field photos are read-only source evidence. Field Photo Prep must never overwrite, resize in place, rename, move, delete, or replace an original photo. Processing failures must leave originals unchanged.

## Governing documents

Development is governed by:

- `AGENTS.md` — repository guardrails and required working process;
- `CONTRACT.md` — approved product behavior and protected photo rules;
- `CHANGE_CONTROL_CONTRACT.md` — change levels, approvals, rollback, and merge rules;
- `TESTING_CONTRACT.md` — required development and release verification; and
- `REGRESSION_CHECKLIST.md` — practical checks used to prove protected behavior still works.

Read the governing documents before changing runtime behavior, Android permissions, storage handling, metadata handling, build configuration, signing, or release behavior.

## Development workflow

Runtime work must be developed on a branch rather than directly on `main`. Changes use the smallest honest Level 1, Level 2, or Level 3 classification defined by the change-control contract. Focused tests are used during development, and required final verification must pass before merge or release.

The initial contract foundation must be reviewed and approved before Android runtime code is added.
