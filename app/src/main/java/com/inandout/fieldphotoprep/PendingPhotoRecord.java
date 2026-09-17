package com.inandout.fieldphotoprep;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

public final class PendingPhotoRecord implements Comparable<PendingPhotoRecord> {
    public enum State {
        CAPTURING,
        WAITING,
        UPLOADING,
        FAILED,
        UNCERTAIN,
        UPLOADED
    }

    static final int CURRENT_SCHEMA_VERSION = 4;

    private static final String KEY_SCHEMA_VERSION = "schemaVersion";
    private static final String KEY_ID = "id";
    private static final String KEY_IMAGE_FILE = "imageFile";
    private static final String KEY_STATE = "state";
    private static final String KEY_CREATED_AT = "createdAtEpochMs";
    private static final String KEY_CAPTURE_SEQUENCE = "captureSequence";
    private static final String KEY_ADDRESS_ID = "addressId";
    private static final String KEY_ADDRESS_NAME = "addressName";
    private static final String KEY_WORK_ORDER_ID = "workOrderId";
    private static final String KEY_WORK_ORDER_NAME = "workOrderName";
    private static final String KEY_UPLOAD_ATTEMPT_COUNT = "uploadAttemptCount";
    private static final String KEY_LAST_ATTEMPT_AT = "lastAttemptAtEpochMs";
    private static final String KEY_STATUS_DETAIL = "statusDetail";
    private static final String KEY_PROVISIONAL_REMOTE_FILE_ID = "provisionalRemoteFileId";
    private static final String KEY_REMOTE_FILE_ID = "remoteFileId";

    private final String id;
    private final String imageFileName;
    private final State state;
    private final long createdAtEpochMs;
    private final int captureSequence;
    private final String addressId;
    private final String addressName;
    private final String workOrderId;
    private final String workOrderName;
    private final int uploadAttemptCount;
    private final long lastAttemptAtEpochMs;
    private final String statusDetail;
    private final String provisionalRemoteFileId;
    private final String remoteFileId;

    public PendingPhotoRecord(
            String id,
            String imageFileName,
            State state,
            long createdAtEpochMs,
            String addressId,
            String addressName,
            String workOrderId,
            String workOrderName) {
        this(
                id,
                imageFileName,
                state,
                createdAtEpochMs,
                0,
                addressId,
                addressName,
                workOrderId,
                workOrderName,
                0,
                0L,
                null,
                null,
                null);
    }

    private PendingPhotoRecord(
            String id,
            String imageFileName,
            State state,
            long createdAtEpochMs,
            int captureSequence,
            String addressId,
            String addressName,
            String workOrderId,
            String workOrderName,
            int uploadAttemptCount,
            long lastAttemptAtEpochMs,
            String statusDetail,
            String provisionalRemoteFileId,
            String remoteFileId) {
        this.id = requireUuid(id);
        this.imageFileName = requireExpectedImageFileName(id, imageFileName);
        this.state = Objects.requireNonNull(state, "state");
        if (createdAtEpochMs <= 0) {
            throw new IllegalArgumentException("createdAtEpochMs must be positive.");
        }
        this.createdAtEpochMs = createdAtEpochMs;
        if (captureSequence < 0) {
            throw new IllegalArgumentException("captureSequence cannot be negative.");
        }
        this.captureSequence = captureSequence;
        this.addressId = requireText(addressId, "addressId");
        this.addressName = requireText(addressName, "addressName");
        this.workOrderId = requireText(workOrderId, "workOrderId");
        this.workOrderName = requireText(workOrderName, "workOrderName");
        this.uploadAttemptCount = uploadAttemptCount;
        this.lastAttemptAtEpochMs = lastAttemptAtEpochMs;
        this.statusDetail = normalizeOptional(statusDetail);
        this.provisionalRemoteFileId = normalizeOptional(provisionalRemoteFileId);
        this.remoteFileId = normalizeOptional(remoteFileId);
        validateQueueState();
    }

    public static PendingPhotoRecord createCapturing(
            String id,
            long createdAtEpochMs,
            String addressId,
            String addressName,
            String workOrderId,
            String workOrderName) {
        return new PendingPhotoRecord(
                id,
                imageFileNameFor(id),
                State.CAPTURING,
                createdAtEpochMs,
                addressId,
                addressName,
                workOrderId,
                workOrderName);
    }

    public static PendingPhotoRecord createCapturing(
            String id,
            long createdAtEpochMs,
            String addressId,
            String addressName,
            String workOrderId,
            String workOrderName,
            int captureSequence) {
        if (captureSequence <= 0) {
            throw new IllegalArgumentException("New captureSequence must be positive.");
        }
        return new PendingPhotoRecord(
                id,
                imageFileNameFor(id),
                State.CAPTURING,
                createdAtEpochMs,
                captureSequence,
                addressId,
                addressName,
                workOrderId,
                workOrderName,
                0,
                0L,
                null,
                null,
                null);
    }

    public String id() {
        return id;
    }

    public String imageFileName() {
        return imageFileName;
    }

    public String metadataFileName() {
        return metadataFileNameFor(id);
    }

    public State state() {
        return state;
    }

    public long createdAtEpochMs() {
        return createdAtEpochMs;
    }

    public int captureSequence() {
        return captureSequence;
    }

    public boolean hasCaptureSequence() {
        return captureSequence > 0;
    }

    public String addressId() {
        return addressId;
    }

    public String addressName() {
        return addressName;
    }

    public String workOrderId() {
        return workOrderId;
    }

    public String workOrderName() {
        return workOrderName;
    }

    public int uploadAttemptCount() {
        return uploadAttemptCount;
    }

    public long lastAttemptAtEpochMs() {
        return lastAttemptAtEpochMs;
    }

    public String statusDetail() {
        return statusDetail;
    }

    public String provisionalRemoteFileId() {
        return provisionalRemoteFileId;
    }

    public String remoteFileId() {
        return remoteFileId;
    }

    public boolean canBeginUploadAttempt() {
        return state == State.WAITING || state == State.FAILED;
    }

    public boolean canDiscardLocally() {
        return state == State.WAITING || state == State.FAILED;
    }

    public boolean needsProtectedImage() {
        return state == State.WAITING
                || state == State.UPLOADING
                || state == State.FAILED
                || state == State.UNCERTAIN;
    }

    public PendingPhotoRecord withState(State newState) {
        Objects.requireNonNull(newState, "newState");
        if ((state != State.CAPTURING && state != State.WAITING)
                || (newState != State.CAPTURING && newState != State.WAITING)) {
            throw new IllegalStateException("Capture-state helper cannot change upload queue state.");
        }
        return copy(newState, 0, 0L, null, null, null);
    }

    public PendingPhotoRecord beginUploadAttempt(long nowEpochMs) {
        if (!canBeginUploadAttempt()) {
            throw new IllegalStateException("Only waiting or failed photos can begin an upload attempt.");
        }
        if (nowEpochMs <= 0) {
            throw new IllegalArgumentException("Upload attempt time must be positive.");
        }
        if (uploadAttemptCount == Integer.MAX_VALUE) {
            throw new IllegalStateException("Upload attempt count cannot be incremented safely.");
        }
        return copy(State.UPLOADING, uploadAttemptCount + 1, nowEpochMs, null, null, null);
    }

    public PendingPhotoRecord recordProvisionalRemoteFileId(String provisionalRemoteFileId) {
        if (state != State.UPLOADING) {
            throw new IllegalStateException("Only an in-flight upload can record provisional remote identity.");
        }
        String requiredId = requireText(
                provisionalRemoteFileId,
                "provisional remote file identity");
        if (this.provisionalRemoteFileId != null) {
            if (!this.provisionalRemoteFileId.equals(requiredId)) {
                throw new IllegalStateException(
                        "An upload cannot replace its provisional remote identity.");
            }
            return this;
        }
        return copy(
                State.UPLOADING,
                uploadAttemptCount,
                lastAttemptAtEpochMs,
                null,
                requiredId,
                null);
    }

    public PendingPhotoRecord markUploadFailed(String detail) {
        if (state != State.UPLOADING) {
            throw new IllegalStateException("Only an in-flight upload can be marked failed.");
        }
        if (provisionalRemoteFileId != null) {
            throw new IllegalStateException(
                    "An upload with provisional remote identity cannot become retryable failure.");
        }
        return copy(State.FAILED, uploadAttemptCount, lastAttemptAtEpochMs,
                requireText(detail, "failure detail"), null, null);
    }

    public PendingPhotoRecord markUploadUncertain(String detail) {
        if (state != State.UPLOADING) {
            throw new IllegalStateException("Only an in-flight upload can become uncertain.");
        }
        return copy(State.UNCERTAIN, uploadAttemptCount, lastAttemptAtEpochMs,
                requireText(detail, "uncertainty detail"), provisionalRemoteFileId, null);
    }

    public PendingPhotoRecord resolveUncertainAsRetryableAbsence(String detail) {
        if (state != State.UNCERTAIN) {
            throw new IllegalStateException(
                    "Only an uncertain upload can be released for retry after reconciliation.");
        }
        if (provisionalRemoteFileId != null) {
            throw new IllegalStateException(
                    "Unresolved provisional remote identity blocks retry release.");
        }
        return copy(
                State.FAILED,
                uploadAttemptCount,
                lastAttemptAtEpochMs,
                requireText(detail, "reconciliation detail"),
                null,
                null);
    }

    public PendingPhotoRecord recoverInterruptedUpload() {
        if (state != State.UPLOADING) {
            return this;
        }
        return markUploadUncertain(
                "Upload was interrupted before local confirmation. Remote result must be reconciled before retry.");
    }

    public PendingPhotoRecord markUploadConfirmed(String confirmedRemoteFileId) {
        if (state != State.UPLOADING && state != State.UNCERTAIN) {
            throw new IllegalStateException("Only an in-flight or uncertain upload can be confirmed.");
        }
        String requiredId = requireText(
                confirmedRemoteFileId,
                "confirmed remote file identity");
        if (provisionalRemoteFileId != null && !provisionalRemoteFileId.equals(requiredId)) {
            throw new IllegalStateException(
                    "Confirmed remote identity must match the recorded provisional identity.");
        }
        return copy(State.UPLOADED, uploadAttemptCount, lastAttemptAtEpochMs,
                null, null, requiredId);
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.setProperty(KEY_SCHEMA_VERSION, Integer.toString(CURRENT_SCHEMA_VERSION));
        properties.setProperty(KEY_ID, id);
        properties.setProperty(KEY_IMAGE_FILE, imageFileName);
        properties.setProperty(KEY_STATE, state.name());
        properties.setProperty(KEY_CREATED_AT, Long.toString(createdAtEpochMs));
        properties.setProperty(KEY_CAPTURE_SEQUENCE, Integer.toString(captureSequence));
        properties.setProperty(KEY_ADDRESS_ID, addressId);
        properties.setProperty(KEY_ADDRESS_NAME, addressName);
        properties.setProperty(KEY_WORK_ORDER_ID, workOrderId);
        properties.setProperty(KEY_WORK_ORDER_NAME, workOrderName);
        properties.setProperty(KEY_UPLOAD_ATTEMPT_COUNT, Integer.toString(uploadAttemptCount));
        properties.setProperty(KEY_LAST_ATTEMPT_AT, Long.toString(lastAttemptAtEpochMs));
        properties.setProperty(KEY_STATUS_DETAIL, statusDetail == null ? "" : statusDetail);
        properties.setProperty(
                KEY_PROVISIONAL_REMOTE_FILE_ID,
                provisionalRemoteFileId == null ? "" : provisionalRemoteFileId);
        properties.setProperty(KEY_REMOTE_FILE_ID, remoteFileId == null ? "" : remoteFileId);
        return properties;
    }

    public static PendingPhotoRecord fromProperties(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        int schemaVersion = parseSchemaVersion(properties);
        String id = requiredProperty(properties, KEY_ID);
        String imageFile = requiredProperty(properties, KEY_IMAGE_FILE);
        State state = parseState(properties);
        long createdAt = parsePositiveLong(properties, KEY_CREATED_AT,
                "Invalid pending-photo createdAtEpochMs.");
        int captureSequence = schemaVersion >= 4
                ? parseNonNegativeInt(properties, KEY_CAPTURE_SEQUENCE)
                : 0;

        if (schemaVersion == 1) {
            if (state != State.CAPTURING && state != State.WAITING) {
                throw new IllegalArgumentException(
                        "Legacy pending-photo state is not valid for schema version 1.");
            }
            return new PendingPhotoRecord(
                    id,
                    imageFile,
                    state,
                    createdAt,
                    0,
                    requiredProperty(properties, KEY_ADDRESS_ID),
                    requiredProperty(properties, KEY_ADDRESS_NAME),
                    requiredProperty(properties, KEY_WORK_ORDER_ID),
                    requiredProperty(properties, KEY_WORK_ORDER_NAME),
                    0,
                    0L,
                    null,
                    null,
                    null);
        }

        int attemptCount = parseNonNegativeInt(properties, KEY_UPLOAD_ATTEMPT_COUNT);
        long lastAttemptAt = parseNonNegativeLong(properties, KEY_LAST_ATTEMPT_AT);
        String provisionalRemoteFileId = schemaVersion >= 3
                ? optionalProperty(properties, KEY_PROVISIONAL_REMOTE_FILE_ID)
                : null;
        return new PendingPhotoRecord(
                id,
                imageFile,
                state,
                createdAt,
                captureSequence,
                requiredProperty(properties, KEY_ADDRESS_ID),
                requiredProperty(properties, KEY_ADDRESS_NAME),
                requiredProperty(properties, KEY_WORK_ORDER_ID),
                requiredProperty(properties, KEY_WORK_ORDER_NAME),
                attemptCount,
                lastAttemptAt,
                optionalProperty(properties, KEY_STATUS_DETAIL),
                provisionalRemoteFileId,
                optionalProperty(properties, KEY_REMOTE_FILE_ID));
    }

    public static String imageFileNameFor(String id) {
        return "photo-" + requireUuid(id) + ".jpg";
    }

    public static String metadataFileNameFor(String id) {
        return "photo-" + requireUuid(id) + ".properties";
    }

    @Override
    public int compareTo(PendingPhotoRecord other) {
        int byTime = Long.compare(createdAtEpochMs, other.createdAtEpochMs);
        return byTime != 0 ? byTime : id.compareTo(other.id);
    }

    private PendingPhotoRecord copy(
            State nextState,
            int nextAttemptCount,
            long nextLastAttemptAt,
            String nextStatusDetail,
            String nextProvisionalRemoteFileId,
            String nextRemoteFileId) {
        return new PendingPhotoRecord(
                id,
                imageFileName,
                nextState,
                createdAtEpochMs,
                captureSequence,
                addressId,
                addressName,
                workOrderId,
                workOrderName,
                nextAttemptCount,
                nextLastAttemptAt,
                nextStatusDetail,
                nextProvisionalRemoteFileId,
                nextRemoteFileId);
    }

    private void validateQueueState() {
        if (uploadAttemptCount < 0) {
            throw new IllegalArgumentException("uploadAttemptCount cannot be negative.");
        }
        if (lastAttemptAtEpochMs < 0) {
            throw new IllegalArgumentException("lastAttemptAtEpochMs cannot be negative.");
        }
        if (uploadAttemptCount == 0 && lastAttemptAtEpochMs != 0L) {
            throw new IllegalArgumentException(
                    "A photo with no upload attempts cannot have a last-attempt time.");
        }
        if (uploadAttemptCount > 0 && lastAttemptAtEpochMs <= 0L) {
            throw new IllegalArgumentException(
                    "An attempted upload requires a positive last-attempt time.");
        }

        switch (state) {
            case CAPTURING:
            case WAITING:
                if (uploadAttemptCount != 0 || lastAttemptAtEpochMs != 0L
                        || statusDetail != null || provisionalRemoteFileId != null
                        || remoteFileId != null) {
                    throw new IllegalArgumentException(
                            "Capture/waiting state cannot contain upload-result bookkeeping.");
                }
                break;
            case UPLOADING:
                if (uploadAttemptCount <= 0 || statusDetail != null || remoteFileId != null) {
                    throw new IllegalArgumentException(
                            "Uploading state requires an attempt and no confirmed result bookkeeping yet.");
                }
                break;
            case FAILED:
                if (uploadAttemptCount <= 0 || statusDetail == null
                        || provisionalRemoteFileId != null || remoteFileId != null) {
                    throw new IllegalArgumentException(
                            "Failed state requires attempt detail and no remote identity evidence.");
                }
                break;
            case UNCERTAIN:
                if (uploadAttemptCount <= 0 || statusDetail == null || remoteFileId != null) {
                    throw new IllegalArgumentException(
                            "Uncertain state requires attempt detail and no confirmed remote identity.");
                }
                break;
            case UPLOADED:
                if (uploadAttemptCount <= 0 || statusDetail != null
                        || provisionalRemoteFileId != null || remoteFileId == null) {
                    throw new IllegalArgumentException(
                            "Uploaded state requires one confirmed remote identity and no provisional identity.");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported pending-photo state.");
        }
    }

    private static int parseSchemaVersion(Properties properties) {
        String raw = properties.getProperty(KEY_SCHEMA_VERSION);
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        int version;
        try {
            version = Integer.parseInt(raw.trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Invalid pending-photo schema version.", error);
        }
        if (version != 1 && version != 2 && version != 3
                && version != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported pending-photo schema version: " + version);
        }
        return version;
    }

    private static State parseState(Properties properties) {
        try {
            return State.valueOf(requiredProperty(properties, KEY_STATE));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid pending-photo state.", error);
        }
    }

    private static long parsePositiveLong(Properties properties, String key, String message) {
        long value;
        try {
            value = Long.parseLong(requiredProperty(properties, key));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(message, error);
        }
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static int parseNonNegativeInt(Properties properties, String key) {
        int value;
        try {
            value = Integer.parseInt(requiredProperty(properties, key));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "Invalid pending-photo uploadAttemptCount.", error);
        }
        if (value < 0) {
            throw new IllegalArgumentException("Invalid pending-photo uploadAttemptCount.");
        }
        return value;
    }

    private static long parseNonNegativeLong(Properties properties, String key) {
        long value;
        try {
            value = Long.parseLong(requiredProperty(properties, key));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "Invalid pending-photo lastAttemptAtEpochMs.", error);
        }
        if (value < 0) {
            throw new IllegalArgumentException("Invalid pending-photo lastAttemptAtEpochMs.");
        }
        return value;
    }

    private static String requireUuid(String value) {
        String text = requireText(value, "id");
        try {
            UUID.fromString(text);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Pending-photo id must be a UUID.", error);
        }
        return text;
    }

    private static String requireExpectedImageFileName(String id, String imageFileName) {
        String expected = "photo-" + requireUuid(id) + ".jpg";
        if (!expected.equals(imageFileName)) {
            throw new IllegalArgumentException(
                    "Pending-photo image filename does not match its id.");
        }
        return expected;
    }

    private static String requiredProperty(Properties properties, String key) {
        return requireText(properties.getProperty(key), key);
    }

    private static String optionalProperty(Properties properties, String key) {
        return normalizeOptional(properties.getProperty(key));
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }
}
