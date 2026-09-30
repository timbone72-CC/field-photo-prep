package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class PropertyLifecycleStoreInstrumentedTest {
    private final Context context =
            InstrumentationRegistry.getInstrumentation().getTargetContext();

    @After
    public void clearStore() {
        context.getSharedPreferences(PropertyLifecycleStore.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void workActivityPersistsWithoutChangingLifecycleState() {
        PropertyLifecycleStore store = new PropertyLifecycleStore(context);
        store.archiveAfterCleanupProven("company-a", "address-a");
        store.markWorkActivity("company-a", "address-a", 100L);
        store.markWorkActivity("company-a", "address-a", 90L);

        PropertyLifecycleStore reloaded = new PropertyLifecycleStore(context);
        PropertyLifecycleStore.Snapshot snapshot =
                reloaded.snapshot("company-a", "address-a");

        assertEquals(PropertyLifecycleStore.State.ARCHIVED, snapshot.state());
        assertEquals(100L, snapshot.lastUsedEpochMs());
        assertTrue(snapshot.hasLastUsed());
    }

    @Test
    public void explicitLifecycleTransitionsPreserveLastUsed() {
        PropertyLifecycleStore store = new PropertyLifecycleStore(context);
        store.markWorkActivity("company", "address", 123L);

        store.archiveAfterCleanupProven("company", "address");
        assertEquals(
                PropertyLifecycleStore.State.ARCHIVED,
                store.snapshot("company", "address").state());
        assertEquals(123L, store.snapshot("company", "address").lastUsedEpochMs());

        store.reactivate("company", "address");
        assertEquals(
                PropertyLifecycleStore.State.ACTIVE,
                store.snapshot("company", "address").state());
        assertEquals(123L, store.snapshot("company", "address").lastUsedEpochMs());

        store.deleteAfterCleanupProven("company", "address");
        assertEquals(
                PropertyLifecycleStore.State.DELETED,
                store.snapshot("company", "address").state());
        assertEquals(123L, store.snapshot("company", "address").lastUsedEpochMs());
    }

    @Test
    public void companyIdentitySeparatesSameAddressProviderId() {
        PropertyLifecycleStore store = new PropertyLifecycleStore(context);
        store.markWorkActivity("company-a", "shared-address", 111L);
        store.archiveAfterCleanupProven("company-b", "shared-address");

        assertEquals(
                111L,
                store.snapshot("company-a", "shared-address").lastUsedEpochMs());
        assertEquals(
                PropertyLifecycleStore.State.ACTIVE,
                store.snapshot("company-a", "shared-address").state());
        assertEquals(
                PropertyLifecycleStore.State.ARCHIVED,
                store.snapshot("company-b", "shared-address").state());
        assertEquals(
                0L,
                store.snapshot("company-b", "shared-address").lastUsedEpochMs());
    }
}
