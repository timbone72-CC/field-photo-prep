package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

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
