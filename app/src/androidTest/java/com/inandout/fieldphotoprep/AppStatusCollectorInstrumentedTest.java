package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public final class AppStatusCollectorInstrumentedTest {
    @Test
    public void collectorReadsQueueWithoutChangingQueueState() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File fixture = new File(context.getCacheDir(), "app-status-" + UUID.randomUUID());
        PendingPhotoStore store = new PendingPhotoStore(new File(fixture, "pending"));
        File preparedRoot = new File(fixture, "prepared");
        DriveFolder address = new DriveFolder("status-address", "STATUS_TEST_ADDRESS");
        DriveFolder workOrder = new DriveFolder("status-work", "STATUS TEST - 2026-09-26");

        try {
            PendingPhotoRecord capturing = store.beginCapture(address, workOrder);
            writeByte(store.imageFile(capturing));

            PendingPhotoRecord waiting = store.beginCapture(address, workOrder);
            writeByte(store.imageFile(waiting));
            waiting = store.finishCaptureIfImageExists(waiting.id());

            PendingPhotoRecord uploading = store.beginCapture(address, workOrder);
            writeByte(store.imageFile(uploading));
            uploading = store.finishCaptureIfImageExists(uploading.id());
            uploading = store.beginUploadAttempt(uploading.id());

            PendingPhotoRecord failed = store.beginCapture(address, workOrder);
            writeByte(store.imageFile(failed));
            failed = store.finishCaptureIfImageExists(failed.id());
            failed = store.beginUploadAttempt(failed.id());
            failed = store.markUploadFailed(failed.id(), "safe test failure");

            PendingPhotoRecord uncertain = store.beginCapture(address, workOrder);
            writeByte(store.imageFile(uncertain));
            uncertain = store.finishCaptureIfImageExists(uncertain.id());
            uncertain = store.beginUploadAttempt(uncertain.id());
            uncertain = store.markUploadUncertain(uncertain.id(), "safe test uncertainty");

            PendingPhotoRecord uploaded = store.beginCapture(address, workOrder);
            writeByte(store.imageFile(uploaded));
            uploaded = store.finishCaptureIfImageExists(uploaded.id());
            uploaded = store.beginUploadAttempt(uploaded.id());
            uploaded = store.markUploadConfirmed(uploaded.id(), "remote-status-test");

            FieldPhotoPrepApplication app =
                    (FieldPhotoPrepApplication) context.getApplicationContext();
            RuntimeAuthorizationManager manager = app.authorizationManager();
            FolderPrefs prefs = new FolderPrefs(context);
            AuthorizationActionGuard authorizationGuard =
                    new AuthorizationActionGuard(manager);
            OrganizationDriveBindingGuard bindingGuard =
                    new OrganizationDriveBindingGuard(
                            prefs,
                            authorizationGuard,
                            uri -> false);

            AppStatusCollector collector = new AppStatusCollector(
                    context,
                    manager,
                    prefs,
                    bindingGuard,
                    store,
                    preparedRoot,
                    "status-test");

            AppStatusSnapshot.QueueCounts counts = collector.collect().queueCounts();

            assertTrue(counts.readable());
            assertEquals(1, counts.capturing());
            assertEquals(1, counts.waiting());
            assertEquals(1, counts.uploading());
            assertEquals(1, counts.failed());
            assertEquals(1, counts.uncertain());
            assertEquals(1, counts.uploaded());
            assertEquals(6, counts.protectedOriginals());
            assertEquals(1, counts.cleanupPending());
            assertEquals(6, counts.signOutBlocking());

            assertEquals(PendingPhotoRecord.State.CAPTURING, store.getById(capturing.id()).state());
            assertEquals(PendingPhotoRecord.State.WAITING, store.getById(waiting.id()).state());
            assertEquals(PendingPhotoRecord.State.UPLOADING, store.getById(uploading.id()).state());
            assertEquals(PendingPhotoRecord.State.FAILED, store.getById(failed.id()).state());
            assertEquals(PendingPhotoRecord.State.UNCERTAIN, store.getById(uncertain.id()).state());
            assertEquals(PendingPhotoRecord.State.UPLOADED, store.getById(uploaded.id()).state());
        } finally {
            delete(fixture);
        }
    }

    private static void writeByte(File file) throws Exception {
        File parent = file.getParentFile();
        assertTrue(parent.isDirectory() || parent.mkdirs());
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(1);
            output.getFD().sync();
        }
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                delete(child);
            }
        }
        file.delete();
    }
}
