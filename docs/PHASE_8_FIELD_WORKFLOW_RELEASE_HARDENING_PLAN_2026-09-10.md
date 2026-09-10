# Phase 8 — Field Workflow / Release Hardening Plan

Date: 2026-09-10

Branch: `phase8/field-workflow-release-hardening`

Governed base: current `main` after merged Phase 3B/4 integration (`98d777123f971c3b2849bb9816c0ee4b7b48af13` or descendant documentation-only state)

## Goal

Turn the now-proven core workflow into a field-ready first release without expanding Field Photo Prep into a larger camera/gallery/Drive-management product.

The approved core remains:

`choose/reuse address → choose/create/reuse dated work order → take still photos → prepare → upload to immutable Drive destination → reconcile uncertainty when needed → clean local image copies only after confirmed success`

## Evidence entering Phase 8

The Samsung Galaxy A16 real-device session established the core safety path on Android + Google Drive SAF/DocumentsProvider:

- persisted master-folder access;
- existing address/work-order discovery;
- Phase 3B Clear & Reuse fail-closed provider freshness and successful same-identity reuse;
- Phase 4 address create/reuse without duplication;
- real camera capture and restart survival;
- immutable photo destination binding;
- visually usable preparation;
- real Drive upload to exact stored work-order identity;
- confirmed remote identity persistence;
- Phase 7B cleanup after confirmation;
- Phase 7B restart survival;
- stable test signing and install/update continuity;
- final Phase 3B/4 integration onto Phase 7B passed proportional A16 smoke and CI.

## Phase 8A — Field workflow polish

Change level: **Level 1** so long as implementation stays wording/layout-only and does not change button actions, Drive decisions, stored identity, queue state, camera invocation, or upload semantics.

Observed friction to address:

1. The work-order name field looked enough like a search field that the operator typed into it while trying to locate an existing work order.
2. Existing work-order selection and new/reused work-order entry were visually adjacent without a clear conceptual divider.
3. The main screen still displayed a stale developer phase label (`Phase 5`).
4. The photo screen still displayed a developer phase label (`Phase 7B`) and internal implementation wording that is useful during development but noisy for normal field use.

Approved 8A changes:

- replace developer phase labels with user-facing workflow labels;
- rename `Select Work Order` to `Select Existing Work Order`;
- add a short static label separating existing-work-order selection from new/reused work-order entry;
- change the input hint to `Work order name only — e.g. Cut Grass`;
- add static helper text that the date is selected separately;
- clarify `Use / Create Address` as an address-folder action;
- clarify `Use / Create Work Order` as a dated work-order action;
- simplify photo-screen top copy without changing queue/status details needed for safety;
- preserve all existing control wiring and enabled/disabled guards exactly.

Explicitly excluded from 8A:

- no camera API change;
- no CameraX;
- no attempt to suppress Samsung/external-camera OK/Retake UI;
- no Drive/provider changes;
- no folder identity, create/reuse, deletion, upload, reconciliation, retry, or cleanup semantic changes;
- no schema or migration;
- no background worker/service;
- no new dependency.

Verification:

- inspect exact diff for copy/layout-only ownership;
- normal debug build/launch smoke is sufficient if only Java UI construction wording/layout changes;
- if implementation touches logic, reclassify before continuing.

## Phase 8B — Normal install/update/release path

Change level: **Level 3** because release signing/deployment identity controls future update continuity.

Intent:

- keep the checked-in stable test key strictly non-production;
- define a production/release signing path that does not commit private release key material to the repository;
- make versionCode/versionName progression intentional;
- produce one release-candidate APK with a documented identity/checksum;
- prove install/update behavior on the primary Android device without losing persisted app state unexpectedly;
- document rollback to the last known-good release candidate.

No Play Store publication is assumed. Distribution method will be selected only from what the operator actually needs for continued use/testing.

## Phase 8C — Second Android phone/shared-master reality check

Physical-device boundary.

Goal:

Prove the same minimal workflow on another supported Android phone using that phone user's Google Drive access to the shared/approved master folder.

Smallest required observations:

- install normally;
- choose the intended Google Drive provider/account context;
- select the shared approved master folder;
- reopen an existing safe address/work order without creating duplicates;
- take/prepare/upload one disposable photo to the exact safe work order;
- verify the file appears in Drive and is visually usable;
- restart and confirm confirmed state remains safe;
- confirm no assumption in the app depends on the primary phone's account-local provider IDs being portable to another device/account.

Do not use live customer content when the safe test hierarchy can prove the behavior.

## Field-use findings policy

Phase 8 fixes **real friction**, not hypothetical feature ideas.

Candidate changes are accepted only when field/device evidence shows they materially improve:

- speed;
- clarity;
- recoverability;
- install/update reliability;
- destination safety;
- weak-network behavior;
- permission-loss handling.

Feature expansion stays out unless the observed workflow requires it.

## Camera review-screen decision

The Samsung camera currently presents its own OK/Retake review after a picture. That screen is owned by the external camera app launched through `ACTION_IMAGE_CAPTURE`, not by Field Photo Prep.

For the first release, keep the external-camera implementation because it is already field-proven and keeps the app lean. Removing the review screen reliably would likely require owning camera capture in-app (for example CameraX), which is a materially larger subsystem. Revisit only if real field use demonstrates that the extra confirmation tap is worth that cost.

## Stop conditions

Stop or replan if Phase 8 work would:

- alter immutable destination binding;
- weaken provider freshness or duplicate prevention;
- add a new destructive Drive path;
- alter upload/retry/UNCERTAIN semantics;
- risk deleting an unconfirmed original;
- require broader Drive permissions;
- require a new camera subsystem without explicit scope approval;
- require production signing secrets to be committed to source control.

## Next straight-line step

Complete 8A copy/layout polish and verify it without phone dependency. Then build 8B release/install hardening as far as can be proven without another device. Stage one combined primary-phone release-candidate gate at the next genuine physical-device boundary, followed by 8C on the second Android phone.
