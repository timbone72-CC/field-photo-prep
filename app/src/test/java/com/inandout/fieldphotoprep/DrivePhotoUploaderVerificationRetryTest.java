package com.inandout.fieldphotoprep;

import android.provider.DocumentsContract;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class DrivePhotoUploaderVerificationRetryTest {
    private static final String PHOTO_ID = "11111111-1111-4111-8111-111111111111";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void transientVerificationReadFailureSettlesWithoutUncertain() throws Exception {
        File prepared = prepared("prepared-photo");
        SettlingProvider provider = new SettlingProvider(prepared.length());
        provider.verificationReadFailuresRemaining = 2;
        DrivePhotoUploader uploader = new DrivePhotoUploader(
                provider,
                millis -> { },
                TestAuthorization.allowedGuard());

        DrivePhotoUploader.UploadResult result = upload(uploader, prepared);

        assertEquals("remote-photo-id", result.remoteFileId());
        assertEquals(3, provider.remoteReadCount);
    }

    @Test
    public void transientStaleSizeSettlesWithoutUncertain() throws Exception {
        File prepared = prepared("prepared-photo");
        SettlingProvider provider = new SettlingProvider(prepared.length());
        provider.staleSizesRemaining = 2;
        DrivePhotoUploader uploader = new DrivePhotoUploader(
                provider,
                millis -> { },
                TestAuthorization.allowedGuard());

        DrivePhotoUploader.UploadResult result = upload(uploader, prepared);

        assertEquals(prepared.length(), result.bytesWritten());
        assertEquals(3, provider.remoteReadCount);
    }

    @Test
    public void persistentVerificationReadFailureStillRemainsUncertain() throws Exception {
        File prepared = prepared("prepared-photo");
        SettlingProvider provider = new SettlingProvider(prepared.length());
        provider.verificationReadFailuresRemaining = DrivePhotoUploader.VERIFY_MAX_ATTEMPTS + 2;
        DrivePhotoUploader uploader = new DrivePhotoUploader(
                provider,
                millis -> { },
                TestAuthorization.allowedGuard());

        try {
            upload(uploader, prepared);
            fail("Expected unresolved verification to remain uncertain");
        } catch (DrivePhotoUploader.UploadException error) {
            assertTrue(error.remoteStateUncertain());
            assertTrue(error.getMessage().contains("provider settle window"));
        }

        assertEquals(DrivePhotoUploader.VERIFY_MAX_ATTEMPTS, provider.remoteReadCount);
    }

    @Test
    public void hardIdentityMismatchDoesNotWaitForMoreAttempts() throws Exception {
        File prepared = prepared("prepared-photo");
        SettlingProvider provider = new SettlingProvider(prepared.length());
        provider.returnWrongMimeType = true;
        DrivePhotoUploader uploader = new DrivePhotoUploader(
                provider,
                millis -> { },
                TestAuthorization.allowedGuard());

        try {
            upload(uploader, prepared);
            fail("Expected hard remote metadata mismatch");
        } catch (DrivePhotoUploader.UploadException error) {
            assertTrue(error.remoteStateUncertain());
        }

        assertEquals(1, provider.remoteReadCount);
    }

    private DrivePhotoUploader.UploadResult upload(
            DrivePhotoUploader uploader,
            File prepared) throws Exception {
        PendingPhotoRecord uploading = uploadingRecord();
        DrivePhotoUploader.CreatedUpload created = uploader.create(uploading, prepared);
        PendingPhotoRecord persisted =
                uploading.recordProvisionalRemoteFileId(created.remoteFileId());
        return uploader.writeAndVerify(persisted, created, prepared);
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

    private static final class SettlingProvider implements DrivePhotoUploader.ProviderOps {
        final long expectedBytes;
        int verificationReadFailuresRemaining;
        int staleSizesRemaining;
        int remoteReadCount;
        boolean returnWrongMimeType;
        boolean created;
        long bytesWritten;

        SettlingProvider(long expectedBytes) {
            this.expectedBytes = expectedBytes;
        }

        @Override
        public DrivePhotoUploader.RemoteDocument readDocument(String documentId) throws IOException {
            if ("work-provider-id".equals(documentId)) {
                return new DrivePhotoUploader.RemoteDocument(
                        documentId,
                        "Work Order",
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        -1L);
            }
            if (!"remote-photo-id".equals(documentId) || !created) {
                throw new IOException("missing document");
            }

            remoteReadCount++;
            if (verificationReadFailuresRemaining > 0) {
                verificationReadFailuresRemaining--;
                throw new IOException("provider still publishing remote file");
            }

            long size = staleSizesRemaining > 0 ? 0L : expectedBytes;
            if (staleSizesRemaining > 0) {
                staleSizesRemaining--;
            }
            return new DrivePhotoUploader.RemoteDocument(
                    documentId,
                    DrivePhotoUploader.remoteFileNameFor(PHOTO_ID),
                    returnWrongMimeType ? "application/octet-stream" : DrivePhotoUploader.JPEG_MIME_TYPE,
                    size);
        }

        @Override
        public DrivePhotoUploader.RemoteDocument createJpeg(
                String parentDocumentId,
                String displayName) {
            created = true;
            return new DrivePhotoUploader.RemoteDocument(
                    "remote-photo-id",
                    displayName,
                    DrivePhotoUploader.JPEG_MIME_TYPE,
                    -1L);
        }

        @Override
        public long writeDocument(String documentId, File source) {
            bytesWritten = source.length();
            return bytesWritten;
        }
    }
}
