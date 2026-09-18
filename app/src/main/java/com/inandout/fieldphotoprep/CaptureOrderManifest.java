package com.inandout.fieldphotoprep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds a read-only capture-order manifest from confirmed local photo history.
 * The manifest intentionally omits provider identities; it is for exact ordering/matching only.
 */
public final class CaptureOrderManifest {
    public static final String HEADER = "FPP_CAPTURE_ORDER_V1";

    public static final class Result {
        private final int count;
        private final String text;

        Result(int count, String text) {
            this.count = count;
            this.text = text;
        }

        public int count() {
            return count;
        }

        public String text() {
            return text;
        }
    }

    private CaptureOrderManifest() {
    }

    public static Result build(
            String addressDisplay,
            String workOrderDisplay,
            String expectedAddressId,
            String expectedWorkOrderId,
            List<PendingPhotoRecord> records) {
        requireText(addressDisplay, "address display");
        requireText(workOrderDisplay, "work-order display");
        requireText(expectedAddressId, "address id");
        requireText(expectedWorkOrderId, "work-order id");
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("At least one confirmed photo is required.");
        }

        List<PendingPhotoRecord> ordered = new ArrayList<>(records);
        Collections.sort(ordered);
        Set<String> photoIds = new HashSet<>();

        for (PendingPhotoRecord record : ordered) {
            if (record == null) {
                throw new IllegalArgumentException("Capture-order records cannot contain null.");
            }
            if (!expectedAddressId.equals(record.addressId())
                    || !expectedWorkOrderId.equals(record.workOrderId())) {
                throw new IllegalArgumentException(
                        "Every capture-order record must belong to the exact open work order.");
            }
            if (record.state() != PendingPhotoRecord.State.UPLOADED
                    || record.remoteFileId() == null
                    || record.remoteFileId().isBlank()) {
                throw new IllegalArgumentException(
                        "Capture order can be exported only after every photo is confirmed uploaded.");
            }
            if (!photoIds.add(record.id())) {
                throw new IllegalArgumentException("Duplicate photo identity in capture-order records.");
            }
        }

        int width = Math.max(3, Integer.toString(ordered.size()).length());
        StringBuilder output = new StringBuilder();
        output.append(HEADER).append('\n');
        output.append("address=").append(oneLine(addressDisplay)).append('\n');
        output.append("workOrder=").append(oneLine(workOrderDisplay)).append('\n');
        output.append("count=").append(ordered.size()).append('\n');

        for (int i = 0; i < ordered.size(); i++) {
            PendingPhotoRecord record = ordered.get(i);
            int sequenceValue = record.hasCaptureSequence()
                    ? record.captureSequence()
                    : i + 1;
            String sequence = String.format(Locale.US, "%0" + width + "d", sequenceValue);
            output.append(sequence)
                    .append('|').append(record.createdAtEpochMs())
                    .append('|').append(record.id())
                    .append('|').append(DrivePhotoUploader.remoteFileNameFor(record))
                    .append('\n');
        }

        return new Result(ordered.size(), output.toString());
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value;
    }

    private static String oneLine(String value) {
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
