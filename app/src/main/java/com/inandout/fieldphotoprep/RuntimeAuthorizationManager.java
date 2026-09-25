package com.inandout.fieldphotoprep;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Process-level owner for Phase 12E authorization state and serialized/coalesced revalidation.
 *
 * This class is deliberately free of Android UI concerns. Activities consume its immutable
 * decisions; protected mutation entry points must ask again immediately before starting work.
 */
final class RuntimeAuthorizationManager {
    interface SessionStore {
        AuthSessionState load();
        void save(AuthSessionState state) throws Exception;
        void clear();
    }

    interface Backend {
        SupabaseAuthClient.AuthTokens refreshSession(String refreshToken) throws IOException;

        SupabaseAuthClient.StoredMembershipValidation validateStoredMembership(
                SupabaseAuthClient.AuthTokens tokens,
                AuthSessionState stored,
                long validatedAtEpochSeconds) throws IOException;
    }

    interface Clock {
        long wallEpochSeconds();
        long monotonicEpochSeconds();
    }

    private final Object lock = new Object();
    private final SessionStore sessionStore;
    private final Backend backend;
    private final Clock clock;
    private final Executor executor;
    private final RuntimeAuthorizationPolicy policy = new RuntimeAuthorizationPolicy();

    private CompletableFuture<AuthorizationDecision> inFlight;
    private RuntimeAuthorizationPolicy.Observation lastObservation =
            RuntimeAuthorizationPolicy.Observation.none();
    private String observedUserId;
    private String observedOrganizationId;
    private String observedMembershipId;
    private long monotonicValidatedAtSeconds = -1L;
    private long anchoredValidationWallSeconds = -1L;
    private long sessionGeneration;

    RuntimeAuthorizationManager(
            SessionStore sessionStore,
            Backend backend,
            Clock clock,
            Executor executor) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.backend = Objects.requireNonNull(backend, "backend");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    AuthorizationDecision currentDecision() {
        synchronized (lock) {
            AuthSessionState stored = sessionStore.load();
            return evaluateStoredLocked(stored);
        }
    }

    AuthSessionState storedSession() {
        synchronized (lock) {
            return sessionStore.load();
        }
    }

    void replaceAuthenticatedSession(AuthSessionState state) throws Exception {
        Objects.requireNonNull(state, "state");
        synchronized (lock) {
            sessionGeneration++;
            sessionStore.save(state);
            inFlight = null;
            if (state.isActiveOwnerOrMember()
                    && state.lastMembershipValidatedAtEpochSeconds() > 0L) {
                anchoredValidationWallSeconds =
                        state.lastMembershipValidatedAtEpochSeconds();
                monotonicValidatedAtSeconds = clock.monotonicEpochSeconds();
                setObservationLocked(
                        state,
                        RuntimeAuthorizationPolicy.Observation.active(
                                state.userId(),
                                state.organizationId(),
                                state.membershipId(),
                                state.role()));
            } else {
                clearObservationLocked();
            }
        }
    }

    void clearAuthenticatedSession() {
        synchronized (lock) {
            sessionGeneration++;
            sessionStore.clear();
            inFlight = null;
            clearObservationLocked();
        }
    }

    CompletableFuture<AuthorizationDecision> revalidateAsync() {
        synchronized (lock) {
            if (inFlight != null) {
                return inFlight;
            }

            CompletableFuture<AuthorizationDecision> future =
                    CompletableFuture.supplyAsync(this::revalidateInternal, executor);
            inFlight = future;
            future.whenComplete((decision, error) -> {
                synchronized (lock) {
                    if (inFlight == future) {
                        inFlight = null;
                    }
                }
            });
            return future;
        }
    }

    private AuthorizationDecision revalidateInternal() {
        final AuthSessionState stored;
        final long generationAtStart;
        synchronized (lock) {
            stored = sessionStore.load();
            generationAtStart = sessionGeneration;
            if (stored == null) {
                clearObservationLocked();
                return evaluateStoredLocked(null);
            }
        }

        final SupabaseAuthClient.AuthTokens refreshed;
        try {
            refreshed = backend.refreshSession(stored.refreshToken());
        } catch (SupabaseAuthClient.AuthException error) {
            synchronized (lock) {
                if (sessionGeneration != generationAtStart
                        || !sameSessionVersion(sessionStore.load(), stored)) {
                    return evaluateStoredLocked(sessionStore.load());
                }
                if (error.isAuthenticationRejected()) {
                    failClosedPersistentlyLocked(stored);
                    clearObservationLocked();
                    return new AuthorizationDecision(
                            AuthorizationDecision.State.SIGN_IN_REQUIRED,
                            stored.userId(),
                            stored.organizationId(),
                            stored.role(),
                            0L);
                }
                if (error.isTemporaryServerFailure()) {
                    setObservationLocked(
                            stored,
                            RuntimeAuthorizationPolicy.Observation.indeterminate());
                    return evaluateStoredLocked(stored);
                }
                failClosedPersistentlyLocked(stored);
                clearObservationLocked();
                return evaluateStoredLocked(sessionStore.load());
            }
        } catch (IOException error) {
            synchronized (lock) {
                if (sessionGeneration != generationAtStart
                        || !sameSessionVersion(sessionStore.load(), stored)) {
                    return evaluateStoredLocked(sessionStore.load());
                }
                setObservationLocked(
                        stored,
                        RuntimeAuthorizationPolicy.Observation.indeterminate());
                return evaluateStoredLocked(stored);
            }
        }

        AuthSessionState rotated = stored.withSessionTokens(
                refreshed.accessToken(),
                refreshed.refreshToken(),
                refreshed.expiresAtEpochSeconds());
        synchronized (lock) {
            if (sessionGeneration != generationAtStart
                    || !sameSessionVersion(sessionStore.load(), stored)) {
                return evaluateStoredLocked(sessionStore.load());
            }
            try {
                sessionStore.save(rotated);
            } catch (Exception persistenceError) {
                clearObservationLocked();
                return new AuthorizationDecision(
                        AuthorizationDecision.State.RECHECK_REQUIRED,
                        stored.userId(),
                        stored.organizationId(),
                        stored.role(),
                        0L);
            }
        }

        final long validatedAt = clock.wallEpochSeconds();
        final SupabaseAuthClient.StoredMembershipValidation validation;
        try {
            validation = backend.validateStoredMembership(
                    refreshed,
                    rotated,
                    validatedAt);
        } catch (SupabaseAuthClient.AuthException error) {
            synchronized (lock) {
                if (sessionGeneration != generationAtStart
                        || !sameSessionVersion(sessionStore.load(), rotated)) {
                    return evaluateStoredLocked(sessionStore.load());
                }
                if (error.isAuthenticationRejected()) {
                    failClosedPersistentlyLocked(rotated);
                    clearObservationLocked();
                    return new AuthorizationDecision(
                            AuthorizationDecision.State.SIGN_IN_REQUIRED,
                            stored.userId(),
                            stored.organizationId(),
                            stored.role(),
                            0L);
                }
                if (error.isTemporaryServerFailure()) {
                    setObservationLocked(
                            rotated,
                            RuntimeAuthorizationPolicy.Observation.indeterminate());
                    return evaluateStoredLocked(rotated);
                }
                failClosedPersistentlyLocked(rotated);
                clearObservationLocked();
                return evaluateStoredLocked(sessionStore.load());
            }
        } catch (IOException error) {
            synchronized (lock) {
                if (sessionGeneration != generationAtStart
                        || !sameSessionVersion(sessionStore.load(), rotated)) {
                    return evaluateStoredLocked(sessionStore.load());
                }
                setObservationLocked(
                        rotated,
                        RuntimeAuthorizationPolicy.Observation.indeterminate());
                return evaluateStoredLocked(rotated);
            }
        }

        if (!isCurrentSession(generationAtStart, rotated)) {
            return currentDecision();
        }

        synchronized (lock) {
            if (sessionGeneration != generationAtStart
                    || !sameSessionVersion(sessionStore.load(), rotated)) {
                return evaluateStoredLocked(sessionStore.load());
            }
            switch (validation.kind()) {
                case ACTIVE:
                    AuthSessionState active = validation.activeState();
                    if (active == null) {
                        failClosedPersistentlyLocked(rotated);
                        clearObservationLocked();
                        return evaluateStoredLocked(sessionStore.load());
                    }
                    try {
                        sessionStore.save(active);
                    } catch (Exception persistenceError) {
                        clearObservationLocked();
                        return new AuthorizationDecision(
                                AuthorizationDecision.State.RECHECK_REQUIRED,
                                stored.userId(),
                                stored.organizationId(),
                                stored.role(),
                                0L);
                    }
                    anchoredValidationWallSeconds =
                            active.lastMembershipValidatedAtEpochSeconds();
                    monotonicValidatedAtSeconds = clock.monotonicEpochSeconds();
                    setObservationLocked(
                            active,
                            RuntimeAuthorizationPolicy.Observation.active(
                                    active.userId(),
                                    active.organizationId(),
                                    active.membershipId(),
                                    active.role()));
                    return evaluateStoredLocked(active);

                case REVOKED:
                    AuthSessionState revoked = copyWithValidation(
                            rotated,
                            "REVOKED",
                            rotated.lastMembershipValidatedAtEpochSeconds());
                    persistAuthoritativeBlockLocked(revoked);
                    setObservationLocked(
                            revoked,
                            RuntimeAuthorizationPolicy.Observation.revoked(
                                    revoked.userId(),
                                    revoked.organizationId(),
                                    revoked.membershipId()));
                    return evaluateStoredLocked(revoked);

                case IDENTITY_MISMATCH:
                    AuthSessionState mismatched = copyWithValidation(
                            rotated,
                            rotated.membershipStatus(),
                            0L);
                    persistAuthoritativeBlockLocked(mismatched);
                    setObservationLocked(
                            mismatched,
                            RuntimeAuthorizationPolicy.Observation.identityMismatch());
                    return evaluateStoredLocked(mismatched);

                case NO_MEMBERSHIP:
                default:
                    AuthSessionState noMembership = copyWithValidation(
                            rotated,
                            rotated.membershipStatus(),
                            0L);
                    persistAuthoritativeBlockLocked(noMembership);
                    setObservationLocked(
                            noMembership,
                            RuntimeAuthorizationPolicy.Observation.noMembership());
                    return evaluateStoredLocked(noMembership);
            }
        }
    }

    private boolean isCurrentSession(
            long expectedGeneration,
            AuthSessionState expectedState) {
        synchronized (lock) {
            return sessionGeneration == expectedGeneration
                    && sameSessionVersion(sessionStore.load(), expectedState);
        }
    }

    private static boolean sameSessionVersion(
            AuthSessionState current,
            AuthSessionState expected) {
        return current != null
                && expected != null
                && current.userId().equals(expected.userId())
                && current.organizationId().equals(expected.organizationId())
                && current.membershipId().equals(expected.membershipId())
                && current.refreshToken().equals(expected.refreshToken());
    }

    private AuthorizationDecision evaluateStoredLocked(AuthSessionState stored) {
        if (stored == null) {
            clearObservationLocked();
            return policy.evaluate(
                    null,
                    RuntimeAuthorizationPolicy.Observation.none(),
                    clock.wallEpochSeconds(),
                    null);
        }

        RuntimeAuthorizationPolicy.Observation observation =
                observationForStoredLocked(stored);
        Long monotonicElapsed = monotonicElapsedForStoredLocked(stored);

        if (observation != null
                && observation == lastObservation
                && lastObservation.kind() == RuntimeAuthorizationPolicy.ObservationKind.AUTHORITATIVE_ACTIVE
                && monotonicElapsed != null
                && monotonicElapsed >= RuntimeAuthorizationPolicy.GRACE_SECONDS) {
            observation = RuntimeAuthorizationPolicy.Observation.none();
        }

        return policy.evaluate(
                stored,
                observation == null
                        ? RuntimeAuthorizationPolicy.Observation.none()
                        : observation,
                clock.wallEpochSeconds(),
                monotonicElapsed);
    }

    private RuntimeAuthorizationPolicy.Observation observationForStoredLocked(
            AuthSessionState stored) {
        if (!matchesObservedIdentityLocked(stored)) {
            clearObservationLocked();
            return RuntimeAuthorizationPolicy.Observation.none();
        }
        if (clock.wallEpochSeconds() < stored.lastMembershipValidatedAtEpochSeconds()) {
            return RuntimeAuthorizationPolicy.Observation.none();
        }
        return lastObservation;
    }

    private Long monotonicElapsedForStoredLocked(AuthSessionState stored) {
        if (monotonicValidatedAtSeconds < 0L
                || anchoredValidationWallSeconds <= 0L
                || anchoredValidationWallSeconds
                != stored.lastMembershipValidatedAtEpochSeconds()) {
            return null;
        }
        return clock.monotonicEpochSeconds() - monotonicValidatedAtSeconds;
    }

    private void setObservationLocked(
            AuthSessionState stored,
            RuntimeAuthorizationPolicy.Observation observation) {
        lastObservation = Objects.requireNonNull(observation, "observation");
        observedUserId = stored.userId();
        observedOrganizationId = stored.organizationId();
        observedMembershipId = stored.membershipId();
    }

    private boolean matchesObservedIdentityLocked(AuthSessionState stored) {
        return observedUserId != null
                && observedUserId.equals(stored.userId())
                && observedOrganizationId != null
                && observedOrganizationId.equals(stored.organizationId())
                && observedMembershipId != null
                && observedMembershipId.equals(stored.membershipId());
    }

    private void clearObservationLocked() {
        lastObservation = RuntimeAuthorizationPolicy.Observation.none();
        observedUserId = null;
        observedOrganizationId = null;
        observedMembershipId = null;
        monotonicValidatedAtSeconds = -1L;
        anchoredValidationWallSeconds = -1L;
    }

    private void failClosedPersistentlyLocked(AuthSessionState state) {
        AuthSessionState invalidated = copyWithValidation(
                state,
                state.membershipStatus(),
                0L);
        persistAuthoritativeBlockLocked(invalidated);
    }

    private void persistAuthoritativeBlockLocked(AuthSessionState blocked) {
        try {
            sessionStore.save(blocked);
        } catch (Exception persistenceError) {
            // A stale ACTIVE snapshot must not survive a known authoritative denial.
            sessionStore.clear();
        }
        monotonicValidatedAtSeconds = -1L;
        anchoredValidationWallSeconds = -1L;
    }

    private static AuthSessionState copyWithValidation(
            AuthSessionState state,
            String membershipStatus,
            long lastValidatedAtEpochSeconds) {
        return new AuthSessionState(
                state.accessToken(),
                state.refreshToken(),
                state.expiresAtEpochSeconds(),
                state.userId(),
                state.email(),
                state.organizationId(),
                state.organizationName(),
                state.membershipId(),
                state.role(),
                membershipStatus,
                lastValidatedAtEpochSeconds);
    }
}
