package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NextActionGuideTest {
    @Test
    public void homeGuidanceUsesExistingConnectionAndSelectionState() {
        assertAction(
                NextActionGuide.home(false, false, 0, false),
                NextActionGuide.Kind.CONNECT_DRIVE,
                "Next: Connect Google Drive",
                true);
        assertAction(
                NextActionGuide.home(false, true, 0, false),
                NextActionGuide.Kind.ADD_PROPERTY,
                "Next: Add a Property",
                true);
        assertAction(
                NextActionGuide.home(false, true, 3, false),
                NextActionGuide.Kind.CHOOSE_PROPERTY,
                "Next: Choose a Property",
                false);
        assertAction(
                NextActionGuide.home(false, true, 3, true),
                NextActionGuide.Kind.OPEN_WORK_ORDERS,
                "Next: Open Work Orders",
                true);
    }

    @Test
    public void workOrderGuidanceOnlyAdvancesAfterExactSelection() {
        assertAction(
                NextActionGuide.workOrders(false, false),
                NextActionGuide.Kind.CHOOSE_OR_ADD_WORK_ORDER,
                "Next: Choose or Add Work Order",
                false);
        assertAction(
                NextActionGuide.workOrders(false, true),
                NextActionGuide.Kind.TAKE_PHOTOS,
                "Next: Take Photos",
                true);
    }

    @Test
    public void photoGuidancePrioritizesSafetyAndRoutineProgression() {
        assertAction(
                NextActionGuide.photos(false, false, false, 0, 0, 0, 0, 0, false, false),
                NextActionGuide.Kind.TAKE_PHOTOS,
                "Next: Take Photos",
                true);

        assertAction(
                NextActionGuide.photos(true, false, false, 5, 0, 1, 4, 0, false, false),
                NextActionGuide.Kind.PREPARING,
                "Preparing Photo…",
                false);

        assertAction(
                NextActionGuide.photos(false, true, true, 5, 2, 0, 3, 0, false, false),
                NextActionGuide.Kind.CHECKING_UPLOADS,
                "Checking Uploads…",
                false);

        assertAction(
                NextActionGuide.photos(false, false, false, 5, 2, 0, 3, 0, false, false),
                NextActionGuide.Kind.CHECK_UPLOADS,
                "Next: Check 2 Uploads",
                true);

        assertAction(
                NextActionGuide.photos(false, false, false, 5, 0, 1, 4, 0, false, false),
                NextActionGuide.Kind.PREPARE_PHOTO,
                "Next: Prepare Photo",
                true);

        assertAction(
                NextActionGuide.photos(false, false, false, 5, 0, 0, 4, 0, false, false),
                NextActionGuide.Kind.SELECT_READY,
                "Next: Select 4 Ready Photos",
                true);

        assertAction(
                NextActionGuide.photos(false, false, false, 5, 0, 0, 4, 3, true, false),
                NextActionGuide.Kind.UPLOAD_SELECTED,
                "Next: Upload Selected (3)",
                true);

        assertAction(
                NextActionGuide.photos(false, false, false, 5, 0, 0, 0, 0, false, true),
                NextActionGuide.Kind.DONE,
                "Done — Return to Work Orders",
                true);
    }

    @Test
    public void busyGuidanceNeverInventsASecondActionPath() {
        NextActionGuide.Action home = NextActionGuide.home(true, true, 10, true);
        assertFalse(home.enabled());
        assertEquals("Loading…", home.label());

        NextActionGuide.Action work = NextActionGuide.workOrders(true, true);
        assertFalse(work.enabled());
        assertEquals("Working…", work.label());

        NextActionGuide.Action upload = NextActionGuide.photos(
                false, true, false, 10, 0, 0, 10, 10, true, false);
        assertFalse(upload.enabled());
        assertEquals(NextActionGuide.Kind.UPLOADING, upload.kind());
        assertEquals("Uploading…", upload.label());
    }

    private static void assertAction(
            NextActionGuide.Action action,
            NextActionGuide.Kind kind,
            String label,
            boolean enabled) {
        assertEquals(kind, action.kind());
        assertEquals(label, action.label());
        if (enabled) {
            assertTrue(action.enabled());
        } else {
            assertFalse(action.enabled());
        }
    }
}
