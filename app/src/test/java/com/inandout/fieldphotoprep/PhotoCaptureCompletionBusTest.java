package com.inandout.fieldphotoprep;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class PhotoCaptureCompletionBusTest {
    private static final String PHOTO_ID = "11111111-1111-4111-8111-111111111111";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void cleanupBus() {
        PhotoCaptureCompletionBus.clearListenerForTests();
    }

    @Test
    public void successfulCapturePublishesAfterWaitingStateIsDurable() throws Exception {
        PendingPhotoStore store = newStore("publish-success");
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> receivedId = new AtomicReference<>();
        PhotoCaptureCompletionBus.setListener(photoId -> {
            calls.incrementAndGet();
            receivedId.set(photoId);
        });

        PendingPhotoRecord record = beginAndWrite(store, "photo-bytes");
        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(record.id());

        assertEquals(PendingPhotoRecord.State.WAITING, waiting.state());
        assertEquals(PendingPhotoRecord.State.WAITING, store.getById(PHOTO_ID).state());
        assertEquals(1, calls.get());
        assertEquals(PHOTO_ID, receivedId.get());
    }

    @Test
    public void emptyCapturePublishesNothing() throws Exception {
        PendingPhotoStore store = newStore("publish-empty");
        AtomicInteger calls = new AtomicInteger();
        PhotoCaptureCompletionBus.setListener(photoId -> calls.incrementAndGet());

        PendingPhotoRecord record = store.beginCapture(address(), workOrder());
        PendingPhotoRecord result = store.finishCaptureIfImageExists(record.id());

        assertNull(result);
        assertNull(store.getById(PHOTO_ID));
        assertEquals(0, calls.get());
    }

    @Test
    public void brokenListenerCannotInvalidateDurableCapture() throws Exception {
        PendingPhotoStore store = newStore("publish-listener-failure");
        PhotoCaptureCompletionBus.setListener(photoId -> {
            throw new IllegalStateException("scheduler unavailable");
        });

        PendingPhotoRecord record = beginAndWrite(store, "protected-photo");
        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(record.id());

        assertEquals(PendingPhotoRecord.State.WAITING, waiting.state());
        assertEquals(PendingPhotoRecord.State.WAITING, store.getById(PHOTO_ID).state());
    }

    private PendingPhotoStore newStore(String folderName) throws Exception {
        File root = temporaryFolder.newFolder(folderName);
        return new PendingPhotoStore(root, () -> PHOTO_ID, () -> 1_700_000_000_000L);
    }

    private PendingPhotoRecord beginAndWrite(PendingPhotoStore store, String bytes) throws Exception {
        PendingPhotoRecord record = store.beginCapture(address(), workOrder());
        try (FileOutputStream output = new FileOutputStream(store.imageFile(record))) {
            output.write(bytes.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        return record;
    }

    private static DriveFolder address() {
        return new DriveFolder("address-id", "1607 Crestview Drive");
    }

    private static DriveFolder workOrder() {
        return new DriveFolder("work-id", "Cut Grass - 2026-09-21");
    }
}
