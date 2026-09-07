package com.inandout.fieldphotoprep;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class WorkOrderFolderNameTest {
    @Test
    public void buildsApprovedWorkOrderDateName() {
        assertEquals(
                "Cut Grass - 2026-09-06",
                WorkOrderFolderName.build("  Cut Grass  ", "2026-09-06"));
    }

    @Test
    public void rejectsBlankWorkOrderName() {
        try {
            WorkOrderFolderName.build("   ", "2026-09-06");
            fail("Expected blank work-order name to fail");
        } catch (IllegalArgumentException expected) {
            assertEquals("Enter a work-order name.", expected.getMessage());
        }
    }

    @Test
    public void rejectsInvalidDate() {
        try {
            WorkOrderFolderName.build("Cut Grass", "2026-02-30");
            fail("Expected invalid date to fail");
        } catch (IllegalArgumentException expected) {
            assertEquals("Choose a valid work-order date.", expected.getMessage());
        }
    }
}
