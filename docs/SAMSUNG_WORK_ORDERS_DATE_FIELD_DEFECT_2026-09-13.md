# Samsung Work Orders Date Field Defect

Date: 2026-09-13
Status: **RESOLVED + MERGED — AUTOMATED PASS + SAMSUNG DEVICE PASS**
Original branch recorded from: `feat/concept-3-ui-makeover-20260912`
Repair branch: `fix/work-order-date-readability-20260913`
Repair PR: #37
Main merge commit: `4043f5476ae5637151fe5c6b68287079c3b5d478`
Observed device: Samsung Galaxy S21

## Confirmed defect

On the Work Orders screen, the date control under **New dated work order** was difficult to read on the physical Samsung device. The date value was vertically cramped/clipped inside its field and visually crowded by the area below it.

This was a real field-UI defect, not a speculative emulator-only issue. The operator provided physical-device evidence after the Phase 9D Samsung verification gate.

## User impact

The date is part of creating a dated work-order folder. If the selected date is hard to read, the operator can more easily create a work order for the wrong date. The underlying date-selection logic itself was not reported broken.

## Approved repair scope

The repair was limited to presentation/layout:

- the date control is full-width and clearly displays the complete date;
- the control remains 48dp high with readable 14sp text;
- the date text is vertically centered and left aligned;
- **Add Work Order** is moved to its own full-width row below the date;
- **Maintenance actions** no longer crowds the date/create row;
- existing date-selection behavior and dated-work-order creation logic are unchanged.

The repair did not redesign the Work Orders screen, change date semantics, change `YYYY-MM-DD` folder naming, or alter Drive/work-order identity behavior.

## Classification

This was a **Level 2 UI fix**. It changed presentation/layout only and did not alter Drive access, folder identity, creation/reuse rules, stored work-order selection, photo behavior, queue behavior, or camera behavior.

## Automated verification

Android CI run `34760292364` passed on exact date-runtime head `079bb722b61ab5f33d264de71d964cadd49f70b5`.

The passing gate included:

- unit tests;
- internal debug APK build;
- stable signer verification;
- full instrumentation/rendered-screen suite;
- launch smoke;
- APK and rendered-test artifacts.

Internal APK artifact: `10318179463`.
Rendered/test evidence artifact: `10318159339`.

An earlier CI run failed only because the newly added test used an overly strict pixel-level tolerance for Android `sp` rounding. The runtime layout compiled and rendered; the test was corrected to compare effective text size in `sp`, then the complete suite passed.

Final version reconciliation then advanced the app to versionCode 18 / `0.13-field-ui` (`0.13-field-ui-internal` for debug/internal). The exact version-reconciled head `64339686ae0be08a2148b9b73390c35575e30584` passed Android CI run `34761274823` before merge.

## Physical Samsung verification

Samsung Galaxy S21 physical-device gate: **PASS** on 2026-09-13 using the APK built from date-runtime head `079bb722b61ab5f33d264de71d964cadd49f70b5`.

Operator result: **works**.

The operator confirmed:

- the Work Orders date is now readable;
- the date picker still opens normally;
- no new visual issue was reported in the repaired control.

This physical gate is complete and should not be repeated unless the date-layout implementation changes.

## Merge result

The two Samsung follow-up defects were resolved and physically verified before merge:

1. Batch Discard / multi-photo local deletion — automated PASS + Samsung PASS.
2. Work Orders date-field readability — automated PASS + Samsung PASS.

The operator explicitly approved the complete merge sequence on 2026-09-13. PR #37 was retargeted to `main`, its four-file diff was verified as the date repair/test/doc plus version reconciliation only, and it merged successfully as `4043f5476ae5637151fe5c6b68287079c3b5d478`.