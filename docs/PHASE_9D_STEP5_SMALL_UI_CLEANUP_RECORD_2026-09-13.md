# Phase 9D Step 5 — Small UI Cleanup Record

Date: 2026-09-13
Status: **IMPLEMENTED — AUTOMATED VERIFICATION PENDING**
Branch: `feat/concept-3-ui-makeover-20260912`
PR: #35
Rollback baseline: `6008764cb7156ce8a9da5454f85e6a418373455b`

## Scope

Step 5 performs only the two cleanup items approved in the locked Phase 9D repair plan:

1. increase the Photos upload-selection checkbox target from 44dp wide to 48dp × 48dp;
2. remove the obsolete hidden `work_order_select_existing` selector and its Java binding/picker plumbing after confirming the visible `work_order_list` is the real production selection path.

## Dependency check

The dependency audit confirmed:

- `work_order_list` owns visible work-order selection and calls the existing `selectWorkOrder(...)` owner;
- the hidden `work_order_select_existing` button was the only caller of `showWorkOrderPicker()`;
- the hidden button's render/enabled-state code existed only to support that obsolete control;
- no Drive, folder identity, work-order creation/reuse, Clear & Reuse, Photos, or camera owner depended on the hidden picker.

Therefore the hidden selector path was safe to remove.

## Runtime changes

- `app/src/main/res/layout/row_photo.xml`
  - `photo_row_check` width: 44dp → 48dp;
  - height remains 48dp.

- `app/src/main/res/layout/screen_work_orders.xml`
  - removed the hidden 1dp × 1dp `work_order_select_existing` button.

- `app/src/main/java/com/inandout/fieldphotoprep/MainActivity.java`
  - removed the obsolete selector field and layout binding;
  - removed its click listener;
  - removed the unused picker dialog/helper rendering path;
  - removed busy/not-busy state plumbing that existed only for that hidden control;
  - preserved visible `work_order_list` selection and `selectWorkOrder(...)` unchanged.

## Test change

- `Concept3UiStructureInstrumentedTest`
  - now locks `photo_row_check` to 48dp × 48dp.

Step 4 interaction coverage continues to exercise visible work-order row selection and navigation, so removing the hidden duplicate picker does not reduce tested production access.

## Protected behavior unchanged

No changes to:

- SAF permissions or provider identity;
- address/work-order folder discovery or creation;
- work-order selection identity;
- reuse or Clear & Reuse behavior;
- protected originals;
- photo preparation;
- queue/upload/retry/UNCERTAIN/reconciliation/cleanup;
- camera behavior;
- Home Drive options.

No new feature or Settings surface was added.

## Verification gate

Run the normal full Android CI suite on the final Step 5 head. Step 5 is complete only if unit tests, build, signer verification, full instrumentation, launch smoke, and artifacts all pass.
