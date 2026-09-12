# Phase 9A — Home / Properties Implementation Record

Date: 2026-09-12

Status: **IMPLEMENTED — AUTOMATED GATE BLOCKED BY RUNNER STARTUP FAILURE; DEVICE APK NOT STAGED**

Branch: `feat/phase-9a-home-properties`

PR: #34 — `Phase 9A Home and Properties redesign`

Rollback baseline: `e77b83bf07505cc586fbf8766cb8623db51e35b7`

Approved specification: `docs/PHASE_9A_HOME_PROPERTIES_UI_SPEC_2026-09-12.md`

Current executable runtime head: `fb8dba9cb3ace14c15867226c58d64e411b1a9c0`

## Problem

The prior Phase 9 redesign was rejected on the physical Samsung phone because it preserved the old development-oriented Home hierarchy and mainly changed styling. The rejected screen had an oversized title/status/Drive block, a dominant full-width New Address action, tall address cards, repeated helper text, and raw underscore-heavy Drive folder names.

The operator approved a reset: build and lock Home/Properties first, from governed `main`, using a purpose-built layout rather than decorators.

## Approved behavior implemented

Phase 9A now provides a dedicated Home surface with:

- a compact one-line `Field Photo Prep` title;
- explicit Android system-bar inset handling;
- a compact Drive status strip;
- actual selected master-folder name and `Drive connected` state when access is valid;
- Refresh plus a quiet `Change Drive` overflow action;
- no Connect Drive button while already connected;
- compact `Properties` count;
- dedicated property-row layout and adapter;
- display-only underscore/whitespace normalization;
- duplicate display-name disambiguation only when required;
- a compact bottom-end `+ New Address` action;
- a small inline error/status surface rather than routine giant status copy;
- a friendly `No properties yet` empty state.

The existing Work Orders surface is intentionally retained unchanged for the Phase 9A gate. It will be redesigned only in Phase 9B after 9A is physically accepted and locked.

## Owning files

Runtime/UI:

- `app/src/main/java/com/inandout/fieldphotoprep/MainActivity.java`
- `app/src/main/java/com/inandout/fieldphotoprep/PropertyDisplayName.java`
- `app/src/main/java/com/inandout/fieldphotoprep/PropertyListAdapter.java`
- `app/src/main/res/layout/screen_home_properties.xml`
- `app/src/main/res/layout/row_home_property.xml`
- Phase 9A Home colors/drawables/icons under `app/src/main/res/`

Tests:

- `app/src/test/java/com/inandout/fieldphotoprep/PropertyDisplayNameTest.java`
- `app/src/androidTest/java/com/inandout/fieldphotoprep/Phase9AHomeInstrumentedTest.java`

## Read / write surfaces

The new Home presentation reads:

- persisted master-folder display name/tree permission state through existing `FolderPrefs` and permission checks;
- existing direct child address results returned by `DriveClient`;
- existing immutable `DriveFolder` provider IDs and names.

It writes no new persisted schema and introduces no new Drive write path.

Existing operator actions still delegate to the existing MainActivity owner methods for master-folder selection, address refresh/create/reuse decisions, exact identity persistence, and transition into Work Orders.

## Protected behavior

This phase intentionally does **not** alter:

- Drive provider/document identity;
- address create duplicate/reuse rules;
- work-order create/reuse/Clear & Reuse semantics;
- camera or CameraX layout;
- protected original storage;
- photo preparation;
- queue state;
- upload destination, retry, UNCERTAIN, reconciliation, or cleanup;
- signing or deployment identity.

Display normalization is presentation only. Example: `101_CHUCKER_LN_ELK_CITY_OK` may display as `101 CHUCKER LN ELK CITY OK`; the stored Drive name and provider ID are unchanged.

## Focused coverage

`PropertyDisplayNameTest` covers display-only underscore conversion, whitespace collapse, and null-safe display behavior.

`Phase9AHomeInstrumentedTest` launches a disconnected Home state and requires:

- the purpose-built `Field Photo Prep` Home title;
- compact disconnected Drive state;
- Connect visible only in the disconnected case;
- Refresh hidden when disconnected;
- New Address remains wrap-content rather than a full-width action;
- the old visible `Address and work-order setup` development subtitle is absent from Home.

## Automated verification status

GitHub Actions runs for the executable runtime have repeatedly failed before any workflow steps were created/exposed (`steps: null`), including a manual failed-job rerun. This is treated as CI-runner/infrastructure blockage, **not** as passing evidence and not as an executable test failure.

No APK will be staged for the operator until one full Android CI run executes and passes on the exact final executable runtime head.

## Physical gate after automation recovers

One Samsung Home/Properties smoke only:

1. title cleanly below status bar;
2. unmistakably structurally different from the rejected screenshot;
3. compact Drive status, no giant Drive panel;
4. no Connect Drive action while connected;
5. compact New Address action;
6. human-readable property display names;
7. materially denser/shorter property rows with the list dominating the screen;
8. no repeated `Tap to view work orders` helper copy;
9. Refresh available;
10. tap one existing property and confirm the existing Work Orders flow opens.

No camera retest and no Drive create/delete/rename test is required solely for Phase 9A.
