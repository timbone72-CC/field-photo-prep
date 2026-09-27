package com.inandout.fieldphotoprep;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Immutable, read-only Phase 12I view model.
 *
 * This object consumes already-owned runtime state. It does not persist, authorize, reconcile,
 * upload, or mutate Drive/queue/account state.
 */
final class AppStatusSnapshot {
    private static final DateTimeFormatter SUPPORT_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm'Z'")
                    .withZone(ZoneOffset.UTC);

    static final class QueueCounts {
        private final int capturing;
        private final int waiting;
        private final int uploading;
        private final int failed;
        private final int uncertain;
        private final int uploaded;
        private final int protectedOriginals;
        private final int cleanupPending;
        private final int unreadable;
        private final int signOutBlocking;
        private final boolean readable;

        QueueCounts(
                int capturing,
                int waiting,
                int uploading,
                int failed,
                int uncertain,
                int uploaded,
                int protectedOriginals,
                int cleanupPending,
                int unreadable) {
            this(
                    capturing,
                    waiting,
                    uploading,
                    failed,
                    uncertain,
                    uploaded,
                    protectedOriginals,
                    cleanupPending,
                    unreadable,
                    capturing + waiting + uploading + failed + uncertain + cleanupPending + unreadable,
                    true);
        }

        QueueCounts(
                int capturing,
                int waiting,
                int uploading,
                int failed,
                int uncertain,
                int uploaded,
                int protectedOriginals,
                int cleanupPending,
                int unreadable,
                int signOutBlocking) {
            this(
                    capturing,
                    waiting,
                    uploading,
                    failed,
                    uncertain,
                    uploaded,
                    protectedOriginals,
                    cleanupPending,
                    unreadable,
                    signOutBlocking,
                    true);
        }

        private QueueCounts(
                int capturing,
                int waiting,
                int uploading,
                int failed,
                int uncertain,
                int uploaded,
                int protectedOriginals,
                int cleanupPending,
                int unreadable,
                int signOutBlocking,
                boolean readable) {
            this.capturing = requireNonNegative(capturing, "capturing");
            this.waiting = requireNonNegative(waiting, "waiting");
            this.uploading = requireNonNegative(uploading, "uploading");
            this.failed = requireNonNegative(failed, "failed");
            this.uncertain = requireNonNegative(uncertain, "uncertain");
            this.uploaded = requireNonNegative(uploaded, "uploaded");
            this.protectedOriginals =
                    requireNonNegative(protectedOriginals, "protectedOriginals");
            this.cleanupPending = requireNonNegative(cleanupPending, "cleanupPending");
            this.unreadable = requireNonNegative(unreadable, "unreadable");
            this.signOutBlocking = requireNonNegative(signOutBlocking, "signOutBlocking");
            this.readable = readable;
        }

        static QueueCounts unavailable() {
            return new QueueCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        }

        int capturing() { return capturing; }
        int waiting() { return waiting; }
        int uploading() { return uploading; }
        int failed() { return failed; }
        int uncertain() { return uncertain; }
        int uploaded() { return uploaded; }
        int protectedOriginals() { return protectedOriginals; }
        int cleanupPending() { return cleanupPending; }
        int unreadable() { return unreadable; }
        int signOutBlocking() { return signOutBlocking; }
        boolean readable() { return readable; }

        int unresolved() {
            return capturing + waiting + uploading + failed + uncertain + unreadable;
        }

        private static int requireNonNegative(int value, String name) {
            if (value < 0) {
                throw new IllegalArgumentException(name + " cannot be negative");
            }
            return value;
        }
    }

    private final AuthorizationDecision.State authorizationState;
    private final OrganizationDriveBindingGuard.State driveState;
    private final String organizationName;
    private final String role;
    private final String membershipStatus;
    private final long lastValidatedAtEpochSeconds;
    private final long graceRemainingSeconds;
    private final String currentCompanyName;
    private final QueueCounts queueCounts;
    private final String appVersion;
    private final boolean cameraPermissionGranted;

    private AppStatusSnapshot(
            AuthorizationDecision.State authorizationState,
            OrganizationDriveBindingGuard.State driveState,
            String organizationName,
            String role,
            String membershipStatus,
            long lastValidatedAtEpochSeconds,
            long graceRemainingSeconds,
            String currentCompanyName,
            QueueCounts queueCounts,
            String appVersion,
            boolean cameraPermissionGranted) {
        this.authorizationState =
                Objects.requireNonNull(authorizationState, "authorizationState");
        this.driveState = Objects.requireNonNull(driveState, "driveState");
        this.organizationName = normalizeDisplay(organizationName);
        this.role = normalizeDisplay(role);
        this.membershipStatus = normalizeDisplay(membershipStatus);
        this.lastValidatedAtEpochSeconds = Math.max(0L, lastValidatedAtEpochSeconds);
        this.graceRemainingSeconds = Math.max(0L, graceRemainingSeconds);
        this.currentCompanyName = normalizeDisplay(currentCompanyName);
        this.queueCounts = Objects.requireNonNull(queueCounts, "queueCounts");
        this.appVersion = normalizeDisplay(appVersion);
        this.cameraPermissionGranted = cameraPermissionGranted;
    }

    static AppStatusSnapshot from(
            AuthorizationDecision decision,
            AuthSessionState session,
            OrganizationDriveBindingGuard.State driveState,
            String currentCompanyName,
            QueueCounts queueCounts,
            String appVersion,
            boolean cameraPermissionGranted) {
        AuthorizationDecision.State authState = decision == null
                ? AuthorizationDecision.State.SIGN_IN_REQUIRED
                : decision.state();
        String organizationName = session == null ? null : session.organizationName();
        String role = session == null ? null : session.role();
        String membershipStatus = session == null ? null : session.membershipStatus();
        long lastValidated = session == null
                ? 0L
                : session.lastMembershipValidatedAtEpochSeconds();
        long graceRemaining = decision == null ? 0L : decision.graceRemainingSeconds();

        return new AppStatusSnapshot(
                authState,
                driveState == null
                        ? OrganizationDriveBindingGuard.State.NO_WORKSPACE
                        : driveState,
                organizationName,
                role,
                membershipStatus,
                lastValidated,
                graceRemaining,
                currentCompanyName,
                queueCounts,
                appVersion,
                cameraPermissionGranted);
    }

    AuthorizationDecision.State authorizationState() { return authorizationState; }
    OrganizationDriveBindingGuard.State driveState() { return driveState; }
    String organizationName() { return organizationName; }
    String role() { return role; }
    String membershipStatus() { return membershipStatus; }
    long lastValidatedAtEpochSeconds() { return lastValidatedAtEpochSeconds; }
    long graceRemainingSeconds() { return graceRemainingSeconds; }
    String currentCompanyName() { return currentCompanyName; }
    QueueCounts queueCounts() { return queueCounts; }
    String appVersion() { return appVersion; }
    boolean cameraPermissionGranted() { return cameraPermissionGranted; }

    boolean requiresSignIn() {
        return authorizationState == AuthorizationDecision.State.SIGN_IN_REQUIRED;
    }

    boolean canConnectDrive() {
        return authorizationState == AuthorizationDecision.State.VALIDATED
                && driveState != OrganizationDriveBindingGuard.State.USABLE;
    }

    String accountLabel() {
        switch (authorizationState) {
            case VALIDATED:
                return "Validated";
            case GRACE:
                return "Offline grace";
            case RECHECK_REQUIRED:
                return "Recheck required";
            case SIGN_IN_REQUIRED:
                return "Sign in required";
            case NO_MEMBERSHIP:
                return "No active membership";
            case REVOKED:
                return "Membership revoked";
            case DRIVE_DISCONNECTED:
            default:
                return "Drive disconnected";
        }
    }

    String driveLabel() {
        switch (driveState) {
            case NO_WORKSPACE:
                return "Not connected";
            case AUTHORIZATION_REQUIRED:
                return "Account recheck required";
            case LEGACY_UNBOUND:
                return "Drive confirmation required";
            case INVALID_BINDING:
                return "Reconnect required";
            case WRONG_ORGANIZATION:
                return "Different organization";
            case PERMISSION_MISSING:
                return "Drive access expired";
            case USABLE:
            default:
                return "Connected";
        }
    }

    String graceRemainingLabel() {
        if (authorizationState != AuthorizationDecision.State.GRACE) {
            return "Not applicable";
        }
        if (graceRemainingSeconds <= 0L) {
            return "0 min";
        }
        long wholeMinutes = graceRemainingSeconds / 60L;
        if (wholeMinutes == 0L) {
            return "<1 min";
        }
        long hours = wholeMinutes / 60L;
        long minutes = wholeMinutes % 60L;
        if (hours == 0L) {
            return wholeMinutes + " min";
        }
        return hours + "h " + minutes + "m";
    }

    String lastValidationLabel() {
        if (lastValidatedAtEpochSeconds <= 0L) {
            return "Not validated";
        }
        long minuteFloor = (lastValidatedAtEpochSeconds / 60L) * 60L;
        return SUPPORT_TIME.format(Instant.ofEpochSecond(minuteFloor));
    }

    String supportSummary() {
        StringBuilder output = new StringBuilder();
        output.append("Field Photo Prep Support Status\n");
        output.append("App version: ").append(safeVersion()).append('\n');
        output.append("Account state: ").append(authorizationState.name()).append('\n');
        output.append("Role: ").append(safeRole()).append('\n');
        output.append("Grace remaining: ").append(graceRemainingLabel()).append('\n');
        output.append("Last validation: ").append(lastValidationLabel()).append('\n');
        output.append("Drive state: ").append(driveState.name()).append('\n');
        output.append("Camera permission: ")
                .append(cameraPermissionGranted ? "GRANTED" : "NOT_GRANTED")
                .append('\n');
        if (!queueCounts.readable()) {
            output.append("Queue status: UNAVAILABLE");
            return output.toString();
        }
        output.append("Queue CAPTURING: ").append(queueCounts.capturing()).append('\n');
        output.append("Queue WAITING: ").append(queueCounts.waiting()).append('\n');
        output.append("Queue UPLOADING: ").append(queueCounts.uploading()).append('\n');
        output.append("Queue FAILED: ").append(queueCounts.failed()).append('\n');
        output.append("Queue UNCERTAIN: ").append(queueCounts.uncertain()).append('\n');
        output.append("Queue UPLOADED: ").append(queueCounts.uploaded()).append('\n');
        output.append("Protected originals: ")
                .append(queueCounts.protectedOriginals())
                .append('\n');
        output.append("Cleanup pending: ").append(queueCounts.cleanupPending()).append('\n');
        output.append("Unreadable local records: ").append(queueCounts.unreadable());
        return output.toString();
    }

    private String safeRole() {
        if ("OWNER".equals(role) || "MEMBER".equals(role)) {
            return role;
        }
        return "UNKNOWN";
    }

    private String safeVersion() {
        if (appVersion != null && appVersion.matches("[A-Za-z0-9._+\\-]+")) {
            return appVersion;
        }
        return "unknown";
    }

    private static String normalizeDisplay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
