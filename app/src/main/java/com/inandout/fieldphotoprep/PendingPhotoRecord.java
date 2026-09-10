package com.inandout.fieldphotoprep;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

public final class PendingPhotoRecord implements Comparable<PendingPhotoRecord> {
    public enum State {
        CAPTURING,
        WAITING
    }

    private static final String KEY_ID = "id";
    private static final String KEY_IMAGE_FILE = "imageFile";
    private static final String KEY_STATE = "state";
    private static final String KEY_CREATED_AT = "createdAtEpochMs";
    private static final String KEY_ADDRESS_ID = "addressId";
    private static final String KEY_ADDRESS_NAME = "addressName";
    private static final String KEY_WORK_ORDER_ID = "workOrderId";
    private static final String KEY_WORK_ORDER_NAME = "workOrderName";

    private final String id;
    private final String imageFileName;
    private final State state;
    private final long createdAtEpochMs;
    private final String addressId;
    private final String addressName;
    private final String workOrderId;
    private final String workOrderName;

    public PendingPhotoRecord(
            String id,
            String imageFileName,
            State state,
            long createdAtEpochMs,
            String addressId,
            String addressName,
            String workOrderId,
            String workOrderName) {
        this.id = requireUuid(id);
        this.imageFileName = requireExpectedImageFileName(id, imageFileName);
        this.state = Objects.requireNonNull(state, "state");
        if (createdAtEpochMs <= 0) {
            throw new IllegalArgumentException("createdAtEpochMs must be positive.");
        }
        this.createdAtEpochMs = createdAtEpochMs;
        this.addressId = requireText(addressId, "addressId");
        this.addressName = requireText(addressName, "addressName");
        this.workOrderId = requireText(workOrderId, "workOrderId");
        this.workOrderName = requireText(workOrderName, "workOrderName");
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

    public PendingPhotoRecord withState(State newState) {
        return new PendingPhotoRecord(
                id,
                imageFileName,
                newState,
                createdAtEpochMs,
                addressId,
                addressName,
                workOrderId,
                workOrderName);
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.setProperty(KEY_ID, id);
        properties.setProperty(KEY_IMAGE_FILE, imageFileName);
        properties.setProperty(KEY_STATE, state.name());
        properties.setProperty(KEY_CREATED_AT, Long.toString(createdAtEpochMs));
        properties.setProperty(KEY_ADDRESS_ID, addressId);
        properties.setProperty(KEY_ADDRESS_NAME, addressName);
        properties.setProperty(KEY_WORK_ORDER_ID, workOrderId);
        properties.setProperty(KEY_WORK_ORDER_NAME, workOrderName);
        return properties;
    }

    public static PendingPhotoRecord fromProperties(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        String id = requiredProperty(properties, KEY_ID);
        String imageFile = requiredProperty(properties, KEY_IMAGE_FILE);
        State state;
        try {
            state = State.valueOf(requiredProperty(properties, KEY_STATE));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid pending-photo state.", error);
        }
        long createdAt;
        try {
            createdAt = Long.parseLong(requiredProperty(properties, KEY_CREATED_AT));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Invalid pending-photo createdAtEpochMs.", error);
        }
        return new PendingPhotoRecord(
                id,
                imageFile,
                state,
                createdAt,
                requiredProperty(properties, KEY_ADDRESS_ID),
                requiredProperty(properties, KEY_ADDRESS_NAME),
                requiredProperty(properties, KEY_WORK_ORDER_ID),
                requiredProperty(properties, KEY_WORK_ORDER_NAME));
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
            throw new IllegalArgumentException("Pending-photo image filename does not match its id.");
        }
        return expected;
    }

    private static String requiredProperty(Properties properties, String key) {
        return requireText(properties.getProperty(key), key);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }
}
