package com.inandout.fieldphotoprep;

import static org.junit.Assert.*;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class Concept3UiStructureInstrumentedTest {
    @Test
    public void threeScreensShareCompactIconNavigationAndFieldActions() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        for (int layout : new int[]{R.layout.screen_home_properties,
                R.layout.screen_work_orders, R.layout.screen_photos}) {
            View screen = inflater.inflate(layout, null, false);
            View nav = screen.findViewById(R.id.field_bottom_navigation);
            assertNotNull(nav);
            assertEquals(dp(context, 56), nav.getLayoutParams().height);
            for (int id : new int[]{R.id.nav_home, R.id.nav_work_orders, R.id.nav_photos}) {
                Button item = screen.findViewById(id);
                assertNotNull(item.getCompoundDrawables()[1]);
                assertEquals(1, item.getMaxLines());
                assertEquals(11 * context.getResources().getDisplayMetrics().scaledDensity,
                        item.getTextSize(), 0.1f);
            }
        }
        View home = inflater.inflate(R.layout.screen_home_properties, null, false);
        assertNotNull(home.findViewById(R.id.home_property_list));
        assertNotNull(home.findViewById(R.id.home_new_address_button));
        assertEquals(dp(context, 48), home.findViewById(R.id.drive_status_strip).getLayoutParams().height);
        View work = inflater.inflate(R.layout.screen_work_orders, null, false);
        assertNotNull(work.findViewById(R.id.work_order_list));
        assertNotNull(work.findViewById(R.id.work_order_create_button));
        assertNotNull(work.findViewById(R.id.work_order_photos));
        assertEquals(View.GONE, work.findViewById(R.id.work_order_maintenance).getVisibility());
        View photos = inflater.inflate(R.layout.screen_photos, null, false);
        assertNotNull(photos.findViewById(R.id.photos_open_camera));
        assertNotNull(photos.findViewById(R.id.photos_pending_list));
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT,
                photos.findViewById(R.id.photos_upload_selected).getLayoutParams().width);
        View row = inflater.inflate(R.layout.row_photo, null, false);
        assertNotNull(row.findViewById(R.id.photo_row_check));
        assertNotNull(row.findViewById(R.id.photo_row_thumb));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
