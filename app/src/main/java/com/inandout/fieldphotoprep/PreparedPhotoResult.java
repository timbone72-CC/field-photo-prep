package com.inandout.fieldphotoprep;

import java.io.File;
import java.util.Objects;

public final class PreparedPhotoResult {
    private final String photoId;
    private final File file;
    private final int width;
    private final int height;
    private final long originalBytes;
    private final long preparedBytes;

    public PreparedPhotoResult(
            String photoId,
            File file,
            int width,
            int height,
            long originalBytes,
            long preparedBytes) {
        this.photoId = Objects.requireNonNull(photoId);
        this.file = Objects.requireNonNull(file);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Prepared photo dimensions must be positive.");
        }
        if (originalBytes <= 0 || preparedBytes <= 0) {
            throw new IllegalArgumentException("Prepared photo byte counts must be positive.");
        }
        this.width = width;
        this.height = height;
        this.originalBytes = originalBytes;
        this.preparedBytes = preparedBytes;
    }

    public String photoId() {
        return photoId;
    }

    public File file() {
        return file;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public long originalBytes() {
        return originalBytes;
    }

    public long preparedBytes() {
        return preparedBytes;
    }
}
