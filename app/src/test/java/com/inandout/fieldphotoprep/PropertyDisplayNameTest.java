package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PropertyDisplayNameTest {
    @Test
    public void underscoresBecomeSpacesWithoutChangingAddressMeaning() {
        assertEquals(
                "101 CHUCKER LN ELK CITY OK",
                PropertyDisplayName.fromDriveFolderName("101_CHUCKER_LN_ELK_CITY_OK"));
    }

    @Test
    public void repeatedWhitespaceCollapsesForDisplayOnly() {
        assertEquals(
                "120 S BROADWAY ST SAYRE OK 73662",
                PropertyDisplayName.fromDriveFolderName(" 120__S_BROADWAY_ST__SAYRE_OK_73662 "));
    }

    @Test
    public void nullIsSafeForDisplay() {
        assertEquals("", PropertyDisplayName.fromDriveFolderName(null));
    }
    @Test
    public void reportedTaskSuffixIsRemovedOnlyFromPropertyPresentation() {
        String raw = "1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST";
        assertEquals("1607 CRESTVIEW DR CORDELL", PropertyDisplayName.fromDriveFolderName(raw));
        assertEquals("1607 CRESTVIEW DR CORDELL PRESSURE TEST",
                PropertyDisplayName.readableFolderName(raw));
        assertEquals("1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST", raw);
        assertEquals("Pressure Test - 2026-09-13",
                PropertyDisplayName.readableFolderName("Pressure_Test - 2026-09-13"));
    }

    @Test
    public void addressComponentsAndUnrecognizedSuffixesAreNotGuessedAway() {
        assertEquals("150 BLUESTEM RD WEATHERFORD RD",
                PropertyDisplayName.fromDriveFolderName("150_BLUESTEM_RD_WEATHERFORD_RD"));
        assertEquals("12 PRESSURE TEST RD",
                PropertyDisplayName.fromDriveFolderName("12_PRESSURE_TEST_RD"));
        assertEquals("PRESSURE TEST", PropertyDisplayName.fromDriveFolderName("PRESSURE_TEST"));
    }
}
