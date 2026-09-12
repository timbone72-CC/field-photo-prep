package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class Phase9AHomeInstrumentedTest {
    @Before
    public void clearFolderPreferences() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void disconnectedHomeUsesPurposeBuiltCompactStructure() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                TextView title = activity.findViewById(R.id.home_title);
                TextView driveState = activity.findViewById(R.id.home_drive_state);
                Button connect = activity.findViewById(R.id.home_connect_button);
                ImageButton refresh = activity.findViewById(R.id.home_refresh_button);
                Button newAddress = activity.findViewById(R.id.home_new_address_button);
                ListView properties = activity.findViewById(R.id.home_property_list);

                assertNotNull(title);
                assertEquals("Field Photo Prep", title.getText().toString());
                assertNotNull(driveState);
                assertEquals("Not connected", driveState.getText().toString());
                assertEquals(View.VISIBLE, connect.getVisibility());
                assertEquals(View.GONE, refresh.getVisibility());
                assertNotNull(properties);

                assertEquals(
                        "New Address must remain a compact bottom-end action, not match-parent width",
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        newAddress.getLayoutParams().width);
                assertTrue(newAddress.getText().toString().contains("New Address"));

                assertTrue(
                        "The old development subtitle must not be visible on Home",
                        !containsVisibleText(activity.findViewById(android.R.id.content),
                                "Address and work-order setup"));
            });
        }
    }

    private static boolean containsVisibleText(View view, String target) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }
        if (view instanceof TextView
                && target.contentEquals(((TextView) view).getText())) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsVisibleText(group.getChildAt(i), target)) {
                    return true;
                }
            }
        }
        return false;
    }
}
