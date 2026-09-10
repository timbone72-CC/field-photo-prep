package com.inandout.fieldphotoprep;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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

    private static final int FRESH_FOLDER_MAX_ATTEMPTS = 6;
    private static final long FRESH_FOLDER_RETRY_DELAY_MS = 500L;

    private static final class FolderQueryResult {
        private final List<DriveFolder> folders;
        private final boolean loading;

        FolderQueryResult(List<DriveFolder> folders, boolean loading) {
            this.folders = folders;
            this.loading = loading;
        }
    }

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
        return queryFolders(resolver, childrenUri).folders;
    }

    public List<DriveFolder> listFoldersFresh(
            ContentResolver resolver,
            Uri treeUri,
            String parentDocumentId) throws IOException {
        Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocumentId);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId);

        boolean refreshAccepted = requestFreshProviderState(resolver, parentUri, childrenUri);
        if (!refreshAccepted) {
            throw new IOException(
                    "Drive could not confirm a fresh folder listing. Nothing was created; try again after Drive finishes syncing.");
        }

        List<DriveFolder> previousSettled = null;
        for (int attempt = 0; attempt < FRESH_FOLDER_MAX_ATTEMPTS; attempt++) {
            FolderQueryResult result = queryFolders(resolver, childrenUri);
            if (!result.loading) {
                if (previousSettled != null && sameFolders(previousSettled, result.folders)) {
                    return result.folders;
                }
                previousSettled = result.folders;
            } else {
                previousSettled = null;
            }

            if (attempt + 1 < FRESH_FOLDER_MAX_ATTEMPTS) {
                sleepForFreshnessRetry();
            }
        }

        throw new IOException(
                "Drive did not return two matching settled folder checks. Nothing was created; wait for Drive to sync and try again.");
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

    static boolean sameFolders(List<DriveFolder> first, List<DriveFolder> second) {
        if (first.size() != second.size()) {
            return false;
        }
        List<DriveFolder> firstSorted = new ArrayList<>(first);
        List<DriveFolder> secondSorted = new ArrayList<>(second);
        Collections.sort(firstSorted);
        Collections.sort(secondSorted);
        for (int i = 0; i < firstSorted.size(); i++) {
            DriveFolder a = firstSorted.get(i);
            DriveFolder b = secondSorted.get(i);
            if (!a.id().equals(b.id()) || !a.name().equals(b.name())) {
                return false;
            }
        }
        return true;
    }

    static boolean isFolderMimeType(String mimeType) {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
    }

    static boolean isAuthoritativeFolderState(boolean refreshAccepted, boolean loading) {
        return refreshAccepted && !loading;
    }

    private FolderQueryResult queryFolders(ContentResolver resolver, Uri childrenUri) throws IOException {
        List<DriveFolder> folders = new ArrayList<>();
        boolean loading;
        try (Cursor cursor = resolver.query(childrenUri, PROJECTION, null, null, null)) {
            if (cursor == null) {
                throw new IOException("The selected folder did not return a folder list.");
            }
            loading = isCursorLoading(cursor);
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
        return new FolderQueryResult(folders, loading);
    }

    private boolean requestFreshProviderState(
            ContentResolver resolver,
            Uri parentUri,
            Uri childrenUri) {
        boolean accepted = false;
        try {
            accepted = resolver.refresh(parentUri, Bundle.EMPTY, null);
        } catch (RuntimeException ignored) {
            // Fall through to the child-list URI refresh request.
        }
        try {
            accepted = resolver.refresh(childrenUri, Bundle.EMPTY, null) || accepted;
        } catch (RuntimeException ignored) {
            // A provider that cannot refresh is not authoritative enough for a create decision.
        }
        return accepted;
    }

    private static boolean isCursorLoading(Cursor cursor) {
        Bundle extras = cursor.getExtras();
        return extras != null && extras.getBoolean(DocumentsContract.EXTRA_LOADING, false);
    }

    private static void sleepForFreshnessRetry() throws IOException {
        try {
            Thread.sleep(FRESH_FOLDER_RETRY_DELAY_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Folder verification was interrupted.", interrupted);
        }
    }
}
