package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PhotoPreparationGateTest {
    @Test
    public void onePreparationOwnsTheGateUntilItFinishes() {
        PhotoPreparationGate gate = new PhotoPreparationGate();

        assertTrue(gate.tryBegin("photo-a"));
        assertTrue(gate.isBusy());
        assertTrue(gate.isPreparing("photo-a"));
        assertFalse(gate.tryBegin("photo-b"));
        assertFalse(gate.isPreparing("photo-b"));

        gate.finish("photo-a");

        assertFalse(gate.isBusy());
        assertNull(gate.activePhotoId());
        assertTrue(gate.tryBegin("photo-b"));
    }

    @Test
    public void wrongPhotoCannotReleaseAnotherPhotosOwnership() {
        PhotoPreparationGate gate = new PhotoPreparationGate();
        assertTrue(gate.tryBegin("photo-a"));

        gate.finish("photo-b");

        assertTrue(gate.isBusy());
        assertTrue(gate.isPreparing("photo-a"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankPhotoIdentityCannotAcquireGate() {
        new PhotoPreparationGate().tryBegin("   ");
    }
}
