package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AuthSessionStateTest {
    @Test
    public void rotatedTokensPreserveValidatedIdentitySnapshotUntilRevalidation() {
        AuthSessionState stored = new AuthSessionState(
                "old-access",
                "old-refresh",
                2000000000L,
                "789bc0d8-6f9d-4ab4-9f87-33195109a65a",
                "inandoutinspections2026@gmail.com",
                "3198253e-8d41-4419-874e-46e2d3906928",
                "In And Out Cleaner Inspections LLC",
                "de55393f-6386-480a-9823-b6ee4d9901a0",
                "OWNER",
                "ACTIVE",
                1900000000L);

        AuthSessionState rotated = stored.withSessionTokens(
                "new-access",
                "new-refresh",
                2100000000L);

        assertEquals("new-access", rotated.accessToken());
        assertEquals("new-refresh", rotated.refreshToken());
        assertEquals(2100000000L, rotated.expiresAtEpochSeconds());

        assertEquals(stored.userId(), rotated.userId());
        assertEquals(stored.email(), rotated.email());
        assertEquals(stored.organizationId(), rotated.organizationId());
        assertEquals(stored.organizationName(), rotated.organizationName());
        assertEquals(stored.membershipId(), rotated.membershipId());
        assertEquals(stored.role(), rotated.role());
        assertEquals(stored.membershipStatus(), rotated.membershipStatus());
        assertEquals(
                stored.lastMembershipValidatedAtEpochSeconds(),
                rotated.lastMembershipValidatedAtEpochSeconds());
    }
}
