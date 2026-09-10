package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class DrivePhotoUploaderStageBindingTest {
    private static final String PHOTO_ID = "11111111-1111-4111-8111-111111111111";
    private static final String OTHER_PHOTO_ID = "22222222-2222-4222-8222-222222222222";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createdUploadTokenCannotBeReusedForAnotherPhotoOrDestination() throws Exception {
        File prepared = temporaryFolder.newFile("prepared.jpg");
        Files.write(prepared.toPath(), "prepared-jpeg".getBytes(StandardCharsets.UTF_8));

        DrivePhotoUploader.CreatedUpload created = new DrivePhotoUploader.CreatedUpload(
                PHOTO_ID,
                "work-provider-id",
                "remote-photo-id",
                DrivePhotoUploader.remoteFileNameFor(PHOTO_ID),
                prepared.length());

        PendingPhotoRecord other = PendingPhotoRecord.createCapturing(
                        OTHER_PHOTO_ID,
                        1_700_000_000_000L,
                        "address-provider-id",
                        "Address",
                        "other-work-provider-id",
                        "Other Work - 2026-09-10")
                .withState(PendingPhotoRecord.State.WAITING)
                .beginUploadAttempt(1_700_000_100_000L)
                .recordProvisionalRemoteFileId("remote-photo-id");

        NoWriteProvider provider = new NoWriteProvider();
        try {
            new DrivePhotoUploader(provider).writeAndVerify(other, created, prepared);
            fail("Expected staged upload binding failure");
        } catch (DrivePhotoUploader.UploadException expected) {
            assertTrue(expected.remoteStateUncertain());
        }
        assertFalse(provider.writeCalled);
    }

    private static final class NoWriteProvider implements DrivePhotoUploader.ProviderOps {
        boolean writeCalled;

        @Override
        public DrivePhotoUploader.RemoteDocument readDocument(String documentId) {
            throw new AssertionError("Provider read must not run after staged-token binding failure.");
        }

        @Override
        public DrivePhotoUploader.RemoteDocument createJpeg(String parentDocumentId, String displayName) {
            throw new AssertionError("Provider create must not run during write-stage binding validation.");
        }

        @Override
        public long writeDocument(String documentId, File source) {
            writeCalled = true;
            throw new AssertionError("Writer must not run for a staged token from another photo/destination.");
        }
    }
}
