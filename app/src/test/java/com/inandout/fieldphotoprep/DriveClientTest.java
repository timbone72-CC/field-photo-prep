package com.inandout.fieldphotoprep;

import android.provider.DocumentsContract;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DriveClientTest {
    @Test
    public void directoryMimeTypeIsAccepted() {
        assertTrue(DriveClient.isFolderMimeType(DocumentsContract.Document.MIME_TYPE_DIR));
    }

    @Test
    public void ordinaryFilesAreNotPresentedAsAddressFolders() {
        assertFalse(DriveClient.isFolderMimeType("image/jpeg"));
        assertFalse(DriveClient.isFolderMimeType("application/pdf"));
    }
}
