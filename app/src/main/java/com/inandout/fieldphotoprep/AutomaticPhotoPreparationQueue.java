package com.inandout.fieldphotoprep;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Process-local serial scheduler for non-destructive prepared-photo creation.
 *
 * Durable recovery does not depend on this in-memory queue. The source of truth remains the
 * persisted WAITING photo record plus protected original. On process restart, eligible WAITING
 * photos are scanned and queued again.
 */
public final class AutomaticPhotoPreparationQueue implements PhotoCaptureCompletionBus.Listener {
    interface PreparationOperation {
        boolean hasPreparedCopy(String photoId) throws IOException;

        void prepare(PendingPhotoRecord record) throws IOException;
    }

    private final PendingPhotoStore photoStore;
    private final PhotoPreparationGate preparationGate;
    private final PreparationOperation preparationOperation;
    private final ArrayDeque<String> pendingPhotoIds = new ArrayDeque<>();
    private final Set<String> queuedPhotoIds = new HashSet<>();
    private final Map<String, String> lastFailures = new HashMap<>();

    private boolean workerRunning;
    private String activePhotoId;

    public AutomaticPhotoPreparationQueue(
            PendingPhotoStore photoStore,
            PhotoPreparer photoPreparer,
            PhotoPreparationGate preparationGate) {
        this(
                photoStore,
                preparationGate,
                new PreparationOperation() {
                    @Override
                    public boolean hasPreparedCopy(String photoId) throws IOException {
                        File prepared = photoPreparer.preparedFile(photoId);
                        return prepared.isFile() && prepared.length() > 0;
                    }

                    @Override
                    public void prepare(PendingPhotoRecord record) throws IOException {
                        photoPreparer.prepare(photoStore, record);
                    }
                });
    }

    AutomaticPhotoPreparationQueue(
            PendingPhotoStore photoStore,
            PhotoPreparationGate preparationGate,
            PreparationOperation preparationOperation) {
        if (photoStore == null || preparationGate == null || preparationOperation == null) {
            throw new IllegalArgumentException(
                    "Photo store, preparation gate, and preparation operation are required.");
        }
        this.photoStore = photoStore;
        this.preparationGate = preparationGate;
        this.preparationOperation = preparationOperation;
    }

    @Override
    public void onPhotoWaiting(String photoId) {
        enqueue(photoId);
    }

    public void enqueue(String photoId) {
        if (photoId == null || photoId.isBlank()) {
            return;
        }
        synchronized (this) {
            if (photoId.equals(activePhotoId) || !queuedPhotoIds.add(photoId)) {
                return;
            }
            pendingPhotoIds.addLast(photoId);
            startWorkerLocked();
        }
    }

    /**
     * Rebuilds process-local work from durable WAITING records after restart or missed delivery.
     */
    public void enqueueEligibleWaitingPhotos() throws IOException {
        PendingPhotoStore.ScanResult scan = photoStore.scan();
        for (PendingPhotoRecord record : scan.records()) {
            if (record.state() != PendingPhotoRecord.State.WAITING) {
                continue;
            }
            if (!photoStore.hasImageData(record)) {
                continue;
            }
            if (preparationOperation.hasPreparedCopy(record.id())) {
                continue;
            }
            enqueue(record.id());
        }
    }

    public synchronized boolean isQueuedOrActive(String photoId) {
        return photoId != null
                && (photoId.equals(activePhotoId) || queuedPhotoIds.contains(photoId));
    }

    public synchronized String activePhotoId() {
        return activePhotoId;
    }

    public synchronized int queuedCount() {
        return pendingPhotoIds.size() + (activePhotoId == null ? 0 : 1);
    }

    public synchronized String lastFailure(String photoId) {
        return photoId == null ? null : lastFailures.get(photoId);
    }

    boolean awaitIdleForTests(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + Math.max(0L, timeoutMs);
        synchronized (this) {
            while (workerRunning || activePhotoId != null || !pendingPhotoIds.isEmpty()) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0L) {
                    return false;
                }
                wait(remaining);
            }
            return true;
        }
    }

    private void startWorkerLocked() {
        if (workerRunning) {
            return;
        }
        workerRunning = true;
        Thread worker = new Thread(this::drainLoop, "FieldPhotoPrep-auto-prepare");
        try {
            worker.start();
        } catch (RuntimeException | Error error) {
            workerRunning = false;
            notifyAll();
            throw error;
        }
    }

    private void drainLoop() {
        while (true) {
            String photoId;
            synchronized (this) {
                photoId = pendingPhotoIds.pollFirst();
                if (photoId == null) {
                    workerRunning = false;
                    activePhotoId = null;
                    notifyAll();
                    return;
                }
                queuedPhotoIds.remove(photoId);
                activePhotoId = photoId;
            }

            try {
                prepareOne(photoId);
                synchronized (this) {
                    lastFailures.remove(photoId);
                }
            } catch (Throwable error) {
                synchronized (this) {
                    lastFailures.put(photoId, safeMessage(error));
                }
            } finally {
                synchronized (this) {
                    if (photoId.equals(activePhotoId)) {
                        activePhotoId = null;
                    }
                    notifyAll();
                }
            }
        }
    }

    private void prepareOne(String photoId) throws IOException, InterruptedException {
        PendingPhotoRecord record = photoStore.getById(photoId);
        if (!isEligible(record)) {
            return;
        }
        if (preparationOperation.hasPreparedCopy(photoId)) {
            return;
        }

        acquirePreparationGate(photoId);
        try {
            record = photoStore.getById(photoId);
            if (!isEligible(record)) {
                return;
            }
            if (preparationOperation.hasPreparedCopy(photoId)) {
                return;
            }
            preparationOperation.prepare(record);
        } finally {
            preparationGate.finish(photoId);
        }
    }

    private boolean isEligible(PendingPhotoRecord record) throws IOException {
        return record != null
                && record.state() == PendingPhotoRecord.State.WAITING
                && photoStore.hasImageData(record);
    }

    private void acquirePreparationGate(String photoId) throws InterruptedException {
        while (!preparationGate.tryBegin(photoId)) {
            Thread.sleep(25L);
        }
    }

    private static String safeMessage(Throwable error) {
        if (error == null) {
            return "Automatic preparation failed.";
        }
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message;
    }
}
