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
        private final List<String> unusableWaitingPhotoIds;

        ScanResult(
                List<PendingPhotoRecord> records,
                List<String> corruptMetadataFiles,
                List<String> unusableWaitingPhotoIds) {
            this.records = Collections.unmodifiableList(new ArrayList<>(records));
            this.corruptMetadataFiles = Collections.unmodifiableList(new ArrayList<>(corruptMetadataFiles));
            this.unusableWaitingPhotoIds = Collections.unmodifiableList(new ArrayList<>(unusableWaitingPhotoIds));
        }

        public List<PendingPhotoRecord> records() {
            return records;
        }

        public List<String> corruptMetadataFiles() {
            return corruptMetadataFiles;
        }

        public List<String> unusableWaitingPhotoIds() {
            return unusableWaitingPhotoIds;
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
        if (!hasImageData(record)) {
            removeEmptyReservation(record);
            return null;
        }
        PendingPhotoRecord waiting = record.withState(PendingPhotoRecord.State.WAITING);
        writeRecord(waiting);
        return waiting;
    }

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

    public ScanResult scan() throws IOException {
        ensureRoot();
        List<PendingPhotoRecord> records = new ArrayList<>();
        List<String> corrupt = new ArrayList<>();
        List<String> unusableWaiting = new ArrayList<>();

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
                if (record.state() == PendingPhotoRecord.State.WAITING && !hasImageData(record)) {
                    unusableWaiting.add(record.id());
                }
            } catch (IOException | RuntimeException error) {
                corrupt.add(file.getName());
            }
        }

        Collections.sort(records);
        Collections.sort(corrupt);
        Collections.sort(unusableWaiting);
        return new ScanResult(records, corrupt, unusableWaiting);
    }

    public PendingPhotoRecord getById(String id) throws IOException {
        ensureRoot();
        File metadata = new File(root, PendingPhotoRecord.metadataFileNameFor(id));
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
        PendingPhotoRecord record = getById(id);
        if (record == null) {
            throw new IOException("The selected temporary photo no longer exists.");
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
