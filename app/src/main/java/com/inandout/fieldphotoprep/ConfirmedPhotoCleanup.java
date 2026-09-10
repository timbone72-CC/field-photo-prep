package com.inandout.fieldphotoprep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Removes only local image copies after durable confirmed upload bookkeeping already exists.
 * Remote success is never rolled back because local cleanup is incomplete.
 */
public final class ConfirmedPhotoCleanup {
    public static final class Result {
        private final boolean protectedOriginalRemoved;
        private final boolean preparedCopyRemoved;
        private final List<String> failures;

        Result(
                boolean protectedOriginalRemoved,
                boolean preparedCopyRemoved,
                List<String> failures) {
            this.protectedOriginalRemoved = protectedOriginalRemoved;
            this.preparedCopyRemoved = preparedCopyRemoved;
            this.failures = Collections.unmodifiableList(new ArrayList<>(failures));
        }

        public boolean protectedOriginalRemoved() {
            return protectedOriginalRemoved;
        }

        public boolean preparedCopyRemoved() {
            return preparedCopyRemoved;
        }

        public boolean complete() {
            return protectedOriginalRemoved && preparedCopyRemoved;
        }

        public List<String> failures() {
            return failures;
        }

        public String summary() {
            if (complete()) {
                return "Confirmed upload retained; local original and prepared copy removed.";
            }
            return "Confirmed upload retained; local cleanup is incomplete and may be retried.";
        }
    }

    private final PendingPhotoStore photoStore;
    private final PhotoPreparer photoPreparer;

    public ConfirmedPhotoCleanup(PendingPhotoStore photoStore, PhotoPreparer photoPreparer) {
        this.photoStore = Objects.requireNonNull(photoStore, "photoStore");
        this.photoPreparer = Objects.requireNonNull(photoPreparer, "photoPreparer");
    }

    public Result cleanup(String photoId) throws IOException {
        PendingPhotoRecord record = photoStore.getById(photoId);
        if (record == null) {
            throw new IOException("The selected photo record no longer exists.");
        }
        if (record.state() != PendingPhotoRecord.State.UPLOADED || record.remoteFileId() == null) {
            throw new IOException(
                    "Local cleanup is allowed only after durable confirmed uploaded state.");
        }

        boolean originalRemoved = false;
        boolean preparedRemoved = false;
        List<String> failures = new ArrayList<>();

        try {
            photoStore.removeProtectedImageAfterConfirmedUpload(photoId);
            originalRemoved = true;
        } catch (IOException error) {
            failures.add(nonblank(error.getMessage(), "Protected original cleanup failed."));
        }

        try {
            photoPreparer.removePreparedCopyAfterConfirmedUpload(record);
            preparedRemoved = true;
        } catch (IOException error) {
            failures.add(nonblank(error.getMessage(), "Prepared copy cleanup failed."));
        }

        return new Result(originalRemoved, preparedRemoved, failures);
    }

    private static String nonblank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
