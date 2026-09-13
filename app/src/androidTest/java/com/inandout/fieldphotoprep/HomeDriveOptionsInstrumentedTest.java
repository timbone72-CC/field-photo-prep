package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class HomeDriveOptionsInstrumentedTest {
    @Test
    public void homeOverflowExposesOnlyChangeDriveAndLaunchesExistingTreePicker() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                new IntentFilter(Intent.ACTION_OPEN_DOCUMENT_TREE),
                new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null),
                true);

        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                View overflow = activity.findViewById(R.id.home_drive_options_button);
                overflow.setVisibility(View.VISIBLE);
                overflow.setEnabled(true);
                assertTrue(overflow.performClick());
            });

            instrumentation.waitForIdleSync();
            AccessibilityNodeInfo root = instrumentation.getUiAutomation().getRootInActiveWindow();
            assertNotNull(root);
            List<AccessibilityNodeInfo> changeDrive = root.findAccessibilityNodeInfosByText("Change Drive");
            assertFalse("Drive options must expose Change Drive", changeDrive.isEmpty());
            assertTrue("Step 3 must not add a Settings option",
                    root.findAccessibilityNodeInfosByText("Settings").isEmpty());

            AccessibilityNodeInfo clickable = clickableAncestor(changeDrive.get(0));
            assertNotNull("Change Drive row must be clickable", clickable);
            assertTrue(clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK));
            instrumentation.waitForIdleSync();
            assertTrue("Change Drive must launch the existing ACTION_OPEN_DOCUMENT_TREE path",
                    instrumentation.checkMonitorHit(monitor, 1));
            monitor = null;

            scenario.onActivity(activity -> {
                try {
                    Method setBusy = MainActivity.class.getDeclaredMethod("setBusy", String.class);
                    setBusy.setAccessible(true);
                    View overflow = activity.findViewById(R.id.home_drive_options_button);
                    overflow.setVisibility(View.VISIBLE);
                    setBusy.invoke(activity, "Test Drive operation");
                    assertFalse("Drive options must be disabled while Drive work is busy",
                            overflow.isEnabled());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
        } finally {
            if (monitor != null) {
                instrumentation.removeMonitor(monitor);
            }
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    private static AccessibilityNodeInfo clickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null && !current.isClickable()) {
            current = current.getParent();
        }
        return current;
    }
}
