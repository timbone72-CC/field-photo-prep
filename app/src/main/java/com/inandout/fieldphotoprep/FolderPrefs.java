package com.inandout.fieldphotoprep;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public final class FolderPrefs {
    private static final String PREFS = "field_photo_prep";
    private static final String MASTER_URI = "master_tree_uri";
    private static final String MASTER_ID = "master_folder_id";
    private static final String MASTER_NAME = "master_folder_name";
    private static final String ADDRESS_ID = "current_address_folder_id";
    private static final String ADDRESS_NAME = "current_address_folder_name";
    private static final String WORK_ORDER_ID = "current_work_order_folder_id";
    private static final String WORK_ORDER_NAME = "current_work_order_folder_name";
    private static final String WORK_ORDER_ADDRESS_ID = "current_work_order_address_folder_id";

    private final SharedPreferences prefs;

    public FolderPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Uri getMasterTreeUri() {
        String value = prefs.getString(MASTER_URI, null);
        return value == null ? null : Uri.parse(value);
    }

    public DriveFolder getMasterFolder() {
        return getFolder(MASTER_ID, MASTER_NAME);
    }

    public DriveFolder getCurrentAddress() {
        return getFolder(ADDRESS_ID, ADDRESS_NAME);
    }

    public DriveFolder getCurrentWorkOrder() {
        DriveFolder folder = getFolder(WORK_ORDER_ID, WORK_ORDER_NAME);
        if (folder == null) {
            return null;
        }
        String addressId = prefs.getString(ADDRESS_ID, null);
        String boundAddressId = prefs.getString(WORK_ORDER_ADDRESS_ID, null);
        if (addressId == null
                || boundAddressId == null
                || !addressId.equals(boundAddressId)
                || folder.id().equals(addressId)) {
            return null;
        }
        return folder;
    }

    public void setMasterFolder(Uri treeUri, DriveFolder folder) {
        prefs.edit()
                .putString(MASTER_URI, treeUri.toString())
                .putString(MASTER_ID, folder.id())
                .putString(MASTER_NAME, folder.name())
                .remove(ADDRESS_ID)
                .remove(ADDRESS_NAME)
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
        if (!folder.id().equals(previousId)) {
            editor.remove(WORK_ORDER_ID)
                    .remove(WORK_ORDER_NAME)
                    .remove(WORK_ORDER_ADDRESS_ID);
        }
        editor.apply();
    }

    public void setCurrentWorkOrder(DriveFolder folder) {
        String addressId = prefs.getString(ADDRESS_ID, null);
        if (addressId == null || folder.id().equals(addressId)) {
            clearCurrentWorkOrder();
            return;
        }
        prefs.edit()
                .putString(WORK_ORDER_ID, folder.id())
                .putString(WORK_ORDER_NAME, folder.name())
                .putString(WORK_ORDER_ADDRESS_ID, addressId)
                .apply();
    }

    public void clearCurrentWorkOrder() {
        prefs.edit()
                .remove(WORK_ORDER_ID)
                .remove(WORK_ORDER_NAME)
                .remove(WORK_ORDER_ADDRESS_ID)
                .apply();
    }

    private DriveFolder getFolder(String idKey, String nameKey) {
        String id = prefs.getString(idKey, null);
        String name = prefs.getString(nameKey, null);
        return id == null || name == null ? null : new DriveFolder(id, name);
    }
}
