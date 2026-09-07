package com.inandout.fieldphotoprep;

import android.content.Context;
import android.content.SharedPreferences;

public final class FolderPrefs {
    private static final String PREFS = "field_photo_prep";
    private static final String MASTER_ID = "master_folder_id";
    private static final String MASTER_NAME = "master_folder_name";

    private final SharedPreferences prefs;

    public FolderPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public DriveFolder getMasterFolder() {
        String id = prefs.getString(MASTER_ID, null);
        String name = prefs.getString(MASTER_NAME, null);
        return id == null || name == null ? null : new DriveFolder(id, name);
    }

    public void setMasterFolder(DriveFolder folder) {
        prefs.edit()
                .putString(MASTER_ID, folder.id())
                .putString(MASTER_NAME, folder.name())
                .apply();
    }
}
