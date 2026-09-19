package com.inandout.fieldphotoprep;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class CompanyFolderNameTest {
    @Test
    public void trimsOnlyOuterWhitespace() {
        assertEquals("Tresmolino Jobs", CompanyFolderName.build("  Tresmolino Jobs  "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankCompanyName() {
        CompanyFolderName.build("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullCompanyName() {
        CompanyFolderName.build(null);
    }
}
