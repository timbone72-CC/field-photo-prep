package com.inandout.fieldphotoprep;

import android.provider.DocumentsContract;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void exactWorkOrderMatchDoesNotUseCaseOrPartialGuessing() {
        List<DriveFolder> folders = Arrays.asList(
                new DriveFolder("1", "Cut Grass - 2026-09-06"),
                new DriveFolder("2", "cut grass - 2026-09-06"),
                new DriveFolder("3", "Cut Grass - 2026-09-13"));

        List<DriveFolder> matches = DriveClient.findExactNameMatches(
                folders, "Cut Grass - 2026-09-06");

        assertEquals(1, matches.size());
        assertEquals("1", matches.get(0).id());
    }

    @Test
    public void duplicateExactNamesRemainMultipleForOperatorChoice() {
        List<DriveFolder> folders = Arrays.asList(
                new DriveFolder("b", "Cut Grass - 2026-09-06"),
                new DriveFolder("a", "Cut Grass - 2026-09-06"));

        List<DriveFolder> matches = DriveClient.findExactNameMatches(
                folders, "Cut Grass - 2026-09-06");

        assertEquals(2, matches.size());
        assertEquals("a", matches.get(0).id());
        assertEquals("b", matches.get(1).id());
    }
}
