package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;

public final class ProtectedWorkGuardTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void emptyStoreDoesNotBlockSignOut() throws Exception {
        Fixture fixture = fixture();

        ProtectedWorkGuard.Result result = fixture.guard.inspect();

        assertFalse(result.blocksSignOut());
        assertEquals(0, result.blockingCount());
    }

    @Test
    public void nonemptyCapturingReservationBlocksSignOut() throws Exception {
        Fixture fixture = fixture();
        PendingPhotoRecord record = fixture.store.beginCapture(address(), workOrder());
        writeBytes(fixture.store.imageFile(record));

        ProtectedWorkGuard.Result result = fixture.guard.inspect();

        assertTrue(result.blocksSignOut());
        assertEquals(1, result.capturingCount());
    }

    @Test
    public void emptyCapturingReservationDoesNotByItselfBlockSignOut() throws Exception {
        Fixture fixture = fixture();
        fixture.store.beginCapture(address(), workOrder());

        ProtectedWorkGuard.Result result = fixture.guard.inspect();

        assertFalse(result.blocksSignOut());
        assertEquals(0, result.capturingCount());
    }

    @Test
    public void waitingPhotoBlocksSignOut() throws Exception {
        Fixture fixture = fixture();
        PendingPhotoRecord waiting = createWaiting(fixture);

        ProtectedWorkGuard.Result result = fixture.guard.inspect();

        assertTrue(result.blocksSignOut());
        assertEquals(PendingPhotoRecord.State.WAITING, waiting.state());
        assertEquals(1, result.queuedCount());
    }

    @Test
    public void failedPhotoBlocksSignOut() throws Exception {
        Fixture fixture = fixture();
        PendingPhotoRecord waiting = createWaiting(fixture);
        fixture.store.beginUploadAttempt(waiting.id());
        fixture.store.markUploadFailed(waiting.id(), "safe failure");

        ProtectedWorkGuard.Result result = fixture.guard.inspect();

        assertTrue(result.blocksSignOut());
        assertEquals(1, result.queuedCount());
    }

    @Test
    public void uncertainPhotoBlocksSignOut() throws Exception {
        Fixture fixture = fixture();
        PendingPhotoRecord waiting = createWaiting(fixture);
        fixture.store.beginUploadAttempt(waiting.id());
        fixture.store.markUploadUncertain(waiting.id(), "remote state unknown");

        ProtectedWorkGuard.Result result = fixture.guard.inspect();

        assertTrue(result.blocksSignOut());
        assertEquals(1, result.queuedCount());
    }

    @Test
    public void uploadedHistoryWithoutLocalCopiesDoesNotBlock() throws Exception {
        Fixture fixture = fixture();
        PendingPhotoRecord uploaded = createUploaded(fixture);
        fixture.store.removeProtectedImageAfterConfirmedUpload(uploaded.id());

        ProtectedWorkGuard.Result result = fixture.guard.inspect();

        assertFalse(result.blocksSignOut());
        assertEquals(0, result.cleanupPendingCount());
    }

    @Test
    public void uploadedPhotoWithProtectedOriginalStillPresentBlocksCleanup() throws Exception {
        Fixture fixture = fixture();
        createUploaded(fixture);

        ProtectedWorkGuard.Result result = fixture.guard.inspect();

        assertTrue(result.blocksSignOut());
        assertEquals(1, result.cleanupPendingCount());
    }

    @Test
    public void uploadedPhotoWithPreparedCopyStillPresentBlocksCleanup() throws Exception {
        Fixture fixture = fixture();
        PendingPhotoRecord uploaded = createUploaded(fixture);
        fixture.store.removeProtectedImageAfterConfirmedUpload(uploaded.id());
        writeBytes(fixture.preparer.preparedFile(uploaded.id()));

        ProtectedWorkGuard.Result result = fixture.guard.inspect();

        assertTrue(result.blocksSignOut());
        assertEquals(1, result.cleanupPendingCount());
    }

    private Fixture fixture() throws Exception {
        File root = temporaryFolder.newFolder();
        PendingPhotoStore store = new PendingPhotoStore(new File(root, "pending"));
        PhotoPreparer preparer = new PhotoPreparer(new File(root, "prepared"));
        return new Fixture(store, preparer, new ProtectedWorkGuard(store, preparer));
    }

    private PendingPhotoRecord createWaiting(Fixture fixture) throws Exception {
        PendingPhotoRecord capturing = fixture.store.beginCapture(address(), workOrder());
        writeBytes(fixture.store.imageFile(capturing));
        return fixture.store.finishCaptureIfImageExists(capturing.id());
    }

    private PendingPhotoRecord createUploaded(Fixture fixture) throws Exception {
        PendingPhotoRecord waiting = createWaiting(fixture);
        PendingPhotoRecord uploading = fixture.store.beginUploadAttempt(waiting.id());
        fixture.store.recordProvisionalRemoteFileId(uploading.id(), "remote-1");
        return fixture.store.markUploadConfirmed(uploading.id(), "remote-1");
    }

    private static void writeBytes(File file) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create test directory.");
        }
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(new byte[] {1, 2, 3});
            output.getFD().sync();
        }
    }

    private static DriveFolder address() {
        return new DriveFolder("address-1", "100 Test Address");
    }

    private static DriveFolder workOrder() {
        return new DriveFolder("work-order-1", "Inspection - 2026-09-25");
    }

    private static final class Fixture {
        private final PendingPhotoStore store;
        private final PhotoPreparer preparer;
        private final ProtectedWorkGuard guard;

        private Fixture(
                PendingPhotoStore store,
                PhotoPreparer preparer,
                ProtectedWorkGuard guard) {
            this.store = store;
            this.preparer = preparer;
            this.guard = guard;
        }
    }
}
