# Phase 12E — Runtime Authorization Enforcement Evidence

Date: 2026-09-25  
Status: **PHASE 12E COMPLETE; AUTOMATED GATES PASS; SAMSUNG REALITY GATE PASS; PR #72 APPROVED FOR MERGE**

## Exact implementation

Branch: `phase-12e/runtime-authorization-enforcement`  
Runtime head: `ff8403edec07e4b60bb23e17eccd3af08c66ad2c`  
Base: `0e1104db2c2d6026a427bfa7b5baa9213b2fac25`  
PR: https://github.com/timbone72-CC/field-photo-prep/pull/72

Implemented: one central decision, deterministic 72-hour grace, session refresh/revalidation, persisted authoritative revocation, startup/resume enforcement, capture and per-shutter checks, guarded folder create/rename/delete, guarded upload before queue mutation and remote create, protected-work Sign Out guard, and conditional session clearing so a delayed Sign Out cannot erase a newer login/refresh. Sign-out/replacement in-flight validation tests cover the session-generation barrier.

Protected originals, preparation, immutable queued destinations, upload uncertainty, and read-only reconciliation remain under their existing owners. No Supabase job/photo mirror, Drive account inference, schema migration, or Organization switching was added.

## Automated evidence

- Exact runtime head Android CI run 875, ID `36172452699`: **SUCCESS**.
- `gradle test`: PASS, including `RuntimeAuthorizationPolicyTest`, `RuntimeAuthorizationManagerTest`, `ProtectedWorkGuardTest`, `AuthorizationActionGuardTest`, `DriveClientTest`, uploader/coordinator/batch tests.
- `gradle assembleDebug`: PASS.
- Stable internal APK signer verification: PASS.
- Android emulator instrumented suite and launch smoke: PASS.
- Internal APK workflow artifact ID `10880572226`, digest `sha256:b2a3498c8eff8fbf02eac739031d3cede68242944439d0cd82137c443ec6ea2c` (workflow artifact ZIP digest, not an asserted APK byte digest).
- `concept-3-rendered-verification` artifact ID `10880472339`.
- Local workspace did not have Gradle/Android SDK; these are exact-head CI results, not claimed local runs.

## Changed surface

Production Java: `AuthActivity`, `AuthorizationActionGuard`, `AuthorizationDecision`, `CameraCaptureActivity`, `DriveClient`, `DrivePhotoUploader`, `FieldPhotoPrepApplication`, `MainActivity`, `PendingPhotoStore`, `PhotoCaptureActivity`, `PhotoUploadCoordinator`, `ProtectedWorkGuard`, `RuntimeAuthorizationManager`, `RuntimeAuthorizationPolicy`, `SupabaseAuthClient`.

Focused Java tests: `AuthorizationActionGuardTest`, `DriveClientTest`, `DrivePhotoUploaderStageBindingTest`, `DrivePhotoUploaderTest`, `DrivePhotoUploaderVerificationRetryTest`, `PhotoBatchUploadRunnerTest`, `PhotoUploadCoordinatorTest`, `PhotoUploadReconciliationCoordinatorTest`, `ProtectedWorkGuardTest`, `RuntimeAuthorizationManagerTest`, `RuntimeAuthorizationPolicyTest`, `TestAuthorization`.

Documentation: `PHASE_12E_BUILD_STATE.md` and this evidence record; roadmap status updated separately on this documentation commit. The precise final list must be refreshed from PR #72 before merge because documentation commits follow the runtime head.

## Lower-level bypass review

Production provider writes occur through `DriveClient.createFolder`, `renameFolder`, `deleteDocument` and `DrivePhotoUploader.create`; each requires a current action guard. Upload calls flow through `PhotoUploadCoordinator.upload` and its guard precedes `beginUploadAttempt`. The camera reservation call sites in `PhotoCaptureActivity` and `CameraCaptureActivity` check current capture authorization; the latter rechecks before each shutter. Reconciliation queries provider state and does not create, rename, or delete remote content. Existing tests deny folder writes before provider calls, photo create before provider calls, upload before queue mutation, and the next camera/Drive action after authority loss. This is source plus automated fixture evidence; the real phone remains a separate gate.

## Samsung device evidence recorded 2026-09-25

PASS on Samsung device:
- Installed and exercised the current Phase 12E test build with retained app data.
- Protected-photo counts appeared beside the correct Home property and exact work order.
- Sign Out was correctly blocked while protected photos remained.
- After protected work was resolved, Sign Out completed and Sign In completed successfully.
- With account connectivity unavailable but inside the saved 72-hour validation window, captured one protected photo on disposable work order `TREE TRIM 3 - 2026-09-19`.
- The offline photo remained protected locally, reached WAITING/Ready to upload, and produced its prepared copy.
- After connectivity was restored, the app resumed the authorized path and the exact selected photo uploaded to Drive.
- Device UI confirmed: `Batch finished: 1 of 1 selected photo confirmed in Drive.`
- The resulting photo row was marked `Uploaded` under the same exact work order.

Samsung reality gate: **PASS**.

The exact 72-hour boundary, clock rollback, revocation, and session-generation race remain deterministic automated claims; no three-day wait or live revocation test was required.

## Smallest physical Samsung gate

Use the exact internal artifact from CI 875 and a disposable work order under the approved safe Drive test hierarchy. At the start of the phone session, and before install/test transition and final gate classification, reread `docs/MASTER_DEVICE_REALITY_GATE_PLAN_2026-09-10.md`. Do not use a live customer job.

1. Install the exact internal APK once over the existing Internal app; confirm the correct app and retained local workspace/queue state.
2. Online: open/reopen, use Account/Recheck Account, confirm ACTIVE Organization and ordinary safe test-folder navigation.
3. Turn off account connectivity briefly. Within the fresh 72-hour window, confirm previously selected disposable work order can still capture a protected photo; do not require Drive to work without its own connectivity.
4. Return online; confirm automatic successful account revalidation, and, only when Drive is available, perform the ordinary safe test upload to its stored exact destination. Confirm protected original and queue result follow existing rules.
5. If the disposable protected photo remains unresolved, confirm Sign Out is blocked and the photo stays intact. Do not force an uncertain retry or delete work merely to finish this observation.
6. If all work is independently confirmed and cleanup complete, an allowed Sign Out/re-sign-in may be observed once; it is not a reason to discard a protected fixture.

The exact 72-hour boundary, clock rollback, revocation, and session-generation race are deterministic automated claims. No real three-day wait or live revocation experiment is required. If provider state becomes uncertain, stop the affected remote path and preserve evidence.

## Merge authorization

All required Phase 12E automated and proportional Samsung gates passed. Operator Level 3 merge approval was given on 2026-09-25. PR #72 may be merged after final PR metadata/evidence refresh and confirmation that the exact final branch head is green.
