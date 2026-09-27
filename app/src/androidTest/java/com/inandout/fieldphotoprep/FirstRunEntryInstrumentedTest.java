package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class FirstRunEntryInstrumentedTest {
    private FieldPhotoPrepApplication app;
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        app = (FieldPhotoPrepApplication) context.getApplicationContext();
        app.authorizationManager().clearAuthenticatedSession();
    }

    @After
    public void tearDown() {
        app.authorizationManager().clearAuthenticatedSession();
    }

    @Test
    public void noSessionLauncherRoutesToExistingAuthActivity() throws Exception {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
            AuthActivity auth = FirstRunAuthTestHelper.awaitResumed(AuthActivity.class);
            assertNotNull("No-session launch must route to the existing AuthActivity", auth);

            InstrumentationRegistry.getInstrumentation().runOnMainSync(auth::finish);
            assertNotNull(
                    "Closing the auth gate must leave existing read/recovery Home reachable",
                    FirstRunAuthTestHelper.awaitResumed(MainActivity.class));
        }
    }
}
