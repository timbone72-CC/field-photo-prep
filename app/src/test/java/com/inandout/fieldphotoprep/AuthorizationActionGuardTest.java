package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public final class AuthorizationActionGuardTest {
    @Test
    public void secondCameraShutterCheckObservesAuthorityLoss() throws Exception {
        AtomicReference<AuthorizationDecision.State> state =
                new AtomicReference<>(AuthorizationDecision.State.VALIDATED);
        AuthorizationActionGuard guard = new AuthorizationActionGuard(() ->
                new AuthorizationDecision(
                        state.get(),
                        "user-1",
                        "org-1",
                        "OWNER",
                        0L));

        guard.requireNewCapture();

        state.set(AuthorizationDecision.State.REVOKED);

        assertThrows(IOException.class, guard::requireNewCapture);
    }

    @Test
    public void nextDriveAttemptObservesAuthorityLoss() throws Exception {
        AtomicReference<AuthorizationDecision.State> state =
                new AtomicReference<>(AuthorizationDecision.State.GRACE);
        AuthorizationActionGuard guard = new AuthorizationActionGuard(() ->
                new AuthorizationDecision(
                        state.get(),
                        "user-1",
                        "org-1",
                        "OWNER",
                        60L));

        guard.requireDriveMutation();

        state.set(AuthorizationDecision.State.RECHECK_REQUIRED);

        assertThrows(IOException.class, guard::requireDriveMutation);
    }
}
