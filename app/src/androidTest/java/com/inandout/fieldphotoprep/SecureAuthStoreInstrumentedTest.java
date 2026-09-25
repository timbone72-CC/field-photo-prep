package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class SecureAuthStoreInstrumentedTest {
    private Context context;
    private SecureAuthStore store;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        store = new SecureAuthStore(context);
        store.clear();
    }

    @After
    public void tearDown() {
        store.clear();
    }

    @Test
    public void encryptedSessionRoundTripsAndClears() throws Exception {
        AuthSessionState expected = new AuthSessionState(
                "access-token-value",
                "refresh-token-value",
                2000000000L,
                "789bc0d8-6f9d-4ab4-9f87-33195109a65a",
                "inandoutinspections2026@gmail.com",
                "3198253e-8d41-4419-874e-46e2d3906928",
                "In And Out Cleaner Inspections LLC",
                "de55393f-6386-480a-9823-b6ee4d9901a0",
                "OWNER",
                "ACTIVE",
                1900000000L);

        store.save(expected);

        AuthSessionState actual = store.load();
        assertNotNull(actual);
        assertEquals(expected.accessToken(), actual.accessToken());
        assertEquals(expected.refreshToken(), actual.refreshToken());
        assertEquals(expected.userId(), actual.userId());
        assertEquals(expected.organizationId(), actual.organizationId());
        assertEquals(expected.membershipId(), actual.membershipId());
        assertEquals("OWNER", actual.role());
        assertEquals("ACTIVE", actual.membershipStatus());
        assertTrue(actual.isActiveOwnerOrMember());

        SharedPreferences raw = context.getSharedPreferences(
                SecureAuthStore.PREFS_NAME,
                Context.MODE_PRIVATE);
        String persisted = raw.getString("encrypted_state", "");
        assertFalse("Access token must not be stored as plaintext",
                persisted.contains("access-token-value"));
        assertFalse("Refresh token must not be stored as plaintext",
                persisted.contains("refresh-token-value"));

        store.clear();
        assertNull(store.load());
        assertFalse(store.hasStoredSession());
    }

    @Test
    public void corruptedCiphertextFailsClosedAndClearsStoredState() {
        SharedPreferences raw = context.getSharedPreferences(
                SecureAuthStore.PREFS_NAME,
                Context.MODE_PRIVATE);
        raw.edit().putString("encrypted_state", "{not-valid-encrypted-state}").commit();

        assertNull(store.load());
        assertFalse(store.hasStoredSession());
    }
}
