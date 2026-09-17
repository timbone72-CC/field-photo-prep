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
    public void createUsesExactStoredWorkOrderAndDoesNotWriteBytes() throws Exception {
        File prepared = prepared("prepared-jpeg-bytes");
        FakeProvider provider = new FakeProvider();
        DrivePhotoUploader uploader = new DrivePhotoUploader(provider);

        DrivePhotoUploader.CreatedUpload created = uploader.create(uploadingRecord(), prepared);

        assertEquals("work-provider-id", provider.createParentId);
        assertEquals("012_field-photo-" + PHOTO_ID + ".jpg", provider.createDisplayName);
        assertEquals("remote-photo-id", created.remoteFileId());
        assertEquals(prepared.length(), created.expectedBytes());
        assertFalse(provider.writeCalled);
    }

    @Test
    public void writeAndVerifyRequiresPersistedMatchingProvisionalIdentity() throws Exception {
        File prepared = prepared("prepared-jpeg-bytes");
        FakeProvider provider = new FakeProvider();
        DrivePhotoUploader uploader = new DrivePhotoUploader(provider);
        PendingPhotoRecord uploading = uploadingRecord();
        DrivePhotoUploader.CreatedUpload created = uploader.create(uploading, prepared);

        try {
            uploader.writeAndVerify(uploading, created, prepared);
            fail("Expected write barrier failure");
        } catch (DrivePhotoUploader.UploadException error) {
            assertTrue(error.remoteStateUncertain());
        }
        assertFalse(provider.writeCalled);

        PendingPhotoRecord withWrongProvisional =
                uploading.recordProvisionalRemoteFileId("different-remote-id");
        try {
            uploader.writeAndVerify(withWrongProvisional, created, prepared);
            fail("Expected mismatched provisional identity failure");
        } catch (DrivePhotoUploader.UploadException error) {
            assertTrue(error.remoteStateUncertain());
        }
        assertFalse(provider.writeCalled);

        PendingPhotoRecord persisted =
                uploading.recordProvisionalRemoteFileId(created.remoteFileId());
        DrivePhotoUploader.UploadResult result =
                uploader.writeAndVerify(persisted, created, prepared);

        assertTrue(provider.writeCalled);
        assertEquals(prepared.length(), provider.lastWrittenBytes);
        assertEquals("remote-photo-id", result.remoteFileId());
        assertEquals(prepared.length(), result.bytesWritten());
    }

    @Test
    public void unreadableDestinationFailsBeforeRemoteCreateAndIsRetryable() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.failDestinationRead = true;

        try {
            new DrivePhotoUploader(provider).create(uploadingRecord(), prepared("bytes"));
            fail("Expected upload failure");
        } catch (DrivePhotoUploader.UploadException error) {
            assertFalse(error.remoteStateUncertain());
        }
        assertFalse(provider.createCalled);
        assertFalse(provider.writeCalled);
    }

    @Test
    public void nonFolderDestinationFailsBeforeRemoteCreate() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.destinationMimeType = "image/jpeg";

        try {
            new DrivePhotoUploader(provider).create(uploadingRecord(), prepared("bytes"));
            fail("Expected upload failure");
        } catch (DrivePhotoUploader.UploadException error) {
            assertFalse(error.remoteStateUncertain());
        }
        assertFalse(provider.createCalled);
        assertFalse(provider.writeCalled);
    }

    @Test
    public void createFailureIsUncertainBecauseRemoteSideEffectMayExist() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.failCreate = true;

        try {
            new DrivePhotoUploader(provider).create(uploadingRecord(), prepared("prepared-photo"));
            fail("Expected uncertain upload result");
        } catch (DrivePhotoUploader.UploadException error) {
            assertTrue(error.remoteStateUncertain());
        }
        assertTrue(provider.createCalled);
        assertFalse(provider.writeCalled);
    }

    @Test
    public void writeFailureAfterPersistedProvisionalIdentityIsUncertain() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.failWrite = true;

        assertWriteStageUncertain(provider);
        assertTrue(provider.createCalled);
        assertTrue(provider.writeCalled);
    }

    @Test
    public void verificationFailureAfterWriteIsUncertain() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.failVerificationRead = true;

        assertWriteStageUncertain(provider);
        assertTrue(provider.createCalled);
        assertTrue(provider.writeCalled);
    }

    @Test
    public void incompleteByteCountIsUncertain() throws Exception {
        FakeProvider provider = new FakeProvider();
        provider.shortWrite = true;

        assertWriteStageUncertain(provider);
        assertTrue(provider.writeCalled);
    }

    @Test
    public void changedPreparedFileAfterCreateStopsBeforeWriter() throws Exception {
        FakeProvider provider = new FakeProvider();
        DrivePhotoUploader uploader = new DrivePhotoUploader(provider);
        File prepared = prepared("first-size");
        PendingPhotoRecord uploading = uploadingRecord();
        DrivePhotoUploader.CreatedUpload created = uploader.create(uploading, prepared);
        PendingPhotoRecord persisted =
                uploading.recordProvisionalRemoteFileId(created.remoteFileId());
        Files.write(prepared.toPath(), "different-size-content".getBytes(StandardCharsets.UTF_8));

        try {
            uploader.writeAndVerify(persisted, created, prepared);
            fail("Expected uncertain changed-source result");
        } catch (DrivePhotoUploader.UploadException error) {
            assertTrue(error.remoteStateUncertain());
        }
        assertFalse(provider.writeCalled);
    }

    private void assertWriteStageUncertain(FakeProvider provider) throws Exception {
        DrivePhotoUploader uploader = new DrivePhotoUploader(provider);
        File prepared = prepared("prepared-photo");
        PendingPhotoRecord uploading = uploadingRecord();
        DrivePhotoUploader.CreatedUpload created = uploader.create(uploading, prepared);
        PendingPhotoRecord persisted =
                uploading.recordProvisionalRemoteFileId(created.remoteFileId());
        try {
            uploader.writeAndVerify(persisted, created, prepared);
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
                        "Cut Grass - 2026-09-09",
                        12)
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
