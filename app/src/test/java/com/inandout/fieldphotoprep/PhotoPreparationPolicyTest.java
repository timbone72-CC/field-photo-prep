package com.inandout.fieldphotoprep;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public final class PhotoPreparationPolicyTest {
    @Test
    public void smallerPhotoIsNotUpscaled() {
        PhotoPreparationPolicy.Dimensions dimensions =
                PhotoPreparationPolicy.targetDimensions(1600, 1200);

        assertEquals(1600, dimensions.width());
        assertEquals(1200, dimensions.height());
    }

    @Test
    public void largeLandscapePhotoKeepsAspectRatio() {
        PhotoPreparationPolicy.Dimensions dimensions =
                PhotoPreparationPolicy.targetDimensions(4000, 3000);

        assertEquals(2048, dimensions.width());
        assertEquals(1536, dimensions.height());
    }

    @Test
    public void largePortraitPhotoKeepsAspectRatio() {
        PhotoPreparationPolicy.Dimensions dimensions =
                PhotoPreparationPolicy.targetDimensions(3000, 4000);

        assertEquals(1536, dimensions.width());
        assertEquals(2048, dimensions.height());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidDimensionsAreRejected() {
        PhotoPreparationPolicy.targetDimensions(0, 1200);
    }

    @Test
    public void preparedFilenameIsDeterministicFromPhotoIdentity() {
        String id = UUID.randomUUID().toString();

        assertEquals("prepared-" + id + ".jpg", PhotoPreparationPolicy.preparedFileNameFor(id));
        assertEquals("prepared-" + id + ".jpg", PhotoPreparationPolicy.preparedFileNameFor(id));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidPhotoIdentityCannotBecomePreparedPath() {
        PhotoPreparationPolicy.preparedFileNameFor("../outside");
    }
}
