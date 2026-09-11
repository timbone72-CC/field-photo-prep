package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

public final class PhotoPreparationGateTest {
    @After
    public void resetGate() {
        PhotoPreparationGate.resetForTests();
    }

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
        gate.finish("photo-b");
    }

    @Test
    public void separateInstancesShareOneProcessWideOwner() {
        PhotoPreparationGate first = new PhotoPreparationGate();
        PhotoPreparationGate second = new PhotoPreparationGate();

        assertTrue(first.tryBegin("photo-a"));
        assertTrue(second.isBusy());
        assertEquals("photo-a", second.activePhotoId());
        assertFalse(second.tryBegin("photo-b"));

        second.finish("photo-a");

        assertFalse(first.isBusy());
        assertTrue(second.tryBegin("photo-b"));
        second.finish("photo-b");
    }

    @Test
    public void wrongPhotoCannotReleaseAnotherPhotosOwnership() {
        PhotoPreparationGate gate = new PhotoPreparationGate();
        assertTrue(gate.tryBegin("photo-a"));

        gate.finish("photo-b");

        assertTrue(gate.isBusy());
        assertTrue(gate.isPreparing("photo-a"));
        gate.finish("photo-a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankPhotoIdentityCannotAcquireGate() {
        new PhotoPreparationGate().tryBegin("   ");
    }
}
