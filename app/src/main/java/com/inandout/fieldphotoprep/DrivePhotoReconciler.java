package com.inandout.fieldphotoprep;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Resolves only persisted UNCERTAIN photo uploads. It never creates, writes, deletes, renames, or
 * redirects remote content.
 */
public final class DrivePhotoReconciler {
    private static final int FRESH_CHILD_MAX_ATTEMPTS = 6;
    private static final long FRESH_CHILD_RETRY_DELAY_MS = 500L;

    interface Sleeper {
        void sleep(long millis) throws IOException;
    }

    interface ProviderOps {
        RemoteDocument readDocument(String documentId) throws IOException;

        boolean requestFreshParent(String parentDocumentId) throws IOException;

        ChildQueryResult queryChildren(String parentDocumentId) throws IOException;

        byte[] sha256Remote(String documentId) throws IOException;
    }

    static final class RemoteDocument {
        private final String id;
        private final String displayName;
        private final String mimeType;
        private final long sizeBytes;

        RemoteDocument(String id, String displayName, String mimeType, long sizeBytes) {
            this.id = requireText(id, "remote document id");
            this.displayName = requireText(displayName, "remote display name");
            this.mimeType = requireText(mimeType, "remote MIME type");
            if (sizeBytes < -1L) {
                throw new IllegalArgumentException("Remote size must be -1 for unknown or non-negative.");
            }
            this.sizeBytes = sizeBytes;
        }

        String id() {
            return id;
        }

        String displayName() {
            return displayName;
        }

        String mimeType() {
            return mimeType;
        }

        long sizeBytes() {
            return sizeBytes;
        }
    }

    static final class ChildQueryResult {
        private final List<RemoteDocument> documents;
        private final boolean loading;

        ChildQueryResult(List<RemoteDocument> documents, boolean loading) {
            List<RemoteDocument> copy = new ArrayList<>(Objects.requireNonNull(documents, "documents"));
            copy.sort(Comparator.comparing(RemoteDocument::id));
            this.documents = Collections.unmodifiableList(copy);
            this.loading = loading;
        }

        List<RemoteDocument> documents() {
            return documents;
        }

        boolean loading() {
            return loading;
        }
    }

    public static final class Result {
        public enum Outcome {
            CONFIRMED_MATCH,
            CONFIRMED_ABSENT_RETRY_SAFE,
            REMAIN_UNCERTAIN
        }

        private final Outcome outcome;
        private final String remoteFileId;
        private final String detail;

        private Result(Outcome outcome, String remoteFileId, String detail) {
            this.outcome = Objects.requireNonNull(outcome, "outcome");
            this.remoteFileId = normalizeOptional(remoteFileId);
            this.detail = requireText(detail, "reconciliation detail");
            if (outcome == Outcome.CONFIRMED_MATCH && this.remoteFileId == null) {
                throw new IllegalArgumentException("Confirmed reconciliation requires remote identity.");
            }
            if (outcome != Outcome.CONFIRMED_MATCH && this.remoteFileId != null) {
                throw new IllegalArgumentException("Only confirmed reconciliation may carry remote identity.");
            }
        }

        public Outcome outcome() {
            return outcome;
        }

        public String remoteFileId() {
            return remoteFileId;
        }

        public String detail() {
            return detail;
        }

        static Result confirmed(String remoteFileId) {
            return new Result(
                    Outcome.CONFIRMED_MATCH,
                    remoteFileId,
                    "Remote photo content matches the prepared local photo.");
        }

        static Result retrySafeAbsent() {
            return new Result(
                    Outcome.CONFIRMED_ABSENT_RETRY_SAFE,
                    null,
                    "Settled provider state confirmed the expected remote photo is absent; retry is safe.");
        }

        static Result uncertain(String detail) {
            return new Result(Outcome.REMAIN_UNCERTAIN, null, detail);
        }
    }

    private final ProviderOps provider;
    private final Sleeper sleeper;

    public DrivePhotoReconciler(ContentResolver resolver, Uri treeUri) {
        this(new AndroidProviderOps(resolver, treeUri), DrivePhotoReconciler::sleepNormally);
    }

    DrivePhotoReconciler(ProviderOps provider) {
        this(provider, millis -> { });
    }

    DrivePhotoReconciler(ProviderOps provider, Sleeper sleeper) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
    }

    public Result reconcile(PendingPhotoRecord record, File preparedFile) {
        if (record == null || record.state() != PendingPhotoRecord.State.UNCERTAIN) {
            return Result.uncertain("Only a persisted UNCERTAIN photo can be reconciled.");
        }
        if (preparedFile == null || !preparedFile.isFile() || preparedFile.length() <= 0) {
            return Result.uncertain(
                    "The prepared local JPEG is unavailable, so remote content cannot be proven identical.");
        }

        final byte[] localHash;
        try {
            localHash = sha256File(preparedFile);
        } catch (IOException error) {
            return Result.uncertain("The prepared local JPEG could not be hashed safely.");
        }

        final String expectedName = DrivePhotoUploader.remoteFileNameFor(record.id());
        final String provisionalId = record.provisionalRemoteFileId();

        if (provisionalId != null) {
            try {
                RemoteDocument provisional = provider.readDocument(provisionalId);
                Result exactResult = proveCandidate(
                        provisional,
                        provisionalId,
                        expectedName,
                        preparedFile.length(),
                        localHash);
                if (exactResult.outcome() == Result.Outcome.CONFIRMED_MATCH) {
                    return exactResult;
                }
                if (isDefiniteCandidateMismatch(provisional, expectedName, preparedFile.length())) {
                    return exactResult;
                }
                if (exactResult.outcome() == Result.Outcome.REMAIN_UNCERTAIN) {
                    return exactResult;
                }
            } catch (IOException | RuntimeException ignored) {
                // Fall back to settled parent discovery. Unresolved provisional identity still blocks
                // retry release if the parent later appears empty.
            }
        }

        final List<RemoteDocument> children;
        try {
            children = settledChildren(record.workOrderId());
        } catch (IOException | RuntimeException error) {
            return Result.uncertain(
                    "Drive could not establish two matching settled parent listings; retry remains blocked.");
        }

        List<RemoteDocument> matches = new ArrayList<>();
        for (RemoteDocument child : children) {
            if (expectedName.equals(child.displayName())) {
                matches.add(child);
            }
        }

        if (matches.isEmpty()) {
            if (provisionalId != null) {
                return Result.uncertain(
                        "The deterministic filename is absent from settled parent state, but unresolved provisional remote identity remains.");
            }
            return Result.retrySafeAbsent();
        }

        if (matches.size() > 1) {
            return Result.uncertain(
                    "Multiple remote photos have the deterministic filename; the app will not guess between them.");
        }

        RemoteDocument candidate = matches.get(0);
        if (provisionalId != null && !provisionalId.equals(candidate.id())) {
            return Result.uncertain(
                    "A deterministic-name candidate exists, but it does not match the unresolved provisional remote identity.");
        }

        return proveCandidate(
                candidate,
                candidate.id(),
                expectedName,
                preparedFile.length(),
                localHash);
    }

    private Result proveCandidate(
            RemoteDocument candidate,
            String expectedId,
            String expectedName,
            long expectedBytes,
            byte[] localHash) {
        if (candidate == null
                || !expectedId.equals(candidate.id())
                || !expectedName.equals(candidate.displayName())
                || !DrivePhotoUploader.JPEG_MIME_TYPE.equals(candidate.mimeType())) {
            return Result.uncertain(
                    "Remote candidate identity, name, or MIME type does not match the expected photo.");
        }

        // Provider size metadata is advisory during cloud synchronization. A stale 0 or old byte
        // count must not override a stronger read-only SHA-256 proof of the exact remote content.
        final byte[] remoteHash;
        try {
            remoteHash = provider.sha256Remote(candidate.id());
        } catch (IOException | RuntimeException error) {
            return Result.uncertain(
                    "Remote candidate content could not be read strongly enough to prove a match.");
        }

        if (!Arrays.equals(localHash, remoteHash)) {
            return Result.uncertain(
                    "Remote candidate content does not match the prepared local photo.");
        }
        return Result.confirmed(candidate.id());
    }

    private static boolean isDefiniteCandidateMismatch(
            RemoteDocument candidate,
            String expectedName,
            long expectedBytes) {
        return candidate == null
                || !expectedName.equals(candidate.displayName())
                || !DrivePhotoUploader.JPEG_MIME_TYPE.equals(candidate.mimeType());
    }

    private List<RemoteDocument> settledChildren(String parentDocumentId) throws IOException {
        if (!provider.requestFreshParent(parentDocumentId)) {
            throw new IOException("Drive provider did not accept a refresh request for reconciliation.");
        }

        List<RemoteDocument> previousSettled = null;
        for (int attempt = 0; attempt < FRESH_CHILD_MAX_ATTEMPTS; attempt++) {
            ChildQueryResult result = provider.queryChildren(parentDocumentId);
            if (!result.loading()) {
                if (previousSettled != null
                        && sameRemoteDocuments(previousSettled, result.documents())) {
                    return result.documents();
                }
                previousSettled = result.documents();
            } else {
                previousSettled = null;
            }

            if (attempt + 1 < FRESH_CHILD_MAX_ATTEMPTS) {
                sleeper.sleep(FRESH_CHILD_RETRY_DELAY_MS);
            }
        }

        throw new IOException("Drive did not return two matching settled parent listings.");
    }

    static boolean sameRemoteDocuments(List<RemoteDocument> first, List<RemoteDocument> second) {
        if (first.size() != second.size()) {
            return false;
        }
        List<RemoteDocument> a = new ArrayList<>(first);
        List<RemoteDocument> b = new ArrayList<>(second);
        Comparator<RemoteDocument> byId = Comparator.comparing(RemoteDocument::id);
        a.sort(byId);
        b.sort(byId);
        for (int i = 0; i < a.size(); i++) {
            RemoteDocument left = a.get(i);
            RemoteDocument right = b.get(i);
            if (!left.id().equals(right.id())
                    || !left.displayName().equals(right.displayName())
                    || !left.mimeType().equals(right.mimeType())
                    || left.sizeBytes() != right.sizeBytes()) {
                return false;
            }
        }
        return true;
    }

    static byte[] sha256File(File file) throws IOException {
        if (file == null || !file.isFile() || file.length() <= 0) {
            throw new IOException("File is missing or empty.");
        }
        try (InputStream input = new FileInputStream(file)) {
            return sha256(input);
        }
    }

    private static byte[] sha256(InputStream input) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 is unavailable.", impossible);
        }
        byte[] buffer = new byte[64 * 1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            digest.update(buffer, 0, count);
        }
        return digest.digest();
    }

    private static void sleepNormally(long millis) throws IOException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Drive reconciliation freshness wait was interrupted.", interrupted);
        }
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }

    private static final class AndroidProviderOps implements ProviderOps {
        private static final String[] PROJECTION = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
        };

        private final ContentResolver resolver;
        private final Uri treeUri;

        AndroidProviderOps(ContentResolver resolver, Uri treeUri) {
            this.resolver = Objects.requireNonNull(resolver, "resolver");
            this.treeUri = treeUri;
        }

        @Override
        public RemoteDocument readDocument(String documentId) throws IOException {
            ensureTreeUri();
            Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
            try (Cursor cursor = resolver.query(documentUri, PROJECTION, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst()) {
                    throw new IOException("Document provider did not return the requested remote photo.");
                }
                if (isCursorLoading(cursor)) {
                    throw new IOException("Drive is still loading the requested remote photo.");
                }
                return readCurrentDocument(cursor);
            } catch (SecurityException error) {
                throw new IOException("Persisted Drive access is unavailable for reconciliation.", error);
            }
        }

        @Override
        public boolean requestFreshParent(String parentDocumentId) throws IOException {
            ensureTreeUri();
            Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocumentId);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId);
            boolean accepted = false;
            try {
                accepted = resolver.refresh(parentUri, Bundle.EMPTY, null);
            } catch (RuntimeException ignored) {
                // Try the child-list URI as the second provider refresh surface.
            }
            try {
                accepted = resolver.refresh(childrenUri, Bundle.EMPTY, null) || accepted;
            } catch (RuntimeException ignored) {
                // A provider may not implement refresh; settled non-loading queries remain authoritative.
            }

            // ContentResolver.refresh() is a best-effort hint. Android providers may return false
            // simply because refresh is unsupported. Reconciliation authority comes from the two
            // matching non-loading child snapshots below, not this optional hint's return value.
            return true;
        }

        @Override
        public ChildQueryResult queryChildren(String parentDocumentId) throws IOException {
            ensureTreeUri();
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId);
            List<RemoteDocument> documents = new ArrayList<>();
            try (Cursor cursor = resolver.query(childrenUri, PROJECTION, null, null, null)) {
                if (cursor == null) {
                    throw new IOException("Drive did not return the reconciliation parent contents.");
                }
                boolean loading = isCursorLoading(cursor);
                while (cursor.moveToNext()) {
                    documents.add(readCurrentDocument(cursor));
                }
                return new ChildQueryResult(documents, loading);
            } catch (SecurityException error) {
                throw new IOException("Persisted Drive access is unavailable for reconciliation.", error);
            }
        }

        @Override
        public byte[] sha256Remote(String documentId) throws IOException {
            ensureTreeUri();
            Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
            try (InputStream input = resolver.openInputStream(documentUri)) {
                if (input == null) {
                    throw new IOException("Document provider did not open the remote photo for reading.");
                }
                return sha256(input);
            } catch (SecurityException error) {
                throw new IOException("Drive access was denied while reading remote photo content.", error);
            }
        }

        private RemoteDocument readCurrentDocument(Cursor cursor) {
            int idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
            int sizeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
            long size = sizeColumn < 0 || cursor.isNull(sizeColumn) ? -1L : cursor.getLong(sizeColumn);
            return new RemoteDocument(
                    cursor.getString(idColumn),
                    cursor.getString(nameColumn),
                    cursor.getString(mimeColumn),
                    size);
        }

        private boolean isCursorLoading(Cursor cursor) {
            Bundle extras = cursor.getExtras();
            return extras != null && extras.getBoolean(DocumentsContract.EXTRA_LOADING, false);
        }

        private void ensureTreeUri() throws IOException {
            if (treeUri == null) {
                throw new IOException("No persisted master Drive folder is available.");
            }
        }
    }
}
