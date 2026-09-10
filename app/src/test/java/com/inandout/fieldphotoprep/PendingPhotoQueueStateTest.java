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

public final class PendingPhotoQueueStateTest {
    private static final String ID1 = "11111111-1111-4111-8111-111111111111";
    private static final String ID2 = "22222222-2222-4222-8222-222222222222";
    private static final long CAPTURE_TIME = 1_700_000_000_000L;
    private static final long ATTEMPT_TIME_1 = 1_700_000_100_000L;
    private static final long ATTEMPT_TIME_2 = 1_700_000_200_000L;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void legacyWaitingRecordLoadsWithSafeDefaultsAndNextWriteUsesSchema2() throws Exception {
        File root = temporaryFolder.newFolder("legacy");
        writeLegacyRecord(root, ID1, PendingPhotoRecord.State.WAITING,
                "address-1", "Address One", "work-1", "Cut Grass - 2026-09-09");
        writeImageBytes(new File(root, PendingPhotoRecord.imageFileNameFor(ID1)), "legacy-image");

        PendingPhotoStore store = new PendingPhotoStore(root, () -> ID2, () -> ATTEMPT_TIME_1);
        PendingPhotoRecord loaded = store.getById(ID1);

        assertEquals(PendingPhotoRecord.State.WAITING, loaded.state());
        assertEquals(0, loaded.uploadAttemptCount());
        assertEquals(0L, loaded.lastAttemptAtEpochMs());
        assertNull(loaded.statusDetail());
        assertNull(loaded.remoteFileId());

        PendingPhotoRecord uploading = store.beginUploadAttempt(ID1);
        Properties persisted = loadProperties(new File(root, uploading.metadataFileName()));

        assertEquals("2", persisted.getProperty("schemaVersion"));
        assertEquals("1", persisted.getProperty("uploadAttemptCount"));
        assertEquals(Long.toString(ATTEMPT_TIME_1), persisted.getProperty("lastAttemptAtEpochMs"));
        assertEquals("work-1", uploading.workOrderId());
    }

    @Test
    public void legacyCapturingRecordStillReconcilesToWaiting() throws Exception {
        File root = temporaryFolder.newFolder("legacy-capture");
        writeLegacyRecord(root, ID1, PendingPhotoRecord.State.CAPTURING,
                "address-1", "Address One", "work-1", "Work - 2026-09-09");
        writeImageBytes(new File(root, PendingPhotoRecord.imageFileNameFor(ID1)), "captured-image");

        PendingPhotoStore store = new PendingPhotoStore(root);
        PendingPhotoStore.ScanResult result = store.reconcileInterruptedCaptures();

        assertEquals(1, result.records().size());
        assertEquals(PendingPhotoRecord.State.WAITING, result.records().get(0).state());
        assertTrue(store.hasImageData(result.records().get(0)));
        Properties persisted = loadProperties(new File(root, result.records().get(0).metadataFileName()));
        assertEquals("2", persisted.getProperty("schemaVersion"));
    }

    @Test
    public void unknownSchemaFailsClosedAndLeavesPairedImageUntouched() throws Exception {
        File root = temporaryFolder.newFolder("unknown-schema");
        Properties properties = legacyProperties(ID1, PendingPhotoRecord.State.WAITING,
                "address-1", "Address One", "work-1", "Work - 2026-09-09");
        properties.setProperty("schemaVersion", "99");
        writeProperties(new File(root, PendingPhotoRecord.metadataFileNameFor(ID1)), properties);
        File image = new File(root, PendingPhotoRecord.imageFileNameFor(ID1));
        writeImageBytes(image, "valuable-image");

        PendingPhotoStore.ScanResult scan = new PendingPhotoStore(root).scan();

        assertEquals(1, scan.corruptMetadataFiles().size());
        assertEquals(PendingPhotoRecord.metadataFileNameFor(ID1), scan.corruptMetadataFiles().get(0));
        assertTrue(image.isFile());
        assertTrue(image.length() > 0);
    }

    @Test
    public void waitingToUploadingPersistsAttemptAndExactDestinationAcrossReload() throws Exception {
        File root = temporaryFolder.newFolder("uploading");
        PendingPhotoStore store = waitingStore(root, ID1, ATTEMPT_TIME_1,
                "address-1", "work-1");

        PendingPhotoRecord uploading = store.beginUploadAttempt(ID1);
        PendingPhotoRecord reloaded = new PendingPhotoStore(root).getById(ID1);

        assertEquals(PendingPhotoRecord.State.UPLOADING, uploading.state());
        assertEquals(PendingPhotoRecord.State.UPLOADING, reloaded.state());
        assertEquals(1, reloaded.uploadAttemptCount());
        assertEquals(ATTEMPT_TIME_1, reloaded.lastAttemptAtEpochMs());
        assertEquals("address-1", reloaded.addressId());
        assertEquals("work-1", reloaded.workOrderId());
        assertTrue(new PendingPhotoStore(root).hasImageData(reloaded));
    }

    @Test
    public void failedRetryIncrementsAttemptAndNeverChangesDestination() throws Exception {
        File root = temporaryFolder.newFolder("retry");
        MutableTime time = new MutableTime(ATTEMPT_TIME_1);
        PendingPhotoStore store = waitingStore(root, ID1, time,
                "address-1", "work-original");

        store.beginUploadAttempt(ID1);
        PendingPhotoRecord failed = store.markUploadFailed(ID1, "No service");
        assertEquals(PendingPhotoRecord.State.FAILED, failed.state());
        assertEquals(1, failed.uploadAttemptCount());
        assertEquals("work-original", failed.workOrderId());

        time.value = ATTEMPT_TIME_2;
        PendingPhotoRecord retry = store.beginUploadAttempt(ID1);

        assertEquals(PendingPhotoRecord.State.UPLOADING, retry.state());
        assertEquals(2, retry.uploadAttemptCount());
        assertEquals(ATTEMPT_TIME_2, retry.lastAttemptAtEpochMs());
        assertEquals("address-1", retry.addressId());
        assertEquals("work-original", retry.workOrderId());
        assertNull(retry.statusDetail());
    }

    @Test
    public void failurePersistsDetailWithoutDeletingProtectedImage() throws Exception {
        File root = temporaryFolder.newFolder("failed");
        PendingPhotoStore store = waitingStore(root, ID1, ATTEMPT_TIME_1,
                "address-1", "work-1");
        store.beginUploadAttempt(ID1);

        PendingPhotoRecord failed = store.markUploadFailed(ID1, "Network unavailable");
        PendingPhotoRecord reloaded = new PendingPhotoStore(root).getById(ID1);

        assertEquals(PendingPhotoRecord.State.FAILED, reloaded.state());
        assertEquals("Network unavailable", reloaded.statusDetail());
        assertEquals(1, reloaded.uploadAttemptCount());
        assertTrue(store.hasImageData(failed));
        assertTrue(reloaded.canBeginUploadAttempt());
    }

    @Test
    public void explicitUncertainStateBlocksBlindRetry() throws Exception {
        File root = temporaryFolder.newFolder("uncertain");
        PendingPhotoStore store = waitingStore(root, ID1, ATTEMPT_TIME_1,
                "address-1", "work-1");
        store.beginUploadAttempt(ID1);

        PendingPhotoRecord uncertain = store.markUploadUncertain(ID1, "Provider result timed out");

        assertEquals(PendingPhotoRecord.State.UNCERTAIN, uncertain.state());
        assertFalse(uncertain.canBeginUploadAttempt());
        assertEquals(1, store.scan().uncertainPhotoIds().size());
        expectIOException(() -> store.beginUploadAttempt(ID1));
        assertEquals(PendingPhotoRecord.State.UNCERTAIN, store.getById(ID1).state());
        assertTrue(store.hasImageData(uncertain));
    }

    @Test
    public void restartReconciliationConvertsUploadingToUncertainNotFailedOrWaiting() throws Exception {
        File root = temporaryFolder.newFolder("restart");
        PendingPhotoStore firstProcess = waitingStore(root, ID1, ATTEMPT_TIME_1,
                "address-1", "work-1");
        firstProcess.beginUploadAttempt(ID1);

        PendingPhotoStore restarted = new PendingPhotoStore(root);
        PendingPhotoStore.ScanResult scan = restarted.reconcileInterruptedUploads();
        PendingPhotoRecord recovered = restarted.getById(ID1);

        assertEquals(PendingPhotoRecord.State.UNCERTAIN, recovered.state());
        assertEquals(1, recovered.uploadAttemptCount());
        assertEquals(ATTEMPT_TIME_1, recovered.lastAttemptAtEpochMs());
        assertEquals("work-1", recovered.workOrderId());
        assertTrue(restarted.hasImageData(recovered));
        assertEquals(1, scan.uncertainPhotoIds().size());
        expectIOException(() -> restarted.beginUploadAttempt(ID1));
    }

    @Test
    public void confirmedSuccessRequiresRemoteIdentityAndIsTerminalForAutomaticRetry() throws Exception {
        File root = temporaryFolder.newFolder("confirmed");
        PendingPhotoStore store = waitingStore(root, ID1, ATTEMPT_TIME_1,
                "address-1", "work-1");
        store.beginUploadAttempt(ID1);

        expectIOException(() -> store.markUploadConfirmed(ID1, "  "));
        assertEquals(PendingPhotoRecord.State.UPLOADING, store.getById(ID1).state());

        PendingPhotoRecord uploaded = store.markUploadConfirmed(ID1, "remote-file-123");
        PendingPhotoRecord reloaded = new PendingPhotoStore(root).getById(ID1);

        assertEquals(PendingPhotoRecord.State.UPLOADED, reloaded.state());
        assertEquals("remote-file-123", reloaded.remoteFileId());
        assertFalse(reloaded.canBeginUploadAttempt());
        assertTrue(store.hasImageData(uploaded));
        expectIOException(() -> store.beginUploadAttempt(ID1));
    }

    @Test
    public void uncertainUploadCanLaterBeConfirmedWithoutBlindRetry() throws Exception {
        File root = temporaryFolder.newFolder("uncertain-confirmed");
        PendingPhotoStore store = waitingStore(root, ID1, ATTEMPT_TIME_1,
                "address-1", "work-1");
        store.beginUploadAttempt(ID1);
        store.markUploadUncertain(ID1, "Need remote reconciliation");

        PendingPhotoRecord uploaded = store.markUploadConfirmed(ID1, "remote-after-reconcile");

        assertEquals(PendingPhotoRecord.State.UPLOADED, uploaded.state());
        assertEquals(1, uploaded.uploadAttemptCount());
        assertEquals("remote-after-reconcile", uploaded.remoteFileId());
        assertEquals("work-1", uploaded.workOrderId());
    }

    @Test
    public void onePhotoFailureDoesNotChangeAnotherQueueRecord() throws Exception {
        File root = temporaryFolder.newFolder("isolation");
        SequenceIds ids = new SequenceIds(ID1, ID2);
        PendingPhotoStore store = new PendingPhotoStore(root, ids, () -> ATTEMPT_TIME_1);
        PendingPhotoRecord first = captureWaiting(store, "address-1", "work-1");
        PendingPhotoRecord second = captureWaiting(store, "address-2", "work-2");

        store.beginUploadAttempt(first.id());
        store.markUploadFailed(first.id(), "First failed");

        PendingPhotoRecord firstReloaded = store.getById(first.id());
        PendingPhotoRecord secondReloaded = store.getById(second.id());

        assertEquals(PendingPhotoRecord.State.FAILED, firstReloaded.state());
        assertEquals(PendingPhotoRecord.State.WAITING, secondReloaded.state());
        assertEquals(0, secondReloaded.uploadAttemptCount());
        assertEquals("work-2", secondReloaded.workOrderId());
        assertTrue(store.hasImageData(secondReloaded));
    }

    @Test
    public void discardRefusesUploadingUncertainAndUploadedEvidence() throws Exception {
        File rootUploading = temporaryFolder.newFolder("discard-uploading");
        PendingPhotoStore uploadingStore = waitingStore(rootUploading, ID1, ATTEMPT_TIME_1,
                "address-1", "work-1");
        PendingPhotoRecord uploading = uploadingStore.beginUploadAttempt(ID1);
        expectIOException(() -> uploadingStore.discard(ID1));
        assertTrue(uploadingStore.hasImageData(uploading));

        File rootUncertain = temporaryFolder.newFolder("discard-uncertain");
        PendingPhotoStore uncertainStore = waitingStore(rootUncertain, ID1, ATTEMPT_TIME_1,
                "address-1", "work-1");
        uncertainStore.beginUploadAttempt(ID1);
        PendingPhotoRecord uncertain = uncertainStore.markUploadUncertain(ID1, "Unknown remote result");
        expectIOException(() -> uncertainStore.discard(ID1));
        assertTrue(uncertainStore.hasImageData(uncertain));

        File rootUploaded = temporaryFolder.newFolder("discard-uploaded");
        PendingPhotoStore uploadedStore = waitingStore(rootUploaded, ID1, ATTEMPT_TIME_1,
                "address-1", "work-1");
        uploadedStore.beginUploadAttempt(ID1);
        PendingPhotoRecord uploaded = uploadedStore.markUploadConfirmed(ID1, "remote-1");
        expectIOException(() -> uploadedStore.discard(ID1));
        assertTrue(uploadedStore.hasImageData(uploaded));
    }

    @Test
    public void missingImageInFailedOrUncertainStateIsSurfacedNotDeleted() throws Exception {
        File root = temporaryFolder.newFolder("missing-queued-image");
        PendingPhotoStore store = waitingStore(root, ID1, ATTEMPT_TIME_1,
                "address-1", "work-1");
        store.beginUploadAttempt(ID1);
        PendingPhotoRecord uncertain = store.markUploadUncertain(ID1, "Unknown remote result");
        assertTrue(store.imageFile(uncertain).delete());

        PendingPhotoStore.ScanResult scan = store.scan();

        assertEquals(1, scan.unusableQueuedPhotoIds().size());
        assertEquals(ID1, scan.unusableQueuedPhotoIds().get(0));
        assertEquals(1, scan.uncertainPhotoIds().size());
        assertTrue(new File(root, uncertain.metadataFileName()).isFile());
    }

    private static PendingPhotoStore waitingStore(
            File root,
            String id,
            long attemptTime,
            String addressId,
            String workOrderId) throws Exception {
        return waitingStore(root, id, new MutableTime(attemptTime), addressId, workOrderId);
    }

    private static PendingPhotoStore waitingStore(
            File root,
            String id,
            PendingPhotoStore.TimeSource timeSource,
            String addressId,
            String workOrderId) throws Exception {
        PendingPhotoStore store = new PendingPhotoStore(root, () -> id, timeSource);
        PendingPhotoRecord record = store.beginCapture(
                new DriveFolder(addressId, "Address " + addressId),
                new DriveFolder(workOrderId, "Work " + workOrderId));
        writeImageBytes(store.imageFile(record), "jpeg-data-" + id);
        store.finishCaptureIfImageExists(id);
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

    private static void writeLegacyRecord(
            File root,
            String id,
            PendingPhotoRecord.State state,
            String addressId,
            String addressName,
            String workOrderId,
            String workOrderName) throws Exception {
        writeProperties(
                new File(root, PendingPhotoRecord.metadataFileNameFor(id)),
                legacyProperties(id, state, addressId, addressName, workOrderId, workOrderName));
    }

    private static Properties legacyProperties(
            String id,
            PendingPhotoRecord.State state,
            String addressId,
            String addressName,
            String workOrderId,
            String workOrderName) {
        Properties properties = new Properties();
        properties.setProperty("id", id);
        properties.setProperty("imageFile", PendingPhotoRecord.imageFileNameFor(id));
        properties.setProperty("state", state.name());
        properties.setProperty("createdAtEpochMs", Long.toString(CAPTURE_TIME));
        properties.setProperty("addressId", addressId);
        properties.setProperty("addressName", addressName);
        properties.setProperty("workOrderId", workOrderId);
        properties.setProperty("workOrderName", workOrderName);
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

    private static final class MutableTime implements PendingPhotoStore.TimeSource {
        long value;

        MutableTime(long value) {
            this.value = value;
        }

        @Override
        public long nowEpochMs() {
            return value;
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
}
