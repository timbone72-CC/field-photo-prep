from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"anchor not found: {label}")
    return text.replace(old, new, 1)


# PendingPhotoRecord: schema v4 stores an optional capture sequence while schema 1-3 stay readable.
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
                    '''        this(
                id,
                imageFileName,
                state,
                createdAtEpochMs,
                addressId,
                addressName,
                workOrderId,
                workOrderName,
                0,
                0L,
                null,
                null,
                null);
''',
                    '''        this(
                id,
                imageFileName,
                state,
                createdAtEpochMs,
                0,
                addressId,
                addressName,
                workOrderId,
                workOrderName,
                0,
                0L,
                null,
                null,
                null);
''',
                    'public constructor default sequence')
text = replace_once(text,
                    '''    private PendingPhotoRecord(
            String id,
            String imageFileName,
            State state,
            long createdAtEpochMs,
            String addressId,
''',
                    '''    private PendingPhotoRecord(
            String id,
            String imageFileName,
            State state,
            long createdAtEpochMs,
            int captureSequence,
            String addressId,
''',
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
                    '''        long createdAt = parsePositiveLong(properties, KEY_CREATED_AT,
                "Invalid pending-photo createdAtEpochMs.");

''',
                    '''        long createdAt = parsePositiveLong(properties, KEY_CREATED_AT,
                "Invalid pending-photo createdAtEpochMs.");
        int captureSequence = schemaVersion >= 4
                ? parseNonNegativeInt(properties, KEY_CAPTURE_SEQUENCE)
                : 0;

''',
                    'load capture sequence')
text = replace_once(text,
                    '''                    state,
                    createdAt,
                    requiredProperty(properties, KEY_ADDRESS_ID),
''',
                    '''                    state,
                    createdAt,
                    0,
                    requiredProperty(properties, KEY_ADDRESS_ID),
''',
                    'schema1 constructor sequence')
text = replace_once(text,
                    '''                state,
                createdAt,
                requiredProperty(properties, KEY_ADDRESS_ID),
''',
                    '''                state,
                createdAt,
                captureSequence,
                requiredProperty(properties, KEY_ADDRESS_ID),
''',
                    'schema2plus constructor sequence')
text = replace_once(text,
                    '''                nextState,
                createdAtEpochMs,
                addressId,
''',
                    '''                nextState,
                createdAtEpochMs,
                captureSequence,
                addressId,
''',
                    'copy preserves sequence')
text = replace_once(text,
                    '        if (version != 1 && version != 2 && version != CURRENT_SCHEMA_VERSION) {\n',
                    '        if (version != 1 && version != 2 && version != 3\n'
                    '                && version != CURRENT_SCHEMA_VERSION) {\n',
                    'schema3 remains supported')
path.write_text(text)


# PendingPhotoStore: reserve sequence through a durable app-private ledger before accepting capture.
path = Path('app/src/main/java/com/inandout/fieldphotoprep/PendingPhotoStore.java')
text = path.read_text()
text = replace_once(text,
                    'public final class PendingPhotoStore {\n',
                    'public final class PendingPhotoStore {\n'
                    '    static final String CAPTURE_SEQUENCE_LEDGER_FILE = "capture-sequences.properties";\n'
                    '    private static final Object CAPTURE_SEQUENCE_LOCK = new Object();\n',
                    'capture sequence ledger constants')
old_begin = '''    public PendingPhotoRecord beginCapture(DriveFolder address, DriveFolder workOrder) throws IOException {
        if (address == null || workOrder == null) {
            throw new IOException("An exact address and work order are required before capture.");
        }
        ensureRoot();

        String id = idSource.nextId();
        PendingPhotoRecord record = PendingPhotoRecord.createCapturing(
                id,
                timeSource.nowEpochMs(),
                address.id(),
                address.name(),
                workOrder.id(),
                workOrder.name());
        File image = imageFile(record);
        File metadata = metadataFile(record);

        if (image.exists() || metadata.exists() || !image.createNewFile()) {
            throw new IOException("Could not reserve a unique local photo file.");
        }

        try {
            writeRecord(record);
            return record;
        } catch (IOException | RuntimeException error) {
            if (image.length() == 0) {
                image.delete();
            }
            throw error;
        }
    }

'''
new_begin = '''    public PendingPhotoRecord beginCapture(DriveFolder address, DriveFolder workOrder) throws IOException {
        if (address == null || workOrder == null) {
            throw new IOException("An exact address and work order are required before capture.");
        }
        synchronized (CAPTURE_SEQUENCE_LOCK) {
            ensureRoot();
            int captureSequence = reserveNextCaptureSequence(workOrder.id());
            String id = idSource.nextId();
            PendingPhotoRecord record = PendingPhotoRecord.createCapturing(
                    id,
                    timeSource.nowEpochMs(),
                    address.id(),
                    address.name(),
                    workOrder.id(),
                    workOrder.name(),
                    captureSequence);
            File image = imageFile(record);
            File metadata = metadataFile(record);

            if (image.exists() || metadata.exists() || !image.createNewFile()) {
                throw new IOException("Could not reserve a unique local photo file.");
            }

            try {
                writeRecord(record);
                return record;
            } catch (IOException | RuntimeException error) {
                if (image.length() == 0) {
                    image.delete();
                }
                throw error;
            }
        }
    }

    private int reserveNextCaptureSequence(String workOrderId) throws IOException {
        Properties ledger = readCaptureSequenceLedger();
        int persistedLast = readLedgerSequence(ledger, workOrderId);
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
        int baseline = Math.max(persistedLast, Math.max(retainedCount, maxStoredSequence));
        if (baseline == Integer.MAX_VALUE) {
            throw new IOException("Capture-order sequence is exhausted for this work order.");
        }
        int next = baseline + 1;
        ledger.setProperty(workOrderId, Integer.toString(next));
        writeCaptureSequenceLedger(ledger);
        return next;
    }

    private Properties readCaptureSequenceLedger() throws IOException {
        File ledgerFile = new File(root, CAPTURE_SEQUENCE_LEDGER_FILE);
        ensureDirectChild(ledgerFile);
        Properties ledger = new Properties();
        if (!ledgerFile.exists()) {
            return ledger;
        }
        if (!ledgerFile.isFile()) {
            throw new IOException("Capture-order sequence ledger is not a readable file.");
        }
        try (FileInputStream input = new FileInputStream(ledgerFile)) {
            ledger.load(input);
        } catch (IOException | RuntimeException error) {
            throw new IOException("Capture-order sequence ledger could not be read safely.", error);
        }
        return ledger;
    }

    private int readLedgerSequence(Properties ledger, String workOrderId) throws IOException {
        String raw = ledger.getProperty(workOrderId);
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        final int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException error) {
            throw new IOException("Capture-order sequence ledger contains an invalid value.", error);
        }
        if (value < 0) {
            throw new IOException("Capture-order sequence ledger contains a negative value.");
        }
        return value;
    }

    private void writeCaptureSequenceLedger(Properties ledger) throws IOException {
        File target = new File(root, CAPTURE_SEQUENCE_LEDGER_FILE);
        ensureDirectChild(target);
        File temp = new File(root, CAPTURE_SEQUENCE_LEDGER_FILE + ".tmp-" + UUID.randomUUID());
        ensureDirectChild(temp);
        try (FileOutputStream output = new FileOutputStream(temp)) {
            ledger.store(output, "Field Photo Prep capture sequence ledger");
            output.getFD().sync();
        } catch (IOException | RuntimeException error) {
            temp.delete();
            throw new IOException("Capture-order sequence ledger could not be written safely.", error);
        }
        try {
            try {
                Files.move(
                        temp.toPath(),
                        target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

'''
text = replace_once(text, old_begin, new_begin, 'durable sequence reservation')
path.write_text(text)


# DrivePhotoUploader: sequenced records get sortable names; legacy records retain old names.
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
        int width = Math.max(3, Integer.toString(record.captureSequence()).length());
        return String.format(Locale.US, "%0" + width + "d_%s", record.captureSequence(), legacyName);
    }
'''
text = replace_once(text, old_helper, new_helper, 'record-based remote filename')
path.write_text(text)


# Reconciler must prove the exact record-based deterministic filename.
path = Path('app/src/main/java/com/inandout/fieldphotoprep/DrivePhotoReconciler.java')
text = path.read_text()
text = replace_once(text,
                    '        final String expectedName = DrivePhotoUploader.remoteFileNameFor(record.id());\n',
                    '        final String expectedName = DrivePhotoUploader.remoteFileNameFor(record);\n',
                    'reconcile record-based name')
path.write_text(text)


# Capture-order diagnostic export reports the persisted sequence/name for new records.
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


# Existing uploader safety suite now exercises a sequenced new photo while retaining legacy helper coverage.
path = Path('app/src/test/java/com/inandout/fieldphotoprep/DrivePhotoUploaderTest.java')
text = path.read_text()
text = replace_once(text,
                    '        assertEquals(DrivePhotoUploader.remoteFileNameFor(PHOTO_ID), provider.createDisplayName);\n',
                    '        assertEquals("012_field-photo-" + PHOTO_ID + ".jpg", provider.createDisplayName);\n',
                    'sequenced create name assertion')
text = replace_once(text,
                    '''                        "work-provider-id",
                        "Cut Grass - 2026-09-09")
''',
                    '''                        "work-provider-id",
                        "Cut Grass - 2026-09-09",
                        12)
''',
                    'sequenced uploader fixture')
path.write_text(text)


# Add one explicit sequenced reconciliation proof without changing the legacy reconciliation fixtures.
path = Path('app/src/test/java/com/inandout/fieldphotoprep/DrivePhotoReconcilerTest.java')
text = path.read_text()
anchor = '    private File prepared(String text) throws Exception {\n'
method = '''    @Test
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

'''
text = replace_once(text, anchor, method + anchor, 'sequenced reconciliation test')
path.write_text(text)


# Focused schema/ledger/naming regression suite.
path = Path('app/src/test/java/com/inandout/fieldphotoprep/AutomaticCaptureOrderFilenameTest.java')
path.write_text(r'''package com.inandout.fieldphotoprep;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class AutomaticCaptureOrderFilenameTest {
    private static final String ID1 = "11111111-1111-4111-8111-111111111111";
    private static final String ID2 = "22222222-2222-4222-8222-222222222222";
    private static final String ID3 = "33333333-3333-4333-8333-333333333333";
    private static final String ID4 = "44444444-4444-4444-8444-444444444444";
    private static final DriveFolder ADDRESS = new DriveFolder("address-a", "Address A");
    private static final DriveFolder WORK_A = new DriveFolder("work-a", "Inspection - 2026-09-17");
    private static final DriveFolder WORK_B = new DriveFolder("work-b", "Grass Cut - 2026-09-17");

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void schemaV4RoundTripPreservesSequenceAndRemoteName() {
        PendingPhotoRecord record = PendingPhotoRecord.createCapturing(
                ID1, 1_700_000_000_000L,
                ADDRESS.id(), ADDRESS.name(), WORK_A.id(), WORK_A.name(), 7);

        PendingPhotoRecord restored = PendingPhotoRecord.fromProperties(record.toProperties());

        assertEquals(7, restored.captureSequence());
        assertTrue(restored.hasCaptureSequence());
        assertEquals("007_field-photo-" + ID1 + ".jpg",
                DrivePhotoUploader.remoteFileNameFor(restored));
    }

    @Test
    public void schemaV3RemainsReadableAndUsesLegacyRemoteName() {
        PendingPhotoRecord record = PendingPhotoRecord.createCapturing(
                ID1, 1_700_000_000_000L,
                ADDRESS.id(), ADDRESS.name(), WORK_A.id(), WORK_A.name(), 7);
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
    public void newCapturesAdvanceAndSeparateWorkOrdersStartAtOne() throws Exception {
        File root = temporaryFolder.newFolder("queue");
        PendingPhotoStore store = store(root, ID1, ID2, ID3, ID4);

        assertEquals(1, store.beginCapture(ADDRESS, WORK_A).captureSequence());
        assertEquals(2, store.beginCapture(ADDRESS, WORK_A).captureSequence());
        assertEquals(1, store.beginCapture(ADDRESS, WORK_B).captureSequence());
        assertEquals(3, store.beginCapture(ADDRESS, WORK_A).captureSequence());
    }

    @Test
    public void storeReopenContinuesFromDurableLedger() throws Exception {
        File root = temporaryFolder.newFolder("queue-reopen");
        PendingPhotoStore first = store(root, ID1);
        assertEquals(1, first.beginCapture(ADDRESS, WORK_A).captureSequence());

        PendingPhotoStore reopened = store(root, ID2);
        assertEquals(2, reopened.beginCapture(ADDRESS, WORK_A).captureSequence());
    }

    @Test
    public void removingEmptyReservationDoesNotReuseConsumedSequence() throws Exception {
        File root = temporaryFolder.newFolder("queue-empty-gap");
        PendingPhotoStore store = store(root, ID1, ID2);
        PendingPhotoRecord first = store.beginCapture(ADDRESS, WORK_A);
        assertEquals(1, first.captureSequence());
        assertNull(store.finishCaptureIfImageExists(first.id()));

        PendingPhotoRecord second = store.beginCapture(ADDRESS, WORK_A);
        assertEquals(2, second.captureSequence());
    }

    @Test
    public void explicitLocalDiscardDoesNotReuseConsumedSequence() throws Exception {
        File root = temporaryFolder.newFolder("queue-discard-gap");
        PendingPhotoStore store = store(root, ID1, ID2);
        PendingPhotoRecord first = store.beginCapture(ADDRESS, WORK_A);
        Files.write(store.imageFile(first).toPath(), new byte[] {1, 2, 3});
        PendingPhotoRecord waiting = store.finishCaptureIfImageExists(first.id());
        assertTrue(waiting != null && waiting.state() == PendingPhotoRecord.State.WAITING);
        store.discard(first.id());

        PendingPhotoRecord second = store.beginCapture(ADDRESS, WORK_A);
        assertEquals(2, second.captureSequence());
    }

    @Test
    public void missingLedgerBootstrapsAfterRetainedLegacyRecords() throws Exception {
        File root = temporaryFolder.newFolder("queue-legacy-bootstrap");
        writeLegacyRecord(root, ID1, 1_700_000_000_001L);
        writeLegacyRecord(root, ID2, 1_700_000_000_002L);

        PendingPhotoStore store = store(root, ID3);
        PendingPhotoRecord next = store.beginCapture(ADDRESS, WORK_A);

        assertEquals(3, next.captureSequence());
    }

    @Test
    public void corruptLedgerFailsClosedBeforePhotoReservation() throws Exception {
        File root = temporaryFolder.newFolder("queue-corrupt-ledger");
        Files.writeString(
                new File(root, PendingPhotoStore.CAPTURE_SEQUENCE_LEDGER_FILE).toPath(),
                "work-a=not-a-number\n",
                StandardCharsets.UTF_8);
        PendingPhotoStore store = store(root, ID1);

        try {
            store.beginCapture(ADDRESS, WORK_A);
            fail("Expected corrupt sequence ledger to block capture reservation");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("invalid value"));
        }
        assertTrue(store.scan().records().isEmpty());
    }

    @Test
    public void sequenceAbove999WidensWithoutChangingUuidIdentity() {
        PendingPhotoRecord record = PendingPhotoRecord.createCapturing(
                ID1, 1_700_000_000_000L,
                ADDRESS.id(), ADDRESS.name(), WORK_A.id(), WORK_A.name(), 1000);

        assertEquals("1000_field-photo-" + ID1 + ".jpg",
                DrivePhotoUploader.remoteFileNameFor(record));
    }

    @Test
    public void uploadStateTransitionsPreserveSequence() {
        PendingPhotoRecord uploaded = PendingPhotoRecord.createCapturing(
                ID1, 1_700_000_000_000L,
                ADDRESS.id(), ADDRESS.name(), WORK_A.id(), WORK_A.name(), 9)
                .withState(PendingPhotoRecord.State.WAITING)
                .beginUploadAttempt(1_700_000_100_000L)
                .recordProvisionalRemoteFileId("remote-9")
                .markUploadConfirmed("remote-9");

        assertEquals(9, uploaded.captureSequence());
        assertEquals("009_field-photo-" + ID1 + ".jpg",
                DrivePhotoUploader.remoteFileNameFor(uploaded));
    }

    private PendingPhotoStore store(File root, String... ids) {
        final int[] index = {0};
        final long[] time = {1_700_000_000_000L};
        return new PendingPhotoStore(
                root,
                () -> {
                    if (index[0] >= ids.length) {
                        throw new IllegalStateException("No more deterministic photo ids.");
                    }
                    return ids[index[0]++];
                },
                () -> time[0]++);
    }

    private void writeLegacyRecord(File root, String id, long createdAt) throws Exception {
        PendingPhotoRecord legacy = PendingPhotoRecord.createCapturing(
                id, createdAt,
                ADDRESS.id(), ADDRESS.name(), WORK_A.id(), WORK_A.name());
        Properties properties = legacy.toProperties();
        properties.setProperty("schemaVersion", "3");
        properties.remove("captureSequence");
        File metadata = new File(root, PendingPhotoRecord.metadataFileNameFor(id));
        try (FileOutputStream output = new FileOutputStream(metadata)) {
            properties.store(output, "legacy fixture");
            output.getFD().sync();
        }
    }
}
''')


# Version the internal test build without touching signing behavior.
path = Path('app/build.gradle')
text = path.read_text()
text = replace_once(text, '        versionCode 21\n', '        versionCode 22\n', 'version code')
text = replace_once(text,
                    "        versionName '0.16-capture-order-export'\n",
                    "        versionName '0.17-automatic-capture-order'\n",
                    'version name')
path.write_text(text)
