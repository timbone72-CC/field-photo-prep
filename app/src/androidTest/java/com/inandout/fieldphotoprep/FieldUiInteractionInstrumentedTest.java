package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Focused interaction coverage for the remaining Concept 3 field controls.
 *
 * These tests deliberately avoid real Drive, camera, upload, and destructive paths. Existing
 * owner tests continue to cover those behaviors.
 */
@RunWith(AndroidJUnit4.class)
public final class FieldUiInteractionInstrumentedTest {
    @Test
    public void propertyRowBackAndBottomTabsUseProductionNavigation() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();

        DriveFolder property = new DriveFolder("interaction-property", "1607_CRESTVIEW_DR_CORDELL_OK");
        DriveFolder workOrder = new DriveFolder("interaction-work", "Pressure Test - 2026-09-13");
        FolderPrefs prefs = new FolderPrefs(context);
        prefs.setCurrentAddress(property);
        prefs.setCurrentWorkOrder(workOrder);

        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    bindSingleProperty(activity, property);
                    ListView propertyList = activity.findViewById(R.id.home_property_list);
                    assertTrue("Property row handler must accept the tapped property",
                            propertyList.performItemClick(null, 0, 0));
                    assertEquals(View.GONE, activity.findViewById(R.id.home_root).getVisibility());
                    assertEquals(View.VISIBLE, activity.findViewById(R.id.work_orders_root).getVisibility());
                    assertEquals(PropertyDisplayName.fromDriveFolderName(property.name()),
                            ((TextView) activity.findViewById(R.id.work_order_address)).getText().toString());

                    assertTrue(activity.findViewById(R.id.work_order_back).performClick());
                    assertEquals(View.VISIBLE, activity.findViewById(R.id.home_root).getVisibility());

                    bindSingleProperty(activity, property);
                    View homeRoot = activity.findViewById(R.id.home_root);
                    assertTrue(homeRoot.findViewById(R.id.nav_work_orders).performClick());
                    assertEquals(View.VISIBLE, activity.findViewById(R.id.work_orders_root).getVisibility());

                    View workRoot = activity.findViewById(R.id.work_orders_root);
                    assertTrue(workRoot.findViewById(R.id.nav_home).performClick());
                    assertEquals(View.VISIBLE, activity.findViewById(R.id.home_root).getVisibility());

                    bindSingleProperty(activity, property);
                    assertTrue(activity.findViewById(R.id.home_root)
                            .findViewById(R.id.nav_work_orders).performClick());
                    assertTrue(activity.findViewById(R.id.work_orders_root)
                            .findViewById(R.id.nav_photos).performClick());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });

            PhotoCaptureActivity photos = awaitResumed(PhotoCaptureActivity.class);
            assertNotNull("Work Orders -> Photos must reach the real Photos activity", photos);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertTrue(photos.findViewById(R.id.nav_work_orders).performClick());
            });

            MainActivity resumedMain = awaitResumed(MainActivity.class);
            assertNotNull("Photos -> Work Orders must return to MainActivity", resumedMain);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    assertEquals(View.VISIBLE,
                            resumedMain.findViewById(R.id.work_orders_root).getVisibility()));
        } finally {
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    @Test
    public void selectAllReadyAndClearOnlyAffectEligiblePhotos() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();

        DriveFolder property = new DriveFolder("batch-property", "101_TEST_ST");
        DriveFolder workOrder = new DriveFolder("batch-work", "Inspection - 2026-09-13");
        FolderPrefs prefs = new FolderPrefs(context);
        prefs.setCurrentAddress(property);
        prefs.setCurrentWorkOrder(workOrder);

        File fixtures = new File(context.getCacheDir(), "field-ui-interactions-" + UUID.randomUUID());
        PendingPhotoStore store = new PendingPhotoStore(new File(fixtures, "pending"));
        PhotoPreparer preparer = new PhotoPreparer(new File(fixtures, "prepared"));

        PendingPhotoRecord readyOne = createPreparedPhoto(store, preparer, property, workOrder);
        PendingPhotoRecord readyTwo = createPreparedPhoto(store, preparer, property, workOrder);
        PendingPhotoRecord uncertain = createPreparedPhoto(store, preparer, property, workOrder);
        store.beginUploadAttempt(uncertain.id());
        store.markUploadUncertain(uncertain.id(), "Test uncertain result");
        uncertain = store.getById(uncertain.id());
        PendingPhotoRecord uploaded = createPreparedPhoto(store, preparer, property, workOrder);
        store.beginUploadAttempt(uploaded.id());
        store.markUploadConfirmed(uploaded.id(), "test-remote-photo");
        uploaded = store.getById(uploaded.id());

        List<PendingPhotoRecord> records = List.of(readyOne, readyTwo, uncertain, uploaded);
        Intent intent = new Intent(context, PhotoCaptureActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<PhotoCaptureActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    setField(activity, "photoStore", store);
                    setField(activity, "photoPreparer", preparer);
                    call(activity, "renderPhotoList",
                            new Class<?>[]{List.class, List.class},
                            records, new ArrayList<String>());

                    Button selectAll = activity.findViewById(R.id.photos_select_all);
                    Button clear = activity.findViewById(R.id.photos_clear_selection);
                    Button uploadSelected = activity.findViewById(R.id.photos_upload_selected);
                    assertTrue(selectAll.isEnabled());
                    assertTrue(selectAll.performClick());

                    assertEquals("Upload Selected (2)", uploadSelected.getText().toString());
                    assertTrue(clear.isEnabled());

                    ListView list = activity.findViewById(R.id.photos_pending_list);
                    assertEquals(5, list.getAdapter().getCount()); // header + four photo rows
                    assertTrue(((CheckBox) photoRow(list, 0)
                            .findViewById(R.id.photo_row_check)).isChecked());
                    assertTrue(((CheckBox) photoRow(list, 1)
                            .findViewById(R.id.photo_row_check)).isChecked());

                    CheckBox uncertainCheck = photoRow(list, 2)
                            .findViewById(R.id.photo_row_check);
                    assertFalse(uncertainCheck.isChecked());
                    assertFalse(uncertainCheck.isEnabled());

                    CheckBox uploadedCheck = photoRow(list, 3)
                            .findViewById(R.id.photo_row_check);
                    assertFalse(uploadedCheck.isChecked());
                    assertEquals(View.INVISIBLE, uploadedCheck.getVisibility());

                    assertTrue(clear.performClick());
                    assertEquals("Upload Selected (0)", uploadSelected.getText().toString());
                    assertFalse(clear.isEnabled());
                    assertFalse(((CheckBox) photoRow(list, 0)
                            .findViewById(R.id.photo_row_check)).isChecked());
                    assertFalse(((CheckBox) photoRow(list, 1)
                            .findViewById(R.id.photo_row_check)).isChecked());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
        } finally {
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
            delete(fixtures);
        }
    }

    private static View photoRow(ListView list, int photoIndex) {
        return list.getAdapter().getView(photoIndex + 1, null, list);
    }

    private static void bindSingleProperty(MainActivity activity, DriveFolder property) throws Exception {
        @SuppressWarnings("unchecked")
        List<DriveFolder> visible = (List<DriveFolder>) field(activity, "visibleFolders");
        visible.clear();
        visible.add(property);
        call(activity, "notifyFolderAdapters", new Class<?>[]{});
        activity.findViewById(R.id.home_property_list).setEnabled(true);
    }

    private static PendingPhotoRecord createPreparedPhoto(
            PendingPhotoStore store,
            PhotoPreparer preparer,
            DriveFolder property,
            DriveFolder workOrder) throws Exception {
        PendingPhotoRecord record = store.beginCapture(property, workOrder);
        writeJpeg(store.imageFile(record));
        record = store.finishCaptureIfImageExists(record.id());
        preparer.prepare(store, record);
        return store.getById(record.id());
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

    private static <T extends Activity> T awaitResumed(Class<T> type) throws Exception {
        Activity[] found = new Activity[1];
        for (int attempt = 0; attempt < 100; attempt++) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)) {
                    if (type.isInstance(activity)) {
                        found[0] = activity;
                    }
                }
            });
            if (found[0] != null) {
                return type.cast(found[0]);
            }
            Thread.sleep(50);
        }
        return null;
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

    private static void call(Object owner, String name, Class<?>[] types, Object... values)
            throws Exception {
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
