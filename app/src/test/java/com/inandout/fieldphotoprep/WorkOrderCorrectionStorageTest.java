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

    /**
     * Connected sequence regression for the reported workflow: edit the dated folder identity,
     * then Clear & Reuse that same dated occurrence, preserving all other queued work.
     * Provider deletion/rename is separately verified on a real Android Drive provider.
     */
    @Test public void editThenSameDateClearRetiresOnlyThatOccurrenceAcrossRestart() throws Exception {
        File root = temp.newFolder();
        PendingPhotoStore store = new PendingPhotoStore(root);
        PhotoPreparer preparer = new PhotoPreparer(temp.newFolder());
        DriveFolder address = new DriveFolder("address", "Address");
        DriveFolder oldName = new DriveFolder("wo", "GRASS CUT - 2026-10-06");
        DriveFolder corrected = new DriveFolder("wo", "GRASS CUT - 2026-09-29");
        DriveFolder other = new DriveFolder("other-wo", "INITIAL SECURE - 2026-10-06");

        PendingPhotoRecord oldPhoto = store.beginCapture(address, oldName);
        Files.write(store.imageFile(oldPhoto).toPath(), new byte[]{1, 2, 3});
        store.finishCaptureIfImageExists(oldPhoto.id());
        store.beginUploadAttempt(oldPhoto.id());
        store.markUploadConfirmed(oldPhoto.id(), "confirmed-old-remote");

        PendingPhotoRecord otherPending = store.beginCapture(address, other);
        byte[] otherBytes = new byte[]{4, 5, 6};
        Files.write(store.imageFile(otherPending).toPath(), otherBytes);
        store.finishCaptureIfImageExists(otherPending.id());

        WorkOrderEditPolicy.validate(
                java.util.Arrays.asList(oldName, other),
                oldName.id(), oldName.name(), corrected.name());
        store.requireWorkOrderCorrectionSafe(oldName.id());
        PendingPhotoStore afterEdit = new PendingPhotoStore(root);
        assertEquals("wo", afterEdit.getById(oldPhoto.id()).workOrderId());
        assertEquals(1, afterEdit.getById(oldPhoto.id()).captureSequence());
        assertEquals("confirmed-old-remote", afterEdit.getById(oldPhoto.id()).remoteFileId());
        assertArrayEquals(otherBytes, Files.readAllBytes(afterEdit.imageFile(otherPending).toPath()));

        String sameDateName = WorkOrderFolderName.reuseNameForDate(
                corrected.name(), "2026-09-29");
        assertEquals(corrected.name(), sameDateName);
        afterEdit.prepareCaptureSequenceResetForReuse(corrected.id(), sameDateName, true);
        PendingPhotoStore interrupted = new PendingPhotoStore(root);
        assertThrows(IOException.class, () -> interrupted.beginCapture(address, corrected));
        assertThrows(IOException.class, () -> interrupted.requireWorkOrderCorrectionSafe(corrected.id()));
        assertNotNull(interrupted.getById(oldPhoto.id()));
        assertNotNull(interrupted.getById(otherPending.id()));

        interrupted.completeCaptureSequenceResetForReuse(corrected.id(), sameDateName, preparer);
        PendingPhotoStore finished = new PendingPhotoStore(root);
        assertNull(finished.getById(oldPhoto.id()));
        assertTrue(finished.recordsForWorkOrder(corrected.id()).isEmpty());
        assertFalse(finished.imageFile(oldPhoto).exists());
        assertEquals(PendingPhotoRecord.State.WAITING, finished.getById(otherPending.id()).state());
        assertArrayEquals(otherBytes, Files.readAllBytes(finished.imageFile(otherPending).toPath()));
        assertEquals(1, finished.beginCapture(address, corrected).captureSequence());
        assertEquals(2, finished.beginCapture(address, other).captureSequence());
    }

}
