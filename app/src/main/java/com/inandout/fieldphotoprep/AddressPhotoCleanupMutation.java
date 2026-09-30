package com.inandout.fieldphotoprep;

import java.io.IOException;

final class AddressPhotoCleanupMutation {
    interface Operations {
        void deletePhoto(String photoDocumentId) throws IOException;
        DriveClient.ChildSnapshot readWorkOrderChildren(String workOrderDocumentId)
                throws IOException;
    }

    static final class Failure extends IOException {
        private final int removedCount;
        private final boolean remoteMutationAttempted;

        Failure(
                String message,
                int removedCount,
                boolean remoteMutationAttempted,
                Throwable cause) {
            super(message, cause);
            this.removedCount = removedCount;
            this.remoteMutationAttempted = remoteMutationAttempted;
        }

        int removedCount() {
            return removedCount;
        }

        boolean remoteMutationAttempted() {
            return remoteMutationAttempted;
        }
    }

    private AddressPhotoCleanupMutation() {}

    static int removeApprovedPhotosAndVerifyEmpty(
            AddressPhotoCleanupPlan plan,
            Operations operations) throws Failure {
        if (plan == null || operations == null) {
            throw new IllegalArgumentException("Cleanup plan and operations are required.");
        }

        int removedCount = 0;
        boolean remoteMutationAttempted = false;
        try {
            for (AddressPhotoCleanupPlan.WorkOrderTarget target : plan.targets()) {
                for (String photoId : target.photoDocumentIds()) {
                    remoteMutationAttempted = true;
                    operations.deletePhoto(photoId);
                    removedCount++;
                }
            }

            for (AddressPhotoCleanupPlan.WorkOrderTarget target : plan.targets()) {
                DriveClient.ChildSnapshot after =
                        operations.readWorkOrderChildren(target.folder().id());
                if (after.photoCount() != 0) {
                    throw new IOException(
                            "Drive still reports " + after.photoCount()
                                    + " photo"
                                    + (after.photoCount() == 1 ? "" : "s")
                                    + " in "
                                    + PropertyDisplayName.readableFolderName(target.folder().name())
                                    + ".");
                }
            }
            return removedCount;
        } catch (Exception error) {
            throw new Failure(
                    "Address photo cleanup did not complete.",
                    removedCount,
                    remoteMutationAttempted,
                    error);
        }
    }
}
