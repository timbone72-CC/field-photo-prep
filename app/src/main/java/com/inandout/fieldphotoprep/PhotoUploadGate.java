package com.inandout.fieldphotoprep;

/** Process-local guard that prevents duplicate concurrent Drive upload attempts. */
public final class PhotoUploadGate {
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

    public synchronized boolean isUploading(String photoId) {
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
