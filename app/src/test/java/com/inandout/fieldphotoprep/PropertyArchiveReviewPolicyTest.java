package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PropertyArchiveReviewPolicyTest {
    @Test
    public void ninetyDayReviewIncludesOnlyKnownActiveAddressesAtOrPastCutoff() {
        long now = Instant.parse("2026-09-29T12:00:00Z").toEpochMilli();
        DriveFolder due = new DriveFolder("due", "100_DUE_ST");
        DriveFolder recent = new DriveFolder("recent", "200_RECENT_ST");
        DriveFolder unknown = new DriveFolder("unknown", "300_UNKNOWN_ST");
        DriveFolder archived = new DriveFolder("archived", "400_ARCHIVED_ST");

        Map<String, PropertyLifecycleStore.Snapshot> lifecycle = new HashMap<>();
        lifecycle.put("due", snapshot(
                PropertyLifecycleStore.State.ACTIVE,
                Instant.parse("2026-07-01T12:00:00Z").toEpochMilli()));
        lifecycle.put("recent", snapshot(
                PropertyLifecycleStore.State.ACTIVE,
                Instant.parse("2026-07-02T12:00:00Z").toEpochMilli()));
        lifecycle.put("archived", snapshot(
                PropertyLifecycleStore.State.ARCHIVED,
                Instant.parse("2026-06-01T12:00:00Z").toEpochMilli()));

        assertEquals(
                List.of(due),
                PropertyArchiveReviewPolicy.candidates(
                        List.of(recent, archived, unknown, due),
                        lifecycle,
                        PropertyArchiveReviewPrefs.Threshold.DAYS_90,
                        now));
    }

    @Test
    public void sixMonthReviewUsesCalendarMonthCutoff() {
        long now = Instant.parse("2026-09-29T12:00:00Z").toEpochMilli();
        assertEquals(
                Instant.parse("2026-03-29T12:00:00Z").toEpochMilli(),
                PropertyArchiveReviewPolicy.cutoffEpochMs(
                        PropertyArchiveReviewPrefs.Threshold.MONTHS_6,
                        now));
    }

    @Test
    public void neverProducesNoReviewCandidates() {
        long now = Instant.parse("2026-09-29T12:00:00Z").toEpochMilli();
        DriveFolder old = new DriveFolder("old", "100_OLD_ST");
        Map<String, PropertyLifecycleStore.Snapshot> lifecycle = new HashMap<>();
        lifecycle.put("old", snapshot(
                PropertyLifecycleStore.State.ACTIVE,
                Instant.parse("2020-01-01T00:00:00Z").toEpochMilli()));

        assertEquals(
                List.of(),
                PropertyArchiveReviewPolicy.candidates(
                        List.of(old),
                        lifecycle,
                        PropertyArchiveReviewPrefs.Threshold.NEVER,
                        now));
    }

    private static PropertyLifecycleStore.Snapshot snapshot(
            PropertyLifecycleStore.State state,
            long lastUsed) {
        return new PropertyLifecycleStore.Snapshot(state, lastUsed);
    }
}
