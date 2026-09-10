package com.inandout.fieldphotoprep;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public final class PendingPhotoSchema3MigrationTest {
    private static final String ID = "33333333-3333-4333-8333-333333333333";
    private static final long CAPTURE_TIME = 1_700_000_000_000L;
    private static final long ATTEMPT_TIME = 1_700_000_100_000L;

    @Test
    public void schema2UncertainPreservesStateAndDetailWithNullProvisionalIdentity() {
        PendingPhotoRecord restored = PendingPhotoRecord.fromProperties(schema2Properties(
                PendingPhotoRecord.State.UNCERTAIN,
                "Provider result unknown",
                null));

        assertEquals(PendingPhotoRecord.State.UNCERTAIN, restored.state());
        assertEquals(1, restored.uploadAttemptCount());
        assertEquals(ATTEMPT_TIME, restored.lastAttemptAtEpochMs());
        assertEquals("Provider result unknown", restored.statusDetail());
        assertNull(restored.provisionalRemoteFileId());
        assertNull(restored.remoteFileId());
        assertEquals("address-legacy", restored.addressId());
        assertEquals("work-legacy", restored.workOrderId());
        assertFalse(restored.canBeginUploadAttempt());
    }

    @Test
    public void schema2UploadedPreservesConfirmedIdentityWithNullProvisionalIdentity() {
        PendingPhotoRecord restored = PendingPhotoRecord.fromProperties(schema2Properties(
                PendingPhotoRecord.State.UPLOADED,
                null,
                "confirmed-provider-id"));

        assertEquals(PendingPhotoRecord.State.UPLOADED, restored.state());
        assertEquals(1, restored.uploadAttemptCount());
        assertEquals(ATTEMPT_TIME, restored.lastAttemptAtEpochMs());
        assertNull(restored.statusDetail());
        assertNull(restored.provisionalRemoteFileId());
        assertEquals("confirmed-provider-id", restored.remoteFileId());
        assertEquals("address-legacy", restored.addressId());
        assertEquals("work-legacy", restored.workOrderId());
        assertFalse(restored.canBeginUploadAttempt());
    }

    private static Properties schema2Properties(
            PendingPhotoRecord.State state,
            String statusDetail,
            String remoteFileId) {
        Properties properties = new Properties();
        properties.setProperty("schemaVersion", "2");
        properties.setProperty("id", ID);
        properties.setProperty("imageFile", PendingPhotoRecord.imageFileNameFor(ID));
        properties.setProperty("state", state.name());
        properties.setProperty("createdAtEpochMs", Long.toString(CAPTURE_TIME));
        properties.setProperty("addressId", "address-legacy");
        properties.setProperty("addressName", "Legacy Address");
        properties.setProperty("workOrderId", "work-legacy");
        properties.setProperty("workOrderName", "Legacy Work");
        properties.setProperty("uploadAttemptCount", "1");
        properties.setProperty("lastAttemptAtEpochMs", Long.toString(ATTEMPT_TIME));
        properties.setProperty("statusDetail", statusDetail == null ? "" : statusDetail);
        properties.setProperty("remoteFileId", remoteFileId == null ? "" : remoteFileId);
        return properties;
    }
}
