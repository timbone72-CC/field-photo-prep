package com.inandout.fieldphotoprep;

final class TestAuthorization {
    private TestAuthorization() {
    }

    static AuthorizationActionGuard allowedGuard() {
        return new AuthorizationActionGuard(() -> new AuthorizationDecision(
                AuthorizationDecision.State.VALIDATED,
                "test-user",
                "test-organization",
                "OWNER",
                0L));
    }
}
