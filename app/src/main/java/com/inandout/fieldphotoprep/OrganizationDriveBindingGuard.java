package com.inandout.fieldphotoprep;

import android.net.Uri;

import java.io.IOException;
import java.util.Objects;

/**
 * Shared Phase 12H boundary between authoritative FPP Organization identity and the local
 * provider-bound Drive workspace.
 */
final class OrganizationDriveBindingGuard {
    enum State {
        NO_WORKSPACE,
        AUTHORIZATION_REQUIRED,
        LEGACY_UNBOUND,
        INVALID_BINDING,
        WRONG_ORGANIZATION,
        PERMISSION_MISSING,
        USABLE
    }

    static final class Result {
        private final State state;

        Result(State state) {
            this.state = Objects.requireNonNull(state, "state");
        }

        State state() { return state; }
        boolean isUsable() { return state == State.USABLE; }

        String message() {
            switch (state) {
                case NO_WORKSPACE:
                    return "Google Drive is not connected.";
                case AUTHORIZATION_REQUIRED:
                    return "Recheck the Field Photo Prep account before connecting or using Google Drive.";
                case LEGACY_UNBOUND:
                    return "Confirm the saved Drive workspace for this Field Photo Prep organization.";
                case INVALID_BINDING:
                    return "The saved Drive workspace binding is incomplete. Reconnect the workspace.";
                case WRONG_ORGANIZATION:
                    return "The saved Drive workspace belongs to a different Field Photo Prep organization. Connect this organization's workspace.";
                case PERMISSION_MISSING:
                    return "Drive access expired. Reconnect the workspace.";
                case USABLE:
                default:
                    return "";
            }
        }
    }

    interface ReadPermissionChecker {
        boolean hasPersistedReadPermission(Uri treeUri);
    }

    private final FolderPrefs folderPrefs;
    private final AuthorizationActionGuard authorizationGuard;
    private final ReadPermissionChecker readPermissionChecker;

    OrganizationDriveBindingGuard(
            FolderPrefs folderPrefs,
            AuthorizationActionGuard authorizationGuard,
            ReadPermissionChecker readPermissionChecker) {
        this.folderPrefs = Objects.requireNonNull(folderPrefs, "folderPrefs");
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard, "authorizationGuard");
        this.readPermissionChecker =
                Objects.requireNonNull(readPermissionChecker, "readPermissionChecker");
    }

    Result current() {
        FolderPrefs.DriveBinding binding = folderPrefs.getDriveBinding();
        Uri treeUri = binding.treeUri();
        return evaluate(
                authorizationGuard.currentDecision(),
                treeUri != null,
                binding.rootFolder() != null,
                binding.organizationId(),
                binding.version(),
                treeUri != null && readPermissionChecker.hasPersistedReadPermission(treeUri));
    }

    Uri requireUsableTreeUri() throws IOException {
        FolderPrefs.DriveBinding binding = folderPrefs.getDriveBinding();
        Uri treeUri = binding.treeUri();
        Result result = evaluate(
                authorizationGuard.currentDecision(),
                treeUri != null,
                binding.rootFolder() != null,
                binding.organizationId(),
                binding.version(),
                treeUri != null && readPermissionChecker.hasPersistedReadPermission(treeUri));
        if (!result.isUsable() || treeUri == null) {
            throw new IOException(result.message());
        }
        return treeUri;
    }

    String requireValidatedOrganizationForBinding() throws IOException {
        AuthorizationDecision decision = authorizationGuard.currentDecision();
        if (decision.state() != AuthorizationDecision.State.VALIDATED
                || decision.organizationId() == null) {
            throw new IOException(
                    "Recheck the Field Photo Prep account online before connecting a Drive workspace.");
        }
        return decision.organizationId();
    }

    static Result evaluate(
            AuthorizationDecision decision,
            boolean hasTreeUri,
            boolean hasRootIdentity,
            String boundOrganizationId,
            int bindingVersion,
            boolean hasPersistedReadPermission) {
        String normalizedBoundOrganizationId = normalize(boundOrganizationId);
        boolean noBindingMetadata =
                normalizedBoundOrganizationId == null && bindingVersion == 0;

        if (!hasTreeUri && !hasRootIdentity && noBindingMetadata) {
            return new Result(State.NO_WORKSPACE);
        }
        if (hasTreeUri != hasRootIdentity || !hasTreeUri) {
            return new Result(State.INVALID_BINDING);
        }
        if (noBindingMetadata) {
            return new Result(State.LEGACY_UNBOUND);
        }
        if (normalizedBoundOrganizationId == null
                || bindingVersion != FolderPrefs.ORGANIZATION_DRIVE_BINDING_VERSION) {
            return new Result(State.INVALID_BINDING);
        }
        if (decision == null
                || !decision.allowsNewDriveMutation()
                || decision.organizationId() == null) {
            return new Result(State.AUTHORIZATION_REQUIRED);
        }
        if (!normalizedBoundOrganizationId.equals(decision.organizationId())) {
            return new Result(State.WRONG_ORGANIZATION);
        }
        if (!hasPersistedReadPermission) {
            return new Result(State.PERMISSION_MISSING);
        }
        return new Result(State.USABLE);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
