package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class AppStatusActivityInstrumentedTest {
    private Context context;
    private RuntimeAuthorizationManager authorizationManager;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FieldPhotoPrepApplication app =
                (FieldPhotoPrepApplication) context.getApplicationContext();
        authorizationManager = app.authorizationManager();
        authorizationManager.clearAuthenticatedSession();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @After
    public void tearDown() {
        authorizationManager.clearAuthenticatedSession();
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void signedOutStatusRendersReadOnlyRecoverySurface() {
        Intent intent = new Intent(context, AppStatusActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        try (ActivityScenario<AppStatusActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                TextView title = activity.findViewById(R.id.app_status_title);
                TextView account = activity.findViewById(R.id.app_status_account);
                TextView drive = activity.findViewById(R.id.app_status_drive);
                Button recheck = activity.findViewById(R.id.app_status_recheck);
                Button connect = activity.findViewById(R.id.app_status_connect_drive);
                Button copy = activity.findViewById(R.id.app_status_copy_support);

                assertEquals("App Status", title.getText().toString());
                assertTrue(account.getText().toString().contains("State: Sign in required"));
                assertTrue(drive.getText().toString().contains("State: Account recheck required")
                        || drive.getText().toString().contains("State: Not connected"));
                assertEquals("Sign In", recheck.getText().toString());
                assertEquals(View.GONE, connect.getVisibility());
                assertTrue(copy.isEnabled());
                assertFalse(account.getText().toString().contains("content://"));
            });
        }
    }
}
