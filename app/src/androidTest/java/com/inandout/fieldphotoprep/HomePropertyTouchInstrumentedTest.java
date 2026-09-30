package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.ListView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Regression for real finger touches. Calling the menu/selection methods directly does not
 * prove the nested ListView row and options affordance receive Android touch events.
 */
@RunWith(AndroidJUnit4.class)
public final class HomePropertyTouchInstrumentedTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

    @After
    public void cleanUp() {
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE)
                .edit().clear().commit();
        context.getSharedPreferences(PropertyLifecycleStore.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test
    public void tappingAnAddressAfterSearchOpensItsExistingWorkOrders() {
        bindTestCompany();
        DriveFolder address = new DriveFolder("tap-test-id", "99998_PHASE_13_TEST");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(mainIntent())) {
            FirstRunAuthTestHelper.dismissRequiredGateIfPresent();
            final float[] coordinates = new float[2];
            scenario.onActivity(activity -> {
                seedHome(activity, address);
                EditText search = activity.findViewById(R.id.home_property_search);
                search.setText("99998");
            });
            // Adapter data changes are synchronous; row inflation/layout is not. Let Android
            // measure and draw the list before obtaining the physical tap location.
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> {
                ListView list = activity.findViewById(R.id.home_property_list);
                assertEquals(1, list.getAdapter().getCount());
                // Synthetic DocumentsProvider fixtures are not real SAF grants.
                list.setEnabled(true);
                View row = list.getChildAt(0);
                assertTrue("Search result row must be laid out", row != null && row.getHeight() > 0);
                assertTrue("Address list must be enabled", list.isEnabled());
                int[] xy = new int[2];
                row.getLocationOnScreen(xy);
                coordinates[0] = xy[0] + row.getWidth() * .45f;
                coordinates[1] = xy[1] + row.getHeight() * .5f;
            });
            tap(coordinates[0], coordinates[1]);
            scenario.onActivity(activity -> assertEquals(
                    "Touching the address row must open Work Orders",
                    View.GONE,
                    activity.findViewById(R.id.home_root).getVisibility()));
        }
    }

    @Test
    public void tappingAddressOptionsAfterSearchOpensActualPopup() throws Exception {
        bindTestCompany();
        DriveFolder address = new DriveFolder("tap-menu-id", "99998_PHASE_13_TEST");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(mainIntent())) {
            FirstRunAuthTestHelper.dismissRequiredGateIfPresent();
            final float[] coordinates = new float[2];
            scenario.onActivity(activity -> {
                seedHome(activity, address);
                EditText search = activity.findViewById(R.id.home_property_search);
                search.setText("99998");
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> {
                ListView list = activity.findViewById(R.id.home_property_list);
                assertEquals(1, list.getAdapter().getCount());
                list.setEnabled(true);
                View row = list.getChildAt(0);
                assertTrue("Search result row must be laid out", row != null && row.getHeight() > 0);
                View options = row.findViewById(R.id.property_options);
                assertTrue("Address options must be visible", options.getVisibility() == View.VISIBLE);
                int[] xy = new int[2];
                options.getLocationOnScreen(xy);
                coordinates[0] = xy[0] + options.getWidth() * .5f;
                coordinates[1] = xy[1] + options.getHeight() * .5f;
            });
            tap(coordinates[0], coordinates[1]);
            assertFalse("Real options tap must open Archive menu",
                    awaitText("Archive").isEmpty());
            assertFalse("Real options tap must offer Delete Address",
                    awaitText("Delete Address").isEmpty());
        }
    }

    private void seedHome(MainActivity activity, DriveFolder address) {
        try {
            Method setFolders = MainActivity.class.getDeclaredMethod(
                    "setDiscoveredPropertyFolders", List.class);
            setFolders.setAccessible(true);
            setFolders.invoke(activity, List.of(address));
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private void bindTestCompany() {
        SharedPreferences prefs =
                context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE);
        prefs.edit().clear()
                .putString("master_tree_uri", "content://com.example.documents/tree/company")
                .putString("master_folder_id", "company")
                .putString("master_folder_name", "Test Company")
                .commit();
    }

    private Intent mainIntent() {
        return new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

    private void tap(float x, float y) {
        long now = android.os.SystemClock.uptimeMillis();
        instrumentation.sendPointerSync(MotionEvent.obtain(
                now, now, MotionEvent.ACTION_DOWN, x, y, 0));
        instrumentation.sendPointerSync(MotionEvent.obtain(
                now, now + 90, MotionEvent.ACTION_UP, x, y, 0));
        instrumentation.waitForIdleSync();
    }

    private List<AccessibilityNodeInfo> awaitText(String target)
            throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            AccessibilityNodeInfo root =
                    instrumentation.getUiAutomation().getRootInActiveWindow();
            if (root != null) {
                List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByText(target);
                if (!matches.isEmpty()) {
                    return matches;
                }
            }
            Thread.sleep(50L);
        }
        AccessibilityNodeInfo root =
                instrumentation.getUiAutomation().getRootInActiveWindow();
        return root == null ? List.of() : root.findAccessibilityNodeInfosByText(target);
    }
}
