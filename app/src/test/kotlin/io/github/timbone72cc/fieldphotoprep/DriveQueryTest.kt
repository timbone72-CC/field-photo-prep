package io.github.timbone72cc.fieldphotoprep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DriveQueryTest {
    @Test
    fun targetsOnlyDirectChildFoldersOfExactParent() {
        assertEquals(
            "'folder-123' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false",
            DriveQuery.childFolders("folder-123"),
        )
    }

    @Test
    fun escapesDriveQueryLiteralCharacters() {
        assertEquals(
            "'folder\\'\\\\id' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false",
            DriveQuery.childFolders("folder'\\id"),
        )
    }

    @Test
    fun rejectsBlankParentId() {
        assertThrows(IllegalArgumentException::class.java) {
            DriveQuery.childFolders("   ")
        }
    }
}
