package com.inandout.fieldphotoprep;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class WorkOrderFolderName {
    private WorkOrderFolderName() {
    }

    public static String build(String workOrderName, String localDate) {
        String workOrder = workOrderName == null ? "" : workOrderName.trim();
        if (workOrder.isEmpty()) {
            throw new IllegalArgumentException("Enter a work-order name.");
        }
        try {
            LocalDate.parse(localDate);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Choose a valid work-order date.");
        }
        return workOrder + " - " + localDate;
    }
}
