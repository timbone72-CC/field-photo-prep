package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public final class WorkOrderCorrectionStorageTest {
    @Rule public final TemporaryFolder temp = new TemporaryFolder();

    @Test public void nameCorrectionKeepsWaitingOriginalDestinationAndSequenceAcrossRestart() throws Exception {
        File root = temp.newFolder();
        PendingPhotoStore store = new PendingPhotoStore(root);
        DriveFolder address = new DriveFolder("address", "Address");
        DriveFolder original = new DriveFolder("wo", "GRASS CUT - 2026-10-06");
        PendingPhotoRecord photo = store.beginCapture(address, original);
        byte[] bytes = {1, 2, 3};
        Files.write(store.imageFile(photo).toPath(), bytes);
        store.finishCaptureIfImageExists(photo.id());
        store.requireWorkOrderCorrectionSafe("wo");
        PendingPhotoStore reopened = new PendingPhotoStore(root);
        assertEquals("wo", reopened.getById(photo.id()).workOrderId());
        assertEquals(original.name(), reopened.getById(photo.id()).workOrderName());
        assertEquals(PendingPhotoRecord.State.WAITING, reopened.getById(photo.id()).state());
        assertArrayEquals(bytes, Files.readAllBytes(reopened.imageFile(photo).toPath()));
        assertEquals(2, reopened.beginCapture(address,
                new DriveFolder("wo", "Corrected - 2026-09-29")).captureSequence());
    }

    @Test public void unfinishedReuseBlocksCorrectionWithoutChangingReset() throws Exception {
        PendingPhotoStore store = new PendingPhotoStore(temp.newFolder());
        store.prepareCaptureSequenceResetForReuse("wo", "GRASS CUT - 2026-10-07");
        assertThrows(IOException.class, () -> store.requireWorkOrderCorrectionSafe("wo"));
        store.requireWorkOrderCorrectionSafe("other");
        store.completeCaptureSequenceResetForReuse("wo", "GRASS CUT - 2026-10-07");
        store.requireWorkOrderCorrectionSafe("wo");
    }
}
