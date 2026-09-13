package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class LocalPhotoDiscardCoordinatorTest {
    private static final String ID1 = "11111111-1111-4111-8111-111111111111";
    private static final String ID2 = "22222222-2222-4222-8222-222222222222";
    private static final String ID3 = "33333333-3333-4333-8333-333333333333";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void discardBatchRemovesOnlySelectedLocalPhotosAndPreparedCopies() throws Exception {
        File root = temporaryFolder.newFolder("batch-discard");
        PendingPhotoStore store = new PendingPhotoStore(
                new File(root, "pending"),
                new SequenceIds(ID1, ID2, ID3),
                () -> 1_700_000_000_000L);
        PhotoPreparer preparer = new PhotoPreparer(new File(root, "prepared"));
        DriveFolder address = new DriveFolder("address-id", "101 Test St");
        DriveFolder workOrder = new DriveFolder("work-id", "Inspection - 2026-09-13");

        PendingPhotoRecord first = waitingPhoto(store, preparer, address, workOrder, "first");
        PendingPhotoRecord second = waitingPhoto(store, preparer, address, workOrder, "second");
        PendingPhotoRecord keep = waitingPhoto(store, preparer, address, workOrder, "keep");

        LocalPhotoDiscardCoordinator coordinator =
                new LocalPhotoDiscardCoordinator(store, preparer);
        LocalPhotoDiscardCoordinator.BatchResult result = coordinator.discardBatch(
                List.of(first.id(), second.id()), address.id(), workOrder.id());

        assertTrue(result.complete());
        assertEquals(2, result.selectedCount());
        assertEquals(2, result.discardedCount());
        assertNull(store.getById(first.id()));
        assertNull(store.getById(second.id()));
        assertFalse(preparer.preparedFile(first.id()).exists());
        assertFalse(preparer.preparedFile(second.id()).exists());

        PendingPhotoRecord remaining = store.getById(keep.id());
        assertNotNull(remaining);
        assertTrue(store.hasImageData(remaining));
        assertTrue(preparer.preparedFile(keep.id()).isFile());
    }

    @Test
    public void unsafeSelectedPhotoAbortsBeforeAnyLocalDeletion() throws Exception {
        File root = temporaryFolder.newFolder("batch-discard-unsafe");
        PendingPhotoStore store = new PendingPhotoStore(
                new File(root, "pending"),
                new SequenceIds(ID1, ID2),
                () -> 1_700_000_000_000L);
        PhotoPreparer preparer = new PhotoPreparer(new File(root, "prepared"));
        DriveFolder address = new DriveFolder("address-id", "101 Test St");
        DriveFolder workOrder = new DriveFolder("work-id", "Inspection - 2026-09-13");

        PendingPhotoRecord safe = waitingPhoto(store, preparer, address, workOrder, "safe");
        PendingPhotoRecord uncertain = waitingPhoto(store, preparer, address, workOrder, "uncertain");
        store.beginUploadAttempt(uncertain.id());
        store.markUploadUncertain(uncertain.id(), "Synthetic uncertain result");

        LocalPhotoDiscardCoordinator coordinator =
                new LocalPhotoDiscardCoordinator(store, preparer);
        LocalPhotoDiscardCoordinator.BatchResult result = coordinator.discardBatch(
                List.of(safe.id(), uncertain.id()), address.id(), workOrder.id());

        assertFalse(result.complete());
        assertEquals(0, result.discardedCount());
        assertNotNull(result.failureDetail());
        assertNotNull(store.getById(safe.id()));
        assertNotNull(store.getById(uncertain.id()));
        assertTrue(store.hasImageData(store.getById(safe.id())));
        assertTrue(store.hasImageData(store.getById(uncertain.id())));
        assertTrue(preparer.preparedFile(safe.id()).isFile());
        assertTrue(preparer.preparedFile(uncertain.id()).isFile());
    }

    @Test
    public void wrongWorkOrderOrDuplicateSelectionFailsClosedBeforeDeletion() throws Exception {
        File root = temporaryFolder.newFolder("batch-discard-binding");
        PendingPhotoStore store = new PendingPhotoStore(
                new File(root, "pending"),
                new SequenceIds(ID1),
                () -> 1_700_000_000_000L);
        PhotoPreparer preparer = new PhotoPreparer(new File(root, "prepared"));
        DriveFolder address = new DriveFolder("address-id", "101 Test St");
        DriveFolder workOrder = new DriveFolder("work-id", "Inspection - 2026-09-13");
        PendingPhotoRecord photo = waitingPhoto(store, preparer, address, workOrder, "photo");

        LocalPhotoDiscardCoordinator coordinator =
                new LocalPhotoDiscardCoordinator(store, preparer);
        LocalPhotoDiscardCoordinator.BatchResult wrongWorkOrder = coordinator.discardBatch(
                List.of(photo.id()), address.id(), "different-work-id");
        LocalPhotoDiscardCoordinator.BatchResult duplicate = coordinator.discardBatch(
                List.of(photo.id(), photo.id()), address.id(), workOrder.id());

        assertFalse(wrongWorkOrder.complete());
        assertEquals(0, wrongWorkOrder.discardedCount());
        assertFalse(duplicate.complete());
        assertEquals(0, duplicate.discardedCount());
        assertNotNull(store.getById(photo.id()));
        assertTrue(store.hasImageData(store.getById(photo.id())));
        assertTrue(preparer.preparedFile(photo.id()).isFile());
    }

    @Test
    public void retrySafeFailedPhotoRemainsEligibleForExplicitLocalDiscard() throws Exception {
        File root = temporaryFolder.newFolder("batch-discard-failed");
        PendingPhotoStore store = new PendingPhotoStore(
                new File(root, "pending"),
                new SequenceIds(ID1),
                () -> 1_700_000_000_000L);
        PhotoPreparer preparer = new PhotoPreparer(new File(root, "prepared"));
        DriveFolder address = new DriveFolder("address-id", "101 Test St");
        DriveFolder workOrder = new DriveFolder("work-id", "Inspection - 2026-09-13");
        PendingPhotoRecord photo = waitingPhoto(store, preparer, address, workOrder, "failed");
        store.beginUploadAttempt(photo.id());
        store.markUploadFailed(photo.id(), "Known retry-safe failure");

        LocalPhotoDiscardCoordinator coordinator =
                new LocalPhotoDiscardCoordinator(store, preparer);
        coordinator.discardOne(photo.id(), address.id(), workOrder.id());

        assertNull(store.getById(photo.id()));
        assertFalse(preparer.preparedFile(photo.id()).exists());
    }

    private static PendingPhotoRecord waitingPhoto(
            PendingPhotoStore store,
            PhotoPreparer preparer,
            DriveFolder address,
            DriveFolder workOrder,
            String content) throws Exception {
        PendingPhotoRecord record = store.beginCapture(address, workOrder);
        write(store.imageFile(record), content + "-original");
        record = store.finishCaptureIfImageExists(record.id());
        write(preparer.preparedFile(record.id()), content + "-prepared");
        return store.getById(record.id());
    }

    private static void write(File file, String content) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            assertTrue(parent.mkdirs());
        }
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
