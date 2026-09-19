# Phase 11B — Clear Company Switcher — 2026-09-18

Status: Level 2 implementation authorized by the operator request: "We need a clearer way to switch."

## User-facing problem

The active company is visible on Home, but switching companies is hidden under:
`⋯ → Switch Company`.

Company switching is a frequent field action, so the current control makes the operator hunt through a secondary menu.

## Approved behavior

- The active company card on Home remains the primary company/status surface.
- In multi-company workspace mode, the card exposes a visible **Switch** control.
- Tapping the company-name area also opens the company chooser directly.
- The company chooser continues to use the existing exact provider-ID selection path.
- The overflow menu remains for lower-frequency actions: Add Company, Edit Company, Change Workspace.
- Legacy single-company mode does not show a misleading company switch control.

## Owning files/functions

- `screen_home_properties.xml`
- `MainActivity.buildHomeUi()`
- `MainActivity.renderSavedMaster()`
- `MainActivity.setBusy()/setNotBusy()`
- focused Home company-switch instrumentation tests

## Read surfaces

Existing workspace/company state only.

## Write surfaces

No new persistence or Drive writes.
Selecting a company continues to call the existing `showCompanyChooser()/selectCompany()` path.

## Protected behavior

This change must not alter:
- workspace/provider identity;
- company/provider identity;
- company create/rename rules;
- property/work-order discovery parents;
- queued-photo destinations;
- capture, preparation, upload, retry, reconciliation, deletion, or permissions.

## Primary risks

- switch control visible when no multi-company workspace exists;
- switch action available while Drive work is busy;
- new control accidentally bypasses the existing company chooser;
- layout crowding/truncation on phone-sized screens.

## Focused tests

Prove:
- multi-company Home renders a visible Switch control;
- tapping Switch invokes the existing company chooser;
- tapping the company-name region invokes the same chooser;
- Switch is disabled while busy;
- legacy single-company mode hides the Switch control.

## Rollback

Rollback point: `66aeed51fc7065f4f18e06677e3931daf4f3977f`.

Revert this narrow UI change if the Home header becomes unusable. No Drive/photo state rollback is required because this change adds no new stored state or remote writes.

## Smoke checks

- Home still renders active company + workspace.
- HNP/Tresmolino company chooser remains populated from Drive.
- selecting a company still refreshes only that company's properties.
- overflow retains Add Company / Edit Company / Change Workspace.
