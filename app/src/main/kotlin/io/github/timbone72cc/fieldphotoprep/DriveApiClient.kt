package io.github.timbone72cc.fieldphotoprep

import com.google.gson.JsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class DriveApiException(
    val statusCode: Int,
    message: String,
) : IOException(message)

class DriveApiClient {
    fun listFolders(
        accessToken: String,
        parentId: String,
    ): List<DriveFolder> {
        require(accessToken.isNotBlank()) { "Access token must not be blank." }
        val allFolders = mutableListOf<DriveFolder>()
        var pageToken: String? = null

        do {
            val page = listFolderPage(accessToken, parentId, pageToken)
            allFolders += page.folders
            pageToken = page.nextPageToken
        } while (!pageToken.isNullOrBlank())

        return allFolders.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    private fun listFolderPage(
        accessToken: String,
        parentId: String,
        pageToken: String?,
    ): FolderPage {
        val query = DriveQuery.childFolders(parentId)
        val parameters = linkedMapOf(
            "q" to query,
            "spaces" to "drive",
            "fields" to "nextPageToken,files(id,name,mimeType)",
            "pageSize" to "1000",
            "orderBy" to "name",
            "supportsAllDrives" to "true",
            "includeItemsFromAllDrives" to "true",
        )
        if (!pageToken.isNullOrBlank()) {
            parameters["pageToken"] = pageToken
        }

        val encodedQuery = parameters.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val url = URI.create("$DRIVE_FILES_URL?$encodedQuery").toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }

        try {
            val status = connection.responseCode
            val body = if (status in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            if (status !in 200..299) {
                throw DriveApiException(
                    statusCode = status,
                    message = "Google Drive returned HTTP $status${body.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()}",
                )
            }
            return parseFolderPage(body)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseFolderPage(json: String): FolderPage {
        val root = JsonParser.parseString(json).asJsonObject
        val folders = root.getAsJsonArray("files")
            ?.mapNotNull { item ->
                val objectValue = item.asJsonObject
                val id = objectValue.get("id")?.asString?.takeIf { it.isNotBlank() }
                val name = objectValue.get("name")?.asString?.takeIf { it.isNotBlank() }
                if (id == null || name == null) null else DriveFolder(id = id, name = name)
            }
            .orEmpty()
        val nextPageToken = root.get("nextPageToken")
            ?.takeUnless { it.isJsonNull }
            ?.asString
            ?.takeIf { it.isNotBlank() }
        return FolderPage(folders = folders, nextPageToken = nextPageToken)
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    internal data class FolderPage(
        val folders: List<DriveFolder>,
        val nextPageToken: String?,
    )

    companion object {
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    }
}
