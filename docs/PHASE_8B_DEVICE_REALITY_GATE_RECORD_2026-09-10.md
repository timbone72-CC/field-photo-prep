# Phase 8B Device Reality Gate Record

Date: 2026-09-10

Device: Samsung Galaxy A16

Branch: `phase8b/internal-production-package-separation`

Exact tested runtime head: `29fc85d910bab6c3ee2477bee63db8ae14c4c94b`

Android CI run: `34528641357`

Artifact: `10172742178` (`field-photo-prep-internal-apk`)

## Purpose

Verify only the new Phase 8B Android package-identity boundary without repeating Phase 7 camera/preparation/upload/reconciliation/cleanup behavior that was already physically validated and was not changed by Phase 8B.

## Physical observations

The Samsung Galaxy A16 physical gate passed:

- `Field Photo Prep Internal` installed successfully as the new internal package `com.inandout.fieldphotoprep.internal`.
- The existing `Field Photo Prep` package remained installed.
- Launcher evidence showed both apps installed side-by-side as distinct applications.
- `Field Photo Prep Internal` opened normally.
- The internal app selected and connected to the existing `HNP Jobs` Google Drive SAF master.
- The internal app reopened the existing safe `FIELD PHOTO PREP TEST → Cut Grass - 2026-09-21` work-order path without creating a duplicate destination.
- Camera launch and return worked under the new `.internal` package identity and package-derived FileProvider authority.
- The captured disposable photo returned as a protected local `WAITING` record with suffix `…08D7F98A` under the exact existing work order and displayed destination identity suffix `…Vmh4GuLy`.

## Reused prior evidence

Phase 8B changed build/package/release identity only. It did not change Drive folder semantics, camera capture logic, photo preparation, queue state, upload sequencing, uncertainty reconciliation, confirmed cleanup, or restart behavior.

Therefore the successful Phase 7 physical evidence for prepare/upload/cleanup/restart remains valid and was not repeated solely for confidence.

## Automated evidence

Exact runtime head `29fc85d910bab6c3ee2477bee63db8ae14c4c94b` passed Android CI run `34528641357`, including:

- JVM/unit tests;
- internal debug build;
- stable non-production test signer verification;
- Android instrumentation;
- install/launch smoke targeting `com.inandout.fieldphotoprep.internal`;
- internal APK artifact packaging.

## Result

**PASS**

Phase 8B proves the internal/test package can coexist with the future production package identity, can obtain its own persisted SAF access, and can launch/return from camera capture correctly under the suffixed application/FileProvider identity.

No production signing key was created or committed. Production signing remains a separate secure-release concern.

## Next step

Merge Phase 8B after governance approval, then proceed to Phase 8C second-Android-phone/shared-master reality validation.
