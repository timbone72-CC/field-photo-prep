package com.inandout.fieldphotoprep;

import android.provider.DocumentsContract;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class PhotoUploadCoordinatorTest {
    private static final String ID1 = "11111111-1111-4111-8111-111111111111";
    private static final String ID2 = "22222222-2222-4222-8222-222222222222";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void confirmedDriveResultBecomesUploadedAndPreservesLocalFiles() throws Exception {
        Fixture fixture = fixture(ID1);
        File original = fixture.store.imageFile(fixture.waiting);
        File prepared = fixture.preparer.preparedFile(ID1);
        long originalBytes = original.length();
        long preparedBytes = prepared.length();

        PendingPhotoRecord uploaded = fixture.coordinator.upload(ID1);

        assertEquals(PendingPhotoRecord.State.UPLOADED, uploaded.state());
        assertEquals("remote-photo-id", uploaded.remoteFileId());
        assertEquals("work-provider-id", uploaded.workOrderId());
        assertEquals(1, uploaded.uploadAttemptCount());
        assertTrue(original.isFile());
        assertEquals(originalBytes, original.length());
        assertTrue(prepared.isFile());
        assertEquals(preparedBytes, prepared.length());
    }

    @Test
    public void safePreCreateFailureBecomesFailedThenRetryKeepsDestinationAndIncrementsAttempt()
            throws Exception {
        Fixture fixture = fixture(ID1);
        fixture.provider.failDestinationRead = true;

        try {
            fixture.coordinator.upload(ID1);
            fail("Expected first upload to fail safely");
        } catch (Exception expected) {
            // State assertions below are the contract.
        }

        PendingPhotoRecord failed = fixture.store.getById(ID1);
        assertEquals(PendingPhotoRecord.State.FAILED, failed.state());
        assertEquals("work-provider-id", failed.workOrderId());
        assertEquals(1, failed.uploadAttemptCount());
        assertTrue(fixture.store.hasImageData(failed));
        assertTrue(fixture.preparer.preparedFile(ID1).isFile());

        fixture.provider.failDestinationRead = false;
        PendingPhotoRecord uploaded = fixture.coordinator.upload(ID1);

        assertEquals(PendingPhotoRecord.State.UPLOADED, uploaded.state());
        assertEquals(2, uploaded.uploadAttemptCount());
        assertEquals("work-provider-id", fixture.provider.createParentId);
    }

    @Test
    public void writeFailureBecomesUncertainAndCannotBlindlyRetry() throws Exception {
        Fixture fixture = fixture(ID1);
        fixture.provider.failWrite = true;

        try {
            fixture.coordinator.upload(ID1);
            fail("Expected ambiguous upload");
        } catch (Exception expected) {
            // State assertions below are the contract.
        }

        PendingPhotoRecord uncertain = fixture.store.getById(ID1);
        assertEquals(PendingPhotoRecord.State.UNCERTAIN, uncertain.state());
        assertEquals("work-provider-id", uncertain.workOrderId());
        assertTrue(fixture.store.hasImageData(uncertain));
        assertTrue(fixture.preparer.preparedFile(ID1).isFile());

        fixture.provider.failWrite = false;
        try {
            fixture.coordinator.upload(ID1);
            fail("UNCERTAIN must not retry automatically");
        } catch (Exception expected) {
            assertTrue(expected.getMessage().contains("cannot begin an upload"));
        }
        assertEquals(1, fixture.store.getById(ID1).uploadAttemptCount());
    }

    @Test
    public void missingPreparedCopyDoesNotChangeQueueStateOrAttemptCount() throws Exception {
        Fixture fixture = fixture(ID1);
        assertTrue(fixture.preparer.preparedFile(ID1).delete());

        try {
            fixture.coordinator.upload(ID1);
            fail("Expected missing prepared copy failure");
        } catch (Exception expected) {
            assertTrue(expected.getMessage().contains("Prepare this photo"));
        }

        PendingPhotoRecord stillWaiting = fixture.store.getById(ID1);
        assertEquals(PendingPhotoRecord.State.WAITING, stillWaiting.state());
        assertEquals(0, stillWaiting.uploadAttemptCount());
        assertFalse(fixture.provider.createCalled);
    }

    @Test
    public void onePhotoFailureDoesNotMutateAnotherQueuedPhoto() throws Exception {
        File pendingRoot = temporaryFolder.newFolder("pending-many");
        File preparedRoot = temporaryFolder.newFolder("prepared-many");
        SequenceIds ids = new SequenceIds(ID1, ID2);
        PendingPhotoStore store = new PendingPhotoStore(
                pendingRoot,
                ids,
                new SequenceTimes(1_700_000_000_000L, 1_700_000_000_100L,
                        1_700_000_100_000L, 1_700_000_100_100L));
        PhotoPreparer preparer = new PhotoPreparer(preparedRoot);

        PendingPhotoRecord first = createWaiting(store, "work-provider-id", ID1);
        PendingPhotoRecord second = createWaiting(store, "other-work-id", ID2);
        write(preparer.preparedFile(ID1), "prepared-one");
        write(preparer.preparedFile(ID2), "prepared-two");

        FakeProvider provider = new FakeProvider();
        provider.failDestinationRead = true;
        PhotoUploadCoordinator coordinator = new PhotoUploadCoordinator(
                store,
                preparer,
                new DrivePhotoUploader(provider));

        try {
            coordinator.upload(first.id());
            fail("Expected first photo failure");
        } catch (Exception expected) {
            // Expected.
        }

        PendingPhotoRecord firstAfter = store.getById(ID1);
        PendingPhotoRecord secondAfter = store.getById(ID2);
        assertEquals(PendingPhotoRecord.State.FAILED, firstAfter.state());
        assertEquals(PendingPhotoRecord.State.WAITING, secondAfter.state());
        assertEquals("other-work-id", secondAfter.workOrderId());
        assertEquals(0, secondAfter.uploadAttemptCount());
        assertTrue(store.hasImageData(secondAfter));
    }

    private Fixture fixture(String id) throws Exception {
        File pendingRoot = temporaryFolder.newFolder("pending-" + id.substring(0, 4));
        File preparedRoot = temporaryFolder.newFolder("prepared-" + id.substring(0, 4));
        PendingPhotoStore store = new PendingPhotoStore(
                pendingRoot,
                () -> id,
                new SequenceTimes(1_700_000_000_000L, 1_700_000_100_000L,
                        1_700_000_200_000L));
        PhotoPreparer preparer = new PhotoPreparer(preparedRoot);
        PendingPhotoRecord waiting = createWaiting(store, "work-provider-id", id);
        write(preparer.preparedFile(id), "prepared-jpeg");
        FakeProvider provider = new FakeProvider();
        PhotoUploadCoordinator coordinator = new PhotoUploadCoordinator(
                store,
                preparer,
                new DrivePhotoUploader(provider));
        return new Fixture(store, preparer, waiting, provider, coordinator);
    }

    private static PendingPhotoRecord createWaiting(
            PendingPhotoStore store,
            String workOrderId,
            String expectedId) throws Exception {
        PendingPhotoRecord capturing = store.beginCapture(
                new DriveFolder("address-provider-id", "Address"),
                new DriveFolder(workOrderId, "Work - 2026-09-09"));
        assertEquals(expectedId, capturing.id());
        write(store.imageFile(capturing), "protected-original");
        return store.finishCaptureIfImageExists(expectedId);
    }

    private static void write(File file, String content) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
    }

    private static final class Fixture {
        final PendingPhotoStore store;
        final PhotoPreparer preparer;
        final PendingPhotoRecord waiting;
        final FakeProvider provider;
        final PhotoUploadCoordinator coordinator;

        Fixture(
                PendingPhotoStore store,
                PhotoPreparer preparer,
                PendingPhotoRecord waiting,
                FakeProvider provider,
                PhotoUploadCoordinator coordinator) {
            this.store = store;
            this.preparer = preparer;
            this.waiting = waiting;
            this.provider = provider;
            this.coordinator = coordinator;
        }
    }

    private static final class FakeProvider implements DrivePhotoUploader.ProviderOps {
        boolean failDestinationRead;
        boolean failWrite;
        boolean createCalled;
        String createParentId;
        String createDisplayName;
        long bytesWritten;
        boolean created;

        @Override
        public DrivePhotoUploader.RemoteDocument readDocument(String documentId)
                throws java.io.IOException {
            if (documentId.endsWith("work-id") || "work-provider-id".equals(documentId)) {
                if (failDestinationRead) {
                    throw new java.io.IOException("destination unavailable");
                }
                return new DrivePhotoUploader.RemoteDocument(
                        documentId,
                        "Renamed work folder",
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        -1L);
            }
            if ("remote-photo-id".equals(documentId) && created) {
                return new DrivePhotoUploader.RemoteDocument(
                        documentId,
                        createDisplayName,
                        DrivePhotoUploader.JPEG_MIME_TYPE,
                        bytesWritten);
            }
            throw new java.io.IOException("missing document");
        }

        @Override
        public DrivePhotoUploader.RemoteDocument createJpeg(
                String parentDocumentId,
                String displayName) {
            createCalled = true;
            created = true;
            createParentId = parentDocumentId;
            createDisplayName = displayName;
            return new DrivePhotoUploader.RemoteDocument(
                    "remote-photo-id",
                    displayName,
                    DrivePhotoUploader.JPEG_MIME_TYPE,
                    -1L);
        }

        @Override
        public long writeDocument(String documentId, File source) throws java.io.IOException {
            if (failWrite) {
                throw new java.io.IOException("write interrupted");
            }
            bytesWritten = source.length();
            return bytesWritten;
        }
    }

    private static final class SequenceIds implements PendingPhotoStore.IdSource {
        private final String[] ids;
        private int index;

        SequenceIds(String... ids) {
            this.ids = ids;
        }

        @Override
        public String nextId() {
            return ids[index++];
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
            return times[Math.min(index++, times.length - 1)];
        }
    }
}
