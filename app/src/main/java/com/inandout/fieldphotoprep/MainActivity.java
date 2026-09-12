package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.UriPermission;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int REQUEST_MASTER_FOLDER = 1001;

    private enum Screen {
        ADDRESSES,
        WORK_ORDERS
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DriveClient driveClient = new DriveClient();
    private final List<DriveFolder> visibleFolders = new ArrayList<>();

    private FolderPrefs folderPrefs;
    private Screen screen = Screen.ADDRESSES;
    private DriveFolder selectedAddress;
    private DriveFolder selectedWorkOrder;
    private LocalDate selectedDate = LocalDate.now();
    private boolean createBlockedUntilRefresh;
    private boolean busy;

    private FrameLayout appRoot;
    private View homeRoot;
    private LinearLayout legacyRoot;

    private TextView statusText;
    private TextView homeStatusText;
    private TextView legacyStatusText;
    private TextView legacyMasterText;
    private TextView homeMasterNameText;
    private TextView homeDriveStateText;
    private TextView homePropertyCountText;
    private TextView homeEmptyText;
    private View homeDriveStatusDot;
    private ProgressBar homeProgress;
    private ImageButton driveOptionsButton;

    private TextView addressText;
    private TextView currentWorkOrderText;
    private LinearLayout workOrderControls;
    private ScrollView workOrderScroll;
    private Button chooseMasterButton;
    private ImageButton refreshAddressButton;
    private Button useCreateAddressButton;
    private Button backButton;
    private Button refreshWorkOrdersButton;
    private Button selectWorkOrderButton;
    private Button dateButton;
    private Button useCreateButton;
    private Button reuseEmptyButton;
    private Button clearReuseButton;
    private Button photosButton;
    private EditText workOrderInput;
    private ListView folderList;
    private ArrayAdapter<DriveFolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        folderPrefs = new FolderPrefs(this);
        buildUi();
        showAddressScreen(false);
        renderSavedMaster();

        Uri savedTree = folderPrefs.getMasterTreeUri();
        if (savedTree == null) {
            showHomeInlineMessage("Google Drive is not connected.");
        } else if (hasPersistedReadPermission(savedTree)) {
            clearHomeInlineMessage();
            refreshAddressFolders();
        } else {
            showHomeInlineMessage("Drive access expired. Reconnect the master folder.");
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (screen == Screen.WORK_ORDERS) {
            showAddressScreen(true);
        } else {
            super.onBackPressed();
        }
    }

    private void buildUi() {
        appRoot = new FrameLayout(this);
        appRoot.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        buildHomeUi();
        buildLegacyWorkOrderUi();

        appRoot.addView(homeRoot, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        appRoot.addView(legacyRoot, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(appRoot);
        updateDateButton();
    }

    private void buildHomeUi() {
        homeRoot = LayoutInflater.from(this).inflate(R.layout.screen_home_properties, appRoot, false);
        homeStatusText = homeRoot.findViewById(R.id.home_status_text);
        homeMasterNameText = homeRoot.findViewById(R.id.home_master_name);
        homeDriveStateText = homeRoot.findViewById(R.id.home_drive_state);
        homePropertyCountText = homeRoot.findViewById(R.id.home_property_count);
        homeEmptyText = homeRoot.findViewById(R.id.home_empty_text);
        homeDriveStatusDot = homeRoot.findViewById(R.id.drive_status_dot);
        homeProgress = homeRoot.findViewById(R.id.home_progress);
        driveOptionsButton = homeRoot.findViewById(R.id.home_drive_options_button);

        chooseMasterButton = homeRoot.findViewById(R.id.home_connect_button);
        refreshAddressButton = homeRoot.findViewById(R.id.home_refresh_button);
        useCreateAddressButton = homeRoot.findViewById(R.id.home_new_address_button);
        folderList = homeRoot.findViewById(R.id.home_property_list);

        chooseMasterButton.setOnClickListener(v -> chooseMasterFolder());
        refreshAddressButton.setOnClickListener(v -> refreshAddressFolders());
        useCreateAddressButton.setOnClickListener(v -> showAddressEntryDialog());
        driveOptionsButton.setOnClickListener(this::showDriveOptions);

        adapter = new PropertyListAdapter(this, visibleFolders);
        folderList.setAdapter(adapter);
        folderList.setOnItemClickListener((parent, view, position, id) -> {
            if (screen != Screen.ADDRESSES || busy || position < 0 || position >= visibleFolders.size()) {
                return;
            }
            openAddress(visibleFolders.get(position));
        });

        ViewCompat.setOnApplyWindowInsetsListener(homeRoot, (view, insets) -> {
            var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(homeRoot);
    }

    private void showDriveOptions(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Change Drive");
        menu.setOnMenuItemClickListener(item -> {
            chooseMasterFolder();
            return true;
        });
        menu.show();
    }

    private void buildLegacyWorkOrderUi() {
        int pad = dp(16);
        legacyRoot = new LinearLayout(this);
        legacyRoot.setOrientation(LinearLayout.VERTICAL);
        legacyRoot.setPadding(pad, pad, pad, pad);
        legacyRoot.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Field Photo Prep");
        title.setTextSize(24);
        legacyRoot.addView(title);

        TextView phase = new TextView(this);
        phase.setText("Address and work-order setup");
        phase.setTextSize(14);
        legacyRoot.addView(phase);

        legacyStatusText = new TextView(this);
        legacyStatusText.setPadding(0, dp(12), 0, dp(8));
        legacyRoot.addView(legacyStatusText);

        legacyMasterText = new TextView(this);
        legacyMasterText.setPadding(0, dp(8), 0, dp(8));
        legacyRoot.addView(legacyMasterText);

        workOrderControls = new LinearLayout(this);
        workOrderControls.setOrientation(LinearLayout.VERTICAL);

        addressText = new TextView(this);
        addressText.setTextSize(18);
        addressText.setPadding(0, dp(4), 0, dp(8));
        workOrderControls.addView(addressText);

        LinearLayout navigationRow = new LinearLayout(this);
        navigationRow.setOrientation(LinearLayout.HORIZONTAL);

        backButton = new Button(this);
        backButton.setText("Back to Addresses");
        backButton.setOnClickListener(v -> showAddressScreen(true));
        navigationRow.addView(backButton, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        refreshWorkOrdersButton = new Button(this);
        refreshWorkOrdersButton.setText("Refresh Work Orders");
        refreshWorkOrdersButton.setOnClickListener(v -> refreshWorkOrderFolders());
        navigationRow.addView(refreshWorkOrdersButton, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        workOrderControls.addView(navigationRow);

        selectWorkOrderButton = new Button(this);
        selectWorkOrderButton.setText("Select Existing Work Order");
        selectWorkOrderButton.setEnabled(false);
        selectWorkOrderButton.setOnClickListener(v -> showWorkOrderPicker());
        workOrderControls.addView(selectWorkOrderButton);

        workOrderInput = new EditText(this);
        workOrderInput.setHint("Work order name only — e.g. Cut Grass");
        workOrderInput.setSingleLine(true);
        workOrderControls.addView(workOrderInput);

        dateButton = new Button(this);
        dateButton.setOnClickListener(v -> chooseWorkOrderDate());
        workOrderControls.addView(dateButton);

        useCreateButton = new Button(this);
        useCreateButton.setText("Use / Create Dated Work Order");
        useCreateButton.setOnClickListener(v -> useOrCreateWorkOrder());
        workOrderControls.addView(useCreateButton);

        reuseEmptyButton = new Button(this);
        reuseEmptyButton.setText("Reuse Selected Empty Folder");
        reuseEmptyButton.setOnClickListener(v -> reuseSelectedEmptyFolder());
        workOrderControls.addView(reuseEmptyButton);

        clearReuseButton = new Button(this);
        clearReuseButton.setText("Clear & Reuse Selected Folder");
        clearReuseButton.setOnClickListener(v -> prepareClearAndReuse());
        workOrderControls.addView(clearReuseButton);

        currentWorkOrderText = new TextView(this);
        currentWorkOrderText.setPadding(0, dp(8), 0, dp(4));
        workOrderControls.addView(currentWorkOrderText);

        photosButton = new Button(this);
        photosButton.setText("Photos for Selected Work Order");
        photosButton.setEnabled(false);
        photosButton.setOnClickListener(v -> openPhotoCapture());
        workOrderControls.addView(photosButton);

        workOrderScroll = new ScrollView(this);
        workOrderScroll.setFillViewport(true);
        workOrderScroll.addView(workOrderControls, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        legacyRoot.addView(workOrderScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        legacyRoot.setFitsSystemWindows(true);
        legacyRoot.setVisibility(View.GONE);
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
            showAddressScreen(false);
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

        createBlockedUntilRefresh = false;
        setBusy("Refreshing address folders…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFolders(getContentResolver(), treeUri);
                runOnUiThread(() -> {
                    visibleFolders.clear();
                    visibleFolders.addAll(folders);
                    adapter.notifyDataSetChanged();
                    renderPropertyCountAndEmptyState();
                    clearHomeInlineMessage();
                    setNotBusy();
                });
            } catch (Exception error) {
                runOnUiThread(() -> showError("Could not read address folders", error));
            }
        });
    }

    private void showAddressEntryDialog() {
        Uri treeUri = folderPrefs.getMasterTreeUri();
        DriveFolder master = folderPrefs.getMasterFolder();
        if (treeUri == null || master == null) {
            showMessage("Choose a master folder first.");
            return;
        }
        if (!hasPersistedReadPermission(treeUri) || !hasPersistedWritePermission(treeUri)) {
            showMessage("The master folder needs read/write access before an address can be created.");
            return;
        }
        if (createBlockedUntilRefresh) {
            showMessage("Refresh address folders before trying another Drive write.");
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Address folder name");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Use or create address folder")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Use / Create", (dialog, which) ->
                        useOrCreateAddress(input.getText().toString()))
                .show();
    }

    private void useOrCreateAddress(String rawName) {
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
        if (!hasPersistedWritePermission(treeUri)) {
            showMessage("This master folder is read-only. Choose it again and allow write access.");
            return;
        }
        if (createBlockedUntilRefresh) {
            showMessage("Refresh address folders before trying another Drive write.");
            return;
        }

        final String requestedName;
        try {
            requestedName = AddressFolderName.build(rawName);
        } catch (IllegalArgumentException error) {
            showMessage(error.getMessage());
            return;
        }

        final String masterId = master.id();
        setBusy("Checking for address " + requestedName + "…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, masterId);
                List<DriveFolder> matches = DriveClient.findExactNameMatches(folders, requestedName);

                if (matches.size() > 1) {
                    runOnUiThread(() -> {
                        if (screen != Screen.ADDRESSES) {
                            return;
                        }
                        visibleFolders.clear();
                        visibleFolders.addAll(folders);
                        adapter.notifyDataSetChanged();
                        renderPropertyCountAndEmptyState();
                        showHomeInlineMessage(matches.size() + " address folders named " + requestedName
                                + " already exist. Choose the intended property; no folder was created.");
                        setNotBusy();
                    });
                    return;
                }

                if (matches.size() == 1) {
                    DriveFolder existing = matches.get(0);
                    runOnUiThread(() -> {
                        if (screen != Screen.ADDRESSES) {
                            return;
                        }
                        visibleFolders.clear();
                        visibleFolders.addAll(folders);
                        adapter.notifyDataSetChanged();
                        renderPropertyCountAndEmptyState();
                        openAddress(existing);
                    });
                    return;
                }

                DriveFolder created = driveClient.createFolder(
                        getContentResolver(), treeUri, masterId, requestedName);
                List<DriveFolder> afterCreate = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, masterId);
                DriveFolder verified = DriveClient.findById(afterCreate, created.id());
                List<DriveFolder> verifiedMatches = DriveClient.findExactNameMatches(afterCreate, requestedName);

                if (verified == null || !verified.name().equals(requestedName)) {
                    throw new IOException("Drive returned an address ID that could not be verified under the selected master.");
                }

                if (verifiedMatches.size() != 1 || !verifiedMatches.get(0).id().equals(created.id())) {
                    runOnUiThread(() -> {
                        if (screen != Screen.ADDRESSES) {
                            return;
                        }
                        createBlockedUntilRefresh = true;
                        visibleFolders.clear();
                        visibleFolders.addAll(afterCreate);
                        adapter.notifyDataSetChanged();
                        renderPropertyCountAndEmptyState();
                        showHomeInlineMessage("Address create result is ambiguous. Choose the intended same-named property; no additional folder will be created until refresh.");
                        setNotBusy();
                    });
                    return;
                }

                runOnUiThread(() -> {
                    if (screen != Screen.ADDRESSES) {
                        return;
                    }
                    visibleFolders.clear();
                    visibleFolders.addAll(afterCreate);
                    adapter.notifyDataSetChanged();
                    renderPropertyCountAndEmptyState();
                    openAddress(verified);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    createBlockedUntilRefresh = true;
                    showError("Address create result is not safe to repeat. Refresh address folders before trying again", error);
                });
            }
        });
    }

    private void openAddress(DriveFolder address) {
        selectedAddress = address;
        folderPrefs.setCurrentAddress(address);
        selectedWorkOrder = folderPrefs.getCurrentWorkOrder();
        createBlockedUntilRefresh = false;
        screen = Screen.WORK_ORDERS;
        statusText = legacyStatusText;
        homeRoot.setVisibility(View.GONE);
        legacyRoot.setVisibility(View.VISIBLE);
        addressText.setText("Address: " + address.name());
        renderCurrentWorkOrder();
        renderWorkOrderPickerButton();
        applySystemBarAppearance(false);
        refreshWorkOrderFolders();
    }

    private void showAddressScreen(boolean refresh) {
        screen = Screen.ADDRESSES;
        selectedAddress = null;
        selectedWorkOrder = null;
        createBlockedUntilRefresh = false;
        statusText = homeStatusText;
        legacyRoot.setVisibility(View.GONE);
        homeRoot.setVisibility(View.VISIBLE);
        visibleFolders.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        renderPropertyCountAndEmptyState();
        renderSavedMaster();
        clearHomeInlineMessage();
        applySystemBarAppearance(true);
        setNotBusy();
        if (refresh && folderPrefs.getMasterTreeUri() != null) {
            refreshAddressFolders();
        }
    }

    private void refreshWorkOrderFolders() {
        if (selectedAddress == null) {
            showMessage("Choose an address first.");
            return;
        }
        Uri treeUri = folderPrefs.getMasterTreeUri();
        if (treeUri == null || !hasPersistedReadPermission(treeUri)) {
            showMessage("Master folder access expired. Choose it again.");
            return;
        }

        createBlockedUntilRefresh = false;
        String addressId = selectedAddress.id();
        setBusy("Refreshing work-order folders…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFolders(
                        getContentResolver(), treeUri, addressId);
                runOnUiThread(() -> {
                    if (!isStillOnAddress(addressId)) {
                        return;
                    }
                    visibleFolders.clear();
                    visibleFolders.addAll(folders);
                    reconcileSelectedWorkOrder(folders);
                    adapter.notifyDataSetChanged();
                    statusText.setText(folders.size() + " work-order folder"
                            + (folders.size() == 1 ? "" : "s") + " found.");
                    setNotBusy();
                });
            } catch (Exception error) {
                runOnUiThread(() -> showError("Could not read work-order folders", error));
            }
        });
    }

    private void showWorkOrderPicker() {
        if (screen != Screen.WORK_ORDERS || visibleFolders.isEmpty()) {
            showMessage("No work-order folders are available to select.");
            return;
        }

        CharSequence[] labels = new CharSequence[visibleFolders.size()];
        for (int i = 0; i < visibleFolders.size(); i++) {
            labels[i] = folderLabel(visibleFolders.get(i));
        }

        new AlertDialog.Builder(this)
                .setTitle("Select existing work-order folder")
                .setItems(labels, (dialog, which) -> {
                    if (which >= 0 && which < visibleFolders.size()) {
                        selectWorkOrder(visibleFolders.get(which), "Work order selected");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renderWorkOrderPickerButton() {
        if (selectWorkOrderButton == null) {
            return;
        }
        int count = screen == Screen.WORK_ORDERS ? visibleFolders.size() : 0;
        selectWorkOrderButton.setText(count == 0
                ? "Select Existing Work Order"
                : "Select Existing Work Order (" + count + ")");
    }

    private void chooseWorkOrderDate() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    updateDateButton();
                },
                selectedDate.getYear(),
                selectedDate.getMonthValue() - 1,
                selectedDate.getDayOfMonth());
        dialog.show();
    }

    private void updateDateButton() {
        if (dateButton != null) {
            dateButton.setText("Date: " + selectedDate);
        }
    }

    private void useOrCreateWorkOrder() {
        if (selectedAddress == null) {
            showMessage("Choose an address first.");
            return;
        }
        Uri treeUri = folderPrefs.getMasterTreeUri();
        if (treeUri == null || !hasPersistedReadPermission(treeUri)) {
            showMessage("Master folder access expired. Choose it again.");
            return;
        }
        if (!hasPersistedWritePermission(treeUri)) {
            showMessage("This master folder is read-only. Choose it again and allow write access.");
            return;
        }
        if (createBlockedUntilRefresh) {
            showMessage("Refresh work-order folders before trying another Drive write.");
            return;
        }

        final String requestedName;
        try {
            requestedName = WorkOrderFolderName.build(workOrderInput.getText().toString(), selectedDate.toString());
        } catch (IllegalArgumentException error) {
            showMessage(error.getMessage());
            return;
        }

        final String addressId = selectedAddress.id();
        setBusy("Checking for " + requestedName + "…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, addressId);
                List<DriveFolder> matches = DriveClient.findExactNameMatches(folders, requestedName);

                if (matches.size() > 1) {
                    runOnUiThread(() -> {
                        if (!isStillOnAddress(addressId)) {
                            return;
                        }
                        visibleFolders.clear();
                        visibleFolders.addAll(folders);
                        adapter.notifyDataSetChanged();
                        statusText.setText(matches.size() + " folders named " + requestedName
                                + " already exist. Select the intended one; no folder was created.");
                        setNotBusy();
                    });
                    return;
                }

                if (matches.size() == 1) {
                    DriveFolder existing = matches.get(0);
                    runOnUiThread(() -> {
                        if (!isStillOnAddress(addressId)) {
                            return;
                        }
                        visibleFolders.clear();
                        visibleFolders.addAll(folders);
                        adapter.notifyDataSetChanged();
                        selectWorkOrder(existing, "Existing work-order folder reused");
                    });
                    return;
                }

                DriveFolder created = driveClient.createFolder(
                        getContentResolver(), treeUri, addressId, requestedName);
                List<DriveFolder> afterCreate = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, addressId);
                DriveFolder verified = DriveClient.findById(afterCreate, created.id());
                List<DriveFolder> verifiedMatches = DriveClient.findExactNameMatches(afterCreate, requestedName);

                if (verified == null || !verified.name().equals(requestedName)) {
                    throw new IOException("Drive returned a work-order ID that could not be verified under the selected address.");
                }

                if (verifiedMatches.size() != 1 || !verifiedMatches.get(0).id().equals(created.id())) {
                    runOnUiThread(() -> {
                        if (!isStillOnAddress(addressId)) {
                            return;
                        }
                        createBlockedUntilRefresh = true;
                        visibleFolders.clear();
                        visibleFolders.addAll(afterCreate);
                        adapter.notifyDataSetChanged();
                        statusText.setText("Work-order create result is ambiguous. Select the intended same-named folder; no additional folder will be created until refresh.");
                        setNotBusy();
                    });
                    return;
                }

                runOnUiThread(() -> {
                    if (!isStillOnAddress(addressId)) {
                        return;
                    }
                    visibleFolders.clear();
                    visibleFolders.addAll(afterCreate);
                    adapter.notifyDataSetChanged();
                    selectWorkOrder(verified, "Work-order folder created");
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    createBlockedUntilRefresh = true;
                    showError("Work-order create result is not safe to repeat. Refresh before trying again", error);
                });
            }
        });
    }

    private void reuseSelectedEmptyFolder() {
        if (selectedAddress == null) {
            showMessage("Choose an address first.");
            return;
        }
        if (selectedWorkOrder == null) {
            showMessage("Select the older work-order folder you want to reuse first.");
            return;
        }
        Uri treeUri = folderPrefs.getMasterTreeUri();
        if (treeUri == null || !hasPersistedReadPermission(treeUri)) {
            showMessage("Master folder access expired. Choose it again.");
            return;
        }
        if (!hasPersistedWritePermission(treeUri)) {
            showMessage("This master folder is read-only. Choose it again and allow write access.");
            return;
        }
        if (createBlockedUntilRefresh) {
            showMessage("Refresh work-order folders before trying another Drive write.");
            return;
        }

        final String requestedName;
        try {
            requestedName = WorkOrderFolderName.build(workOrderInput.getText().toString(), selectedDate.toString());
        } catch (IllegalArgumentException error) {
            showMessage(error.getMessage());
            return;
        }

        final String candidateId = selectedWorkOrder.id();
        final String candidateName = selectedWorkOrder.name();
        if (candidateName.equals(requestedName)) {
            showMessage("The selected folder already has the requested work-order date.");
            return;
        }
        try {
            if (!WorkOrderFolderName.isOlderSameWorkOrderFolder(
                    candidateName, workOrderInput.getText().toString(), selectedDate.toString())) {
                showMessage("Select an older folder for the same work order before reusing it.");
                return;
            }
        } catch (IllegalArgumentException error) {
            showMessage(error.getMessage());
            return;
        }

        final String addressId = selectedAddress.id();
        setBusy("Checking whether " + candidateName + " is safe to reuse…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, addressId);
                List<DriveFolder> requestedMatches = DriveClient.findExactNameMatches(folders, requestedName);

                if (requestedMatches.size() > 1) {
                    runOnUiThread(() -> {
                        if (!isStillOnAddress(addressId)) {
                            return;
                        }
                        visibleFolders.clear();
                        visibleFolders.addAll(folders);
                        adapter.notifyDataSetChanged();
                        statusText.setText(requestedMatches.size() + " folders named " + requestedName
                                + " already exist. No old folder was renamed; select the intended existing folder.");
                        setNotBusy();
                    });
                    return;
                }

                if (requestedMatches.size() == 1) {
                    DriveFolder existing = requestedMatches.get(0);
                    runOnUiThread(() -> {
                        if (!isStillOnAddress(addressId)) {
                            return;
                        }
                        visibleFolders.clear();
                        visibleFolders.addAll(folders);
                        adapter.notifyDataSetChanged();
                        selectWorkOrder(existing, "Requested dated folder already exists; old folder unchanged");
                    });
                    return;
                }

                DriveFolder actualCandidate = DriveClient.findById(folders, candidateId);
                if (actualCandidate == null) {
                    runOnUiThread(() -> showMessage(
                            "The selected old folder is no longer under this address. Refresh and choose again."));
                    return;
                }
                if (!WorkOrderFolderName.isOlderSameWorkOrderFolder(
                        actualCandidate.name(), workOrderInput.getText().toString(), selectedDate.toString())) {
                    runOnUiThread(() -> showMessage(
                            "The selected folder no longer matches an older occurrence of this work order."));
                    return;
                }

                if (!driveClient.isFolderEmpty(getContentResolver(), treeUri, candidateId)) {
                    runOnUiThread(() -> showMessage(
                            "Selected old folder is not empty. Nothing was renamed or deleted."));
                    return;
                }

                DriveFolder renameResult = driveClient.renameFolder(
                        getContentResolver(), treeUri, candidateId, requestedName);
                if (!candidateId.equals(renameResult.id())) {
                    throw new IOException("Drive rename changed the folder identity; refresh before proceeding.");
                }

                List<DriveFolder> afterRename = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, addressId);
                DriveFolder verified = DriveClient.findById(afterRename, candidateId);
                if (verified == null || !verified.name().equals(requestedName)) {
                    throw new IOException("Drive rename could not be verified with the original folder identity.");
                }
                List<DriveFolder> verifiedMatches = DriveClient.findExactNameMatches(afterRename, requestedName);
                if (verifiedMatches.size() != 1 || !verifiedMatches.get(0).id().equals(candidateId)) {
                    throw new IOException("The renamed folder is ambiguous. Refresh and choose the intended folder.");
                }

                runOnUiThread(() -> {
                    if (!isStillOnAddress(addressId)) {
                        return;
                    }
                    visibleFolders.clear();
                    visibleFolders.addAll(afterRename);
                    adapter.notifyDataSetChanged();
                    selectWorkOrder(verified, "Empty folder reused with the same identity");
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    createBlockedUntilRefresh = true;
                    showError("Folder reuse result is not safe to repeat. Refresh before trying again", error);
                });
            }
        });
    }

    private void prepareClearAndReuse() {
        if (selectedAddress == null) {
            showMessage("Choose an address first.");
            return;
        }
        if (selectedWorkOrder == null) {
            showMessage("Select the older work-order folder you want to clear and reuse first.");
            return;
        }
        Uri treeUri = folderPrefs.getMasterTreeUri();
        if (treeUri == null || !hasPersistedReadPermission(treeUri)) {
            showMessage("Master folder access expired. Choose it again.");
            return;
        }
        if (!hasPersistedWritePermission(treeUri)) {
            showMessage("This master folder is read-only. Choose it again and allow write access.");
            return;
        }
        if (createBlockedUntilRefresh) {
            showMessage("Refresh work-order folders before trying another Drive write.");
            return;
        }

        final String requestedName;
        try {
            requestedName = WorkOrderFolderName.build(workOrderInput.getText().toString(), selectedDate.toString());
        } catch (IllegalArgumentException error) {
            showMessage(error.getMessage());
            return;
        }

        final String candidateId = selectedWorkOrder.id();
        final String candidateName = selectedWorkOrder.name();
        if (candidateName.equals(requestedName)) {
            showMessage("The selected folder already has the requested work-order date.");
            return;
        }
        try {
            if (!WorkOrderFolderName.isOlderSameWorkOrderFolder(
                    candidateName, workOrderInput.getText().toString(), selectedDate.toString())) {
                showMessage("Select an older folder for the same work order before clearing it.");
                return;
            }
        } catch (IllegalArgumentException error) {
            showMessage(error.getMessage());
            return;
        }

        final String addressId = selectedAddress.id();
        setBusy("Reviewing " + candidateName + " for Clear & Reuse…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, addressId);
                List<DriveFolder> requestedMatches = DriveClient.findExactNameMatches(folders, requestedName);

                if (requestedMatches.size() > 1) {
                    runOnUiThread(() -> {
                        if (!isStillOnAddress(addressId)) {
                            return;
                        }
                        visibleFolders.clear();
                        visibleFolders.addAll(folders);
                        adapter.notifyDataSetChanged();
                        statusText.setText(requestedMatches.size() + " folders named " + requestedName
                                + " already exist. Nothing was deleted; select the intended existing folder.");
                        setNotBusy();
                    });
                    return;
                }

                if (requestedMatches.size() == 1) {
                    DriveFolder existing = requestedMatches.get(0);
                    runOnUiThread(() -> {
                        if (!isStillOnAddress(addressId)) {
                            return;
                        }
                        visibleFolders.clear();
                        visibleFolders.addAll(folders);
                        adapter.notifyDataSetChanged();
                        selectWorkOrder(existing, "Requested dated folder already exists; old folder unchanged");
                    });
                    return;
                }

                DriveFolder actualCandidate = DriveClient.findById(folders, candidateId);
                if (actualCandidate == null) {
                    runOnUiThread(() -> showMessage(
                            "The selected old folder is no longer under this address. Refresh and choose again."));
                    return;
                }
                if (!actualCandidate.name().equals(candidateName)) {
                    runOnUiThread(() -> showMessage(
                            "The selected folder name changed. Nothing was deleted; refresh and choose again."));
                    return;
                }
                if (!WorkOrderFolderName.isOlderSameWorkOrderFolder(
                        actualCandidate.name(), workOrderInput.getText().toString(), selectedDate.toString())) {
                    runOnUiThread(() -> showMessage(
                            "The selected folder no longer matches an older occurrence of this work order."));
                    return;
                }

                DriveClient.ChildSnapshot snapshot = driveClient.listDirectChildren(
                        getContentResolver(), treeUri, candidateId);
                if (snapshot.count() == 0) {
                    runOnUiThread(() -> showMessage(
                            "Selected folder is empty. Use Reuse Selected Empty Folder; nothing was deleted."));
                    return;
                }

                runOnUiThread(() -> showClearReuseConfirmation(
                        treeUri,
                        addressId,
                        candidateId,
                        candidateName,
                        requestedName,
                        snapshot));
            } catch (Exception error) {
                runOnUiThread(() -> showError(
                        "Could not verify Clear & Reuse. Nothing was deleted", error));
            }
        });
    }

    private void showClearReuseConfirmation(
            Uri treeUri,
            String addressId,
            String candidateId,
            String candidateName,
            String requestedName,
            DriveClient.ChildSnapshot snapshot) {
        if (!isStillOnAddress(addressId)
                || selectedWorkOrder == null
                || !selectedWorkOrder.id().equals(candidateId)) {
            showMessage("The selected folder changed. Nothing was deleted.");
            return;
        }

        DriveFolder master = folderPrefs.getMasterFolder();
        if (master == null || selectedAddress == null) {
            showMessage("The folder hierarchy is incomplete. Nothing was deleted.");
            return;
        }

        String folderWarning = snapshot.folderCount() == 0
                ? ""
                : "\n\n" + snapshot.folderCount() + " of those direct items "
                + (snapshot.folderCount() == 1 ? "is a child folder" : "are child folders")
                + "; deleting a child folder also removes its contents.";

        new AlertDialog.Builder(this)
                .setTitle("Clear & Reuse selected folder?")
                .setMessage("Master: " + master.name()
                        + "\nAddress: " + selectedAddress.name()
                        + "\nOld folder: " + candidateName
                        + "\nDirect items to remove: " + snapshot.count()
                        + "\nNew folder: " + requestedName
                        + folderWarning
                        + "\n\nOnly continue if this full path and item count are correct.")
                .setNegativeButton("Cancel", (dialog, which) ->
                        showMessage("Clear & Reuse cancelled. Nothing was changed."))
                .setPositiveButton("Clear & Reuse", (dialog, which) ->
                        performConfirmedClearReuse(
                                treeUri,
                                addressId,
                                candidateId,
                                candidateName,
                                requestedName,
                                snapshot))
                .setOnCancelListener(dialog ->
                        showMessage("Clear & Reuse cancelled. Nothing was changed."))
                .show();
    }

    private void performConfirmedClearReuse(
            Uri treeUri,
            String addressId,
            String candidateId,
            String candidateName,
            String requestedName,
            DriveClient.ChildSnapshot approvedSnapshot) {
        if (!isStillOnAddress(addressId)
                || selectedWorkOrder == null
                || !selectedWorkOrder.id().equals(candidateId)) {
            showMessage("The selected folder changed. Nothing was deleted.");
            return;
        }

        setBusy("Rechecking approved folder before deletion…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, addressId);
                List<DriveFolder> requestedMatches = DriveClient.findExactNameMatches(folders, requestedName);
                if (!requestedMatches.isEmpty()) {
                    runOnUiThread(() -> showMessage(
                            "The requested dated folder now exists. Nothing was deleted; refresh and choose the intended folder."));
                    return;
                }

                DriveFolder actualCandidate = DriveClient.findById(folders, candidateId);
                if (actualCandidate == null) {
                    runOnUiThread(() -> showMessage(
                            "The selected old folder is no longer under this address. Nothing was deleted."));
                    return;
                }
                if (!actualCandidate.name().equals(candidateName)) {
                    runOnUiThread(() -> showMessage(
                            "The selected folder name changed after confirmation. Nothing was deleted."));
                    return;
                }
                if (!WorkOrderFolderName.isOlderSameWorkOrderFolder(
                        actualCandidate.name(), workOrderInput.getText().toString(), selectedDate.toString())) {
                    runOnUiThread(() -> showMessage(
                            "The selected folder no longer matches the requested reuse. Nothing was deleted."));
                    return;
                }

                DriveClient.ChildSnapshot currentSnapshot = driveClient.listDirectChildren(
                        getContentResolver(), treeUri, candidateId);
                if (!DriveClient.sameDocumentIds(
                        approvedSnapshot.documentIds(), currentSnapshot.documentIds())) {
                    runOnUiThread(() -> showMessage(
                            "Folder contents changed after confirmation. Nothing was deleted; review Clear & Reuse again."));
                    return;
                }

                int deletedCount = 0;
                try {
                    for (String childId : currentSnapshot.documentIds()) {
                        driveClient.deleteDocument(getContentResolver(), treeUri, childId);
                        deletedCount++;
                    }
                } catch (Exception error) {
                    final int removed = deletedCount;
                    runOnUiThread(() -> {
                        createBlockedUntilRefresh = true;
                        showError(
                                "Clear & Reuse stopped after removing " + removed + " of "
                                        + currentSnapshot.count()
                                        + " items. The work-order folder was not renamed. Refresh and inspect before retrying",
                                error);
                    });
                    return;
                }

                try {
                    DriveClient.ChildSnapshot afterDelete = driveClient.listDirectChildren(
                            getContentResolver(), treeUri, candidateId);
                    if (afterDelete.count() != 0) {
                        throw new IOException("Drive still reports " + afterDelete.count()
                                + " child item" + (afterDelete.count() == 1 ? "" : "s") + ".");
                    }

                    DriveFolder renameResult = driveClient.renameFolder(
                            getContentResolver(), treeUri, candidateId, requestedName);
                    if (!candidateId.equals(renameResult.id())) {
                        throw new IOException("Drive rename changed the folder identity.");
                    }

                    List<DriveFolder> afterRename = driveClient.listFoldersFresh(
                            getContentResolver(), treeUri, addressId);
                    DriveFolder verified = DriveClient.findById(afterRename, candidateId);
                    if (verified == null || !verified.name().equals(requestedName)) {
                        throw new IOException("Drive rename could not be verified with the original folder identity.");
                    }
                    List<DriveFolder> verifiedMatches = DriveClient.findExactNameMatches(afterRename, requestedName);
                    if (verifiedMatches.size() != 1 || !verifiedMatches.get(0).id().equals(candidateId)) {
                        throw new IOException("The renamed folder is ambiguous.");
                    }

                    runOnUiThread(() -> {
                        if (!isStillOnAddress(addressId)) {
                            return;
                        }
                        visibleFolders.clear();
                        visibleFolders.addAll(afterRename);
                        adapter.notifyDataSetChanged();
                        selectWorkOrder(verified, "Clear & Reuse complete with the same identity");
                    });
                } catch (Exception error) {
                    runOnUiThread(() -> {
                        createBlockedUntilRefresh = true;
                        showError(
                                "Clear & Reuse is incomplete. Old items may have been removed, but the folder was not confirmed ready. Refresh and inspect before any further Drive write",
                                error);
                    });
                }
            } catch (Exception error) {
                runOnUiThread(() -> showError(
                        "Could not revalidate Clear & Reuse. Nothing was deleted", error));
            }
        });
    }

    private void openPhotoCapture() {
        if (selectedAddress == null || selectedWorkOrder == null) {
            showMessage("Select an exact work order before taking photos.");
            return;
        }

        DriveFolder savedAddress = folderPrefs.getCurrentAddress();
        DriveFolder savedWorkOrder = folderPrefs.getCurrentWorkOrder();
        if (savedAddress == null
                || savedWorkOrder == null
                || !selectedAddress.id().equals(savedAddress.id())
                || !selectedWorkOrder.id().equals(savedWorkOrder.id())) {
            showMessage("The selected work-order identity changed. Choose the work order again before taking photos.");
            return;
        }

        startActivity(new Intent(this, PhotoCaptureActivity.class));
    }

    private boolean isStillOnAddress(String addressId) {
        return screen == Screen.WORK_ORDERS
                && selectedAddress != null
                && selectedAddress.id().equals(addressId);
    }

    private void selectWorkOrder(DriveFolder folder, String message) {
        selectedWorkOrder = folder;
        folderPrefs.setCurrentWorkOrder(folder);
        renderCurrentWorkOrder();
        statusText.setText(message + ": " + folder.name());
        setNotBusy();
    }

    private void reconcileSelectedWorkOrder(List<DriveFolder> folders) {
        if (selectedWorkOrder == null) {
            renderCurrentWorkOrder();
            return;
        }
        DriveFolder actual = DriveClient.findById(folders, selectedWorkOrder.id());
        if (actual == null) {
            selectedWorkOrder = null;
            folderPrefs.clearCurrentWorkOrder();
        } else {
            selectedWorkOrder = actual;
            folderPrefs.setCurrentWorkOrder(actual);
        }
        renderCurrentWorkOrder();
    }

    private void renderCurrentWorkOrder() {
        if (currentWorkOrderText == null) {
            return;
        }
        currentWorkOrderText.setText(selectedWorkOrder == null
                ? "Selected work order: none"
                : "Selected work order: " + selectedWorkOrder.name());
    }

    private String folderLabel(DriveFolder folder) {
        if (!hasDuplicateVisibleName(folder.name())) {
            return folder.name();
        }
        return folder.name() + "  [" + shortId(folder.id()) + "]";
    }

    private boolean hasDuplicateVisibleName(String name) {
        int count = 0;
        for (DriveFolder folder : visibleFolders) {
            if (folder.name().equals(name) && ++count > 1) {
                return true;
            }
        }
        return false;
    }

    private String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(id.length() - 8);
    }

    private boolean hasPersistedReadPermission(Uri treeUri) {
        for (UriPermission permission : getContentResolver().getPersistedUriPermissions()) {
            if (treeUri.equals(permission.getUri()) && permission.isReadPermission()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPersistedWritePermission(Uri treeUri) {
        for (UriPermission permission : getContentResolver().getPersistedUriPermissions()) {
            if (treeUri.equals(permission.getUri()) && permission.isWritePermission()) {
                return true;
            }
        }
        return false;
    }

    private void renderSavedMaster() {
        DriveFolder master = folderPrefs == null ? null : folderPrefs.getMasterFolder();
        Uri treeUri = folderPrefs == null ? null : folderPrefs.getMasterTreeUri();
        boolean canRead = treeUri != null && hasPersistedReadPermission(treeUri);

        if (legacyMasterText != null) {
            legacyMasterText.setText(master == null
                    ? "Master folder: not selected"
                    : "Master folder: " + master.name());
        }

        if (homeMasterNameText != null) {
            if (master == null) {
                homeMasterNameText.setText("Google Drive");
                homeDriveStateText.setText("Not connected");
                chooseMasterButton.setText("Connect Drive");
                chooseMasterButton.setVisibility(View.VISIBLE);
                refreshAddressButton.setVisibility(View.GONE);
                driveOptionsButton.setVisibility(View.GONE);
                tintDriveStatusDot(R.color.home_text_secondary);
            } else if (canRead) {
                homeMasterNameText.setText(master.name());
                homeDriveStateText.setText("Drive connected");
                chooseMasterButton.setVisibility(View.GONE);
                refreshAddressButton.setVisibility(View.VISIBLE);
                driveOptionsButton.setVisibility(View.VISIBLE);
                tintDriveStatusDot(R.color.home_primary);
            } else {
                homeMasterNameText.setText(master.name());
                homeDriveStateText.setText("Drive access expired");
                chooseMasterButton.setText("Reconnect");
                chooseMasterButton.setVisibility(View.VISIBLE);
                refreshAddressButton.setVisibility(View.GONE);
                driveOptionsButton.setVisibility(View.VISIBLE);
                tintDriveStatusDot(R.color.home_error);
            }
        }

        if (refreshAddressButton != null) {
            refreshAddressButton.setEnabled(canRead && !busy);
        }
        renderPropertyCountAndEmptyState();
    }

    private void tintDriveStatusDot(int colorRes) {
        if (homeDriveStatusDot == null) {
            return;
        }
        ViewCompat.setBackgroundTintList(homeDriveStatusDot,
                ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));
    }

    private void renderPropertyCountAndEmptyState() {
        if (homePropertyCountText != null) {
            int count = visibleFolders.size();
            homePropertyCountText.setText(count == 1 ? "1 property" : count + " properties");
        }
        if (homeEmptyText != null) {
            Uri treeUri = folderPrefs == null ? null : folderPrefs.getMasterTreeUri();
            boolean connected = treeUri != null && hasPersistedReadPermission(treeUri);
            homeEmptyText.setVisibility(connected && !busy && visibleFolders.isEmpty()
                    ? View.VISIBLE : View.GONE);
        }
    }

    private void showHomeInlineMessage(String message) {
        if (homeStatusText == null) {
            return;
        }
        homeStatusText.setText(message == null ? "" : message);
        homeStatusText.setVisibility(message == null || message.isBlank() ? View.GONE : View.VISIBLE);
    }

    private void clearHomeInlineMessage() {
        showHomeInlineMessage(null);
    }

    private void setBusy(String message) {
        busy = true;
        if (screen == Screen.ADDRESSES) {
            clearHomeInlineMessage();
            if (homeProgress != null) {
                homeProgress.setVisibility(View.VISIBLE);
            }
        } else {
            statusText.setText(message);
        }
        chooseMasterButton.setEnabled(false);
        refreshAddressButton.setEnabled(false);
        useCreateAddressButton.setEnabled(false);
        backButton.setEnabled(false);
        refreshWorkOrdersButton.setEnabled(false);
        selectWorkOrderButton.setEnabled(false);
        workOrderInput.setEnabled(false);
        dateButton.setEnabled(false);
        useCreateButton.setEnabled(false);
        reuseEmptyButton.setEnabled(false);
        clearReuseButton.setEnabled(false);
        photosButton.setEnabled(false);
        folderList.setEnabled(false);
        renderPropertyCountAndEmptyState();
    }

    private void setNotBusy() {
        busy = false;
        Uri treeUri = folderPrefs.getMasterTreeUri();
        boolean canRead = treeUri != null && hasPersistedReadPermission(treeUri);
        boolean canWrite = treeUri != null && hasPersistedWritePermission(treeUri);

        if (homeProgress != null) {
            homeProgress.setVisibility(View.GONE);
        }
        renderWorkOrderPickerButton();
        folderList.setEnabled(canRead);
        if (screen == Screen.ADDRESSES) {
            chooseMasterButton.setEnabled(true);
            refreshAddressButton.setEnabled(canRead);
            useCreateAddressButton.setEnabled(canRead && canWrite && !createBlockedUntilRefresh);
            selectWorkOrderButton.setEnabled(false);
            photosButton.setEnabled(false);
            renderSavedMaster();
        } else {
            backButton.setEnabled(true);
            refreshWorkOrdersButton.setEnabled(canRead);
            selectWorkOrderButton.setEnabled(canRead && !visibleFolders.isEmpty());
            workOrderInput.setEnabled(canRead);
            dateButton.setEnabled(canRead);
            useCreateButton.setEnabled(canRead && canWrite && !createBlockedUntilRefresh);
            reuseEmptyButton.setEnabled(canRead && canWrite
                    && selectedWorkOrder != null
                    && !createBlockedUntilRefresh);
            clearReuseButton.setEnabled(canRead && canWrite
                    && selectedWorkOrder != null
                    && !createBlockedUntilRefresh);
            photosButton.setEnabled(selectedAddress != null && selectedWorkOrder != null);
        }
        renderPropertyCountAndEmptyState();
    }

    private void showMessage(String message) {
        if (screen == Screen.ADDRESSES) {
            showHomeInlineMessage(message);
        } else {
            statusText.setText(message);
        }
        setNotBusy();
    }

    private void showError(String prefix, Throwable error) {
        String detail = error.getMessage();
        String message = prefix + (detail == null ? "." : ": " + detail);
        if (screen == Screen.ADDRESSES) {
            showHomeInlineMessage(message);
        } else {
            statusText.setText(message);
        }
        setNotBusy();
    }

    private void applySystemBarAppearance(boolean home) {
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        if (home) {
            controller.setAppearanceLightStatusBars(!night);
            controller.setAppearanceLightNavigationBars(!night);
            int color = ContextCompat.getColor(this, R.color.home_background);
            getWindow().setStatusBarColor(color);
            getWindow().setNavigationBarColor(color);
        } else {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
            getWindow().setStatusBarColor(Color.WHITE);
            getWindow().setNavigationBarColor(Color.WHITE);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
