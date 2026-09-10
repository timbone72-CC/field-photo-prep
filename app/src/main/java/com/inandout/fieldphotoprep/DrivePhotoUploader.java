package com.inandout.fieldphotoprep;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

public final class DrivePhotoUploader {
    static final String JPEG_MIME_TYPE = "image/jpeg";

    interface ProviderOps {
        RemoteDocument readDocument(String documentId) throws IOException;

        RemoteDocument createJpeg(String parentDocumentId, String displayName) throws IOException;

        long writeDocument(String documentId, File source) throws IOException;
    }

    static final class RemoteDocument {
        private final String id;
        private final String displayName;
        private final String mimeType;
        private final long sizeBytes;

        RemoteDocument(String id, String displayName, String mimeType, long sizeBytes) {
            this.id = requireText(id, "remote document id");
            this.displayName = requireText(displayName, "remote display name");
            this.mimeType = requireText(mimeType, "remote MIME type");
            if (sizeBytes < -1L) {
                throw new IllegalArgumentException("Remote size must be -1 for unknown or non-negative.");
            }
            this.sizeBytes = sizeBytes;
        }

        String id() {
            return id;
        }

        String displayName() {
            return displayName;
        }

        String mimeType() {
            return mimeType;
        }

        long sizeBytes() {
            return sizeBytes;
        }
    }

    public static final class UploadResult {
        private final String remoteFileId;
        private final String remoteDisplayName;
        private final long bytesWritten;

        UploadResult(String remoteFileId, String remoteDisplayName, long bytesWritten) {
            this.remoteFileId = requireText(remoteFileId, "remote file id");
            this.remoteDisplayName = requireText(remoteDisplayName, "remote display name");
            if (bytesWritten <= 0) {
                throw new IllegalArgumentException("Uploaded byte count must be positive.");
            }
            this.bytesWritten = bytesWritten;
        }

        public String remoteFileId() {
            return remoteFileId;
        }

        public String remoteDisplayName() {
            return remoteDisplayName;
        }

        public long bytesWritten() {
            return bytesWritten;
        }
    }

    public static final class UploadException extends IOException {
        private final boolean remoteStateUncertain;

        UploadException(String message, Throwable cause, boolean remoteStateUncertain) {
            super(message, cause);
            this.remoteStateUncertain = remoteStateUncertain;
        }

        public boolean remoteStateUncertain() {
            return remoteStateUncertain;
        }
    }

    private final ProviderOps provider;

    public DrivePhotoUploader(ContentResolver resolver, Uri treeUri) {
        this(new AndroidProviderOps(resolver, treeUri));
    }

    DrivePhotoUploader(ProviderOps provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public UploadResult upload(PendingPhotoRecord uploadingRecord, File preparedFile)
            throws UploadException {
        if (uploadingRecord == null) {
            throw safeFailure("A persisted uploading photo record is required.", null);
        }
        if (uploadingRecord.state() != PendingPhotoRecord.State.UPLOADING) {
            throw safeFailure("The photo must be durably UPLOADING before Drive creation begins.", null);
        }
        if (preparedFile == null || !preparedFile.isFile() || preparedFile.length() <= 0) {
            throw safeFailure("The prepared JPEG is missing or empty. No Drive file was created.", null);
        }

        final long expectedBytes = preparedFile.length();
        final String destinationId = uploadingRecord.workOrderId();
        final String remoteName = remoteFileNameFor(uploadingRecord.id());

        final RemoteDocument destination;
        try {
            destination = provider.readDocument(destinationId);
        } catch (IOException | RuntimeException error) {
            throw safeFailure("The exact stored work-order destination could not be read. No Drive file was created.", error);
        }

        if (!destinationId.equals(destination.id())) {
            throw safeFailure("Drive returned a different destination identity. No Drive file was created.", null);
        }
        if (!DocumentsContract.Document.MIME_TYPE_DIR.equals(destination.mimeType())) {
            throw safeFailure("The exact stored work-order destination is no longer a folder. No Drive file was created.", null);
        }

        final RemoteDocument created;
        try {
            created = provider.createJpeg(destinationId, remoteName);
        } catch (IOException | RuntimeException error) {
            throw uncertainFailure(
                    "Drive photo creation did not return a safely classifiable result. Remote state must be reconciled before retry.",
                    error);
        }

        if (created == null) {
            throw uncertainFailure(
                    "Drive did not return the created photo identity. Remote state must be reconciled before retry.",
                    null);
        }

        final long bytesWritten;
        try {
            bytesWritten = provider.writeDocument(created.id(), preparedFile);
        } catch (IOException | RuntimeException error) {
            throw uncertainFailure(
                    "Drive photo writing was interrupted after creation began. Remote state must be reconciled before retry.",
                    error);
        }
        if (bytesWritten != expectedBytes) {
            throw uncertainFailure(
                    "Drive did not accept the complete prepared photo byte count. Remote state must be reconciled before retry.",
                    null);
        }

        final RemoteDocument verified;
        try {
            verified = provider.readDocument(created.id());
        } catch (IOException | RuntimeException error) {
            throw uncertainFailure(
                    "The created Drive photo could not be verified. Remote state must be reconciled before retry.",
                    error);
        }

        if (verified == null
                || !created.id().equals(verified.id())
                || !JPEG_MIME_TYPE.equals(verified.mimeType())
                || !remoteName.equals(verified.displayName())
                || verified.sizeBytes() == 0L
                || (verified.sizeBytes() > 0L && verified.sizeBytes() != expectedBytes)) {
            throw uncertainFailure(
                    "The created Drive photo metadata did not verify exactly. Remote state must be reconciled before retry.",
                    null);
        }

        return new UploadResult(verified.id(), verified.displayName(), bytesWritten);
    }

    static String remoteFileNameFor(String photoId) {
        return "field-photo-" + PendingPhotoRecord.imageFileNameFor(photoId)
                .substring("photo-".length());
    }

    private static UploadException safeFailure(String message, Throwable cause) {
        return new UploadException(message, cause, false);
    }

    private static UploadException uncertainFailure(String message, Throwable cause) {
        return new UploadException(message, cause, true);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }

    private static final class AndroidProviderOps implements ProviderOps {
        private static final String[] PROJECTION = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
        };

        private final ContentResolver resolver;
        private final Uri treeUri;

        AndroidProviderOps(ContentResolver resolver, Uri treeUri) {
            this.resolver = Objects.requireNonNull(resolver, "resolver");
            this.treeUri = treeUri;
        }

        @Override
        public RemoteDocument readDocument(String documentId) throws IOException {
            ensureTreeUri();
            Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
            try (Cursor cursor = resolver.query(documentUri, PROJECTION, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst()) {
                    throw new IOException("Document provider did not return the requested document.");
                }
                int idColumn = cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                int nameColumn = cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                int mimeColumn = cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_MIME_TYPE);
                int sizeColumn = cursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_SIZE);
                long size = sizeColumn < 0 || cursor.isNull(sizeColumn)
                        ? -1L
                        : cursor.getLong(sizeColumn);
                return new RemoteDocument(
                        cursor.getString(idColumn),
                        cursor.getString(nameColumn),
                        cursor.getString(mimeColumn),
                        size);
            } catch (SecurityException error) {
                throw new IOException("Persisted Drive access is unavailable for the requested document.", error);
            }
        }

        @Override
        public RemoteDocument createJpeg(String parentDocumentId, String displayName)
                throws IOException {
            ensureTreeUri();
            Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocumentId);
            final Uri createdUri;
            try {
                createdUri = DocumentsContract.createDocument(
                        resolver,
                        parentUri,
                        JPEG_MIME_TYPE,
                        displayName);
            } catch (SecurityException error) {
                throw new IOException("Drive access was denied while creating the photo.", error);
            }
            if (createdUri == null) {
                throw new IOException("Document provider returned no created photo URI.");
            }
            String createdId = DocumentsContract.getDocumentId(createdUri);
            if (createdId == null || createdId.isBlank()) {
                throw new IOException("Document provider created a photo without returning its identity.");
            }
            return new RemoteDocument(createdId, displayName, JPEG_MIME_TYPE, -1L);
        }

        @Override
        public long writeDocument(String documentId, File source) throws IOException {
            ensureTreeUri();
            Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
            long written = 0L;
            byte[] buffer = new byte[64 * 1024];

            ParcelFileDescriptor descriptor;
            try {
                descriptor = resolver.openFileDescriptor(documentUri, "w");
            } catch (SecurityException error) {
                throw new IOException("Drive access was denied while opening the photo for writing.", error);
            }
            if (descriptor == null) {
                throw new IOException("Document provider did not open the created photo for writing.");
            }

            try (FileInputStream input = new FileInputStream(source);
                 ParcelFileDescriptor.AutoCloseOutputStream output =
                         new ParcelFileDescriptor.AutoCloseOutputStream(descriptor)) {
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                    written += count;
                }
                output.flush();
            }
            return written;
        }

        private void ensureTreeUri() throws IOException {
            if (treeUri == null) {
                throw new IOException("No persisted master Drive folder is available.");
            }
        }
    }
}
