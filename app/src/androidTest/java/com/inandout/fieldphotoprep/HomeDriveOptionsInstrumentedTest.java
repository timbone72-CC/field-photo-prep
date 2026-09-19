package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    public void homeOverflowExposesCompanyActionsAndHonorsBusyState() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences raw =
                context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE);
        raw.edit()
                .clear()
                .putString("workspace_tree_uri", "content://com.example.documents/tree/photos")
                .putString("workspace_folder_id", "workspace")
                .putString("workspace_folder_name", "Photos")
                .putString("current_company_folder_id", "company")
                .putString("current_company_folder_name", "HNP Jobs")
                .commit();

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

            assertFalse("Company options must expose Switch Company",
                    awaitText(instrumentation, "Switch Company").isEmpty());
            assertFalse("Company options must expose Add Company",
                    awaitText(instrumentation, "Add Company").isEmpty());
            assertFalse("Company options must expose Edit Company",
                    awaitText(instrumentation, "Edit Company").isEmpty());
            assertFalse("Company options must expose Change Workspace",
                    awaitText(instrumentation, "Change Workspace").isEmpty());
            assertTrue("Company options must not invent a Settings surface",
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
            raw.edit().clear().commit();
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
