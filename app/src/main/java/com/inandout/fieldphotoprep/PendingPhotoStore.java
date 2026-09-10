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
        ensureRoot();

        String id = idSource.nextId();
        PendingPhotoRecord record = PendingPhotoRecord.createCapturing(
                id,
                timeSource.nowEpochMs(),
                address.id(),
                address.name(),
                workOrder.id(),
                workOrder.name());
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
                writeRecord(record.withState(PendingPhotoRecord.State.WAITING));
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

    private static boolean isMetadataFile(String name) {
        return name.startsWith("photo-") && name.endsWith(".properties");
    }
}
