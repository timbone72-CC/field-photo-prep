# Field Photo Prep Agent Guardrails

This repository builds the **Field Photo Prep** Android app. The app prepares field-work photos for faster upload while protecting the original photos.

These guardrails apply to every human or automated agent working in this repository.

## 1. Governing documents

Before changing runtime behavior, tests that define runtime behavior, Android permissions, storage behavior, metadata handling, build configuration, signing configuration, or release behavior, read:

1. `CONTRACT.md`
2. `CHANGE_CONTROL_CONTRACT.md`
3. `TESTING_CONTRACT.md`
4. the relevant sections of `REGRESSION_CHECKLIST.md`
5. `README.md` when the change affects setup, installation, or operator workflow

During the initial repository-bootstrap work, these documents may be created one at a time on a documentation branch. No Android runtime code may be added until the initial contract foundation has been reviewed and approved.

## 2. Original-photo protection is the highest-priority rule

Original field photos are source evidence and must be treated as read-only input.

Unless the governing contract is explicitly changed with operator approval, the app must never:

- overwrite an original photo;
- resize an original photo in place;
- rename an original photo;
- move an original photo;
- delete an original photo;
- replace an original photo with a resized copy;
- silently change an original photo's metadata.

The app may read an original photo and create a separate resized copy.

A processing failure must leave the original photo unchanged.

## 3. Approved baseline behavior

The initial approved product direction is:

- Android app name: **Field Photo Prep**;
- company-neutral design rather than HNP-specific behavior;
- direct APK installation is acceptable; Play Store distribution is not required for the initial release;
- batch photo selection is required;
- the default resize target is **60% of the original pixel dimensions**;
- additional resize choices may be added later without changing the original-photo protection rule;
- resized copies are written to a separate **Field Photo Prep** output location;
- original filenames are retained when possible;
- if an output filename already exists, the app creates a unique new filename rather than overwriting any file;
- original date/time and GPS/location metadata are preserved in the resized copy when Android and the source format make that metadata available;
- failure to preserve some metadata does not destroy an otherwise successful resized copy, but the app must clearly report that metadata was not fully preserved;
- resized copies may be shared to Google Drive through Android's normal share/storage workflow;
- the initial app must not require a remote server, subscription, or company-specific account.

These are baseline rules, not permission to add every related feature at once.

## 4. Smallest honest change class

Use the smallest change level that honestly covers the risk. When uncertain between two levels, use the higher level.

### Level 1 — low risk

Examples:

- documentation;
- comments;
- test wording that does not redefine approved behavior;
- noninteractive copy;
- appearance-only changes that cannot affect photo selection, storage, metadata, resizing, sharing, permissions, or file naming.

Required process:

- use a branch;
- record the narrow scope;
- inspect the diff;
- run only the checks relevant to the changed surface.

Documentation-only changes do not require Android runtime tests.

### Level 2 — normal feature or fix

Examples:

- batch-selection UI;
- resize-percentage controls;
- progress display;
- output naming behavior;
- ordinary error messages;
- non-destructive share/export behavior;
- fixes that do not broaden permissions or change protected storage rules.

Required process:

- use a branch and pull request;
- record the problem, approved behavior, owning files, protected behavior, focused tests, risks, rollback point, and affected smoke checks;
- run focused tests during development;
- run the complete required automated verification once on the final runtime head before merge;
- inspect the final diff.

### Level 3 — high risk

Examples:

- any code that can modify, move, rename, replace, or delete original photos;
- changes to Android storage or media permissions that broaden access;
- metadata-writing changes that could corrupt output or originals;
- automatic background uploads;
- direct Google Drive API integration or broader cloud permissions;
- output-folder migrations;
- bulk deletion or cleanup;
- signing, release, or deployment changes that could make the installed app unusable;
- any change whose failure could cause broad photo loss or corruption.

Required process:

- use a dedicated branch and pull request;
- create a detailed impact record;
- identify realistic test photos and a safe validation method;
- document exact rollback steps;
- pass focused and complete required verification;
- obtain explicit operator approval before merge.

## 5. Authorization without repeated permission loops

- The user's approved request authorizes the documented Level 1 or Level 2 scope.
- Do not ask for the same approval again when the scope has not changed.
- Ask again when the scope expands, a material assumption proves false, or Level 3 pre-merge approval is required.
- An audit finding or adjacent defect is not automatic authorization to fix it.

## 6. Branch and main protection

- `main` is the stable release baseline, not an experiment surface.
- Do not intentionally change app behavior directly on `main`.
- Create a branch for changes and use a pull request before merging runtime work.
- A direct-to-`main` emergency action may only restore a known-good version. It may not add a feature, refactor, rename, or redesign.

## 7. Scope and ownership

- Change the module that owns the behavior.
- State which photo/file surfaces are read and which are written.
- Preserve unrelated controls, settings, photos, metadata, filenames, and output files.
- Do not turn a narrow fix into cleanup, refactoring, renaming, architecture replacement, or feature expansion without approval.
- Report adjacent defects separately.

## 8. Storage and permission safety

- Request only the Android permissions needed for the approved workflow.
- Prefer Android system pickers and scoped-storage mechanisms over broad filesystem access.
- Do not add broad all-files access merely for convenience.
- Do not add cloud credentials, API keys, signing secrets, or private tokens to GitHub.
- Do not assume Google Drive access is required when Android sharing can satisfy the approved workflow.
- A denied permission or unavailable source photo must fail safely and must not damage any file.

## 9. Output safety

- Resized files are copies, not replacements.
- A successful output write must be completed before the app reports that photo as successfully prepared.
- Existing output files must never be silently overwritten.
- Partial or failed output files must not be presented as successful.
- Batch processing must report failed items separately from successful items.
- One failed photo must not silently cause already successful output copies to be deleted.

## 10. Metadata safety

- Preserve original capture date/time and GPS/location metadata in resized copies when available and supported.
- Never invent missing GPS or capture-time metadata.
- Never report metadata as preserved unless the written output was verified to contain the intended metadata.
- If some metadata cannot be preserved, keep the successfully resized copy and clearly report the limitation.

## 11. Verification

`TESTING_CONTRACT.md` will own exact test selection and timing. Until it exists, use these bootstrap rules:

- documentation-only changes require diff inspection, not Android runtime tests;
- runtime development uses focused tests for the changed behavior;
- after fixing a focused failure, rerun that focused test before unrelated tests;
- before merging runtime work, run the complete required automated test/build checks on the final runtime head;
- a required test or build failure stops commit/push/merge/release automation for that change until repaired;
- manual testing must use disposable test copies, never irreplaceable field originals as the only test data.

## 12. Release safety

- Do not distribute an APK that has not passed the verification required by the governing contracts.
- Record the last known-good commit before release-changing work.
- Installation testing must confirm that upgrading the app does not alter existing original photos or previously prepared output files.
- Signing keys and credentials must remain outside the repository.

## 13. Contract precedence

If runtime code, comments, tests, documentation, or convenience conflict with an approved governing contract, the contract wins until the operator explicitly approves a contract change.
