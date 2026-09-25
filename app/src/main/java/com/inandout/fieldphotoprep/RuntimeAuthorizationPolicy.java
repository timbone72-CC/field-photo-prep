package com.inandout.fieldphotoprep;

import java.util.Objects;

/**
 * Pure Phase 12E account-authorization policy.
 *
 * This class owns no network, storage, Drive, or UI work. Callers provide the stored validated
 * identity, the latest authoritative/indeterminate account observation, and trusted clock
 * evidence. The policy then returns one immutable decision.
 */
final class RuntimeAuthorizationPolicy {
    static final long GRACE_SECONDS = 72L * 60L * 60L;

    enum ObservationKind {
        NONE,
        AUTHORITATIVE_ACTIVE,
        AUTHORITATIVE_REVOKED,
        AUTHORITATIVE_NO_MEMBERSHIP,
        AUTHORITATIVE_IDENTITY_MISMATCH,
        INDETERMINATE
    }

    static final class Observation {
        private final ObservationKind kind;
        private final String userId;
        private final String organizationId;
        private final String membershipId;
        private final String role;

        private Observation(
                ObservationKind kind,
                String userId,
                String organizationId,
                String membershipId,
                String role) {
            this.kind = Objects.requireNonNull(kind, "kind");
            this.userId = normalize(userId);
            this.organizationId = normalize(organizationId);
            this.membershipId = normalize(membershipId);
            this.role = normalize(role);
        }

        static Observation none() {
            return new Observation(ObservationKind.NONE, null, null, null, null);
        }

        static Observation indeterminate() {
            return new Observation(ObservationKind.INDETERMINATE, null, null, null, null);
        }

        static Observation active(
                String userId,
                String organizationId,
                String membershipId,
                String role) {
            return new Observation(
                    ObservationKind.AUTHORITATIVE_ACTIVE,
                    userId,
                    organizationId,
                    membershipId,
                    role);
        }

        static Observation revoked(
                String userId,
                String organizationId,
                String membershipId) {
            return new Observation(
                    ObservationKind.AUTHORITATIVE_REVOKED,
                    userId,
                    organizationId,
                    membershipId,
                    null);
        }

        static Observation noMembership() {
            return new Observation(
                    ObservationKind.AUTHORITATIVE_NO_MEMBERSHIP,
                    null,
                    null,
                    null,
                    null);
        }

        static Observation identityMismatch() {
            return new Observation(
                    ObservationKind.AUTHORITATIVE_IDENTITY_MISMATCH,
                    null,
                    null,
                    null,
                    null);
        }

        ObservationKind kind() {
            return kind;
        }
    }

    AuthorizationDecision evaluate(
            AuthSessionState stored,
            Observation observation,
            long nowEpochSeconds,
            Long monotonicElapsedSinceValidationSeconds) {
        Observation current = observation == null ? Observation.none() : observation;

        if (stored == null) {
            return decision(AuthorizationDecision.State.SIGN_IN_REQUIRED, null, 0L);
        }

        switch (current.kind) {
            case AUTHORITATIVE_ACTIVE:
                if (!sameIdentity(stored, current) || !isOwnerOrMember(current.role)) {
                    return decision(AuthorizationDecision.State.NO_MEMBERSHIP, stored, 0L);
                }
                return new AuthorizationDecision(
                        AuthorizationDecision.State.VALIDATED,
                        stored.userId(),
                        stored.organizationId(),
                        current.role,
                        0L);

            case AUTHORITATIVE_REVOKED:
                if (sameIdentity(stored, current)) {
                    return decision(AuthorizationDecision.State.REVOKED, stored, 0L);
                }
                return decision(AuthorizationDecision.State.NO_MEMBERSHIP, stored, 0L);

            case AUTHORITATIVE_NO_MEMBERSHIP:
            case AUTHORITATIVE_IDENTITY_MISMATCH:
                return decision(AuthorizationDecision.State.NO_MEMBERSHIP, stored, 0L);

            case INDETERMINATE:
            case NONE:
            default:
                break;
        }

        if ("REVOKED".equals(stored.membershipStatus())) {
            return decision(AuthorizationDecision.State.REVOKED, stored, 0L);
        }
        if (!stored.isActiveOwnerOrMember()) {
            return decision(AuthorizationDecision.State.NO_MEMBERSHIP, stored, 0L);
        }

        GraceResult grace = evaluateGrace(
                stored.lastMembershipValidatedAtEpochSeconds(),
                nowEpochSeconds,
                monotonicElapsedSinceValidationSeconds);
        if (!grace.allowed) {
            return decision(AuthorizationDecision.State.RECHECK_REQUIRED, stored, 0L);
        }
        return decision(AuthorizationDecision.State.GRACE, stored, grace.remainingSeconds);
    }

    private GraceResult evaluateGrace(
            long lastValidatedAtEpochSeconds,
            long nowEpochSeconds,
            Long monotonicElapsedSinceValidationSeconds) {
        if (lastValidatedAtEpochSeconds <= 0L || nowEpochSeconds <= 0L) {
            return GraceResult.blocked();
        }

        long wallElapsed = nowEpochSeconds - lastValidatedAtEpochSeconds;
        if (wallElapsed < 0L) {
            return GraceResult.blocked();
        }

        long elapsed;
        if (monotonicElapsedSinceValidationSeconds != null) {
            elapsed = monotonicElapsedSinceValidationSeconds;
            if (elapsed < 0L) {
                return GraceResult.blocked();
            }
        } else {
            elapsed = wallElapsed;
        }

        if (elapsed >= GRACE_SECONDS) {
            return GraceResult.blocked();
        }
        return GraceResult.allowed(GRACE_SECONDS - elapsed);
    }

    private static boolean sameIdentity(AuthSessionState stored, Observation observation) {
        return stored.userId().equals(observation.userId)
                && stored.organizationId().equals(observation.organizationId)
                && stored.membershipId().equals(observation.membershipId);
    }

    private static boolean isOwnerOrMember(String role) {
        return "OWNER".equals(role) || "MEMBER".equals(role);
    }

    private static AuthorizationDecision decision(
            AuthorizationDecision.State state,
            AuthSessionState stored,
            long graceRemainingSeconds) {
        return new AuthorizationDecision(
                state,
                stored == null ? null : stored.userId(),
                stored == null ? null : stored.organizationId(),
                stored == null ? null : stored.role(),
                graceRemainingSeconds);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static final class GraceResult {
        private final boolean allowed;
        private final long remainingSeconds;

        private GraceResult(boolean allowed, long remainingSeconds) {
            this.allowed = allowed;
            this.remainingSeconds = remainingSeconds;
        }

        static GraceResult allowed(long remainingSeconds) {
            return new GraceResult(true, remainingSeconds);
        }

        static GraceResult blocked() {
            return new GraceResult(false, 0L);
        }
    }
}
