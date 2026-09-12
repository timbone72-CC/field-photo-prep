package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CameraLensMathTest {
    @Test
    public void effectiveRatioCombinesPhysicalLensAndLocalZoom() {
        assertEquals(0.6f, CameraLensMath.effectiveRatio(0.6f, 1f), 0.0001f);
        assertEquals(1.2f, CameraLensMath.effectiveRatio(0.6f, 2f), 0.0001f);
        assertEquals(3f, CameraLensMath.effectiveRatio(1f, 3f), 0.0001f);
    }

    @Test
    public void desiredEffectiveRatioMapsToPhysicalCameraZoom() {
        assertEquals(1f,
                CameraLensMath.localRatioForEffective(0.6f, 0.6f, 1f, 10f),
                0.0001f);
        assertEquals(1.6667f,
                CameraLensMath.localRatioForEffective(1f, 0.6f, 1f, 10f),
                0.001f);
    }

    @Test
    public void localRatioIsClampedToCurrentCameraRange() {
        assertEquals(1f,
                CameraLensMath.localRatioForEffective(0.2f, 0.6f, 1f, 8f),
                0.0001f);
        assertEquals(8f,
                CameraLensMath.localRatioForEffective(12f, 1f, 1f, 8f),
                0.0001f);
    }

    @Test
    public void ultraWideRequiresRealSubOneRatio() {
        assertTrue(CameraLensMath.isUltraWide(0.6f));
        assertFalse(CameraLensMath.isUltraWide(0.95f));
        assertFalse(CameraLensMath.isUltraWide(1f));
        assertFalse(CameraLensMath.isUltraWide(Float.NaN));
    }
}
