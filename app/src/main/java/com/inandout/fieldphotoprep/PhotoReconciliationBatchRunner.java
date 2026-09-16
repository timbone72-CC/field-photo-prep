package com.inandout.fieldphotoprep;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Sequential orchestration for read-only remote reconciliation. The runner owns no Drive logic and
 * grants no retry authority; every item delegates to the existing per-photo reconciliation path.
 */
public final class PhotoReconciliationBatchRunner {
    public enum Outcome {
        CONFIRMED_MATCH,
        RETRY_SAFE_ABSENT,
        REMAIN_UNCERTAIN,
        ERROR
    }

    public interface Attempt {
        AttemptResult reconcile(String photoId) throws Exception;
    }

    public static final class AttemptResult {
        private final Outcome outcome;
        private final boolean cleanupComplete;
        private final String detail;

        private AttemptResult(Outcome outcome, boolean cleanupComplete, String detail) {
            this.outcome = Objects.requireNonNull(outcome, "outcome");
            this.cleanupComplete = cleanupComplete;
            this.detail = normalizeDetail(detail);
            if (outcome != Outcome.CONFIRMED_MATCH && cleanupComplete) {
                throw new IllegalArgumentException(
                        "Only a confirmed reconciliation may report confirmed-photo cleanup.");
            }
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

        public static AttemptResult confirmed(boolean cleanupComplete) {
            return new AttemptResult(Outcome.CONFIRMED_MATCH, cleanupComplete, null);
        }

        public static AttemptResult retrySafe() {
            return new AttemptResult(Outcome.RETRY_SAFE_ABSENT, false, null);
        }

        public static AttemptResult uncertain(String detail) {
            return new AttemptResult(Outcome.REMAIN_UNCERTAIN, false, detail);
        }

        public static AttemptResult error(String detail) {
            return new AttemptResult(Outcome.ERROR, false, detail);
        }
    }

    public static final class BatchResult {
        private final int selectedCount;
        private final int confirmedCount;
        private final int retrySafeCount;
        private final int uncertainCount;
        private final int errorCount;
        private final int cleanupPendingCount;

        private BatchResult(
                int selectedCount,
                int confirmedCount,
                int retrySafeCount,
                int uncertainCount,
                int errorCount,
                int cleanupPendingCount) {
            this.selectedCount = selectedCount;
            this.confirmedCount = confirmedCount;
            this.retrySafeCount = retrySafeCount;
            this.uncertainCount = uncertainCount;
            this.errorCount = errorCount;
            this.cleanupPendingCount = cleanupPendingCount;
        }

        public int selectedCount() {
            return selectedCount;
        }

        public int checkedCount() {
            return confirmedCount + retrySafeCount + uncertainCount + errorCount;
        }

        public int confirmedCount() {
            return confirmedCount;
        }

        public int retrySafeCount() {
            return retrySafeCount;
        }

        public int uncertainCount() {
            return uncertainCount;
        }

        public int errorCount() {
            return errorCount;
        }

        public int cleanupPendingCount() {
            return cleanupPendingCount;
        }

        public int unresolvedCount() {
            return uncertainCount + errorCount;
        }
    }

    public BatchResult run(List<String> photoIds, Attempt attempt) {
        Objects.requireNonNull(photoIds, "photoIds");
        Objects.requireNonNull(attempt, "attempt");
        validateSnapshot(photoIds);

        int confirmed = 0;
        int retrySafe = 0;
        int uncertain = 0;
        int errors = 0;
        int cleanupPending = 0;

        for (String photoId : photoIds) {
            final AttemptResult result;
            try {
                AttemptResult attempted = attempt.reconcile(photoId);
                result = attempted == null
                        ? AttemptResult.error("Reconciliation returned no result.")
                        : attempted;
            } catch (Exception error) {
                result = AttemptResult.error(errorDetail(error));
            }

            switch (result.outcome()) {
                case CONFIRMED_MATCH:
                    confirmed++;
                    if (!result.cleanupComplete()) {
                        cleanupPending++;
                    }
                    break;
                case RETRY_SAFE_ABSENT:
                    retrySafe++;
                    break;
                case REMAIN_UNCERTAIN:
                    uncertain++;
                    break;
                case ERROR:
                default:
                    errors++;
                    break;
            }
        }

        return new BatchResult(
                photoIds.size(),
                confirmed,
                retrySafe,
                uncertain,
                errors,
                cleanupPending);
    }

    private static void validateSnapshot(List<String> photoIds) {
        Set<String> unique = new HashSet<>();
        for (String photoId : photoIds) {
            if (photoId == null || photoId.isBlank()) {
                throw new IllegalArgumentException(
                        "Bulk reconciliation snapshot contains a blank photo identity.");
            }
            if (!unique.add(photoId)) {
                throw new IllegalArgumentException(
                        "Bulk reconciliation snapshot contains a duplicate photo identity.");
            }
        }
    }

    private static String errorDetail(Exception error) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.isBlank()
                ? "Reconciliation failed without a readable detail."
                : message;
    }

    private static String normalizeDetail(String detail) {
        if (detail == null) {
            return null;
        }
        String trimmed = detail.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
