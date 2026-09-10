package com.inandout.fieldphotoprep;

import android.provider.DocumentsContract;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class DrivePhotoUploaderTest {
    private static final String PHOTO_ID = "11111111-1111-4111-8111-111111111111";
    private static final String OTHER_PHOTO_ID = "22222222-2222-4222-8222-222222222222";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void remoteFilenameIsDeterministicAndUniqueByPhotoIdentity() {
        assertEquals(
                "field-photo-11111111-1111-4111-8111-111111111111.jpg",
                DrivePhotoUploader.remoteFileNameFor(PHOTO_ID));
        assertEquals(
                DrivePhotoUploader.remoteFileNameFor(PHOTO_ID),
                DrivePhotoUploader.remoteFileNameFor(PHOTO_ID));
        assertFalse(DrivePhotoUploader.remoteFileNameFor(PHOTO_ID)
                .equals(DrivePhotoUploader.remoteFileNameFor(OTHER_PHOTO_ID)));
    }

    @Test
    public void successCreatesUnderExactStoredWorkOrderAndReturnsVerifiedIdentity() throws Exception {
        File prepared = prepared("prepared-jpeg-bytes");
        FakeProvider provider = new FakeProvider();
        DrivePhotoUploader uploader = new DrivePhotoUploader(provider);

        DrivePhotoUploader.UploadResult result = uploader.upload(uploadingRecord(), prepared);

        assertEquals("work-provider-id", provider.createParentId);
        assertEquals(DrivePhotoUploader.remoteFileNameFor(PHOTO_ID), provider.createDisplayName);
        assertEquals(prepared.length(), provider.lastWrittenBytes);
        assertEquals("remote-photo-id", result.remoteFileId());
        assertEquals(prepared.length(), result.bytesWritten());
    }

    @Test
    public void unreadableDestinationFailsBeforeRemoteCreateAndIsRetryable() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.failDestinationRead = true;

        try {
            new DrivePhotoUploader(provider).upload(uploadingRecord(), prepared("bytes"));
            fail("Expected upload failure");
        } catch (DrivePhotoUploader.UploadException error) {
            assertFalse(error.remoteStateUncertain());
        }
        assertFalse(provider.createCalled);
    }

    @Test
    public void nonFolderDestinationFailsBeforeRemoteCreate() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.destinationMimeType = "image/jpeg";

        try {
            new DrivePhotoUploader(provider).upload(uploadingRecord(), prepared("bytes"));
            fail("Expected upload failure");
        } catch (DrivePhotoUploader.UploadException error) {
            assertFalse(error.remoteStateUncertain());
        }
        assertFalse(provider.createCalled);
    }

    @Test
    public void createFailureIsUncertainBecauseRemoteSideEffectMayExist() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.failCreate = true;

        assertUncertain(provider);
        assertTrue(provider.createCalled);
    }

    @Test
    public void writeFailureAfterCreateIsUncertain() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.failWrite = true;

        assertUncertain(provider);
        assertTrue(provider.createCalled);
    }

    @Test
    public void verificationFailureAfterWriteIsUncertain() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.failVerificationRead = true;

        assertUncertain(provider);
        assertTrue(provider.createCalled);
        assertTrue(provider.writeCalled);
    }

    @Test
    public void incompleteByteCountIsUncertain() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.shortWrite = true;

        assertUncertain(provider);
        assertTrue(provider.writeCalled);
    }

    private void assertUncertain(FakeProvider provider) throws Exception {
        try {
            new DrivePhotoUploader(provider).upload(uploadingRecord(), prepared("prepared-photo"));
            fail("Expected uncertain upload result");
        } catch (DrivePhotoUploader.UploadException error) {
            assertTrue(error.remoteStateUncertain());
        }
    }

    private PendingPhotoRecord uploadingRecord() {
        return PendingPhotoRecord.createCapturing(
                        PHOTO_ID,
                        1_700_000_000_000L,
                        "address-provider-id",
                        "Address",
                        "work-provider-id",
                        "Cut Grass - 2026-09-09")
                .withState(PendingPhotoRecord.State.WAITING)
                .beginUploadAttempt(1_700_000_100_000L);
    }

    private File prepared(String content) throws Exception {
        File file = temporaryFolder.newFile();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static final class FakeProvider implements DrivePhotoUploader.ProviderOps {
        String destinationMimeType = DocumentsContract.Document.MIME_TYPE_DIR;
        boolean failDestinationRead;
        boolean failCreate;
        boolean failWrite;
        boolean failVerificationRead;
        boolean shortWrite;
        boolean createCalled;
        boolean writeCalled;
        String createParentId;
        String createDisplayName;
        long lastWrittenBytes;
        private boolean created;

        @Override
        public DrivePhotoUploader.RemoteDocument readDocument(String documentId)
                throws java.io.IOException {
            if ("work-provider-id".equals(documentId)) {
                if (failDestinationRead) {
                    throw new java.io.IOException("destination unavailable");
                }
                return new DrivePhotoUploader.RemoteDocument(
                        documentId,
                        "Renamed Work Order",
                        destinationMimeType,
                        -1L);
            }
            if ("remote-photo-id".equals(documentId) && created) {
                if (failVerificationRead) {
                    throw new java.io.IOException("verification unavailable");
                }
                return new DrivePhotoUploader.RemoteDocument(
                        documentId,
                        createDisplayName,
                        DrivePhotoUploader.JPEG_MIME_TYPE,
                        lastWrittenBytes);
            }
            throw new java.io.IOException("missing document");
        }

        @Override
        public DrivePhotoUploader.RemoteDocument createJpeg(
                String parentDocumentId,
                String displayName) throws java.io.IOException {
            createCalled = true;
            createParentId = parentDocumentId;
            createDisplayName = displayName;
            if (failCreate) {
                throw new java.io.IOException("create interrupted");
            }
            created = true;
            return new DrivePhotoUploader.RemoteDocument(
                    "remote-photo-id",
                    displayName,
                    DrivePhotoUploader.JPEG_MIME_TYPE,
                    -1L);
        }

        @Override
        public long writeDocument(String documentId, File source) throws java.io.IOException {
            writeCalled = true;
            if (failWrite) {
                throw new java.io.IOException("write interrupted");
            }
            long actual = source.length();
            lastWrittenBytes = shortWrite ? Math.max(0L, actual - 1L) : actual;
            return lastWrittenBytes;
        }
    }
}
