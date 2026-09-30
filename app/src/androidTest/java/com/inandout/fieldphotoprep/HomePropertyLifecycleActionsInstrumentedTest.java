package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class HomePropertyLifecycleActionsInstrumentedTest {
    private final Context context =
            InstrumentationRegistry.getInstrumentation().getTargetContext();

    @After
    public void clearState() {
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE)
                .edit().clear().commit();
        context.getSharedPreferences(PropertyLifecycleStore.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test
    public void archivedOptionsReactivateWithoutChangingLastUsed() throws Exception {
        bindCompany();
        DriveFolder archived = new DriveFolder("archived-id", "200_MAIN_ST");
        PropertyLifecycleStore lifecycle = new PropertyLifecycleStore(context);
        lifecycle.markWorkActivity("company", archived.id(), 123L);
        lifecycle.archiveAfterCleanupProven("company", archived.id());

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(mainIntent())) {
            FirstRunAuthTestHelper.dismissRequiredGateIfPresent();
            scenario.onActivity(activity -> {
                invoke(activity, "setDiscoveredPropertyFolders",
                        new Class<?>[]{List.class}, List.of(archived));
                EditText search = activity.findViewById(R.id.home_property_search);
                search.setText("main");
                invoke(activity, "showPropertyOptions",
                        new Class<?>[]{View.class, DriveFolder.class},
                        search, archived);
            });

            AccessibilityNodeInfo reactivate =
                    clickableAncestor(first(awaitText(instrumentation, "Reactivate")));
            assertNotNull(reactivate);
            assertTrue(reactivate.performAction(AccessibilityNodeInfo.ACTION_CLICK));
            instrumentation.waitForIdleSync();

            PropertyLifecycleStore.Snapshot after =
                    lifecycle.snapshot("company", archived.id());
            assertEquals(PropertyLifecycleStore.State.ACTIVE, after.state());
            assertEquals(123L, after.lastUsedEpochMs());

            scenario.onActivity(activity -> {
                ListView list = activity.findViewById(R.id.home_property_list);
                assertEquals(1, list.getAdapter().getCount());
                View row = list.getAdapter().getView(0, null, list);
                TextView lifecycleText = row.findViewById(R.id.property_lifecycle);
                assertFalse(lifecycleText.getText().toString().contains("Archived"));
            });
        }
    }

    @Test
    public void archiveAndDeleteRequireWritableDriveBeforeCleanup() throws Exception {
        bindCompany();
        DriveFolder active = new DriveFolder("active-id", "100_MAIN_ST");
        PropertyLifecycleStore lifecycle = new PropertyLifecycleStore(context);
        lifecycle.markWorkActivity("company", active.id(), 222L);

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(mainIntent())) {
            FirstRunAuthTestHelper.dismissRequiredGateIfPresent();

            openOptions(scenario, active);
            AccessibilityNodeInfo archive =
                    clickableAncestor(first(awaitText(instrumentation, "Archive")));
            assertNotNull(archive);
            assertTrue(archive.performAction(AccessibilityNodeInfo.ACTION_CLICK));
            instrumentation.waitForIdleSync();

            assertEquals(
                    PropertyLifecycleStore.State.ACTIVE,
                    lifecycle.snapshot("company", active.id()).state());
            scenario.onActivity(activity -> {
                TextView status = activity.findViewById(R.id.home_status_text);
                String message = status.getText().toString();
                assertFalse("Drive cleanup rejection must be visible", message.isBlank());
                assertTrue(
                        "Cleanup must be rejected by an existing Drive authority/access guard",
                        message.contains("Drive")
                                || message.contains("workspace")
                                || message.contains("access"));
            });

            openOptions(scenario, active);
            AccessibilityNodeInfo delete =
                    clickableAncestor(first(awaitText(instrumentation, "Delete Address")));
            assertNotNull(delete);
            assertTrue(delete.performAction(AccessibilityNodeInfo.ACTION_CLICK));
            instrumentation.waitForIdleSync();

            PropertyLifecycleStore.Snapshot after =
                    lifecycle.snapshot("company", active.id());
            assertEquals(PropertyLifecycleStore.State.ACTIVE, after.state());
            assertEquals(222L, after.lastUsedEpochMs());
        }
    }

    @Test
    public void deliberateReAddRestoresSuppressedExactProviderIdentity() throws Exception {
        bindCompany();
        DriveFolder removed = new DriveFolder("same-provider-id", "300_MAIN_ST");
        PropertyLifecycleStore lifecycle = new PropertyLifecycleStore(context);
        lifecycle.deleteAfterCleanupProven("company", removed.id());

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(mainIntent())) {
            FirstRunAuthTestHelper.dismissRequiredGateIfPresent();
            scenario.onActivity(activity -> {
                invoke(activity, "setDiscoveredPropertyFolders",
                        new Class<?>[]{List.class}, List.of(removed));
                invoke(activity, "handleExistingAddressFromCreate",
                        new Class<?>[]{DriveFolder.class}, removed);
            });

            AccessibilityNodeInfo addBack =
                    clickableAncestor(first(awaitText(instrumentation, "Add Back")));
            assertNotNull(addBack);
            assertTrue(addBack.performAction(AccessibilityNodeInfo.ACTION_CLICK));
            instrumentation.waitForIdleSync();

            assertEquals(
                    PropertyLifecycleStore.State.ACTIVE,
                    lifecycle.snapshot("company", removed.id()).state());
            DriveFolder saved = new FolderPrefs(context).getCurrentAddress();
            assertNotNull(saved);
            assertEquals(removed.id(), saved.id());
        }
    }

    private void bindCompany() {
        SharedPreferences prefs =
                context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE);
        prefs.edit()
                .clear()
                .putString("master_tree_uri", "content://com.example.documents/tree/company")
                .putString("master_folder_id", "company")
                .putString("master_folder_name", "Test Company")
                .commit();
    }

    private Intent mainIntent() {
        return new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

    private static void openOptions(
            ActivityScenario<MainActivity> scenario,
            DriveFolder folder) {
        scenario.onActivity(activity -> {
            invoke(activity, "setDiscoveredPropertyFolders",
                    new Class<?>[]{List.class}, List.of(folder));
            View anchor = activity.findViewById(R.id.home_property_search);
            invoke(activity, "showPropertyOptions",
                    new Class<?>[]{View.class, DriveFolder.class},
                    anchor, folder);
        });
    }

    private static void invoke(
            Object target,
            String name,
            Class<?>[] parameterTypes,
            Object... args) {
        try {
            Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private static List<AccessibilityNodeInfo> awaitText(
            Instrumentation instrumentation,
            String text) throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
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
        AccessibilityNodeInfo root =
                instrumentation.getUiAutomation().getRootInActiveWindow();
        return root == null ? List.of() : root.findAccessibilityNodeInfosByText(text);
    }

    private static AccessibilityNodeInfo clickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null && !current.isClickable()) {
            current = current.getParent();
        }
        return current;
    }

    private static AccessibilityNodeInfo first(List<AccessibilityNodeInfo> nodes) {
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    private static AccessibilityNodeInfo last(List<AccessibilityNodeInfo> nodes) {
        return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
    }
}
