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
    static final int VERIFY_MAX_ATTEMPTS = 8;
    static final long VERIFY_RETRY_DELAY_MS = 500L;

    interface Sleeper {
        void sleep(long millis) throws IOException;
    }

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

    public static final class CreatedUpload {
        private final String photoId;
        private final String destinationId;
        private final String remoteFileId;
        private final String remoteDisplayName;
        private final long expectedBytes;

        CreatedUpload(
                String photoId,
                String destinationId,
                String remoteFileId,
                String remoteDisplayName,
                long expectedBytes) {
            this.photoId = requireText(photoId, "created upload photo id");
            this.destinationId = requireText(destinationId, "created upload destination id");
            this.remoteFileId = requireText(remoteFileId, "created remote file id");
            this.remoteDisplayName = requireText(remoteDisplayName, "created remote display name");
            if (expectedBytes <= 0) {
                throw new IllegalArgumentException("Expected upload byte count must be positive.");
            }
            this.expectedBytes = expectedBytes;
        }

        String photoId() {
            return photoId;
        }

        String destinationId() {
            return destinationId;
        }

        public String remoteFileId() {
            return remoteFileId;
        }

        public String remoteDisplayName() {
            return remoteDisplayName;
        }

        public long expectedBytes() {
            return expectedBytes;
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
    private final Sleeper sleeper;

    public DrivePhotoUploader(ContentResolver resolver, Uri treeUri) {
        this(new AndroidProviderOps(resolver, treeUri), DrivePhotoUploader::sleepNormally);
    }

    DrivePhotoUploader(ProviderOps provider) {
        this(provider, millis -> { });
    }

    DrivePhotoUploader(ProviderOps provider, Sleeper sleeper) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
    }

    /**
     * Validates the immutable queued destination and performs only the remote create operation.
     * No prepared JPEG bytes are written by this method.
     */
    public CreatedUpload create(PendingPhotoRecord uploadingRecord, File preparedFile)
            throws UploadException {
        validateUploadingRecordBeforeCreate(uploadingRecord);
        validatePreparedBeforeCreate(preparedFile);

        final long expectedBytes = preparedFile.length();
        final String destinationId = uploadingRecord.workOrderId();
        final String remoteName = remoteFileNameFor(uploadingRecord.id());

        final RemoteDocument destination;
        try {
            destination = provider.readDocument(destinationId);
        } catch (IOException | RuntimeException error) {
            throw safeFailure(
                    "The exact stored work-order destination could not be read. No Drive file was created.",
                    error);
        }

        if (!destinationId.equals(destination.id())) {
            throw safeFailure(
                    "Drive returned a different destination identity. No Drive file was created.",
                    null);
        }
        if (!DocumentsContract.Document.MIME_TYPE_DIR.equals(destination.mimeType())) {
            throw safeFailure(
                    "The exact stored work-order destination is no longer a folder. No Drive file was created.",
                    null);
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
        if (!JPEG_MIME_TYPE.equals(created.mimeType()) || !remoteName.equals(created.displayName())) {
            throw uncertainFailure(
                    "Drive returned unexpected created-photo metadata. Remote state must be reconciled before retry.",
                    null);
        }

        return new CreatedUpload(
                uploadingRecord.id(),
                destinationId,
                created.id(),
                remoteName,
                expectedBytes);
    }

    /**
     * Writes and verifies a photo only after the queue record proves the created identity was
     * durably persisted as provisional duplicate-protection evidence.
     */
    public UploadResult writeAndVerify(
            PendingPhotoRecord uploadingRecord,
            CreatedUpload createdUpload,
            File preparedFile) throws UploadException {
        if (uploadingRecord == null || uploadingRecord.state() != PendingPhotoRecord.State.UPLOADING) {
            throw uncertainFailure(
                    "The created Drive photo cannot be written without a persisted UPLOADING record. Remote state must be reconciled before retry.",
                    null);
        }
        if (createdUpload == null) {
            throw uncertainFailure(
                    "The created Drive photo identity is unavailable. Remote state must be reconciled before retry.",
                    null);
        }
        if (!createdUpload.photoId().equals(uploadingRecord.id())
                || !createdUpload.destinationId().equals(uploadingRecord.workOrderId())) {
            throw uncertainFailure(
                    "The created Drive photo token does not belong to this photo and destination. Remote state must be reconciled before retry.",
                    null);
        }
        if (!createdUpload.remoteFileId().equals(uploadingRecord.provisionalRemoteFileId())) {
            throw uncertainFailure(
                    "The created Drive photo identity was not durably bound to this upload before writing. Remote state must be reconciled before retry.",
                    null);
        }
        if (!createdUpload.remoteDisplayName().equals(remoteFileNameFor(uploadingRecord.id()))) {
            throw uncertainFailure(
                    "The created Drive photo name does not match this photo identity. Remote state must be reconciled before retry.",
                    null);
        }
        if (preparedFile == null || !preparedFile.isFile() || preparedFile.length() <= 0) {
            throw uncertainFailure(
                    "The prepared JPEG became unavailable after Drive creation. Remote state must be reconciled before retry.",
                    null);
        }
        if (preparedFile.length() != createdUpload.expectedBytes()) {
            throw uncertainFailure(
                    "The prepared JPEG changed after Drive creation. Remote state must be reconciled before retry.",
                    null);
        }

        final long bytesWritten;
        try {
            bytesWritten = provider.writeDocument(createdUpload.remoteFileId(), preparedFile);
        } catch (IOException | RuntimeException error) {
            throw uncertainFailure(
                    "Drive photo writing was interrupted after creation began. Remote state must be reconciled before retry.",
                    error);
        }
        if (bytesWritten != createdUpload.expectedBytes()) {
            throw uncertainFailure(
                    "Drive did not accept the complete prepared photo byte count. Remote state must be reconciled before retry.",
                    null);
        }

        return verifyAfterProviderSettle(createdUpload, bytesWritten);
    }

    private UploadResult verifyAfterProviderSettle(
            CreatedUpload createdUpload,
            long bytesWritten) throws UploadException {
        IOException lastReadError = null;
        RemoteDocument lastObserved = null;

        for (int attempt = 0; attempt < VERIFY_MAX_ATTEMPTS; attempt++) {
            try {
                RemoteDocument verified = provider.readDocument(createdUpload.remoteFileId());
                lastObserved = verified;
                lastReadError = null;

                if (verified != null
                        && (!createdUpload.remoteFileId().equals(verified.id())
                        || !JPEG_MIME_TYPE.equals(verified.mimeType())
                        || !createdUpload.remoteDisplayName().equals(verified.displayName()))) {
                    throw uncertainFailure(
                            "The created Drive photo identity, name, or MIME type did not verify exactly. Remote state must be reconciled before retry.",
                            null);
                }

                if (verified != null
                        && verified.sizeBytes() != 0L
                        && (verified.sizeBytes() < 0L
                        || verified.sizeBytes() == createdUpload.expectedBytes())) {
                    return new UploadResult(
                            verified.id(),
                            verified.displayName(),
                            bytesWritten);
                }
            } catch (UploadException hardMismatch) {
                throw hardMismatch;
            } catch (IOException error) {
                lastReadError = error;
            } catch (RuntimeException error) {
                lastReadError = new IOException("Unexpected provider readback failure.", error);
            }

            if (attempt + 1 < VERIFY_MAX_ATTEMPTS) {
                try {
                    sleeper.sleep(VERIFY_RETRY_DELAY_MS);
                } catch (IOException waitError) {
                    throw uncertainFailure(
                            "Drive verification wait was interrupted after the photo was written. Remote state must be reconciled before retry.",
                            waitError);
                }
            }
        }

        if (lastReadError != null) {
            throw uncertainFailure(
                    "The created Drive photo could not be verified after the provider settle window. Remote state must be reconciled before retry.",
                    lastReadError);
        }

        if (lastObserved == null) {
            throw uncertainFailure(
                    "Drive did not return the created photo during the provider settle window. Remote state must be reconciled before retry.",
                    null);
        }

        throw uncertainFailure(
                "The created Drive photo byte size did not settle to the prepared photo size before verification timed out. Remote state must be reconciled before retry.",
                null);
    }

    static String remoteFileNameFor(String photoId) {
        return "field-photo-" + PendingPhotoRecord.imageFileNameFor(photoId)
                .substring("photo-".length());
    }

    static long writePreparedToDescriptor(ParcelFileDescriptor descriptor, File source)
            throws IOException {
        if (descriptor == null) {
            throw new IOException("Document provider did not open the created photo for writing.");
        }
        if (source == null || !source.isFile() || source.length() <= 0) {
            descriptor.close();
            throw new IOException("Prepared photo source is missing or empty.");
        }

        long written = 0L;
        byte[] buffer = new byte[64 * 1024];
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

    private static void validateUploadingRecordBeforeCreate(PendingPhotoRecord uploadingRecord)
            throws UploadException {
        if (uploadingRecord == null) {
            throw safeFailure("A persisted uploading photo record is required.", null);
        }
        if (uploadingRecord.state() != PendingPhotoRecord.State.UPLOADING) {
            throw safeFailure(
                    "The photo must be durably UPLOADING before Drive creation begins.",
                    null);
        }
        if (uploadingRecord.provisionalRemoteFileId() != null) {
            throw uncertainFailure(
                    "This upload already has provisional remote identity and must not create another Drive photo.",
                    null);
        }
    }

    private static void validatePreparedBeforeCreate(File preparedFile) throws UploadException {
        if (preparedFile == null || !preparedFile.isFile() || preparedFile.length() <= 0) {
            throw safeFailure(
                    "The prepared JPEG is missing or empty. No Drive file was created.",
                    null);
        }
    }

    private static UploadException safeFailure(String message, Throwable cause) {
        return new UploadException(message, cause, false);
    }

    private static UploadException uncertainFailure(String message, Throwable cause) {
        return new UploadException(message, cause, true);
    }

    private static void sleepNormally(long millis) throws IOException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Drive verification settle wait was interrupted.", interrupted);
        }
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
                throw new IOException(
                        "Persisted Drive access is unavailable for the requested document.",
                        error);
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
                throw new IOException(
                        "Document provider created a photo without returning its identity.");
            }
            return new RemoteDocument(createdId, displayName, JPEG_MIME_TYPE, -1L);
        }

        @Override
        public long writeDocument(String documentId, File source) throws IOException {
            ensureTreeUri();
            Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
            final ParcelFileDescriptor descriptor;
            try {
                descriptor = resolver.openFileDescriptor(documentUri, "w");
            } catch (SecurityException error) {
                throw new IOException(
                        "Drive access was denied while opening the photo for writing.",
                        error);
            }
            return writePreparedToDescriptor(descriptor, source);
        }

        private void ensureTreeUri() throws IOException {
            if (treeUri == null) {
                throw new IOException("No persisted master Drive folder is available.");
            }
        }
    }
}
