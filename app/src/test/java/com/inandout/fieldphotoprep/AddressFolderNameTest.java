package com.inandout.fieldphotoprep;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class AddressFolderNameTest {
    @Test
    public void trimsOnlyOuterWhitespace() {
        assertEquals(
                "1607 Crestview Drive",
                AddressFolderName.build("  1607 Crestview Drive  "));
    }

    @Test
    public void preservesOperatorFormattingInsideName() {
        assertEquals(
                "1607_CRESTVIEW_DR_CORDELL_OK",
                AddressFolderName.build("1607_CRESTVIEW_DR_CORDELL_OK"));
        assertEquals(
                "1607  Crestview Drive",
                AddressFolderName.build("1607  Crestview Drive"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankName() {
        AddressFolderName.build("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullName() {
        AddressFolderName.build(null);
    }
}
