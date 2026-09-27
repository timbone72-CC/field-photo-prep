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
                TextView safe = activity.findViewById(R.id.app_status_recovery_safe);
                TextView blocked = activity.findViewById(R.id.app_status_recovery_blocked);
                TextView protectedText =
                        activity.findViewById(R.id.app_status_recovery_protected);
                TextView next = activity.findViewById(R.id.app_status_recovery_next);
                Button recovery = activity.findViewById(R.id.app_status_recovery_action);
                Button copy = activity.findViewById(R.id.app_status_copy_support);

                assertEquals("App Status", title.getText().toString());
                assertTrue(account.getText().toString().contains("State: Sign in required"));
                assertTrue(drive.getText().toString().contains("State: Account recheck required")
                        || drive.getText().toString().contains("State: Not connected"));
                assertTrue(safe.getText().toString().startsWith("Safe now:"));
                assertTrue(blocked.getText().toString().contains("sign-in"));
                assertTrue(protectedText.getText().toString().startsWith("Protected:"));
                assertTrue(next.getText().toString().contains("Sign In"));
                assertEquals("Sign In", recovery.getText().toString());
                assertEquals(View.VISIBLE, recovery.getVisibility());
                assertTrue(copy.isEnabled());
                assertFalse(account.getText().toString().contains("content://"));
                assertFalse(protectedText.getText().toString().contains("content://"));
            });
        }
    }
}
