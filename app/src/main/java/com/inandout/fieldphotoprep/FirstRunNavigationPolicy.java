package com.inandout.fieldphotoprep;

/**
 * Phase 12G navigation-only policy.
 *
 * Authorization ownership remains in RuntimeAuthorizationManager/Policy. This helper only decides
 * whether the launcher must route through the existing AuthActivity and when that required gate
 * may return to the existing field workflow.
 */
final class FirstRunNavigationPolicy {
    private FirstRunNavigationPolicy() {}

    static boolean requiresAuthentication(AuthorizationDecision decision) {
        return decision == null
                || decision.state() == AuthorizationDecision.State.SIGN_IN_REQUIRED;
    }

    static boolean completesRequiredAuthentication(AuthorizationDecision decision) {
        return decision != null
                && decision.state() == AuthorizationDecision.State.VALIDATED;
    }
}
