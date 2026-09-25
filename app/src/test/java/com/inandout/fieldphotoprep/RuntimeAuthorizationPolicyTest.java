package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RuntimeAuthorizationPolicyTest {
    private static final long VALIDATED_AT = 2_000_000_000L;

    private final RuntimeAuthorizationPolicy policy = new RuntimeAuthorizationPolicy();

    @Test
    public void noSessionRequiresSignInAndKeepsRecoveryReadable() {
        AuthorizationDecision decision = policy.evaluate(
                null,
                RuntimeAuthorizationPolicy.Observation.none(),
                VALIDATED_AT + 1L,
                null);

        assertEquals(AuthorizationDecision.State.SIGN_IN_REQUIRED, decision.state());
        assertFalse(decision.allowsNewCapture());
        assertFalse(decision.allowsNewDriveMutation());
        assertTrue(decision.allowsReadAndRecovery());
    }

    @Test
    public void authoritativeMatchingActiveMembershipIsValidated() {
        AuthSessionState stored = ownerSession("ACTIVE", VALIDATED_AT);

        AuthorizationDecision decision = policy.evaluate(
                stored,
                RuntimeAuthorizationPolicy.Observation.active(
                        stored.userId(),
                        stored.organizationId(),
                        stored.membershipId(),
                        "OWNER"),
                VALIDATED_AT + 500L,
                500L);

        assertEquals(AuthorizationDecision.State.VALIDATED, decision.state());
        assertTrue(decision.allowsNewCapture());
        assertTrue(decision.allowsNewDriveMutation());
        assertTrue(decision.allowsMemberAdministration());
    }

    @Test
    public void temporaryFailureOneInstantBeforeBoundaryUsesGrace() {
        AuthSessionState stored = ownerSession("ACTIVE", VALIDATED_AT);

        AuthorizationDecision decision = policy.evaluate(
                stored,
                RuntimeAuthorizationPolicy.Observation.indeterminate(),
                VALIDATED_AT + RuntimeAuthorizationPolicy.GRACE_SECONDS - 1L,
                RuntimeAuthorizationPolicy.GRACE_SECONDS - 1L);

        assertEquals(AuthorizationDecision.State.GRACE, decision.state());
        assertEquals(1L, decision.graceRemainingSeconds());
        assertTrue(decision.allowsNewCapture());
        assertTrue(decision.allowsNewDriveMutation());
        assertFalse(decision.allowsMemberAdministration());
    }

    @Test
    public void exactGraceBoundaryBlocks() {
        AuthSessionState stored = ownerSession("ACTIVE", VALIDATED_AT);

        AuthorizationDecision decision = policy.evaluate(
                stored,
                RuntimeAuthorizationPolicy.Observation.indeterminate(),
                VALIDATED_AT + RuntimeAuthorizationPolicy.GRACE_SECONDS,
                RuntimeAuthorizationPolicy.GRACE_SECONDS);

        assertEquals(AuthorizationDecision.State.RECHECK_REQUIRED, decision.state());
        assertFalse(decision.allowsNewCapture());
        assertFalse(decision.allowsNewDriveMutation());
    }

    @Test
    public void beyondGraceBoundaryBlocks() {
        AuthSessionState stored = ownerSession("ACTIVE", VALIDATED_AT);

        AuthorizationDecision decision = policy.evaluate(
                stored,
                RuntimeAuthorizationPolicy.Observation.indeterminate(),
                VALIDATED_AT + RuntimeAuthorizationPolicy.GRACE_SECONDS + 1L,
                RuntimeAuthorizationPolicy.GRACE_SECONDS + 1L);

        assertEquals(AuthorizationDecision.State.RECHECK_REQUIRED, decision.state());
    }

    @Test
    public void wallClockRollbackFailsClosedEvenWithShortMonotonicElapsed() {
        AuthSessionState stored = ownerSession("ACTIVE", VALIDATED_AT);

        AuthorizationDecision decision = policy.evaluate(
                stored,
                RuntimeAuthorizationPolicy.Observation.indeterminate(),
                VALIDATED_AT - 1L,
                30L);

        assertEquals(AuthorizationDecision.State.RECHECK_REQUIRED, decision.state());
    }

    @Test
    public void authoritativeRevocationEndsGraceImmediately() {
        AuthSessionState stored = ownerSession("ACTIVE", VALIDATED_AT);

        AuthorizationDecision decision = policy.evaluate(
                stored,
                RuntimeAuthorizationPolicy.Observation.revoked(
                        stored.userId(),
                        stored.organizationId(),
                        stored.membershipId()),
                VALIDATED_AT + 30L,
                30L);

        assertEquals(AuthorizationDecision.State.REVOKED, decision.state());
        assertFalse(decision.allowsNewCapture());
        assertFalse(decision.allowsNewDriveMutation());
    }

    @Test
    public void persistedRevocationCannotResurrectOfflineGraceAfterRestart() {
        AuthSessionState stored = ownerSession("REVOKED", VALIDATED_AT);

        AuthorizationDecision decision = policy.evaluate(
                stored,
                RuntimeAuthorizationPolicy.Observation.indeterminate(),
                VALIDATED_AT + 30L,
                null);

        assertEquals(AuthorizationDecision.State.REVOKED, decision.state());
    }

    @Test
    public void differentAuthoritativeIdentityCannotBorrowStoredGrace() {
        AuthSessionState stored = ownerSession("ACTIVE", VALIDATED_AT);

        AuthorizationDecision decision = policy.evaluate(
                stored,
                RuntimeAuthorizationPolicy.Observation.active(
                        "different-user",
                        stored.organizationId(),
                        stored.membershipId(),
                        "OWNER"),
                VALIDATED_AT + 30L,
                30L);

        assertEquals(AuthorizationDecision.State.NO_MEMBERSHIP, decision.state());
    }

    @Test
    public void memberCanWorkButCannotAdministerMemberships() {
        AuthSessionState stored = memberSession("ACTIVE", VALIDATED_AT);

        AuthorizationDecision decision = policy.evaluate(
                stored,
                RuntimeAuthorizationPolicy.Observation.active(
                        stored.userId(),
                        stored.organizationId(),
                        stored.membershipId(),
                        "MEMBER"),
                VALIDATED_AT + 10L,
                10L);

        assertEquals(AuthorizationDecision.State.VALIDATED, decision.state());
        assertTrue(decision.allowsNewCapture());
        assertTrue(decision.allowsNewDriveMutation());
        assertFalse(decision.allowsMemberAdministration());
    }

    @Test
    public void missingValidationTimeHasNoFirstUseOfflineGrace() {
        AuthSessionState stored = ownerSession("ACTIVE", 0L);

        AuthorizationDecision decision = policy.evaluate(
                stored,
                RuntimeAuthorizationPolicy.Observation.indeterminate(),
                VALIDATED_AT,
                null);

        assertEquals(AuthorizationDecision.State.RECHECK_REQUIRED, decision.state());
    }

    private static AuthSessionState ownerSession(String status, long validatedAt) {
        return session("OWNER", status, validatedAt);
    }

    private static AuthSessionState memberSession(String status, long validatedAt) {
        return session("MEMBER", status, validatedAt);
    }

    private static AuthSessionState session(String role, String status, long validatedAt) {
        return new AuthSessionState(
                "access",
                "refresh",
                VALIDATED_AT + 10_000L,
                "user-1",
                "user@example.com",
                "org-1",
                "Example Organization",
                "membership-1",
                role,
                status,
                validatedAt);
    }
}
