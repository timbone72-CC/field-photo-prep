package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public final class DrivePhotoReconcilerProviderToleranceTest {
    private static final String PHOTO_ID = "22222222-2222-4222-8222-222222222222";
    private static final String WORK_ID = "work-provider-id";
    private static final String REMOTE_ID = "remote-photo-id";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void exactProvisionalMatchUsesHashWhenProviderSizeIsStaleZero() throws Exception {
        File prepared = prepared("matching-photo-content");
        PendingPhotoRecord uncertain = uncertainWithProvisional(REMOTE_ID);
        FakeProvider provider = new FakeProvider();
        provider.exact = candidate(REMOTE_ID, 0L);
        provider.remoteHash = DrivePhotoReconciler.sha256File(prepared);

        DrivePhotoReconciler.Result result = new DrivePhotoReconciler(provider).reconcile(
                uncertain,
                prepared);

        assertEquals(DrivePhotoReconciler.Result.Outcome.CONFIRMED_MATCH, result.outcome());
        assertEquals(REMOTE_ID, result.remoteFileId());
        assertEquals(1, provider.hashCalls);
        assertEquals(0, provider.queryCalls);
    }

    @Test
    public void discoveredMatchUsesHashWhenProviderSizeIsStaleZero() throws Exception {
        File prepared = prepared("matching-photo-content");
        PendingPhotoRecord uncertain = uncertainWithoutProvisional();
        FakeProvider provider = new FakeProvider();
        DrivePhotoReconciler.RemoteDocument candidate = candidate(REMOTE_ID, 0L);
        provider.first = new DrivePhotoReconciler.ChildQueryResult(
                Collections.singletonList(candidate), false);
        provider.second = new DrivePhotoReconciler.ChildQueryResult(
                Collections.singletonList(candidate), false);
        provider.remoteHash = DrivePhotoReconciler.sha256File(prepared);

        DrivePhotoReconciler.Result result = new DrivePhotoReconciler(provider).reconcile(
                uncertain,
                prepared);

        assertEquals(DrivePhotoReconciler.Result.Outcome.CONFIRMED_MATCH, result.outcome());
        assertEquals(REMOTE_ID, result.remoteFileId());
        assertEquals(1, provider.hashCalls);
        assertEquals(2, provider.queryCalls);
    }

    private File prepared(String text) throws Exception {
        File file = temporaryFolder.newFile("prepared-" + System.nanoTime() + ".jpg");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        return file;
    }

    private static PendingPhotoRecord uncertainWithProvisional(String remoteId) {
        return PendingPhotoRecord.createCapturing(
                PHOTO_ID,
                1_700_000_000_000L,
                "address-provider-id",
                "Address",
                WORK_ID,
                "Cut Grass - 2026-09-15")
                .withState(PendingPhotoRecord.State.WAITING)
                .beginUploadAttempt(1_700_000_100_000L)
                .recordProvisionalRemoteFileId(remoteId)
                .markUploadUncertain("ambiguous write result");
    }

    private static PendingPhotoRecord uncertainWithoutProvisional() {
        return PendingPhotoRecord.createCapturing(
                PHOTO_ID,
                1_700_000_000_000L,
                "address-provider-id",
                "Address",
                WORK_ID,
                "Cut Grass - 2026-09-15")
                .withState(PendingPhotoRecord.State.WAITING)
                .beginUploadAttempt(1_700_000_100_000L)
                .markUploadUncertain("ambiguous create result");
    }

    private static DrivePhotoReconciler.RemoteDocument candidate(String id, long size) {
        return new DrivePhotoReconciler.RemoteDocument(
                id,
                DrivePhotoUploader.remoteFileNameFor(PHOTO_ID),
                DrivePhotoUploader.JPEG_MIME_TYPE,
                size);
    }

    private static final class FakeProvider implements DrivePhotoReconciler.ProviderOps {
        DrivePhotoReconciler.RemoteDocument exact;
        DrivePhotoReconciler.ChildQueryResult first;
        DrivePhotoReconciler.ChildQueryResult second;
        byte[] remoteHash;
        int queryCalls;
        int hashCalls;

        @Override
        public DrivePhotoReconciler.RemoteDocument readDocument(String documentId)
                throws IOException {
            if (exact == null) {
                throw new IOException("exact document unavailable");
            }
            return exact;
        }

        @Override
        public boolean requestFreshParent(String parentDocumentId) {
            return true;
        }

        @Override
        public DrivePhotoReconciler.ChildQueryResult queryChildren(String parentDocumentId)
                throws IOException {
            queryCalls++;
            if (queryCalls == 1 && first != null) {
                return first;
            }
            if (queryCalls == 2 && second != null) {
                return second;
            }
            throw new IOException("no child query fixture");
        }

        @Override
        public byte[] sha256Remote(String documentId) throws IOException {
            hashCalls++;
            if (remoteHash == null) {
                throw new IOException("remote content unavailable");
            }
            return remoteHash;
        }
    }
}
