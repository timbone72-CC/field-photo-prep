package com.inandout.fieldphotoprep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Runs an operator-selected batch strictly one photo at a time.
 *
 * The per-photo callback still owns the actual upload protocol. This class only owns deterministic
 * sequencing and the rule that ambiguous/unverified results stop later remote writes.
 */
public final class PhotoBatchUploadRunner {
    public enum Outcome {
        CONFIRMED,
        SAFE_FAILURE,
        STOP_UNCERTAIN,
        STOP_UNVERIFIED
    }

    public interface Attempt {
        AttemptResult attempt(String photoId) throws Exception;
    }

    public static final class AttemptResult {
        private final Outcome outcome;
        private final boolean cleanupComplete;
        private final String detail;

        private AttemptResult(Outcome outcome, boolean cleanupComplete, String detail) {
            this.outcome = Objects.requireNonNull(outcome, "outcome");
            this.cleanupComplete = cleanupComplete;
            this.detail = detail == null || detail.isBlank() ? null : detail;
        }

        public static AttemptResult confirmed(boolean cleanupComplete) {
            return new AttemptResult(Outcome.CONFIRMED, cleanupComplete, null);
        }

        public static AttemptResult safeFailure(String detail) {
            return new AttemptResult(Outcome.SAFE_FAILURE, false, requireDetail(detail));
        }

        public static AttemptResult stopUncertain(String detail) {
            return new AttemptResult(Outcome.STOP_UNCERTAIN, false, requireDetail(detail));
        }

        public static AttemptResult stopUnverified(String detail) {
            return new AttemptResult(Outcome.STOP_UNVERIFIED, false, requireDetail(detail));
        }

        public Outcome outcome() {
            return outcome;
        }

        public boolean cleanupComplete() {
            return cleanupComplete;
        }

        public String detail() {
            return detail;
        }

        private static String requireDetail(String detail) {
            if (detail == null || detail.isBlank()) {
                throw new IllegalArgumentException("detail is required for a non-confirmed batch result.");
            }
            return detail;
        }
    }

    public static final class BatchResult {
        private final int selectedCount;
        private final int attemptedCount;
        private final List<String> confirmedPhotoIds;
        private final List<String> safeFailurePhotoIds;
        private final int cleanupPendingCount;
        private final String stoppedPhotoId;
        private final Outcome stopOutcome;
        private final String stopDetail;

        BatchResult(
                int selectedCount,
                int attemptedCount,
                List<String> confirmedPhotoIds,
                List<String> safeFailurePhotoIds,
                int cleanupPendingCount,
                String stoppedPhotoId,
                Outcome stopOutcome,
                String stopDetail) {
            this.selectedCount = selectedCount;
            this.attemptedCount = attemptedCount;
            this.confirmedPhotoIds = List.copyOf(confirmedPhotoIds);
            this.safeFailurePhotoIds = List.copyOf(safeFailurePhotoIds);
            this.cleanupPendingCount = cleanupPendingCount;
            this.stoppedPhotoId = stoppedPhotoId;
            this.stopOutcome = stopOutcome;
            this.stopDetail = stopDetail;
        }

        public int selectedCount() {
            return selectedCount;
        }

        public int attemptedCount() {
            return attemptedCount;
        }

        public int confirmedCount() {
            return confirmedPhotoIds.size();
        }

        public List<String> confirmedPhotoIds() {
            return confirmedPhotoIds;
        }

        public int safeFailureCount() {
            return safeFailurePhotoIds.size();
        }

        public List<String> safeFailurePhotoIds() {
            return safeFailurePhotoIds;
        }

        public int cleanupPendingCount() {
            return cleanupPendingCount;
        }

        public int unattemptedCount() {
            return selectedCount - attemptedCount;
        }

        public boolean stoppedEarly() {
            return stopOutcome != null;
        }

        public String stoppedPhotoId() {
            return stoppedPhotoId;
        }

        public Outcome stopOutcome() {
            return stopOutcome;
        }

        public String stopDetail() {
            return stopDetail;
        }
    }

    public BatchResult run(List<String> photoIds, Attempt attempt) {
        Objects.requireNonNull(photoIds, "photoIds");
        Objects.requireNonNull(attempt, "attempt");
        validateUniquePhotoIds(photoIds);

        List<String> confirmed = new ArrayList<>();
        List<String> safeFailures = new ArrayList<>();
        int cleanupPending = 0;
        int attempted = 0;
        String stoppedPhotoId = null;
        Outcome stopOutcome = null;
        String stopDetail = null;

        for (String photoId : photoIds) {
            attempted++;
            final AttemptResult result;
            try {
                result = Objects.requireNonNull(
                        attempt.attempt(photoId),
                        "batch attempt result");
            } catch (Exception error) {
                stoppedPhotoId = photoId;
                stopOutcome = Outcome.STOP_UNVERIFIED;
                String message = error.getMessage();
                stopDetail = message == null || message.isBlank()
                        ? "The batch could not verify the active photo result safely."
                        : message;
                break;
            }

            switch (result.outcome()) {
                case CONFIRMED:
                    confirmed.add(photoId);
                    if (!result.cleanupComplete()) {
                        cleanupPending++;
                    }
                    break;
                case SAFE_FAILURE:
                    safeFailures.add(photoId);
                    break;
                case STOP_UNCERTAIN:
                case STOP_UNVERIFIED:
                    stoppedPhotoId = photoId;
                    stopOutcome = result.outcome();
                    stopDetail = result.detail();
                    break;
                default:
                    throw new IllegalStateException("Unsupported batch outcome.");
            }

            if (stopOutcome != null) {
                break;
            }
        }

        return new BatchResult(
                photoIds.size(),
                attempted,
                confirmed,
                safeFailures,
                cleanupPending,
                stoppedPhotoId,
                stopOutcome,
                stopDetail);
    }

    private static void validateUniquePhotoIds(List<String> photoIds) {
        Set<String> seen = new HashSet<>();
        for (String photoId : photoIds) {
            if (photoId == null || photoId.isBlank()) {
                throw new IllegalArgumentException("Every selected photo ID is required.");
            }
            if (!seen.add(photoId)) {
                throw new IllegalArgumentException(
                        "A selected batch cannot contain the same photo more than once: " + photoId);
            }
        }
    }
}
