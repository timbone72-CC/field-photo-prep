# Field Photo Prep Change Control Contract

## Purpose

This contract controls how Field Photo Prep changes are proposed, implemented, tested, reviewed, merged, released, and rolled back.

Its purpose is to protect original field photos, prevent unsafe storage or permission changes, and keep development moving without unnecessary process.

The amount of process must match the amount of risk.

## 1. Change levels

Use the smallest honest change level. When uncertain between two levels, use the higher level.

### Level 1 — low risk

Typical examples:

- documentation and comments;
- test descriptions that do not redefine approved behavior;
- noninteractive wording;
- appearance-only changes that cannot affect photo selection, resizing, storage, metadata, sharing, file naming, permissions, or release behavior.

Required process:

- use a branch;
- record the narrow scope;
- inspect the diff;
- run only checks relevant to the changed surface;
- no Android runtime test is required for documentation-only changes.

### Level 2 — normal feature or fix

Typical examples:

- photo-picker UI behavior;
- resize-percentage controls;
- progress display;
- result summaries;
- filename presentation;
- ordinary batch-processing fixes that do not change the storage or permission contract;
- normal sharing-flow improvements that continue using the approved Android workflow.

Required process:

- use a branch and pull request;
- record the problem, evidence, approved scope, owning files, protected behavior, focused tests, rollback point, and affected smoke checks;
- change only the owning implementation and its tests;
- run focused tests during development;
- run the complete required automated verification once on the final runtime head before merge;
- perform the affected device smoke check before declaring the change complete.

The user's approved request authorizes Level 1 and Level 2 work inside the documented scope. Do not ask for the same approval again unless the scope expands or assumptions prove false.

### Level 3 — high risk

Typical examples:

- any change that can overwrite, move, rename, replace, or delete original photos;
- storage-model changes or migrations;
- changes to the output-folder strategy that could collide with source files;
- broad Android storage permissions;
- permission changes that increase access beyond the approved photo/file workflow;
- automatic cleanup or deletion;
- metadata-writing changes that could alter originals;
- automatic upload or remote synchronization;
- Google Drive API integration beyond Android's normal operator-initiated share/storage flow;
- signing, release, package-identity, or deployment changes whose failure could prevent safe installation or updates;
- changes whose failure could broadly lose data or make the app unsafe to use.

Required process:

- use a dedicated branch and pull request;
- create the full impact record described below;
- verify the exact read and write surfaces;
- use realistic test photos or a safe test environment;
- identify explicit rollback steps before implementation;
- run focused tests and the complete required automated verification;
- run all affected device data-preservation checks;
- obtain explicit operator approval before merge.

A Level 3 change must stop if required structural assumptions remain unverified.

## 2. Authorization

- The user's approved request authorizes the documented scope.
- Level 1 and Level 2 changes do not require repeated approval when scope remains unchanged.
- Level 3 changes require explicit pre-merge operator approval.
- A contract finding, audit result, adjacent defect, or code-review observation is not automatic authorization to implement another change.
- Adjacent defects must be reported separately unless the user approves expanding scope.
- No feature work is performed directly on `main`.

## 3. Required change records

### Level 1 record

State:

- what is changing;
- why it is low risk;
- files expected to change;
- protected behavior;
- the check that proves the changed surface is still correct.

### Level 2 record

State:

- exact user-facing problem or requested feature;
- reproducible evidence or approved behavior;
- owning files and functions;
- read surfaces;
- write surfaces;
- protected behavior;
- focused tests;
- primary risks;
- rollback commit;
- affected device smoke checks.

### Level 3 impact record

In addition to the Level 2 record, state:

- exact original-photo access required;
- exact output-file access required;
- Android permission changes;
- metadata changes;
- storage or schema changes;
- hard limits and batch-size assumptions;
- low-storage behavior;
- partial-failure behavior;
- cancellation behavior;
- realistic fixture or safe-environment plan;
- baseline and expected final verification results;
- failure recovery steps;
- explicit pre-merge approval status.

## 4. Original-photo safety boundary

Original-photo protection from `CONTRACT.md` is a hard ownership boundary.

A change may not treat original photos as temporary working files.

Runtime code must not:

- open originals for destructive write access when read access is sufficient;
- save resized output back over the source URI or source path;
- rename or move originals as part of normal processing;
- delete originals after successful resize or share;
- use a cleanup operation that cannot distinguish original files from app-created output files.

If implementation requires violating one of these rules, stop and reclassify the work as Level 3. The governing contract must be reviewed before implementation continues.

## 5. Permission control

Use the narrowest Android permission model that can satisfy the approved workflow.

- Prefer Android-supported photo picker or document APIs over broad storage access when practical.
- Do not request broad file-system access merely for convenience.
- Do not request network, account, or Google Drive permissions unless a feature actually requires them and the contract has approved that feature.
- Permission expansion is never hidden inside an unrelated feature or fix.
- Any permission added to the manifest must be explained in the change record.

## 6. Diff control

- Inspect every changed file and function.
- Explain every changed block for Level 2 and Level 3 changes.
- Remove unrelated edits.
- A display-only request may not change storage, resizing, metadata, permissions, file naming, or sharing behavior.
- A resize-control change may not silently change metadata handling or original-photo protection.
- A sharing change may not silently add automatic upload or background synchronization.

## 7. Testing and failure-stop rule

`TESTING_CONTRACT.md` owns test selection, timing, reuse, and failure-stop behavior.

General rules:

- run focused tests while developing;
- after fixing a focused failure, rerun that focused test before unrelated tests;
- run the complete required automated verification once on the final runtime head before merge;
- documentation-only changes do not require runtime tests;
- any required test failure stops commit, push, merge, APK publication, or release until the failure is resolved or the change is abandoned.

## 8. Device verification

Runtime behavior that reads or writes photos must be verified with safe test images before release.

Affected checks may include:

- originals remain byte-for-byte or visibly unchanged where appropriate;
- resized copies are created separately;
- output dimensions match the selected percentage within expected rounding;
- filenames do not overwrite existing output files;
- supported date/time metadata is preserved;
- supported GPS/location metadata is preserved when available;
- metadata-copy failure is reported without damaging originals;
- cancellation leaves originals unchanged;
- partial batch failure does not remove successful or source files unexpectedly;
- low-storage failure leaves originals unchanged;
- Android share flow receives the resized copies, not the originals, unless the operator deliberately selected originals outside the app workflow.

The relevant subset becomes part of the regression checklist.

## 9. Merge and release

- Commit and push the branch before review.
- Use a pull request into `main`.
- Merge only after the checks required by the selected change level pass.
- Level 3 requires explicit operator approval before merge.
- APK generation or installation does not bypass contract or test requirements.
- Do not claim a released change is complete until its required device smoke check passes.

## 10. Rollback

Every runtime change must identify a known-good rollback point before merge.

If post-release verification fails:

1. stop additional feature work on the affected surface;
2. preserve evidence of the failure;
3. restore or rebuild from the recorded known-good commit when necessary;
4. verify the restored APK or runtime behavior;
5. diagnose the failed change on a branch rather than experimenting on `main`.

A direct-to-`main` emergency action may only restore a known-good version. It may not add a feature, redesign the app, change permissions, or alter the storage contract.

## 11. Maintenance rule

Contracts are guardrails, not substitutes for judgment.

Do not add process that does not reduce a real risk. Do not weaken photo-safety rules merely to make development faster.
