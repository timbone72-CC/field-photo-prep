package com.inandout.fieldphotoprep;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Coordinates operator-confirmed local photo discard without owning queue-state rules.
 *
 * PendingPhotoRecord/PendingPhotoStore remain authoritative for whether a photo may be discarded.
 * This coordinator adds exact address/work-order binding checks, whole-batch preflight, and
 * fail-closed sequential execution. It performs no Drive/provider operation.
 */
final class LocalPhotoDiscardCoordinator {
    static final class BatchResult {
        private final int selectedCount;
        private final int discardedCount;
        private final String stoppedPhotoId;
        private final String failureDetail;

        BatchResult(
                int selectedCount,
                int discardedCount,
                String stoppedPhotoId,
                String failureDetail) {
            this.selectedCount = selectedCount;
            this.discardedCount = discardedCount;
            this.stoppedPhotoId = stoppedPhotoId;
            this.failureDetail = failureDetail;
        }

        int selectedCount() {
            return selectedCount;
        }

        int discardedCount() {
            return discardedCount;
        }

        String stoppedPhotoId() {
            return stoppedPhotoId;
        }

        String failureDetail() {
            return failureDetail;
        }

        boolean complete() {
            return failureDetail == null && discardedCount == selectedCount;
        }
    }

    private final PendingPhotoStore photoStore;
    private final PhotoPreparer photoPreparer;

    LocalPhotoDiscardCoordinator(
            PendingPhotoStore photoStore,
            PhotoPreparer photoPreparer) {
        if (photoStore == null || photoPreparer == null) {
            throw new IllegalArgumentException("Local photo discard requires photo storage and preparation owners.");
        }
        this.photoStore = photoStore;
        this.photoPreparer = photoPreparer;
    }

    List<String> validateSnapshot(
            List<String> selectedPhotoIds,
            String addressId,
            String workOrderId) throws IOException {
        if (selectedPhotoIds == null || selectedPhotoIds.isEmpty()) {
            throw new IOException("Select at least one local photo to discard.");
        }
        if (addressId == null || addressId.trim().isEmpty()
                || workOrderId == null || workOrderId.trim().isEmpty()) {
            throw new IOException("An exact address and work order are required before local discard.");
        }

        LinkedHashSet<String> unique = new LinkedHashSet<>(selectedPhotoIds);
        if (unique.size() != selectedPhotoIds.size() || unique.contains(null)) {
            throw new IOException("The selected local photo identities are not safe to discard as a batch.");
        }

        ArrayList<String> snapshot = new ArrayList<>(unique);
        for (String photoId : snapshot) {
            requireDiscardSafe(photoId, addressId, workOrderId);
        }
        return snapshot;
    }

    BatchResult discardBatch(
            List<String> selectedPhotoIds,
            String addressId,
            String workOrderId) {
        final List<String> snapshot;
        try {
            snapshot = validateSnapshot(selectedPhotoIds, addressId, workOrderId);
        } catch (Exception error) {
            int count = selectedPhotoIds == null ? 0 : selectedPhotoIds.size();
            return new BatchResult(count, 0, null, detail(error));
        }

        int discarded = 0;
        for (String photoId : snapshot) {
            try {
                discardOne(photoId, addressId, workOrderId);
                discarded++;
            } catch (Exception error) {
                return new BatchResult(snapshot.size(), discarded, photoId, detail(error));
            }
        }
        return new BatchResult(snapshot.size(), discarded, null, null);
    }

    void discardOne(
            String photoId,
            String addressId,
            String workOrderId) throws IOException {
        requireDiscardSafe(photoId, addressId, workOrderId);

        File prepared = photoPreparer.preparedFile(photoId);
        if (prepared.exists() && !prepared.delete()) {
            throw new IOException(
                    "Could not remove the prepared local copy. The protected original was left unchanged.");
        }

        // PendingPhotoStore re-reads the persisted record and enforces canDiscardLocally() again.
        photoStore.discard(photoId);
    }

    private PendingPhotoRecord requireDiscardSafe(
            String photoId,
            String addressId,
            String workOrderId) throws IOException {
        if (photoId == null || photoId.trim().isEmpty()) {
            throw new IOException("A selected local photo identity is missing.");
        }

        PendingPhotoRecord record = photoStore.getById(photoId);
        if (record == null) {
            throw new IOException("A selected temporary photo no longer exists.");
        }
        if (!addressId.equals(record.addressId()) || !workOrderId.equals(record.workOrderId())) {
            throw new IOException("A selected photo belongs to a different stored address/work-order destination.");
        }
        if (!record.canDiscardLocally()) {
            throw new IOException("Photo …" + shortId(photoId) + " is " + record.state().name()
                    + " and must be kept because upload/duplicate-protection evidence may still be needed.");
        }
        return record;
    }

    private static String detail(Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()) {
            return "Local discard could not complete safely.";
        }
        return error.getMessage();
    }

    private static String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(id.length() - 8);
    }
}
