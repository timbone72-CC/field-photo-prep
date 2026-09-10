package com.inandout.fieldphotoprep;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class PhotoUploadGateTest {
    @Test
    public void onlyOneUploadOwnsTheProcessGateAtATime() {
        PhotoUploadGate gate = new PhotoUploadGate();

        assertTrue(gate.tryBegin("photo-one"));
        assertTrue(gate.isBusy());
        assertTrue(gate.isUploading("photo-one"));
        assertFalse(gate.tryBegin("photo-two"));
        assertEquals("photo-one", gate.activePhotoId());

        gate.finish("photo-two");
        assertTrue(gate.isBusy());

        gate.finish("photo-one");
        assertFalse(gate.isBusy());
        assertNull(gate.activePhotoId());
        assertTrue(gate.tryBegin("photo-two"));
    }
}
