package com.inandout.fieldphotoprep;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Computes whether signing out would strand protected local work.
 *
 * This is intentionally read-only. It derives its answer from durable queue records and actual
 * local image/prepared-copy presence rather than from UI counters.
 */
final class ProtectedWorkGuard {
    static final class Result {
        private final int blockingCount;
        private final int capturingCount;
        private final int queuedCount;
        private final int cleanupPendingCount;
        private final int unreadableCount;

        Result(
                int blockingCount,
                int capturingCount,
                int queuedCount,
                int cleanupPendingCount,
                int unreadableCount) {
            this.blockingCount = blockingCount;
            this.capturingCount = capturingCount;
            this.queuedCount = queuedCount;
            this.cleanupPendingCount = cleanupPendingCount;
            this.unreadableCount = unreadableCount;
        }

        boolean blocksSignOut() {
            return blockingCount > 0;
        }

        int blockingCount() {
            return blockingCount;
        }

        int capturingCount() {
            return capturingCount;
        }

        int queuedCount() {
            return queuedCount;
        }

        int cleanupPendingCount() {
            return cleanupPendingCount;
        }

        int unreadableCount() {
            return unreadableCount;
        }
    }

    private final PendingPhotoStore photoStore;
    private final PhotoPreparer photoPreparer;

    ProtectedWorkGuard(PendingPhotoStore photoStore, PhotoPreparer photoPreparer) {
        this.photoStore = Objects.requireNonNull(photoStore, "photoStore");
        this.photoPreparer = Objects.requireNonNull(photoPreparer, "photoPreparer");
    }

    Result inspect() throws IOException {
        PendingPhotoStore.ScanResult scan =
                photoStore.scanAllPersistedRecordsForProtection();
        int capturing = 0;
        int queued = 0;
        int cleanupPending = 0;
        int unreadable = scan.corruptMetadataFiles().size();

        for (PendingPhotoRecord record : scan.records()) {
            switch (record.state()) {
                case CAPTURING:
                    if (hasNonEmptyProtectedOriginal(record)) {
                        capturing++;
                    }
                    break;
                case WAITING:
                case UPLOADING:
                case FAILED:
                case UNCERTAIN:
                    queued++;
                    break;
                case UPLOADED:
                    if (hasAnyLocalCopy(record)) {
                        cleanupPending++;
                    }
                    break;
                default:
                    throw new IOException("Unsupported protected-photo state.");
            }
        }

        int total = capturing + queued + cleanupPending + unreadable;
        return new Result(total, capturing, queued, cleanupPending, unreadable);
    }

    private boolean hasNonEmptyProtectedOriginal(PendingPhotoRecord record) throws IOException {
        return photoStore.hasImageData(record);
    }

    private boolean hasAnyLocalCopy(PendingPhotoRecord record) throws IOException {
        if (photoStore.hasImageData(record)) {
            return true;
        }
        File prepared = photoPreparer.preparedFile(record.id());
        return prepared.isFile() && prepared.length() > 0L;
    }
}
