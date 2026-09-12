package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraXConfig;

import java.io.File;
import java.io.IOException;

public final class FieldPhotoPrepApplication extends Application implements CameraXConfig.Provider {
    private AutomaticPhotoPreparationQueue automaticPreparationQueue;

    @Override
    public CameraXConfig getCameraXConfig() {
        return Camera2Config.defaultConfig();
    }

    @Override
    public void onCreate() {
        super.onCreate();

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

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {
                // Presentation decoration runs after Activity.onResume so the owned content view and
                // latest queue render are already present. CameraCaptureActivity is intentionally
                // excluded and remains a locked design surface.
                if (activity instanceof MainActivity) {
                    MainScreenDecorator.decorate(activity);
                } else if (activity instanceof PhotoCaptureActivity) {
                    PhotoScreenDecorator.decorate(activity);
                    Concept3PhotoEnhancer.enhance(activity);
                }
            }

            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    AutomaticPhotoPreparationQueue automaticPreparationQueue() {
        return automaticPreparationQueue;
    }
}
