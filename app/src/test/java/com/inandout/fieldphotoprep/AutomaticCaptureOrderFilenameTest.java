package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class AutomaticCaptureOrderFilenameTest {
    private static final String ID1 = "11111111-1111-4111-8111-111111111111";
    private static final String ID2 = "22222222-2222-4222-8222-222222222222";
    private static final String ID3 = "33333333-3333-4333-8333-333333333333";
    private static final String ID4 = "44444444-4444-4444-8444-444444444444";
    private static final DriveFolder ADDRESS = new DriveFolder("address-a", "Address A");
    private static final DriveFolder WORK_A = new DriveFolder("work-a", "Inspection - 2026-09-17");
    private static final DriveFolder WORK_A_REUSED =
            new DriveFolder("work-a", "Inspection - 2026-09-24");
    private static final DriveFolder WORK_A_REUSED_AGAIN =
            new DriveFolder("work-a", "Inspection - 2026-10-01");
    private static final DriveFolder WORK_A_RENAMED =
            new DriveFolder("work-a", "Inspection - renamed in Drive");
    private static final DriveFolder WORK_B = new DriveFolder("work-b", "Grass Cut - 2026-09-17");

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void schemaV4RoundTripPreservesSequenceAndRemoteName() {
        PendingPhotoRecord record = PendingPhotoRecord.createCapturing(
                ID1, 1_700_000_000_000L,
                ADDRESS.id(), ADDRESS.name(), WORK_A.id(), WORK_A.name(), 7);

        PendingPhotoRecord restored = PendingPhotoRecord.fromProperties(record.toProperties());

        assertEquals(7, restored.captureSequence());
        assertTrue(restored.hasCaptureSequence());
        assertEquals("007_field-photo-" + ID1 + ".jpg",
                DrivePhotoUploader.remoteFileNameFor(restored));
    }

    @Test
    public void schemaV3RemainsReadableAndUsesLegacyRemoteName() {
        PendingPhotoRecord record = PendingPhotoRecord.createCapturing(
                ID1, 1_700_000_000_000L,
                ADDRESS.id(), ADDRESS.name(), WORK_A.id(), WORK_A.name(), 7);
        Properties legacy = record.toProperties();
        legacy.setProperty("schemaVersion", "3");
        legacy.remove("captureSequence");

        PendingPhotoRecord restored = PendingPhotoRecord.fromProperties(legacy);

        assertEquals(0, restored.captureSequence());
        assertFalse(restored.hasCaptureSequence());
        assertEquals("field-photo-" + ID1 + ".jpg",
                DrivePhotoUploader.remoteFileNameFor(restored));
    }

    @Test
    public void newCapturesAdvanceAndSeparateWorkOrdersStartAtOne() throws Exception {
        File root = temporaryFolder.newFolder("queue");
        PendingPhotoStore store = store(root, ID1, ID2, ID3, ID4);

        assertEquals(1, store.beginCapture(ADDRESS, WORK_A).captureSequence());
        assertEquals(2, store.beginCapture(ADDRESS, WORK_A).captureSequence());
        assertEquals(1, store.beginCapture(ADDRESS, WORK_B).captureSequence());
        assertEquals(3, store.beginCapture(ADDRESS, WORK_A).captureSequence());
    }

    @Test
    public void storeReopenContinuesFromDurableLedger() throws Exception {
        File root = temporaryFolder.newFolder("queue-reopen");
        PendingPhotoStore first = store(root, ID1);
        assertEquals(1, first.beginCapture(ADDRESS, WORK_A).captureSequence());

        PendingPhotoStore reopened = store(root, ID2);
        assertEquals(2, reopened.beginCapture(ADDRESS, WORK_A).captureSequence());
    }

    @Test
    public void removingEmptyReservationDoesNotReuseConsumedSequence() throws Exception {
        File root = temporaryFolder.newFolder("queue-empty-gap");
        PendingPhotoStore store = store(root, ID1, ID2);
        PendingPhotoRecord first = store.beginCapture(ADDRESS, WORK_A);
        assertEquals(1, first.captureSequence());
        assertNull(store.finishCaptureIfImageExists(first.id()));

        PendingPhotoRecord second = store.beginCapture(ADDRESS, WORK_A);
        assertEquals(2, second.captureSequence());
    }

    @Test
    public void explicitLocalDiscardDoesNotReuseConsumedSequence() throws Exception {
        File root = temporaryFolder.newFolder("queue-discard-gap");
        PendingPhotoStore store = store(root, ID1, ID2);
        PendingPhotoRecord first = store.beginCapture(ADDRESS, WORK_A);
        Files.write(store.imageFile(first).toPath(), new byte[] {1, 2, 3});
        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(first.id());
        assertTrue(waiting != null && waiting.state() == PendingPhotoRecord.State.WAITING);
        store.discard(first.id());

        PendingPhotoRecord second = store.beginCapture(ADDRESS, WORK_A);
        assertEquals(2, second.captureSequence());
    }

    @Test
    public void missingLedgerBootstrapsAfterRetainedLegacyRecords() throws Exception {
        File root = temporaryFolder.newFolder("queue-legacy-bootstrap");
        writeLegacyRecord(root, ID1, 1_700_000_000_001L);
        writeLegacyRecord(root, ID2, 1_700_000_000_002L);

        PendingPhotoStore store = store(root, ID3);
        PendingPhotoRecord next = store.beginCapture(ADDRESS, WORK_A);

        assertEquals(3, next.captureSequence());
    }

    @Test
    public void corruptLedgerFailsClosedBeforePhotoReservation() throws Exception {
        File root = temporaryFolder.newFolder("queue-corrupt-ledger");
        Files.write(
                new File(root, PendingPhotoStore.CAPTURE_SEQUENCE_LEDGER_FILE).toPath(),
                "work-a=not-a-number\n".getBytes(StandardCharsets.UTF_8));
        PendingPhotoStore store = store(root, ID1);

        try {
            store.beginCapture(ADDRESS, WORK_A);
            fail("Expected corrupt sequence ledger to block capture reservation");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("invalid value"));
        }
        assertTrue(store.scan().records().isEmpty());
    }

    @Test
    public void sequenceAbove999WidensWithoutChangingUuidIdentity() {
        PendingPhotoRecord record = PendingPhotoRecord.createCapturing(
                ID1, 1_700_000_000_000L,
                ADDRESS.id(), ADDRESS.name(), WORK_A.id(), WORK_A.name(), 1000);

        assertEquals("1000_field-photo-" + ID1 + ".jpg",
                DrivePhotoUploader.remoteFileNameFor(record));
    }

    @Test
    public void uploadStateTransitionsPreserveSequence() {
        PendingPhotoRecord uploaded = PendingPhotoRecord.createCapturing(
                ID1, 1_700_000_000_000L,
                ADDRESS.id(), ADDRESS.name(), WORK_A.id(), WORK_A.name(), 9)
                .withState(PendingPhotoRecord.State.WAITING)
                .beginUploadAttempt(1_700_000_100_000L)
                .recordProvisionalRemoteFileId("remote-9")
                .markUploadConfirmed("remote-9");

        assertEquals(9, uploaded.captureSequence());
        assertEquals("009_field-photo-" + ID1 + ".jpg",
                DrivePhotoUploader.remoteFileNameFor(uploaded));
    }

    @Test
    public void reusedWorkOrderStartsAtOneAndContinuesAtTwo() throws Exception {
        File root = temporaryFolder.newFolder("queue-reused-occurrence");
        PendingPhotoStore store = store(root, ID1, ID2, ID3, ID4);
        confirmUploaded(store, store.beginCapture(ADDRESS, WORK_A), "remote-old-1");
        confirmUploaded(store, store.beginCapture(ADDRESS, WORK_A), "remote-old-2");

        store.prepareCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());
        store.completeCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());

        PendingPhotoRecord firstNew = store.beginCapture(ADDRESS, WORK_A_REUSED);
        PendingPhotoRecord secondNew = store.beginCapture(ADDRESS, WORK_A_REUSED);

        assertEquals(1, firstNew.captureSequence());
        assertEquals(2, secondNew.captureSequence());
        assertEquals("001_field-photo-" + ID3 + ".jpg",
                DrivePhotoUploader.remoteFileNameFor(firstNew));
        assertEquals("002_field-photo-" + ID4 + ".jpg",
                DrivePhotoUploader.remoteFileNameFor(secondNew));
    }

    @Test
    public void reusedWorkOrderResetSurvivesStoreReopen() throws Exception {
        File root = temporaryFolder.newFolder("queue-reuse-reopen");
        PendingPhotoStore first = store(root, ID1, ID2);
        confirmUploaded(first, first.beginCapture(ADDRESS, WORK_A), "remote-old-1");
        confirmUploaded(first, first.beginCapture(ADDRESS, WORK_A), "remote-old-2");
        first.prepareCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());
        first.completeCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());

        PendingPhotoStore reopened = store(root, ID3, ID4);
        assertEquals(1, reopened.beginCapture(ADDRESS, WORK_A_REUSED).captureSequence());
        assertEquals(2, reopened.beginCapture(ADDRESS, WORK_A_REUSED).captureSequence());
    }

    @Test
    public void pendingReuseResetSelfHealsOnlyWhenVerifiedTargetNameIsOpen() throws Exception {
        File root = temporaryFolder.newFolder("queue-reuse-pending");
        PendingPhotoStore first = store(root, ID1);
        confirmUploaded(first, first.beginCapture(ADDRESS, WORK_A), "remote-old-1");
        first.prepareCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());

        PendingPhotoStore reopened = store(root, ID2, ID3);
        try {
            reopened.beginCapture(ADDRESS, WORK_A);
            fail("Expected old folder name to remain blocked while reuse reset is pending");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("unfinished reuse reset"));
        }

        PendingPhotoRecord recovered = reopened.beginCapture(ADDRESS, WORK_A_REUSED);
        assertEquals(1, recovered.captureSequence());
        reopened.completeCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());
    }

    @Test
    public void reuseResetPreparationBlocksUnconfirmedOldPhoto() throws Exception {
        File root = temporaryFolder.newFolder("queue-reuse-unconfirmed");
        PendingPhotoStore store = store(root, ID1);
        PendingPhotoRecord old = store.beginCapture(ADDRESS, WORK_A);
        Files.write(store.imageFile(old).toPath(), new byte[] {9, 8, 7});
        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(old.id());
        assertEquals(PendingPhotoRecord.State.WAITING, waiting.state());

        try {
            store.prepareCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());
            fail("Expected unconfirmed old photo to block work-order reuse reset");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("unconfirmed local photo"));
        }
    }

    @Test
    public void confirmedOldHistoryDoesNotRaiseReusedOccurrenceBaseline() throws Exception {
        File root = temporaryFolder.newFolder("queue-reuse-confirmed-history");
        PendingPhotoStore store = store(root, ID1, ID2, ID3);
        PendingPhotoRecord old1 = store.beginCapture(ADDRESS, WORK_A);
        confirmUploaded(store, old1, "remote-old-1");
        PendingPhotoRecord old2 = store.beginCapture(ADDRESS, WORK_A);
        PendingPhotoRecord confirmed2 = confirmUploaded(store, old2, "remote-old-2");
        assertEquals(2, confirmed2.captureSequence());

        store.prepareCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());
        store.completeCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());
        PendingPhotoRecord newOccurrence = store.beginCapture(ADDRESS, WORK_A_REUSED);

        assertEquals(1, newOccurrence.captureSequence());
    }

    @Test
    public void anotherReuseOfSameProviderIdentityRestartsAgainAtOne() throws Exception {
        File root = temporaryFolder.newFolder("queue-reuse-again");
        PendingPhotoStore store = store(root, ID1, ID2, ID3);
        confirmUploaded(store, store.beginCapture(ADDRESS, WORK_A), "remote-old");
        store.prepareCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());
        store.completeCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());
        PendingPhotoRecord middle = store.beginCapture(ADDRESS, WORK_A_REUSED);
        confirmUploaded(store, middle, "remote-middle");
        assertEquals(1, middle.captureSequence());

        store.prepareCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED_AGAIN.name());
        store.completeCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED_AGAIN.name());
        PendingPhotoRecord newest = store.beginCapture(ADDRESS, WORK_A_REUSED_AGAIN);

        assertEquals(1, newest.captureSequence());
    }

    @Test
    public void completedReuseContinuesByProviderIdentityAfterVisibleRename() throws Exception {
        File root = temporaryFolder.newFolder("queue-reuse-visible-rename");
        PendingPhotoStore store = store(root, ID1, ID2, ID3);
        confirmUploaded(store, store.beginCapture(ADDRESS, WORK_A), "remote-old");
        store.prepareCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());
        store.completeCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());

        PendingPhotoRecord firstNew = store.beginCapture(ADDRESS, WORK_A_REUSED);
        PendingPhotoRecord afterVisibleRename = store.beginCapture(ADDRESS, WORK_A_RENAMED);

        assertEquals(1, firstNew.captureSequence());
        assertEquals(2, afterVisibleRename.captureSequence());
    }

    @Test
    public void discardedReservationGapStillRemainsConsumedAfterReuseReset() throws Exception {
        File root = temporaryFolder.newFolder("queue-reuse-gap");
        PendingPhotoStore store = store(root, ID1, ID2, ID3);
        confirmUploaded(store, store.beginCapture(ADDRESS, WORK_A), "remote-old");
        store.prepareCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());
        store.completeCaptureSequenceResetForReuse(WORK_A.id(), WORK_A_REUSED.name());

        PendingPhotoRecord firstNew = store.beginCapture(ADDRESS, WORK_A_REUSED);
        assertEquals(1, firstNew.captureSequence());
        assertNull(store.finishCaptureIfImageExists(firstNew.id()));

        PendingPhotoRecord secondNew = store.beginCapture(ADDRESS, WORK_A_REUSED);
        assertEquals(2, secondNew.captureSequence());
    }

    private PendingPhotoRecord confirmUploaded(
            PendingPhotoStore store,
            PendingPhotoRecord capturing,
            String remoteId) throws Exception {
        Files.write(store.imageFile(capturing).toPath(), new byte[] {1, 2, 3, 4});
        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(capturing.id());
        assertTrue(waiting != null);
        store.beginUploadAttempt(capturing.id());
        store.recordProvisionalRemoteFileId(capturing.id(), remoteId);
        return store.markUploadConfirmed(capturing.id(), remoteId);
    }

    private PendingPhotoStore store(File root, String... ids) {
        final int[] index = {0};
        final long[] time = {1_700_000_000_000L};
        return new PendingPhotoStore(
                root,
                () -> {
                    if (index[0] >= ids.length) {
                        throw new IllegalStateException("No more deterministic photo ids.");
                    }
                    return ids[index[0]++];
                },
                () -> time[0]++);
    }

    private void writeLegacyRecord(File root, String id, long createdAt) throws Exception {
        PendingPhotoRecord legacy = PendingPhotoRecord.createCapturing(
                id, createdAt,
                ADDRESS.id(), ADDRESS.name(), WORK_A.id(), WORK_A.name());
        Properties properties = legacy.toProperties();
        properties.setProperty("schemaVersion", "3");
        properties.remove("captureSequence");
        File metadata = new File(root, PendingPhotoRecord.metadataFileNameFor(id));
        try (FileOutputStream output = new FileOutputStream(metadata)) {
            properties.store(output, "legacy fixture");
            output.getFD().sync();
        }
    }
}
