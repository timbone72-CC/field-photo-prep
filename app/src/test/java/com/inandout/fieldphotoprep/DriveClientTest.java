package com.inandout.fieldphotoprep;

import android.provider.DocumentsContract;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

    @Test
    public void folderIdentityLookupUsesIdNotVisibleName() {
        List<DriveFolder> folders = Arrays.asList(
                new DriveFolder("old-id", "Cut Grass - 2026-09-06"),
                new DriveFolder("target-id", "Cut Grass - 2026-09-06"));

        DriveFolder found = DriveClient.findById(folders, "target-id");

        assertEquals("target-id", found.id());
        assertNull(DriveClient.findById(folders, "missing-id"));
    }

    @Test
    public void clearReuseConfirmationSnapshotMatchesSameIdsRegardlessOfOrder() {
        assertTrue(DriveClient.sameDocumentIds(
                Arrays.asList("photo-2", "photo-1"),
                Arrays.asList("photo-1", "photo-2")));
    }

    @Test
    public void clearReuseConfirmationSnapshotRejectsChangedContents() {
        assertFalse(DriveClient.sameDocumentIds(
                Arrays.asList("photo-1", "photo-2"),
                Arrays.asList("photo-1", "photo-3")));
        assertFalse(DriveClient.sameDocumentIds(
                Arrays.asList("photo-1", "photo-2"),
                Arrays.asList("photo-1")));
    }

    @Test
    public void childSnapshotCountsDirectItemsAndChildFolders() {
        DriveClient.ChildSnapshot snapshot = new DriveClient.ChildSnapshot(
                Arrays.asList("photo", "nested-folder"), 1);

        assertEquals(2, snapshot.count());
        assertEquals(1, snapshot.folderCount());
        assertEquals(Arrays.asList("photo", "nested-folder"), snapshot.documentIds());
    }
}
