package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class AuthDeepLinkInstrumentedTest {
    @Test
    public void buildSpecificAuthUriResolvesToDedicatedAuthActivity() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Uri uri = Uri.parse(AuthConfig.redirectUri()
                + "#access_token=a&refresh_token=b&type=recovery");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE);

        List<ResolveInfo> matches = context.getPackageManager()
                .queryIntentActivities(intent, 0);

        boolean authActivityFound = false;
        boolean mainActivityFound = false;
        for (ResolveInfo match : matches) {
            if (!context.getPackageName().equals(match.activityInfo.packageName)) {
                continue;
            }
            if (AuthActivity.class.getName().equals(match.activityInfo.name)) {
                authActivityFound = true;
            }
            if (MainActivity.class.getName().equals(match.activityInfo.name)) {
                mainActivityFound = true;
            }
        }

        assertTrue("Auth callback must resolve to AuthActivity", authActivityFound);
        assertFalse("MainActivity must not own auth callback links", mainActivityFound);
    }

    @Test
    public void internalBuildUsesInternalSpecificCallbackScheme() {
        if (BuildConfig.APPLICATION_ID.endsWith(".internal")) {
            assertTrue(AuthConfig.redirectScheme().endsWith(".internal"));
        }
    }
}
