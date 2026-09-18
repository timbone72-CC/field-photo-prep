package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

public final class CaptureOrderManifestTest {
    private static final String ADDRESS_ID = "address-provider-id";
    private static final String WORK_ORDER_ID = "work-order-provider-id";

    @Test
    public void buildSortsByCaptureTimeThenPhotoIdAndOmitsProviderIdentity() {
        PendingPhotoRecord later = uploaded(
                "22222222-2222-2222-2222-222222222222", 2000L, "remote-later");
        PendingPhotoRecord firstTieB = uploaded(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", 1000L, "remote-b");
        PendingPhotoRecord firstTieA = uploaded(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", 1000L, "remote-a");

        CaptureOrderManifest.Result result = CaptureOrderManifest.build(
                "820 META ST CORDELL OK",
                "Full Property Condition - 2026-09-15",
                ADDRESS_ID,
                WORK_ORDER_ID,
                Arrays.asList(later, firstTieB, firstTieA));

        assertEquals(3, result.count());
        String text = result.text();
        int a = text.indexOf("001|1000|aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa|");
        int b = text.indexOf("002|1000|bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb|");
        int laterIndex = text.indexOf("003|2000|22222222-2222-2222-2222-222222222222|");
        assertTrue(a > 0);
        assertTrue(b > a);
        assertTrue(laterIndex > b);
        assertTrue(text.contains("field-photo-aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa.jpg"));
        assertFalse(text.contains("remote-a"));
        assertFalse(text.contains("remote-b"));
        assertFalse(text.contains("remote-later"));
    }

    @Test
    public void buildRejectsAnyUnconfirmedRecord() {
        PendingPhotoRecord waiting = PendingPhotoRecord.createCapturing(
                "33333333-3333-3333-3333-333333333333",
                3000L,
                ADDRESS_ID,
                "820 META ST CORDELL OK",
                WORK_ORDER_ID,
                "Full Property Condition - 2026-09-15")
                .withState(PendingPhotoRecord.State.WAITING);
        try {
            CaptureOrderManifest.build(
                    "820 META ST CORDELL OK",
                    "Full Property Condition - 2026-09-15",
                    ADDRESS_ID,
                    WORK_ORDER_ID,
                    Arrays.asList(waiting));
            fail("Expected unconfirmed record rejection.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("confirmed uploaded"));
        }
    }

    @Test
    public void buildRejectsWrongWorkOrderBinding() {
        PendingPhotoRecord wrong = uploadedFor(
                "44444444-4444-4444-4444-444444444444",
                4000L,
                "remote-wrong",
                "other-work-order");
        try {
            CaptureOrderManifest.build(
                    "820 META ST CORDELL OK",
                    "Full Property Condition - 2026-09-15",
                    ADDRESS_ID,
                    WORK_ORDER_ID,
                    Arrays.asList(wrong));
            fail("Expected exact work-order rejection.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("exact open work order"));
        }
    }

    private static PendingPhotoRecord uploaded(String id, long createdAt, String remoteId) {
        return uploadedFor(id, createdAt, remoteId, WORK_ORDER_ID);
    }

    private static PendingPhotoRecord uploadedFor(
            String id,
            long createdAt,
            String remoteId,
            String workOrderId) {
        PendingPhotoRecord waiting = PendingPhotoRecord.createCapturing(
                id,
                createdAt,
                ADDRESS_ID,
                "820 META ST CORDELL OK",
                workOrderId,
                "Full Property Condition - 2026-09-15")
                .withState(PendingPhotoRecord.State.WAITING);
        PendingPhotoRecord uploading = waiting.beginUploadAttempt(createdAt + 1L);
        return uploading.markUploadConfirmed(remoteId);
    }
}
