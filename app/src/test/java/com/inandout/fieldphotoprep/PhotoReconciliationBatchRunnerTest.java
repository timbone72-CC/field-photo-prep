package com.inandout.fieldphotoprep;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class PhotoReconciliationBatchRunnerTest {
    @Test
    public void reconcilesEverySnapshotIdOnceInOrderAndCountsOutcomes() {
        PhotoReconciliationBatchRunner runner = new PhotoReconciliationBatchRunner();
        List<String> attempted = new ArrayList<>();
        List<String> ids = Arrays.asList("photo-a", "photo-b", "photo-c", "photo-d");

        PhotoReconciliationBatchRunner.BatchResult result = runner.run(ids, photoId -> {
            attempted.add(photoId);
            switch (photoId) {
                case "photo-a":
                    return PhotoReconciliationBatchRunner.AttemptResult.confirmed(true);
                case "photo-b":
                    return PhotoReconciliationBatchRunner.AttemptResult.retrySafe();
                case "photo-c":
                    return PhotoReconciliationBatchRunner.AttemptResult.uncertain("still ambiguous");
                case "photo-d":
                default:
                    return PhotoReconciliationBatchRunner.AttemptResult.confirmed(false);
            }
        });

        assertEquals(ids, attempted);
        assertEquals(4, result.selectedCount());
        assertEquals(4, result.checkedCount());
        assertEquals(2, result.confirmedCount());
        assertEquals(1, result.retrySafeCount());
        assertEquals(1, result.uncertainCount());
        assertEquals(0, result.errorCount());
        assertEquals(1, result.cleanupPendingCount());
        assertEquals(1, result.unresolvedCount());
    }

    @Test
    public void oneReconciliationExceptionIsCountedAndLaterReadOnlyChecksContinue() {
        PhotoReconciliationBatchRunner runner = new PhotoReconciliationBatchRunner();
        List<String> attempted = new ArrayList<>();

        PhotoReconciliationBatchRunner.BatchResult result = runner.run(
                Arrays.asList("photo-a", "photo-b", "photo-c"),
                photoId -> {
                    attempted.add(photoId);
                    if ("photo-b".equals(photoId)) {
                        throw new IOException("provider read failed");
                    }
                    return PhotoReconciliationBatchRunner.AttemptResult.confirmed(true);
                });

        assertEquals(Arrays.asList("photo-a", "photo-b", "photo-c"), attempted);
        assertEquals(2, result.confirmedCount());
        assertEquals(0, result.retrySafeCount());
        assertEquals(0, result.uncertainCount());
        assertEquals(1, result.errorCount());
        assertEquals(1, result.unresolvedCount());
    }

    @Test
    public void duplicateSnapshotFailsBeforeAnyAttempt() {
        PhotoReconciliationBatchRunner runner = new PhotoReconciliationBatchRunner();
        AtomicInteger attempts = new AtomicInteger();

        try {
            runner.run(Arrays.asList("photo-a", "photo-a"), photoId -> {
                attempts.incrementAndGet();
                return PhotoReconciliationBatchRunner.AttemptResult.confirmed(true);
            });
            fail("Expected duplicate snapshot to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals(0, attempts.get());
        }
    }

    @Test
    public void nullAttemptResultRemainsUnresolvedRatherThanBecomingSuccess() {
        PhotoReconciliationBatchRunner runner = new PhotoReconciliationBatchRunner();

        PhotoReconciliationBatchRunner.BatchResult result = runner.run(
                Arrays.asList("photo-a"),
                photoId -> null);

        assertEquals(0, result.confirmedCount());
        assertEquals(0, result.retrySafeCount());
        assertEquals(0, result.uncertainCount());
        assertEquals(1, result.errorCount());
        assertEquals(1, result.unresolvedCount());
    }
}
