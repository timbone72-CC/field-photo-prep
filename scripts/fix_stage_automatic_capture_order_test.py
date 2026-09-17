from pathlib import Path

path = Path('app/src/test/java/com/inandout/fieldphotoprep/AutomaticCaptureOrderFilenameTest.java')
text = path.read_text()
old = '''        Files.writeString(
                new File(root, PendingPhotoStore.CAPTURE_SEQUENCE_LEDGER_FILE).toPath(),
                "work-a=not-a-number\\n",
                StandardCharsets.UTF_8);
'''
new = '''        Files.write(
                new File(root, PendingPhotoStore.CAPTURE_SEQUENCE_LEDGER_FILE).toPath(),
                "work-a=not-a-number\\n".getBytes(StandardCharsets.UTF_8));
'''
if old not in text:
    raise SystemExit('ledger corrupt-fixture write anchor not found')
path.write_text(text.replace(old, new, 1))
