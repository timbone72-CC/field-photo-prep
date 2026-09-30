package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PropertyHomePolicyTest {
    @Test
    public void normalHomeShowsOnlyActiveAndSortsKnownRecentWorkFirst() {
        DriveFolder oldKnown = new DriveFolder("old", "200_SECOND_ST");
        DriveFolder recent = new DriveFolder("recent", "100_FIRST_ST");
        DriveFolder unknown = new DriveFolder("unknown", "050_ALPHA_ST");
        DriveFolder archived = new DriveFolder("archived", "300_ARCHIVED_ST");
        DriveFolder deleted = new DriveFolder("deleted", "400_DELETED_ST");

        List<DriveFolder> discovered = List.of(oldKnown, recent, unknown, archived, deleted);
        Map<String, PropertyLifecycleStore.Snapshot> lifecycle = new HashMap<>();
        lifecycle.put("old", snapshot(PropertyLifecycleStore.State.ACTIVE, 100L));
        lifecycle.put("recent", snapshot(PropertyLifecycleStore.State.ACTIVE, 200L));
        lifecycle.put("archived", snapshot(PropertyLifecycleStore.State.ARCHIVED, 300L));
        lifecycle.put("deleted", snapshot(PropertyLifecycleStore.State.DELETED, 400L));

        List<DriveFolder> visible =
                PropertyHomePolicy.visibleProperties(discovered, lifecycle, "");

        assertEquals(List.of(recent, oldKnown, unknown), visible);
    }

    @Test
    public void searchFindsActiveAndArchivedButNeverDeleted() {
        DriveFolder active = new DriveFolder("active", "123_MAIN_ST");
        DriveFolder archived = new DriveFolder("archived", "912_MAIN_ST");
        DriveFolder deleted = new DriveFolder("deleted", "999_MAIN_ST");
        Map<String, PropertyLifecycleStore.Snapshot> lifecycle = new HashMap<>();
        lifecycle.put("archived", snapshot(PropertyLifecycleStore.State.ARCHIVED, 10L));
        lifecycle.put("deleted", snapshot(PropertyLifecycleStore.State.DELETED, 20L));

        List<DriveFolder> visible = PropertyHomePolicy.visibleProperties(
                List.of(active, archived, deleted),
                lifecycle,
                "main");

        assertEquals(2, visible.size());
        assertTrue(visible.contains(active));
        assertTrue(visible.contains(archived));
        assertFalse(visible.contains(deleted));
        assertTrue(PropertyHomePolicy.isArchived(lifecycle, archived.id()));
    }

    @Test
    public void searchNormalizesDriveFolderSpacing() {
        DriveFolder folder = new DriveFolder("one", "101_W_MAIN_ST_ELK_CITY_OK");

        List<DriveFolder> visible = PropertyHomePolicy.visibleProperties(
                List.of(folder),
                new HashMap<>(),
                "w main");

        assertEquals(List.of(folder), visible);
    }

    @Test
    public void unknownActivitySortsAlphabeticallyAfterKnownActivity() {
        DriveFolder beta = new DriveFolder("beta", "200_BETA_ST");
        DriveFolder alpha = new DriveFolder("alpha", "100_ALPHA_ST");
        DriveFolder recent = new DriveFolder("recent", "300_ZETA_ST");
        Map<String, PropertyLifecycleStore.Snapshot> lifecycle = new HashMap<>();
        lifecycle.put("recent", snapshot(PropertyLifecycleStore.State.ACTIVE, 50L));

        List<DriveFolder> visible = PropertyHomePolicy.visibleProperties(
                List.of(beta, recent, alpha),
                lifecycle,
                "");

        assertEquals(List.of(recent, alpha, beta), visible);
    }

    private static PropertyLifecycleStore.Snapshot snapshot(
            PropertyLifecycleStore.State state,
            long lastUsed) {
        return new PropertyLifecycleStore.Snapshot(state, lastUsed);
    }
}
