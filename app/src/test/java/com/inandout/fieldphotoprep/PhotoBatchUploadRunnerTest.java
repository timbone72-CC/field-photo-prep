package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class PhotoBatchUploadRunnerTest {
    @Test
    public void attemptsSelectedPhotosExactlyOnceInOrder() {
        PhotoBatchUploadRunner runner = new PhotoBatchUploadRunner();
        List<String> seen = new ArrayList<>();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();

        PhotoBatchUploadRunner.BatchResult result = runner.run(
                List.of("a", "b", "c"),
                photoId -> {
                    int now = active.incrementAndGet();
                    maxActive.set(Math.max(maxActive.get(), now));
                    seen.add(photoId);
                    active.decrementAndGet();
                    return PhotoBatchUploadRunner.AttemptResult.confirmed(true);
                });

        assertEquals(List.of("a", "b", "c"), seen);
        assertEquals(1, maxActive.get());
        assertEquals(3, result.attemptedCount());
        assertEquals(3, result.confirmedCount());
        assertEquals(0, result.safeFailureCount());
        assertEquals(0, result.unattemptedCount());
        assertFalse(result.stoppedEarly());
    }

    @Test
    public void safeFailureDoesNotBlockLaterSelectedPhoto() {
        PhotoBatchUploadRunner runner = new PhotoBatchUploadRunner();
        List<String> seen = new ArrayList<>();

        PhotoBatchUploadRunner.BatchResult result = runner.run(
                List.of("a", "b", "c"),
                photoId -> {
                    seen.add(photoId);
                    if ("b".equals(photoId)) {
                        return PhotoBatchUploadRunner.AttemptResult.safeFailure("retry-safe failure");
                    }
                    return PhotoBatchUploadRunner.AttemptResult.confirmed(true);
                });

        assertEquals(List.of("a", "b", "c"), seen);
        assertEquals(2, result.confirmedCount());
        assertEquals(List.of("b"), result.safeFailurePhotoIds());
        assertFalse(result.stoppedEarly());
    }

    @Test
    public void uncertainResultStopsBeforeAnyLaterRemoteAttempt() {
        PhotoBatchUploadRunner runner = new PhotoBatchUploadRunner();
        List<String> seen = new ArrayList<>();

        PhotoBatchUploadRunner.BatchResult result = runner.run(
                List.of("a", "b", "c", "d"),
                photoId -> {
                    seen.add(photoId);
                    if ("b".equals(photoId)) {
                        return PhotoBatchUploadRunner.AttemptResult.stopUncertain(
                                "remote state needs reconciliation");
                    }
                    return PhotoBatchUploadRunner.AttemptResult.confirmed(true);
                });

        assertEquals(List.of("a", "b"), seen);
        assertEquals(2, result.attemptedCount());
        assertEquals(1, result.confirmedCount());
        assertEquals(2, result.unattemptedCount());
        assertTrue(result.stoppedEarly());
        assertEquals("b", result.stoppedPhotoId());
        assertEquals(PhotoBatchUploadRunner.Outcome.STOP_UNCERTAIN, result.stopOutcome());
    }

    @Test
    public void unverifiedExceptionStopsBeforeLaterPhoto() {
        PhotoBatchUploadRunner runner = new PhotoBatchUploadRunner();
        List<String> seen = new ArrayList<>();

        PhotoBatchUploadRunner.BatchResult result = runner.run(
                List.of("a", "b", "c"),
                photoId -> {
                    seen.add(photoId);
                    if ("b".equals(photoId)) {
                        throw new IllegalStateException("state could not be verified");
                    }
                    return PhotoBatchUploadRunner.AttemptResult.confirmed(true);
                });

        assertEquals(List.of("a", "b"), seen);
        assertEquals(PhotoBatchUploadRunner.Outcome.STOP_UNVERIFIED, result.stopOutcome());
        assertEquals(1, result.unattemptedCount());
    }

    @Test
    public void duplicateSelectionIsRejectedBeforeAnyAttempt() {
        PhotoBatchUploadRunner runner = new PhotoBatchUploadRunner();
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(
                IllegalArgumentException.class,
                () -> runner.run(
                        List.of("a", "b", "a"),
                        photoId -> {
                            attempts.incrementAndGet();
                            return PhotoBatchUploadRunner.AttemptResult.confirmed(true);
                        }));

        assertEquals(0, attempts.get());
    }

    @Test
    public void confirmedUploadCanReportCleanupPendingWithoutStoppingBatch() {
        PhotoBatchUploadRunner runner = new PhotoBatchUploadRunner();

        PhotoBatchUploadRunner.BatchResult result = runner.run(
                List.of("a", "b"),
                photoId -> PhotoBatchUploadRunner.AttemptResult.confirmed(!"a".equals(photoId)));

        assertEquals(2, result.confirmedCount());
        assertEquals(1, result.cleanupPendingCount());
        assertFalse(result.stoppedEarly());
    }
}
