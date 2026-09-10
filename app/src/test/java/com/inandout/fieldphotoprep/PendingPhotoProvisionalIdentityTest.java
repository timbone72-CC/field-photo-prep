package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class PendingPhotoProvisionalIdentityTest {
    private static final String ID1 = "11111111-1111-4111-8111-111111111111";
    private static final String ID2 = "22222222-2222-4222-8222-222222222222";
    private static final long CAPTURE_TIME = 1_700_000_000_000L;
    private static final long ATTEMPT_TIME = 1_700_000_100_000L;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void schema2UploadingRecordLoadsWithNullProvisionalAndNextWriteUsesSchema3()
            throws Exception {
        File root = temporaryFolder.newFolder("schema2");
        Properties schema2 = schema2Properties(
                ID1,
                PendingPhotoRecord.State.UPLOADING,
                1,
                ATTEMPT_TIME,
                null,
                null);
        writeProperties(new File(root, PendingPhotoRecord.metadataFileNameFor(ID1)), schema2);
        writeImageBytes(new File(root, PendingPhotoRecord.imageFileNameFor(ID1)), "protected-image");

        PendingPhotoStore store = new PendingPhotoStore(root);
        PendingPhotoRecord loaded = store.getById(ID1);

        assertEquals(PendingPhotoRecord.State.UPLOADING, loaded.state());
        assertNull(loaded.provisionalRemoteFileId());
        assertNull(loaded.remoteFileId());

        PendingPhotoRecord updated = store.recordProvisionalRemoteFileId(ID1, "provider-created-1");
        Properties persisted = loadProperties(new File(root, updated.metadataFileName()));

        assertEquals("3", persisted.getProperty("schemaVersion"));
        assertEquals("provider-created-1", persisted.getProperty("provisionalRemoteFileId"));
        assertEquals("", persisted.getProperty("remoteFileId"));
        assertEquals("work-1", updated.workOrderId());
    }

    @Test
    public void provisionalIdentityPersistsAndSurvivesRestartRecovery() throws Exception {
        File root = temporaryFolder.newFolder("restart");
        PendingPhotoStore store = waitingStore(root, ID1);
        store.beginUploadAttempt(ID1);
        store.recordProvisionalRemoteFileId(ID1, "provider-created-1");

        PendingPhotoStore restarted = new PendingPhotoStore(root);
        PendingPhotoRecord beforeRecovery = restarted.getById(ID1);
        assertEquals(PendingPhotoRecord.State.UPLOADING, beforeRecovery.state());
        assertEquals("provider-created-1", beforeRecovery.provisionalRemoteFileId());

        restarted.reconcileInterruptedUploads();
        PendingPhotoRecord recovered = restarted.getById(ID1);

        assertEquals(PendingPhotoRecord.State.UNCERTAIN, recovered.state());
        assertEquals("provider-created-1", recovered.provisionalRemoteFileId());
        assertNull(recovered.remoteFileId());
        assertEquals("work-1", recovered.workOrderId());
        assertFalse(recovered.canBeginUploadAttempt());
        assertTrue(restarted.hasImageData(recovered));
    }

    @Test
    public void provisionalIdentityCannotBecomeRetryableFailure() throws Exception {
        File root = temporaryFolder.newFolder("no-failed-after-create");
        PendingPhotoStore store = waitingStore(root, ID1);
        store.beginUploadAttempt(ID1);
        store.recordProvisionalRemoteFileId(ID1, "provider-created-1");

        expectIOException(() -> store.markUploadFailed(ID1, "Do not make this retryable"));

        PendingPhotoRecord unchanged = store.getById(ID1);
        assertEquals(PendingPhotoRecord.State.UPLOADING, unchanged.state());
        assertEquals("provider-created-1", unchanged.provisionalRemoteFileId());
        assertFalse(unchanged.canBeginUploadAttempt());
    }

    @Test
    public void provisionalIdentityIsImmutableWithinOneUploadAttempt() throws Exception {
        File root = temporaryFolder.newFolder("immutable");
        PendingPhotoStore store = waitingStore(root, ID1);
        store.beginUploadAttempt(ID1);

        PendingPhotoRecord first = store.recordProvisionalRemoteFileId(ID1, "provider-created-1");
        PendingPhotoRecord same = store.recordProvisionalRemoteFileId(ID1, "provider-created-1");
        expectIOException(() -> store.recordProvisionalRemoteFileId(ID1, "provider-created-2"));

        assertEquals("provider-created-1", first.provisionalRemoteFileId());
        assertEquals("provider-created-1", same.provisionalRemoteFileId());
        assertEquals("provider-created-1", store.getById(ID1).provisionalRemoteFileId());
    }

    @Test
    public void confirmedSuccessPromotesMatchingProvisionalIdentity() throws Exception {
        File root = temporaryFolder.newFolder("promote");
        PendingPhotoStore store = waitingStore(root, ID1);
        store.beginUploadAttempt(ID1);
        store.recordProvisionalRemoteFileId(ID1, "provider-created-1");

        expectIOException(() -> store.markUploadConfirmed(ID1, "different-provider-id"));
        PendingPhotoRecord stillUploading = store.getById(ID1);
        assertEquals(PendingPhotoRecord.State.UPLOADING, stillUploading.state());
        assertEquals("provider-created-1", stillUploading.provisionalRemoteFileId());
        assertNull(stillUploading.remoteFileId());

        PendingPhotoRecord uploaded = store.markUploadConfirmed(ID1, "provider-created-1");
        PendingPhotoRecord reloaded = new PendingPhotoStore(root).getById(ID1);

        assertEquals(PendingPhotoRecord.State.UPLOADED, uploaded.state());
        assertNull(reloaded.provisionalRemoteFileId());
        assertEquals("provider-created-1", reloaded.remoteFileId());
        assertFalse(reloaded.canBeginUploadAttempt());
    }

    @Test
    public void provisionalIdentityChangeIsIsolatedToOnePhoto() throws Exception {
        File root = temporaryFolder.newFolder("isolation");
        SequenceIds ids = new SequenceIds(ID1, ID2);
        PendingPhotoStore store = new PendingPhotoStore(root, ids, () -> ATTEMPT_TIME);
        PendingPhotoRecord first = captureWaiting(store, "address-1", "work-1");
        PendingPhotoRecord second = captureWaiting(store, "address-2", "work-2");

        store.beginUploadAttempt(first.id());
        store.recordProvisionalRemoteFileId(first.id(), "provider-created-1");

        PendingPhotoRecord firstReloaded = store.getById(first.id());
        PendingPhotoRecord secondReloaded = store.getById(second.id());

        assertEquals("provider-created-1", firstReloaded.provisionalRemoteFileId());
        assertEquals(PendingPhotoRecord.State.WAITING, secondReloaded.state());
        assertNull(secondReloaded.provisionalRemoteFileId());
        assertNull(secondReloaded.remoteFileId());
        assertEquals("work-2", secondReloaded.workOrderId());
        assertTrue(store.hasImageData(secondReloaded));
    }

    @Test
    public void schema3WaitingRecordWithProvisionalIdentityFailsClosed() throws Exception {
        Properties invalid = PendingPhotoRecord.createCapturing(
                ID1,
                CAPTURE_TIME,
                "address-1",
                "Address One",
                "work-1",
                "Work One").withState(PendingPhotoRecord.State.WAITING).toProperties();
        invalid.setProperty("provisionalRemoteFileId", "provider-created-1");

        try {
            PendingPhotoRecord.fromProperties(invalid);
            fail("Expected invalid waiting record to fail closed");
        } catch (IllegalArgumentException expected) {
            // Expected fail-closed state validation.
        }
    }

    private static PendingPhotoStore waitingStore(File root, String id) throws Exception {
        PendingPhotoStore store = new PendingPhotoStore(root, () -> id, () -> ATTEMPT_TIME);
        captureWaiting(store, "address-1", "work-1");
        return store;
    }

    private static PendingPhotoRecord captureWaiting(
            PendingPhotoStore store,
            String addressId,
            String workOrderId) throws Exception {
        PendingPhotoRecord record = store.beginCapture(
                new DriveFolder(addressId, "Address " + addressId),
                new DriveFolder(workOrderId, "Work " + workOrderId));
        writeImageBytes(store.imageFile(record), "jpeg-data-" + record.id());
        return store.finishCaptureIfImageExists(record.id());
    }

    private static Properties schema2Properties(
            String id,
            PendingPhotoRecord.State state,
            int attemptCount,
            long lastAttemptAt,
            String statusDetail,
            String remoteFileId) {
        Properties properties = new Properties();
        properties.setProperty("schemaVersion", "2");
        properties.setProperty("id", id);
        properties.setProperty("imageFile", PendingPhotoRecord.imageFileNameFor(id));
        properties.setProperty("state", state.name());
        properties.setProperty("createdAtEpochMs", Long.toString(CAPTURE_TIME));
        properties.setProperty("addressId", "address-1");
        properties.setProperty("addressName", "Address One");
        properties.setProperty("workOrderId", "work-1");
        properties.setProperty("workOrderName", "Work One");
        properties.setProperty("uploadAttemptCount", Integer.toString(attemptCount));
        properties.setProperty("lastAttemptAtEpochMs", Long.toString(lastAttemptAt));
        properties.setProperty("statusDetail", statusDetail == null ? "" : statusDetail);
        properties.setProperty("remoteFileId", remoteFileId == null ? "" : remoteFileId);
        return properties;
    }

    private static void writeProperties(File file, Properties properties) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            properties.store(output, "test");
            output.getFD().sync();
        }
    }

    private static Properties loadProperties(File file) throws Exception {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        }
        return properties;
    }

    private static void writeImageBytes(File file, String content) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
    }

    private static void expectIOException(ThrowingRunnable action) throws Exception {
        try {
            action.run();
            fail("Expected IOException");
        } catch (IOException expected) {
            // Expected fail-closed behavior.
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
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
}
