package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class CameraFlashModeTest {
    @Test
    public void defaultsAndCyclesAutoOnOff() {
        assertEquals("Flash: Auto", CameraFlashMode.AUTO.buttonLabel());
        assertEquals(CameraFlashMode.ON, CameraFlashMode.AUTO.next());
        assertEquals(CameraFlashMode.OFF, CameraFlashMode.ON.next());
        assertEquals(CameraFlashMode.AUTO, CameraFlashMode.OFF.next());
    }
}
