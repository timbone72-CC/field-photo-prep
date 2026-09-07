package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int REQUEST_AUTHORIZE = 1001;
    private static final String DRIVE_METADATA_READONLY =
            "https://www.googleapis.com/auth/drive.metadata.readonly";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DriveClient driveClient = new DriveClient();
    private final Deque<DriveFolder> browseStack = new ArrayDeque<>();
    private final List<DriveFolder> visibleFolders = new ArrayList<>();

    private AuthorizationClient authorizationClient;
    private FolderPrefs folderPrefs;
    private String accessToken;
    private boolean browsing;

    private TextView statusText;
    private TextView masterText;
    private TextView browsePathText;
    private Button connectButton;
    private Button chooseMasterButton;
    private Button refreshButton;
    private Button useFolderButton;
    private Button backButton;
    private ListView folderList;
    private ArrayAdapter<DriveFolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Draw a usable screen before touching Google Play services. If Google
        // authorization cannot initialize on a particular device, keep the app
        // open and show the problem instead of crashing before first paint.
        folderPrefs = new FolderPrefs(this);
        buildUi();
        renderSavedMaster();

        try {
            authorizationClient = Identity.getAuthorizationClient(this);
            statusText.setText("Ready. Connect Google Drive to continue.");
        } catch (Throwable error) {
            authorizationClient = null;
            connectButton.setEnabled(false);
            showStartupError("Google authorization is unavailable on this device", error);
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
        phase.setText("Phase 1 · Drive folder connection");
        phase.setTextSize(14);
        root.addView(phase);

        statusText = new TextView(this);
        statusText.setText("Starting…");
        statusText.setPadding(0, dp(12), 0, dp(8));
        root.addView(statusText);

        connectButton = new Button(this);
        connectButton.setText("Connect Google Drive");
        connectButton.setOnClickListener(v -> authorizeDrive());
        root.addView(connectButton);

        masterText = new TextView(this);
        masterText.setPadding(0, dp(16), 0, dp(8));
        root.addView(masterText);

        chooseMasterButton = new Button(this);
        chooseMasterButton.setText("Choose Master Folder");
        chooseMasterButton.setEnabled(false);
        chooseMasterButton.setOnClickListener(v -> startFolderBrowser());
        root.addView(chooseMasterButton);

        refreshButton = new Button(this);
        refreshButton.setText("Refresh Address Folders");
        refreshButton.setEnabled(false);
        refreshButton.setOnClickListener(v -> refreshAddressFolders());
        root.addView(refreshButton);

        browsePathText = new TextView(this);
        browsePathText.setVisibility(View.GONE);
        browsePathText.setPadding(0, dp(12), 0, dp(4));
        root.addView(browsePathText);

        LinearLayout browserButtons = new LinearLayout(this);
        browserButtons.setOrientation(LinearLayout.HORIZONTAL);
        backButton = new Button(this);
        backButton.setText("Back");
        backButton.setVisibility(View.GONE);
        backButton.setOnClickListener(v -> browseBack());
        browserButtons.addView(backButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        useFolderButton = new Button(this);
        useFolderButton.setText("Use This Folder");
        useFolderButton.setVisibility(View.GONE);
        useFolderButton.setOnClickListener(v -> useCurrentBrowseFolder());
        browserButtons.addView(useFolderButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(browserButtons);

        folderList = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, visibleFolders);
        folderList.setAdapter(adapter);
        folderList.setOnItemClickListener((parent, view, position, id) -> {
            DriveFolder folder = visibleFolders.get(position);
            if (browsing) {
                browseInto(folder);
            }
        });
        root.addView(folderList, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
    }

    private void authorizeDrive() {
        if (authorizationClient == null) {
            showMessage("Google authorization is unavailable. Restart the app after updating Google Play services.");
            return;
        }

        setBusy("Connecting to Google Drive…");
        try {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .setRequestedScopes(Collections.singletonList(new Scope(DRIVE_METADATA_READONLY)))
                    .build();

            authorizationClient.authorize(request)
                    .addOnSuccessListener(this::handleAuthorizationResult)
                    .addOnFailureListener(error -> showError("Google Drive authorization failed", error));
        } catch (Throwable error) {
            showError("Could not start Google Drive authorization", error);
        }
    }

    private void handleAuthorizationResult(AuthorizationResult result) {
        if (result.hasResolution()) {
            PendingIntent pendingIntent = result.getPendingIntent();
            if (pendingIntent == null) {
                showMessage("Google Drive authorization needs attention, but no consent screen was available.");
                return;
            }
            try {
                startIntentSenderForResult(
                        pendingIntent.getIntentSender(), REQUEST_AUTHORIZE, null, 0, 0, 0);
            } catch (Exception error) {
                showError("Could not open Google authorization", error);
            }
            return;
        }
        acceptAccessToken(result);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_AUTHORIZE) {
            return;
        }
        if (resultCode != RESULT_OK || data == null) {
            showMessage("Google Drive connection was cancelled.");
            return;
        }
        if (authorizationClient == null) {
            showMessage("Google authorization is unavailable.");
            return;
        }
        try {
            AuthorizationResult result = authorizationClient.getAuthorizationResultFromIntent(data);
            acceptAccessToken(result);
        } catch (ApiException error) {
            showError("Google Drive authorization failed", error);
        }
    }

    private void acceptAccessToken(AuthorizationResult result) {
        accessToken = result.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            showMessage("Google Drive did not return an access token.");
            return;
        }
        statusText.setText("Google Drive connected. Folder metadata only.");
        connectButton.setText("Reconnect Google Drive");
        chooseMasterButton.setEnabled(true);
        refreshButton.setEnabled(folderPrefs.getMasterFolder() != null);
        DriveFolder saved = folderPrefs.getMasterFolder();
        if (saved != null) {
            refreshAddressFolders();
        } else {
            setNotBusy();
        }
    }

    private void startFolderBrowser() {
        if (!requireToken()) return;
        browsing = true;
        browseStack.clear();
        browsePathText.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
        useFolderButton.setVisibility(View.GONE);
        loadFolders("root", "My Drive");
    }

    private void browseInto(DriveFolder folder) {
        browseStack.addLast(folder);
        loadFolders(folder.id(), buildBrowsePath());
    }

    private void browseBack() {
        if (!browsing) return;
        if (browseStack.isEmpty()) {
            finishBrowsing();
            return;
        }
        browseStack.removeLast();
        if (browseStack.isEmpty()) {
            loadFolders("root", "My Drive");
        } else {
            DriveFolder current = browseStack.getLast();
            loadFolders(current.id(), buildBrowsePath());
        }
    }

    private void useCurrentBrowseFolder() {
        if (browseStack.isEmpty()) return;
        DriveFolder selected = browseStack.getLast();
        folderPrefs.setMasterFolder(selected);
        finishBrowsing();
        renderSavedMaster();
        refreshAddressFolders();
    }

    private void finishBrowsing() {
        browsing = false;
        browseStack.clear();
        browsePathText.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        useFolderButton.setVisibility(View.GONE);
        visibleFolders.clear();
        adapter.notifyDataSetChanged();
    }

    private String buildBrowsePath() {
        StringBuilder path = new StringBuilder("My Drive");
        for (DriveFolder folder : browseStack) {
            path.append(" / ").append(folder.name());
        }
        return path.toString();
    }

    private void loadFolders(String parentId, String label) {
        browsePathText.setText("Browsing: " + label + "\nTap a folder to open it.");
        useFolderButton.setVisibility(browseStack.isEmpty() ? View.GONE : View.VISIBLE);
        fetchFolders(parentId, "Loading folders…");
    }

    private void refreshAddressFolders() {
        DriveFolder master = folderPrefs.getMasterFolder();
        if (master == null) {
            showMessage("Choose a master folder first.");
            return;
        }
        if (!requireToken()) return;
        browsing = false;
        browsePathText.setVisibility(View.VISIBLE);
        browsePathText.setText("Address folders in " + master.name());
        backButton.setVisibility(View.GONE);
        useFolderButton.setVisibility(View.GONE);
        fetchFolders(master.id(), "Refreshing address folders…");
    }

    private void fetchFolders(String parentId, String busyMessage) {
        if (!requireToken()) return;
        setBusy(busyMessage);
        String token = accessToken;
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFolders(token, parentId);
                runOnUiThread(() -> {
                    visibleFolders.clear();
                    visibleFolders.addAll(folders);
                    adapter.notifyDataSetChanged();
                    statusText.setText(folders.size() + " folder" + (folders.size() == 1 ? "" : "s") + " found.");
                    setNotBusy();
                });
            } catch (Exception error) {
                runOnUiThread(() -> showError("Could not read Drive folders", error));
            }
        });
    }

    private boolean requireToken() {
        if (accessToken == null || accessToken.isBlank()) {
            showMessage("Connect Google Drive first.");
            return false;
        }
        return true;
    }

    private void renderSavedMaster() {
        DriveFolder master = folderPrefs.getMasterFolder();
        masterText.setText(master == null
                ? "Master folder: not selected"
                : "Master folder: " + master.name());
        refreshButton.setEnabled(master != null && accessToken != null);
    }

    private void setBusy(String message) {
        statusText.setText(message);
        connectButton.setEnabled(false);
        chooseMasterButton.setEnabled(false);
        refreshButton.setEnabled(false);
    }

    private void setNotBusy() {
        connectButton.setEnabled(authorizationClient != null);
        chooseMasterButton.setEnabled(accessToken != null);
        refreshButton.setEnabled(accessToken != null && folderPrefs.getMasterFolder() != null);
    }

    private void showMessage(String message) {
        statusText.setText(message);
        setNotBusy();
    }

    private void showError(String prefix, Throwable error) {
        String detail = error.getMessage();
        String type = error.getClass().getSimpleName();
        statusText.setText(prefix + " [" + type + "]" + (detail == null ? "." : ": " + detail));
        setNotBusy();
    }

    private void showStartupError(String prefix, Throwable error) {
        String detail = error.getMessage();
        String type = error.getClass().getSimpleName();
        statusText.setText(prefix + " [" + type + "]" + (detail == null ? "." : ": " + detail));
        chooseMasterButton.setEnabled(false);
        refreshButton.setEnabled(false);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
