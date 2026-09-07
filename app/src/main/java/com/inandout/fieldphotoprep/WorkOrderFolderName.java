package com.inandout.fieldphotoprep;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class WorkOrderFolderName {
    private static final String DATE_SEPARATOR = " - ";

    private WorkOrderFolderName() {
    }

    public static String build(String workOrderName, String localDate) {
        String workOrder = normalizeWorkOrder(workOrderName);
        LocalDate date = parseRequestedDate(localDate);
        return workOrder + DATE_SEPARATOR + date;
    }

    public static boolean isOlderSameWorkOrderFolder(
            String candidateFolderName,
            String requestedWorkOrderName,
            String requestedLocalDate) {
        String requestedWorkOrder = normalizeWorkOrder(requestedWorkOrderName);
        LocalDate requestedDate = parseRequestedDate(requestedLocalDate);
        ParsedFolder candidate = parseExisting(candidateFolderName);
        return candidate != null
                && candidate.workOrder.equals(requestedWorkOrder)
                && candidate.date.isBefore(requestedDate);
    }

    private static String normalizeWorkOrder(String workOrderName) {
        String workOrder = workOrderName == null ? "" : workOrderName.trim();
        if (workOrder.isEmpty()) {
            throw new IllegalArgumentException("Enter a work-order name.");
        }
        return workOrder;
    }

    private static LocalDate parseRequestedDate(String localDate) {
        try {
            return LocalDate.parse(localDate);
        } catch (DateTimeParseException | NullPointerException error) {
            throw new IllegalArgumentException("Choose a valid work-order date.");
        }
    }

    private static ParsedFolder parseExisting(String folderName) {
        if (folderName == null) {
            return null;
        }
        int separator = folderName.lastIndexOf(DATE_SEPARATOR);
        if (separator <= 0 || separator + DATE_SEPARATOR.length() >= folderName.length()) {
            return null;
        }
        String workOrder = folderName.substring(0, separator);
        String dateText = folderName.substring(separator + DATE_SEPARATOR.length());
        try {
            return new ParsedFolder(workOrder, LocalDate.parse(dateText));
        } catch (DateTimeParseException error) {
            return null;
        }
    }

    private static final class ParsedFolder {
        private final String workOrder;
        private final LocalDate date;

        private ParsedFolder(String workOrder, LocalDate date) {
            this.workOrder = workOrder;
            this.date = date;
        }
    }
}
