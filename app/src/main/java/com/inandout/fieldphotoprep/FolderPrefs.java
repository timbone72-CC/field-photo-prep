package com.inandout.fieldphotoprep;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public final class FolderPrefs {
    private static final String PREFS = "field_photo_prep";

    // Legacy single-company master keys. Keep these intact as rollback/migration state.
    private static final String MASTER_URI = "master_tree_uri";
    private static final String MASTER_ID = "master_folder_id";
    private static final String MASTER_NAME = "master_folder_name";

    private static final String WORKSPACE_URI = "workspace_tree_uri";
    private static final String WORKSPACE_ID = "workspace_folder_id";
    private static final String WORKSPACE_NAME = "workspace_folder_name";
    private static final String COMPANY_ID = "current_company_folder_id";
    private static final String COMPANY_NAME = "current_company_folder_name";

    private static final String ADDRESS_ID = "current_address_folder_id";
    private static final String ADDRESS_NAME = "current_address_folder_name";
    private static final String ADDRESS_COMPANY_ID = "current_address_company_folder_id";

    private static final String WORK_ORDER_ID = "current_work_order_folder_id";
    private static final String WORK_ORDER_NAME = "current_work_order_folder_name";
    private static final String WORK_ORDER_ADDRESS_ID = "current_work_order_address_folder_id";

    private final SharedPreferences prefs;

    public FolderPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean hasWorkspace() {
        return getWorkspaceTreeUri() != null && getWorkspaceFolder() != null;
    }

    public Uri getWorkspaceTreeUri() {
        return getUri(WORKSPACE_URI);
    }

    public DriveFolder getWorkspaceFolder() {
        return getFolder(WORKSPACE_ID, WORKSPACE_NAME);
    }

    public Uri getLegacyMasterTreeUri() {
        return getUri(MASTER_URI);
    }

    public DriveFolder getLegacyMasterFolder() {
        return getFolder(MASTER_ID, MASTER_NAME);
    }

    /**
     * Compatibility accessor used by upload/reconciliation code.
     * In multi-company mode the broader workspace tree owns the SAF grant.
     * In legacy mode the original single-company tree remains usable.
     */
    public Uri getMasterTreeUri() {
        Uri workspace = getWorkspaceTreeUri();
        return workspace != null ? workspace : getLegacyMasterTreeUri();
    }

    /**
     * Compatibility accessor for the address parent.
     * In multi-company mode this is the active company, never the workspace root.
     */
    public DriveFolder getMasterFolder() {
        if (hasWorkspace()) {
            return getCurrentCompany();
        }
        return getLegacyMasterFolder();
    }

    public DriveFolder getCurrentCompany() {
        return getFolder(COMPANY_ID, COMPANY_NAME);
    }

    public DriveFolder getCurrentAddress() {
        DriveFolder folder = getFolder(ADDRESS_ID, ADDRESS_NAME);
        if (folder == null) {
            return null;
        }
        if (!hasWorkspace()) {
            return folder;
        }
        DriveFolder company = getCurrentCompany();
        String boundCompanyId = prefs.getString(ADDRESS_COMPANY_ID, null);
        if (company == null
                || boundCompanyId == null
                || !company.id().equals(boundCompanyId)
                || folder.id().equals(company.id())) {
            return null;
        }
        return folder;
    }

    public DriveFolder getCurrentWorkOrder() {
        DriveFolder folder = getFolder(WORK_ORDER_ID, WORK_ORDER_NAME);
        DriveFolder address = getCurrentAddress();
        if (folder == null || address == null) {
            return null;
        }
        String boundAddressId = prefs.getString(WORK_ORDER_ADDRESS_ID, null);
        if (boundAddressId == null
                || !address.id().equals(boundAddressId)
                || folder.id().equals(address.id())) {
            return null;
        }
        return folder;
    }

    /**
     * Legacy single-company selection. Retained so a pre-Phase-11 install remains
     * operational until the operator explicitly chooses a broader workspace.
     */
    public void setMasterFolder(Uri treeUri, DriveFolder folder) {
        prefs.edit()
                .putString(MASTER_URI, treeUri.toString())
                .putString(MASTER_ID, folder.id())
                .putString(MASTER_NAME, folder.name())
                .remove(ADDRESS_ID)
                .remove(ADDRESS_NAME)
                .remove(ADDRESS_COMPANY_ID)
                .remove(WORK_ORDER_ID)
                .remove(WORK_ORDER_NAME)
                .remove(WORK_ORDER_ADDRESS_ID)
                .apply();
    }

    public void setWorkspaceFolder(Uri treeUri, DriveFolder folder) {
        prefs.edit()
                .putString(WORKSPACE_URI, treeUri.toString())
                .putString(WORKSPACE_ID, folder.id())
                .putString(WORKSPACE_NAME, folder.name())
                .remove(COMPANY_ID)
                .remove(COMPANY_NAME)
                .remove(ADDRESS_ID)
                .remove(ADDRESS_NAME)
                .remove(ADDRESS_COMPANY_ID)
                .remove(WORK_ORDER_ID)
                .remove(WORK_ORDER_NAME)
                .remove(WORK_ORDER_ADDRESS_ID)
                .apply();
    }

    public void setCurrentCompany(DriveFolder folder) {
        String previousId = prefs.getString(COMPANY_ID, null);
        SharedPreferences.Editor editor = prefs.edit()
                .putString(COMPANY_ID, folder.id())
                .putString(COMPANY_NAME, folder.name());

        if (!folder.id().equals(previousId)) {
            editor.remove(ADDRESS_ID)
                    .remove(ADDRESS_NAME)
                    .remove(ADDRESS_COMPANY_ID)
                    .remove(WORK_ORDER_ID)
                    .remove(WORK_ORDER_NAME)
                    .remove(WORK_ORDER_ADDRESS_ID);
        }
        editor.apply();
    }

    public void clearCurrentCompany() {
        prefs.edit()
                .remove(COMPANY_ID)
                .remove(COMPANY_NAME)
                .remove(ADDRESS_ID)
                .remove(ADDRESS_NAME)
                .remove(ADDRESS_COMPANY_ID)
                .remove(WORK_ORDER_ID)
                .remove(WORK_ORDER_NAME)
                .remove(WORK_ORDER_ADDRESS_ID)
                .apply();
    }

    public void setCurrentAddress(DriveFolder folder) {
        String previousId = prefs.getString(ADDRESS_ID, null);
        SharedPreferences.Editor editor = prefs.edit()
                .putString(ADDRESS_ID, folder.id())
                .putString(ADDRESS_NAME, folder.name());

        if (hasWorkspace()) {
            DriveFolder company = getCurrentCompany();
            if (company == null || folder.id().equals(company.id())) {
                clearCurrentAddress();
                return;
            }
            editor.putString(ADDRESS_COMPANY_ID, company.id());
        } else {
            editor.remove(ADDRESS_COMPANY_ID);
        }

        if (!folder.id().equals(previousId)) {
            editor.remove(WORK_ORDER_ID)
                    .remove(WORK_ORDER_NAME)
                    .remove(WORK_ORDER_ADDRESS_ID);
        }
        editor.apply();
    }

    public void clearCurrentAddress() {
        prefs.edit()
                .remove(ADDRESS_ID)
                .remove(ADDRESS_NAME)
                .remove(ADDRESS_COMPANY_ID)
                .remove(WORK_ORDER_ID)
                .remove(WORK_ORDER_NAME)
                .remove(WORK_ORDER_ADDRESS_ID)
                .apply();
    }

    public void setCurrentWorkOrder(DriveFolder folder) {
        DriveFolder address = getCurrentAddress();
        if (address == null || folder.id().equals(address.id())) {
            clearCurrentWorkOrder();
            return;
        }
        prefs.edit()
                .putString(WORK_ORDER_ID, folder.id())
                .putString(WORK_ORDER_NAME, folder.name())
                .putString(WORK_ORDER_ADDRESS_ID, address.id())
                .apply();
    }

    public void clearCurrentWorkOrder() {
        prefs.edit()
                .remove(WORK_ORDER_ID)
                .remove(WORK_ORDER_NAME)
                .remove(WORK_ORDER_ADDRESS_ID)
                .apply();
    }

    private Uri getUri(String key) {
        String value = prefs.getString(key, null);
        return value == null ? null : Uri.parse(value);
    }

    private DriveFolder getFolder(String idKey, String nameKey) {
        String id = prefs.getString(idKey, null);
        String name = prefs.getString(nameKey, null);
        return id == null || name == null ? null : new DriveFolder(id, name);
    }
}
