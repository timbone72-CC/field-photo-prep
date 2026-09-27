package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FirstRunNavigationPolicyTest {
    @Test
    public void signInRequiredRoutesToExistingAuthActivity() {
        assertTrue(FirstRunNavigationPolicy.requiresAuthentication(
                decision(AuthorizationDecision.State.SIGN_IN_REQUIRED)));
    }

    @Test
    public void otherAuthorizationStatesDoNotCreateASecondAuthRoute() {
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
        assertFalse(FirstRunNavigationPolicy.requiresAuthentication(null));
    }

    @Test
    public void successfulRequiredEntryReturnsToMainOnlyAfterValidation() {
        assertTrue(FirstRunNavigationPolicy.shouldReturnToMain(
                true,
                decision(AuthorizationDecision.State.VALIDATED)));

        assertFalse(FirstRunNavigationPolicy.shouldReturnToMain(
                false,
                decision(AuthorizationDecision.State.VALIDATED)));
        assertFalse(FirstRunNavigationPolicy.shouldReturnToMain(
                true,
                decision(AuthorizationDecision.State.SIGN_IN_REQUIRED)));
        assertFalse(FirstRunNavigationPolicy.shouldReturnToMain(
                true,
                decision(AuthorizationDecision.State.GRACE)));
        assertFalse(FirstRunNavigationPolicy.shouldReturnToMain(true, null));
    }

    private static AuthorizationDecision decision(AuthorizationDecision.State state) {
        return new AuthorizationDecision(state, "user", "organization", "MEMBER", 0L);
    }
}
