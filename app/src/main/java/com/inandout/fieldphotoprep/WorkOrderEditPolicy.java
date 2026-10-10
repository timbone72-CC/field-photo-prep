package com.inandout.fieldphotoprep;

import java.time.LocalDate;
import java.util.List;

/** Name correction preserves the existing work occurrence and provider identity. */
final class WorkOrderEditPolicy {
    static String name(String folderName) {
        int split = folderName == null ? -1 : folderName.lastIndexOf(" - ");
        return split > 0 && WorkOrderFolderName.isDatedWorkOrderFolder(folderName)
                ? folderName.substring(0, split) : folderName;
    }

    static LocalDate date(String folderName) {
        return WorkOrderFolderName.isDatedWorkOrderFolder(folderName)
                ? LocalDate.parse(folderName.substring(folderName.lastIndexOf(" - ") + 3))
                : LocalDate.now();
    }

    static void validate(List<DriveFolder> folders, String id, String original, String requested) {
        DriveFolder current = DriveClient.findById(folders, id);
        if (current == null || !original.equals(current.name())) {
            throw new IllegalArgumentException("This work order changed. Refresh and select it again.");
        }
        for (DriveFolder folder : DriveClient.findExactNameMatches(folders, requested)) {
            if (!id.equals(folder.id())) {
                throw new IllegalArgumentException("Another work order already uses that name and date. Choose a different name or date.");
            }
        }
    }
}
