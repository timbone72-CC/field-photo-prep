package com.inandout.fieldphotoprep;

import android.app.Activity;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

final class FirstRunAuthTestHelper {
    private FirstRunAuthTestHelper() {}

    static void dismissRequiredGateIfPresent() {
        for (int attempt = 0; attempt < 80; attempt++) {
            AuthActivity auth = awaitResumedOnce(AuthActivity.class);
            if (auth != null) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(auth::finish);
            }
            if (awaitResumedOnce(MainActivity.class) != null) {
                return;
            }
            sleepBriefly();
        }
        throw new AssertionError("MainActivity did not resume after dismissing the required auth gate.");
    }

    static <T extends Activity> T awaitResumed(Class<T> type) {
        for (int attempt = 0; attempt < 100; attempt++) {
            T activity = awaitResumedOnce(type);
            if (activity != null) {
                return activity;
            }
            sleepBriefly();
        }
        return null;
    }

    private static void sleepBriefly() {
        try {
            sleepBriefly();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for Activity lifecycle state.", error);
        }
    }

    private static <T extends Activity> T awaitResumedOnce(Class<T> type) {
        Activity[] found = new Activity[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)) {
                if (type.isInstance(activity)) {
                    found[0] = activity;
                    break;
                }
            }
        });
        return found[0] == null ? null : type.cast(found[0]);
    }
}
