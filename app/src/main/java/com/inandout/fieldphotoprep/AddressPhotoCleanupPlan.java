package com.inandout.fieldphotoprep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class AddressPhotoCleanupPlan {
    static final class WorkOrderTarget {
        private final DriveFolder folder;
        private final List<String> photoDocumentIds;
        private final int preservedCount;

        WorkOrderTarget(
                DriveFolder folder,
                List<String> photoDocumentIds,
                int preservedCount) {
            if (folder == null) {
                throw new IllegalArgumentException("Work-order folder is required.");
            }
            this.folder = folder;
            this.photoDocumentIds = Collections.unmodifiableList(
                    new ArrayList<>(photoDocumentIds == null
                            ? Collections.emptyList()
                            : photoDocumentIds));
            this.preservedCount = Math.max(0, preservedCount);
        }

        DriveFolder folder() {
            return folder;
        }

        List<String> photoDocumentIds() {
            return photoDocumentIds;
        }

        int photoCount() {
            return photoDocumentIds.size();
        }

        int preservedCount() {
            return preservedCount;
        }
    }

    private final DriveFolder address;
    private final List<DriveFolder> workOrderFolders;
    private final List<WorkOrderTarget> targets;
    private final Map<String, WorkOrderTarget> targetByWorkOrderId;
    private final int totalPhotoCount;
    private final int totalPreservedCount;

    static AddressPhotoCleanupPlan create(
            DriveFolder address,
            List<DriveFolder> workOrderFolders,
            Map<String, DriveClient.ChildSnapshot> snapshotsByWorkOrderId) {
        if (address == null) {
            throw new IllegalArgumentException("Address is required.");
        }
        List<DriveFolder> sortedFolders = new ArrayList<>(
                workOrderFolders == null ? Collections.emptyList() : workOrderFolders);
        Collections.sort(sortedFolders);

        List<WorkOrderTarget> targets = new ArrayList<>();
        for (DriveFolder folder : sortedFolders) {
            DriveClient.ChildSnapshot snapshot = snapshotsByWorkOrderId == null
                    ? null
                    : snapshotsByWorkOrderId.get(folder.id());
            if (snapshot == null) {
                throw new IllegalArgumentException(
                        "Missing direct-child snapshot for work order " + folder.id());
            }
            targets.add(new WorkOrderTarget(
                    folder,
                    snapshot.photoDocumentIds(),
                    snapshot.preservedCount()));
        }
        return new AddressPhotoCleanupPlan(address, sortedFolders, targets);
    }

    private AddressPhotoCleanupPlan(
            DriveFolder address,
            List<DriveFolder> workOrderFolders,
            List<WorkOrderTarget> targets) {
        this.address = address;
        this.workOrderFolders = Collections.unmodifiableList(
                new ArrayList<>(workOrderFolders));
        this.targets = Collections.unmodifiableList(new ArrayList<>(targets));
        this.targetByWorkOrderId = new HashMap<>();

        int photos = 0;
        int preserved = 0;
        for (WorkOrderTarget target : targets) {
            if (targetByWorkOrderId.put(target.folder().id(), target) != null) {
                throw new IllegalArgumentException("Duplicate work-order provider identity.");
            }
            photos += target.photoCount();
            preserved += target.preservedCount();
        }
        this.totalPhotoCount = photos;
        this.totalPreservedCount = preserved;
    }

    DriveFolder address() {
        return address;
    }

    List<DriveFolder> workOrderFolders() {
        return workOrderFolders;
    }

    List<WorkOrderTarget> targets() {
        return targets;
    }

    int workOrderCount() {
        return workOrderFolders.size();
    }

    int totalPhotoCount() {
        return totalPhotoCount;
    }

    int totalPreservedCount() {
        return totalPreservedCount;
    }

    boolean matchesWorkOrderFolders(List<DriveFolder> currentWorkOrderFolders) {
        return DriveClient.sameFolders(
                workOrderFolders,
                currentWorkOrderFolders == null
                        ? Collections.emptyList()
                        : currentWorkOrderFolders);
    }

    boolean matchesPhotoSnapshot(
            String workOrderId,
            DriveClient.ChildSnapshot currentSnapshot) {
        WorkOrderTarget target = targetByWorkOrderId.get(workOrderId);
        return target != null
                && currentSnapshot != null
                && DriveClient.sameDocumentIds(
                        target.photoDocumentIds(),
                        currentSnapshot.photoDocumentIds());
    }
}
