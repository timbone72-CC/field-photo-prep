package com.inandout.fieldphotoprep;

import android.app.Application;
import android.os.SystemClock;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraXConfig;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FieldPhotoPrepApplication extends Application implements CameraXConfig.Provider {
    private AutomaticPhotoPreparationQueue automaticPreparationQueue;
    private ExecutorService authorizationExecutor;
    private RuntimeAuthorizationManager authorizationManager;

    @Override
    public CameraXConfig getCameraXConfig() {
        return Camera2Config.defaultConfig();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SecureAuthStore secureAuthStore = new SecureAuthStore(this);
        SupabaseAuthClient authClient = new SupabaseAuthClient();
        authorizationExecutor = Executors.newSingleThreadExecutor();
        authorizationManager = new RuntimeAuthorizationManager(
                new RuntimeAuthorizationManager.SessionStore() {
                    @Override
                    public AuthSessionState load() {
                        return secureAuthStore.load();
                    }

                    @Override
                    public void save(AuthSessionState state) throws Exception {
                        secureAuthStore.save(state);
                    }

                    @Override
                    public void clear() {
                        secureAuthStore.clear();
                    }
                },
                new RuntimeAuthorizationManager.Backend() {
                    @Override
                    public SupabaseAuthClient.AuthTokens refreshSession(String refreshToken)
                            throws IOException {
                        return authClient.refreshSession(refreshToken);
                    }

                    @Override
                    public SupabaseAuthClient.StoredMembershipValidation validateStoredMembership(
                            SupabaseAuthClient.AuthTokens tokens,
                            AuthSessionState stored,
                            long validatedAtEpochSeconds) throws IOException {
                        return authClient.validateStoredMembership(
                                tokens,
                                stored,
                                validatedAtEpochSeconds);
                    }
                },
                new RuntimeAuthorizationManager.Clock() {
                    @Override
                    public long wallEpochSeconds() {
                        return System.currentTimeMillis() / 1000L;
                    }

                    @Override
                    public long monotonicEpochSeconds() {
                        return SystemClock.elapsedRealtime() / 1000L;
                    }
                },
                authorizationExecutor);

        // Startup validation is asynchronous and coalesces with any Activity foreground recheck.
        // Existing protected-photo recovery remains independent of account-service availability.
        authorizationManager.revalidateAsync();

        PendingPhotoStore store = new PendingPhotoStore(new File(getFilesDir(), "pending_photos"));
        PhotoPreparer preparer = new PhotoPreparer(new File(getFilesDir(), "prepared_photos"));
        automaticPreparationQueue = new AutomaticPhotoPreparationQueue(
                store,
                preparer,
                new PhotoPreparationGate());

        boolean startupRecoverySucceeded = true;
        try {
            QueueStartupRecovery.reconcile(store);
        } catch (IOException ignored) {
            startupRecoverySucceeded = false;
            // Fail closed: leave prior persisted queue state untouched. The photo screen will
            // surface unreadable/in-flight records and does not make them automatically retryable.
        }

        PhotoCaptureCompletionBus.setListener(automaticPreparationQueue);

        if (startupRecoverySucceeded) {
            try {
                automaticPreparationQueue.enqueueEligibleWaitingPhotos();
            } catch (IOException ignored) {
                // Durable WAITING originals remain intact. A later capture event, app restart, or
                // manual Prepare action can recover preparation without changing queue identity.
            }
        }
    }

    AutomaticPhotoPreparationQueue automaticPreparationQueue() {
        return automaticPreparationQueue;
    }

    RuntimeAuthorizationManager authorizationManager() {
        return authorizationManager;
    }
}
