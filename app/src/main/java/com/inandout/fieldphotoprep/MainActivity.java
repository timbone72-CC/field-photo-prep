package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int REQUEST_MASTER_FOLDER = 1001;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DriveClient driveClient = new DriveClient();
    private final List<DriveFolder> visibleFolders = new ArrayList<>();

    private FolderPrefs folderPrefs;
    private TextView statusText;
    private TextView masterText;
    private Button chooseMasterButton;
    private Button refreshButton;
    private ListView folderList;
    private ArrayAdapter<DriveFolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        folderPrefs = new FolderPrefs(this);
        buildUi();
        renderSavedMaster();

        Uri savedTree = folderPrefs.getMasterTreeUri();
        if (savedTree == null) {
            statusText.setText("Choose the master Drive folder to begin.");
        } else if (hasPersistedReadPermission(savedTree)) {
            statusText.setText("Master folder ready.");
            refreshAddressFolders();
        } else {
            statusText.setText("Master folder access expired. Choose it again.");
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Field Photo Prep");
        title.setTextSize(24);
        root.addView(title);

        TextView phase = new TextView(this);
        phase.setText("Phase 1 · Master folder");
        phase.setTextSize(14);
        root.addView(phase);

        statusText = new TextView(this);
        statusText.setPadding(0, dp(12), 0, dp(8));
        root.addView(statusText);

        masterText = new TextView(this);
        masterText.setPadding(0, dp(8), 0, dp(8));
        root.addView(masterText);

        chooseMasterButton = new Button(this);
        chooseMasterButton.setText("Choose Master Folder");
        chooseMasterButton.setOnClickListener(v -> chooseMasterFolder());
        root.addView(chooseMasterButton);

        refreshButton = new Button(this);
        refreshButton.setText("Refresh Address Folders");
        refreshButton.setEnabled(false);
        refreshButton.setOnClickListener(v -> refreshAddressFolders());
        root.addView(refreshButton);

        TextView listLabel = new TextView(this);
        listLabel.setText("Address folders");
        listLabel.setTextSize(16);
        listLabel.setPadding(0, dp(16), 0, dp(4));
        root.addView(listLabel);

        folderList = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, visibleFolders);
        folderList.setAdapter(adapter);
        root.addView(folderList, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
    }

    private void chooseMasterFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_MASTER_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_MASTER_FOLDER) {
            return;
        }
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            showMessage("Folder selection cancelled.");
            return;
        }

        Uri treeUri = data.getData();
        int grantedFlags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(treeUri, grantedFlags);
            DriveFolder master = driveClient.getTreeFolder(getContentResolver(), treeUri);
            folderPrefs.setMasterFolder(treeUri, master);
            renderSavedMaster();
            refreshAddressFolders();
        } catch (Exception error) {
            showError("Could not keep access to that folder", error);
        }
    }

    private void refreshAddressFolders() {
        Uri treeUri = folderPrefs.getMasterTreeUri();
        DriveFolder master = folderPrefs.getMasterFolder();
        if (treeUri == null || master == null) {
            showMessage("Choose a master folder first.");
            return;
        }
        if (!hasPersistedReadPermission(treeUri)) {
            showMessage("Master folder access expired. Choose it again.");
            return;
        }

        setBusy("Refreshing address folders…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFolders(getContentResolver(), treeUri);
                runOnUiThread(() -> {
                    visibleFolders.clear();
                    visibleFolders.addAll(folders);
                    adapter.notifyDataSetChanged();
                    statusText.setText(folders.size() + " address folder"
                            + (folders.size() == 1 ? "" : "s") + " found.");
                    setNotBusy();
                });
            } catch (Exception error) {
                runOnUiThread(() -> showError("Could not read address folders", error));
            }
        });
    }

    private boolean hasPersistedReadPermission(Uri treeUri) {
        for (UriPermission permission : getContentResolver().getPersistedUriPermissions()) {
            if (treeUri.equals(permission.getUri()) && permission.isReadPermission()) {
                return true;
            }
        }
        return false;
    }

    private void renderSavedMaster() {
        DriveFolder master = folderPrefs.getMasterFolder();
        masterText.setText(master == null
                ? "Master folder: not selected"
                : "Master folder: " + master.name());
        refreshButton.setEnabled(master != null
                && folderPrefs.getMasterTreeUri() != null
                && hasPersistedReadPermission(folderPrefs.getMasterTreeUri()));
    }

    private void setBusy(String message) {
        statusText.setText(message);
        chooseMasterButton.setEnabled(false);
        refreshButton.setEnabled(false);
    }

    private void setNotBusy() {
        chooseMasterButton.setEnabled(true);
        Uri treeUri = folderPrefs.getMasterTreeUri();
        refreshButton.setEnabled(treeUri != null && hasPersistedReadPermission(treeUri));
    }

    private void showMessage(String message) {
        statusText.setText(message);
        setNotBusy();
    }

    private void showError(String prefix, Throwable error) {
        String detail = error.getMessage();
        statusText.setText(prefix + (detail == null ? "." : ": " + detail));
        setNotBusy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
