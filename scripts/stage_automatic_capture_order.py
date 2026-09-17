from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"anchor not found: {label}")
    return text.replace(old, new, 1)


# PendingPhotoRecord: schema v4 with optional legacy-compatible captureSequence.
path = Path('app/src/main/java/com/inandout/fieldphotoprep/PendingPhotoRecord.java')
text = path.read_text()
text = replace_once(text, 'static final int CURRENT_SCHEMA_VERSION = 3;',
                    'static final int CURRENT_SCHEMA_VERSION = 4;', 'schema version')
text = replace_once(text,
                    '    private static final String KEY_CREATED_AT = "createdAtEpochMs";\n',
                    '    private static final String KEY_CREATED_AT = "createdAtEpochMs";\n'
                    '    private static final String KEY_CAPTURE_SEQUENCE = "captureSequence";\n',
                    'capture sequence key')
text = replace_once(text,
                    '    private final long createdAtEpochMs;\n',
                    '    private final long createdAtEpochMs;\n'
                    '    private final int captureSequence;\n',
                    'capture sequence field')
text = replace_once(text,
                    '                createdAtEpochMs,\n                addressId,\n',
                    '                createdAtEpochMs,\n                0,\n                addressId,\n',
                    'public constructor default sequence')
text = replace_once(text,
                    '            State state,\n            long createdAtEpochMs,\n            String addressId,\n',
                    '            State state,\n            long createdAtEpochMs,\n            int captureSequence,\n            String addressId,\n',
                    'private constructor sequence arg')
text = replace_once(text,
                    '        this.createdAtEpochMs = createdAtEpochMs;\n        this.addressId = requireText(addressId, "addressId");\n',
                    '        this.createdAtEpochMs = createdAtEpochMs;\n'
                    '        if (captureSequence < 0) {\n'
                    '            throw new IllegalArgumentException("captureSequence cannot be negative.");\n'
                    '        }\n'
                    '        this.captureSequence = captureSequence;\n'
                    '        this.addressId = requireText(addressId, "addressId");\n',
                    'capture sequence validation')
old_create = '''    public static PendingPhotoRecord createCapturing(
            String id,
            long createdAtEpochMs,
            String addressId,
            String addressName,
            String workOrderId,
            String workOrderName) {
        return new PendingPhotoRecord(
                id,
                imageFileNameFor(id),
                State.CAPTURING,
                createdAtEpochMs,
                addressId,
                addressName,
                workOrderId,
                workOrderName);
    }
'''
new_create = '''    public static PendingPhotoRecord createCapturing(
            String id,
            long createdAtEpochMs,
            String addressId,
            String addressName,
            String workOrderId,
            String workOrderName) {
        return new PendingPhotoRecord(
                id,
                imageFileNameFor(id),
                State.CAPTURING,
                createdAtEpochMs,
                0,
                addressId,
                addressName,
                workOrderId,
                workOrderName);
    }

    public static PendingPhotoRecord createCapturing(
            String id,
            long createdAtEpochMs,
            String addressId,
            String addressName,
            String workOrderId,
            String workOrderName,
            int captureSequence) {
        if (captureSequence <= 0) {
            throw new IllegalArgumentException("New captureSequence must be positive.");
        }
        return new PendingPhotoRecord(
                id,
                imageFileNameFor(id),
                State.CAPTURING,
                createdAtEpochMs,
                captureSequence,
                addressId,
                addressName,
                workOrderId,
                workOrderName);
    }
'''
text = replace_once(text, old_create, new_create, 'createCapturing overload')
text = replace_once(text,
                    '    public long createdAtEpochMs() {\n        return createdAtEpochMs;\n    }\n',
                    '    public long createdAtEpochMs() {\n        return createdAtEpochMs;\n    }\n\n'
                    '    public int captureSequence() {\n        return captureSequence;\n    }\n\n'
                    '    public boolean hasCaptureSequence() {\n        return captureSequence > 0;\n    }\n',
                    'capture sequence getters')
text = replace_once(text,
                    '        properties.setProperty(KEY_CREATED_AT, Long.toString(createdAtEpochMs));\n',
                    '        properties.setProperty(KEY_CREATED_AT, Long.toString(createdAtEpochMs));\n'
                    '        properties.setProperty(KEY_CAPTURE_SEQUENCE, Integer.toString(captureSequence));\n',
                    'persist capture sequence')
text = replace_once(text,
                    '        long createdAt = parsePositiveLong(properties, KEY_CREATED_AT,\n                "Invalid pending-photo createdAtEpochMs.");\n\n',
                    '        long createdAt = parsePositiveLong(properties, KEY_CREATED_AT,\n'
                    '                "Invalid pending-photo createdAtEpochMs.");\n'
                    '        int captureSequence = schemaVersion >= 4\n'
                    '                ? parseNonNegativeInt(properties, KEY_CAPTURE_SEQUENCE)\n'
                    '                : 0;\n\n',
                    'load capture sequence')
text = replace_once(text,
                    '                    state,\n                    createdAt,\n                    requiredProperty(properties, KEY_ADDRESS_ID),\n',
                    '                    state,\n                    createdAt,\n                    0,\n                    requiredProperty(properties, KEY_ADDRESS_ID),\n',
                    'legacy constructor sequence')
text = replace_once(text,
                    '                state,\n                createdAt,\n                requiredProperty(properties, KEY_ADDRESS_ID),\n',
                    '                state,\n                createdAt,\n                captureSequence,\n                requiredProperty(properties, KEY_ADDRESS_ID),\n',
                    'current constructor sequence')
text = replace_once(text,
                    '                nextState,\n                createdAtEpochMs,\n                addressId,\n',
                    '                nextState,\n                createdAtEpochMs,\n                captureSequence,\n                addressId,\n',
                    'copy preserves sequence')
text = replace_once(text,
                    '    @Override\n    public int compareTo(PendingPhotoRecord other) {\n        int byTime = Long.compare(createdAtEpochMs, other.createdAtEpochMs);\n        return byTime != 0 ? byTime : id.compareTo(other.id);\n    }\n',
                    '    @Override\n'
                    '    public int compareTo(PendingPhotoRecord other) {\n'
                    '        if (workOrderId.equals(other.workOrderId)\n'
                    '                && captureSequence > 0\n'
                    '                && other.captureSequence > 0) {\n'
                    '            int bySequence = Integer.compare(captureSequence, other.captureSequence);\n'
                    '            if (bySequence != 0) {\n'
                    '                return bySequence;\n'
                    '            }\n'
                    '        }\n'
                    '        int byTime = Long.compare(createdAtEpochMs, other.createdAtEpochMs);\n'
                    '        return byTime != 0 ? byTime : id.compareTo(other.id);\n'
                    '    }\n',
                    'sequence ordering')
path.write_text(text)


# PendingPhotoStore: allocate sequence durably before accepting a shot.
path = Path('app/src/main/java/com/inandout/fieldphotoprep/PendingPhotoStore.java')
text = path.read_text()
text = replace_once(text,
                    'public final class PendingPhotoStore {\n',
                    'public final class PendingPhotoStore {\n'
                    '    static final int MAX_CAPTURE_SEQUENCE = 999;\n',
                    'store max sequence')
text = replace_once(text,
                    '    public PendingPhotoRecord beginCapture(DriveFolder address, DriveFolder workOrder) throws IOException {\n',
                    '    public synchronized PendingPhotoRecord beginCapture(DriveFolder address, DriveFolder workOrder) throws IOException {\n',
                    'synchronized capture reservation')
text = replace_once(text,
                    '        ensureRoot();\n\n        String id = idSource.nextId();\n',
                    '        ensureRoot();\n\n'
                    '        int captureSequence = nextCaptureSequenceForWorkOrder(workOrder.id());\n'
                    '        String id = idSource.nextId();\n',
                    'allocate sequence before id')
text = replace_once(text,
                    '                workOrder.id(),\n                workOrder.name());\n',
                    '                workOrder.id(),\n                workOrder.name(),\n                captureSequence);\n',
                    'bind sequence to record')
anchor = '    public PendingPhotoRecord finishCaptureIfImageExists(String id) throws IOException {\n'
methods = '''    private int nextCaptureSequenceForWorkOrder(String workOrderId) throws IOException {
        ScanResult existing = scan();
        int retainedCount = 0;
        int maxStoredSequence = 0;
        for (PendingPhotoRecord record : existing.records()) {
            if (!workOrderId.equals(record.workOrderId())) {
                continue;
            }
            retainedCount++;
            maxStoredSequence = Math.max(maxStoredSequence, record.captureSequence());
        }
        return nextCaptureSequenceValue(retainedCount, maxStoredSequence);
    }

    static int nextCaptureSequenceValue(int retainedCount, int maxStoredSequence)
            throws IOException {
        if (retainedCount < 0 || maxStoredSequence < 0) {
            throw new IOException("Capture-order history is invalid.");
        }
        int baseline = Math.max(retainedCount, maxStoredSequence);
        if (baseline >= MAX_CAPTURE_SEQUENCE) {
            throw new IOException(
                    "This work order already has 999 retained photo positions. Start a new work order before taking another photo so Drive order remains exact.");
        }
        return baseline + 1;
    }

'''
text = replace_once(text, anchor, methods + anchor, 'sequence allocator methods')
path.write_text(text)


# DrivePhotoUploader: new records get NNN_ prefix; legacy records keep old names.
path = Path('app/src/main/java/com/inandout/fieldphotoprep/DrivePhotoUploader.java')
text = path.read_text()
text = replace_once(text,
                    'import java.util.Objects;\n',
                    'import java.util.Locale;\nimport java.util.Objects;\n',
                    'Locale import')
text = replace_once(text,
                    '        final String remoteName = remoteFileNameFor(uploadingRecord.id());\n',
                    '        final String remoteName = remoteFileNameFor(uploadingRecord);\n',
                    'create remote name')
text = replace_once(text,
                    '        if (!createdUpload.remoteDisplayName().equals(remoteFileNameFor(uploadingRecord.id()))) {\n',
                    '        if (!createdUpload.remoteDisplayName().equals(remoteFileNameFor(uploadingRecord))) {\n',
                    'write verify remote name')
old_helper = '''    static String remoteFileNameFor(String photoId) {
        return "field-photo-" + PendingPhotoRecord.imageFileNameFor(photoId)
                .substring("photo-".length());
    }
'''
new_helper = '''    static String remoteFileNameFor(String photoId) {
        return "field-photo-" + PendingPhotoRecord.imageFileNameFor(photoId)
                .substring("photo-".length());
    }

    static String remoteFileNameFor(PendingPhotoRecord record) {
        Objects.requireNonNull(record, "record");
        String legacyName = remoteFileNameFor(record.id());
        if (!record.hasCaptureSequence()) {
            return legacyName;
        }
        if (record.captureSequence() > PendingPhotoStore.MAX_CAPTURE_SEQUENCE) {
            throw new IllegalArgumentException("Capture sequence exceeds supported Drive ordering range.");
        }
        return String.format(Locale.US, "%03d_%s", record.captureSequence(), legacyName);
    }
'''
text = replace_once(text, old_helper, new_helper, 'record-based remote filename')
path.write_text(text)


# Reconciler must use exactly the same deterministic record-based name.
path = Path('app/src/main/java/com/inandout/fieldphotoprep/DrivePhotoReconciler.java')
text = path.read_text()
text = replace_once(text,
                    '        final String expectedName = DrivePhotoUploader.remoteFileNameFor(record.id());\n',
                    '        final String expectedName = DrivePhotoUploader.remoteFileNameFor(record);\n',
                    'reconcile record-based name')
path.write_text(text)


# Capture-order diagnostic export reports stored sequence/name when present.
path = Path('app/src/main/java/com/inandout/fieldphotoprep/CaptureOrderManifest.java')
text = path.read_text()
text = replace_once(text,
                    '            String sequence = String.format(Locale.US, "%0" + width + "d", i + 1);\n',
                    '            int sequenceValue = record.hasCaptureSequence()\n'
                    '                    ? record.captureSequence()\n'
                    '                    : i + 1;\n'
                    '            String sequence = String.format(Locale.US, "%0" + width + "d", sequenceValue);\n',
                    'manifest stored sequence')
text = replace_once(text,
                    '                    .append(\'|\').append(DrivePhotoUploader.remoteFileNameFor(record.id()))\n',
                    '                    .append(\'|\').append(DrivePhotoUploader.remoteFileNameFor(record))\n',
                    'manifest record-based name')
path.write_text(text)


# Exercise sequenced uploader behavior through the existing upload safety suite.
path = Path('app/src/test/java/com/inandout/fieldphotoprep/DrivePhotoUploaderTest.java')
text = path.read_text()
text = replace_once(text,
                    '        assertEquals(DrivePhotoUploader.remoteFileNameFor(PHOTO_ID), provider.createDisplayName);\n',
                    '        assertEquals("012_field-photo-" + PHOTO_ID + ".jpg", provider.createDisplayName);\n',
                    'uploader expected sequenced name')
text = replace_once(text,
                    '                        "work-provider-id",\n                        "Cut Grass - 2026-09-09")\n',
                    '                        "work-provider-id",\n                        "Cut Grass - 2026-09-09",\n                        12)\n',
                    'uploader sequenced fixture')
path.write_text(text)


# New focused regression suite for schema/allocator/legacy/new naming.
path = Path('app/src/test/java/com/inandout/fieldphotoprep/AutomaticCaptureOrderFilenameTest.java')
path.write_text(r'''package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class AutomaticCaptureOrderFilenameTest {
    private static final String ID1 = "11111111-1111-4111-8111-111111111111";
    private static final String ID2 = "22222222-2222-4222-8222-222222222222";
    private static final String ID3 = "33333333-3333-4333-8333-333333333333";
    private static final String ID4 = "44444444-4444-4444-8444-444444444444";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void schemaV4RoundTripPreservesCaptureSequenceAndRemoteName() {
        PendingPhotoRecord record = sequenced(ID1, "work-a", 7);

        PendingPhotoRecord restored = PendingPhotoRecord.fromProperties(record.toProperties());

        assertEquals(7, restored.captureSequence());
        assertTrue(restored.hasCaptureSequence());
        assertEquals("007_field-photo-" + ID1 + ".jpg",
                DrivePhotoUploader.remoteFileNameFor(restored));
    }

    @Test
    public void legacySchemaV3LoadsUnsequencedAndKeepsLegacyRemoteName() {
        PendingPhotoRecord record = sequenced(ID1, "work-a", 7);
        Properties legacy = record.toProperties();
        legacy.setProperty("schemaVersion", "3");
        legacy.remove("captureSequence");

        PendingPhotoRecord restored = PendingPhotoRecord.fromProperties(legacy);

        assertEquals(0, restored.captureSequence());
        assertFalse(restored.hasCaptureSequence());
        assertEquals("field-photo-" + ID1 + ".jpg",
                DrivePhotoUploader.remoteFileNameFor(restored));
    }

    @Test
    public void captureSequenceIncrementsPerWorkOrderAndSurvivesStoreReopen() throws Exception {
        File root = temporaryFolder.newFolder("pending");
        SequenceIds ids = new SequenceIds(ID1, ID2, ID3, ID4);
        PendingPhotoStore store = new PendingPhotoStore(
                root,
                ids,
                new IncrementingTime(1_700_000_000_000L));
        DriveFolder address = new DriveFolder("address", "Address");
        DriveFolder workA = new DriveFolder("work-a", "Work A - 2026-09-17");
        DriveFolder workB = new DriveFolder("work-b", "Work B - 2026-09-17");

        PendingPhotoRecord first = store.beginCapture(address, workA);
        PendingPhotoRecord second = store.beginCapture(address, workA);
        PendingPhotoRecord otherWork = store.beginCapture(address, workB);

        PendingPhotoStore reopened = new PendingPhotoStore(
                root,
                ids,
                new IncrementingTime(1_700_000_100_000L));
        PendingPhotoRecord third = reopened.beginCapture(address, workA);

        assertEquals(1, first.captureSequence());
        assertEquals(2, second.captureSequence());
        assertEquals(1, otherWork.captureSequence());
        assertEquals(3, third.captureSequence());
    }

    @Test
    public void legacyRetainedRecordsAdvanceFirstNewSequence() throws Exception {
        File root = temporaryFolder.newFolder("legacy-pending");
        PendingPhotoStore legacyWriter = new PendingPhotoStore(
                root,
                new SequenceIds(ID1, ID2),
                new IncrementingTime(1_700_000_000_000L));
        DriveFolder address = new DriveFolder("address", "Address");
        DriveFolder work = new DriveFolder("work-a", "Work A - 2026-09-17");
        PendingPhotoRecord first = legacyWriter.beginCapture(address, work);
        PendingPhotoRecord second = legacyWriter.beginCapture(address, work);

        downgradeToLegacy(root, first);
        downgradeToLegacy(root, second);

        PendingPhotoStore upgraded = new PendingPhotoStore(
                root,
                new SequenceIds(ID3),
                new IncrementingTime(1_700_000_100_000L));
        PendingPhotoRecord next = upgraded.beginCapture(address, work);

        assertEquals(3, next.captureSequence());
    }

    @Test
    public void sequenceLimitFailsClosedBeforeOrderingCanBecomeAmbiguous() throws Exception {
        assertEquals(999, PendingPhotoStore.nextCaptureSequenceValue(998, 998));
        try {
            PendingPhotoStore.nextCaptureSequenceValue(999, 999);
            fail("Expected 1000th-position refusal.");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("999"));
        }
    }

    private static PendingPhotoRecord sequenced(String id, String workId, int sequence) {
        return PendingPhotoRecord.createCapturing(
                id,
                1_700_000_000_000L + sequence,
                "address",
                "Address",
                workId,
                "Work - 2026-09-17",
                sequence);
    }

    private static void downgradeToLegacy(File root, PendingPhotoRecord record) throws Exception {
        File metadata = new File(root, record.metadataFileName());
        Properties properties = new Properties();
        try (java.io.FileInputStream input = new java.io.FileInputStream(metadata)) {
            properties.load(input);
        }
        properties.setProperty("schemaVersion", "3");
        properties.remove("captureSequence");
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(metadata)) {
            properties.store(output, "legacy fixture");
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

    private static final class IncrementingTime implements PendingPhotoStore.TimeSource {
        private long value;

        IncrementingTime(long initial) {
            this.value = initial;
        }

        @Override
        public long nowEpochMs() {
            return value++;
        }
    }
}
''')


# Add one sequenced reconciliation proof while retaining the existing legacy tests.
path = Path('app/src/test/java/com/inandout/fieldphotoprep/DrivePhotoReconcilerTest.java')
text = path.read_text()
insert_anchor = '    @Test\n    public void multipleDeterministicNameCandidatesRemainUncertain() throws Exception {\n'
new_test = '''    @Test
    public void sequencedDeterministicNameCandidateConfirms() throws Exception {
        File prepared = prepared("sequenced-photo-bytes");
        PendingPhotoRecord uncertain = PendingPhotoRecord.createCapturing(
                PHOTO_ID,
                1_700_000_000_000L,
                "address-provider-id",
                "Address",
                WORK_ID,
                "Cut Grass - 2026-09-21",
                7)
                .withState(PendingPhotoRecord.State.WAITING)
                .beginUploadAttempt(1_700_000_100_000L)
                .markUploadUncertain("ambiguous create result");
        FakeProvider provider = new FakeProvider();
        DrivePhotoReconciler.RemoteDocument candidate = new DrivePhotoReconciler.RemoteDocument(
                REMOTE_ID,
                "007_field-photo-" + PHOTO_ID + ".jpg",
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

'''
text = replace_once(text, insert_anchor, new_test + insert_anchor, 'sequenced reconciliation test')
path.write_text(text)


# Version bump only; signing configuration is intentionally untouched.
path = Path('app/build.gradle')
text = path.read_text()
text = replace_once(text, '        versionCode 21\n', '        versionCode 22\n', 'version code')
text = replace_once(text,
                    "        versionName '0.16-capture-order-export'\n",
                    "        versionName '0.17-automatic-capture-order'\n",
                    'version name')
path.write_text(text)
