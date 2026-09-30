package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class PendingPhotoStoreTest {
    private static final String ID1 = "11111111-1111-4111-8111-111111111111";
    private static final String ID2 = "22222222-2222-4222-8222-222222222222";
    private static final String ID3 = "33333333-3333-4333-8333-333333333333";
    private static final String ID4 = "44444444-4444-4444-8444-444444444444";
    private static final String ID5 = "55555555-5555-4555-8555-555555555555";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void beginCapturePersistsExactBindingBeforeImageDataExists() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        PendingPhotoStore store = store(root, ID1, 1_700_000_000_000L);
        DriveFolder address = new DriveFolder("address-id", "1607 Crestview Drive");
        DriveFolder workOrder = new DriveFolder("work-id", "Cut Grass - 2026-09-09");

        PendingPhotoRecord created = store.beginCapture(address, workOrder);
        PendingPhotoStore reloaded = new PendingPhotoStore(root);
        PendingPhotoRecord persisted = reloaded.getById(ID1);

        assertEquals(PendingPhotoRecord.State.CAPTURING, created.state());
        assertEquals("address-id", persisted.addressId());
        assertEquals("work-id", persisted.workOrderId());
        assertEquals("Cut Grass - 2026-09-09", persisted.workOrderName());
        assertTrue(reloaded.imageFile(persisted).isFile());
        assertEquals(0L, reloaded.imageFile(persisted).length());
    }

    @Test
    public void successfulCaptureRequiresBytesThenPersistsWaitingAcrossReload() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        PendingPhotoStore store = store(root, ID1, 1_700_000_000_000L);
        PendingPhotoRecord created = store.beginCapture(
                new DriveFolder("address-id", "Address"),
                new DriveFolder("work-id", "Cut Grass - 2026-09-09"));
        writeImageBytes(store.imageFile(created), "jpeg-data");

        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(ID1);
        PendingPhotoRecord reloaded = new PendingPhotoStore(root).getById(ID1);

        assertEquals(PendingPhotoRecord.State.WAITING, waiting.state());
        assertEquals(PendingPhotoRecord.State.WAITING, reloaded.state());
        assertTrue(new PendingPhotoStore(root).hasImageData(reloaded));
    }

    @Test
    public void emptyCameraReturnRemovesOnlyEmptyReservation() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        PendingPhotoStore store = store(root, ID1, 1_700_000_000_000L);
        PendingPhotoRecord created = store.beginCapture(
                new DriveFolder("address-id", "Address"),
                new DriveFolder("work-id", "Work - 2026-09-09"));
        File image = store.imageFile(created);

        PendingPhotoRecord result = store.finishCaptureIfImageExists(ID1);

        assertNull(result);
        assertFalse(image.exists());
        assertNull(store.getById(ID1));
    }

    @Test
    public void interruptedCaptureWithBytesIsPreservedAsWaiting() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        PendingPhotoStore store = store(root, ID1, 1_700_000_000_000L);
        PendingPhotoRecord capturing = store.beginCapture(
                new DriveFolder("address-id", "Address"),
                new DriveFolder("work-id", "Work - 2026-09-09"));
        writeImageBytes(store.imageFile(capturing), "captured-before-process-death");

        PendingPhotoStore restarted = new PendingPhotoStore(root);
        PendingPhotoStore.ScanResult result = restarted.reconcileInterruptedCaptures();

        assertEquals(1, result.records().size());
        assertEquals(PendingPhotoRecord.State.WAITING, result.records().get(0).state());
        assertTrue(restarted.hasImageData(result.records().get(0)));
    }

    @Test
    public void interruptedEmptyReservationIsCleanedWithoutTouchingOtherPhoto() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        SequenceIds ids = new SequenceIds(ID1, ID2);
        PendingPhotoStore store = new PendingPhotoStore(root, ids, () -> 1_700_000_000_000L);
        PendingPhotoRecord empty = store.beginCapture(
                new DriveFolder("address-id", "Address"),
                new DriveFolder("work-1", "Work 1 - 2026-09-09"));
        PendingPhotoRecord protectedPhoto = store.beginCapture(
                new DriveFolder("address-id", "Address"),
                new DriveFolder("work-2", "Work 2 - 2026-09-09"));
        writeImageBytes(store.imageFile(protectedPhoto), "keep-me");
        store.finishCaptureIfImageExists(ID2);

        PendingPhotoStore.ScanResult result = store.reconcileInterruptedCaptures();

        assertNull(store.getById(empty.id()));
        assertFalse(store.imageFile(empty).exists());
        assertEquals(1, result.records().size());
        assertEquals(ID2, result.records().get(0).id());
        assertTrue(store.hasImageData(result.records().get(0)));
    }

    @Test
    public void destinationBindingDoesNotChangeWhenAnotherWorkOrderExists() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        PendingPhotoStore store = store(root, ID1, 1_700_000_000_000L);
        PendingPhotoRecord created = store.beginCapture(
                new DriveFolder("address-1", "Address One"),
                new DriveFolder("work-1", "Cut Grass - 2026-09-09"));
        DriveFolder laterSelection = new DriveFolder("work-2", "Winterization - 2026-11-15");

        PendingPhotoRecord reloaded = store.getById(created.id());

        assertEquals("work-1", reloaded.workOrderId());
        assertFalse(laterSelection.id().equals(reloaded.workOrderId()));
    }

    @Test
    public void discardRemovesOnlySelectedLocalPhoto() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        SequenceIds ids = new SequenceIds(ID1, ID2);
        PendingPhotoStore store = new PendingPhotoStore(root, ids, () -> 1_700_000_000_000L);
        PendingPhotoRecord first = store.beginCapture(
                new DriveFolder("address-id", "Address"),
                new DriveFolder("work-id", "Work - 2026-09-09"));
        writeImageBytes(store.imageFile(first), "first");
        store.finishCaptureIfImageExists(ID1);
        PendingPhotoRecord second = store.beginCapture(
                new DriveFolder("address-id", "Address"),
                new DriveFolder("work-id", "Work - 2026-09-09"));
        writeImageBytes(store.imageFile(second), "second");
        store.finishCaptureIfImageExists(ID2);

        store.discard(ID1);
        List<PendingPhotoRecord> remaining = store.recordsForWorkOrder("work-id");

        assertNull(store.getById(ID1));
        assertEquals(1, remaining.size());
        assertEquals(ID2, remaining.get(0).id());
        assertTrue(store.hasImageData(remaining.get(0)));
    }

    @Test
    public void corruptMetadataDoesNotDeletePairedImage() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        File metadata = new File(root, PendingPhotoRecord.metadataFileNameFor(ID1));
        Files.write(metadata.toPath(), "bad metadata".getBytes(StandardCharsets.UTF_8));
        File image = new File(root, PendingPhotoRecord.imageFileNameFor(ID1));
        writeImageBytes(image, "valuable-image-bytes");

        PendingPhotoStore.ScanResult result = new PendingPhotoStore(root).reconcileInterruptedCaptures();

        assertEquals(1, result.corruptMetadataFiles().size());
        assertTrue(image.isFile());
        assertTrue(image.length() > 0);
    }

    @Test
    public void waitingRecordWithMissingImageIsSurfacedNotDeleted() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        PendingPhotoStore store = store(root, ID1, 1_700_000_000_000L);
        PendingPhotoRecord created = store.beginCapture(
                new DriveFolder("address-id", "Address"),
                new DriveFolder("work-id", "Work - 2026-09-09"));
        writeImageBytes(store.imageFile(created), "bytes");
        store.finishCaptureIfImageExists(ID1);
        assertTrue(store.imageFile(created).delete());

        PendingPhotoStore.ScanResult scan = store.scan();

        assertEquals(1, scan.records().size());
        assertEquals(1, scan.unusableWaitingPhotoIds().size());
        assertEquals(ID1, scan.unusableWaitingPhotoIds().get(0));
        assertTrue(new File(root, PendingPhotoRecord.metadataFileNameFor(ID1)).isFile());
    }

    @Test
    public void addressCleanupAllowsConfirmedUploadedHistory() throws Exception {
        File root = temporaryFolder.newFolder("pending-cleanup-uploaded");
        PendingPhotoStore store = store(root, ID1, 1_700_000_000_000L);
        PendingPhotoRecord created = store.beginCapture(
                new DriveFolder("address-id", "Address"),
                new DriveFolder("work-id", "Work - 2026-09-09"));
        writeImageBytes(store.imageFile(created), "bytes");
        store.finishCaptureIfImageExists(ID1);
        store.beginUploadAttempt(ID1);
        store.markUploadConfirmed(ID1, "remote-photo");

        store.requireAddressCleanupSafe("address-id");
    }

    @Test
    public void addressCleanupBlocksAnyUnresolvedPhotoForTargetAddress() throws Exception {
        File root = temporaryFolder.newFolder("pending-cleanup-blocked");
        SequenceIds ids = new SequenceIds(ID1, ID2);
        PendingPhotoStore store = new PendingPhotoStore(root, ids, () -> 1_700_000_000_000L);

        PendingPhotoRecord target = store.beginCapture(
                new DriveFolder("target-address", "Target"),
                new DriveFolder("target-work", "Work - 2026-09-09"));
        writeImageBytes(store.imageFile(target), "keep-target");
        store.finishCaptureIfImageExists(ID1);

        PendingPhotoRecord unrelated = store.beginCapture(
                new DriveFolder("other-address", "Other"),
                new DriveFolder("other-work", "Work - 2026-09-09"));
        writeImageBytes(store.imageFile(unrelated), "keep-other");
        store.finishCaptureIfImageExists(ID2);

        try {
            store.requireAddressCleanupSafe("target-address");
            org.junit.Assert.fail("Expected unresolved target photo to block cleanup");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("WAITING"));
        }

        store.discard(ID1);
        store.requireAddressCleanupSafe("target-address");
    }

    @Test
    public void unreadableMetadataBlocksAddressCleanupBecauseOwnershipCannotBeProven() throws Exception {
        File root = temporaryFolder.newFolder("pending-cleanup-corrupt");
        File metadata = new File(root, PendingPhotoRecord.metadataFileNameFor(ID1));
        Files.write(metadata.toPath(), "bad metadata".getBytes(StandardCharsets.UTF_8));

        try {
            new PendingPhotoStore(root).requireAddressCleanupSafe("address-id");
            org.junit.Assert.fail("Expected unreadable metadata to block cleanup");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("metadata is unreadable"));
        }
    }

    @Test
    public void verifiedAddressArchiveRetiresOldHistoryAndStartsBothWorkOrdersAt001()
            throws Exception {
        File root = temporaryFolder.newFolder("retire-archive");
        PhotoPreparer preparer = new PhotoPreparer(temporaryFolder.newFolder("retire-prepared"));
        SequenceIds ids = new SequenceIds(ID1, ID2, ID3, ID4, ID5);
        PendingPhotoStore store = new PendingPhotoStore(
                root, ids, new java.util.concurrent.atomic.AtomicLong(1_700_000_000_000L)::getAndIncrement);
        DriveFolder address = new DriveFolder("target-address", "99998 TEST");
        DriveFolder workA = new DriveFolder("work-a", "TEST A - 2026-09-29");
        DriveFolder workB = new DriveFolder("work-b", "TEST B - 2026-09-29");
        DriveFolder unrelated = new DriveFolder("other-work", "OTHER - 2026-09-29");
        for (DriveFolder work : java.util.Arrays.asList(workA, workB)) {
            PendingPhotoRecord photo = store.beginCapture(address, work);
            writeImageBytes(store.imageFile(photo), "old-image");
            store.finishCaptureIfImageExists(photo.id());
            store.beginUploadAttempt(photo.id());
            store.markUploadConfirmed(photo.id(), "confirmed-" + work.id());
            writeImageBytes(preparer.preparedFile(photo.id()), "leftover-prepared");
        }
        PendingPhotoRecord outside = store.beginCapture(
                new DriveFolder("unrelated-address", "Other"), unrelated);
        writeImageBytes(store.imageFile(outside), "protected-and-untouched");
        store.finishCaptureIfImageExists(outside.id());

        int retired = store.retireConfirmedAddressHistoryAfterVerifiedRemoteCleanup(
                address.id(), java.util.Arrays.asList(workA, workB), preparer);
        assertEquals(2, retired);
        assertNull(store.getById(ID1));
        assertNull(store.getById(ID2));
        assertFalse(new File(root, PendingPhotoRecord.imageFileNameFor(ID1)).exists());
        assertFalse(new File(root, PendingPhotoRecord.imageFileNameFor(ID2)).exists());
        assertFalse(preparer.preparedFile(ID1).exists());
        assertFalse(preparer.preparedFile(ID2).exists());
        assertEquals(PendingPhotoRecord.State.WAITING, store.getById(outside.id()).state());
        assertTrue(store.hasImageData(outside));
        assertEquals(1, store.beginCapture(address, workA).captureSequence());
        assertEquals(1, store.beginCapture(address, workB).captureSequence());
    }

    @Test
    public void unresolvedPhotoPreventsHistoryRetirementAndSequenceReset() throws Exception {
        File root = temporaryFolder.newFolder("retire-blocked");
        SequenceIds ids = new SequenceIds(ID1, ID2, ID3);
        PendingPhotoStore store = new PendingPhotoStore(root, ids, () -> 1_700_000_000_000L);
        PhotoPreparer preparer = new PhotoPreparer(temporaryFolder.newFolder("blocked-prepared"));
        DriveFolder address = new DriveFolder("target-address", "Address");
        DriveFolder work = new DriveFolder("work-a", "TEST - 2026-09-29");
        PendingPhotoRecord old = store.beginCapture(address, work);
        writeImageBytes(store.imageFile(old), "uploaded");
        store.finishCaptureIfImageExists(old.id());
        store.beginUploadAttempt(old.id());
        store.markUploadConfirmed(old.id(), "remote-old");
        PendingPhotoRecord waiting = store.beginCapture(address, work);
        writeImageBytes(store.imageFile(waiting), "protect-waiting");
        store.finishCaptureIfImageExists(waiting.id());

        try {
            store.retireConfirmedAddressHistoryAfterVerifiedRemoteCleanup(
                    address.id(), java.util.Collections.singletonList(work), preparer);
            org.junit.Assert.fail("Expected WAITING record to block retirement");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("WAITING"));
        }
        assertEquals(PendingPhotoRecord.State.UPLOADED, store.getById(old.id()).state());
        assertTrue(store.hasImageData(waiting));
        assertEquals(3, store.beginCapture(address, work).captureSequence());
    }

    @Test
    public void failedLocalRetirementKeepsMetadataAndDoesNotResetSequence()
            throws Exception {
        File root = temporaryFolder.newFolder("retire-io-fail");
        SequenceIds ids = new SequenceIds(ID1, ID2);
        PendingPhotoStore store = new PendingPhotoStore(root, ids, () -> 1_700_000_000_000L);
        PhotoPreparer preparer = new PhotoPreparer(temporaryFolder.newFolder("failed-prepared"));
        DriveFolder address = new DriveFolder("target-address", "Address");
        DriveFolder work = new DriveFolder("work-a", "TEST - 2026-09-29");
        PendingPhotoRecord old = store.beginCapture(address, work);
        writeImageBytes(store.imageFile(old), "uploaded");
        store.finishCaptureIfImageExists(old.id());
        store.beginUploadAttempt(old.id());
        store.markUploadConfirmed(old.id(), "remote-old");
        // A non-empty directory at the derivative path simulates an undeletable leftover.
        File blocked = preparer.preparedFile(old.id());
        assertTrue(blocked.mkdirs());
        writeImageBytes(new File(blocked, "block"), "keep");
        try {
            store.retireConfirmedAddressHistoryAfterVerifiedRemoteCleanup(
                    address.id(), java.util.Collections.singletonList(work), preparer);
            org.junit.Assert.fail("Expected local retirement to stop on I/O failure");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("prepared local copy"));
        }
        assertTrue(store.getById(old.id()) != null);
        assertTrue(new File(blocked, "block").delete());
        assertTrue(blocked.delete());
        assertEquals(1, store.retireConfirmedAddressHistoryAfterVerifiedRemoteCleanup(
                address.id(), java.util.Collections.singletonList(work), preparer));
        assertNull(store.getById(old.id()));
        assertEquals(1, store.beginCapture(address, work).captureSequence());
    }

    private static PendingPhotoStore store(File root, String id, long time) {
        return new PendingPhotoStore(root, () -> id, () -> time);
    }

    private static void writeImageBytes(File file, String content) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
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
