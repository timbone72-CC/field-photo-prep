package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public final class SharedDriveWorkOrderRowInstrumentedTest {
    @Test
    public void sharedDriveCountIsSeparateFromLocalProtectedCount() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        List<DriveFolder> workOrders = new ArrayList<>(Arrays.asList(
                new DriveFolder("wo-a", "GRASS CUT - 2026-10-09")));
        Map<String, Integer> local = new HashMap<>();
        Map<String, Integer> remote = new HashMap<>();
        Set<String> errors = new HashSet<>();
        local.put("wo-a", 4);
        remote.put("wo-a", 152);
        WorkOrderListAdapter adapter = new WorkOrderListAdapter(
                context, workOrders, local, remote, errors);
        View row = adapter.getView(0, null, new FrameLayout(context));
        TextView driveTotal = row.findViewById(R.id.work_order_row_drive_photo_count);
        TextView localTotal = row.findViewById(R.id.work_order_row_photo_count);
        assertNotNull(driveTotal);
        assertEquals("In Drive: 152", driveTotal.getText().toString());
        assertEquals("4 photos", localTotal.getText().toString());

        remote.clear();
        View reused = adapter.getView(0, row, new FrameLayout(context));
        assertEquals("In Drive: —", ((TextView) reused.findViewById(
                R.id.work_order_row_drive_photo_count)).getText().toString());

        errors.add("wo-a");
        reused = adapter.getView(0, reused, new FrameLayout(context));
        assertEquals("In Drive: unavailable", ((TextView) reused.findViewById(
                R.id.work_order_row_drive_photo_count)).getText().toString());

        errors.clear();
        remote.put("wo-a", 0);
        reused = adapter.getView(0, reused, new FrameLayout(context));
        assertEquals("In Drive: 0", ((TextView) reused.findViewById(
                R.id.work_order_row_drive_photo_count)).getText().toString());
    }
}
