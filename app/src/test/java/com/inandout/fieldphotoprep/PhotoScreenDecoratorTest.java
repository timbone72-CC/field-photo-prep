package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.lang.reflect.Method;

public final class PhotoScreenDecoratorTest {
    private String friendly(String value) throws Exception {
        Method method = PhotoScreenDecorator.class.getDeclaredMethod("friendlyPhotoLabel", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, value);
    }

    @Test
    public void preparedWaitingPhotoIsShownAsReadyWithoutTechnicalId() throws Exception {
        assertEquals(
                "Ready to upload\nSep 11, 10:15 PM",
                friendly("WAITING · PREPARED\nSep 11, 10:15 PM · …1234ABCD"));
    }

    @Test
    public void uncertainPhotoIsShownAsNeedsAttention() throws Exception {
        assertEquals(
                "Needs attention\nSep 11, 10:16 PM",
                friendly("UNCERTAIN · attempt 1 · PREPARED\nSep 11, 10:16 PM · …5F0C11CC"));
    }

    @Test
    public void reconcilingUncertainPhotoIsShownAsCheckingUpload() throws Exception {
        assertEquals(
                "Checking upload\nSep 11, 10:16 PM",
                friendly("UNCERTAIN · attempt 1 · RECONCILING · PREPARED\nSep 11, 10:16 PM · …5F0C11CC"));
    }

    @Test
    public void uploadedPhotoIsShownAsUploaded() throws Exception {
        assertEquals(
                "Uploaded\nSep 11, 10:17 PM",
                friendly("UPLOADED · attempt 1\nSep 11, 10:17 PM · …05B7105E"));
    }

    @Test
    public void failedPhotoKeepsRetrySafeMeaning() throws Exception {
        assertEquals(
                "Upload failed — safe to retry\nSep 11, 10:18 PM",
                friendly("FAILED · attempt 1 · PREPARED\nSep 11, 10:18 PM · …5180E9A8"));
    }
}
