package com.inandout.fieldphotoprep;

import java.util.Objects;

/**
 * Pure Phase 12J presentation policy over already-owned authorization, Drive, and local-work state.
 *
 * This policy never authorizes, persists, retries, reconciles, uploads, deletes, signs out, or
 * selects a Drive workspace. It only explains the next safe operator step.
 */
final class RecoveryGuidancePolicy {
    enum Action {
        SIGN_IN,
        RECHECK_ACCOUNT,
        CONTACT_OWNER,
        CONNECT_DRIVE,
        OPEN_PHOTOS,
        NONE
    }

    static final class Guidance {
        private final String safeNow;
        private final String blockedNow;
        private final String protectedData;
        private final Action action;
        private final String actionLabel;

        Guidance(
                String safeNow,
                String blockedNow,
                String protectedData,
                Action action,
                String actionLabel) {
            this.safeNow = Objects.requireNonNull(safeNow, "safeNow");
            this.blockedNow = Objects.requireNonNull(blockedNow, "blockedNow");
            this.protectedData = Objects.requireNonNull(protectedData, "protectedData");
            this.action = Objects.requireNonNull(action, "action");
            this.actionLabel = actionLabel;
            if (action == Action.NONE && actionLabel != null) {
                throw new IllegalArgumentException("NONE action cannot expose a label.");
            }
            if (action != Action.NONE && (actionLabel == null || actionLabel.isBlank())) {
                throw new IllegalArgumentException("Action label is required.");
            }
        }

        String safeNow() { return safeNow; }
        String blockedNow() { return blockedNow; }
        String protectedData() { return protectedData; }
        Action action() { return action; }
        String actionLabel() { return actionLabel; }
    }

    private RecoveryGuidancePolicy() {}

    static Guidance from(AppStatusSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        AuthorizationDecision.State auth = snapshot.authorizationState();
        AppStatusSnapshot.QueueCounts queue = snapshot.queueCounts();

        if (auth == AuthorizationDecision.State.SIGN_IN_REQUIRED) {
            return guidance(
                    "You can review App Status and protected local work.",
                    "New field work and Drive changes require Field Photo Prep sign-in.",
                    protectionText(queue),
                    Action.SIGN_IN,
                    "Sign In");
        }

        if (auth == AuthorizationDecision.State.REVOKED) {
            return guidance(
                    "Existing protected local work remains preserved for safe recovery.",
                    "New field work and Drive writes are blocked because this Membership is revoked.",
                    protectionText(queue),
                    Action.CONTACT_OWNER,
                    "Contact Organization Owner");
        }

        if (auth == AuthorizationDecision.State.NO_MEMBERSHIP) {
            return guidance(
                    "Existing protected local work remains preserved for safe recovery.",
                    "New field work and Drive writes are blocked because no ACTIVE Membership is available.",
                    protectionText(queue),
                    Action.CONTACT_OWNER,
                    "Contact Organization Owner");
        }

        if (auth == AuthorizationDecision.State.RECHECK_REQUIRED
                || auth == AuthorizationDecision.State.DRIVE_DISCONNECTED) {
            return guidance(
                    "You can review protected local work and current app status.",
                    "New field work and Drive mutations are blocked until the account is revalidated.",
                    protectionText(queue),
                    Action.RECHECK_ACCOUNT,
                    "Recheck Account");
        }

        if (!queue.readable() || queue.unreadable() > 0) {
            return guidance(
                    "No recovery action has changed or deleted local work.",
                    "Some protected local photo records cannot be read safely.",
                    "Protected local work is being left unchanged. Do not delete app data to clear this state.",
                    Action.NONE,
                    null);
        }

        if (queue.uncertain() > 0) {
            return guidance(
                    "Other protected local work remains available for review.",
                    "At least one upload has an uncertain remote result. Do not retry it blindly.",
                    protectionText(queue),
                    Action.OPEN_PHOTOS,
                    "Open Photos");
        }

        OrganizationDriveBindingGuard.State drive = snapshot.driveState();
        if (drive != OrganizationDriveBindingGuard.State.USABLE) {
            if (auth != AuthorizationDecision.State.VALIDATED) {
                return guidance(
                        "Existing protected work and the saved Drive identity remain preserved.",
                        "Drive recovery waits for an online validated Field Photo Prep account.",
                        protectionText(queue),
                        Action.RECHECK_ACCOUNT,
                        "Recheck Account");
            }
            return guidance(
                    "Existing protected local work and saved destination identities remain unchanged.",
                    driveBlockedText(drive),
                    protectionText(queue),
                    Action.CONNECT_DRIVE,
                    "Connect Drive");
        }

        if (auth == AuthorizationDecision.State.GRACE) {
            return guidance(
                    "Existing field work for this Organization remains available during the displayed offline grace window.",
                    "Membership administration, Organization switching, and Drive rebinding require online validation.",
                    protectionText(queue),
                    Action.RECHECK_ACCOUNT,
                    "Recheck Account");
        }

        if (queue.failed() > 0 || queue.waiting() > 0 || queue.uploading() > 0
                || queue.capturing() > 0 || queue.cleanupPending() > 0) {
            return guidance(
                    "Your account and Drive workspace are available.",
                    "Protected local photo work still needs normal completion or cleanup.",
                    protectionText(queue),
                    Action.OPEN_PHOTOS,
                    "Open Photos");
        }

        return guidance(
                "Your Field Photo Prep account and Drive workspace are ready.",
                "Nothing is currently blocked.",
                "No unresolved protected local photo work is reported.",
                Action.NONE,
                null);
    }

    private static Guidance guidance(
            String safeNow,
            String blockedNow,
            String protectedData,
            Action action,
            String label) {
        return new Guidance(safeNow, blockedNow, protectedData, action, label);
    }

    private static String protectionText(AppStatusSnapshot.QueueCounts queue) {
        if (!queue.readable()) {
            return "Local queue status is unavailable. Protected work was left unchanged.";
        }

        int unresolved = queue.unresolved();
        int cleanup = queue.cleanupPending();
        if (unresolved <= 0 && cleanup <= 0 && queue.protectedOriginals() <= 0) {
            return "No unresolved protected local photo work is reported.";
        }

        return "Protected local work remains on this device: "
                + unresolved + " unresolved record"
                + (unresolved == 1 ? "" : "s")
                + ", " + queue.protectedOriginals() + " protected original"
                + (queue.protectedOriginals() == 1 ? "" : "s")
                + ", and " + cleanup + " confirmed-upload cleanup item"
                + (cleanup == 1 ? "" : "s")
                + ". Account or Drive recovery will not delete or reassign it.";
    }

    private static String driveBlockedText(OrganizationDriveBindingGuard.State state) {
        switch (state) {
            case NO_WORKSPACE:
                return "Google Drive is not connected for this Organization.";
            case AUTHORIZATION_REQUIRED:
                return "Drive use is blocked until the Field Photo Prep account is revalidated.";
            case LEGACY_UNBOUND:
                return "The saved Drive workspace must be confirmed for this Organization.";
            case INVALID_BINDING:
                return "The saved Drive workspace binding is incomplete and must be reconnected.";
            case WRONG_ORGANIZATION:
                return "The saved Drive workspace belongs to a different Field Photo Prep Organization.";
            case PERMISSION_MISSING:
                return "Android no longer has the persisted read access needed for this Drive workspace.";
            case USABLE:
            default:
                return "Google Drive is available.";
        }
    }
}
