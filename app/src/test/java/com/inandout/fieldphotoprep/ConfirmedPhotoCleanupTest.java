package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class ConfirmedPhotoCleanupTest {
    private static final String PHOTO_ID = "11111111-1111-4111-8111-111111111111";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void confirmedCleanupRemovesBothImagesButRetainsUploadedMetadataAndRemoteIdentity()
            throws Exception {
        Fixture fixture = uploadedFixture();
        File original = fixture.store.imageFile(fixture.uploaded);
        File prepared = fixture.preparer.preparedFile(PHOTO_ID);
        assertTrue(original.isFile());
        assertTrue(prepared.isFile());

        ConfirmedPhotoCleanup.Result result = fixture.cleanup.cleanup(PHOTO_ID);

        assertTrue(result.complete());
        assertTrue(result.protectedOriginalRemoved());
        assertTrue(result.preparedCopyRemoved());
        assertFalse(original.exists());
        assertFalse(prepared.exists());

        PendingPhotoRecord retained = fixture.store.getById(PHOTO_ID);
        assertEquals(PendingPhotoRecord.State.UPLOADED, retained.state());
        assertEquals("remote-photo-id", retained.remoteFileId());
        assertEquals("work-provider-id", retained.workOrderId());
        assertEquals(1, retained.uploadAttemptCount());
    }

    @Test
    public void cleanupRefusesNonUploadedRecordAndLeavesFilesUntouched() throws Exception {
        File pendingRoot = temporaryFolder.newFolder("pending-waiting");
        File preparedRoot = temporaryFolder.newFolder("prepared-waiting");
        PendingPhotoStore store = new PendingPhotoStore(
                pendingRoot,
                () -> PHOTO_ID,
                () -> 1_700_000_000_000L);
        PhotoPreparer preparer = new PhotoPreparer(preparedRoot);
        PendingPhotoRecord capturing = store.beginCapture(
                new DriveFolder("address-provider-id", "Address"),
                new DriveFolder("work-provider-id", "Cut Grass - 2026-09-21"));
        write(store.imageFile(capturing), "protected-original");
        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(PHOTO_ID);
        File prepared = preparer.preparedFile(PHOTO_ID);
        write(prepared, "prepared-copy");

        ConfirmedPhotoCleanup cleanup = new ConfirmedPhotoCleanup(store, preparer);
        try {
            cleanup.cleanup(PHOTO_ID);
            fail("Expected cleanup to reject WAITING state");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("only after durable confirmed"));
        }

        assertTrue(store.imageFile(waiting).isFile());
        assertTrue(prepared.isFile());
        assertEquals(PendingPhotoRecord.State.WAITING, store.getById(PHOTO_ID).state());
    }

    @Test
    public void localDeletionFailureKeepsUploadedTruthAndRetryCanFinishCleanup() throws Exception {
        Fixture fixture = uploadedFixture();
        File original = fixture.store.imageFile(fixture.uploaded);
        File prepared = fixture.preparer.preparedFile(PHOTO_ID);

        assertTrue(original.delete());
        assertTrue(original.mkdir());
        File blocker = new File(original, "keep.txt");
        write(blocker, "block directory deletion");

        ConfirmedPhotoCleanup.Result first = fixture.cleanup.cleanup(PHOTO_ID);

        assertFalse(first.complete());
        assertFalse(first.protectedOriginalRemoved());
        assertTrue(first.preparedCopyRemoved());
        assertTrue(original.isDirectory());
        assertFalse(prepared.exists());

        PendingPhotoRecord afterFailure = fixture.store.getById(PHOTO_ID);
        assertEquals(PendingPhotoRecord.State.UPLOADED, afterFailure.state());
        assertEquals("remote-photo-id", afterFailure.remoteFileId());

        assertTrue(blocker.delete());
        assertTrue(original.delete());

        ConfirmedPhotoCleanup.Result second = fixture.cleanup.cleanup(PHOTO_ID);
        assertTrue(second.complete());
        PendingPhotoRecord afterRetry = fixture.store.getById(PHOTO_ID);
        assertEquals(PendingPhotoRecord.State.UPLOADED, afterRetry.state());
        assertEquals("remote-photo-id", afterRetry.remoteFileId());
    }

    private Fixture uploadedFixture() throws Exception {
        File pendingRoot = temporaryFolder.newFolder("pending-uploaded");
        File preparedRoot = temporaryFolder.newFolder("prepared-uploaded");
        PendingPhotoStore store = new PendingPhotoStore(
                pendingRoot,
                () -> PHOTO_ID,
                new SequenceTimes(1_700_000_000_000L, 1_700_000_100_000L));
        PhotoPreparer preparer = new PhotoPreparer(preparedRoot);
        PendingPhotoRecord capturing = store.beginCapture(
                new DriveFolder("address-provider-id", "Address"),
                new DriveFolder("work-provider-id", "Cut Grass - 2026-09-21"));
        write(store.imageFile(capturing), "protected-original");
        store.finishCaptureIfImageExists(PHOTO_ID);
        write(preparer.preparedFile(PHOTO_ID), "prepared-copy");
        store.beginUploadAttempt(PHOTO_ID);
        PendingPhotoRecord uploaded = store.markUploadConfirmed(PHOTO_ID, "remote-photo-id");
        return new Fixture(store, preparer, uploaded, new ConfirmedPhotoCleanup(store, preparer));
    }

    private static void write(File file, String text) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
    }

    private static final class Fixture {
        final PendingPhotoStore store;
        final PhotoPreparer preparer;
        final PendingPhotoRecord uploaded;
        final ConfirmedPhotoCleanup cleanup;

        Fixture(
                PendingPhotoStore store,
                PhotoPreparer preparer,
                PendingPhotoRecord uploaded,
                ConfirmedPhotoCleanup cleanup) {
            this.store = store;
            this.preparer = preparer;
            this.uploaded = uploaded;
            this.cleanup = cleanup;
        }
    }

    private static final class SequenceTimes implements PendingPhotoStore.TimeSource {
        private final long[] times;
        private int index;

        SequenceTimes(long... times) {
            this.times = times;
        }

        @Override
        public long nowEpochMs() {
            return times[index++];
        }
    }
}
