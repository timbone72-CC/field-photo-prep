package com.inandout.fieldphotoprep;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.exifinterface.media.ExifInterface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public final class PhotoPreparerInstrumentedTest {
    private File testRoot;
    private PendingPhotoStore pendingStore;
    private PhotoPreparer preparer;
    private DriveFolder address;
    private DriveFolder workOrder;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        testRoot = new File(context.getCacheDir(), "phase6a-" + UUID.randomUUID());
        assertTrue(testRoot.mkdirs());
        pendingStore = new PendingPhotoStore(new File(testRoot, "pending"));
        preparer = new PhotoPreparer(new File(testRoot, "prepared"));
        address = new DriveFolder("address-stable-id", "FIELD PHOTO PREP TEST");
        workOrder = new DriveFolder("work-order-stable-id", "Cut Grass - 2026-09-20");
    }

    @After
    public void tearDown() throws Exception {
        deleteRecursively(testRoot);
    }

    @Test
    public void preparesLargeJpegWithoutChangingProtectedOriginal() throws Exception {
        PendingPhotoRecord record = pendingStore.beginCapture(address, workOrder);
        File original = pendingStore.imageFile(record);
        writeTestJpeg(original, 2200, 1100);
        record = pendingStore.finishCaptureIfImageExists(record.id());
        assertNotNull(record);

        byte[] before = Files.readAllBytes(original.toPath());
        PreparedPhotoResult result = preparer.prepare(pendingStore, record);
        byte[] after = Files.readAllBytes(original.toPath());

        assertArrayEquals(before, after);
        assertEquals(record.id(), result.photoId());
        assertEquals(2048, result.width());
        assertEquals(1024, result.height());
        assertTrue(result.file().isFile());
        assertTrue(result.file().length() > 0);

        Bitmap prepared = BitmapFactory.decodeFile(result.file().getAbsolutePath());
        assertNotNull(prepared);
        assertEquals(2048, prepared.getWidth());
        assertEquals(1024, prepared.getHeight());
        prepared.recycle();

        PreparedPhotoResult repeated = preparer.prepare(pendingStore, record);
        assertEquals(result.file().getCanonicalPath(), repeated.file().getCanonicalPath());
        assertArrayEquals(before, Files.readAllBytes(original.toPath()));
    }

    @Test
    public void exifRotationIsAppliedToPreparedPixels() throws Exception {
        PendingPhotoRecord record = pendingStore.beginCapture(address, workOrder);
        File original = pendingStore.imageFile(record);
        writeOrientationJpeg(original);

        ExifInterface sourceExif = new ExifInterface(original);
        sourceExif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                Integer.toString(ExifInterface.ORIENTATION_ROTATE_90));
        sourceExif.saveAttributes();

        record = pendingStore.finishCaptureIfImageExists(record.id());
        assertNotNull(record);
        byte[] before = Files.readAllBytes(original.toPath());

        PreparedPhotoResult result = preparer.prepare(pendingStore, record);

        assertArrayEquals(before, Files.readAllBytes(original.toPath()));
        assertEquals(80, result.width());
        assertEquals(120, result.height());

        Bitmap prepared = BitmapFactory.decodeFile(result.file().getAbsolutePath());
        assertNotNull(prepared);
        assertEquals(80, prepared.getWidth());
        assertEquals(120, prepared.getHeight());
        prepared.recycle();

        ExifInterface preparedExif = new ExifInterface(result.file());
        assertEquals(0, preparedExif.getRotationDegrees());
        assertFalse(preparedExif.isFlipped());
    }

    @Test
    public void missingOriginalFailsWithoutRemovingPhotoMetadata() throws Exception {
        PendingPhotoRecord record = pendingStore.beginCapture(address, workOrder);
        File original = pendingStore.imageFile(record);
        writeTestJpeg(original, 100, 80);
        record = pendingStore.finishCaptureIfImageExists(record.id());
        assertNotNull(record);
        String id = record.id();
        assertTrue(original.delete());

        try {
            preparer.prepare(pendingStore, record);
            fail("Expected preparation to fail when the protected original is missing.");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("missing or empty"));
        }

        PendingPhotoRecord stillRecorded = pendingStore.getById(id);
        assertNotNull(stillRecorded);
        assertEquals(PendingPhotoRecord.State.WAITING, stillRecorded.state());
        assertFalse(preparer.preparedFile(id).exists());
    }

    private static void writeTestJpeg(File file, int width, int height) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.rgb(40, 120, 200));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setColor(Color.rgb(220, 180, 60));
        canvas.drawRect(width / 2f, 0, width, height, paint);
        try (FileOutputStream output = new FileOutputStream(file)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 96, output));
            output.flush();
            output.getFD().sync();
        } finally {
            bitmap.recycle();
        }
    }

    private static void writeOrientationJpeg(File file) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        canvas.drawRect(0, 0, 60, 80, paint);
        paint.setColor(Color.BLUE);
        canvas.drawRect(60, 0, 120, 80, paint);
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
