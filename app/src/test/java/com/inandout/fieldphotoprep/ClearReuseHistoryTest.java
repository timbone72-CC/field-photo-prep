package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public final class ClearReuseHistoryTest {
    @Rule public final TemporaryFolder temp = new TemporaryFolder();
    private final DriveFolder address = new DriveFolder("address", "Address");
    private final DriveFolder work = new DriveFolder("wo", "GRASS CUT - 2026-10-06");

    private PendingPhotoRecord uploaded(PendingPhotoStore store, DriveFolder folder) throws Exception {
        PendingPhotoRecord photo = store.beginCapture(address, folder);
        Files.write(store.imageFile(photo).toPath(), new byte[]{1, 2, 3});
        store.finishCaptureIfImageExists(photo.id());
        store.beginUploadAttempt(photo.id());
        return store.markUploadConfirmed(photo.id(), "remote-" + photo.id());
    }

    @Test public void sameDateClearRetiresOnlySelectedHistoryAndRestartsAt001AfterReload() throws Exception {
        File root = temp.newFolder();
        PendingPhotoStore store = new PendingPhotoStore(root);
        PhotoPreparer preparer = new PhotoPreparer(temp.newFolder());
        PendingPhotoRecord old = uploaded(store, work);
        DriveFolder other = new DriveFolder("other", "Other - 2026-10-06");
        PendingPhotoRecord outside = uploaded(store, other);
        store.prepareCaptureSequenceResetForReuse(work.id(), work.name(), true);
        store.completeCaptureSequenceResetForReuse(work.id(), work.name(), preparer);
        PendingPhotoStore reopened = new PendingPhotoStore(root);
        assertNull(reopened.getById(old.id()));
        assertFalse(store.imageFile(old).exists());
        assertTrue(reopened.recordsForWorkOrder(work.id()).isEmpty());
        assertNotNull(reopened.getById(outside.id()));
        assertEquals(1, reopened.beginCapture(address, work).captureSequence());
        assertEquals(2, reopened.beginCapture(address, other).captureSequence());
    }

    @Test public void restartCannotTreatUnfinishedSameDateClearAsCompleted() throws Exception {
        File root = temp.newFolder();
        PendingPhotoStore store = new PendingPhotoStore(root);
        PendingPhotoRecord old = uploaded(store, work);
        store.prepareCaptureSequenceResetForReuse(work.id(), work.name(), true);
        PendingPhotoStore reopened = new PendingPhotoStore(root);
        assertThrows(IOException.class, () -> reopened.beginCapture(address, work));
        assertNotNull(reopened.getById(old.id()));
        reopened.completeCaptureSequenceResetForReuse(work.id(), work.name(), new PhotoPreparer(temp.newFolder()));
        assertEquals(1, reopened.beginCapture(address, work).captureSequence());
    }

    @Test public void unresolvedRecordAppearingAfterPreparationBlocksAllHistoryRetirement() throws Exception {
        File root = temp.newFolder();
        PendingPhotoStore store = new PendingPhotoStore(root);
        PhotoPreparer preparer = new PhotoPreparer(temp.newFolder());
        PendingPhotoRecord old = uploaded(store, work);
        store.prepareCaptureSequenceResetForReuse(work.id(), work.name(), true);
        File otherRoot = temp.newFolder();
        PendingPhotoStore concurrent = new PendingPhotoStore(otherRoot);
        PendingPhotoRecord pending = concurrent.beginCapture(address, work);
        Files.copy(new File(otherRoot, pending.metadataFileName()).toPath(),
                new File(root, pending.metadataFileName()).toPath());
        assertThrows(IOException.class, () -> store.completeCaptureSequenceResetForReuse(work.id(), work.name(), preparer));
        assertNotNull(store.getById(old.id()));
        assertNotNull(store.getById(pending.id()));
        assertThrows(IOException.class, () -> store.beginCapture(address, work));
    }
}
