package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public final class DriveOptionsPolicyTest {
    @Test
    public void unsafeBindingExposesOnlyAccountRecovery() {
        assertArrayEquals(
                new CharSequence[]{"Account"},
                DriveOptionsPolicy.items(false, true, true));
    }

    @Test
    public void usableWorkspaceWithCompanyKeepsNormalCompanyActions() {
        assertArrayEquals(
                new CharSequence[]{"Add Company", "Edit Company", "Change Workspace", "Account"},
                DriveOptionsPolicy.items(true, true, true));
    }

    @Test
    public void usableWorkspaceWithoutCompanyKeepsChooserAndAddActions() {
        assertArrayEquals(
                new CharSequence[]{"Choose Company", "Add Company", "Change Workspace", "Account"},
                DriveOptionsPolicy.items(true, true, false));
    }

    @Test
    public void usableLegacySingleCompanyStateKeepsSetupAndAccountActions() {
        assertArrayEquals(
                new CharSequence[]{"Set Up Companies", "Account"},
                DriveOptionsPolicy.items(true, false, false));
    }
}
