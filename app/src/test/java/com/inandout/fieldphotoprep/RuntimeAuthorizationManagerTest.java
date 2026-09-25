package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class RuntimeAuthorizationManagerTest {
    private static final long NOW = 2_000_000_000L;

    @Test
    public void signOutDuringRefreshCannotRestoreOldSession() {
        FakeStore store = new FakeStore(activeSession(NOW - 10L));
        FakeClock clock = new FakeClock(NOW, 100L);
        final RuntimeAuthorizationManager[] holder = new RuntimeAuthorizationManager[1];

        RuntimeAuthorizationManager.Backend backend =
                new RuntimeAuthorizationManager.Backend() {
                    @Override
                    public SupabaseAuthClient.AuthTokens refreshSession(String refreshToken) {
                        holder[0].clearAuthenticatedSession();
                        return new SupabaseAuthClient.AuthTokens(
                                "rotated-access",
                                "rotated-refresh",
                                NOW + 7200L,
                                "user-1",
                                "user@example.com");
                    }

                    @Override
                    public SupabaseAuthClient.StoredMembershipValidation validateStoredMembership(
                            SupabaseAuthClient.AuthTokens tokens,
                            AuthSessionState stored,
                            long validatedAtEpochSeconds) {
                        throw new AssertionError(
                                "Membership validation must not run after the session was cleared.");
                    }
                };

        RuntimeAuthorizationManager manager =
                new RuntimeAuthorizationManager(store, backend, clock, Runnable::run);
        holder[0] = manager;

        AuthorizationDecision decision = manager.revalidateAsync().join();

        assertEquals(AuthorizationDecision.State.SIGN_IN_REQUIRED, decision.state());
        assertEquals(null, store.state);
    }

    @Test
    public void concurrentRevalidationRequestsAreCoalesced() {
        FakeStore store = new FakeStore(activeSession(NOW - 10L));
        FakeClock clock = new FakeClock(NOW, 100L);
        FakeBackend backend = FakeBackend.active(store, clock);
        ManualExecutor executor = new ManualExecutor();
        RuntimeAuthorizationManager manager =
                new RuntimeAuthorizationManager(store, backend, clock, executor);

        CompletableFuture<AuthorizationDecision> first = manager.revalidateAsync();
        CompletableFuture<AuthorizationDecision> second = manager.revalidateAsync();

        assertSame(first, second);
        assertEquals(0, backend.refreshCount);

        executor.runNext();

        assertEquals(1, backend.refreshCount);
        assertEquals(AuthorizationDecision.State.VALIDATED, first.join().state());
    }

    @Test
    public void rotatedTokensArePersistedBeforeMembershipValidation() {
        FakeStore store = new FakeStore(activeSession(NOW - 10L));
        FakeClock clock = new FakeClock(NOW, 100L);
        FakeBackend backend = FakeBackend.active(store, clock);
        RuntimeAuthorizationManager manager =
                new RuntimeAuthorizationManager(store, backend, clock, Runnable::run);

        AuthorizationDecision decision = manager.revalidateAsync().join();

        assertEquals(AuthorizationDecision.State.VALIDATED, decision.state());
        assertEquals(
                List.of("refresh", "save:rotated-access", "validate", "save:rotated-access"),
                storeAndBackendOrder(store, backend));
        assertTrue(backend.sawRotatedTokensPersistedBeforeValidation);
    }

    @Test
    public void temporaryRefreshFailureUsesExistingGrace() {
        FakeStore store = new FakeStore(activeSession(NOW - 60L));
        FakeClock clock = new FakeClock(NOW, 100L);
        FakeBackend backend = FakeBackend.temporaryRefreshFailure();
        RuntimeAuthorizationManager manager =
                new RuntimeAuthorizationManager(store, backend, clock, Runnable::run);

        AuthorizationDecision decision = manager.revalidateAsync().join();

        assertEquals(AuthorizationDecision.State.GRACE, decision.state());
        assertTrue(decision.allowsNewCapture());
        assertFalse(decision.allowsMemberAdministration());
    }

    @Test
    public void revokedMembershipIsPersistedAndCannotReturnToOfflineGrace() {
        FakeStore store = new FakeStore(activeSession(NOW - 60L));
        FakeClock clock = new FakeClock(NOW, 100L);
        FakeBackend backend = FakeBackend.revoked();
        RuntimeAuthorizationManager manager =
                new RuntimeAuthorizationManager(store, backend, clock, Runnable::run);

        AuthorizationDecision decision = manager.revalidateAsync().join();

        assertEquals(AuthorizationDecision.State.REVOKED, decision.state());
        assertEquals("REVOKED", store.state.membershipStatus());

        RuntimeAuthorizationManager afterRestart =
                new RuntimeAuthorizationManager(
                        store,
                        FakeBackend.temporaryRefreshFailure(),
                        new FakeClock(NOW + 10L, 10L),
                        Runnable::run);

        assertEquals(
                AuthorizationDecision.State.REVOKED,
                afterRestart.currentDecision().state());
    }

    @Test
    public void rejectedRefreshRequiresSignInAndInvalidatesOfflineGrace() {
        FakeStore store = new FakeStore(activeSession(NOW - 60L));
        FakeClock clock = new FakeClock(NOW, 100L);
        FakeBackend backend = FakeBackend.rejectedRefresh();
        RuntimeAuthorizationManager manager =
                new RuntimeAuthorizationManager(store, backend, clock, Runnable::run);

        AuthorizationDecision decision = manager.revalidateAsync().join();

        assertEquals(AuthorizationDecision.State.SIGN_IN_REQUIRED, decision.state());
        assertEquals(0L, store.state.lastMembershipValidatedAtEpochSeconds());

        RuntimeAuthorizationManager afterRestart =
                new RuntimeAuthorizationManager(
                        store,
                        FakeBackend.temporaryRefreshFailure(),
                        new FakeClock(NOW + 10L, 10L),
                        Runnable::run);

        assertEquals(
                AuthorizationDecision.State.RECHECK_REQUIRED,
                afterRestart.currentDecision().state());
    }

    @Test
    public void membershipBadRequestFailsClosedWithoutPretendingSignInWasRejected() {
        FakeStore store = new FakeStore(activeSession(NOW - 60L));
        FakeClock clock = new FakeClock(NOW, 100L);
        FakeBackend backend = FakeBackend.membershipBadRequest();
        RuntimeAuthorizationManager manager =
                new RuntimeAuthorizationManager(store, backend, clock, Runnable::run);

        AuthorizationDecision decision = manager.revalidateAsync().join();

        assertEquals(AuthorizationDecision.State.RECHECK_REQUIRED, decision.state());
        assertTrue(store.state != null);
        assertEquals(0L, store.state.lastMembershipValidatedAtEpochSeconds());
    }

    @Test
    public void successfulValidationResetsTheSeventyTwoHourWindow() {
        FakeStore store = new FakeStore(
                activeSession(NOW - RuntimeAuthorizationPolicy.GRACE_SECONDS + 1L));
        FakeClock clock = new FakeClock(NOW, 500L);
        FakeBackend backend = FakeBackend.active(store, clock);
        RuntimeAuthorizationManager manager =
                new RuntimeAuthorizationManager(store, backend, clock, Runnable::run);

        AuthorizationDecision validated = manager.revalidateAsync().join();
        assertEquals(AuthorizationDecision.State.VALIDATED, validated.state());
        assertEquals(NOW, store.state.lastMembershipValidatedAtEpochSeconds());

        clock.wall = NOW + RuntimeAuthorizationPolicy.GRACE_SECONDS - 1L;
        clock.monotonic = 500L + RuntimeAuthorizationPolicy.GRACE_SECONDS - 1L;
        assertTrue(manager.currentDecision().allowsNewCapture());

        clock.wall = NOW + RuntimeAuthorizationPolicy.GRACE_SECONDS;
        clock.monotonic = 500L + RuntimeAuthorizationPolicy.GRACE_SECONDS;
        assertEquals(
                AuthorizationDecision.State.RECHECK_REQUIRED,
                manager.currentDecision().state());
    }

    private static List<String> storeAndBackendOrder(FakeStore store, FakeBackend backend) {
        List<String> combined = new ArrayList<>();
        combined.addAll(backend.order);
        combined.addAll(store.order);
        combined.sort((a, b) -> Integer.compare(sequence(a), sequence(b)));
        List<String> normalized = new ArrayList<>();
        for (String value : combined) {
            normalized.add(value.substring(value.indexOf(':') + 1));
        }
        return normalized;
    }

    private static int sequence(String value) {
        return Integer.parseInt(value.substring(0, value.indexOf(':')));
    }

    private static AuthSessionState activeSession(long validatedAt) {
        return new AuthSessionState(
                "old-access",
                "old-refresh",
                NOW + 3600L,
                "user-1",
                "user@example.com",
                "org-1",
                "Example Organization",
                "membership-1",
                "OWNER",
                "ACTIVE",
                validatedAt);
    }

    private static final class FakeStore implements RuntimeAuthorizationManager.SessionStore {
        private AuthSessionState state;
        private int sequence;
        private final List<String> order = new ArrayList<>();

        FakeStore(AuthSessionState state) {
            this.state = state;
        }

        @Override
        public AuthSessionState load() {
            return state;
        }

        @Override
        public void save(AuthSessionState state) {
            this.state = state;
            order.add((++sequence) + ":save:" + state.accessToken());
        }

        @Override
        public void clear() {
            state = null;
            order.add((++sequence) + ":clear");
        }
    }

    private static final class FakeClock implements RuntimeAuthorizationManager.Clock {
        private long wall;
        private long monotonic;

        FakeClock(long wall, long monotonic) {
            this.wall = wall;
            this.monotonic = monotonic;
        }

        @Override
        public long wallEpochSeconds() {
            return wall;
        }

        @Override
        public long monotonicEpochSeconds() {
            return monotonic;
        }
    }

    private static final class FakeBackend implements RuntimeAuthorizationManager.Backend {
        private enum Mode {
            ACTIVE,
            REVOKED,
            TEMPORARY_REFRESH_FAILURE,
            REJECTED_REFRESH,
            MEMBERSHIP_BAD_REQUEST
        }

        private static int globalSequence;

        private final Mode mode;
        private final FakeStore store;
        private final FakeClock clock;
        private final List<String> order = new ArrayList<>();
        private int refreshCount;
        private boolean sawRotatedTokensPersistedBeforeValidation;

        private FakeBackend(Mode mode, FakeStore store, FakeClock clock) {
            this.mode = mode;
            this.store = store;
            this.clock = clock;
        }

        static FakeBackend active(FakeStore store, FakeClock clock) {
            globalSequence = 0;
            return new FakeBackend(Mode.ACTIVE, store, clock);
        }

        static FakeBackend revoked() {
            globalSequence = 0;
            return new FakeBackend(Mode.REVOKED, null, null);
        }

        static FakeBackend temporaryRefreshFailure() {
            globalSequence = 0;
            return new FakeBackend(Mode.TEMPORARY_REFRESH_FAILURE, null, null);
        }

        static FakeBackend rejectedRefresh() {
            globalSequence = 0;
            return new FakeBackend(Mode.REJECTED_REFRESH, null, null);
        }

        static FakeBackend membershipBadRequest() {
            globalSequence = 0;
            return new FakeBackend(Mode.MEMBERSHIP_BAD_REQUEST, null, null);
        }

        @Override
        public SupabaseAuthClient.AuthTokens refreshSession(String refreshToken)
                throws IOException {
            refreshCount++;
            order.add((++globalSequence) + ":refresh");
            if (mode == Mode.TEMPORARY_REFRESH_FAILURE) {
                throw new IOException("offline");
            }
            if (mode == Mode.REJECTED_REFRESH) {
                throw new SupabaseAuthClient.AuthException("invalid refresh", 401);
            }
            return new SupabaseAuthClient.AuthTokens(
                    "rotated-access",
                    "rotated-refresh",
                    NOW + 7200L,
                    "user-1",
                    "user@example.com");
        }

        @Override
        public SupabaseAuthClient.StoredMembershipValidation validateStoredMembership(
                SupabaseAuthClient.AuthTokens tokens,
                AuthSessionState stored,
                long validatedAtEpochSeconds) {
            order.add((++globalSequence) + ":validate");
            if (store != null) {
                sawRotatedTokensPersistedBeforeValidation =
                        store.state != null
                                && "rotated-access".equals(store.state.accessToken());
            }
            if (mode == Mode.REVOKED) {
                return SupabaseAuthClient.StoredMembershipValidation.revoked();
            }
            if (mode == Mode.MEMBERSHIP_BAD_REQUEST) {
                throw new SupabaseAuthClient.AuthException("membership query rejected", 400);
            }
            AuthSessionState active = new AuthSessionState(
                    tokens.accessToken(),
                    tokens.refreshToken(),
                    tokens.expiresAtEpochSeconds(),
                    stored.userId(),
                    tokens.email(),
                    stored.organizationId(),
                    stored.organizationName(),
                    stored.membershipId(),
                    stored.role(),
                    "ACTIVE",
                    validatedAtEpochSeconds);
            return SupabaseAuthClient.StoredMembershipValidation.active(active);
        }
    }

    private static final class ManualExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        void runNext() {
            Runnable task = tasks.remove();
            task.run();
        }
    }
}
