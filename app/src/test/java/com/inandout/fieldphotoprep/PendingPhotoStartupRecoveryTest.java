package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class PendingPhotoStartupRecoveryTest {
    private static final String ID = "33333333-3333-4333-8333-333333333333";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void processStartupRecoveryMakesInterruptedUploadUncertain() throws Exception {
        File root = temporaryFolder.newFolder("startup-recovery");
        PendingPhotoStore firstProcess = uploadingStore(root);

        PendingPhotoStore restarted = new PendingPhotoStore(root);
        PendingPhotoStore.ScanResult settled = QueueStartupRecovery.reconcile(restarted);
        PendingPhotoRecord recovered = restarted.getById(ID);

        assertEquals(PendingPhotoRecord.State.UNCERTAIN, recovered.state());
        assertEquals("address-provider-id", recovered.addressId());
        assertEquals("work-provider-id", recovered.workOrderId());
        assertEquals(1, recovered.uploadAttemptCount());
        assertEquals(1, settled.uncertainPhotoIds().size());
        assertTrue(restarted.hasImageData(recovered));
    }

    @Test
    public void photoScreenCaptureReconciliationDoesNotMisclassifyLiveUploadingState() throws Exception {
        File root = temporaryFolder.newFolder("screen-recreation");
        PendingPhotoStore store = uploadingStore(root);

        PendingPhotoStore.ScanResult settled = store.reconcileInterruptedCaptures();
        PendingPhotoRecord stillUploading = store.getById(ID);

        assertEquals(PendingPhotoRecord.State.UPLOADING, stillUploading.state());
        assertEquals(0, settled.uncertainPhotoIds().size());
        assertEquals("work-provider-id", stillUploading.workOrderId());
        assertTrue(store.hasImageData(stillUploading));
    }

    private static PendingPhotoStore uploadingStore(File root) throws Exception {
        PendingPhotoStore store = new PendingPhotoStore(
                root,
                () -> ID,
                () -> 1_700_000_100_000L);
        PendingPhotoRecord capturing = store.beginCapture(
                new DriveFolder("address-provider-id", "Address"),
                new DriveFolder("work-provider-id", "Cut Grass - 2026-09-09"));
        try (FileOutputStream output = new FileOutputStream(store.imageFile(capturing))) {
            output.write("protected-image".getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        store.finishCaptureIfImageExists(ID);
        store.beginUploadAttempt(ID);
        return store;
    }
}
