package com.inandout.fieldphotoprep;

import java.util.Objects;

/**
 * Immutable application-level authorization result for one evaluated instant.
 *
 * UI controls may consume this object, but protected mutation entry points must re-evaluate
 * authorization immediately before starting their work.
 */
final class AuthorizationDecision {
    enum State {
        VALIDATED,
        GRACE,
        RECHECK_REQUIRED,
        SIGN_IN_REQUIRED,
        NO_MEMBERSHIP,
        REVOKED,
        DRIVE_DISCONNECTED
    }

    private final State state;
    private final String userId;
    private final String organizationId;
    private final String role;
    private final long graceRemainingSeconds;

    AuthorizationDecision(
            State state,
            String userId,
            String organizationId,
            String role,
            long graceRemainingSeconds) {
        this.state = Objects.requireNonNull(state, "state");
        this.userId = normalize(userId);
        this.organizationId = normalize(organizationId);
        this.role = normalize(role);
        this.graceRemainingSeconds = Math.max(0L, graceRemainingSeconds);
    }

    State state() {
        return state;
    }

    String userId() {
        return userId;
    }

    String organizationId() {
        return organizationId;
    }

    String role() {
        return role;
    }

    long graceRemainingSeconds() {
        return graceRemainingSeconds;
    }

    boolean allowsNewCapture() {
        return state == State.VALIDATED || state == State.GRACE;
    }

    boolean allowsNewDriveMutation() {
        return state == State.VALIDATED || state == State.GRACE;
    }

    boolean allowsMemberAdministration() {
        return state == State.VALIDATED && "OWNER".equals(role);
    }

    boolean allowsReadAndRecovery() {
        return true;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
