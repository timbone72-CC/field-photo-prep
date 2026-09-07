package com.inandout.fieldphotoprep;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public final class FolderPrefs {
    private static final String PREFS = "field_photo_prep";
    private static final String MASTER_URI = "master_tree_uri";
    private static final String MASTER_ID = "master_folder_id";
    private static final String MASTER_NAME = "master_folder_name";

    private final SharedPreferences prefs;

    public FolderPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Uri getMasterTreeUri() {
        String value = prefs.getString(MASTER_URI, null);
        return value == null ? null : Uri.parse(value);
    }

    public DriveFolder getMasterFolder() {
        String id = prefs.getString(MASTER_ID, null);
        String name = prefs.getString(MASTER_NAME, null);
        return id == null || name == null ? null : new DriveFolder(id, name);
    }

    public void setMasterFolder(Uri treeUri, DriveFolder folder) {
        prefs.edit()
                .putString(MASTER_URI, treeUri.toString())
                .putString(MASTER_ID, folder.id())
                .putString(MASTER_NAME, folder.name())
                .apply();
    }
}
