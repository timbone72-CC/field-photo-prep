package com.inandout.fieldphotoprep;

import org.junit.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DriveClientTest {
    @Test
    public void listQueryIsFolderOnlyAndParentScoped() {
        String url = DriveClient.buildListFoldersUrl("folder-123", null);
        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);

        assertTrue(decoded.contains("'folder-123' in parents"));
        assertTrue(decoded.contains("mimeType = 'application/vnd.google-apps.folder'"));
        assertTrue(decoded.contains("trashed = false"));
        assertFalse(decoded.contains("drive.readonly"));
    }

    @Test
    public void parentIdIsEscapedInsideDriveQuery() {
        String url = DriveClient.buildListFoldersUrl("a'b\\c", "next token");
        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);

        assertTrue(decoded.contains("'a\\'b\\\\c' in parents"));
        assertTrue(decoded.contains("pageToken=next token"));
    }
}
