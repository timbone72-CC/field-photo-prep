package com.inandout.fieldphotoprep;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class SharedDrivePhotoCountLabelTest {
    @Test public void zeroOnlyWhenProviderExplicitlyVerifiedZero() {
        assertEquals("In Drive: 0", SharedDrivePhotoCountLabel.format(0, false));
        assertEquals("In Drive: 152", SharedDrivePhotoCountLabel.format(152, false));
    }

    @Test public void unknownAndErrorsNeverLookLikeZero() {
        assertEquals("In Drive: —", SharedDrivePhotoCountLabel.format(null, false));
        assertEquals("In Drive: unavailable", SharedDrivePhotoCountLabel.format(null, true));
        assertEquals("In Drive: —", SharedDrivePhotoCountLabel.format(-1, false));
    }
}
