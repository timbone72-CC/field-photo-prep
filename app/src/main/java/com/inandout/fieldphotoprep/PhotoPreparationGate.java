package com.inandout.fieldphotoprep;

/**
 * Process-local ownership guard for photo preparation.
 *
 * Only one protected photo may be prepared at a time. This prevents duplicate preparation
 * work and gives destructive local actions a single place to check before touching the same
 * protected original. Process death clears the guard together with the worker thread.
 */
public final class PhotoPreparationGate {
    private String activePhotoId;

    public synchronized boolean tryBegin(String photoId) {
        requirePhotoId(photoId);
        if (activePhotoId != null) {
            return false;
        }
        activePhotoId = photoId;
        return true;
    }

    public synchronized boolean isBusy() {
        return activePhotoId != null;
    }

    public synchronized boolean isPreparing(String photoId) {
        return photoId != null && photoId.equals(activePhotoId);
    }

    public synchronized String activePhotoId() {
        return activePhotoId;
    }

    public synchronized void finish(String photoId) {
        if (photoId != null && photoId.equals(activePhotoId)) {
            activePhotoId = null;
        }
    }

    private static void requirePhotoId(String photoId) {
        if (photoId == null || photoId.isBlank()) {
            throw new IllegalArgumentException("photoId is required.");
        }
    }
}
