package com.inandout.fieldphotoprep;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public final class PendingPhotoRecordTest {
    private static final String ID = "11111111-1111-4111-8111-111111111111";

    @Test
    public void capturingRecordBindsExactDestinationIdentity() {
        PendingPhotoRecord record = PendingPhotoRecord.createCapturing(
                ID,
                1_700_000_000_000L,
                "address-id",
                "1607 Crestview Drive",
                "work-id",
                "Cut Grass - 2026-09-09");

        assertEquals(ID, record.id());
        assertEquals("photo-" + ID + ".jpg", record.imageFileName());
        assertEquals("photo-" + ID + ".properties", record.metadataFileName());
        assertEquals(PendingPhotoRecord.State.CAPTURING, record.state());
        assertEquals("address-id", record.addressId());
        assertEquals("1607 Crestview Drive", record.addressName());
        assertEquals("work-id", record.workOrderId());
        assertEquals("Cut Grass - 2026-09-09", record.workOrderName());
    }

    @Test
    public void propertiesRoundTripPreservesIdentityAndState() {
        PendingPhotoRecord original = PendingPhotoRecord.createCapturing(
                ID,
                1_700_000_000_000L,
                "address-id",
                "Address With  Double Spaces",
                "work-id",
                "Remove Trash - 2026-09-09").withState(PendingPhotoRecord.State.WAITING);

        PendingPhotoRecord restored = PendingPhotoRecord.fromProperties(original.toProperties());

        assertEquals(original.id(), restored.id());
        assertEquals(original.imageFileName(), restored.imageFileName());
        assertEquals(original.state(), restored.state());
        assertEquals(original.createdAtEpochMs(), restored.createdAtEpochMs());
        assertEquals(original.addressId(), restored.addressId());
        assertEquals(original.addressName(), restored.addressName());
        assertEquals(original.workOrderId(), restored.workOrderId());
        assertEquals(original.workOrderName(), restored.workOrderName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsImageFilenameThatDoesNotMatchId() {
        new PendingPhotoRecord(
                ID,
                "../escape.jpg",
                PendingPhotoRecord.State.WAITING,
                1_700_000_000_000L,
                "address-id",
                "Address",
                "work-id",
                "Work - 2026-09-09");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidUuid() {
        PendingPhotoRecord.createCapturing(
                "not-a-uuid",
                1_700_000_000_000L,
                "address-id",
                "Address",
                "work-id",
                "Work - 2026-09-09");
    }

    @Test(expected = IllegalArgumentException.class)
    public void corruptPropertiesFailClosed() {
        Properties properties = new Properties();
        properties.setProperty("id", ID);
        properties.setProperty("imageFile", "photo-" + ID + ".jpg");
        properties.setProperty("state", "UPLOADED");
        properties.setProperty("createdAtEpochMs", "1700000000000");
        properties.setProperty("addressId", "address-id");
        properties.setProperty("addressName", "Address");
        properties.setProperty("workOrderId", "work-id");
        properties.setProperty("workOrderName", "Work - 2026-09-09");

        PendingPhotoRecord.fromProperties(properties);
    }
}
