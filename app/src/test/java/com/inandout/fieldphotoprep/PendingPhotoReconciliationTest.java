package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class PendingPhotoReconciliationTest {
    private static final String PHOTO_ID = "11111111-1111-4111-8111-111111111111";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void uncertainWithoutProvisionalCanBecomeRetryableOnlyAfterExplicitAbsenceResolution()
            throws Exception {
        Fixture fixture = fixture();
        PendingPhotoRecord uncertain = fixture.store.markUploadUncertain(
                PHOTO_ID,
                "create result uncertain");

        assertEquals(PendingPhotoRecord.State.UNCERTAIN, uncertain.state());
        assertFalse(uncertain.canBeginUploadAttempt());
        assertNull(uncertain.provisionalRemoteFileId());

        PendingPhotoRecord released = fixture.store.resolveUncertainAsRetryableAbsence(
                PHOTO_ID,
                "settled provider state proved expected remote photo absent");

        assertEquals(PendingPhotoRecord.State.FAILED, released.state());
        assertTrue(released.canBeginUploadAttempt());
        assertEquals(1, released.uploadAttemptCount());
        assertEquals(1_700_000_100_000L, released.lastAttemptAtEpochMs());
        assertEquals("address-provider-id", released.addressId());
        assertEquals("work-provider-id", released.workOrderId());
        assertNull(released.provisionalRemoteFileId());
        assertNull(released.remoteFileId());
        assertTrue(fixture.store.hasImageData(released));
    }

    @Test
    public void unresolvedProvisionalIdentityBlocksRetryRelease() throws Exception {
        Fixture fixture = fixture();
        fixture.store.recordProvisionalRemoteFileId(PHOTO_ID, "remote-provisional-id");
        fixture.store.markUploadUncertain(PHOTO_ID, "write result uncertain");

        try {
            fixture.store.resolveUncertainAsRetryableAbsence(
                    PHOTO_ID,
                    "parent looked empty");
            fail("Expected unresolved provisional identity to block retry release");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Could not release"));
        }

        PendingPhotoRecord stillUncertain = fixture.store.getById(PHOTO_ID);
        assertEquals(PendingPhotoRecord.State.UNCERTAIN, stillUncertain.state());
        assertEquals("remote-provisional-id", stillUncertain.provisionalRemoteFileId());
        assertFalse(stillUncertain.canBeginUploadAttempt());
        assertTrue(fixture.store.hasImageData(stillUncertain));
    }

    @Test
    public void nonUncertainStateCannotUseReconciliationRetryRelease() throws Exception {
        Fixture fixture = fixture();

        try {
            fixture.store.resolveUncertainAsRetryableAbsence(
                    PHOTO_ID,
                    "not allowed");
            fail("Expected UPLOADING state to reject reconciliation retry release");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Could not release"));
        }

        PendingPhotoRecord current = fixture.store.getById(PHOTO_ID);
        assertEquals(PendingPhotoRecord.State.UPLOADING, current.state());
        assertFalse(current.canBeginUploadAttempt());
    }

    private Fixture fixture() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        PendingPhotoStore store = new PendingPhotoStore(
                root,
                () -> PHOTO_ID,
                new SequenceTimes(1_700_000_000_000L, 1_700_000_100_000L));
        PendingPhotoRecord capturing = store.beginCapture(
                new DriveFolder("address-provider-id", "Address"),
                new DriveFolder("work-provider-id", "Cut Grass - 2026-09-21"));
        write(store.imageFile(capturing), "protected-original");
        store.finishCaptureIfImageExists(PHOTO_ID);
        store.beginUploadAttempt(PHOTO_ID);
        return new Fixture(store);
    }

    private static void write(File file, String text) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
    }

    private static final class Fixture {
        final PendingPhotoStore store;

        Fixture(PendingPhotoStore store) {
            this.store = store;
        }
    }

    private static final class SequenceTimes implements PendingPhotoStore.TimeSource {
        private final long[] times;
        private int index;

        SequenceTimes(long... times) {
            this.times = times;
        }

        @Override
        public long nowEpochMs() {
            return times[index++];
        }
    }
}
