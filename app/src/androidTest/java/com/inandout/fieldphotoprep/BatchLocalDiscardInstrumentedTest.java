package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

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
public final class BatchLocalDiscardInstrumentedTest {
    @Test
    public void generalSelectionDiscardsTwoSafeLocalPhotosAndKeepsUnselectedUncertainPhoto()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();

        DriveFolder property = new DriveFolder("discard-property", "101_TEST_ST");
        DriveFolder workOrder = new DriveFolder("discard-work", "Inspection - 2026-09-13");
        FolderPrefs prefs = new FolderPrefs(context);
        prefs.setCurrentAddress(property);
        prefs.setCurrentWorkOrder(workOrder);

        File fixtures = new File(context.getCacheDir(), "batch-local-discard-" + UUID.randomUUID());
        PendingPhotoStore store = new PendingPhotoStore(new File(fixtures, "pending"));
        PhotoPreparer preparer = new PhotoPreparer(new File(fixtures, "prepared"));

        PendingPhotoRecord ready = createWaitingPhoto(store, property, workOrder);
        preparer.prepare(store, ready);
        ready = store.getById(ready.id());

        PendingPhotoRecord unprepared = createWaitingPhoto(store, property, workOrder);

        PendingPhotoRecord uncertain = createWaitingPhoto(store, property, workOrder);
        preparer.prepare(store, uncertain);
        store.beginUploadAttempt(uncertain.id());
        store.markUploadUncertain(uncertain.id(), "Synthetic uncertain result");
        uncertain = store.getById(uncertain.id());

        List<PendingPhotoRecord> records = List.of(ready, unprepared, uncertain);
        String readyId = ready.id();
        String unpreparedId = unprepared.id();
        String uncertainId = uncertain.id();
        File readyPrepared = preparer.preparedFile(readyId);

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

                    LinearLayout list = activity.findViewById(R.id.photos_pending_list);
                    assertEquals(3, list.getChildCount());

                    CheckBox readyCheck = list.getChildAt(0).findViewById(R.id.photo_row_check);
                    CheckBox unpreparedCheck = list.getChildAt(1).findViewById(R.id.photo_row_check);
                    CheckBox uncertainCheck = list.getChildAt(2).findViewById(R.id.photo_row_check);
                    Button uploadSelected = activity.findViewById(R.id.photos_upload_selected);
                    Button discardSelected = activity.findViewById(R.id.photos_discard_selected);

                    assertTrue("Prepared WAITING photo must remain selectable", readyCheck.isEnabled());
                    assertTrue("Unprepared WAITING photo must be selectable for local discard",
                            unpreparedCheck.isEnabled());
                    assertFalse("UNCERTAIN photo must not be selectable for local discard or normal upload",
                            uncertainCheck.isEnabled());

                    readyCheck.setChecked(true);
                    unpreparedCheck.setChecked(true);

                    assertEquals("Upload Selected (2)", uploadSelected.getText().toString());
                    assertFalse("Mixed selection containing an unprepared photo must not upload",
                            uploadSelected.isEnabled());
                    assertEquals("Discard Selected (2)", discardSelected.getText().toString());
                    assertTrue("Two discard-safe WAITING photos should enable batch discard",
                            discardSelected.isEnabled());
                    assertTrue(discardSelected.performClick());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });

            Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
            AccessibilityNodeInfo root = awaitText(instrumentation, "Discard 2 selected photos?");
            assertNotNull("Batch discard must require one explicit confirmation", root);
            List<AccessibilityNodeInfo> confirmNodes =
                    root.findAccessibilityNodeInfosByText("Discard Selected");
            assertFalse("Confirmation must expose Discard Selected", confirmNodes.isEmpty());
            AccessibilityNodeInfo confirm = clickableAncestor(confirmNodes.get(0));
            assertNotNull(confirm);
            assertTrue(confirm.performAction(AccessibilityNodeInfo.ACTION_CLICK));
            instrumentation.waitForIdleSync();

            for (int attempt = 0; attempt < 40; attempt++) {
                if (store.getById(readyId) == null && store.getById(unpreparedId) == null) {
                    break;
                }
                Thread.sleep(50);
            }

            assertNull("Selected prepared WAITING photo must be removed locally",
                    store.getById(readyId));
            assertNull("Selected unprepared WAITING photo must be removed locally",
                    store.getById(unpreparedId));
            assertFalse("Selected prepared derivative must be removed", readyPrepared.exists());

            PendingPhotoRecord kept = store.getById(uncertainId);
            assertNotNull("Unselected UNCERTAIN photo must remain", kept);
            assertEquals(PendingPhotoRecord.State.UNCERTAIN, kept.state());
            assertTrue("Unselected UNCERTAIN protected original must remain",
                    store.hasImageData(kept));
        } finally {
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
            delete(fixtures);
        }
    }

    private static PendingPhotoRecord createWaitingPhoto(
            PendingPhotoStore store,
            DriveFolder property,
            DriveFolder workOrder) throws Exception {
        PendingPhotoRecord record = store.beginCapture(property, workOrder);
        writeJpeg(store.imageFile(record));
        record = store.finishCaptureIfImageExists(record.id());
        assertNotNull(record);
        return record;
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

    private static AccessibilityNodeInfo awaitText(
            Instrumentation instrumentation,
            String text) throws Exception {
        for (int attempt = 0; attempt < 40; attempt++) {
            instrumentation.waitForIdleSync();
            AccessibilityNodeInfo root = instrumentation.getUiAutomation().getRootInActiveWindow();
            if (root != null && !root.findAccessibilityNodeInfosByText(text).isEmpty()) {
                return root;
            }
            Thread.sleep(50);
        }
        return null;
    }

    private static AccessibilityNodeInfo clickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null && !current.isClickable()) {
            current = current.getParent();
        }
        return current;
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
