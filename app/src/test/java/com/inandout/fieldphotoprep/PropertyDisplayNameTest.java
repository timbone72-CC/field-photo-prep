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
}
