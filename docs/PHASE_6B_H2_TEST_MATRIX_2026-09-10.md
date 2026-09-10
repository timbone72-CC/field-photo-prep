# Phase 6B-H2 Focused Test Matrix

Date: 2026-09-10

This file records the focused automated checks added or updated for the Create/Persist/Write barrier. It does not claim final verification until CI passes on the exact runtime/test head.

## DrivePhotoUploader boundary

- deterministic UUID remote filename remains unchanged;
- create validates the exact stored work-order identity;
- create performs no byte write;
- unreadable/non-folder destination fails before create and remains safely retryable;
- create ambiguity is `UNCERTAIN` because a remote side effect may exist;
- write/verify refuses to run when the supplied `UPLOADING` record lacks the matching persisted provisional remote identity;
- write/verify refuses a mismatched provisional identity;
- prepared-file mutation after create stops before writer invocation;
- write interruption, short write, and verification failure remain `UNCERTAIN`.

## PhotoUploadCoordinator ordering

- successful flow persists `provisionalRemoteFileId` before provider writer invocation;
- the provider writer observes the same persisted provisional identity it is asked to write;
- successful verification promotes that identity into confirmed `remoteFileId` and clears provisional state;
- safe pre-create failure becomes `FAILED` with no provisional identity;
- create failure becomes `UNCERTAIN` with no invented provisional identity;
- write failure preserves the provisional identity in `UNCERTAIN` and blocks blind retry;
- verification failure preserves the provisional identity in `UNCERTAIN`;
- forced provisional-persistence failure after create prevents writer invocation;
- after that forced persistence failure, restored queue state remains `UPLOADING` and process-start recovery converts it to `UNCERTAIN` rather than retryable `FAILED`;
- protected original and prepared JPEG survive all H2 failure paths;
- one photo's failure does not mutate another queued photo.

## Final gate still required

After the final H2 runtime/test head is fixed, run the complete Android CI suite once on that exact head. A passing mocked/emulator suite does not replace the later physical Android + Google Drive `DocumentsProvider` reality gate required before Level-3 merge approval.