package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AppStatusSnapshotTest {
    @Test
    public void accountLabelsCoverEveryAuthorizationState() {
        assertEquals("Validated", snapshot(AuthorizationDecision.State.VALIDATED).accountLabel());
        assertEquals("Offline grace", snapshot(AuthorizationDecision.State.GRACE).accountLabel());
        assertEquals(
                "Recheck required",
                snapshot(AuthorizationDecision.State.RECHECK_REQUIRED).accountLabel());
        assertEquals(
                "Sign in required",
                snapshot(AuthorizationDecision.State.SIGN_IN_REQUIRED).accountLabel());
        assertEquals(
                "No active membership",
                snapshot(AuthorizationDecision.State.NO_MEMBERSHIP).accountLabel());
        assertEquals(
                "Membership revoked",
                snapshot(AuthorizationDecision.State.REVOKED).accountLabel());
        assertEquals(
                "Drive disconnected",
                snapshot(AuthorizationDecision.State.DRIVE_DISCONNECTED).accountLabel());
    }

    @Test
    public void driveLabelsCoverEveryBindingState() {
        assertEquals("Not connected", snapshotDrive(OrganizationDriveBindingGuard.State.NO_WORKSPACE).driveLabel());
        assertEquals(
                "Account recheck required",
                snapshotDrive(OrganizationDriveBindingGuard.State.AUTHORIZATION_REQUIRED).driveLabel());
        assertEquals(
                "Drive confirmation required",
                snapshotDrive(OrganizationDriveBindingGuard.State.LEGACY_UNBOUND).driveLabel());
        assertEquals(
                "Reconnect required",
                snapshotDrive(OrganizationDriveBindingGuard.State.INVALID_BINDING).driveLabel());
        assertEquals(
                "Different organization",
                snapshotDrive(OrganizationDriveBindingGuard.State.WRONG_ORGANIZATION).driveLabel());
        assertEquals(
                "Drive access expired",
                snapshotDrive(OrganizationDriveBindingGuard.State.PERMISSION_MISSING).driveLabel());
        assertEquals("Connected", snapshotDrive(OrganizationDriveBindingGuard.State.USABLE).driveLabel());
    }

    @Test
    public void graceDisplayFloorsTimeWithoutExtendingAuthorization() {
        assertEquals("<1 min", graceSnapshot(59L).graceRemainingLabel());
        assertEquals("1 min", graceSnapshot(60L).graceRemainingLabel());
        assertEquals("59 min", graceSnapshot(3599L).graceRemainingLabel());
        assertEquals("1h 0m", graceSnapshot(3600L).graceRemainingLabel());
        assertEquals("1h 1m", graceSnapshot(3661L).graceRemainingLabel());
        assertEquals(
                "Not applicable",
                snapshot(AuthorizationDecision.State.VALIDATED).graceRemainingLabel());
    }

    @Test
    public void supportSummaryUsesStrictAllowlistAndDropsSensitiveContext() {
        AuthSessionState session = new AuthSessionState(
                "access-secret-token",
                "refresh-secret-token",
                2000000000L,
                "user-secret-uuid",
                "private.person@example.com",
                "org-secret-uuid",
                "Secret Organization content://tree/private",
                "membership-secret-uuid",
                "OWNER",
                "ACTIVE",
                1790467201L);
        AuthorizationDecision decision = new AuthorizationDecision(
                AuthorizationDecision.State.GRACE,
                "user-secret-uuid",
                "org-secret-uuid",
                "OWNER",
                3599L);
        AppStatusSnapshot.QueueCounts counts =
                new AppStatusSnapshot.QueueCounts(1, 2, 3, 4, 5, 6, 7, 8, 9);

        AppStatusSnapshot snapshot = AppStatusSnapshot.from(
                decision,
                session,
                OrganizationDriveBindingGuard.State.WRONG_ORGANIZATION,
                "Sensitive Client / 123 Private Address / provider-id-abc",
                counts,
                "0.28.0-internal",
                false);

        String support = snapshot.supportSummary();

        assertTrue(support.contains("Account state: GRACE"));
        assertTrue(support.contains("Role: OWNER"));
        assertTrue(support.contains("Grace remaining: 59 min"));
        assertTrue(support.contains("Drive state: WRONG_ORGANIZATION"));
        assertTrue(support.contains("Queue UNCERTAIN: 5"));
        assertTrue(support.contains("Protected originals: 7"));
        assertTrue(support.contains("App version: 0.28.0-internal"));

        assertFalse(support.contains("access-secret-token"));
        assertFalse(support.contains("refresh-secret-token"));
        assertFalse(support.contains("private.person@example.com"));
        assertFalse(support.contains("user-secret-uuid"));
        assertFalse(support.contains("org-secret-uuid"));
        assertFalse(support.contains("membership-secret-uuid"));
        assertFalse(support.contains("Secret Organization"));
        assertFalse(support.contains("Sensitive Client"));
        assertFalse(support.contains("123 Private Address"));
        assertFalse(support.contains("provider-id-abc"));
        assertFalse(support.contains("content://"));
    }

    @Test
    public void supportSummarySanitizesUntrustedRoleAndVersion() {
        AuthSessionState session = new AuthSessionState(
                "a",
                "r",
                2000000000L,
                "u",
                "email@example.com",
                "o",
                "Org",
                "m",
                "ADMIN\nsecret",
                "ACTIVE",
                0L);
        AppStatusSnapshot snapshot = AppStatusSnapshot.from(
                new AuthorizationDecision(
                        AuthorizationDecision.State.VALIDATED,
                        "u",
                        "o",
                        "ADMIN\nsecret",
                        0L),
                session,
                OrganizationDriveBindingGuard.State.USABLE,
                null,
                zeroCounts(),
                "0.28.0\nsecret",
                true);

        String support = snapshot.supportSummary();
        assertTrue(support.contains("Role: UNKNOWN"));
        assertTrue(support.contains("App version: unknown"));
        assertFalse(support.contains("secret"));
    }

    @Test
    public void queueCountsRejectNegativeValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AppStatusSnapshot.QueueCounts(0, 0, 0, 0, -1, 0, 0, 0, 0));
    }

    @Test
    public void unresolvedCountUsesOnlyUnresolvedAndUnreadableStates() {
        AppStatusSnapshot.QueueCounts counts =
                new AppStatusSnapshot.QueueCounts(1, 2, 3, 4, 5, 100, 7, 8, 9);
        assertEquals(24, counts.unresolved());
    }

    private static AppStatusSnapshot snapshot(AuthorizationDecision.State state) {
        return AppStatusSnapshot.from(
                new AuthorizationDecision(state, "user", "org", "MEMBER", 0L),
                null,
                OrganizationDriveBindingGuard.State.NO_WORKSPACE,
                null,
                zeroCounts(),
                "test",
                true);
    }

    private static AppStatusSnapshot snapshotDrive(OrganizationDriveBindingGuard.State state) {
        return AppStatusSnapshot.from(
                new AuthorizationDecision(
                        AuthorizationDecision.State.VALIDATED,
                        "user",
                        "org",
                        "MEMBER",
                        0L),
                null,
                state,
                null,
                zeroCounts(),
                "test",
                true);
    }

    private static AppStatusSnapshot graceSnapshot(long seconds) {
        return AppStatusSnapshot.from(
                new AuthorizationDecision(
                        AuthorizationDecision.State.GRACE,
                        "user",
                        "org",
                        "MEMBER",
                        seconds),
                null,
                OrganizationDriveBindingGuard.State.USABLE,
                null,
                zeroCounts(),
                "test",
                true);
    }

    private static AppStatusSnapshot.QueueCounts zeroCounts() {
        return new AppStatusSnapshot.QueueCounts(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
