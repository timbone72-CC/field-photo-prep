package com.inandout.fieldphotoprep;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AddressPhotoCleanupPlanTest {
    @Test
    public void aggregatesExactWorkOrdersPhotosAndPreservedItems() {
        DriveFolder address = new DriveFolder("address", "100_MAIN_ST");
        DriveFolder first = new DriveFolder("work-1", "Cut Grass - 2026-09-20");
        DriveFolder second = new DriveFolder("work-2", "Inspection - 2026-09-21");
        Map<String, DriveClient.ChildSnapshot> snapshots = new HashMap<>();
        snapshots.put("work-1", new DriveClient.ChildSnapshot(
                Arrays.asList("p1", "p2", "note"),
                Arrays.asList("p1", "p2"),
                0));
        snapshots.put("work-2", new DriveClient.ChildSnapshot(
                Arrays.asList("p3", "child-folder"),
                Arrays.asList("p3"),
                1));

        AddressPhotoCleanupPlan plan = AddressPhotoCleanupPlan.create(
                address,
                Arrays.asList(second, first),
                snapshots);

        assertEquals("address", plan.address().id());
        assertEquals(2, plan.workOrderCount());
        assertEquals(3, plan.totalPhotoCount());
        assertEquals(2, plan.totalPreservedCount());
        assertEquals("work-1", plan.targets().get(0).folder().id());
        assertEquals(Arrays.asList("p1", "p2"), plan.targets().get(0).photoDocumentIds());
    }

    @Test
    public void workOrderRevalidationRejectsRenameIdentityChangeOrNewFolder() {
        DriveFolder address = new DriveFolder("address", "100_MAIN_ST");
        DriveFolder work = new DriveFolder("work", "Cut Grass - 2026-09-20");
        Map<String, DriveClient.ChildSnapshot> snapshots = new HashMap<>();
        snapshots.put("work", new DriveClient.ChildSnapshot(
                List.of("p1"), List.of("p1"), 0));
        AddressPhotoCleanupPlan plan =
                AddressPhotoCleanupPlan.create(address, List.of(work), snapshots);

        assertTrue(plan.matchesWorkOrderFolders(List.of(work)));
        assertFalse(plan.matchesWorkOrderFolders(List.of(
                new DriveFolder("work", "Cut Grass - 2026-09-21"))));
        assertFalse(plan.matchesWorkOrderFolders(List.of(
                new DriveFolder("other", "Cut Grass - 2026-09-20"))));
        assertFalse(plan.matchesWorkOrderFolders(Arrays.asList(
                work,
                new DriveFolder("new", "Inspection - 2026-09-21"))));
    }

    @Test
    public void photoRevalidationUsesExactPhotoIdentitySetButAllowsPreservedItemsToChange() {
        DriveFolder address = new DriveFolder("address", "100_MAIN_ST");
        DriveFolder work = new DriveFolder("work", "Cut Grass - 2026-09-20");
        Map<String, DriveClient.ChildSnapshot> snapshots = new HashMap<>();
        snapshots.put("work", new DriveClient.ChildSnapshot(
                Arrays.asList("p1", "note"),
                List.of("p1"),
                0));
        AddressPhotoCleanupPlan plan =
                AddressPhotoCleanupPlan.create(address, List.of(work), snapshots);

        assertTrue(plan.matchesPhotoSnapshot(
                "work",
                new DriveClient.ChildSnapshot(
                        Arrays.asList("p1", "note", "other-note"),
                        List.of("p1"),
                        0)));
        assertFalse(plan.matchesPhotoSnapshot(
                "work",
                new DriveClient.ChildSnapshot(
                        Arrays.asList("p2", "note"),
                        List.of("p2"),
                        0)));
        assertFalse(plan.matchesPhotoSnapshot("missing",
                new DriveClient.ChildSnapshot(List.of(), List.of(), 0)));
    }
}
