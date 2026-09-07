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

    private static final String[] CHILD_ID_PROJECTION = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
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
        return listFolders(resolver, treeUri, DocumentsContract.getTreeDocumentId(treeUri));
    }

    public List<DriveFolder> listFolders(
            ContentResolver resolver,
            Uri treeUri,
            String parentDocumentId) throws IOException {
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

    public boolean isFolderEmpty(
            ContentResolver resolver,
            Uri treeUri,
            String folderDocumentId) throws IOException {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderDocumentId);
        try (Cursor cursor = resolver.query(childrenUri, CHILD_ID_PROJECTION, null, null, null)) {
            if (cursor == null) {
                throw new IOException("Drive did not return the selected folder contents.");
            }
            return !cursor.moveToFirst();
        }
    }

    public DriveFolder createFolder(
            ContentResolver resolver,
            Uri treeUri,
            String parentDocumentId,
            String displayName) throws IOException {
        if (displayName == null || displayName.isBlank()) {
            throw new IOException("Folder name is required.");
        }
        Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocumentId);
        Uri createdUri = DocumentsContract.createDocument(
                resolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                displayName);
        if (createdUri == null) {
            throw new IOException("Drive did not confirm folder creation.");
        }
        String createdDocumentId = DocumentsContract.getDocumentId(createdUri);
        if (createdDocumentId == null || createdDocumentId.isBlank()) {
            throw new IOException("Drive created a folder without returning its identity.");
        }
        return new DriveFolder(createdDocumentId, displayName);
    }

    public DriveFolder renameFolder(
            ContentResolver resolver,
            Uri treeUri,
            String folderDocumentId,
            String newDisplayName) throws IOException {
        if (newDisplayName == null || newDisplayName.isBlank()) {
            throw new IOException("Folder name is required.");
        }
        Uri folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderDocumentId);
        Uri renamedUri = DocumentsContract.renameDocument(resolver, folderUri, newDisplayName);
        if (renamedUri == null) {
            throw new IOException("Drive did not confirm folder rename.");
        }
        String returnedDocumentId = DocumentsContract.getDocumentId(renamedUri);
        if (returnedDocumentId == null || returnedDocumentId.isBlank()) {
            throw new IOException("Drive renamed the folder without returning its identity.");
        }
        return new DriveFolder(returnedDocumentId, newDisplayName);
    }

    static List<DriveFolder> findExactNameMatches(List<DriveFolder> folders, String requestedName) {
        List<DriveFolder> matches = new ArrayList<>();
        for (DriveFolder folder : folders) {
            if (folder.name().equals(requestedName)) {
                matches.add(folder);
            }
        }
        Collections.sort(matches);
        return matches;
    }

    static DriveFolder findById(List<DriveFolder> folders, String documentId) {
        for (DriveFolder folder : folders) {
            if (folder.id().equals(documentId)) {
                return folder;
            }
        }
        return null;
    }

    static boolean isFolderMimeType(String mimeType) {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
    }
}
