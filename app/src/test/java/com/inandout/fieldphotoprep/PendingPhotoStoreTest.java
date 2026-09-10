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
