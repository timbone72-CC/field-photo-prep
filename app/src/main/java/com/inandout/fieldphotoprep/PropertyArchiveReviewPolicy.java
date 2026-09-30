package com.inandout.fieldphotoprep;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class PropertyArchiveReviewPolicy {
    private PropertyArchiveReviewPolicy() {}

    static List<DriveFolder> candidates(
            List<DriveFolder> discovered,
            Map<String, PropertyLifecycleStore.Snapshot> lifecycleByAddressId,
            PropertyArchiveReviewPrefs.Threshold threshold,
            long nowEpochMs) {
        PropertyArchiveReviewPrefs.Threshold effective =
                threshold == null ? PropertyArchiveReviewPrefs.Threshold.DAYS_90 : threshold;
        if (effective == PropertyArchiveReviewPrefs.Threshold.NEVER || nowEpochMs <= 0L) {
            return List.of();
        }

        long cutoffEpochMs = cutoffEpochMs(effective, nowEpochMs);
        List<DriveFolder> result = new ArrayList<>();
        for (DriveFolder folder : PropertyHomePolicy.visibleProperties(
                discovered,
                lifecycleByAddressId,
                "")) {
            PropertyLifecycleStore.Snapshot snapshot =
                    lifecycleByAddressId == null ? null : lifecycleByAddressId.get(folder.id());
            if (snapshot != null
                    && snapshot.state() == PropertyLifecycleStore.State.ACTIVE
                    && snapshot.hasLastUsed()
                    && snapshot.lastUsedEpochMs() <= cutoffEpochMs) {
                result.add(folder);
            }
        }
        return result;
    }

    static long cutoffEpochMs(
            PropertyArchiveReviewPrefs.Threshold threshold,
            long nowEpochMs) {
        if (threshold == null || threshold == PropertyArchiveReviewPrefs.Threshold.NEVER) {
            throw new IllegalArgumentException("A finite archive review threshold is required.");
        }
        if (nowEpochMs <= 0L) {
            throw new IllegalArgumentException("Current time must be positive.");
        }

        ZonedDateTime now = Instant.ofEpochMilli(nowEpochMs).atZone(ZoneOffset.UTC);
        switch (threshold) {
            case DAYS_30:
                return now.minusDays(30).toInstant().toEpochMilli();
            case DAYS_60:
                return now.minusDays(60).toInstant().toEpochMilli();
            case DAYS_90:
                return now.minusDays(90).toInstant().toEpochMilli();
            case MONTHS_6:
                return now.minusMonths(6).toInstant().toEpochMilli();
            case YEAR_1:
                return now.minusYears(1).toInstant().toEpochMilli();
            case NEVER:
            default:
                throw new IllegalArgumentException("A finite archive review threshold is required.");
        }
    }
}
