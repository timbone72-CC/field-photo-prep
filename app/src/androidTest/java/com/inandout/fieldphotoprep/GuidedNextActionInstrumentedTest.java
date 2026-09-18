package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public final class GuidedNextActionInstrumentedTest {
    @Test
    public void preparedPhotosGuideIntoExistingSelectThenUploadOwner() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();

        DriveFolder property = new DriveFolder("guide-property", "101_TEST_ST");
        DriveFolder workOrder = new DriveFolder("guide-work", "Inspection - 2026-09-17");
        FolderPrefs prefs = new FolderPrefs(context);
        prefs.setCurrentAddress(property);
        prefs.setCurrentWorkOrder(workOrder);

        File fixtures = new File(context.getCacheDir(), "guided-next-" + UUID.randomUUID());
        PendingPhotoStore store = new PendingPhotoStore(new File(fixtures, "pending"));
        PhotoPreparer preparer = new PhotoPreparer(new File(fixtures, "prepared"));

        createPreparedPhoto(store, preparer, property, workOrder);
        createPreparedPhoto(store, preparer, property, workOrder);

        Intent intent = new Intent(context, PhotoCaptureActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<PhotoCaptureActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    setField(activity, "photoStore", store);
                    setField(activity, "photoPreparer", preparer);
                    call(activity, "refreshPhotoList");

                    Button next = activity.findViewById(R.id.photos_next_action);
                    assertEquals("Next: Select 2 Ready Photos", next.getText().toString());
                    assertTrue(next.isEnabled());
                    assertTrue(next.performClick());

                    assertEquals("Upload Selected (2)",
                            ((Button) activity.findViewById(R.id.photos_upload_selected))
                                    .getText().toString());
                    assertEquals("Next: Upload Selected (2)", next.getText().toString());
                    assertTrue(next.isEnabled());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
        } finally {
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
            delete(fixtures);
        }
    }

    @Test
    public void workOrderSelectionTurnsExistingPhotosButtonIntoNextTakePhotos() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();

        DriveFolder property = new DriveFolder("guide-property-2", "102_TEST_ST");
        DriveFolder workOrder = new DriveFolder("guide-work-2", "Inspection - 2026-09-17");

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(
                new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK))) {
            scenario.onActivity(activity -> {
                try {
                    setField(activity, "selectedAddress", property);
                    setField(activity, "selectedWorkOrder", workOrder);
                    call(activity, "renderCurrentWorkOrder");

                    Button next = activity.findViewById(R.id.work_order_photos);
                    assertEquals("Next: Take Photos", next.getText().toString());
                    assertTrue(next.isEnabled());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
        } finally {
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    private static void createPreparedPhoto(
            PendingPhotoStore store,
            PhotoPreparer preparer,
            DriveFolder property,
            DriveFolder workOrder) throws Exception {
        PendingPhotoRecord record = store.beginCapture(property, workOrder);
        writeJpeg(store.imageFile(record));
        record = store.finishCaptureIfImageExists(record.id());
        preparer.prepare(store, record);
    }

    private static void writeJpeg(File file) throws Exception {
        Bitmap image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
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

    private static void call(Object owner, String name) throws Exception {
        Method method = owner.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(owner);
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
