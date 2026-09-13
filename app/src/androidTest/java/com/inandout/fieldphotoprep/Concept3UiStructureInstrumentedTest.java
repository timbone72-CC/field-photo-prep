package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class Concept3UiStructureInstrumentedTest {
    @Test
    public void concept3ScreensExposeRealFieldActions() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View home = inflater.inflate(R.layout.screen_home_properties, null, false);
        assertNotNull(home.findViewById(R.id.home_property_list));
        assertNotNull(home.findViewById(R.id.home_new_address_button));
        assertNotNull(home.findViewById(R.id.home_nav_work_orders));

        View work = inflater.inflate(R.layout.screen_work_orders, null, false);
        assertNotNull(work.findViewById(R.id.work_order_list));
        assertNotNull(work.findViewById(R.id.work_order_create_button));
        assertNotNull(work.findViewById(R.id.work_order_photos));
        assertNotNull(work.findViewById(R.id.work_order_maintenance));

        View photos = inflater.inflate(R.layout.screen_photos, null, false);
        assertNotNull(photos.findViewById(R.id.photos_open_camera));
        assertNotNull(photos.findViewById(R.id.photos_pending_list));
        View upload = photos.findViewById(R.id.photos_upload_selected);
        assertNotNull(upload);
        assertTrue(upload.getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
