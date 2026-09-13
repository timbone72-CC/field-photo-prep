# Uploaded-photo checkbox presentation fix

User-reported problem: confirmed uploaded photo rows still display a disabled upload-selection checkbox, implying an available action.

Approved scope: hide only the uploaded row's checkbox; keep its Uploaded status and row detail access. Retain the checkbox's layout space to preserve thumbnail alignment.

Classification: Level 1 appearance-only. No selection eligibility, queue, capture, preparation, retry, cleanup, destination, permission or Drive write behavior changes. No Google Drive integration impact.

Owner: PhotoCaptureActivity.renderPhotoList, reading PendingPhotoRecord state and writing View visibility only. Existing eligibility and listeners are unchanged.

Focused verification: extend the existing real-activity rendered instrumentation fixture with a confirmed uploaded record; assert its checkbox is invisible/disabled and Uploaded status is visible, while the ready record checkbox remains visible/enabled and updates Upload Selected count. Review the final Photos render.

Rollback baseline: 36866a9055e60cd06341f55e3d168f4fbfa952f1. Revert the narrow presentation patch without modifying app data.

Delivery gate: existing Android CI on the final runtime head. Samsung check is limited to uploaded rows having no checkbox and ready rows retaining selection.
