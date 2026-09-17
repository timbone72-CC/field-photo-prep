package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DrivePhotoReconcilerTest {
    private static final String PHOTO_ID = "11111111-1111-4111-8111-111111111111";
    private static final String WORK_ID = "work-provider-id";
    private static final String REMOTE_ID = "remote-photo-id";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void exactProvisionalIdentityWithMatchingContentConfirmsWithoutParentDiscovery()
            throws Exception {
        File prepared = prepared("matching-content");
        PendingPhotoRecord uncertain = uncertainWithProvisional(REMOTE_ID);
        FakeProvider provider = new FakeProvider();
        provider.exact = candidate(REMOTE_ID, prepared.length());
        provider.remoteHash = DrivePhotoReconciler.sha256File(prepared);

        DrivePhotoReconciler.Result result = new DrivePhotoReconciler(provider).reconcile(
                uncertain,
                prepared);

        assertEquals(DrivePhotoReconciler.Result.Outcome.CONFIRMED_MATCH, result.outcome());
        assertEquals(REMOTE_ID, result.remoteFileId());
        assertEquals(1, provider.readCalls);
        assertEquals(1, provider.hashCalls);
        assertEquals(0, provider.refreshCalls);
        assertEquals(0, provider.queryCalls);
    }

    @Test
    public void exactProvisionalMismatchRemainsUncertainWithoutSearchingForReplacement()
            throws Exception {
        File prepared = prepared("matching-content");
        PendingPhotoRecord uncertain = uncertainWithProvisional(REMOTE_ID);
        FakeProvider provider = new FakeProvider();
        provider.exact = new DrivePhotoReconciler.RemoteDocument(
                REMOTE_ID,
                "different-name.jpg",
                DrivePhotoUploader.JPEG_MIME_TYPE,
                prepared.length());

        DrivePhotoReconciler.Result result = new DrivePhotoReconciler(provider).reconcile(
                uncertain,
                prepared);

        assertEquals(DrivePhotoReconciler.Result.Outcome.REMAIN_UNCERTAIN, result.outcome());
        assertEquals(1, provider.readCalls);
        assertEquals(0, provider.refreshCalls);
        assertEquals(0, provider.queryCalls);
        assertEquals(0, provider.hashCalls);
    }

    @Test
    public void noProvisionalAndTwoSettledEmptySnapshotsReleaseRetry() throws Exception {
        File prepared = prepared("local-content");
        PendingPhotoRecord uncertain = uncertainWithoutProvisional();
        FakeProvider provider = new FakeProvider();
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.emptyList(), false));
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.emptyList(), false));

        DrivePhotoReconciler.Result result = new DrivePhotoReconciler(provider).reconcile(
                uncertain,
                prepared);

        assertEquals(
                DrivePhotoReconciler.Result.Outcome.CONFIRMED_ABSENT_RETRY_SAFE,
                result.outcome());
        assertEquals(1, provider.refreshCalls);
        assertEquals(2, provider.queryCalls);
        assertEquals(WORK_ID, provider.lastParentId);
        assertEquals(0, provider.hashCalls);
    }

    @Test
    public void unresolvedProvisionalPlusSettledEmptyParentDoesNotReleaseRetry() throws Exception {
        File prepared = prepared("local-content");
        PendingPhotoRecord uncertain = uncertainWithProvisional(REMOTE_ID);
        FakeProvider provider = new FakeProvider();
        provider.failExactRead = true;
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.emptyList(), false));
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.emptyList(), false));

        DrivePhotoReconciler.Result result = new DrivePhotoReconciler(provider).reconcile(
                uncertain,
                prepared);

        assertEquals(DrivePhotoReconciler.Result.Outcome.REMAIN_UNCERTAIN, result.outcome());
        assertEquals(1, provider.readCalls);
        assertEquals(1, provider.refreshCalls);
        assertEquals(2, provider.queryCalls);
        assertEquals(0, provider.hashCalls);
    }

    @Test
    public void oneDeterministicNameCandidateWithMatchingHashConfirms() throws Exception {
        File prepared = prepared("same-photo-bytes");
        PendingPhotoRecord uncertain = uncertainWithoutProvisional();
        FakeProvider provider = new FakeProvider();
        DrivePhotoReconciler.RemoteDocument candidate = candidate(REMOTE_ID, prepared.length());
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.singletonList(candidate), false));
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.singletonList(candidate), false));
        provider.remoteHash = DrivePhotoReconciler.sha256File(prepared);

        DrivePhotoReconciler.Result result = new DrivePhotoReconciler(provider).reconcile(
                uncertain,
                prepared);

        assertEquals(DrivePhotoReconciler.Result.Outcome.CONFIRMED_MATCH, result.outcome());
        assertEquals(REMOTE_ID, result.remoteFileId());
        assertEquals(1, provider.hashCalls);
    }

    @Test
    public void multipleDeterministicNameCandidatesRemainUncertain() throws Exception {
        File prepared = prepared("same-photo-bytes");
        PendingPhotoRecord uncertain = uncertainWithoutProvisional();
        FakeProvider provider = new FakeProvider();
        List<DrivePhotoReconciler.RemoteDocument> duplicates = Arrays.asList(
                candidate("remote-one", prepared.length()),
                candidate("remote-two", prepared.length()));
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(duplicates, false));
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(duplicates, false));

        DrivePhotoReconciler.Result result = new DrivePhotoReconciler(provider).reconcile(
                uncertain,
                prepared);

        assertEquals(DrivePhotoReconciler.Result.Outcome.REMAIN_UNCERTAIN, result.outcome());
        assertEquals(0, provider.hashCalls);
    }

    @Test
    public void hashMismatchRemainsUncertain() throws Exception {
        File prepared = prepared("local-photo");
        PendingPhotoRecord uncertain = uncertainWithoutProvisional();
        FakeProvider provider = new FakeProvider();
        DrivePhotoReconciler.RemoteDocument candidate = candidate(REMOTE_ID, prepared.length());
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.singletonList(candidate), false));
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.singletonList(candidate), false));
        provider.remoteHash = sha256("different-remote-photo".getBytes(StandardCharsets.UTF_8));

        DrivePhotoReconciler.Result result = new DrivePhotoReconciler(provider).reconcile(
                uncertain,
                prepared);

        assertEquals(DrivePhotoReconciler.Result.Outcome.REMAIN_UNCERTAIN, result.outcome());
        assertEquals(1, provider.hashCalls);
    }

    @Test
    public void loadingOrRefreshRejectionFailsClosed() throws Exception {
        File prepared = prepared("local-photo");
        PendingPhotoRecord uncertain = uncertainWithoutProvisional();

        FakeProvider rejected = new FakeProvider();
        rejected.refreshAccepted = false;
        DrivePhotoReconciler.Result rejectedResult = new DrivePhotoReconciler(rejected).reconcile(
                uncertain,
                prepared);
        assertEquals(
                DrivePhotoReconciler.Result.Outcome.REMAIN_UNCERTAIN,
                rejectedResult.outcome());
        assertEquals(0, rejected.queryCalls);

        FakeProvider loading = new FakeProvider();
        for (int i = 0; i < 6; i++) {
            loading.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                    Collections.emptyList(), true));
        }
        DrivePhotoReconciler.Result loadingResult = new DrivePhotoReconciler(loading).reconcile(
                uncertain,
                prepared);
        assertEquals(
                DrivePhotoReconciler.Result.Outcome.REMAIN_UNCERTAIN,
                loadingResult.outcome());
        assertEquals(6, loading.queryCalls);
    }

    @Test
    public void inconsistentSettledSnapshotsDoNotProveAbsence() throws Exception {
        File prepared = prepared("local-photo");
        PendingPhotoRecord uncertain = uncertainWithoutProvisional();
        FakeProvider provider = new FakeProvider();
        for (int i = 0; i < 6; i++) {
            List<DrivePhotoReconciler.RemoteDocument> docs = (i % 2 == 0)
                    ? Collections.emptyList()
                    : Collections.singletonList(new DrivePhotoReconciler.RemoteDocument(
                            "other-" + i,
                            "other.jpg",
                            DrivePhotoUploader.JPEG_MIME_TYPE,
                            10L));
            provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(docs, false));
        }

        DrivePhotoReconciler.Result result = new DrivePhotoReconciler(provider).reconcile(
                uncertain,
                prepared);

        assertEquals(DrivePhotoReconciler.Result.Outcome.REMAIN_UNCERTAIN, result.outcome());
        assertEquals(6, provider.queryCalls);
    }

    @Test
    public void sequencedRecordReconcilesUsingCaptureOrderFilename() throws Exception {
        File prepared = prepared("sequenced-photo");
        PendingPhotoRecord uncertain = PendingPhotoRecord.createCapturing(
                PHOTO_ID,
                1_700_000_000_000L,
                "address-provider-id",
                "Address",
                WORK_ID,
                "Cut Grass - 2026-09-21",
                12)
                .withState(PendingPhotoRecord.State.WAITING)
                .beginUploadAttempt(1_700_000_100_000L)
                .markUploadUncertain("ambiguous create result");
        FakeProvider provider = new FakeProvider();
        DrivePhotoReconciler.RemoteDocument candidate = new DrivePhotoReconciler.RemoteDocument(
                REMOTE_ID,
                "012_field-photo-" + PHOTO_ID + ".jpg",
                DrivePhotoUploader.JPEG_MIME_TYPE,
                prepared.length());
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.singletonList(candidate), false));
        provider.queries.add(new DrivePhotoReconciler.ChildQueryResult(
                Collections.singletonList(candidate), false));
        provider.remoteHash = DrivePhotoReconciler.sha256File(prepared);

        DrivePhotoReconciler.Result result = new DrivePhotoReconciler(provider).reconcile(
                uncertain,
                prepared);

        assertEquals(DrivePhotoReconciler.Result.Outcome.CONFIRMED_MATCH, result.outcome());
        assertEquals(REMOTE_ID, result.remoteFileId());
    }

    private File prepared(String text) throws Exception {
        File file = temporaryFolder.newFile("prepared-" + System.nanoTime() + ".jpg");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        return file;
    }

    private static PendingPhotoRecord uncertainWithoutProvisional() {
        return PendingPhotoRecord.createCapturing(
                PHOTO_ID,
                1_700_000_000_000L,
                "address-provider-id",
                "Address",
                WORK_ID,
                "Cut Grass - 2026-09-21")
                .withState(PendingPhotoRecord.State.WAITING)
                .beginUploadAttempt(1_700_000_100_000L)
                .markUploadUncertain("ambiguous create result");
    }

    private static PendingPhotoRecord uncertainWithProvisional(String remoteId) {
        return PendingPhotoRecord.createCapturing(
                PHOTO_ID,
                1_700_000_000_000L,
                "address-provider-id",
                "Address",
                WORK_ID,
                "Cut Grass - 2026-09-21")
                .withState(PendingPhotoRecord.State.WAITING)
                .beginUploadAttempt(1_700_000_100_000L)
                .recordProvisionalRemoteFileId(remoteId)
                .markUploadUncertain("ambiguous write result");
    }

    private static DrivePhotoReconciler.RemoteDocument candidate(String id, long size) {
        return new DrivePhotoReconciler.RemoteDocument(
                id,
                DrivePhotoUploader.remoteFileNameFor(PHOTO_ID),
                DrivePhotoUploader.JPEG_MIME_TYPE,
                size);
    }

    private static byte[] sha256(byte[] bytes) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    private static final class FakeProvider implements DrivePhotoReconciler.ProviderOps {
        DrivePhotoReconciler.RemoteDocument exact;
        boolean failExactRead;
        boolean refreshAccepted = true;
        final List<DrivePhotoReconciler.ChildQueryResult> queries = new ArrayList<>();
        byte[] remoteHash;
        int readCalls;
        int refreshCalls;
        int queryCalls;
        int hashCalls;
        String lastParentId;

        @Override
        public DrivePhotoReconciler.RemoteDocument readDocument(String documentId)
                throws IOException {
            readCalls++;
            if (failExactRead || exact == null) {
                throw new IOException("exact document unavailable");
            }
            return exact;
        }

        @Override
        public boolean requestFreshParent(String parentDocumentId) {
            refreshCalls++;
            lastParentId = parentDocumentId;
            return refreshAccepted;
        }

        @Override
        public DrivePhotoReconciler.ChildQueryResult queryChildren(String parentDocumentId)
                throws IOException {
            queryCalls++;
            lastParentId = parentDocumentId;
            if (queries.isEmpty()) {
                throw new IOException("no child query fixture");
            }
            int index = Math.min(queryCalls - 1, queries.size() - 1);
            return queries.get(index);
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
