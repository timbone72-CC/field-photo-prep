package com.inandout.fieldphotoprep;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class PhotoUploadCoordinator {
    private final PendingPhotoStore photoStore;
    private final PhotoPreparer photoPreparer;
    private final DrivePhotoUploader driveUploader;

    public PhotoUploadCoordinator(
            PendingPhotoStore photoStore,
            PhotoPreparer photoPreparer,
            DrivePhotoUploader driveUploader) {
        this.photoStore = Objects.requireNonNull(photoStore, "photoStore");
        this.photoPreparer = Objects.requireNonNull(photoPreparer, "photoPreparer");
        this.driveUploader = Objects.requireNonNull(driveUploader, "driveUploader");
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

        PendingPhotoRecord uploading = photoStore.beginUploadAttempt(photoId);
        final DrivePhotoUploader.UploadResult remoteResult;
        try {
            remoteResult = driveUploader.upload(uploading, prepared);
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
                            "Drive returned a remote photo identity, but local confirmation bookkeeping failed. "
                                    + "Remote state must be reconciled before retry.");
                } catch (IOException ignored) {
                    // Leaving UPLOADING is still fail-closed; process startup converts it to UNCERTAIN.
                }
            }
            throw new IOException(
                    "Drive returned a photo identity, but local confirmation could not be committed safely. "
                            + "The local photo was kept and retry is blocked until the result is reconciled.",
                    confirmationError);
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
