package com.inandout.fieldphotoprep;

final class CameraLensMath {
    private CameraLensMath() {
    }

    static float effectiveRatio(float intrinsicRatio, float cameraZoomRatio) {
        float safeIntrinsic = isUsableRatio(intrinsicRatio) ? intrinsicRatio : 1f;
        float safeCameraZoom = isUsableRatio(cameraZoomRatio) ? cameraZoomRatio : 1f;
        return safeIntrinsic * safeCameraZoom;
    }

    static float localRatioForEffective(
            float desiredEffectiveRatio,
            float intrinsicRatio,
            float minLocalRatio,
            float maxLocalRatio) {
        float safeIntrinsic = isUsableRatio(intrinsicRatio) ? intrinsicRatio : 1f;
        float requestedLocal = isUsableRatio(desiredEffectiveRatio)
                ? desiredEffectiveRatio / safeIntrinsic
                : 1f;
        return CameraZoomMath.clampRatio(requestedLocal, minLocalRatio, maxLocalRatio);
    }

    static boolean isUltraWide(float intrinsicOrEffectiveRatio) {
        return isUsableRatio(intrinsicOrEffectiveRatio) && intrinsicOrEffectiveRatio < 0.95f;
    }

    static boolean isUsableRatio(float ratio) {
        return !Float.isNaN(ratio) && !Float.isInfinite(ratio) && ratio > 0f;
    }
}
