package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
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
    public void homeOverflowExposesOnlyChangeDriveAndHonorsBusyState() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                View overflow = activity.findViewById(R.id.home_drive_options_button);
                overflow.setVisibility(View.VISIBLE);
                overflow.setEnabled(true);
                assertTrue(overflow.performClick());
            });

            List<AccessibilityNodeInfo> changeDrive = awaitText(instrumentation, "Change Drive");
            assertFalse("Drive options must expose Change Drive", changeDrive.isEmpty());
            assertTrue("Step 3 must not add a Settings option",
                    findText(instrumentation, "Settings").isEmpty());

            List<AccessibilityNodeInfo> cancelNodes = awaitText(instrumentation, "Cancel");
            assertFalse("Drive options must remain dismissible", cancelNodes.isEmpty());
            AccessibilityNodeInfo cancel = clickableAncestor(cancelNodes.get(0));
            assertNotNull(cancel);
            assertTrue(cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK));
            instrumentation.waitForIdleSync();

            scenario.onActivity(activity -> {
                try {
                    Method setBusy = MainActivity.class.getDeclaredMethod("setBusy", String.class);
                    setBusy.setAccessible(true);
                    View overflow = activity.findViewById(R.id.home_drive_options_button);
                    overflow.setVisibility(View.VISIBLE);
                    overflow.setEnabled(true);
                    setBusy.invoke(activity, "Test Drive operation");
                    assertFalse("Drive options must be disabled while Drive work is busy",
                            overflow.isEnabled());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
        } finally {
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    private static List<AccessibilityNodeInfo> awaitText(
            Instrumentation instrumentation,
            String text) throws InterruptedException {
        for (int attempt = 0; attempt < 40; attempt++) {
            instrumentation.waitForIdleSync();
            List<AccessibilityNodeInfo> nodes = findText(instrumentation, text);
            if (!nodes.isEmpty()) {
                return nodes;
            }
            Thread.sleep(50);
        }
        return findText(instrumentation, text);
    }

    private static List<AccessibilityNodeInfo> findText(
            Instrumentation instrumentation,
            String text) {
        AccessibilityNodeInfo root = instrumentation.getUiAutomation().getRootInActiveWindow();
        if (root == null) {
            return List.of();
        }
        return root.findAccessibilityNodeInfosByText(text);
    }

    private static AccessibilityNodeInfo clickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null && !current.isClickable()) {
            current = current.getParent();
        }
        return current;
    }
}
