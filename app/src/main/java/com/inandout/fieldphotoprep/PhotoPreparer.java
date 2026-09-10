package com.inandout.fieldphotoprep;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class PhotoPreparer {
    private final File root;

    public PhotoPreparer(File root) {
        this.root = root;
    }

    public PreparedPhotoResult prepare(
            PendingPhotoStore pendingStore,
            PendingPhotoRecord record) throws IOException {
        if (pendingStore == null || record == null) {
            throw new IOException("A protected waiting photo is required for preparation.");
        }
        if (record.state() != PendingPhotoRecord.State.WAITING) {
            throw new IOException("Only a protected waiting photo can be prepared.");
        }

        File original = pendingStore.imageFile(record);
        if (!original.isFile() || original.length() <= 0) {
            throw new IOException("The protected original is missing or empty. Nothing was prepared.");
        }
        long originalBytes = original.length();

        ensureRoot();
        File target = preparedFile(record.id());
        File temp = new File(root, target.getName() + ".tmp-" + UUID.randomUUID());
        ensureDirectChild(temp);

        Bitmap decoded = null;
        Bitmap oriented = null;
        Bitmap scaled = null;
        try {
            decoded = decodeForPreparation(original);
            ExifInterface exif = new ExifInterface(original);
            oriented = applyOrientation(decoded, exif);

            PhotoPreparationPolicy.Dimensions targetDimensions =
                    PhotoPreparationPolicy.targetDimensions(oriented.getWidth(), oriented.getHeight());
            if (targetDimensions.width() == oriented.getWidth()
                    && targetDimensions.height() == oriented.getHeight()) {
                scaled = oriented;
            } else {
                scaled = Bitmap.createScaledBitmap(
                        oriented,
                        targetDimensions.width(),
                        targetDimensions.height(),
                        true);
                if (scaled == null) {
                    throw new IOException("Could not resize the protected photo for upload.");
                }
            }

            writePreparedTemp(scaled, temp);
            verifyPreparedImage(temp, targetDimensions.width(), targetDimensions.height());
            replacePreparedFile(temp, target);

            long preparedBytes = target.length();
            if (preparedBytes <= 0) {
                throw new IOException("Prepared photo was not safely written.");
            }
            return new PreparedPhotoResult(
                    record.id(),
                    target,
                    targetDimensions.width(),
                    targetDimensions.height(),
                    originalBytes,
                    preparedBytes);
        } catch (OutOfMemoryError error) {
            throw new IOException("Not enough memory to prepare this photo. The protected original was kept.", error);
        } finally {
            if (temp.exists()) {
                temp.delete();
            }
            recycleDistinctBitmaps(decoded, oriented, scaled);
        }
    }

    public File preparedFile(String photoId) throws IOException {
        ensureRoot();
        File file = new File(root, PhotoPreparationPolicy.preparedFileNameFor(photoId));
        ensureDirectChild(file);
        return file;
    }

    private Bitmap decodeForPreparation(File original) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(original.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("The protected original is not a decodable image.");
        }

        int inSampleSize = 1;
        int longEdge = Math.max(bounds.outWidth, bounds.outHeight);
        while (longEdge / (inSampleSize * 2) >= PhotoPreparationPolicy.MAX_LONG_EDGE) {
            inSampleSize *= 2;
        }

        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = inSampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(original.getAbsolutePath(), decode);
        if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            throw new IOException("The protected original could not be decoded.");
        }
        return bitmap;
    }

    private Bitmap applyOrientation(Bitmap source, ExifInterface exif) {
        int rotationDegrees = exif.getRotationDegrees();
        boolean flipped = exif.isFlipped();
        if (rotationDegrees == 0 && !flipped) {
            return source;
        }

        Matrix matrix = new Matrix();
        if (flipped) {
            matrix.postScale(-1f, 1f);
        }
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees);
        }
        return Bitmap.createBitmap(
                source,
                0,
                0,
                source.getWidth(),
                source.getHeight(),
                matrix,
                true);
    }

    private void writePreparedTemp(Bitmap bitmap, File temp) throws IOException {
        try (FileOutputStream output = new FileOutputStream(temp)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, PhotoPreparationPolicy.JPEG_QUALITY, output)) {
                throw new IOException("JPEG preparation did not complete.");
            }
            output.flush();
            output.getFD().sync();
        }
        if (!temp.isFile() || temp.length() <= 0) {
            throw new IOException("Prepared JPEG is empty.");
        }
    }

    private void verifyPreparedImage(File temp, int expectedWidth, int expectedHeight) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(temp.getAbsolutePath(), bounds);
        if (bounds.outWidth != expectedWidth || bounds.outHeight != expectedHeight) {
            throw new IOException("Prepared JPEG dimensions could not be verified.");
        }
    }

    private void replacePreparedFile(File temp, File target) throws IOException {
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
        } catch (IOException error) {
            throw new IOException("Could not safely finish the prepared photo. The protected original was kept.", error);
        }
    }

    private void ensureRoot() throws IOException {
        if (root == null) {
            throw new IOException("Prepared-photo storage is unavailable.");
        }
        if (!root.exists() && !root.mkdirs()) {
            throw new IOException("Could not create prepared-photo storage.");
        }
        if (!root.isDirectory()) {
            throw new IOException("Prepared-photo storage is not a directory.");
        }
    }

    private void ensureDirectChild(File file) throws IOException {
        File canonicalRoot = root.getCanonicalFile();
        File canonicalParent = file.getCanonicalFile().getParentFile();
        if (!canonicalRoot.equals(canonicalParent)) {
            throw new IOException("Prepared-photo path escaped temporary storage.");
        }
    }

    private static void recycleDistinctBitmaps(Bitmap decoded, Bitmap oriented, Bitmap scaled) {
        if (scaled != null && scaled != oriented && scaled != decoded && !scaled.isRecycled()) {
            scaled.recycle();
        }
        if (oriented != null && oriented != decoded && !oriented.isRecycled()) {
            oriented.recycle();
        }
        if (decoded != null && !decoded.isRecycled()) {
            decoded.recycle();
        }
    }
}
