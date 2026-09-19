package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class FolderPrefsParentBindingInstrumentedTest {
    private static final String PREFS = "field_photo_prep";

    @Test
    public void savedWorkOrderRequiresExactCurrentAddressBinding() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        try {
            FolderPrefs prefs = new FolderPrefs(context);
            prefs.setMasterFolder(
                    Uri.parse("content://com.example.documents/tree/master"),
                    new DriveFolder("master", "HNP Jobs"));

            DriveFolder firstAddress = new DriveFolder("address-1", "1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST");
            DriveFolder workOrder = new DriveFolder("work-1", "GRASS CUT");
            prefs.setCurrentAddress(firstAddress);
            prefs.setCurrentWorkOrder(workOrder);

            assertEquals("work-1", prefs.getCurrentWorkOrder().id());

            prefs.setCurrentAddress(new DriveFolder("address-2", "1611_NW_SMITH_AVE"));
            assertNull(prefs.getCurrentWorkOrder());
        } finally {
            raw.edit().clear().commit();
        }
    }

    @Test
    public void workspaceCompanySwitchClearsNavigationButPreservesLegacyRollbackState() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        try {
            FolderPrefs prefs = new FolderPrefs(context);
            Uri legacyUri = Uri.parse("content://com.example.documents/tree/hnp");
            prefs.setMasterFolder(legacyUri, new DriveFolder("company-hnp", "HNP Jobs"));

            prefs.setWorkspaceFolder(
                    Uri.parse("content://com.example.documents/tree/photos"),
                    new DriveFolder("workspace", "Photos"));

            assertTrue(prefs.hasWorkspace());
            assertEquals("company-hnp", prefs.getLegacyMasterFolder().id());
            assertEquals(legacyUri, prefs.getLegacyMasterTreeUri());
            assertNull(prefs.getMasterFolder());

            prefs.setCurrentCompany(new DriveFolder("company-hnp", "HNP Jobs"));
            prefs.setCurrentAddress(new DriveFolder("address-hnp", "1607 Crestview Drive"));
            prefs.setCurrentWorkOrder(new DriveFolder("work-hnp", "Grass Cut - 2026-09-18"));

            assertEquals("company-hnp", prefs.getMasterFolder().id());
            assertEquals("address-hnp", prefs.getCurrentAddress().id());
            assertEquals("work-hnp", prefs.getCurrentWorkOrder().id());

            prefs.setCurrentCompany(new DriveFolder("company-tres", "Tresmolino Jobs"));

            assertEquals("company-tres", prefs.getMasterFolder().id());
            assertNull(prefs.getCurrentAddress());
            assertNull(prefs.getCurrentWorkOrder());
            assertEquals("company-hnp", prefs.getLegacyMasterFolder().id());
        } finally {
            raw.edit().clear().commit();
        }
    }

    @Test
    public void companyRenameBySameIdentityKeepsAddressAndWorkOrderBinding() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        try {
            FolderPrefs prefs = new FolderPrefs(context);
            prefs.setWorkspaceFolder(
                    Uri.parse("content://com.example.documents/tree/photos"),
                    new DriveFolder("workspace", "Photos"));
            prefs.setCurrentCompany(new DriveFolder("company-1", "Tresmolino Jobs"));
            prefs.setCurrentAddress(new DriveFolder("address-1", "213 E 9TH ST VICI OK"));
            prefs.setCurrentWorkOrder(new DriveFolder("work-1", "Initial Secure - 2026-09-21"));

            prefs.setCurrentCompany(new DriveFolder("company-1", "Tresmolino"));

            assertEquals("Tresmolino", prefs.getCurrentCompany().name());
            assertEquals("address-1", prefs.getCurrentAddress().id());
            assertEquals("work-1", prefs.getCurrentWorkOrder().id());
        } finally {
            raw.edit().clear().commit();
        }
    }

    @Test
    public void legacyUnboundOrSelfChildWorkOrderIsNotRestored() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        try {
            raw.edit()
                    .putString("current_address_folder_id", "address-1")
                    .putString("current_address_folder_name", "1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST")
                    .putString("current_work_order_folder_id", "legacy-work")
                    .putString("current_work_order_folder_name", "GRASS CUT")
                    .commit();

            FolderPrefs prefs = new FolderPrefs(context);
            assertNull(prefs.getCurrentWorkOrder());

            prefs.setCurrentAddress(new DriveFolder("address-1", "1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST"));
            prefs.setCurrentWorkOrder(new DriveFolder("address-1", "1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST"));
            assertNull(prefs.getCurrentWorkOrder());
        } finally {
            raw.edit().clear().commit();
        }
    }
}
