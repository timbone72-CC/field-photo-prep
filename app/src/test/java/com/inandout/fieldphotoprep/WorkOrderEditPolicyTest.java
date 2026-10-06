package com.inandout.fieldphotoprep;

import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public final class WorkOrderEditPolicyTest {
    @Test public void prefillPreservesNameIncludingSeparatorsAndDate() {
        assertEquals("GRASS - CUT", WorkOrderEditPolicy.name("GRASS - CUT - 2026-10-06"));
        assertEquals(LocalDate.of(2026, 10, 6), WorkOrderEditPolicy.date("GRASS - CUT - 2026-10-06"));
        assertEquals("GRASS CUT", WorkOrderEditPolicy.name("GRASS CUT"));
    }
    @Test public void correctionsAllowEarlierDateAndSameIdentity() {
        String requested = WorkOrderFolderName.build("Grass Cut", "2026-09-29");
        WorkOrderEditPolicy.validate(Collections.singletonList(
                new DriveFolder("wo", "GRASS CUT - 2026-10-06")),
                "wo", "GRASS CUT - 2026-10-06", requested);
        WorkOrderEditPolicy.validate(Collections.singletonList(new DriveFolder("wo", requested)),
                "wo", requested, requested);
    }
    @Test public void collisionNeverChoosesDifferentFolder() {
        assertThrows(IllegalArgumentException.class, () -> WorkOrderEditPolicy.validate(
                Arrays.asList(new DriveFolder("wo", "old"), new DriveFolder("other", "requested")),
                "wo", "old", "requested"));
    }
    @Test public void staleOrMissingSelectionStopsCorrection() {
        assertThrows(IllegalArgumentException.class, () -> WorkOrderEditPolicy.validate(
                Collections.singletonList(new DriveFolder("wo", "changed")), "wo", "old", "new"));
        assertThrows(IllegalArgumentException.class, () -> WorkOrderEditPolicy.validate(
                Collections.emptyList(), "wo", "old", "new"));
    }
}
