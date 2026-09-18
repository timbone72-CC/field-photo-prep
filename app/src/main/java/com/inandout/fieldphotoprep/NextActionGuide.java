package com.inandout.fieldphotoprep;

/**
 * Stateless presentation policy for the routine field workflow.
 *
 * It derives one suggested action from already-authoritative app state. It owns no persistence,
 * Drive behavior, queue transition, navigation history, or destructive action.
 */
final class NextActionGuide {
    enum Kind {
        CONNECT_DRIVE,
        ADD_PROPERTY,
        CHOOSE_PROPERTY,
        OPEN_WORK_ORDERS,
        CHOOSE_OR_ADD_WORK_ORDER,
        TAKE_PHOTOS,
        PREPARING,
        UPLOADING,
        CHECKING_UPLOADS,
        CHECK_UPLOADS,
        PREPARE_PHOTO,
        SELECT_READY,
        UPLOAD_SELECTED,
        DONE,
        REVIEW_PHOTOS
    }

    static final class Action {
        private final Kind kind;
        private final String label;
        private final boolean enabled;

        Action(Kind kind, String label, boolean enabled) {
            this.kind = kind;
            this.label = label;
            this.enabled = enabled;
        }

        Kind kind() {
            return kind;
        }

        String label() {
            return label;
        }

        boolean enabled() {
            return enabled;
        }
    }

    private NextActionGuide() {
    }

    static Action home(
            boolean busy,
            boolean driveConnected,
            int propertyCount,
            boolean savedPropertyAvailable) {
        if (busy) {
            return disabled(Kind.CHOOSE_PROPERTY, "Loading…");
        }
        if (!driveConnected) {
            return enabled(Kind.CONNECT_DRIVE, "Next: Connect Google Drive");
        }
        if (propertyCount <= 0) {
            return enabled(Kind.ADD_PROPERTY, "Next: Add a Property");
        }
        if (!savedPropertyAvailable) {
            return disabled(Kind.CHOOSE_PROPERTY, "Next: Choose a Property");
        }
        return enabled(Kind.OPEN_WORK_ORDERS, "Next: Open Work Orders");
    }

    static Action workOrders(boolean busy, boolean selectedWorkOrderAvailable) {
        if (busy) {
            return disabled(Kind.CHOOSE_OR_ADD_WORK_ORDER, "Working…");
        }
        if (!selectedWorkOrderAvailable) {
            return disabled(Kind.CHOOSE_OR_ADD_WORK_ORDER, "Next: Choose or Add Work Order");
        }
        return enabled(Kind.TAKE_PHOTOS, "Next: Take Photos");
    }

    static Action photos(
            boolean preparationBusy,
            boolean remoteBusy,
            boolean reconciliationBusy,
            int currentPhotoCount,
            int uncertainCount,
            int waitingUnpreparedCount,
            int readyCount,
            int selectedCount,
            boolean selectedUploadEligible,
            boolean allCurrentUploaded) {
        if (preparationBusy) {
            return disabled(Kind.PREPARING, "Preparing Photo…");
        }
        if (remoteBusy) {
            return disabled(
                    reconciliationBusy ? Kind.CHECKING_UPLOADS : Kind.UPLOADING,
                    reconciliationBusy ? "Checking Uploads…" : "Uploading…");
        }
        if (uncertainCount > 0) {
            return enabled(
                    Kind.CHECK_UPLOADS,
                    "Next: Check " + uncertainCount + " Upload"
                            + (uncertainCount == 1 ? "" : "s"));
        }
        if (currentPhotoCount <= 0) {
            return enabled(Kind.TAKE_PHOTOS, "Next: Take Photos");
        }
        if (waitingUnpreparedCount > 0) {
            return enabled(Kind.PREPARE_PHOTO, "Next: Prepare Photo");
        }
        if (readyCount > 0) {
            if (selectedCount > 0 && selectedUploadEligible) {
                return enabled(
                        Kind.UPLOAD_SELECTED,
                        "Next: Upload Selected (" + selectedCount + ")");
            }
            return enabled(
                    Kind.SELECT_READY,
                    "Next: Select " + readyCount + " Ready Photo"
                            + (readyCount == 1 ? "" : "s"));
        }
        if (allCurrentUploaded) {
            return enabled(Kind.DONE, "Done — Return to Work Orders");
        }
        return disabled(Kind.REVIEW_PHOTOS, "Review Photo Status");
    }

    private static Action enabled(Kind kind, String label) {
        return new Action(kind, label, true);
    }

    private static Action disabled(Kind kind, String label) {
        return new Action(kind, label, false);
    }
}
