package com.inandout.fieldphotoprep;

/**
 * Process-local ownership guard for photo preparation.
 *
 * Every instance shares one process-wide owner. This lets existing manual preparation controls
 * and the automatic preparation queue coordinate without allowing two full image decode/compress
 * operations to run at the same time. Process death clears the guard with the worker threads.
 */
public final class PhotoPreparationGate {
    private static String activePhotoId;

    public boolean tryBegin(String photoId) {
        requirePhotoId(photoId);
        synchronized (PhotoPreparationGate.class) {
            if (activePhotoId != null) {
                return false;
            }
            activePhotoId = photoId;
            return true;
        }
    }

    public boolean isBusy() {
        synchronized (PhotoPreparationGate.class) {
            return activePhotoId != null;
        }
    }

    public boolean isPreparing(String photoId) {
        synchronized (PhotoPreparationGate.class) {
            return photoId != null && photoId.equals(activePhotoId);
        }
    }

    public String activePhotoId() {
        synchronized (PhotoPreparationGate.class) {
            return activePhotoId;
        }
    }

    public void finish(String photoId) {
        synchronized (PhotoPreparationGate.class) {
            if (photoId != null && photoId.equals(activePhotoId)) {
                activePhotoId = null;
            }
        }
    }

    static void resetForTests() {
        synchronized (PhotoPreparationGate.class) {
            activePhotoId = null;
        }
    }

    private static void requirePhotoId(String photoId) {
        if (photoId == null || photoId.isBlank()) {
            throw new IllegalArgumentException("photoId is required.");
        }
    }
}
