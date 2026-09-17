from pathlib import Path

path = Path('app/src/main/java/com/inandout/fieldphotoprep/PendingPhotoRecord.java')
text = path.read_text()
old = '''    public static PendingPhotoRecord createCapturing(
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
new = '''    public static PendingPhotoRecord createCapturing(
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
                workOrderName,
                0,
                0L,
                null,
                null,
                null);
    }
'''
if old not in text:
    raise SystemExit('capture constructor staging block not found')
path.write_text(text.replace(old, new, 1))
