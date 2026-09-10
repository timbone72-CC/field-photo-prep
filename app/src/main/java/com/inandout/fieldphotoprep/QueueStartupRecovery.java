package com.inandout.fieldphotoprep;

import java.io.IOException;

final class QueueStartupRecovery {
    private QueueStartupRecovery() {
    }

    static PendingPhotoStore.ScanResult reconcile(PendingPhotoStore store) throws IOException {
        if (store == null) {
            throw new IOException("Temporary photo storage is unavailable for queue recovery.");
        }
        return store.reconcileInterruptedUploads();
    }
}
