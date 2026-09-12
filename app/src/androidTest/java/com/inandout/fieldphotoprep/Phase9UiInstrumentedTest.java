package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(AndroidJUnit4.class)
public final class Phase9UiInstrumentedTest {
    @Test
    public void conceptThreePhotoScreenRendersRealThumbnailAndCoreWorkflowActions() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DriveFolder address = new DriveFolder("test-address-provider-id", "123 Test Street");
        DriveFolder workOrder = new DriveFolder(
                "test-work-order-provider-id", "Cut Grass - 2026-09-11");

        FolderPrefs prefs = new FolderPrefs(context);
        prefs.setCurrentAddress(address);
        prefs.setCurrentWorkOrder(workOrder);

        PendingPhotoStore store = new PendingPhotoStore(
                new File(context.getFilesDir(), "pending_photos"));
        PendingPhotoRecord record = store.beginCapture(address, workOrder);
        Bitmap bitmap = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888);
        try (FileOutputStream output = new FileOutputStream(store.imageFile(record))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output));
            output.flush();
        } finally {
            bitmap.recycle();
        }
        store.finishCaptureIfImageExists(record.id());

        try (ActivityScenario<PhotoCaptureActivity> scenario =
                     ActivityScenario.launch(PhotoCaptureActivity.class)) {
            scenario.onActivity(activity -> {
                View content = activity.findViewById(android.R.id.content);
                TextView title = findText(content, "Photos");
                TextView selectedWorkOrder = findText(content, "Cut Grass - 2026-09-11");
                Button camera = findButton(content, "Open Camera");
                Button upload = findButtonStartingWith(content, "Upload Selected (");
                ImageView thumbnail = findImageWithDescription(content, "Photo thumbnail");

                assertNotNull("Concept 3 Photos title should be visible", title);
                assertNotNull("selected work order should remain visible", selectedWorkOrder);
                assertNotNull("camera action should be presented as Open Camera", camera);
                assertNotNull("batch upload action should remain present", upload);
                assertTrue("camera action remains enabled with a selected work order", camera.isEnabled());
                assertNotNull("a local field photo should render a thumbnail view", thumbnail);
                assertNotNull("thumbnail view should contain an image", thumbnail.getDrawable());
            });
        }
    }

    private static TextView findText(View root, String value) {
        if (root instanceof TextView && value.contentEquals(((TextView) root).getText())) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findText(group.getChildAt(i), value);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Button findButton(View root, String value) {
        if (root instanceof Button && value.contentEquals(((Button) root).getText())) {
            return (Button) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = findButton(group.getChildAt(i), value);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Button findButtonStartingWith(View root, String prefix) {
        if (root instanceof Button
                && ((Button) root).getText() != null
                && ((Button) root).getText().toString().startsWith(prefix)) {
            return (Button) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = findButtonStartingWith(group.getChildAt(i), prefix);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static ImageView findImageWithDescription(View root, String description) {
        if (root instanceof ImageView
                && root.getContentDescription() != null
                && description.contentEquals(root.getContentDescription())) {
            return (ImageView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                ImageView found = findImageWithDescription(group.getChildAt(i), description);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
