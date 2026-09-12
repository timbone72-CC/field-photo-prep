package com.inandout.fieldphotoprep;

final class CameraZoomMath {
    static final int SLIDER_STEPS = 1000;

    private CameraZoomMath() {
    }

    static float clampRatio(float requested, float minRatio, float maxRatio) {
        float low = Math.min(minRatio, maxRatio);
        float high = Math.max(minRatio, maxRatio);
        if (Float.isNaN(requested) || Float.isInfinite(requested)) {
            return low;
        }
        return Math.max(low, Math.min(high, requested));
    }

    static float pinchTarget(
            float currentRatio,
            float scaleFactor,
            float minRatio,
            float maxRatio) {
        if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor) || scaleFactor <= 0f) {
            return clampRatio(currentRatio, minRatio, maxRatio);
        }
        return clampRatio(currentRatio * scaleFactor, minRatio, maxRatio);
    }

    static float linearZoomFromProgress(int progress) {
        int clamped = Math.max(0, Math.min(SLIDER_STEPS, progress));
        return clamped / (float) SLIDER_STEPS;
    }

    static int progressFromLinearZoom(float linearZoom) {
        if (Float.isNaN(linearZoom) || Float.isInfinite(linearZoom)) {
            return 0;
        }
        float clamped = Math.max(0f, Math.min(1f, linearZoom));
        return Math.round(clamped * SLIDER_STEPS);
    }
}