package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.UriPermission;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;

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

    private TextView screenTitleText;
    private TextView screenSubtitleText;
    private TextView statusText;
    private TextView masterText;
    private TextView listLabel;
    private TextView addressText;
    private TextView currentWorkOrderText;
    private LinearLayout addressControls;
    private LinearLayout workOrderControls;
    private LinearLayout maintenanceSection;
    private ScrollView workOrderScroll;
    private Button chooseMasterButton;
    private Button refreshAddressButton;
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
            statusText.setText("Connect the Google Drive master folder to begin.");
        } else if (hasPersistedReadPermission(savedTree)) {
            statusText.setText("Drive is connected. Refreshing addresses…");
            refreshAddressFolders();
        } else {
            statusText.setText("Drive access expired. Connect the master folder again.");
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
        int pad = dp(16);
        LinearLayout root = FieldUi.vertical(this);
        root.setPadding(pad, dp(10), pad, dp(12));
        FieldUi.applyPageBackground(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        backButton = FieldUi.textButton(this, "Back");
        backButton.setOnClickListener(v -> showAddressScreen(true));
        FieldUi.setButtonIcon((MaterialButton) backButton, R.drawable.ic_arrow_back_24);
        backButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        backParams.setMarginEnd(dp(6));
        header.addView(backButton, backParams);

        LinearLayout titleGroup = FieldUi.vertical(this);
        screenTitleText = FieldUi.screenTitle(this, "Field Photo Prep");
        screenSubtitleText = FieldUi.screenSubtitle(this, "Choose a property");
        titleGroup.addView(screenTitleText);
        titleGroup.addView(screenSubtitleText);
        header.addView(titleGroup, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        statusText = FieldUi.statusText(this);
        root.addView(statusText);

        addressControls = FieldUi.vertical(this);

        LinearLayout driveContent = FieldUi.vertical(this);
        TextView driveTitle = FieldUi.body(this, "Google Drive");
        driveTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        driveTitle.setTextSize(16);
        driveContent.addView(driveTitle);

        masterText = FieldUi.muted(this, "Not connected");
        masterText.setPadding(0, dp(4), 0, dp(12));
        driveContent.addView(masterText);

        LinearLayout driveButtons = new LinearLayout(this);
        driveButtons.setOrientation(LinearLayout.HORIZONTAL);

        chooseMasterButton = FieldUi.secondaryButton(this, "Connect Drive");
        chooseMasterButton.setOnClickListener(v -> chooseMasterFolder());
        driveButtons.addView(chooseMasterButton, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        refreshAddressButton = FieldUi.textButton(this, "Refresh");
        refreshAddressButton.setEnabled(false);
        refreshAddressButton.setOnClickListener(v -> refreshAddressFolders());
        FieldUi.setButtonIcon((MaterialButton) refreshAddressButton, R.drawable.ic_refresh_24);
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        refreshParams.setMarginStart(dp(8));
        driveButtons.addView(refreshAddressButton, refreshParams);
        driveContent.addView(driveButtons);

        MaterialCardView driveCard = FieldUi.paddedCard(this, driveContent);
        addressControls.addView(driveCard, FieldUi.cardParams(this));

        useCreateAddressButton = FieldUi.primaryButton(this, "New Address");
        useCreateAddressButton.setEnabled(false);
        useCreateAddressButton.setOnClickListener(v -> showAddressEntryDialog());
        FieldUi.setButtonIcon((MaterialButton) useCreateAddressButton, R.drawable.ic_add_24);
        addressControls.addView(useCreateAddressButton);
        root.addView(addressControls);

        workOrderControls = FieldUi.vertical(this);

        addressText = FieldUi.muted(this, "");
        addressText.setPadding(dp(2), 0, 0, dp(8));
        workOrderControls.addView(addressText);

        LinearLayout selectedContent = FieldUi.vertical(this);
        TextView selectedHeading = FieldUi.muted(this, "SELECTED WORK ORDER");
        selectedHeading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        selectedContent.addView(selectedHeading);

        currentWorkOrderText = FieldUi.body(this, "No work order selected");
        currentWorkOrderText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        currentWorkOrderText.setTextSize(17);
        currentWorkOrderText.setPadding(0, dp(4), 0, dp(12));
        selectedContent.addView(currentWorkOrderText);

        photosButton = FieldUi.primaryButton(this, "Open Photos");
        photosButton.setEnabled(false);
        photosButton.setOnClickListener(v -> openPhotoCapture());
        selectedContent.addView(photosButton);
        workOrderControls.addView(FieldUi.paddedCard(this, selectedContent), FieldUi.cardParams(this));

        LinearLayout createContent = FieldUi.vertical(this);
        TextView createHeading = FieldUi.body(this, "New dated work order");
        createHeading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        createHeading.setTextSize(16);
        createContent.addView(createHeading);
        TextView createHelp = FieldUi.muted(this, "Enter the work type and choose the service date.");
        createHelp.setPadding(0, dp(3), 0, dp(12));
        createContent.addView(createHelp);

        TextInputLayout workOrderInputLayout = FieldUi.input(this, "Work order name — e.g. Cut Grass");
        workOrderInput = (EditText) workOrderInputLayout.getEditText();
        createContent.addView(workOrderInputLayout);

        dateButton = FieldUi.secondaryButton(this, "Date");
        dateButton.setOnClickListener(v -> chooseWorkOrderDate());
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dateParams.topMargin = dp(10);
        createContent.addView(dateButton, dateParams);

        useCreateButton = FieldUi.primaryButton(this, "Use / Create Work Order");
        useCreateButton.setOnClickListener(v -> useOrCreateWorkOrder());
        LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        createParams.topMargin = dp(8);
        createContent.addView(useCreateButton, createParams);
        workOrderControls.addView(FieldUi.paddedCard(this, createContent), FieldUi.cardParams(this));

        LinearLayout manageContent = FieldUi.vertical(this);
        TextView manageHeading = FieldUi.body(this, "Work-order tools");
        manageHeading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        manageContent.addView(manageHeading);
        TextView manageHelp = FieldUi.muted(this,
                "Selection and folder reuse tools are kept here so normal field work stays simple.");
        manageHelp.setPadding(0, dp(3), 0, dp(10));
        manageContent.addView(manageHelp);

        selectWorkOrderButton = FieldUi.secondaryButton(this, "Select Existing Work Order");
        selectWorkOrderButton.setEnabled(false);
        selectWorkOrderButton.setOnClickListener(v -> showWorkOrderPicker());
        manageContent.addView(selectWorkOrderButton);

        MaterialButton manageButton = FieldUi.textButton(this, "Manage selected folder");
        manageContent.addView(manageButton);

        maintenanceSection = FieldUi.vertical(this);
        maintenanceSection.setVisibility(View.GONE);

        reuseEmptyButton = FieldUi.secondaryButton(this, "Reuse Selected Empty Folder");
        reuseEmptyButton.setOnClickListener(v -> reuseSelectedEmptyFolder());
        maintenanceSection.addView(reuseEmptyButton);

        clearReuseButton = FieldUi.dangerButton(this, "Clear & Reuse Selected Folder");
        clearReuseButton.setOnClickListener(v -> prepareClearAndReuse());
        LinearLayout.LayoutParams dangerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dangerParams.topMargin = dp(8);
        maintenanceSection.addView(clearReuseButton, dangerParams);
        manageContent.addView(maintenanceSection);

        manageButton.setOnClickListener(v -> maintenanceSection.setVisibility(
                maintenanceSection.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        refreshWorkOrdersButton = FieldUi.textButton(this, "Refresh work orders");
        refreshWorkOrdersButton.setOnClickListener(v -> refreshWorkOrderFolders());
        FieldUi.setButtonIcon((MaterialButton) refreshWorkOrdersButton, R.drawable.ic_refresh_24);
        manageContent.addView(refreshWorkOrdersButton);

        workOrderControls.addView(FieldUi.paddedCard(this, manageContent), FieldUi.cardParams(this));

        workOrderScroll = new ScrollView(this);
        workOrderScroll.setFillViewport(false);
        workOrderScroll.setVisibility(View.GONE);
        workOrderScroll.addView(workOrderControls, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(workOrderScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.95f));

        listLabel = FieldUi.sectionTitle(this, "Addresses");
        root.addView(listLabel);

        folderList = new ListView(this);
        folderList.setDivider(new ColorDrawable(Color.TRANSPARENT));
        folderList.setDividerHeight(dp(10));
        folderList.setClipToPadding(false);
        folderList.setPadding(0, 0, 0, dp(12));
        adapter = new ArrayAdapter<DriveFolder>(this, android.R.layout.simple_list_item_1, visibleFolders) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                DriveFolder folder = getItem(position);
                MaterialCardView card = FieldUi.card(MainActivity.this);
                LinearLayout content = FieldUi.vertical(MainActivity.this);
                int cardPad = dp(16);
                content.setPadding(cardPad, dp(13), cardPad, dp(13));

                TextView name = FieldUi.body(MainActivity.this,
                        folder == null ? "" : folder.name());
                name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                name.setTextSize(16);
                content.addView(name);

                String meta;
                if (folder == null) {
                    meta = "";
                } else if (screen == Screen.ADDRESSES) {
                    meta = "Tap to view work orders";
                } else if (selectedWorkOrder != null && selectedWorkOrder.id().equals(folder.id())) {
                    meta = "Selected work order";
                    card.setCardBackgroundColor(FieldUi.color(
                            MainActivity.this, R.color.fpp_primary_container));
                    card.setStrokeColor(FieldUi.color(
                            MainActivity.this, R.color.fpp_primary));
                } else {
                    meta = "Tap to select";
                }
                if (folder != null && hasDuplicateVisibleName(folder.name())) {
                    meta += "  •  ID …" + shortId(folder.id());
                }
                TextView metaText = FieldUi.muted(MainActivity.this, meta);
                metaText.setPadding(0, dp(4), 0, 0);
                content.addView(metaText);
                card.addView(content);
                return card;
            }
        };
        folderList.setAdapter(adapter);
        folderList.setOnItemClickListener((parent, view, position, id) -> {
            DriveFolder folder = visibleFolders.get(position);
            if (screen == Screen.ADDRESSES) {
                openAddress(folder);
            } else {
                selectWorkOrder(folder, "Work order selected");
            }
        });
        root.addView(folderList, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.05f));

        setContentView(root);
        updateDateButton();
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
                    statusText.setText(folders.size() + " address folder"
                            + (folders.size() == 1 ? "" : "s") + " found.");
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
                        statusText.setText(matches.size() + " address folders named " + requestedName
                                + " already exist. Tap the intended one; no folder was created.");
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
                        statusText.setText("Address create result is ambiguous. Tap the intended same-named folder; no additional folder will be created until refresh.");
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
        addressControls.setVisibility(View.GONE);
        workOrderScroll.setVisibility(View.VISIBLE);
        listLabel.setVisibility(View.VISIBLE);
        folderList.setVisibility(View.VISIBLE);
        listLabel.setText("Existing work orders");
        backButton.setVisibility(View.VISIBLE);
        screenTitleText.setText(address.name());
        screenSubtitleText.setText("Work orders");
        addressText.setText("Select an existing work order below or create a new dated one.");
        renderCurrentWorkOrder();
        renderWorkOrderPickerButton();
        refreshWorkOrderFolders();
    }

    private void showAddressScreen(boolean refresh) {
        screen = Screen.ADDRESSES;
        selectedAddress = null;
        selectedWorkOrder = null;
        createBlockedUntilRefresh = false;
        addressControls.setVisibility(View.VISIBLE);
        workOrderScroll.setVisibility(View.GONE);
        listLabel.setVisibility(View.VISIBLE);
        folderList.setVisibility(View.VISIBLE);
        listLabel.setText("Addresses");
        backButton.setVisibility(View.GONE);
        screenTitleText.setText("Field Photo Prep");
        screenSubtitleText.setText("Choose a property");
        if (maintenanceSection != null) {
            maintenanceSection.setVisibility(View.GONE);
        }
        visibleFolders.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        renderSavedMaster();
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
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
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
                ? "No work order selected"
                : selectedWorkOrder.name());
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
        if (masterText != null) {
            masterText.setText(master == null
                    ? "Not connected — choose the approved master folder."
                    : "Connected to " + master.name());
        }
        if (refreshAddressButton != null) {
            Uri treeUri = folderPrefs == null ? null : folderPrefs.getMasterTreeUri();
            refreshAddressButton.setEnabled(treeUri != null && hasPersistedReadPermission(treeUri));
        }
    }

    private void setBusy(String message) {
        statusText.setText(message);
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
    }

    private void setNotBusy() {
        Uri treeUri = folderPrefs.getMasterTreeUri();
        boolean canRead = treeUri != null && hasPersistedReadPermission(treeUri);
        boolean canWrite = treeUri != null && hasPersistedWritePermission(treeUri);

        renderWorkOrderPickerButton();
        folderList.setEnabled(canRead);
        if (screen == Screen.ADDRESSES) {
            chooseMasterButton.setEnabled(true);
            refreshAddressButton.setEnabled(canRead);
            useCreateAddressButton.setEnabled(canRead && canWrite && !createBlockedUntilRefresh);
            selectWorkOrderButton.setEnabled(false);
            photosButton.setEnabled(false);
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
