package com.inandout.fieldphotoprep;

import java.io.IOException;
import java.util.Objects;

/**
 * Small action boundary around the central runtime authorization decision.
 */
final class AuthorizationActionGuard {
    interface DecisionSource {
        AuthorizationDecision currentDecision();
    }

    private final DecisionSource decisionSource;

    AuthorizationActionGuard(RuntimeAuthorizationManager manager) {
        this(Objects.requireNonNull(manager, "manager")::currentDecision);
    }

    AuthorizationActionGuard(DecisionSource decisionSource) {
        this.decisionSource = Objects.requireNonNull(decisionSource, "decisionSource");
    }

    AuthorizationDecision currentDecision() {
        return decisionSource.currentDecision();
    }

    boolean allowsNewCapture() {
        return currentDecision().allowsNewCapture();
    }

    boolean allowsNewDriveMutation() {
        return currentDecision().allowsNewDriveMutation();
    }

    void requireNewCapture() throws IOException {
        AuthorizationDecision decision = currentDecision();
        if (!decision.allowsNewCapture()) {
            throw new IOException(blockedMessage(decision.state(), "take another photo"));
        }
    }

    void requireDriveMutation() throws IOException {
        AuthorizationDecision decision = currentDecision();
        if (!decision.allowsNewDriveMutation()) {
            throw new IOException(blockedMessage(decision.state(), "change Google Drive"));
        }
    }

    private static String blockedMessage(
            AuthorizationDecision.State state,
            String action) {
        switch (state) {
            case SIGN_IN_REQUIRED:
                return "Sign in to Field Photo Prep before you " + action + ".";
            case RECHECK_REQUIRED:
                return "Recheck the Field Photo Prep account before you " + action + ".";
            case REVOKED:
                return "This Field Photo Prep membership is revoked. Existing protected work was kept.";
            case NO_MEMBERSHIP:
                return "This account does not have an active Field Photo Prep membership.";
            case DRIVE_DISCONNECTED:
                return "The approved Drive workspace is not connected for this organization.";
            case VALIDATED:
            case GRACE:
            default:
                return "Field Photo Prep authorization does not allow this action right now.";
        }
    }
}
