package com.inandout.fieldphotoprep;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;

/**
 * Read-only thumbnail decoder for the field UI.
 *
 * This never rewrites, deletes, moves, or otherwise mutates the protected original or prepared
 * upload copy. It decodes a small in-memory preview and applies EXIF orientation for display only.
 */
final class PhotoThumbnailLoader {
    private PhotoThumbnailLoader() {}

    static Bitmap load(File image, int targetPixels) throws IOException {
        if (image == null || !image.isFile() || image.length() <= 0 || targetPixels <= 0) {
            return null;
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(image.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        int sample = 1;
        int shortEdge = Math.min(bounds.outWidth, bounds.outHeight);
        while (shortEdge / (sample * 2) >= targetPixels * 2) {
            sample *= 2;
        }

        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = sample;
        decode.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap source = BitmapFactory.decodeFile(image.getAbsolutePath(), decode);
        if (source == null) {
            return null;
        }

        Bitmap oriented = source;
        try {
            ExifInterface exif = new ExifInterface(image);
            int rotation = exif.getRotationDegrees();
            boolean flipped = exif.isFlipped();
            if (rotation != 0 || flipped) {
                Matrix matrix = new Matrix();
                if (flipped) {
                    matrix.postScale(-1f, 1f);
                }
                if (rotation != 0) {
                    matrix.postRotate(rotation);
                }
                oriented = Bitmap.createBitmap(
                        source,
                        0,
                        0,
                        source.getWidth(),
                        source.getHeight(),
                        matrix,
                        true);
                if (oriented != source) {
                    source.recycle();
                }
            }
        } catch (IOException ignored) {
            // A decodable image without readable EXIF is still useful as a thumbnail.
        }

        return oriented;
    }
}
