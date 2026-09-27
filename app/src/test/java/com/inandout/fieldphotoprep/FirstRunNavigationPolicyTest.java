package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FirstRunNavigationPolicyTest {
    @Test
    public void noDecisionOrSignInRequiredRoutesToExistingAuthActivity() {
        assertTrue(FirstRunNavigationPolicy.requiresAuthentication(null));
        assertTrue(FirstRunNavigationPolicy.requiresAuthentication(
                decision(AuthorizationDecision.State.SIGN_IN_REQUIRED)));
    }

    @Test
    public void existingNonSignInStatesDoNotCreateASecondRecoveryFlow() {
        assertFalse(FirstRunNavigationPolicy.requiresAuthentication(
                decision(AuthorizationDecision.State.VALIDATED)));
        assertFalse(FirstRunNavigationPolicy.requiresAuthentication(
                decision(AuthorizationDecision.State.GRACE)));
        assertFalse(FirstRunNavigationPolicy.requiresAuthentication(
                decision(AuthorizationDecision.State.RECHECK_REQUIRED)));
        assertFalse(FirstRunNavigationPolicy.requiresAuthentication(
                decision(AuthorizationDecision.State.NO_MEMBERSHIP)));
        assertFalse(FirstRunNavigationPolicy.requiresAuthentication(
                decision(AuthorizationDecision.State.REVOKED)));
        assertFalse(FirstRunNavigationPolicy.requiresAuthentication(
                decision(AuthorizationDecision.State.DRIVE_DISCONNECTED)));
    }

    @Test
    public void requiredGateReturnsToFieldFlowOnlyAfterValidatedIdentity() {
        assertTrue(FirstRunNavigationPolicy.completesRequiredAuthentication(
                decision(AuthorizationDecision.State.VALIDATED)));

        assertFalse(FirstRunNavigationPolicy.completesRequiredAuthentication(
                decision(AuthorizationDecision.State.GRACE)));
        assertFalse(FirstRunNavigationPolicy.completesRequiredAuthentication(
                decision(AuthorizationDecision.State.RECHECK_REQUIRED)));
        assertFalse(FirstRunNavigationPolicy.completesRequiredAuthentication(
                decision(AuthorizationDecision.State.SIGN_IN_REQUIRED)));
        assertFalse(FirstRunNavigationPolicy.completesRequiredAuthentication(
                decision(AuthorizationDecision.State.NO_MEMBERSHIP)));
        assertFalse(FirstRunNavigationPolicy.completesRequiredAuthentication(
                decision(AuthorizationDecision.State.REVOKED)));
        assertFalse(FirstRunNavigationPolicy.completesRequiredAuthentication(
                decision(AuthorizationDecision.State.DRIVE_DISCONNECTED)));
    }

    private static AuthorizationDecision decision(AuthorizationDecision.State state) {
        return new AuthorizationDecision(
                state,
                "user-1",
                "organization-1",
                "MEMBER",
                0L);
    }
}
