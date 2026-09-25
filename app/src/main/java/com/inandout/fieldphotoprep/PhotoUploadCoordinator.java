package com.inandout.fieldphotoprep;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class PhotoUploadCoordinator {
    public static final class ReconciliationResult {
        private final DrivePhotoReconciler.Result.Outcome outcome;
        private final PendingPhotoRecord record;
        private final String detail;

        ReconciliationResult(
                DrivePhotoReconciler.Result.Outcome outcome,
                PendingPhotoRecord record,
                String detail) {
            this.outcome = Objects.requireNonNull(outcome, "outcome");
            this.record = Objects.requireNonNull(record, "record");
            this.detail = detail == null || detail.isBlank()
                    ? "Reconciliation completed without additional detail."
                    : detail;
        }

        public DrivePhotoReconciler.Result.Outcome outcome() {
            return outcome;
        }

        public PendingPhotoRecord record() {
            return record;
        }

        public String detail() {
            return detail;
        }
    }

    private final PendingPhotoStore photoStore;
    private final PhotoPreparer photoPreparer;
    private final DrivePhotoUploader driveUploader;
    private final DrivePhotoReconciler driveReconciler;
    private final ConfirmedPhotoCleanup confirmedPhotoCleanup;
    private final AuthorizationActionGuard authorizationGuard;

    public PhotoUploadCoordinator(
            PendingPhotoStore photoStore,
            PhotoPreparer photoPreparer,
            DrivePhotoUploader driveUploader) {
        this(
                photoStore,
                photoPreparer,
                driveUploader,
                null,
                AuthorizationActionGuard.permissiveForTests());
    }

    public PhotoUploadCoordinator(
            PendingPhotoStore photoStore,
            PhotoPreparer photoPreparer,
            DrivePhotoUploader driveUploader,
            DrivePhotoReconciler driveReconciler) {
        this(
                photoStore,
                photoPreparer,
                driveUploader,
                driveReconciler,
                AuthorizationActionGuard.permissiveForTests());
    }

    PhotoUploadCoordinator(
            PendingPhotoStore photoStore,
            PhotoPreparer photoPreparer,
            DrivePhotoUploader driveUploader,
            AuthorizationActionGuard authorizationGuard) {
        this(
                photoStore,
                photoPreparer,
                driveUploader,
                null,
                authorizationGuard);
    }

    PhotoUploadCoordinator(
            PendingPhotoStore photoStore,
            PhotoPreparer photoPreparer,
            DrivePhotoUploader driveUploader,
            DrivePhotoReconciler driveReconciler,
            AuthorizationActionGuard authorizationGuard) {
        this.photoStore = Objects.requireNonNull(photoStore, "photoStore");
        this.photoPreparer = Objects.requireNonNull(photoPreparer, "photoPreparer");
        this.driveUploader = Objects.requireNonNull(driveUploader, "driveUploader");
        this.driveReconciler = driveReconciler;
        this.authorizationGuard = Objects.requireNonNull(
                authorizationGuard,
                "authorizationGuard");
        this.confirmedPhotoCleanup = new ConfirmedPhotoCleanup(photoStore, photoPreparer);
    }

    public PendingPhotoRecord upload(String photoId) throws IOException {
        PendingPhotoRecord before = requireRecord(photoId);
        if (!before.canBeginUploadAttempt()) {
            throw new IOException("This photo cannot begin an upload from state "
                    + before.state().name() + ".");
        }

        File prepared = photoPreparer.preparedFile(photoId);
        if (!prepared.isFile() || prepared.length() <= 0) {
            throw new IOException("Prepare this photo before uploading. Queue state was not changed.");
        }

        authorizationGuard.requireDriveMutation();
        PendingPhotoRecord uploading = photoStore.beginUploadAttempt(photoId);
        final DrivePhotoUploader.CreatedUpload created;
        try {
            created = driveUploader.create(uploading, prepared);
        } catch (DrivePhotoUploader.UploadException error) {
            persistDriveFailure(photoId, error);
            throw new IOException(error.getMessage(), error);
        } catch (RuntimeException error) {
            persistUnexpectedUncertainty(photoId, error);
            throw new IOException(
                    "Upload stopped with an unexpected provider result. Remote state must be reconciled before retry.",
                    error);
        }

        final PendingPhotoRecord uploadingWithProvisional;
        try {
            uploadingWithProvisional = photoStore.recordProvisionalRemoteFileId(
                    photoId,
                    created.remoteFileId());
        } catch (IOException persistenceError) {
            persistCreateBarrierUncertainty(photoId);
            throw new IOException(
                    "Drive created a photo identity, but that identity could not be durably recorded before writing. "
                            + "No photo bytes were intentionally written; retry is blocked until the remote result is reconciled.",
                    persistenceError);
        }

        final DrivePhotoUploader.UploadResult remoteResult;
        try {
            remoteResult = driveUploader.writeAndVerify(
                    uploadingWithProvisional,
                    created,
                    prepared);
        } catch (DrivePhotoUploader.UploadException error) {
            persistDriveFailure(photoId, error);
            throw new IOException(error.getMessage(), error);
        } catch (RuntimeException error) {
            persistUnexpectedUncertainty(photoId, error);
            throw new IOException(
                    "Upload stopped with an unexpected provider result. Remote state must be reconciled before retry.",
                    error);
        }

        try {
            return photoStore.markUploadConfirmed(photoId, remoteResult.remoteFileId());
        } catch (IOException confirmationError) {
            PendingPhotoRecord current = null;
            try {
                current = photoStore.getById(photoId);
            } catch (IOException ignored) {
                // The original confirmation error remains primary; process-start recovery will fail closed.
            }

            if (current != null
                    && current.state() == PendingPhotoRecord.State.UPLOADED
                    && remoteResult.remoteFileId().equals(current.remoteFileId())) {
                return current;
            }

            if (current != null && current.state() == PendingPhotoRecord.State.UPLOADING) {
                try {
                    photoStore.markUploadUncertain(
                            photoId,
                            "Drive verified the created photo, but local confirmation bookkeeping failed. "
                                    + "Remote state must be reconciled before retry.");
                } catch (IOException ignored) {
                    // Leaving UPLOADING is still fail-closed; process startup converts it to UNCERTAIN.
                }
            }
            throw new IOException(
                    "Drive returned a verified photo identity, but local confirmation could not be committed safely. "
                            + "The local photo and provisional remote identity were kept and retry is blocked until the result is reconciled.",
                    confirmationError);
        }
    }

    public ReconciliationResult reconcileUncertain(String photoId) throws IOException {
        PendingPhotoRecord before = requireRecord(photoId);
        if (before.state() != PendingPhotoRecord.State.UNCERTAIN) {
            throw new IOException("Only an UNCERTAIN photo can be remotely reconciled.");
        }
        if (driveReconciler == null) {
            throw new IOException("Remote reconciliation is not configured for this coordinator.");
        }

        File prepared = photoPreparer.preparedFile(photoId);
        DrivePhotoReconciler.Result remoteResult = driveReconciler.reconcile(before, prepared);

        switch (remoteResult.outcome()) {
            case CONFIRMED_MATCH:
                PendingPhotoRecord uploaded = photoStore.markUploadConfirmed(
                        photoId,
                        remoteResult.remoteFileId());
                return new ReconciliationResult(
                        remoteResult.outcome(),
                        uploaded,
                        remoteResult.detail());
            case CONFIRMED_ABSENT_RETRY_SAFE:
                PendingPhotoRecord retryable = photoStore.resolveUncertainAsRetryableAbsence(
                        photoId,
                        remoteResult.detail());
                return new ReconciliationResult(
                        remoteResult.outcome(),
                        retryable,
                        remoteResult.detail());
            case REMAIN_UNCERTAIN:
            default:
                PendingPhotoRecord unchanged = requireRecord(photoId);
                return new ReconciliationResult(
                        DrivePhotoReconciler.Result.Outcome.REMAIN_UNCERTAIN,
                        unchanged,
                        remoteResult.detail());
        }
    }

    public ConfirmedPhotoCleanup.Result cleanupConfirmedLocalData(String photoId)
            throws IOException {
        return confirmedPhotoCleanup.cleanup(photoId);
    }

    private void persistCreateBarrierUncertainty(String photoId) {
        try {
            PendingPhotoRecord current = photoStore.getById(photoId);
            if (current != null && current.state() == PendingPhotoRecord.State.UPLOADING) {
                photoStore.markUploadUncertain(
                        photoId,
                        "Drive created a photo identity, but provisional identity bookkeeping did not complete safely. "
                                + "Remote state must be reconciled before retry.");
            }
        } catch (IOException ignored) {
            // If queue persistence itself is unavailable, keeping UPLOADING is still fail-closed.
            // Process-start recovery later converts a readable UPLOADING record to UNCERTAIN.
        }
    }

    private void persistDriveFailure(String photoId, DrivePhotoUploader.UploadException error)
            throws IOException {
        String detail = nonblankDetail(error);
        try {
            if (error.remoteStateUncertain()) {
                photoStore.markUploadUncertain(photoId, detail);
            } else {
                photoStore.markUploadFailed(photoId, detail);
            }
        } catch (IOException stateError) {
            throw new IOException(
                    "Upload stopped, and its queue result could not be committed safely. "
                            + "The protected photo was kept; do not retry until state is inspected.",
                    stateError);
        }
    }

    private void persistUnexpectedUncertainty(String photoId, RuntimeException error)
            throws IOException {
        try {
            PendingPhotoRecord current = photoStore.getById(photoId);
            if (current != null && current.state() == PendingPhotoRecord.State.UPLOADING) {
                photoStore.markUploadUncertain(
                        photoId,
                        "Unexpected provider failure after upload began. Remote state must be reconciled before retry.");
            }
        } catch (IOException stateError) {
            throw new IOException(
                    "Unexpected upload failure and queue state could not be committed. "
                            + "The protected photo was kept; do not retry until state is inspected.",
                    stateError);
        }
    }

    private PendingPhotoRecord requireRecord(String photoId) throws IOException {
        PendingPhotoRecord record = photoStore.getById(photoId);
        if (record == null) {
            throw new IOException("The selected temporary photo no longer exists.");
        }
        return record;
    }

    private static String nonblankDetail(Throwable error) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.isBlank()
                ? "Upload did not complete safely."
                : message;
    }
}
