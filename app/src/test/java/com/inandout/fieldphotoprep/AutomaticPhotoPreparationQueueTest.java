package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class AutomaticPhotoPreparationQueueTest {
    private static final String ID1 = "11111111-1111-4111-8111-111111111111";
    private static final String ID2 = "22222222-2222-4222-8222-222222222222";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void resetProcessState() {
        PhotoCaptureCompletionBus.clearListenerForTests();
        PhotoPreparationGate.resetForTests();
    }

    @Test
    public void repeatedEnqueueIsDeduplicatedAndPhotosPrepareSerially() throws Exception {
        PendingPhotoStore store = newStore(ID1, ID2);
        PendingPhotoRecord first = capture(store, "first-photo");
        PendingPhotoRecord second = capture(store, "second-photo");
        FakePreparationOperation operation = new FakePreparationOperation();
        AutomaticPhotoPreparationQueue queue = new AutomaticPhotoPreparationQueue(
                store,
                new PhotoPreparationGate(),
                operation);

        queue.enqueue(first.id());
        queue.enqueue(first.id());
        queue.enqueue(second.id());

        assertTrue(queue.awaitIdleForTests(5_000L));
        assertEquals(List.of(ID1, ID2), operation.preparedOrder());
        assertEquals(1, operation.maxConcurrent());
        assertEquals(0, queue.queuedCount());
        assertFalse(new PhotoPreparationGate().isBusy());
    }

    @Test
    public void backlogScanQueuesOnlyWaitingPhotosMissingPreparedCopy() throws Exception {
        PendingPhotoStore store = newStore(ID1, ID2);
        capture(store, "first-photo");
        capture(store, "second-photo");
        FakePreparationOperation operation = new FakePreparationOperation();
        operation.markAlreadyPrepared(ID1);
        AutomaticPhotoPreparationQueue queue = new AutomaticPhotoPreparationQueue(
                store,
                new PhotoPreparationGate(),
                operation);

        queue.enqueueEligibleWaitingPhotos();

        assertTrue(queue.awaitIdleForTests(5_000L));
        assertEquals(List.of(ID2), operation.preparedOrder());
    }

    @Test
    public void preparationFailureKeepsWaitingOriginalRecoverable() throws Exception {
        PendingPhotoStore store = newStore(ID1);
        PendingPhotoRecord waiting = capture(store, "protected-original");
        AutomaticPhotoPreparationQueue.PreparationOperation failing =
                new AutomaticPhotoPreparationQueue.PreparationOperation() {
                    @Override
                    public boolean hasPreparedCopy(String photoId) {
                        return false;
                    }

                    @Override
                    public void prepare(PendingPhotoRecord record) {
                        throw new IllegalStateException("synthetic preparation failure");
                    }
                };
        AutomaticPhotoPreparationQueue queue = new AutomaticPhotoPreparationQueue(
                store,
                new PhotoPreparationGate(),
                failing);

        queue.enqueue(waiting.id());

        assertTrue(queue.awaitIdleForTests(5_000L));
        PendingPhotoRecord after = store.getById(waiting.id());
        assertNotNull(after);
        assertEquals(PendingPhotoRecord.State.WAITING, after.state());
        assertTrue(store.hasImageData(after));
        assertNotNull(queue.lastFailure(waiting.id()));
        assertFalse(new PhotoPreparationGate().isBusy());
    }

    private PendingPhotoStore newStore(String... ids) throws Exception {
        File root = temporaryFolder.newFolder("pending-" + System.nanoTime());
        SequenceIds sequence = new SequenceIds(ids);
        return new PendingPhotoStore(root, sequence, () -> 1_700_000_000_000L);
    }

    private PendingPhotoRecord capture(PendingPhotoStore store, String bytes) throws Exception {
        PendingPhotoRecord record = store.beginCapture(
                new DriveFolder("address-id", "1607 Crestview Drive"),
                new DriveFolder("work-id", "Cut Grass - 2026-09-21"));
        try (FileOutputStream output = new FileOutputStream(store.imageFile(record))) {
            output.write(bytes.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(record.id());
        assertNotNull(waiting);
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

    private static final class FakePreparationOperation
            implements AutomaticPhotoPreparationQueue.PreparationOperation {
        private final List<String> order = Collections.synchronizedList(new ArrayList<>());
        private final Set<String> prepared = Collections.synchronizedSet(new HashSet<>());
        private final AtomicInteger concurrent = new AtomicInteger();
        private final AtomicInteger maxConcurrent = new AtomicInteger();

        @Override
        public boolean hasPreparedCopy(String photoId) {
            return prepared.contains(photoId);
        }

        @Override
        public void prepare(PendingPhotoRecord record) {
            int now = concurrent.incrementAndGet();
            maxConcurrent.accumulateAndGet(now, Math::max);
            try {
                try {
                    Thread.sleep(30L);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("test worker interrupted", error);
                }
                order.add(record.id());
                prepared.add(record.id());
            } finally {
                concurrent.decrementAndGet();
            }
        }

        void markAlreadyPrepared(String photoId) {
            prepared.add(photoId);
        }

        List<String> preparedOrder() {
            synchronized (order) {
                return new ArrayList<>(order);
            }
        }

        int maxConcurrent() {
            return maxConcurrent.get();
        }
    }
}
