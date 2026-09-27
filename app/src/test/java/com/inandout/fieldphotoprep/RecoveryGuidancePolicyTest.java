package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RecoveryGuidancePolicyTest {
    @Test
    public void signInRequiredOutranksDriveAndQueueRecovery() {
        RecoveryGuidancePolicy.Guidance guidance = RecoveryGuidancePolicy.from(
                snapshot(
                        AuthorizationDecision.State.SIGN_IN_REQUIRED,
                        OrganizationDriveBindingGuard.State.PERMISSION_MISSING,
                        counts(0, 2, 0, 1, 1, 0, 3, 0, 0)));

        assertEquals(RecoveryGuidancePolicy.Action.SIGN_IN, guidance.action());
        assertEquals("Sign In", guidance.actionLabel());
        assertTrue(guidance.blockedNow().contains("sign-in"));
        assertTrue(guidance.protectedData().contains("will not delete or reassign"));
    }

    @Test
    public void revokedAndNoMembershipDirectToOwnerNotDrive() {
        for (AuthorizationDecision.State state : new AuthorizationDecision.State[]{
                AuthorizationDecision.State.REVOKED,
                AuthorizationDecision.State.NO_MEMBERSHIP}) {
            RecoveryGuidancePolicy.Guidance guidance = RecoveryGuidancePolicy.from(
                    snapshot(
                            state,
                            OrganizationDriveBindingGuard.State.NO_WORKSPACE,
                            counts(0, 1, 0, 0, 0, 0, 1, 0, 0)));

            assertEquals(RecoveryGuidancePolicy.Action.CONTACT_OWNER, guidance.action());
            assertEquals("Contact Organization Owner", guidance.actionLabel());
            assertFalse(guidance.blockedNow().contains("Connect Drive"));
        }
    }

    @Test
    public void recheckRequiredOutranksDriveRecovery() {
        RecoveryGuidancePolicy.Guidance guidance = RecoveryGuidancePolicy.from(
                snapshot(
                        AuthorizationDecision.State.RECHECK_REQUIRED,
                        OrganizationDriveBindingGuard.State.NO_WORKSPACE,
                        zero()));

        assertEquals(RecoveryGuidancePolicy.Action.RECHECK_ACCOUNT, guidance.action());
        assertEquals("Recheck Account", guidance.actionLabel());
    }

    @Test
    public void unreadableProtectedWorkFailsClosedWithoutMutationAction() {
        RecoveryGuidancePolicy.Guidance guidance = RecoveryGuidancePolicy.from(
                snapshot(
                        AuthorizationDecision.State.VALIDATED,
                        OrganizationDriveBindingGuard.State.USABLE,
                        counts(0, 0, 0, 0, 0, 0, 0, 0, 2)));

        assertEquals(RecoveryGuidancePolicy.Action.NONE, guidance.action());
        assertNull(guidance.actionLabel());
        assertTrue(guidance.blockedNow().contains("cannot be read safely"));
        assertTrue(guidance.protectedData().contains("Do not delete app data"));
    }

    @Test
    public void uncertainUploadOutranksDriveRecoveryAndNeverSuggestsRetry() {
        RecoveryGuidancePolicy.Guidance guidance = RecoveryGuidancePolicy.from(
                snapshot(
                        AuthorizationDecision.State.VALIDATED,
                        OrganizationDriveBindingGuard.State.PERMISSION_MISSING,
                        counts(0, 0, 0, 0, 2, 0, 2, 0, 0)));

        assertEquals(RecoveryGuidancePolicy.Action.OPEN_PHOTOS, guidance.action());
        assertEquals("Open Photos", guidance.actionLabel());
        assertTrue(guidance.blockedNow().contains("Do not retry it blindly"));
        assertFalse(guidance.blockedNow().contains("Retry"));
    }

    @Test
    public void validatedUnusableDriveDelegatesToExistingConnectDrive() {
        for (OrganizationDriveBindingGuard.State drive : new OrganizationDriveBindingGuard.State[]{
                OrganizationDriveBindingGuard.State.NO_WORKSPACE,
                OrganizationDriveBindingGuard.State.LEGACY_UNBOUND,
                OrganizationDriveBindingGuard.State.INVALID_BINDING,
                OrganizationDriveBindingGuard.State.WRONG_ORGANIZATION,
                OrganizationDriveBindingGuard.State.PERMISSION_MISSING}) {
            RecoveryGuidancePolicy.Guidance guidance = RecoveryGuidancePolicy.from(
                    snapshot(AuthorizationDecision.State.VALIDATED, drive, zero()));

            assertEquals(RecoveryGuidancePolicy.Action.CONNECT_DRIVE, guidance.action());
            assertEquals("Connect Drive", guidance.actionLabel());
        }
    }

    @Test
    public void graceKeepsExistingWorkButRequiresRecheckForAdminAndRebinding() {
        RecoveryGuidancePolicy.Guidance guidance = RecoveryGuidancePolicy.from(
                snapshot(
                        AuthorizationDecision.State.GRACE,
                        OrganizationDriveBindingGuard.State.USABLE,
                        counts(0, 1, 0, 0, 0, 0, 1, 0, 0)));

        assertEquals(RecoveryGuidancePolicy.Action.RECHECK_ACCOUNT, guidance.action());
        assertTrue(guidance.safeNow().contains("offline grace"));
        assertTrue(guidance.blockedNow().contains("Drive rebinding"));
    }

    @Test
    public void ordinaryProtectedWorkUsesExistingPhotosSurface() {
        RecoveryGuidancePolicy.Guidance guidance = RecoveryGuidancePolicy.from(
                snapshot(
                        AuthorizationDecision.State.VALIDATED,
                        OrganizationDriveBindingGuard.State.USABLE,
                        counts(0, 2, 0, 1, 0, 0, 3, 1, 0)));

        assertEquals(RecoveryGuidancePolicy.Action.OPEN_PHOTOS, guidance.action());
        assertEquals("Open Photos", guidance.actionLabel());
    }

    @Test
    public void healthyStateNeedsNoRecoveryAction() {
        RecoveryGuidancePolicy.Guidance guidance = RecoveryGuidancePolicy.from(
                snapshot(
                        AuthorizationDecision.State.VALIDATED,
                        OrganizationDriveBindingGuard.State.USABLE,
                        zero()));

        assertEquals(RecoveryGuidancePolicy.Action.NONE, guidance.action());
        assertNull(guidance.actionLabel());
        assertEquals("Nothing is currently blocked.", guidance.blockedNow());
    }

    @Test
    public void unavailableQueueNeverPretendsRecoveryIsSafe() {
        AppStatusSnapshot snapshot = AppStatusSnapshot.from(
                decision(AuthorizationDecision.State.VALIDATED),
                null,
                OrganizationDriveBindingGuard.State.USABLE,
                null,
                AppStatusSnapshot.QueueCounts.unavailable(),
                "test",
                true);

        RecoveryGuidancePolicy.Guidance guidance = RecoveryGuidancePolicy.from(snapshot);
        assertEquals(RecoveryGuidancePolicy.Action.NONE, guidance.action());
        assertTrue(guidance.protectedData().contains("left unchanged"));
    }

    private static AppStatusSnapshot snapshot(
            AuthorizationDecision.State auth,
            OrganizationDriveBindingGuard.State drive,
            AppStatusSnapshot.QueueCounts counts) {
        return AppStatusSnapshot.from(
                decision(auth),
                null,
                drive,
                null,
                counts,
                "test",
                true);
    }

    private static AuthorizationDecision decision(AuthorizationDecision.State state) {
        return new AuthorizationDecision(state, "user", "org", "MEMBER", 3600L);
    }

    private static AppStatusSnapshot.QueueCounts counts(
            int capturing,
            int waiting,
            int uploading,
            int failed,
            int uncertain,
            int uploaded,
            int protectedOriginals,
            int cleanupPending,
            int unreadable) {
        return new AppStatusSnapshot.QueueCounts(
                capturing,
                waiting,
                uploading,
                failed,
                uncertain,
                uploaded,
                protectedOriginals,
                cleanupPending,
                unreadable);
    }

    private static AppStatusSnapshot.QueueCounts zero() {
        return counts(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
