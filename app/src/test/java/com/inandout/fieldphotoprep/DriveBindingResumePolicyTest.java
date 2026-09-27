package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class DriveBindingResumePolicyTest {
    @Test
    public void blockedToUsableRequiresProviderReload() {
        assertTrue(DriveBindingResumePolicy.shouldReload(
                OrganizationDriveBindingGuard.State.WRONG_ORGANIZATION,
                OrganizationDriveBindingGuard.State.USABLE));
        assertTrue(DriveBindingResumePolicy.shouldReload(
                OrganizationDriveBindingGuard.State.AUTHORIZATION_REQUIRED,
                OrganizationDriveBindingGuard.State.USABLE));
        assertTrue(DriveBindingResumePolicy.shouldReload(
                OrganizationDriveBindingGuard.State.LEGACY_UNBOUND,
                OrganizationDriveBindingGuard.State.USABLE));
        assertTrue(DriveBindingResumePolicy.shouldReload(
                OrganizationDriveBindingGuard.State.PERMISSION_MISSING,
                OrganizationDriveBindingGuard.State.USABLE));
    }

    @Test
    public void ordinaryUsableResumeDoesNotReload() {
        assertFalse(DriveBindingResumePolicy.shouldReload(
                OrganizationDriveBindingGuard.State.USABLE,
                OrganizationDriveBindingGuard.State.USABLE));
    }

    @Test
    public void initialOrNoWorkspaceResumeDoesNotInventReload() {
        assertFalse(DriveBindingResumePolicy.shouldReload(
                null,
                OrganizationDriveBindingGuard.State.USABLE));
        assertFalse(DriveBindingResumePolicy.shouldReload(
                OrganizationDriveBindingGuard.State.NO_WORKSPACE,
                OrganizationDriveBindingGuard.State.USABLE));
    }

    @Test
    public void nonUsableCurrentStateNeverReloads() {
        assertFalse(DriveBindingResumePolicy.shouldReload(
                OrganizationDriveBindingGuard.State.WRONG_ORGANIZATION,
                OrganizationDriveBindingGuard.State.AUTHORIZATION_REQUIRED));
    }
}
