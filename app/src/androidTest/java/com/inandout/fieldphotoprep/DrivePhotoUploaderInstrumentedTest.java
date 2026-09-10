package com.inandout.fieldphotoprep;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public final class DrivePhotoUploaderInstrumentedTest {
    @Test
    public void preparedBytesStreamThroughPipeBackedDescriptorWithoutDiskFsyncAssumption()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File source = File.createTempFile("phase6b-pipe-", ".jpg", context.getCacheDir());
        byte[] expected = new byte[4096];
        for (int index = 0; index < expected.length; index++) {
            expected[index] = (byte) (index % 251);
        }
        try (FileOutputStream output = new FileOutputStream(source)) {
            output.write(expected);
        }

        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];

        long written = DrivePhotoUploader.writePreparedToDescriptor(writeSide, source);

        ByteArrayOutputStream received = new ByteArrayOutputStream();
        try (ParcelFileDescriptor.AutoCloseInputStream input =
                     new ParcelFileDescriptor.AutoCloseInputStream(readSide)) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                received.write(buffer, 0, count);
            }
        } finally {
            source.delete();
        }

        assertEquals(expected.length, written);
        assertArrayEquals(expected, received.toByteArray());
    }
}
