package com.inandout.fieldphotoprep;

/**
 * Phase 12G navigation policy only.
 *
 * This class does not persist account state or Drive state. It translates the authoritative
 * Phase 12E authorization decision into the smallest first-run navigation choice.
 */
final class FirstRunNavigationPolicy {
    private FirstRunNavigationPolicy() {}

    static boolean requiresAuthentication(AuthorizationDecision decision) {
        return decision != null
                && decision.state() == AuthorizationDecision.State.SIGN_IN_REQUIRED;
    }

    static boolean continuesOnboardingAfterRedirect(AuthRedirectParser.Kind redirectKind) {
        return redirectKind == AuthRedirectParser.Kind.INVITE;
    }

    static boolean shouldReturnToMain(
            boolean onboardingEntry,
            AuthorizationDecision decision) {
        return onboardingEntry
                && decision != null
                && decision.state() == AuthorizationDecision.State.VALIDATED;
    }
}
