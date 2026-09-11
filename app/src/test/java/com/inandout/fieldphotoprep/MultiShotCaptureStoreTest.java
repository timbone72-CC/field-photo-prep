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
import static org.junit.Assert.assertTrue;

public final class MultiShotCaptureStoreTest {
    private static final String ID1 = "11111111-1111-4111-8111-111111111111";
    private static final String ID2 = "22222222-2222-4222-8222-222222222222";
    private static final String ID3 = "33333333-3333-4333-8333-333333333333";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void repeatedSessionCapturesStayDistinctAndKeepExactDestination() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        SequenceIds ids = new SequenceIds(ID1, ID2, ID3);
        PendingPhotoStore store = new PendingPhotoStore(root, ids, () -> 1_700_000_000_000L);
        DriveFolder address = new DriveFolder("address-id", "1607 Crestview Drive");
        DriveFolder workOrder = new DriveFolder("work-id", "Cut Grass - 2026-09-21");

        PendingPhotoRecord first = capture(store, address, workOrder, "photo-one");
        PendingPhotoRecord second = capture(store, address, workOrder, "photo-two");
        PendingPhotoRecord third = capture(store, address, workOrder, "photo-three");

        List<PendingPhotoRecord> records = store.recordsForWorkOrder("work-id");

        assertEquals(3, records.size());
        assertFalse(first.id().equals(second.id()));
        assertFalse(second.id().equals(third.id()));
        assertFalse(first.imageFileName().equals(second.imageFileName()));
        assertFalse(second.imageFileName().equals(third.imageFileName()));

        for (PendingPhotoRecord record : records) {
            assertEquals(PendingPhotoRecord.State.WAITING, record.state());
            assertEquals("address-id", record.addressId());
            assertEquals("1607 Crestview Drive", record.addressName());
            assertEquals("work-id", record.workOrderId());
            assertEquals("Cut Grass - 2026-09-21", record.workOrderName());
            assertTrue(store.hasImageData(record));
        }
    }

    private static PendingPhotoRecord capture(
            PendingPhotoStore store,
            DriveFolder address,
            DriveFolder workOrder,
            String bytes) throws Exception {
        PendingPhotoRecord record = store.beginCapture(address, workOrder);
        try (FileOutputStream output = new FileOutputStream(store.imageFile(record))) {
            output.write(bytes.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(record.id());
        assertTrue(waiting != null);
        return waiting;
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
