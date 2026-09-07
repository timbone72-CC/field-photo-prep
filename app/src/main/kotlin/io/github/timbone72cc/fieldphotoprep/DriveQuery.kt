package io.github.timbone72cc.fieldphotoprep

object DriveQuery {
    const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"

    fun childFolders(parentId: String): String {
        require(parentId.isNotBlank()) { "Parent folder ID must not be blank." }
        val escapedParentId = parentId
            .replace("\\", "\\\\")
            .replace("'", "\\'")
        return "'$escapedParentId' in parents and mimeType = '$FOLDER_MIME_TYPE' and trashed = false"
    }
}
