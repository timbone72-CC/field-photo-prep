package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.File;

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
    public void companySwitchDoesNotRewriteQueuedPhotoDestination() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        File root = new File(context.getCacheDir(), "company-switch-queued-photo");
        deleteRecursively(root);
        raw.edit().clear().commit();
        try {
            FolderPrefs prefs = new FolderPrefs(context);
            prefs.setWorkspaceFolder(
                    Uri.parse("content://com.example.documents/tree/photos"),
                    new DriveFolder("workspace", "Photos"));
            prefs.setCurrentCompany(new DriveFolder("company-hnp", "HNP Jobs"));

            DriveFolder address = new DriveFolder("address-hnp", "1607 Crestview Drive");
            DriveFolder workOrder = new DriveFolder("work-hnp", "Grass Cut - 2026-09-18");
            prefs.setCurrentAddress(address);
            prefs.setCurrentWorkOrder(workOrder);

            PendingPhotoStore store = new PendingPhotoStore(
                    root,
                    () -> "11111111-1111-1111-1111-111111111111",
                    () -> 1_000L);
            PendingPhotoRecord before = store.beginCapture(address, workOrder);

            prefs.setCurrentCompany(new DriveFolder("company-tres", "Tresmolino Jobs"));

            PendingPhotoRecord after = store.getById(before.id());
            assertEquals("address-hnp", after.addressId());
            assertEquals("work-hnp", after.workOrderId());
            assertEquals("Grass Cut - 2026-09-18", after.workOrderName());
            assertEquals(PendingPhotoRecord.State.CAPTURING, after.state());
            assertNull(prefs.getCurrentAddress());
            assertNull(prefs.getCurrentWorkOrder());
        } finally {
            raw.edit().clear().commit();
            deleteRecursively(root);
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
    @Test
    public void sameProviderRootConfirmationAddsOrganizationBindingWithoutClearingNavigation() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        try {
            FolderPrefs prefs = new FolderPrefs(context);
            Uri tree = Uri.parse("content://com.example.documents/tree/photos");
            prefs.setWorkspaceFolder(tree, new DriveFolder("workspace", "Photos"));
            prefs.setCurrentCompany(new DriveFolder("company-hnp", "HNP Jobs"));
            prefs.setCurrentAddress(new DriveFolder("address-hnp", "1607 Crestview Drive"));
            prefs.setCurrentWorkOrder(new DriveFolder("work-hnp", "Grass Cut - 2026-09-26"));

            assertEquals(0, prefs.getDriveBinding().version());
            assertTrue(prefs.bindSelectedDriveRoot(
                    tree,
                    new DriveFolder("workspace", "Photos"),
                    "org-1"));

            FolderPrefs.DriveBinding binding = prefs.getDriveBinding();
            assertEquals("org-1", binding.organizationId());
            assertEquals(FolderPrefs.ORGANIZATION_DRIVE_BINDING_VERSION, binding.version());
            assertEquals("workspace", binding.rootFolder().id());
            assertEquals("company-hnp", prefs.getCurrentCompany().id());
            assertEquals("address-hnp", prefs.getCurrentAddress().id());
            assertEquals("work-hnp", prefs.getCurrentWorkOrder().id());
        } finally {
            raw.edit().clear().commit();
        }
    }

    @Test
    public void differentProviderRootClearsNavigationButDoesNotRewriteQueuedDestination()
            throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        File root = new File(context.getCacheDir(), "org-binding-queued-photo");
        deleteRecursively(root);
        raw.edit().clear().commit();
        try {
            FolderPrefs prefs = new FolderPrefs(context);
            prefs.setWorkspaceFolder(
                    Uri.parse("content://com.example.documents/tree/photos-old"),
                    new DriveFolder("workspace-old", "Photos"));
            prefs.setCurrentCompany(new DriveFolder("company-hnp", "HNP Jobs"));
            DriveFolder address = new DriveFolder("address-hnp", "1607 Crestview Drive");
            DriveFolder workOrder = new DriveFolder("work-hnp", "Grass Cut - 2026-09-26");
            prefs.setCurrentAddress(address);
            prefs.setCurrentWorkOrder(workOrder);

            PendingPhotoStore store = new PendingPhotoStore(
                    root,
                    () -> "22222222-2222-4222-8222-222222222222",
                    () -> 2_000L);
            PendingPhotoRecord before = store.beginCapture(address, workOrder);

            assertFalse(prefs.bindSelectedDriveRoot(
                    Uri.parse("content://com.example.documents/tree/photos-new"),
                    new DriveFolder("workspace-new", "Photos"),
                    "org-1"));

            FolderPrefs.DriveBinding binding = prefs.getDriveBinding();
            PendingPhotoRecord after = store.getById(before.id());
            assertEquals("org-1", binding.organizationId());
            assertEquals("workspace-new", binding.rootFolder().id());
            assertNull(prefs.getCurrentCompany());
            assertNull(prefs.getCurrentAddress());
            assertNull(prefs.getCurrentWorkOrder());
            assertEquals("address-hnp", after.addressId());
            assertEquals("work-hnp", after.workOrderId());
            assertEquals(PendingPhotoRecord.State.CAPTURING, after.state());
        } finally {
            raw.edit().clear().commit();
            deleteRecursively(root);
        }
    }

    @Test
    public void replacingWorkspaceWithoutOrganizationConfirmationClearsOldBindingTag() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        try {
            FolderPrefs prefs = new FolderPrefs(context);
            Uri first = Uri.parse("content://com.example.documents/tree/photos-1");
            prefs.setWorkspaceFolder(first, new DriveFolder("workspace-1", "Photos"));
            prefs.bindSelectedDriveRoot(first, new DriveFolder("workspace-1", "Photos"), "org-1");
            assertEquals("org-1", prefs.getDriveBinding().organizationId());

            prefs.setWorkspaceFolder(
                    Uri.parse("content://com.example.documents/tree/photos-2"),
                    new DriveFolder("workspace-2", "Other Photos"));

            assertNull(prefs.getDriveBinding().organizationId());
            assertEquals(0, prefs.getDriveBinding().version());
        } finally {
            raw.edit().clear().commit();
        }
    }

    @Test
    public void partialWorkspaceStateDoesNotCombineWithLegacyProviderIdentity() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        try {
            raw.edit()
                    .putString("master_tree_uri", "content://com.example.documents/tree/legacy")
                    .putString("master_folder_id", "legacy-root")
                    .putString("master_folder_name", "HNP Jobs")
                    .putString("workspace_folder_id", "workspace-root")
                    .putString("workspace_folder_name", "Photos")
                    .commit();

            FolderPrefs.DriveBinding binding = new FolderPrefs(context).getDriveBinding();

            assertNull(binding.treeUri());
            assertEquals("workspace-root", binding.rootFolder().id());
        } finally {
            raw.edit().clear().commit();
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

}
