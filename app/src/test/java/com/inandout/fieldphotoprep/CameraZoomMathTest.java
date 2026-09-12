package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class CameraZoomMathTest {
    @Test
    public void sliderProgressMapsAcrossLinearZoomRange() {
        assertEquals(0f, CameraZoomMath.linearZoomFromProgress(0), 0.0001f);
        assertEquals(0.5f, CameraZoomMath.linearZoomFromProgress(500), 0.0001f);
        assertEquals(1f, CameraZoomMath.linearZoomFromProgress(1000), 0.0001f);
        assertEquals(0f, CameraZoomMath.linearZoomFromProgress(-100), 0.0001f);
        assertEquals(1f, CameraZoomMath.linearZoomFromProgress(1200), 0.0001f);
    }

    @Test
    public void linearZoomMapsBackToSliderProgress() {
        assertEquals(0, CameraZoomMath.progressFromLinearZoom(0f));
        assertEquals(250, CameraZoomMath.progressFromLinearZoom(0.25f));
        assertEquals(1000, CameraZoomMath.progressFromLinearZoom(1f));
        assertEquals(0, CameraZoomMath.progressFromLinearZoom(-1f));
        assertEquals(1000, CameraZoomMath.progressFromLinearZoom(2f));
    }

    @Test
    public void pinchTargetClampsToCameraSupportedRange() {
        assertEquals(2f, CameraZoomMath.pinchTarget(1f, 2f, 1f, 8f), 0.0001f);
        assertEquals(8f, CameraZoomMath.pinchTarget(6f, 2f, 1f, 8f), 0.0001f);
        assertEquals(1f, CameraZoomMath.pinchTarget(2f, 0.25f, 1f, 8f), 0.0001f);
    }

    @Test
    public void invalidScaleFactorKeepsCurrentZoomSafelyClamped() {
        assertEquals(3f, CameraZoomMath.pinchTarget(3f, 0f, 1f, 8f), 0.0001f);
        assertEquals(1f, CameraZoomMath.pinchTarget(0.5f, Float.NaN, 1f, 8f), 0.0001f);
    }
}