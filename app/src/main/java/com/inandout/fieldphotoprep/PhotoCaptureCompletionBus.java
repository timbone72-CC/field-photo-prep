package com.inandout.fieldphotoprep;

/**
 * Process-local notification that a protected photo has durably reached WAITING state.
 *
 * Delivery is intentionally best-effort and non-throwing. A downstream preparation scheduling
 * failure must never turn an already-durable camera capture into a failed capture.
 */
public final class PhotoCaptureCompletionBus {
    public interface Listener {
        void onPhotoWaiting(String photoId);
    }

    private static volatile Listener listener;

    private PhotoCaptureCompletionBus() {
    }

    public static void setListener(Listener newListener) {
        listener = newListener;
    }

    static void publishPhotoWaiting(String photoId) {
        if (photoId == null || photoId.isBlank()) {
            return;
        }
        Listener current = listener;
        if (current == null) {
            return;
        }
        try {
            current.onPhotoWaiting(photoId);
        } catch (RuntimeException ignored) {
            // Capture is already durable. Preparation can be recovered from WAITING state later.
        }
    }

    static void clearListenerForTests() {
        listener = null;
    }
}
