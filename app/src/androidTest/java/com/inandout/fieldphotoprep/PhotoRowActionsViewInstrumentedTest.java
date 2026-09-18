package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
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
public final class PhotoRowActionsViewInstrumentedTest {
    @Test
    public void photoActionsControlSelectsExactPhotoAndUsesHiddenExistingActionOwners() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();

        DriveFolder property = new DriveFolder("actions-property", "101_TEST_ST");
        DriveFolder workOrder = new DriveFolder("actions-work", "Inspection - 2026-09-13");
        FolderPrefs prefs = new FolderPrefs(context);
        prefs.setCurrentAddress(property);
        prefs.setCurrentWorkOrder(workOrder);

        File fixtures = new File(context.getCacheDir(), "photo-row-actions-" + UUID.randomUUID());
        PendingPhotoStore store = new PendingPhotoStore(new File(fixtures, "pending"));
        PhotoPreparer preparer = new PhotoPreparer(new File(fixtures, "prepared"));
        PendingPhotoRecord photo = store.beginCapture(property, workOrder);
        writeJpeg(store.imageFile(photo));
        photo = store.finishCaptureIfImageExists(photo.id());
        preparer.prepare(store, photo);
        PendingPhotoRecord expected = store.getById(photo.id());

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
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            scenario.onActivity(activity -> {
                try {
                    ListView list = activity.findViewById(R.id.photos_pending_list);
                    assertEquals(2, list.getAdapter().getCount()); // selected-photo header + one row
                    View row = firstVisiblePhotoRow(list);
                    assertNotNull(row);
                    View actions = row.findViewById(R.id.photo_row_actions);
                    assertNotNull(actions);
                    assertTrue(actions.isClickable());
                    assertEquals("Photo actions", actions.getContentDescription().toString());
                    assertTrue(actions.performClick());

                    assertEquals(expected.id(), field(activity, "selectedPhotoId"));

                    View detailsPanel = (View) field(activity, "selectedActionsPanel");
                    assertEquals(View.VISIBLE, detailsPanel.getVisibility());

                    Button uploadOwner = activity.findViewById(R.id.photos_upload_one);
                    assertEquals(View.GONE, uploadOwner.getVisibility());
                    assertTrue("Prepared waiting photo should retain the existing upload owner state",
                            uploadOwner.isEnabled());
                    assertEquals(View.GONE,
                            activity.findViewById(R.id.photos_prepare).getVisibility());
                    assertEquals(View.GONE,
                            activity.findViewById(R.id.photos_discard).getVisibility());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
        } finally {
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
            delete(fixtures);
        }
    }

    private static View firstVisiblePhotoRow(ListView list) {
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            if (child != null && child.findViewById(R.id.photo_row_actions) != null) {
                return child;
            }
        }
        return null;
    }

    private static void writeJpeg(File file) throws Exception {
        Bitmap image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.LTGRAY);
        try (FileOutputStream output = new FileOutputStream(file)) {
            assertTrue(image.compress(Bitmap.CompressFormat.JPEG, 90, output));
        } finally {
            image.recycle();
        }
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
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
