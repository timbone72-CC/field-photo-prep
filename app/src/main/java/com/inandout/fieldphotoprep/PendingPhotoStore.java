package com.inandout.fieldphotoprep;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class PendingPhotoStore {
    static final String CAPTURE_SEQUENCE_LEDGER_FILE = "capture-sequences.properties";
    private static final String CAPTURE_SEQUENCE_RESET_TARGET_PREFIX = "__reuse_target__:";
    private static final String CAPTURE_SEQUENCE_ACTIVE_OCCURRENCE_PREFIX = "__reuse_active__:";
    private static final Object CAPTURE_SEQUENCE_LOCK = new Object();
    interface IdSource {
        String nextId();
    }

    interface TimeSource {
        long nowEpochMs();
    }

    public static final class ScanResult {
        private final List<PendingPhotoRecord> records;
        private final List<String> corruptMetadataFiles;
        private final List<String> unusableQueuedPhotoIds;
        private final List<String> uncertainPhotoIds;

        ScanResult(
                List<PendingPhotoRecord> records,
                List<String> corruptMetadataFiles,
                List<String> unusableQueuedPhotoIds,
                List<String> uncertainPhotoIds) {
            this.records = Collections.unmodifiableList(new ArrayList<>(records));
            this.corruptMetadataFiles = Collections.unmodifiableList(new ArrayList<>(corruptMetadataFiles));
            this.unusableQueuedPhotoIds = Collections.unmodifiableList(new ArrayList<>(unusableQueuedPhotoIds));
            this.uncertainPhotoIds = Collections.unmodifiableList(new ArrayList<>(uncertainPhotoIds));
        }

        public List<PendingPhotoRecord> records() {
            return records;
        }

        public List<String> corruptMetadataFiles() {
            return corruptMetadataFiles;
        }

        public List<String> unusableQueuedPhotoIds() {
            return unusableQueuedPhotoIds;
        }

        /**
         * Compatibility alias retained for Phase 5/6A callers. Phase 7A broadens this list to all
         * non-capturing, non-confirmed queue states that require a protected image.
         */
        public List<String> unusableWaitingPhotoIds() {
            return unusableQueuedPhotoIds;
        }

        public List<String> uncertainPhotoIds() {
            return uncertainPhotoIds;
        }
    }

    private final File root;
    private final IdSource idSource;
    private final TimeSource timeSource;

    public PendingPhotoStore(File root) {
        this(root, () -> UUID.randomUUID().toString(), System::currentTimeMillis);
    }

    PendingPhotoStore(File root, IdSource idSource, TimeSource timeSource) {
        this.root = root;
        this.idSource = idSource;
        this.timeSource = timeSource;
    }

    public PendingPhotoRecord beginCapture(DriveFolder address, DriveFolder workOrder) throws IOException {
        if (address == null || workOrder == null) {
            throw new IOException("An exact address and work order are required before capture.");
        }
        synchronized (CAPTURE_SEQUENCE_LOCK) {
            ensureRoot();
            int captureSequence = reserveNextCaptureSequence(workOrder);
            String id = idSource.nextId();
            PendingPhotoRecord record = PendingPhotoRecord.createCapturing(
                    id,
                    timeSource.nowEpochMs(),
                    address.id(),
                    address.name(),
                    workOrder.id(),
                    workOrder.name(),
                    captureSequence);
            File image = imageFile(record);
            File metadata = metadataFile(record);

            if (image.exists() || metadata.exists() || !image.createNewFile()) {
                throw new IOException("Could not reserve a unique local photo file.");
            }

            try {
                writeRecord(record);
                return record;
            } catch (IOException | RuntimeException error) {
                if (image.length() == 0) {
                    image.delete();
                }
                throw error;
            }
        }
    }

    /**
     * Persists the intent to turn one exact provider folder into a new dated work occurrence.
     * This happens before the existing reuse Drive write so another capture cannot race into the
     * old occurrence while reuse is in progress. Only confirmed old uploads may remain locally.
     */
    public void prepareCaptureSequenceResetForReuse(
            String workOrderId,
            String requestedWorkOrderName) throws IOException {
        requireText(workOrderId, "workOrderId");
        requireText(requestedWorkOrderName, "requestedWorkOrderName");
        synchronized (CAPTURE_SEQUENCE_LOCK) {
            ensureRoot();
            ScanResult existing = scan();
            if (!existing.corruptMetadataFiles().isEmpty()) {
                throw new IOException(
                        "Temporary photo metadata is unreadable. Resolve it before reusing this work-order folder.");
            }
            for (PendingPhotoRecord record : existing.records()) {
                if (workOrderId.equals(record.workOrderId())
                        && record.state() != PendingPhotoRecord.State.UPLOADED) {
                    throw new IOException(
                            "This work order still has an unconfirmed local photo ("
                                    + record.state().name()
                                    + "). Upload, reconcile, or safely discard it before reusing the folder.");
                }
            }

            Properties ledger = readCaptureSequenceLedger();
            String targetKey = resetTargetKey(workOrderId);
            String existingTarget = normalizeOptional(ledger.getProperty(targetKey));
            if (existingTarget != null && !existingTarget.equals(requestedWorkOrderName)) {
                throw new IOException(
                        "A different work-order reuse reset is already pending for this folder. Complete that reuse before starting another.");
            }
            ledger.setProperty(targetKey, requestedWorkOrderName);
            writeCaptureSequenceLedger(ledger);
        }
    }

    /**
     * Commits a new capture-order occurrence after Drive has verified the same provider ID under
     * the requested new dated name. Repeating completion for that same occurrence is a no-op.
     */
    public void completeCaptureSequenceResetForReuse(
            String workOrderId,
            String requestedWorkOrderName) throws IOException {
        requireText(workOrderId, "workOrderId");
        requireText(requestedWorkOrderName, "requestedWorkOrderName");
        synchronized (CAPTURE_SEQUENCE_LOCK) {
            ensureRoot();
            Properties ledger = readCaptureSequenceLedger();
            String targetKey = resetTargetKey(workOrderId);
            String pendingTarget = normalizeOptional(ledger.getProperty(targetKey));
            if (pendingTarget == null) {
                String activeOccurrence = readActiveOccurrenceName(ledger, workOrderId);
                if (requestedWorkOrderName.equals(activeOccurrence)) {
                    return;
                }
                throw new IOException("Capture-order reuse reset was not prepared for this work order.");
            }
            if (!pendingTarget.equals(requestedWorkOrderName)) {
                throw new IOException("Capture-order reuse reset target does not match the verified folder name.");
            }
            activateReuseReset(ledger, workOrderId, requestedWorkOrderName);
            ledger.remove(targetKey);
            writeCaptureSequenceLedger(ledger);
        }
    }

    private int reserveNextCaptureSequence(DriveFolder workOrder) throws IOException {
        String workOrderId = workOrder.id();
        Properties ledger = readCaptureSequenceLedger();
        String targetKey = resetTargetKey(workOrderId);
        String pendingTarget = normalizeOptional(ledger.getProperty(targetKey));
        if (pendingTarget != null) {
            if (!pendingTarget.equals(workOrder.name())) {
                throw new IOException(
                        "This work-order folder has an unfinished reuse reset. Refresh and complete the intended reuse before taking new photos.");
            }
            activateReuseReset(ledger, workOrderId, pendingTarget);
            ledger.remove(targetKey);
        }

        int persistedLast = readLedgerSequence(ledger, workOrderId);
        int baseline = persistedLast;
        String activeOccurrence = readActiveOccurrenceName(ledger, workOrderId);
        if (activeOccurrence == null) {
            ScanResult existing = scan();
            int retainedCount = 0;
            int maxStoredSequence = 0;
            for (PendingPhotoRecord record : existing.records()) {
                if (!workOrderId.equals(record.workOrderId())) {
                    continue;
                }
                retainedCount++;
                maxStoredSequence = Math.max(maxStoredSequence, record.captureSequence());
            }
            baseline = Math.max(persistedLast, Math.max(retainedCount, maxStoredSequence));
        } else if (!activeOccurrence.equals(workOrder.name())) {
            throw new IOException(
                    "This reused work-order folder no longer matches its active capture-order occurrence. Refresh before taking new photos.");
        }
        if (baseline == Integer.MAX_VALUE) {
            throw new IOException("Capture-order sequence is exhausted for this work order.");
        }
        int next = baseline + 1;
        ledger.setProperty(workOrderId, Integer.toString(next));
        writeCaptureSequenceLedger(ledger);
        return next;
    }

    private void activateReuseReset(
            Properties ledger,
            String workOrderId,
            String requestedWorkOrderName) {
        ledger.setProperty(workOrderId, "0");
        ledger.setProperty(activeOccurrenceKey(workOrderId), requestedWorkOrderName);
    }

    private Properties readCaptureSequenceLedger() throws IOException {
        File ledgerFile = new File(root, CAPTURE_SEQUENCE_LEDGER_FILE);
        ensureDirectChild(ledgerFile);
        Properties ledger = new Properties();
        if (!ledgerFile.exists()) {
            return ledger;
        }
        if (!ledgerFile.isFile()) {
            throw new IOException("Capture-order sequence ledger is not a readable file.");
        }
        try (FileInputStream input = new FileInputStream(ledgerFile)) {
            ledger.load(input);
        } catch (IOException | RuntimeException error) {
            throw new IOException("Capture-order sequence ledger could not be read safely.", error);
        }
        return ledger;
    }

    private int readLedgerSequence(Properties ledger, String workOrderId) throws IOException {
        String raw = ledger.getProperty(workOrderId);
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        final int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException error) {
            throw new IOException("Capture-order sequence ledger contains an invalid value.", error);
        }
        if (value < 0) {
            throw new IOException("Capture-order sequence ledger contains a negative value.");
        }
        return value;
    }

    private String readActiveOccurrenceName(Properties ledger, String workOrderId) {
        return normalizeOptional(ledger.getProperty(activeOccurrenceKey(workOrderId)));
    }

    /**
     * Local presentation helper only. Provider ID remains the upload destination identity; after
     * an FPP-managed reuse, the verified dated work-order name separates retained old history from
     * records captured for the newly reused occurrence.
     */
    public boolean isRecordInCurrentWorkOccurrence(
            PendingPhotoRecord record,
            DriveFolder workOrder) throws IOException {
        if (record == null || workOrder == null || !workOrder.id().equals(record.workOrderId())) {
            return false;
        }
        synchronized (CAPTURE_SEQUENCE_LOCK) {
            ensureRoot();
            String activeOccurrence = readActiveOccurrenceName(
                    readCaptureSequenceLedger(), workOrder.id());
            return activeOccurrence == null
                    || (activeOccurrence.equals(workOrder.name())
                    && activeOccurrence.equals(record.workOrderName()));
        }
    }

    public List<PendingPhotoRecord> recordsForWorkOccurrence(DriveFolder workOrder) throws IOException {
        if (workOrder == null) {
            return Collections.emptyList();
        }
        synchronized (CAPTURE_SEQUENCE_LOCK) {
            ensureRoot();
            Properties ledger = readCaptureSequenceLedger();
            String activeOccurrence = readActiveOccurrenceName(ledger, workOrder.id());
            if (activeOccurrence != null && !activeOccurrence.equals(workOrder.name())) {
                throw new IOException(
                        "The selected work-order name does not match its active reused occurrence.");
            }
            ScanResult result = scan();
            List<PendingPhotoRecord> matches = new ArrayList<>();
            for (PendingPhotoRecord record : result.records()) {
                if (!workOrder.id().equals(record.workOrderId())) {
                    continue;
                }
                if (activeOccurrence == null || activeOccurrence.equals(record.workOrderName())) {
                    matches.add(record);
                }
            }
            return matches;
        }
    }

    private void writeCaptureSequenceLedger(Properties ledger) throws IOException {
        File target = new File(root, CAPTURE_SEQUENCE_LEDGER_FILE);
        ensureDirectChild(target);
        File temp = new File(root, CAPTURE_SEQUENCE_LEDGER_FILE + ".tmp-" + UUID.randomUUID());
        ensureDirectChild(temp);
        try (FileOutputStream output = new FileOutputStream(temp)) {
            ledger.store(output, "Field Photo Prep capture sequence ledger");
            output.getFD().sync();
        } catch (IOException | RuntimeException error) {
            temp.delete();
            throw new IOException("Capture-order sequence ledger could not be written safely.", error);
        }
        try {
            try {
                Files.move(
                        temp.toPath(),
                        target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

    public PendingPhotoRecord finishCaptureIfImageExists(String id) throws IOException {
        PendingPhotoRecord record = getById(id);
        if (record == null) {
            throw new IOException("The pending capture record could not be found.");
        }
        if (record.state() != PendingPhotoRecord.State.CAPTURING) {
            throw new IOException("Only a capturing photo can finish camera capture.");
        }
        if (!hasImageData(record)) {
            removeEmptyReservation(record);
            return null;
        }
        PendingPhotoRecord waiting = record.withState(PendingPhotoRecord.State.WAITING);
        writeRecord(waiting);
        PhotoCaptureCompletionBus.publishPhotoWaiting(waiting.id());
        return waiting;
    }

    /**
     * Resolves only interrupted camera captures. Upload-process recovery is intentionally separate
     * and is invoked once from Application process startup, not every time a screen is recreated.
     */
    public ScanResult reconcileInterruptedCaptures() throws IOException {
        ScanResult before = scan();
        for (PendingPhotoRecord record : before.records()) {
            if (record.state() != PendingPhotoRecord.State.CAPTURING) {
                continue;
            }
            if (hasImageData(record)) {
                PendingPhotoRecord waiting = record.withState(PendingPhotoRecord.State.WAITING);
                writeRecord(waiting);
                PhotoCaptureCompletionBus.publishPhotoWaiting(waiting.id());
            } else {
                removeEmptyReservation(record);
            }
        }
        return scan();
    }

    /**
     * Converts persisted in-flight uploads into UNCERTAIN after process/app restart.
     *
     * A previous process may have reached the remote provider before dying, so an UPLOADING record
     * can never be made retryable merely because the process restarted.
     */
    public ScanResult reconcileInterruptedUploads() throws IOException {
        ScanResult before = scan();
        for (PendingPhotoRecord record : before.records()) {
            if (record.state() == PendingPhotoRecord.State.UPLOADING) {
                writeRecord(record.recoverInterruptedUpload());
            }
        }
        return scan();
    }

    public PendingPhotoRecord beginUploadAttempt(String id) throws IOException {
        PendingPhotoRecord record = requireRecord(id);
        if (!record.canBeginUploadAttempt()) {
            throw new IOException("This photo is not safe to start or retry automatically from state "
                    + record.state().name() + ".");
        }
        if (!hasImageData(record)) {
            throw new IOException("The protected original is missing or empty. Upload did not start.");
        }
        PendingPhotoRecord uploading;
        try {
            uploading = record.beginUploadAttempt(timeSource.nowEpochMs());
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new IOException("Could not begin a safe upload attempt.", error);
        }
        writeRecord(uploading);
        return uploading;
    }

    /**
     * Persists the exact provider identity returned by remote create before byte streaming begins.
     * This is duplicate-protection evidence only; it does not mean the photo is uploaded.
     */
    public PendingPhotoRecord recordProvisionalRemoteFileId(
            String id,
            String provisionalRemoteFileId) throws IOException {
        PendingPhotoRecord record = requireRecord(id);
        PendingPhotoRecord updated;
        try {
            updated = record.recordProvisionalRemoteFileId(provisionalRemoteFileId);
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new IOException(
                    "Could not persist provisional remote identity from the current state.",
                    error);
        }
        writeRecord(updated);
        return updated;
    }

    public PendingPhotoRecord markUploadFailed(String id, String detail) throws IOException {
        PendingPhotoRecord record = requireRecord(id);
        PendingPhotoRecord failed;
        try {
            failed = record.markUploadFailed(detail);
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new IOException("Could not mark upload failure from the current state.", error);
        }
        writeRecord(failed);
        return failed;
    }

    public PendingPhotoRecord markUploadUncertain(String id, String detail) throws IOException {
        PendingPhotoRecord record = requireRecord(id);
        PendingPhotoRecord uncertain;
        try {
            uncertain = record.markUploadUncertain(detail);
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new IOException("Could not mark the upload result uncertain from the current state.", error);
        }
        writeRecord(uncertain);
        return uncertain;
    }

    public PendingPhotoRecord resolveUncertainAsRetryableAbsence(String id, String detail)
            throws IOException {
        PendingPhotoRecord record = requireRecord(id);
        PendingPhotoRecord failed;
        try {
            failed = record.resolveUncertainAsRetryableAbsence(detail);
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new IOException(
                    "Could not release the uncertain upload for retry from the current evidence.",
                    error);
        }
        writeRecord(failed);
        return failed;
    }

    /**
     * Persists confirmed-success bookkeeping only. This method performs no remote operation and the
     * caller must supply the remote identity returned/verified by a later Drive integration phase.
     */
    public PendingPhotoRecord markUploadConfirmed(String id, String confirmedRemoteFileId)
            throws IOException {
        PendingPhotoRecord record = requireRecord(id);
        PendingPhotoRecord uploaded;
        try {
            uploaded = record.markUploadConfirmed(confirmedRemoteFileId);
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new IOException("Could not persist confirmed upload identity from the current state.", error);
        }
        writeRecord(uploaded);
        return uploaded;
    }

    public ScanResult scan() throws IOException {
        ensureRoot();
        List<PendingPhotoRecord> records = new ArrayList<>();
        List<String> corrupt = new ArrayList<>();
        List<String> unusableQueued = new ArrayList<>();
        List<String> uncertain = new ArrayList<>();

        File[] files = root.listFiles();
        if (files == null) {
            throw new IOException("Could not read temporary photo storage.");
        }
        for (File file : files) {
            if (!isMetadataFile(file.getName())) {
                continue;
            }
            try {
                PendingPhotoRecord record = readRecord(file);
                if (!file.getName().equals(record.metadataFileName())) {
                    throw new IllegalArgumentException("Metadata filename does not match pending-photo id.");
                }
                records.add(record);
                if (record.needsProtectedImage() && !hasImageData(record)) {
                    unusableQueued.add(record.id());
                }
                if (record.state() == PendingPhotoRecord.State.UNCERTAIN) {
                    uncertain.add(record.id());
                }
            } catch (IOException | RuntimeException error) {
                corrupt.add(file.getName());
            }
        }

        Collections.sort(records);
        Collections.sort(corrupt);
        Collections.sort(unusableQueued);
        Collections.sort(uncertain);
        return new ScanResult(records, corrupt, unusableQueued, uncertain);
    }

    public PendingPhotoRecord getById(String id) throws IOException {
        ensureRoot();
        File metadata = new File(root, PendingPhotoRecord.metadataFileNameFor(id));
        ensureDirectChild(metadata);
        if (!metadata.isFile()) {
            return null;
        }
        PendingPhotoRecord record = readRecord(metadata);
        if (!record.id().equals(id)) {
            throw new IOException("Pending-photo metadata identity mismatch.");
        }
        return record;
    }

    public List<PendingPhotoRecord> recordsForWorkOrder(String workOrderId) throws IOException {
        ScanResult result = scan();
        List<PendingPhotoRecord> matches = new ArrayList<>();
        for (PendingPhotoRecord record : result.records()) {
            if (record.workOrderId().equals(workOrderId)) {
                matches.add(record);
            }
        }
        return matches;
    }

    public File imageFile(PendingPhotoRecord record) throws IOException {
        ensureRoot();
        File file = new File(root, record.imageFileName());
        ensureDirectChild(file);
        return file;
    }

    public boolean hasImageData(PendingPhotoRecord record) throws IOException {
        File image = imageFile(record);
        return image.isFile() && image.length() > 0;
    }

    public void removeProtectedImageAfterConfirmedUpload(String id) throws IOException {
        PendingPhotoRecord record = requireRecord(id);
        if (record.state() != PendingPhotoRecord.State.UPLOADED || record.remoteFileId() == null) {
            throw new IOException(
                    "Protected-original cleanup requires durable confirmed uploaded state.");
        }
        File image = imageFile(record);
        if (image.exists() && !image.delete()) {
            throw new IOException("Could not remove the confirmed upload's protected local original.");
        }
    }

    public void discard(String id) throws IOException {
        PendingPhotoRecord record = requireRecord(id);
        if (!record.canDiscardLocally()) {
            throw new IOException("Refusing to discard a photo in " + record.state().name()
                    + " state because upload/duplicate-protection evidence may still be needed.");
        }
        File image = imageFile(record);
        if (image.exists() && !image.delete()) {
            throw new IOException("Could not remove the selected temporary image.");
        }
        File metadata = metadataFile(record);
        if (metadata.exists() && !metadata.delete()) {
            throw new IOException("Image was removed, but its local metadata could not be removed.");
        }
    }

    private PendingPhotoRecord requireRecord(String id) throws IOException {
        PendingPhotoRecord record = getById(id);
        if (record == null) {
            throw new IOException("The selected temporary photo no longer exists.");
        }
        return record;
    }

    private PendingPhotoRecord readRecord(File metadata) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(metadata)) {
            properties.load(input);
        }
        try {
            return PendingPhotoRecord.fromProperties(properties);
        } catch (IllegalArgumentException error) {
            throw new IOException("Invalid pending-photo metadata: " + metadata.getName(), error);
        }
    }

    private void writeRecord(PendingPhotoRecord record) throws IOException {
        ensureRoot();
        File target = metadataFile(record);
        File temp = new File(root, record.metadataFileName() + ".tmp-" + UUID.randomUUID());
        ensureDirectChild(temp);

        try (FileOutputStream output = new FileOutputStream(temp)) {
            record.toProperties().store(output, "Field Photo Prep pending photo");
            output.getFD().sync();
        } catch (IOException | RuntimeException error) {
            temp.delete();
            throw error;
        }

        try {
            try {
                Files.move(
                        temp.toPath(),
                        target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

    private void removeEmptyReservation(PendingPhotoRecord record) throws IOException {
        File image = imageFile(record);
        if (image.exists() && image.length() > 0) {
            throw new IOException("Refusing to discard a capture reservation that contains image data.");
        }
        if (image.exists() && !image.delete()) {
            throw new IOException("Could not remove an empty capture reservation.");
        }
        File metadata = metadataFile(record);
        if (metadata.exists() && !metadata.delete()) {
            throw new IOException("Could not remove empty capture metadata.");
        }
    }

    private File metadataFile(PendingPhotoRecord record) throws IOException {
        File file = new File(root, record.metadataFileName());
        ensureDirectChild(file);
        return file;
    }

    private void ensureRoot() throws IOException {
        if (root == null) {
            throw new IOException("Temporary photo storage is unavailable.");
        }
        if (!root.exists() && !root.mkdirs()) {
            throw new IOException("Could not create temporary photo storage.");
        }
        if (!root.isDirectory()) {
            throw new IOException("Temporary photo storage is not a directory.");
        }
    }

    private void ensureDirectChild(File file) throws IOException {
        File canonicalRoot = root.getCanonicalFile();
        File canonicalParent = file.getCanonicalFile().getParentFile();
        if (!canonicalRoot.equals(canonicalParent)) {
            throw new IOException("Pending-photo path escaped temporary storage.");
        }
    }

    private static String resetTargetKey(String workOrderId) {
        return CAPTURE_SEQUENCE_RESET_TARGET_PREFIX + workOrderId;
    }

    private static String activeOccurrenceKey(String workOrderId) {
        return CAPTURE_SEQUENCE_ACTIVE_OCCURRENCE_PREFIX + workOrderId;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireText(String value, String field) throws IOException {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IOException(field + " is required.");
        }
        return normalized;
    }

    private static boolean isMetadataFile(String name) {
        return name.startsWith("photo-") && name.endsWith(".properties");
    }
}
