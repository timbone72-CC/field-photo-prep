package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public final class AutomaticPhotoPreparationInstrumentedTest {
    private File testRoot;
    private PendingPhotoStore store;
    private PhotoPreparer preparer;
    private AutomaticPhotoPreparationQueue queue;
    private DriveFolder address;
    private DriveFolder workOrder;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        testRoot = new File(context.getCacheDir(), "auto-prepare-" + UUID.randomUUID());
        assertTrue(testRoot.mkdirs());
        store = new PendingPhotoStore(new File(testRoot, "pending"));
        preparer = new PhotoPreparer(new File(testRoot, "prepared"));
        PhotoPreparationGate.resetForTests();
        PhotoCaptureCompletionBus.clearListenerForTests();
        queue = new AutomaticPhotoPreparationQueue(store, preparer, new PhotoPreparationGate());
        PhotoCaptureCompletionBus.setListener(queue);
        address = new DriveFolder("address-stable-id", "FIELD PHOTO PREP TEST");
        workOrder = new DriveFolder("work-order-stable-id", "Cut Grass - 2026-09-20");
    }

    @After
    public void tearDown() throws Exception {
        PhotoCaptureCompletionBus.clearListenerForTests();
        PhotoPreparationGate.resetForTests();
        deleteRecursively(testRoot);
    }

    @Test
    public void waitingCaptureAutomaticallyCreatesSizedDerivativeAndPreservesOriginal() throws Exception {
        PendingPhotoRecord record = store.beginCapture(address, workOrder);
        File original = store.imageFile(record);
        writeTestJpeg(original, 2200, 1100, Color.rgb(40, 120, 200));
        byte[] originalBefore = Files.readAllBytes(original.toPath());

        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(record.id());
        assertNotNull(waiting);
        assertTrue(queue.awaitIdleForTests(15_000L));

        File prepared = preparer.preparedFile(waiting.id());
        assertTrue(prepared.isFile());
        assertTrue(prepared.length() > 0);
        assertArrayEquals(originalBefore, Files.readAllBytes(original.toPath()));
        assertEquals(PendingPhotoRecord.State.WAITING, store.getById(waiting.id()).state());

        Bitmap decoded = BitmapFactory.decodeFile(prepared.getAbsolutePath());
        assertNotNull(decoded);
        assertEquals(2048, decoded.getWidth());
        assertEquals(1024, decoded.getHeight());
        decoded.recycle();
    }

    @Test
    public void twoCaptureEventsPrepareTwoIndependentCopiesWithoutManualAction() throws Exception {
        PendingPhotoRecord first = capture(1800, 900, Color.RED);
        PendingPhotoRecord second = capture(1600, 1200, Color.BLUE);

        assertTrue(queue.awaitIdleForTests(15_000L));

        File firstPrepared = preparer.preparedFile(first.id());
        File secondPrepared = preparer.preparedFile(second.id());
        assertTrue(firstPrepared.isFile() && firstPrepared.length() > 0);
        assertTrue(secondPrepared.isFile() && secondPrepared.length() > 0);
        assertTrue(store.hasImageData(store.getById(first.id())));
        assertTrue(store.hasImageData(store.getById(second.id())));
    }

    private PendingPhotoRecord capture(int width, int height, int color) throws Exception {
        PendingPhotoRecord record = store.beginCapture(address, workOrder);
        writeTestJpeg(store.imageFile(record), width, height, color);
        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(record.id());
        assertNotNull(waiting);
        return waiting;
    }

    private static void writeTestJpeg(File file, int width, int height, int color) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0, 0, width, height, paint);
        paint.setColor(Color.WHITE);
        canvas.drawRect(width / 2f, height / 3f, width, height, paint);
        try (FileOutputStream output = new FileOutputStream(file)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 96, output));
            output.flush();
            output.getFD().sync();
        } finally {
            bitmap.recycle();
        }
    }

    private static void deleteRecursively(File file) throws Exception {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(file.toPath());
    }
}
