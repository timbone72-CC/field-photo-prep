package com.inandout.fieldphotoprep;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DriveClient {
    private static final String[] PROJECTION = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
    };

    public DriveFolder getTreeFolder(ContentResolver resolver, Uri treeUri) throws IOException {
        String documentId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
        try (Cursor cursor = resolver.query(documentUri, PROJECTION, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                throw new IOException("The selected folder could not be read.");
            }
            String name = cursor.getString(cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME));
            String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE));
            if (!isFolderMimeType(mimeType)) {
                throw new IOException("The selected item is not a folder.");
            }
            return new DriveFolder(documentId, name);
        }
    }

    public List<DriveFolder> listFolders(ContentResolver resolver, Uri treeUri) throws IOException {
        String parentDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId);
        List<DriveFolder> folders = new ArrayList<>();

        try (Cursor cursor = resolver.query(childrenUri, PROJECTION, null, null, null)) {
            if (cursor == null) {
                throw new IOException("The selected folder did not return a folder list.");
            }
            int idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
            while (cursor.moveToNext()) {
                if (isFolderMimeType(cursor.getString(mimeColumn))) {
                    folders.add(new DriveFolder(cursor.getString(idColumn), cursor.getString(nameColumn)));
                }
            }
        }

        Collections.sort(folders);
        return folders;
    }

    static boolean isFolderMimeType(String mimeType) {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
    }
}
