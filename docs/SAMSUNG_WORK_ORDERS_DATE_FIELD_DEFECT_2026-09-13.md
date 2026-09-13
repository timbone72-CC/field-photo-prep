# Samsung Work Orders Date Field Defect

Date: 2026-09-13
Status: **LOCKED — CONFIRMED DEVICE DEFECT; NOT YET IMPLEMENTED**
Branch recorded from: `feat/concept-3-ui-makeover-20260912`
PR: #35
Observed device: Samsung Galaxy S21

## Confirmed defect

On the Work Orders screen, the date control under **New dated work order** is difficult to read on the physical Samsung device. The date value is vertically cramped/clipped inside its field and is visually crowded by the area below it.

This is a real field-UI defect, not a speculative emulator-only issue. The operator provided physical-device evidence after the Phase 9D Samsung verification gate.

## User impact

The date is part of creating a dated work-order folder. If the selected date is hard to read, the operator can more easily create a work order for the wrong date. This therefore needs correction even though the underlying date-selection logic itself has not been reported broken.

## Locked repair scope

Repair the Work Orders date control so that on the Samsung device:

- the full date value is clearly visible;
- the label/value are vertically centered and not clipped;
- the control has enough height and top/bottom padding for the current font/display settings;
- the **Add Work Order** control and **Maintenance actions** area do not crowd or overlap the date field;
- the existing date-selection behavior and dated-work-order creation logic remain unchanged.

Do not redesign the Work Orders screen, change date semantics, change folder naming, or alter Drive/work-order identity behavior as part of this fix.

## Classification

This is a **Level 2 UI fix**. It changes presentation/layout only and must not alter Drive access, folder identity, creation/reuse rules, stored work-order selection, or photo behavior.

## Order

This repair is locked after the separately scoped **Batch Discard / multi-photo local deletion** change discovered during the same Samsung field pass.

Current order:

1. Batch Discard / multi-photo local deletion.
2. Work Orders date-field readability repair.
3. Version/docs reconciliation and merge review after both approved follow-up repairs are resolved.

## Verification

Focused verification only:

- automated layout/render coverage sufficient to catch clipping at supported text sizes;
- physical Samsung Galaxy S21 confirmation that the full date is readable and controls do not overlap;
- no destructive Drive test required unless implementation unexpectedly touches Drive/work-order creation owners.

No runtime code is changed by this record.
