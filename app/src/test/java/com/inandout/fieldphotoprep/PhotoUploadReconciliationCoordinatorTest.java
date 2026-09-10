package com.inandout.fieldphotoprep;

import android.provider.DocumentsContract;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class PhotoUploadReconciliationCoordinatorTest {
    private static final String PHOTO_ID = "11111111-1111-4111-8111-111111111111";
    private static final String WORK_ID = "work-provider-id";
    private static final String REMOTE_ID = "remote-photo-id";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void matchingRemoteCandidatePromotesUncertainWithoutCallingUploaderCreateOrWrite()
            throws Exception {
        Fixture fixture = fixture(false);
        File prepared = fixture.preparer.preparedFile(PHOTO_ID);
        ReconcileProvider reconcileProvider = new ReconcileProvider();
        DrivePhotoReconciler.RemoteDocument candidate = new DrivePhotoReconciler.RemoteDocument(
                REMOTE_ID,
                DrivePhotoUploader.remoteFileNameFor(PHOTO_ID),
                DrivePhotoUploader.JPEG_MIME_TYPE,
                prepared.length());
        reconcileProvider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.singletonList(candidate), false));
        reconcileProvider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.singletonList(candidate), false));
        reconcileProvider.remoteHash = DrivePhotoReconciler.sha256File(prepared);

        PhotoUploadCoordinator coordinator = new PhotoUploadCoordinator(
                fixture.store,
                fixture.preparer,
                new DrivePhotoUploader(fixture.uploadProvider),
                new DrivePhotoReconciler(reconcileProvider));

        PhotoUploadCoordinator.ReconciliationResult result = coordinator.reconcileUncertain(PHOTO_ID);

        assertEquals(DrivePhotoReconciler.Result.Outcome.CONFIRMED_MATCH, result.outcome());
        assertEquals(PendingPhotoRecord.State.UPLOADED, result.record().state());
        assertEquals(REMOTE_ID, result.record().remoteFileId());
        assertEquals(WORK_ID, result.record().workOrderId());
        assertEquals(1, result.record().uploadAttemptCount());
        assertFalse(fixture.uploadProvider.createCalled);
        assertFalse(fixture.uploadProvider.writeCalled);
    }

    @Test
    public void settledAbsenceReleasesRetryWithoutChangingDestinationOrAttemptHistory()
            throws Exception {
        Fixture fixture = fixture(false);
        ReconcileProvider reconcileProvider = new ReconcileProvider();
        reconcileProvider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.emptyList(), false));
        reconcileProvider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.emptyList(), false));

        PhotoUploadCoordinator coordinator = new PhotoUploadCoordinator(
                fixture.store,
                fixture.preparer,
                new DrivePhotoUploader(fixture.uploadProvider),
                new DrivePhotoReconciler(reconcileProvider));

        PhotoUploadCoordinator.ReconciliationResult result = coordinator.reconcileUncertain(PHOTO_ID);

        assertEquals(
                DrivePhotoReconciler.Result.Outcome.CONFIRMED_ABSENT_RETRY_SAFE,
                result.outcome());
        assertEquals(PendingPhotoRecord.State.FAILED, result.record().state());
        assertTrue(result.record().canBeginUploadAttempt());
        assertEquals(WORK_ID, result.record().workOrderId());
        assertEquals(1, result.record().uploadAttemptCount());
        assertNull(result.record().provisionalRemoteFileId());
        assertFalse(fixture.uploadProvider.createCalled);
        assertFalse(fixture.uploadProvider.writeCalled);
    }

    @Test
    public void unresolvedProvisionalAndEmptyParentRemainUncertain() throws Exception {
        Fixture fixture = fixture(true);
        ReconcileProvider reconcileProvider = new ReconcileProvider();
        reconcileProvider.failExactRead = true;
        reconcileProvider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.emptyList(), false));
        reconcileProvider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.emptyList(), false));

        PhotoUploadCoordinator coordinator = new PhotoUploadCoordinator(
                fixture.store,
                fixture.preparer,
                new DrivePhotoUploader(fixture.uploadProvider),
                new DrivePhotoReconciler(reconcileProvider));

        PhotoUploadCoordinator.ReconciliationResult result = coordinator.reconcileUncertain(PHOTO_ID);

        assertEquals(DrivePhotoReconciler.Result.Outcome.REMAIN_UNCERTAIN, result.outcome());
        assertEquals(PendingPhotoRecord.State.UNCERTAIN, result.record().state());
        assertEquals(REMOTE_ID, result.record().provisionalRemoteFileId());
        assertFalse(result.record().canBeginUploadAttempt());
        assertFalse(fixture.uploadProvider.createCalled);
        assertFalse(fixture.uploadProvider.writeCalled);
    }

    @Test
    public void reconciliationOfOnePhotoDoesNotMutateAnotherQueuedPhoto() throws Exception {
        Fixture fixture = fixture(false);
        String otherId = "22222222-2222-4222-8222-222222222222";
        PendingPhotoStore otherStore = fixture.store;
        PendingPhotoRecord otherCapturing = otherStore.beginCapture(
                new DriveFolder("other-address", "Other Address"),
                new DriveFolder("other-work", "Other Work - 2026-09-22"));
        assertEquals(otherId, otherCapturing.id());
        write(otherStore.imageFile(otherCapturing), "other-original");
        otherStore.finishCaptureIfImageExists(otherId);

        ReconcileProvider reconcileProvider = new ReconcileProvider();
        reconcileProvider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.emptyList(), false));
        reconcileProvider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.emptyList(), false));
        PhotoUploadCoordinator coordinator = new PhotoUploadCoordinator(
                fixture.store,
                fixture.preparer,
                new DrivePhotoUploader(fixture.uploadProvider),
                new DrivePhotoReconciler(reconcileProvider));

        coordinator.reconcileUncertain(PHOTO_ID);

        PendingPhotoRecord other = otherStore.getById(otherId);
        assertEquals(PendingPhotoRecord.State.WAITING, other.state());
        assertEquals("other-work", other.workOrderId());
        assertEquals(0, other.uploadAttemptCount());
        assertTrue(otherStore.hasImageData(other));
    }

    private Fixture fixture(boolean withProvisional) throws Exception {
        File pendingRoot = temporaryFolder.newFolder("pending-" + System.nanoTime());
        File preparedRoot = temporaryFolder.newFolder("prepared-" + System.nanoTime());
        PendingPhotoStore store = new PendingPhotoStore(
                pendingRoot,
                new SequenceIds(
                        PHOTO_ID,
                        "22222222-2222-4222-8222-222222222222"),
                new SequenceTimes(
                        1_700_000_000_000L,
                        1_700_000_100_000L,
                        1_700_000_200_000L));
        PhotoPreparer preparer = new PhotoPreparer(preparedRoot);
        PendingPhotoRecord capturing = store.beginCapture(
                new DriveFolder("address-provider-id", "Address"),
                new DriveFolder(WORK_ID, "Cut Grass - 2026-09-21"));
        write(store.imageFile(capturing), "protected-original");
        store.finishCaptureIfImageExists(PHOTO_ID);
        write(preparer.preparedFile(PHOTO_ID), "prepared-photo");
        store.beginUploadAttempt(PHOTO_ID);
        if (withProvisional) {
            store.recordProvisionalRemoteFileId(PHOTO_ID, REMOTE_ID);
        }
        store.markUploadUncertain(PHOTO_ID, "upload result ambiguous");
        return new Fixture(store, preparer, new NeverUploadProvider());
    }

    private static void write(File file, String text) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
    }

    private static final class Fixture {
        final PendingPhotoStore store;
        final PhotoPreparer preparer;
        final NeverUploadProvider uploadProvider;

        Fixture(PendingPhotoStore store, PhotoPreparer preparer, NeverUploadProvider uploadProvider) {
            this.store = store;
            this.preparer = preparer;
            this.uploadProvider = uploadProvider;
        }
    }

    private static final class NeverUploadProvider implements DrivePhotoUploader.ProviderOps {
        boolean createCalled;
        boolean writeCalled;

        @Override
        public DrivePhotoUploader.RemoteDocument readDocument(String documentId) throws IOException {
            throw new IOException("normal uploader must not be used during reconciliation");
        }

        @Override
        public DrivePhotoUploader.RemoteDocument createJpeg(String parentDocumentId, String displayName)
                throws IOException {
            createCalled = true;
            throw new IOException("normal create must not be used during reconciliation");
        }

        @Override
        public long writeDocument(String documentId, File source) throws IOException {
            writeCalled = true;
            throw new IOException("normal write must not be used during reconciliation");
        }
    }

    private static final class ReconcileProvider implements DrivePhotoReconciler.ProviderOps {
        boolean failExactRead;
        final List<DrivePhotoReconciler.ChildQueryResult> queries = new ArrayList<>();
        byte[] remoteHash;
        int queryCalls;

        @Override
        public DrivePhotoReconciler.RemoteDocument readDocument(String documentId)
                throws IOException {
            if (failExactRead) {
                throw new IOException("exact remote unavailable");
            }
            throw new IOException("no exact remote configured");
        }

        @Override
        public boolean requestFreshParent(String parentDocumentId) {
            return true;
        }

        @Override
        public DrivePhotoReconciler.ChildQueryResult queryChildren(String parentDocumentId)
                throws IOException {
            if (queries.isEmpty()) {
                throw new IOException("no reconciliation child fixture");
            }
            int index = Math.min(queryCalls++, queries.size() - 1);
            return queries.get(index);
        }

        @Override
        public byte[] sha256Remote(String documentId) throws IOException {
            if (remoteHash == null) {
                throw new IOException("remote hash unavailable");
            }
            return remoteHash;
        }
    }

    private static final class SequenceIds implements PendingPhotoStore.IdSource {
        private final String[] ids;
        private int index;

        SequenceIds(String... ids) {
            this.ids = ids;
        }

        @Override
        public String nextId() {
            return ids[index++];
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
