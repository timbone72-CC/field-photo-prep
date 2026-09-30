package com.inandout.fieldphotoprep;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class AddressPhotoCleanupMutationTest {
    @Test
    public void deletesApprovedPhotosSequentiallyAndVerifiesEachWorkOrderEmpty() throws Exception {
        AddressPhotoCleanupPlan plan = planWithThreePhotos();
        List<String> calls = new ArrayList<>();

        int removed = AddressPhotoCleanupMutation.removeApprovedPhotosAndVerifyEmpty(
                plan,
                new AddressPhotoCleanupMutation.Operations() {
                    @Override
                    public void deletePhoto(String photoDocumentId) {
                        calls.add("delete:" + photoDocumentId);
                    }

                    @Override
                    public DriveClient.ChildSnapshot readWorkOrderChildren(String workOrderId) {
                        calls.add("read:" + workOrderId);
                        return new DriveClient.ChildSnapshot(List.of("note"), List.of(), 0);
                    }
                });

        assertEquals(3, removed);
        assertEquals(Arrays.asList(
                "delete:p1",
                "delete:p2",
                "read:work-1",
                "delete:p3",
                "read:work-2"), calls);
    }

    @Test
    public void firstDeleteFailureStopsLaterPhotosAndReportsAttemptedMutation() {
        AddressPhotoCleanupPlan plan = planWithThreePhotos();
        List<String> calls = new ArrayList<>();

        try {
            AddressPhotoCleanupMutation.removeApprovedPhotosAndVerifyEmpty(
                    plan,
                    new AddressPhotoCleanupMutation.Operations() {
                        @Override
                        public void deletePhoto(String photoDocumentId) throws IOException {
                            calls.add("delete:" + photoDocumentId);
                            if ("p2".equals(photoDocumentId)) {
                                throw new IOException("provider failure");
                            }
                        }

                        @Override
                        public DriveClient.ChildSnapshot readWorkOrderChildren(String workOrderId) {
                            calls.add("read:" + workOrderId);
                            return new DriveClient.ChildSnapshot(List.of(), List.of(), 0);
                        }
                    });
            fail("Expected partial cleanup failure");
        } catch (AddressPhotoCleanupMutation.Failure expected) {
            assertEquals(1, expected.removedCount());
            assertTrue(expected.remoteMutationAttempted());
            assertEquals(Arrays.asList("delete:p1", "delete:p2"), calls);
        }
    }

    @Test
    public void postDeleteNonEmptyVerificationStopsBeforeLaterWorkOrder() {
        AddressPhotoCleanupPlan plan = planWithThreePhotos();
        List<String> calls = new ArrayList<>();

        try {
            AddressPhotoCleanupMutation.removeApprovedPhotosAndVerifyEmpty(
                    plan,
                    new AddressPhotoCleanupMutation.Operations() {
                        @Override
                        public void deletePhoto(String photoDocumentId) {
                            calls.add("delete:" + photoDocumentId);
                        }

                        @Override
                        public DriveClient.ChildSnapshot readWorkOrderChildren(String workOrderId) {
                            calls.add("read:" + workOrderId);
                            if ("work-1".equals(workOrderId)) {
                                return new DriveClient.ChildSnapshot(
                                        List.of("late-photo"),
                                        List.of("late-photo"),
                                        0);
                            }
                            return new DriveClient.ChildSnapshot(List.of(), List.of(), 0);
                        }
                    });
            fail("Expected post-delete verification failure");
        } catch (AddressPhotoCleanupMutation.Failure expected) {
            assertEquals(2, expected.removedCount());
            assertTrue(expected.remoteMutationAttempted());
            assertEquals(Arrays.asList(
                    "delete:p1",
                    "delete:p2",
                    "read:work-1"), calls);
        }
    }

    @Test
    public void zeroPhotoCleanupVerifiesWithoutClaimingRemoteMutation() throws Exception {
        DriveFolder address = new DriveFolder("address", "100_MAIN_ST");
        DriveFolder work = new DriveFolder("work", "Inspection - 2026-09-20");
        Map<String, DriveClient.ChildSnapshot> snapshots = new HashMap<>();
        snapshots.put("work", new DriveClient.ChildSnapshot(
                List.of("note"),
                List.of(),
                0));
        AddressPhotoCleanupPlan plan =
                AddressPhotoCleanupPlan.create(address, List.of(work), snapshots);

        int removed = AddressPhotoCleanupMutation.removeApprovedPhotosAndVerifyEmpty(
                plan,
                new AddressPhotoCleanupMutation.Operations() {
                    @Override
                    public void deletePhoto(String photoDocumentId) {
                        fail("No photo should be deleted");
                    }

                    @Override
                    public DriveClient.ChildSnapshot readWorkOrderChildren(String workOrderId) {
                        return new DriveClient.ChildSnapshot(List.of("note"), List.of(), 0);
                    }
                });

        assertEquals(0, removed);
    }

    private static AddressPhotoCleanupPlan planWithThreePhotos() {
        DriveFolder address = new DriveFolder("address", "100_MAIN_ST");
        DriveFolder first = new DriveFolder("work-1", "Cut Grass - 2026-09-20");
        DriveFolder second = new DriveFolder("work-2", "Inspection - 2026-09-21");
        Map<String, DriveClient.ChildSnapshot> snapshots = new HashMap<>();
        snapshots.put("work-1", new DriveClient.ChildSnapshot(
                Arrays.asList("p1", "p2"),
                Arrays.asList("p1", "p2"),
                0));
        snapshots.put("work-2", new DriveClient.ChildSnapshot(
                List.of("p3"),
                List.of("p3"),
                0));
        return AddressPhotoCleanupPlan.create(
                address,
                Arrays.asList(first, second),
                snapshots);
    }
}
