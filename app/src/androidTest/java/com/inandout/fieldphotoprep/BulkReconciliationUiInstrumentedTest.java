package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
public final class BulkReconciliationUiInstrumentedTest {
    @Test
    public void uncertainBacklogShowsOneBulkControlAndStoredReason() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();

        DriveFolder property = new DriveFolder("bulk-reconcile-property", "820_META_TEST");
        DriveFolder workOrder = new DriveFolder(
                "bulk-reconcile-work",
                "Full Property Condition - 2026-09-15");
        FolderPrefs prefs = new FolderPrefs(context);
        prefs.setCurrentAddress(property);
        prefs.setCurrentWorkOrder(workOrder);

        File fixtures = new File(context.getCacheDir(), "bulk-reconcile-ui-" + UUID.randomUUID());
        PendingPhotoStore store = new PendingPhotoStore(new File(fixtures, "pending"));
        PhotoPreparer preparer = new PhotoPreparer(new File(fixtures, "prepared"));

        PendingPhotoRecord photo = store.beginCapture(property, workOrder);
        writeJpeg(store.imageFile(photo));
        photo = store.finishCaptureIfImageExists(photo.id());
        preparer.prepare(store, photo);
        store.beginUploadAttempt(photo.id());
        String reason = "Drive verification readback did not settle.";
        PendingPhotoRecord uncertain = store.markUploadUncertain(photo.id(), reason);

        Intent intent = new Intent(context, PhotoCaptureActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<PhotoCaptureActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    setField(activity, "photoStore", store);
                    setField(activity, "photoPreparer", preparer);
                    call(activity, "refreshPhotoList");

                    Button reconcileAll = activity.findViewById(R.id.photos_reconcile_all);
                    assertEquals(View.VISIBLE, reconcileAll.getVisibility());
                    assertTrue(reconcileAll.isEnabled());
                    assertEquals("Reconcile All Uncertain (1)", reconcileAll.getText().toString());

                    setField(activity, "selectedPhotoId", uncertain.id());
                    call(activity, "renderSelectedPhoto");
                    TextView details = activity.findViewById(R.id.photos_prepared_text);
                    assertTrue(details.getText().toString().contains(reason));
                    assertTrue(details.getText().toString().contains("Reconcile before retry"));
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
        } finally {
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
            delete(fixtures);
        }
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
