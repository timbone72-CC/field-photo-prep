package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public final class PhotoListVirtualizationInstrumentedTest {
    @Test
    public void oneHundredFiftyPhotoModelKeepsOnlyVisibleRowsInflated() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();

        DriveFolder property = new DriveFolder("scale-property", "101_TEST_ST");
        DriveFolder workOrder = new DriveFolder("scale-work", "Inspection - 2026-09-17");
        FolderPrefs prefs = new FolderPrefs(context);
        prefs.setCurrentAddress(property);
        prefs.setCurrentWorkOrder(workOrder);

        List<PendingPhotoRecord> records = new ArrayList<>();
        long baseTime = System.currentTimeMillis() - 150_000L;
        for (int i = 0; i < 150; i++) {
            String id = UUID.randomUUID().toString();
            records.add(new PendingPhotoRecord(
                    id,
                    PendingPhotoRecord.imageFileNameFor(id),
                    PendingPhotoRecord.State.WAITING,
                    baseTime + i,
                    property.id(),
                    property.name(),
                    workOrder.id(),
                    workOrder.name()));
        }

        Intent intent = new Intent(context, PhotoCaptureActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<PhotoCaptureActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    call(activity, "renderPhotoList",
                            new Class<?>[]{List.class, List.class},
                            records, new ArrayList<String>());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            scenario.onActivity(activity -> {
                ListView list = activity.findViewById(R.id.photos_pending_list);
                assertEquals("Header plus 150 photo rows must remain addressable through the adapter",
                        151, list.getAdapter().getCount());
                assertTrue("Virtualized list must have at least one visible child",
                        list.getChildCount() > 0);
                assertTrue("Virtualized list must not inflate all 150 photo rows at once",
                        list.getChildCount() < 40);
            });
        } finally {
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    @Test
    public void visibleThumbnailLoadsAfterRowBindingWithoutBlockingListCreation() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();

        DriveFolder property = new DriveFolder("thumb-property", "102_TEST_ST");
        DriveFolder workOrder = new DriveFolder("thumb-work", "Inspection - 2026-09-17");
        FolderPrefs prefs = new FolderPrefs(context);
        prefs.setCurrentAddress(property);
        prefs.setCurrentWorkOrder(workOrder);

        File fixtures = new File(context.getCacheDir(), "photo-list-thumb-" + UUID.randomUUID());
        PendingPhotoStore store = new PendingPhotoStore(new File(fixtures, "pending"));
        PhotoPreparer preparer = new PhotoPreparer(new File(fixtures, "prepared"));

        PendingPhotoRecord record = store.beginCapture(property, workOrder);
        writeJpeg(store.imageFile(record));
        record = store.finishCaptureIfImageExists(record.id());
        PendingPhotoRecord expected = record;

        Intent intent = new Intent(context, PhotoCaptureActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<PhotoCaptureActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    setField(activity, "photoStore", store);
                    setField(activity, "photoPreparer", preparer);
                    call(activity, "renderPhotoList",
                            new Class<?>[]{List.class, List.class},
                            List.of(expected), new ArrayList<String>());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });

            boolean[] loaded = {false};
            for (int attempt = 0; attempt < 80 && !loaded[0]; attempt++) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity -> {
                    ListView list = activity.findViewById(R.id.photos_pending_list);
                    View row = firstVisiblePhotoRow(list);
                    if (row != null) {
                        ImageView thumbnail = row.findViewById(R.id.photo_row_thumb);
                        loaded[0] = thumbnail != null && thumbnail.getDrawable() != null;
                    }
                });
                if (!loaded[0]) {
                    Thread.sleep(25);
                }
            }
            assertTrue("Background thumbnail decode must eventually populate the visible row", loaded[0]);
        } finally {
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
            delete(fixtures);
        }
    }

    private static View firstVisiblePhotoRow(ListView list) {
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            if (child != null && child.findViewById(R.id.photo_row_thumb) != null) {
                return child;
            }
        }
        return null;
    }

    private static void writeJpeg(File file) throws Exception {
        Bitmap image = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.LTGRAY);
        try (FileOutputStream output = new FileOutputStream(file)) {
            assertTrue(image.compress(Bitmap.CompressFormat.JPEG, 90, output));
            output.getFD().sync();
        } finally {
            image.recycle();
        }
    }

    private static void setField(Object owner, String name, Object value) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private static void call(
            Object owner,
            String name,
            Class<?>[] types,
            Object... values) throws Exception {
        Method method = owner.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(owner, values);
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                delete(child);
            }
        }
        file.delete();
    }
}
