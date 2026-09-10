package com.inandout.fieldphotoprep;

import android.app.Application;

import java.io.File;
import java.io.IOException;

public final class FieldPhotoPrepApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PendingPhotoStore store = new PendingPhotoStore(new File(getFilesDir(), "pending_photos"));
        try {
            QueueStartupRecovery.reconcile(store);
        } catch (IOException ignored) {
            // Fail closed: leave the prior persisted queue state untouched. The photo screen will
            // surface unreadable/in-flight records and does not make them automatically retryable.
        }
    }
}
