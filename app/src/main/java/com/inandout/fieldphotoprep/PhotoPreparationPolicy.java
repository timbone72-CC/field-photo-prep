package com.inandout.fieldphotoprep;

public final class PhotoPreparationPolicy {
    public static final int MAX_LONG_EDGE = 2048;
    public static final int JPEG_QUALITY = 85;

    public static final class Dimensions {
        private final int width;
        private final int height;

        Dimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }
    }

    private PhotoPreparationPolicy() {
    }

    public static Dimensions targetDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Photo dimensions must be positive.");
        }

        int longEdge = Math.max(width, height);
        if (longEdge <= MAX_LONG_EDGE) {
            return new Dimensions(width, height);
        }

        double scale = (double) MAX_LONG_EDGE / (double) longEdge;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        return new Dimensions(targetWidth, targetHeight);
    }

    public static String preparedFileNameFor(String photoId) {
        // Reuse Phase 5 UUID validation without coupling identity to a visible name or timestamp.
        PendingPhotoRecord.imageFileNameFor(photoId);
        return "prepared-" + photoId + ".jpg";
    }
}
