package com.inandout.fieldphotoprep;

import android.provider.DocumentsContract;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class DriveClientTest {
    @Test
    public void deniedAuthorizationStopsFolderCreateBeforeProviderCall() throws Exception {
        DriveClient client = new DriveClient(deniedGuard());

        try {
            client.createFolder(null, null, "parent", "New Folder");
            fail("Expected authorization denial");
        } catch (IOException error) {
            assertTrue(error.getMessage().contains("revoked"));
        }
    }

    @Test
    public void deniedAuthorizationStopsFolderRenameBeforeProviderCall() throws Exception {
        DriveClient client = new DriveClient(deniedGuard());

        try {
            client.renameFolder(null, null, "folder", "Renamed Folder");
            fail("Expected authorization denial");
        } catch (IOException error) {
            assertTrue(error.getMessage().contains("revoked"));
        }
    }

    @Test
    public void deniedAuthorizationStopsFolderDeleteBeforeProviderCall() throws Exception {
        DriveClient client = new DriveClient(deniedGuard());

        try {
            client.deleteDocument(null, null, "child");
            fail("Expected authorization denial");
        } catch (IOException error) {
            assertTrue(error.getMessage().contains("revoked"));
        }
    }

    private static AuthorizationActionGuard deniedGuard() {
        return new AuthorizationActionGuard(() -> new AuthorizationDecision(
                AuthorizationDecision.State.REVOKED,
                "user-1",
                "org-1",
                "OWNER",
                0L));
    }

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
    public void exactAddressMatchIsCaseSensitive() {
        List<DriveFolder> folders = Arrays.asList(
                new DriveFolder("1", "1607 Crestview Drive"),
                new DriveFolder("2", "1607 CRESTVIEW DRIVE"),
                new DriveFolder("3", "1607 Crestview Drive Apt 2"));

        List<DriveFolder> matches = DriveClient.findExactNameMatches(
                folders, "1607 Crestview Drive");

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
    public void impossibleSelfChildListingIsDetectedByProviderId() {
        List<DriveFolder> folders = Arrays.asList(
                new DriveFolder("selected-address", "1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST"),
                new DriveFolder("sibling-address", "1611_NW_SMITH_AVE"));

        assertTrue(DriveClient.containsFolderId(folders, "selected-address"));
        assertFalse(DriveClient.containsFolderId(folders, "real-work-order"));
    }

    @Test
    public void settledFolderSnapshotsMatchRegardlessOfOrder() {
        List<DriveFolder> first = Arrays.asList(
                new DriveFolder("2", "B Address"),
                new DriveFolder("1", "A Address"));
        List<DriveFolder> second = Arrays.asList(
                new DriveFolder("1", "A Address"),
                new DriveFolder("2", "B Address"));

        assertTrue(DriveClient.sameFolders(first, second));
    }

    @Test
    public void settledFolderSnapshotsRejectRenamedOrChangedIdentity() {
        List<DriveFolder> baseline = Arrays.asList(
                new DriveFolder("1", "A Address"),
                new DriveFolder("2", "B Address"));

        assertFalse(DriveClient.sameFolders(
                baseline,
                Arrays.asList(
                        new DriveFolder("1", "A Address Renamed"),
                        new DriveFolder("2", "B Address"))));
        assertFalse(DriveClient.sameFolders(
                baseline,
                Arrays.asList(
                        new DriveFolder("1", "A Address"),
                        new DriveFolder("3", "B Address"))));
        assertFalse(DriveClient.sameFolders(
                baseline,
                Arrays.asList(new DriveFolder("1", "A Address"))));
    }

    @Test
    public void addressCreateRequiresAcceptedRefreshAndSettledProviderState() {
        assertTrue(DriveClient.isAuthoritativeFolderState(true, false));
        assertFalse(DriveClient.isAuthoritativeFolderState(false, false));
        assertFalse(DriveClient.isAuthoritativeFolderState(true, true));
        assertFalse(DriveClient.isAuthoritativeFolderState(false, true));
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

    @Test
    public void destructiveReuseRequiresProviderRefreshAndSettledCursor() {
        assertTrue(DriveClient.isAuthoritativeChildState(true, false));
        assertFalse(DriveClient.isAuthoritativeChildState(false, false));
        assertFalse(DriveClient.isAuthoritativeChildState(true, true));
        assertFalse(DriveClient.isAuthoritativeChildState(false, true));
    }
}
