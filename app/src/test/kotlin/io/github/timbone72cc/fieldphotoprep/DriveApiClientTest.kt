package io.github.timbone72cc.fieldphotoprep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriveApiClientTest {
    private val client = DriveApiClient()

    @Test
    fun parsesFolderMetadataAndNextPageToken() {
        val page = client.parseFolderPage(
            """
            {
              "nextPageToken": "next-1",
              "files": [
                {"id":"id-b","name":"Bravo","mimeType":"application/vnd.google-apps.folder"},
                {"id":"id-a","name":"Alpha","mimeType":"application/vnd.google-apps.folder"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals("next-1", page.nextPageToken)
        assertEquals(
            listOf(
                DriveFolder("id-b", "Bravo"),
                DriveFolder("id-a", "Alpha"),
            ),
            page.folders,
        )
    }

    @Test
    fun missingPageTokenAndInvalidRowsAreSafe() {
        val page = client.parseFolderPage(
            """
            {
              "files": [
                {"id":"good-id","name":"Good"},
                {"id":"missing-name"},
                {"name":"missing-id"}
              ]
            }
            """.trimIndent(),
        )

        assertNull(page.nextPageToken)
        assertEquals(listOf(DriveFolder("good-id", "Good")), page.folders)
    }
}
