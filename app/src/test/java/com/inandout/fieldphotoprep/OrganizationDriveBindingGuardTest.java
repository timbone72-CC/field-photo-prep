package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class OrganizationDriveBindingGuardTest {
    @Test
    public void noSavedWorkspaceIsDisconnected() {
        assertEquals(
                OrganizationDriveBindingGuard.State.NO_WORKSPACE,
                evaluate(validated("org-1"), false, false, null, 0, false).state());
    }

    @Test
    public void legacyUntaggedWorkspaceIsQuarantined() {
        assertEquals(
                OrganizationDriveBindingGuard.State.LEGACY_UNBOUND,
                evaluate(validated("org-1"), true, true, null, 0, true).state());
    }

    @Test
    public void exactOrganizationBindingIsReusable() {
        OrganizationDriveBindingGuard.Result result = evaluate(
                validated("org-1"),
                true,
                true,
                "org-1",
                FolderPrefs.ORGANIZATION_DRIVE_BINDING_VERSION,
                true);
        assertEquals(OrganizationDriveBindingGuard.State.USABLE, result.state());
        assertTrue(result.isUsable());
    }

    @Test
    public void differentOrganizationFailsClosed() {
        assertEquals(
                OrganizationDriveBindingGuard.State.WRONG_ORGANIZATION,
                evaluate(
                        validated("org-2"),
                        true,
                        true,
                        "org-1",
                        FolderPrefs.ORGANIZATION_DRIVE_BINDING_VERSION,
                        true).state());
    }

    @Test
    public void unsupportedOrPartialBindingFailsClosed() {
        assertEquals(
                OrganizationDriveBindingGuard.State.INVALID_BINDING,
                evaluate(validated("org-1"), true, true, "org-1", 99, true).state());
        assertEquals(
                OrganizationDriveBindingGuard.State.INVALID_BINDING,
                evaluate(
                        validated("org-1"),
                        true,
                        false,
                        "org-1",
                        FolderPrefs.ORGANIZATION_DRIVE_BINDING_VERSION,
                        true).state());
    }

    @Test
    public void missingPersistedPermissionFailsClosed() {
        assertEquals(
                OrganizationDriveBindingGuard.State.PERMISSION_MISSING,
                evaluate(
                        validated("org-1"),
                        true,
                        true,
                        "org-1",
                        FolderPrefs.ORGANIZATION_DRIVE_BINDING_VERSION,
                        false).state());
    }

    @Test
    public void graceMayReuseExistingBinding() {
        AuthorizationDecision grace = new AuthorizationDecision(
                AuthorizationDecision.State.GRACE,
                "user-1",
                "org-1",
                "MEMBER",
                60L);
        assertEquals(
                OrganizationDriveBindingGuard.State.USABLE,
                evaluate(
                        grace,
                        true,
                        true,
                        "org-1",
                        FolderPrefs.ORGANIZATION_DRIVE_BINDING_VERSION,
                        true).state());
    }

    @Test
    public void revokedStateCannotUseBinding() {
        AuthorizationDecision revoked = new AuthorizationDecision(
                AuthorizationDecision.State.REVOKED,
                "user-1",
                "org-1",
                "MEMBER",
                0L);
        assertEquals(
                OrganizationDriveBindingGuard.State.AUTHORIZATION_REQUIRED,
                evaluate(
                        revoked,
                        true,
                        true,
                        "org-1",
                        FolderPrefs.ORGANIZATION_DRIVE_BINDING_VERSION,
                        true).state());
    }

    private static AuthorizationDecision validated(String organizationId) {
        return new AuthorizationDecision(
                AuthorizationDecision.State.VALIDATED,
                "user-1",
                organizationId,
                "OWNER",
                0L);
    }

    private static OrganizationDriveBindingGuard.Result evaluate(
            AuthorizationDecision decision,
            boolean hasTreeUri,
            boolean hasRootIdentity,
            String boundOrganizationId,
            int version,
            boolean hasReadPermission) {
        return OrganizationDriveBindingGuard.evaluate(
                decision,
                hasTreeUri,
                hasRootIdentity,
                boundOrganizationId,
                version,
                hasReadPermission);
    }
}
