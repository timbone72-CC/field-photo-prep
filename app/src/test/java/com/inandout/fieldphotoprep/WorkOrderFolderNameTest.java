package com.inandout.fieldphotoprep;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

    @Test
    public void olderSameWorkOrderIsEligibleForEmptyReuse() {
        assertTrue(WorkOrderFolderName.isOlderSameWorkOrderFolder(
                "Cut Grass - 2026-09-06", "Cut Grass", "2026-09-13"));
    }

    @Test
    public void sameOrNewerDateIsNotEligibleForOldFolderReuse() {
        assertFalse(WorkOrderFolderName.isOlderSameWorkOrderFolder(
                "Cut Grass - 2026-09-13", "Cut Grass", "2026-09-13"));
        assertFalse(WorkOrderFolderName.isOlderSameWorkOrderFolder(
                "Cut Grass - 2026-09-20", "Cut Grass", "2026-09-13"));
    }

    @Test
    public void differentOrCaseChangedWorkOrderIsNotGuessedAsReuseCandidate() {
        assertFalse(WorkOrderFolderName.isOlderSameWorkOrderFolder(
                "Remove Trash - 2026-09-06", "Cut Grass", "2026-09-13"));
        assertFalse(WorkOrderFolderName.isOlderSameWorkOrderFolder(
                "cut grass - 2026-09-06", "Cut Grass", "2026-09-13"));
    }

    @Test
    public void malformedExistingFolderNameIsNotEligibleForReuse() {
        assertFalse(WorkOrderFolderName.isOlderSameWorkOrderFolder(
                "Cut Grass", "Cut Grass", "2026-09-13"));
        assertFalse(WorkOrderFolderName.isOlderSameWorkOrderFolder(
                "Cut Grass - not-a-date", "Cut Grass", "2026-09-13"));
    }

    @Test
    public void workOrderTextMayContainSeparatorWithoutBreakingReuseParsing() {
        assertTrue(WorkOrderFolderName.isOlderSameWorkOrderFolder(
                "Debris - Trash - 2026-09-06", "Debris - Trash", "2026-09-13"));
    }
}
