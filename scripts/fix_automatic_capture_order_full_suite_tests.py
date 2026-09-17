from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"anchor not found: {label}")
    return text.replace(old, new, 1)

# Schema 2 upgrade test: current writes now intentionally use schema 4.
path = Path('app/src/test/java/com/inandout/fieldphotoprep/PendingPhotoProvisionalIdentityTest.java')
text = path.read_text()
text = replace_once(
    text,
    'public void schema2UploadingRecordLoadsWithNullProvisionalAndNextWriteUsesSchema3()\n',
    'public void schema2UploadingRecordLoadsWithNullProvisionalAndNextWriteUsesCurrentSchema()\n',
    'provisional test name')
text = replace_once(
    text,
    '        assertEquals("3", persisted.getProperty("schemaVersion"));\n',
    '        assertEquals("4", persisted.getProperty("schemaVersion"));\n',
    'provisional schema expectation')
path.write_text(text)

# Legacy queue tests: rewrites advance to the current schema 4 while preserving legacy semantics.
path = Path('app/src/test/java/com/inandout/fieldphotoprep/PendingPhotoQueueStateTest.java')
text = path.read_text()
text = replace_once(
    text,
    'public void legacyWaitingRecordLoadsWithSafeDefaultsAndNextWriteUsesSchema3() throws Exception {\n',
    'public void legacyWaitingRecordLoadsWithSafeDefaultsAndNextWriteUsesCurrentSchema() throws Exception {\n',
    'legacy queue test name')
text = replace_once(
    text,
    '        assertEquals("3", persisted.getProperty("schemaVersion"));\n',
    '        assertEquals("4", persisted.getProperty("schemaVersion"));\n',
    'legacy waiting schema expectation')
text = replace_once(
    text,
    '        assertEquals("3", persisted.getProperty("schemaVersion"));\n',
    '        assertEquals("4", persisted.getProperty("schemaVersion"));\n',
    'legacy capture schema expectation')
path.write_text(text)

# Coordinator test: a newly captured record is sequenced, so reconciliation must use its exact record-based name.
path = Path('app/src/test/java/com/inandout/fieldphotoprep/PhotoUploadReconciliationCoordinatorTest.java')
text = path.read_text()
old = '''        Fixture fixture = fixture(false);
        File prepared = fixture.preparer.preparedFile(PHOTO_ID);
        ReconcileProvider reconcileProvider = new ReconcileProvider();
        DrivePhotoReconciler.RemoteDocument candidate = new DrivePhotoReconciler.RemoteDocument(
                REMOTE_ID,
                DrivePhotoUploader.remoteFileNameFor(PHOTO_ID),
                DrivePhotoUploader.JPEG_MIME_TYPE,
                prepared.length());
'''
new = '''        Fixture fixture = fixture(false);
        File prepared = fixture.preparer.preparedFile(PHOTO_ID);
        PendingPhotoRecord localRecord = fixture.store.getById(PHOTO_ID);
        assertEquals(1, localRecord.captureSequence());
        ReconcileProvider reconcileProvider = new ReconcileProvider();
        DrivePhotoReconciler.RemoteDocument candidate = new DrivePhotoReconciler.RemoteDocument(
                REMOTE_ID,
                DrivePhotoUploader.remoteFileNameFor(localRecord),
                DrivePhotoUploader.JPEG_MIME_TYPE,
                prepared.length());
'''
text = replace_once(text, old, new, 'coordinator sequenced candidate')
path.write_text(text)
