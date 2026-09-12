package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class Phase9UiInstrumentedTest {
    @Test
    public void photoScreenDecoratesWithoutChangingWorkflowAvailability() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FolderPrefs prefs = new FolderPrefs(context);
        prefs.setCurrentAddress(new DriveFolder("test-address-provider-id", "123 Test Street"));
        prefs.setCurrentWorkOrder(new DriveFolder("test-work-order-provider-id", "Cut Grass - 2026-09-11"));

        try (ActivityScenario<PhotoCaptureActivity> scenario =
                     ActivityScenario.launch(PhotoCaptureActivity.class)) {
            scenario.onActivity(activity -> {
                View content = activity.findViewById(android.R.id.content);
                TextView title = findText(content, "Cut Grass - 2026-09-11");
                Button camera = findButton(content, "Open Camera");
                Button upload = findButtonStartingWith(content, "Upload Selected (");

                assertNotNull("redesigned work-order title should be visible", title);
                assertNotNull("existing camera listener should be presented as Open Camera", camera);
                assertNotNull("existing batch action should remain present", upload);
                assertTrue("camera action remains enabled with a selected work order", camera.isEnabled());
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
}
