package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class WorkOrderChildDiscoveryUiInstrumentedTest {
    @Test
    public void openingPropertyKeepsPropertyRowsSeparateAndClearsWorkOrderSelfState() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        var raw = context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        try {
            raw.edit()
                    .putString("current_address_folder_id", "address-1607")
                    .putString("current_address_folder_name", "1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST")
                    .putString("current_work_order_folder_id", "address-1607")
                    .putString("current_work_order_folder_name", "1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST")
                    .commit();

            DriveFolder property = new DriveFolder(
                    "address-1607", "1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST");
            DriveFolder sibling = new DriveFolder(
                    "address-1611", "1611_NW_SMITH_AVE");

            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            FirstRunAuthTestHelper.dismissRequiredGateIfPresent();
                scenario.onActivity(activity -> {
                    try {
                        @SuppressWarnings("unchecked")
                        List<DriveFolder> properties =
                                (List<DriveFolder>) field(activity, "propertyFolders");
                        @SuppressWarnings("unchecked")
                        List<DriveFolder> workOrders =
                                (List<DriveFolder>) field(activity, "workOrderFolders");

                        properties.clear();
                        properties.add(property);
                        properties.add(sibling);
                        workOrders.clear();
                        workOrders.add(property);
                        call(activity, "notifyFolderAdapters");

                        call(activity, "openAddress", new Class<?>[]{DriveFolder.class}, property);

                        assertTrue("Opening Work Orders must clear stale work-order rows",
                                workOrders.isEmpty());
                        assertTrue("Property rows must remain independently owned",
                                properties.size() == 2
                                        && properties.get(0).id().equals(property.id())
                                        && properties.get(1).id().equals(sibling.id()));
                        assertNull("Legacy self-child state must not become selected work order",
                                field(activity, "selectedWorkOrder"));
                        assertFalse("Photos must stay disabled without a valid work order",
                                activity.findViewById(R.id.work_order_photos).isEnabled());
                    } catch (Exception error) {
                        throw new AssertionError(error);
                    }
                });
            }
        } finally {
            raw.edit().clear().commit();
        }
    }

    @Test
    public void editDialogPrefillsExistingNameDateAndCancelPreservesSelection() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            FirstRunAuthTestHelper.dismissRequiredGateIfPresent();
            scenario.onActivity(activity -> {
                try {
                    fieldSet(activity, "selectedAddress", new DriveFolder("address", "Test address"));
                    fieldSet(activity, "selectedWorkOrder", new DriveFolder("wo", "GRASS CUT - 2026-09-29"));
                    fieldSet(activity, "busy", false);
                    call(activity, "showEditWorkOrderDialog");
                } catch (Exception error) { throw new AssertionError(error); }
            });
            assertFalse(awaitText("GRASS CUT").isEmpty());
            assertFalse(awaitText("Date: 2026-09-29").isEmpty());
            assertTrue(awaitText("Cancel").get(0)
                    .performAction(AccessibilityNodeInfo.ACTION_CLICK));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                try {
                    DriveFolder selected = (DriveFolder) field(activity, "selectedWorkOrder");
                    org.junit.Assert.assertEquals("wo", selected.id());
                    org.junit.Assert.assertEquals("GRASS CUT - 2026-09-29", selected.name());
                    assertTrue(activity.findViewById(R.id.work_order_edit) != null);
                    fieldSet(activity, "workOrderEditInProgress", true);
                    fieldSet(activity, "busy", true);
                    call(activity, "setNotBusy");
                    assertTrue("Auth revalidation must not unlock concurrent edits",
                            (Boolean) field(activity, "busy"));
                    fieldSet(activity, "workOrderEditInProgress", false);
                } catch (Exception error) { throw new AssertionError(error); }
            });
        }
    }

    @Test
    public void sameDateClearConfirmationShowsPhotoCountAndCancelKeepsSelectedWorkOrder() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            FirstRunAuthTestHelper.dismissRequiredGateIfPresent();
            scenario.onActivity(activity -> {
                try {
                    DriveFolder address = new DriveFolder("test-address", "Disposable address");
                    DriveFolder work = new DriveFolder("test-wo", "GRASS CUT - 2026-10-06");
                    call(activity, "openAddress", new Class<?>[]{DriveFolder.class}, address);
                    fieldSet(activity, "selectedWorkOrder", work);
                    fieldSet(activity, "busy", false);
                    DriveClient.ChildSnapshot snapshot = new DriveClient.ChildSnapshot(
                            java.util.Arrays.asList("photo-a", "photo-b"),
                            java.util.Arrays.asList("photo-a", "photo-b"), 0);
                    call(activity, "showClearReuseConfirmation", new Class<?>[]{
                            android.net.Uri.class, String.class, String.class, String.class,
                            String.class, java.time.LocalDate.class, DriveClient.ChildSnapshot.class},
                            android.net.Uri.parse("content://fixture/tree/test"), address.id(),
                            work.id(), work.name(), work.name(), java.time.LocalDate.of(2026, 10, 6), snapshot);
                } catch (Exception error) { throw new AssertionError(error); }
            });
            assertFalse(awaitText("Photos to remove from Drive: 2").isEmpty());
            assertFalse(awaitText("New date: 2026-10-06").isEmpty());
            assertTrue(awaitText("Cancel").get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                try {
                    org.junit.Assert.assertEquals("test-wo", ((DriveFolder) field(activity, "selectedWorkOrder")).id());
                    fieldSet(activity, "workOrderReuseInProgress", true);
                    fieldSet(activity, "busy", true);
                    call(activity, "setNotBusy");
                    assertTrue((Boolean) field(activity, "busy"));
                    fieldSet(activity, "workOrderReuseInProgress", false);
                } catch (Exception error) { throw new AssertionError(error); }
            });
        }
    }

    private static List<AccessibilityNodeInfo> awaitText(String text) throws Exception {
        for (int attempt = 0; attempt < 80; attempt++) {
            AccessibilityNodeInfo root = InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation().getRootInActiveWindow();
            if (root != null) {
                List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByText(text);
                if (!found.isEmpty()) { return found; }
            }
            Thread.sleep(25L);
        }
        throw new AssertionError("Missing dialog control: " + text);
    }

    private static void fieldSet(Object owner, String name, Object value) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void call(Object owner, String name) throws Exception {
        call(owner, name, new Class<?>[]{});
    }

    private static void call(Object owner, String name, Class<?>[] types, Object... values) throws Exception {
        Method method = owner.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(owner, values);
    }
}
