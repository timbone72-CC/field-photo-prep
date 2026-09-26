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
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int REQUEST_MASTER_FOLDER = 1001;

    private enum Screen {
        ADDRESSES,
        WORK_ORDERS
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private DriveClient driveClient;
    private AuthorizationActionGuard authorizationGuard;
    private OrganizationDriveBindingGuard driveBindingGuard;
    private OrganizationDriveBindingGuard.State lastDriveBindingState;
    private final List<DriveFolder> companyFolders = new ArrayList<>();
    private final List<DriveFolder> propertyFolders = new ArrayList<>();
    private final List<DriveFolder> workOrderFolders = new ArrayList<>();
    private final Map<String, Integer> protectedPhotoCountByAddressId = new HashMap<>();
    private final Map<String, Integer> protectedPhotoCountByWorkOrderId = new HashMap<>();

    private FolderPrefs folderPrefs;
    private Screen screen = Screen.ADDRESSES;
    private DriveFolder selectedAddress;
    private DriveFolder selectedWorkOrder;
    private LocalDate selectedDate = LocalDate.now();
    private boolean createBlockedUntilRefresh;
    private boolean companyWriteBlockedUntilRefresh;
    private boolean busy;

    private FrameLayout appRoot;
    private View homeRoot;
    private LinearLayout legacyRoot;

    private TextView statusText;
    private TextView homeStatusText;
    private TextView legacyStatusText;
    private TextView legacyMasterText;
    private TextView homeMasterNameText;
    private TextView homeCompanyChevronText;
    private TextView homeDriveStateText;
    private TextView homePropertyCountText;
    private TextView homeEmptyText;
    private View homeDriveStatusDot;
    private View homeCompanyClickTarget;
    private ProgressBar homeProgress;
    private ImageButton driveOptionsButton;
    private Button homeNextActionButton;

    private TextView addressText;
    private TextView currentWorkOrderText;
    private LinearLayout workOrderControls;
    private ScrollView workOrderScroll;
    private Button chooseMasterButton;
    private ImageButton refreshAddressButton;
    private Button useCreateAddressButton;
    private Button backButton;
    private Button refreshWorkOrdersButton;
    private Button dateButton;
    private Button useCreateButton;
    private Button reuseEmptyButton;
    private Button clearReuseButton;
    private Button photosButton;
    private ListView workOrderList;
    private WorkOrderListAdapter workOrderAdapter;
    private LinearLayout maintenanceControls;
    private Button maintenanceButton;
    private Button homeNavWorkOrdersButton;
    private Button homeNavPhotosButton;
    private Button workNavHomeButton;
    private Button workNavPhotosButton;
    private EditText workOrderInput;
    private ListView folderList;
    private ArrayAdapter<DriveFolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FieldPhotoPrepApplication app = (FieldPhotoPrepApplication) getApplication();
        authorizationGuard = new AuthorizationActionGuard(app.authorizationManager());
        driveClient = new DriveClient(authorizationGuard);
        folderPrefs = new FolderPrefs(this);
        driveBindingGuard = new OrganizationDriveBindingGuard(
                folderPrefs,
                authorizationGuard,
                this::hasPersistedReadPermission);
        buildUi();
        showAddressScreen(false);
        renderSavedMaster();

        restoreSavedDriveIfUsable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FieldPhotoPrepApplication app = (FieldPhotoPrepApplication) getApplication();
        RuntimeAuthorizationManager manager = app.authorizationManager();

        // Re-evaluate immediately from the stored session before rendering any Drive state.
        // This is essential when AuthActivity replaced the current User/Organization while this
        // MainActivity instance remained underneath it on the Android back stack.
        reconcileDriveBindingForCurrentOrganization();

        if (manager != null) {
            manager.revalidateAsync().whenComplete((decision, error) ->
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            reconcileDriveBindingForCurrentOrganization();
                        }
                    }));
        }
        refreshProtectedPhotoCounts();
    }

    private void reconcileDriveBindingForCurrentOrganization() {
        if (folderPrefs == null || driveBindingGuard == null) {
            return;
        }

        OrganizationDriveBindingGuard.Result binding = driveBindingGuard.current();
        OrganizationDriveBindingGuard.State previousState = lastDriveBindingState;
        lastDriveBindingState = binding.state();

        if (binding.state() == OrganizationDriveBindingGuard.State.NO_WORKSPACE) {
            // Ordinary no-workspace state does not represent cross-Organization leakage.
            // Preserve the current local screen/navigation state when returning from Photos or
            // Account so existing Work Orders UI behavior is unchanged.
            renderSavedMaster();
            setNotBusy();
            return;
        }

        if (!binding.isUsable()) {
            // These are process-memory navigation caches only. Persisted provider identity,
            // Organization binding metadata, SAF grants, and queued-photo destinations remain
            // untouched so the owning Organization can safely recover them later.
            companyFolders.clear();
            propertyFolders.clear();
            workOrderFolders.clear();
            selectedAddress = null;
            selectedWorkOrder = null;
            showAddressScreen(false);
            showHomeInlineMessage(binding.message());
            renderPropertyCountAndEmptyState();
            return;
        }

        clearHomeInlineMessage();
        if (DriveBindingResumePolicy.shouldReload(previousState, binding.state())) {
            if (folderPrefs.hasWorkspace()) {
                refreshCompanyFolders(true, false);
            } else {
                refreshAddressFolders();
            }
            return;
        }

        renderSavedMaster();
        setNotBusy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String destination = intent.getStringExtra("field_tab");
        if ("home".equals(destination)) {
            showAddressScreen(true);
        } else if ("work_orders".equals(destination) && screen == Screen.ADDRESSES) {
            openSavedPropertyFromHome();
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
    homeCompanyChevronText = homeRoot.findViewById(R.id.home_company_chevron);
    homeDriveStateText = homeRoot.findViewById(R.id.home_drive_state);
    homePropertyCountText = homeRoot.findViewById(R.id.home_property_count);
    homeEmptyText = homeRoot.findViewById(R.id.home_empty_text);
    homeDriveStatusDot = homeRoot.findViewById(R.id.drive_status_dot);
    homeCompanyClickTarget = homeRoot.findViewById(R.id.home_company_click_target);
    homeProgress = homeRoot.findViewById(R.id.home_progress);
    driveOptionsButton = homeRoot.findViewById(R.id.home_drive_options_button);
    homeNextActionButton = homeRoot.findViewById(R.id.home_next_action);

    chooseMasterButton = homeRoot.findViewById(R.id.home_connect_button);
    refreshAddressButton = homeRoot.findViewById(R.id.home_refresh_button);
    useCreateAddressButton = homeRoot.findViewById(R.id.home_new_address_button);
    folderList = homeRoot.findViewById(R.id.home_property_list);
    homeNavWorkOrdersButton = homeRoot.findViewById(R.id.nav_work_orders);
    homeNavPhotosButton = homeRoot.findViewById(R.id.nav_photos);
    homeRoot.findViewById(R.id.nav_home).setSelected(true);

    chooseMasterButton.setOnClickListener(v -> chooseMasterFolder());
    refreshAddressButton.setOnClickListener(v -> refreshHomeFolders());
    useCreateAddressButton.setOnClickListener(v -> showAddressEntryDialog());
    driveOptionsButton.setOnClickListener(this::showDriveOptions);
    homeCompanyClickTarget.setOnClickListener(v -> openCompanySwitcher());
    homeNavWorkOrdersButton.setOnClickListener(v -> openSavedPropertyFromHome());
    homeNavPhotosButton.setOnClickListener(v -> openSavedPhotosFromHome());

    adapter = new PropertyListAdapter(
            this,
            propertyFolders,
            protectedPhotoCountByAddressId);
    folderList.setAdapter(adapter);
    folderList.setOnItemClickListener((parent, view, position, id) -> {
        if (screen != Screen.ADDRESSES || busy || position < 0 || position >= propertyFolders.size()) {
            return;
        }
        openAddress(propertyFolders.get(position));
    });

    ViewCompat.setOnApplyWindowInsetsListener(homeRoot, (view, insets) -> {
        var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        view.setPadding(0, bars.top, 0, bars.bottom);
        return insets;
    });
    ViewCompat.requestApplyInsets(homeRoot);
}

private void openSavedPropertyFromHome() {
    DriveFolder saved = folderPrefs.getCurrentAddress();
    if (saved == null) {
        showHomeInlineMessage("Choose a property first.");
        return;
    }
    DriveFolder actual = DriveClient.findById(propertyFolders, saved.id());
    if (actual == null) {
        showHomeInlineMessage("Refresh Properties, then choose the property you want to open.");
        return;
    }
    openAddress(actual);
}

private void openSavedPhotosFromHome() {
    DriveFolder savedAddress = folderPrefs.getCurrentAddress();
    DriveFolder savedWorkOrder = folderPrefs.getCurrentWorkOrder();
    if (savedAddress == null || savedWorkOrder == null) {
        showHomeInlineMessage("Choose a property and work order before opening Photos.");
        return;
    }
    startActivity(new Intent(this, PhotoCaptureActivity.class));
}


    private void openCompanySwitcher() {
        if (busy) {
            showHomeInlineMessage("Wait for the current Drive operation to finish.");
            return;
        }
        if (!folderPrefs.hasWorkspace()) {
            showHomeInlineMessage("Set up a company workspace before switching companies.");
            return;
        }
        showCompanyChooser();
    }

    private void showDriveOptions(View anchor) {
        if (busy) {
            showHomeInlineMessage("Wait for the current Drive operation to finish.");
            return;
        }

        final CharSequence[] items = DriveOptionsPolicy.items(
                driveBindingGuard.current().isUsable(),
                folderPrefs.hasWorkspace(),
                folderPrefs.getCurrentCompany() != null);

        new AlertDialog.Builder(this)
                .setTitle("App & Drive")
                .setItems(items, (dialog, which) -> {
                    String action = items[which].toString();
                    switch (action) {
                        case "Choose Company":
                        case "Switch Company":
                            showCompanyChooser();
                            break;
                        case "Add Company":
                            showAddCompanyDialog();
                            break;
                        case "Edit Company":
                            showEditCompanyDialog();
                            break;
                        case "Account":
                            startActivity(new Intent(this, AuthActivity.class));
                            break;
                        case "Set Up Companies":
                        case "Change Workspace":
                        default:
                            chooseMasterFolder();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

private void buildLegacyWorkOrderUi() {
    legacyRoot = (LinearLayout) LayoutInflater.from(this)
            .inflate(R.layout.screen_work_orders, appRoot, false);
    legacyStatusText = legacyRoot.findViewById(R.id.work_order_status);
    legacyMasterText = legacyRoot.findViewById(R.id.work_order_master_text);
    addressText = legacyRoot.findViewById(R.id.work_order_address);
    currentWorkOrderText = legacyRoot.findViewById(R.id.work_order_current);
    workOrderScroll = legacyRoot.findViewById(R.id.work_order_scroll);
    workOrderInput = legacyRoot.findViewById(R.id.work_order_name_input);
    backButton = legacyRoot.findViewById(R.id.work_order_back);
    refreshWorkOrdersButton = legacyRoot.findViewById(R.id.work_order_refresh);
    dateButton = legacyRoot.findViewById(R.id.work_order_date_button);
    useCreateButton = legacyRoot.findViewById(R.id.work_order_create_button);
    reuseEmptyButton = legacyRoot.findViewById(R.id.work_order_reuse_empty);
    clearReuseButton = legacyRoot.findViewById(R.id.work_order_clear_reuse);
    photosButton = legacyRoot.findViewById(R.id.work_order_photos);
    workOrderList = legacyRoot.findViewById(R.id.work_order_list);
    maintenanceControls = legacyRoot.findViewById(R.id.work_order_maintenance);
    maintenanceButton = legacyRoot.findViewById(R.id.work_order_maintenance_toggle);
    workNavHomeButton = legacyRoot.findViewById(R.id.nav_home);
    workNavPhotosButton = legacyRoot.findViewById(R.id.nav_photos);
    legacyRoot.findViewById(R.id.nav_work_orders).setSelected(true);

    workOrderControls = (LinearLayout) workOrderScroll.getChildAt(0);
    workOrderAdapter = new WorkOrderListAdapter(
            this,
            workOrderFolders,
            protectedPhotoCountByWorkOrderId);
    workOrderList.setAdapter(workOrderAdapter);
    workOrderList.setOnItemClickListener((parent, view, position, id) -> {
        if (busy || position < 0 || position >= workOrderFolders.size()) {
            return;
        }
        DriveFolder candidate = workOrderFolders.get(position);
        if (selectedAddress == null || candidate.id().equals(selectedAddress.id())) {
            workOrderFolders.clear();
            selectedWorkOrder = null;
            folderPrefs.clearCurrentWorkOrder();
            createBlockedUntilRefresh = true;
            notifyFolderAdapters();
            renderCurrentWorkOrder();
            showMessage("Drive returned the selected property as a work order. Refresh before continuing.");
            return;
        }
        selectWorkOrder(candidate, "Work order selected");
    });

    backButton.setOnClickListener(v -> showAddressScreen(true));
    refreshWorkOrdersButton.setOnClickListener(v -> refreshWorkOrderFolders());
    dateButton.setOnClickListener(v -> chooseWorkOrderDate());
    useCreateButton.setOnClickListener(v -> useOrCreateWorkOrder());
    reuseEmptyButton.setOnClickListener(v -> reuseSelectedEmptyFolder());
    clearReuseButton.setOnClickListener(v -> prepareClearAndReuse());
    photosButton.setOnClickListener(v -> openPhotoCapture());
    maintenanceButton.setOnClickListener(v -> maintenanceControls.setVisibility(
            maintenanceControls.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
    workNavHomeButton.setOnClickListener(v -> showAddressScreen(true));
    workNavPhotosButton.setOnClickListener(v -> openPhotoCapture());

    ViewCompat.setOnApplyWindowInsetsListener(legacyRoot, (view, insets) -> {
        var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        view.setPadding(dp(14), bars.top, dp(14), bars.bottom);
        return insets;
    });
    ViewCompat.requestApplyInsets(legacyRoot);
    legacyRoot.setVisibility(View.GONE);
}


    private void restoreSavedDriveIfUsable() {
        OrganizationDriveBindingGuard.Result binding = driveBindingGuard.current();
        lastDriveBindingState = binding.state();
        if (!binding.isUsable()) {
            showHomeInlineMessage(binding.message());
            renderSavedMaster();
            return;
        }
        clearHomeInlineMessage();
        if (folderPrefs.hasWorkspace()) {
            refreshCompanyFolders(true, false);
        } else {
            refreshAddressFolders();
        }
    }

    private Uri usableDriveTreeOrMessage() {
        try {
            return driveBindingGuard.requireUsableTreeUri();
        } catch (IOException error) {
            showMessage(error.getMessage());
            renderSavedMaster();
            return null;
        }
    }

    private void chooseMasterFolder() {
        if (busy) {
            showHomeInlineMessage("Wait for the current Drive operation to finish.");
            return;
        }
        try {
            driveBindingGuard.requireValidatedOrganizationForBinding();
        } catch (IOException error) {
            showHomeInlineMessage(error.getMessage());
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Choose Company Workspace")
                .setMessage("Select the Drive folder that contains your company folders. Choose the parent above individual companies, not a company or address folder.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Choose Workspace", (dialog, which) -> launchWorkspaceFolderPicker())
                .show();
    }

    private void launchWorkspaceFolderPicker() {
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
            DriveFolder workspace = driveClient.getTreeFolder(getContentResolver(), treeUri);
            String organizationId = driveBindingGuard.requireValidatedOrganizationForBinding();
            folderPrefs.bindSelectedDriveRoot(treeUri, workspace, organizationId);
            companyFolders.clear();
            propertyFolders.clear();
            notifyFolderAdapters();
            showAddressScreen(false);
            renderSavedMaster();
            if (folderPrefs.hasWorkspace()) {
                refreshCompanyFolders(true, false);
            } else {
                refreshAddressFolders();
            }
        } catch (Exception error) {
            showError("Could not keep access to that workspace", error);
        }
    }

    private void refreshHomeFolders() {
        if (folderPrefs.hasWorkspace() && folderPrefs.getCurrentCompany() == null) {
            refreshCompanyFolders(false, false);
        } else {
            refreshAddressFolders();
        }
    }

    private void refreshCompanyFolders(boolean restoreLegacyCompany, boolean showChooserAfter) {
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
            return;
        }
        DriveFolder workspace = folderPrefs.getWorkspaceFolder();
        if (workspace == null) {
            showMessage("Choose the field-work workspace first.");
            return;
        }
        if (!hasPersistedReadPermission(treeUri)) {
            showMessage("Workspace access expired. Choose it again.");
            return;
        }

        companyWriteBlockedUntilRefresh = false;
        setBusy("Refreshing companies…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFolders(
                        getContentResolver(), treeUri, workspace.id());
                runOnUiThread(() -> {
                    companyFolders.clear();
                    companyFolders.addAll(folders);

                    DriveFolder selected = folderPrefs.getCurrentCompany();
                    DriveFolder actual = selected == null
                            ? null
                            : DriveClient.findById(folders, selected.id());

                    if (actual == null && restoreLegacyCompany) {
                        DriveFolder legacy = folderPrefs.getLegacyMasterFolder();
                        if (legacy != null) {
                            actual = DriveClient.findById(folders, legacy.id());
                        }
                    }

                    propertyFolders.clear();
                    notifyFolderAdapters();

                    if (actual != null) {
                        folderPrefs.setCurrentCompany(actual);
                    } else {
                        folderPrefs.clearCurrentCompany();
                    }

                    renderSavedMaster();
                    setNotBusy();

                    if (showChooserAfter) {
                        if (companyFolders.isEmpty()) {
                            showHomeInlineMessage("No companies are in this workspace yet. Use Add Company.");
                        } else {
                            showCompanyChoiceDialog(companyFolders, "Choose Company");
                        }
                    } else if (actual != null) {
                        refreshAddressFolders();
                    } else {
                        showHomeInlineMessage(companyFolders.isEmpty()
                                ? "No companies yet. Use ⋯ → Add Company."
                                : "Choose a company to see its properties.");
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    companyWriteBlockedUntilRefresh = true;
                    showError("Could not read company folders", error);
                });
            }
        });
    }

    private void showCompanyChooser() {
        if (!folderPrefs.hasWorkspace()) {
            chooseMasterFolder();
            return;
        }
        refreshCompanyFolders(false, true);
    }

    private void showCompanyChoiceDialog(List<DriveFolder> choices, String title) {
        if (choices == null || choices.isEmpty()) {
            showHomeInlineMessage("No companies are available.");
            return;
        }

        CharSequence[] labels = new CharSequence[choices.size()];
        for (int i = 0; i < choices.size(); i++) {
            DriveFolder choice = choices.get(i);
            int sameNameCount = 0;
            for (DriveFolder candidate : choices) {
                if (candidate.name().equals(choice.name())) {
                    sameNameCount++;
                }
            }
            labels[i] = sameNameCount > 1
                    ? choice.name() + " • " + shortFolderId(choice.id())
                    : choice.name();
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(labels, (dialog, which) -> selectCompany(choices.get(which)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String shortFolderId(String id) {
        if (id == null || id.length() <= 6) {
            return id == null ? "unknown" : id;
        }
        return id.substring(id.length() - 6);
    }

    private void selectCompany(DriveFolder company) {
        folderPrefs.setCurrentCompany(company);
        resetWorkOrderDraft();
        selectedAddress = null;
        selectedWorkOrder = null;
        propertyFolders.clear();
        workOrderFolders.clear();
        notifyFolderAdapters();
        renderSavedMaster();
        clearHomeInlineMessage();
        refreshAddressFolders();
    }

    private void showAddCompanyDialog() {
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
            return;
        }
        DriveFolder workspace = folderPrefs.getWorkspaceFolder();
        if (workspace == null) {
            showMessage("Set up the company workspace first.");
            return;
        }
        if (!hasPersistedReadPermission(treeUri) || !hasPersistedWritePermission(treeUri)) {
            showMessage("The workspace needs read/write access before a company can be added.");
            return;
        }
        if (companyWriteBlockedUntilRefresh) {
            showMessage("Refresh companies before trying another company write.");
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Company name");
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle("Add Company")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Use / Create", (dialog, which) ->
                        useOrCreateCompany(input.getText().toString()))
                .show();
    }

    private void useOrCreateCompany(String rawName) {
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
            return;
        }
        DriveFolder workspace = folderPrefs.getWorkspaceFolder();
        if (workspace == null) {
            showMessage("Set up the company workspace first.");
            return;
        }
        if (!hasPersistedReadPermission(treeUri) || !hasPersistedWritePermission(treeUri)) {
            showMessage("The workspace needs read/write access before a company can be added.");
            return;
        }
        if (companyWriteBlockedUntilRefresh) {
            showMessage("Refresh companies before trying another company write.");
            return;
        }

        final String requestedName;
        try {
            requestedName = CompanyFolderName.build(rawName);
        } catch (IllegalArgumentException error) {
            showMessage(error.getMessage());
            return;
        }

        setBusy("Checking for company " + requestedName + "…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, workspace.id());
                List<DriveFolder> matches = DriveClient.findExactNameMatches(folders, requestedName);

                if (matches.size() > 1) {
                    runOnUiThread(() -> {
                        companyFolders.clear();
                        companyFolders.addAll(folders);
                        companyWriteBlockedUntilRefresh = false;
                        setNotBusy();
                        showCompanyChoiceDialog(matches,
                                matches.size() + " companies named " + requestedName);
                    });
                    return;
                }

                if (matches.size() == 1) {
                    DriveFolder existing = matches.get(0);
                    runOnUiThread(() -> {
                        companyFolders.clear();
                        companyFolders.addAll(folders);
                        setNotBusy();
                        selectCompany(existing);
                    });
                    return;
                }

                authorizationGuard.requireDriveMutation();
                DriveFolder created = driveClient.createFolder(
                        getContentResolver(), treeUri, workspace.id(), requestedName);
                List<DriveFolder> afterCreate = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, workspace.id());
                DriveFolder verified = DriveClient.findById(afterCreate, created.id());
                List<DriveFolder> verifiedMatches =
                        DriveClient.findExactNameMatches(afterCreate, requestedName);

                if (verified == null
                        || !verified.name().equals(requestedName)
                        || verifiedMatches.size() != 1
                        || !verifiedMatches.get(0).id().equals(created.id())) {
                    throw new IOException("Drive could not verify exactly one new company under the workspace.");
                }

                runOnUiThread(() -> {
                    companyFolders.clear();
                    companyFolders.addAll(afterCreate);
                    companyWriteBlockedUntilRefresh = false;
                    setNotBusy();
                    selectCompany(verified);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    companyWriteBlockedUntilRefresh = true;
                    showError("Company create result is not safe to repeat. Refresh companies before trying again", error);
                });
            }
        });
    }

    private void showEditCompanyDialog() {
        DriveFolder company = folderPrefs.getCurrentCompany();
        if (company == null) {
            showMessage("Choose a company first.");
            return;
        }
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
            return;
        }
        if (!hasPersistedWritePermission(treeUri)) {
            showMessage("The workspace needs read/write access before a company can be edited.");
            return;
        }
        if (companyWriteBlockedUntilRefresh) {
            showMessage("Refresh companies before trying another company write.");
            return;
        }

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(company.name());
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this)
                .setTitle("Edit Company")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Rename", (dialog, which) ->
                        renameCurrentCompany(input.getText().toString()))
                .show();
    }

    private void renameCurrentCompany(String rawName) {
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
            return;
        }
        DriveFolder workspace = folderPrefs.getWorkspaceFolder();
        DriveFolder company = folderPrefs.getCurrentCompany();
        if (workspace == null || company == null) {
            showMessage("Choose a company first.");
            return;
        }
        if (!hasPersistedReadPermission(treeUri) || !hasPersistedWritePermission(treeUri)) {
            showMessage("The workspace needs read/write access before a company can be edited.");
            return;
        }
        if (companyWriteBlockedUntilRefresh) {
            showMessage("Refresh companies before trying another company write.");
            return;
        }

        final String requestedName;
        try {
            requestedName = CompanyFolderName.build(rawName);
        } catch (IllegalArgumentException error) {
            showMessage(error.getMessage());
            return;
        }

        if (requestedName.equals(company.name())) {
            showMessage("Company name is unchanged.");
            return;
        }

        final String companyId = company.id();
        setBusy("Checking company name…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, workspace.id());
                DriveFolder actual = DriveClient.findById(folders, companyId);
                if (actual == null) {
                    throw new IOException("The selected company is no longer present under this workspace.");
                }

                List<DriveFolder> matches = DriveClient.findExactNameMatches(folders, requestedName);
                for (DriveFolder match : matches) {
                    if (!match.id().equals(companyId)) {
                        runOnUiThread(() -> {
                            companyWriteBlockedUntilRefresh = false;
                            setNotBusy();
                            showHomeInlineMessage("Another company already uses that exact name. Nothing was renamed.");
                        });
                        return;
                    }
                }

                authorizationGuard.requireDriveMutation();
                DriveFolder renamed = driveClient.renameFolder(
                        getContentResolver(), treeUri, companyId, requestedName);
                if (!companyId.equals(renamed.id())) {
                    throw new IOException("Drive rename changed the company folder identity.");
                }

                List<DriveFolder> afterRename = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, workspace.id());
                DriveFolder verified = DriveClient.findById(afterRename, companyId);
                List<DriveFolder> verifiedMatches =
                        DriveClient.findExactNameMatches(afterRename, requestedName);
                if (verified == null
                        || !verified.name().equals(requestedName)
                        || verifiedMatches.size() != 1
                        || !companyId.equals(verifiedMatches.get(0).id())) {
                    throw new IOException("Drive did not verify one unambiguous renamed company with the original identity.");
                }

                runOnUiThread(() -> {
                    companyFolders.clear();
                    companyFolders.addAll(afterRename);
                    folderPrefs.setCurrentCompany(verified);
                    companyWriteBlockedUntilRefresh = false;
                    renderSavedMaster();
                    setNotBusy();
                    showHomeSuccessMessage("Company renamed to " + verified.name() + ".");
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    companyWriteBlockedUntilRefresh = true;
                    showError("Company rename could not be verified. Refresh companies before trying again", error);
                });
            }
        });
    }

    private void refreshAddressFolders() {
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
            return;
        }
        DriveFolder master = folderPrefs.getMasterFolder();
        if (master == null) {
            showMessage(folderPrefs.hasWorkspace()
                    ? "Choose a company first."
                    : "Choose a master folder first.");
            return;
        }
        if (!hasPersistedReadPermission(treeUri)) {
            showMessage("Drive access expired. Choose the workspace again.");
            return;
        }

        createBlockedUntilRefresh = false;
        final String parentId = master.id();
        setBusy("Refreshing address folders…");
        executor.execute(() -> {
            try {
                List<DriveFolder> folders = driveClient.listFolders(
                        getContentResolver(), treeUri, parentId);
                runOnUiThread(() -> {
                    propertyFolders.clear();
                    propertyFolders.addAll(folders);
                    notifyFolderAdapters();
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
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
            return;
        }
        DriveFolder master = folderPrefs.getMasterFolder();
        if (master == null) {
            showMessage(folderPrefs.hasWorkspace()
                    ? "Choose a company first."
                    : "Choose a master folder first.");
            return;
        }
        if (!hasPersistedReadPermission(treeUri) || !hasPersistedWritePermission(treeUri)) {
            showMessage("The selected Drive location needs read/write access before an address can be created.");
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
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
            return;
        }
        DriveFolder master = folderPrefs.getMasterFolder();
        if (master == null) {
            showMessage("Choose a master folder first.");
            return;
        }
        if (!hasPersistedReadPermission(treeUri)) {
            showMessage("Drive access expired. Choose the workspace again.");
            return;
        }
        if (!hasPersistedWritePermission(treeUri)) {
            showMessage("This Drive workspace is read-only. Choose it again and allow write access.");
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
                List<DriveFolder> possibleMatches =
                        AddressFolderAmbiguity.findPossibleMatches(folders, requestedName);

                if (matches.size() > 1) {
                    runOnUiThread(() -> {
                        if (screen != Screen.ADDRESSES) {
                            return;
                        }
                        propertyFolders.clear();
                        propertyFolders.addAll(folders);
                        notifyFolderAdapters();
                        renderPropertyCountAndEmptyState();
                        showHomeInlineMessage(matches.size() + " address folders named " + requestedName
                                + " already exist. Choose the intended property; no folder was created.");
                        setNotBusy();
                    });
                    return;
                }

                if (matches.size() == 1) {
                    DriveFolder existing = matches.get(0);
                    if (possibleMatches.size() != 1
                            || !possibleMatches.get(0).id().equals(existing.id())) {
                        runOnUiThread(() -> {
                            if (screen != Screen.ADDRESSES) {
                                return;
                            }
                            propertyFolders.clear();
                            propertyFolders.addAll(folders);
                            notifyFolderAdapters();
                            renderPropertyCountAndEmptyState();
                            showHomeInlineMessage("More than one possible version of this property exists. "
                                    + "Choose the intended property from the list; no folder was created.");
                            setNotBusy();
                        });
                        return;
                    }
                    runOnUiThread(() -> {
                        if (screen != Screen.ADDRESSES) {
                            return;
                        }
                        propertyFolders.clear();
                        propertyFolders.addAll(folders);
                        notifyFolderAdapters();
                        renderPropertyCountAndEmptyState();
                        openAddress(existing);
                    });
                    return;
                }

                if (!possibleMatches.isEmpty()) {
                    String candidateName = possibleMatches.size() == 1
                            ? possibleMatches.get(0).name()
                            : possibleMatches.size() + " possible property folders";
                    runOnUiThread(() -> {
                        if (screen != Screen.ADDRESSES) {
                            return;
                        }
                        propertyFolders.clear();
                        propertyFolders.addAll(folders);
                        notifyFolderAdapters();
                        renderPropertyCountAndEmptyState();
                        showHomeInlineMessage("Possible existing property match: " + candidateName
                                + ". Choose the intended property from the list; no folder was created.");
                        setNotBusy();
                    });
                    return;
                }

                authorizationGuard.requireDriveMutation();
                DriveFolder created = driveClient.createFolder(
                        getContentResolver(), treeUri, masterId, requestedName);
                List<DriveFolder> afterCreate = driveClient.listFoldersFresh(
                        getContentResolver(), treeUri, masterId);
                DriveFolder verified = DriveClient.findById(afterCreate, created.id());
                List<DriveFolder> verifiedMatches = DriveClient.findExactNameMatches(afterCreate, requestedName);
                List<DriveFolder> verifiedPossibleMatches =
                        AddressFolderAmbiguity.findPossibleMatches(afterCreate, requestedName);

                if (verified == null || !verified.name().equals(requestedName)) {
                    throw new IOException("Drive returned an address ID that could not be verified under the selected company.");
                }

                if (verifiedMatches.size() != 1
                        || !verifiedMatches.get(0).id().equals(created.id())
                        || verifiedPossibleMatches.size() != 1
                        || !verifiedPossibleMatches.get(0).id().equals(created.id())) {
                    runOnUiThread(() -> {
                        if (screen != Screen.ADDRESSES) {
                            return;
                        }
                        createBlockedUntilRefresh = true;
                        propertyFolders.clear();
                        propertyFolders.addAll(afterCreate);
                        notifyFolderAdapters();
                        renderPropertyCountAndEmptyState();
                        showHomeInlineMessage("Address create result is ambiguous. Choose the intended property; no additional folder will be created until refresh.");
                        setNotBusy();
                    });
                    return;
                }

                runOnUiThread(() -> {
                    if (screen != Screen.ADDRESSES) {
                        return;
                    }
                    propertyFolders.clear();
                    propertyFolders.addAll(afterCreate);
                    notifyFolderAdapters();
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
        resetWorkOrderDraft();
        selectedAddress = address;
        folderPrefs.setCurrentAddress(address);
        selectedWorkOrder = folderPrefs.getCurrentWorkOrder();
        createBlockedUntilRefresh = false;
        screen = Screen.WORK_ORDERS;
        statusText = legacyStatusText;
        workOrderFolders.clear();
        notifyFolderAdapters();
        homeRoot.setVisibility(View.GONE);
        legacyRoot.setVisibility(View.VISIBLE);
        addressText.setText(PropertyDisplayName.fromDriveFolderName(address.name()));
        renderCurrentWorkOrder();
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
        workOrderFolders.clear();
        if (adapter != null) {
            notifyFolderAdapters();
        }
        renderPropertyCountAndEmptyState();
        renderSavedMaster();
        clearHomeInlineMessage();
        applySystemBarAppearance(true);
        setNotBusy();
        if (refresh && driveBindingGuard.current().isUsable()) {
            if (folderPrefs.hasWorkspace() && folderPrefs.getCurrentCompany() == null) {
                refreshCompanyFolders(false, false);
            } else {
                refreshAddressFolders();
            }
        }
    }

    private void refreshWorkOrderFolders() {
        if (selectedAddress == null) {
            showMessage("Choose an address first.");
            return;
        }
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
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
                    workOrderFolders.clear();
                    workOrderFolders.addAll(folders);
                    reconcileSelectedWorkOrder(folders);
                    notifyFolderAdapters();
                    setStatusText(folders.size() + " work order" + (folders.size() == 1 ? "" : "s") + " available");
                    statusText.setVisibility(View.GONE);
                    setNotBusy();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!isStillOnAddress(addressId)) {
                        return;
                    }
                    createBlockedUntilRefresh = true;
                    showError("Could not read work-order folders", error);
                });
            }
        });
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

    private void resetWorkOrderDraft() {
        selectedDate = LocalDate.now();
        if (workOrderInput != null) {
            workOrderInput.clearFocus();
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(workOrderInput.getWindowToken(), 0);
            }
            workOrderInput.getText().clear();
            workOrderInput.setText("");
            workOrderInput.setSelection(0);
            workOrderInput.setSaveEnabled(false);
            workOrderInput.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);

            final EditText draftView = workOrderInput;
            draftView.post(() -> {
                if (draftView == workOrderInput && !draftView.hasFocus()) {
                    draftView.getText().clear();
                    draftView.setText("");
                    draftView.setSelection(0);
                }
            });
        }
        updateDateButton();
    }

    private void useOrCreateWorkOrder() {
        if (selectedAddress == null) {
            showMessage("Choose an address first.");
            return;
        }
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
            return;
        }
        if (!hasPersistedWritePermission(treeUri)) {
            showMessage("This Drive workspace is read-only. Choose it again and allow write access.");
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

        if (selectedWorkOrder != null
                && WorkOrderFolderName.shouldRouteSelectedFolderToReuse(
                        selectedWorkOrder.name(),
                        workOrderInput.getText().toString(),
                        selectedDate.toString())) {
            prepareClearAndReuse();
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
                        workOrderFolders.clear();
                        workOrderFolders.addAll(folders);
                        notifyFolderAdapters();
                        setStatusText(matches.size() + " folders named " + requestedName
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
                        workOrderFolders.clear();
                        workOrderFolders.addAll(folders);
                        notifyFolderAdapters();
                        selectWorkOrder(existing, "Existing work-order folder reused", true);
                    });
                    return;
                }

                authorizationGuard.requireDriveMutation();
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
                        workOrderFolders.clear();
                        workOrderFolders.addAll(afterCreate);
                        notifyFolderAdapters();
                        setStatusText("Work-order create result is ambiguous. Select the intended same-named folder; no additional folder will be created until refresh.");
                        setNotBusy();
                    });
                    return;
                }

                runOnUiThread(() -> {
                    if (!isStillOnAddress(addressId)) {
                        return;
                    }
                    workOrderFolders.clear();
                    workOrderFolders.addAll(afterCreate);
                    notifyFolderAdapters();
                    selectWorkOrder(verified, "Work-order folder created", true);
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
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
            return;
        }
        if (!hasPersistedWritePermission(treeUri)) {
            showMessage("This Drive workspace is read-only. Choose it again and allow write access.");
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
                        workOrderFolders.clear();
                        workOrderFolders.addAll(folders);
                        notifyFolderAdapters();
                        setStatusText(requestedMatches.size() + " folders named " + requestedName
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
                        workOrderFolders.clear();
                        workOrderFolders.addAll(folders);
                        notifyFolderAdapters();
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

                authorizationGuard.requireDriveMutation();
                PendingPhotoStore photoStore = new PendingPhotoStore(
                        new File(getFilesDir(), "pending_photos"));
                photoStore.prepareCaptureSequenceResetForReuse(candidateId, requestedName);

                authorizationGuard.requireDriveMutation();
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

                photoStore.completeCaptureSequenceResetForReuse(candidateId, requestedName);

                runOnUiThread(() -> {
                    if (!isStillOnAddress(addressId)) {
                        return;
                    }
                    workOrderFolders.clear();
                    workOrderFolders.addAll(afterRename);
                    notifyFolderAdapters();
                    selectWorkOrder(verified, "Empty folder reused with the same identity; photo numbering restarted at 001", true);
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
        Uri treeUri = usableDriveTreeOrMessage();
        if (treeUri == null) {
            return;
        }
        if (!hasPersistedWritePermission(treeUri)) {
            showMessage("This Drive workspace is read-only. Choose it again and allow write access.");
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
                        workOrderFolders.clear();
                        workOrderFolders.addAll(folders);
                        notifyFolderAdapters();
                        setStatusText(requestedMatches.size() + " folders named " + requestedName
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
                        workOrderFolders.clear();
                        workOrderFolders.addAll(folders);
                        notifyFolderAdapters();
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
                    runOnUiThread(this::reuseSelectedEmptyFolder);
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

                authorizationGuard.requireDriveMutation();
                PendingPhotoStore photoStore = new PendingPhotoStore(
                        new File(getFilesDir(), "pending_photos"));
                photoStore.prepareCaptureSequenceResetForReuse(candidateId, requestedName);

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

                    authorizationGuard.requireDriveMutation();
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

                    photoStore.completeCaptureSequenceResetForReuse(candidateId, requestedName);

                    runOnUiThread(() -> {
                        if (!isStillOnAddress(addressId)) {
                            return;
                        }
                        workOrderFolders.clear();
                        workOrderFolders.addAll(afterRename);
                        notifyFolderAdapters();
                        selectWorkOrder(verified, "Clear & Reuse complete with the same identity; photo numbering restarted at 001", true);
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
        selectWorkOrder(folder, message, false);
    }

    private void selectWorkOrder(DriveFolder folder, String message, boolean success) {
        if (selectedAddress == null || folder.id().equals(selectedAddress.id())) {
            selectedWorkOrder = null;
            folderPrefs.clearCurrentWorkOrder();
            createBlockedUntilRefresh = true;
            renderCurrentWorkOrder();
            showMessage("The selected property cannot be used as its own work order. Refresh before continuing.");
            return;
        }
        selectedWorkOrder = folder;
        folderPrefs.setCurrentWorkOrder(folder);
        renderCurrentWorkOrder();
        if (workOrderAdapter != null) { workOrderAdapter.setSelectedId(folder.id()); }
        setStatusText(
                message + ": " + PropertyDisplayName.readableFolderName(folder.name()),
                success ? StatusBanner.Tone.SUCCESS : StatusBanner.Tone.INFO);
        statusText.setVisibility(View.VISIBLE);
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


    private void refreshProtectedPhotoCounts() {
        executor.execute(() -> {
            final Map<String, Integer> counts;
            final Map<String, Integer> workOrderCounts;
            try {
                PendingPhotoStore store = new PendingPhotoStore(
                        new File(getFilesDir(), "pending_photos"));
                PhotoPreparer preparer = new PhotoPreparer(
                        new File(getFilesDir(), "prepared_photos"));
                ProtectedWorkGuard.Result protectedWork =
                        new ProtectedWorkGuard(store, preparer).inspect();
                counts = new HashMap<>(protectedWork.addressCounts());
                workOrderCounts = new HashMap<>(protectedWork.workOrderCounts());
            } catch (Exception error) {
                return;
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                protectedPhotoCountByAddressId.clear();
                protectedPhotoCountByAddressId.putAll(counts);
                protectedPhotoCountByWorkOrderId.clear();
                protectedPhotoCountByWorkOrderId.putAll(workOrderCounts);
                notifyFolderAdapters();
            });
        });
    }

    private void notifyFolderAdapters() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (workOrderAdapter != null) {
            workOrderAdapter.notifyDataSetChanged();
        }
    }

    private void renderCurrentWorkOrder() {
        if (currentWorkOrderText == null) {
            return;
        }
        currentWorkOrderText.setText(selectedWorkOrder == null
                ? "Select a work order below"
                : "Selected: " + PropertyDisplayName.readableFolderName(selectedWorkOrder.name()));
        if (workOrderAdapter != null) {
            workOrderAdapter.setSelectedId(selectedWorkOrder == null ? null : selectedWorkOrder.id());
        }
        renderWorkOrderNextAction();
    }

    private void renderWorkOrderNextAction() {
        if (photosButton == null) {
            return;
        }
        NextActionGuide.Action action =
                NextActionGuide.workOrders(busy, selectedAddress != null && selectedWorkOrder != null);
        photosButton.setText(action.label());
        photosButton.setEnabled(action.enabled());
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
        if (folderPrefs == null) {
            return;
        }

        DriveFolder workspace = folderPrefs.getWorkspaceFolder();
        DriveFolder company = folderPrefs.getCurrentCompany();
        DriveFolder legacyMaster = folderPrefs.getLegacyMasterFolder();
        Uri treeUri = folderPrefs.getMasterTreeUri();
        OrganizationDriveBindingGuard.Result binding = driveBindingGuard.current();
        boolean canRead = binding.isUsable();
        DriveFolder activeParent = canRead ? folderPrefs.getMasterFolder() : null;

        if (legacyMasterText != null) {
            legacyMasterText.setText(activeParent == null
                    ? "Company: not selected"
                    : "Company: " + activeParent.name());
        }

        if (homeMasterNameText != null) {
            if (!canRead) {
                homeMasterNameText.setText("Google Drive");
                homeDriveStateText.setText(binding.state()
                        == OrganizationDriveBindingGuard.State.NO_WORKSPACE
                        ? "Not connected"
                        : binding.message());
                chooseMasterButton.setText(
                        binding.state() == OrganizationDriveBindingGuard.State.LEGACY_UNBOUND
                                ? "Confirm Drive"
                                : "Connect Drive");
                chooseMasterButton.setVisibility(View.VISIBLE);
                refreshAddressButton.setVisibility(View.GONE);
                driveOptionsButton.setVisibility(View.VISIBLE);
                tintDriveStatusDot(binding.state()
                        == OrganizationDriveBindingGuard.State.NO_WORKSPACE
                                ? R.color.home_text_secondary
                                : R.color.home_error);
            } else if (folderPrefs.hasWorkspace()) {
                chooseMasterButton.setVisibility(View.GONE);
                refreshAddressButton.setVisibility(View.VISIBLE);
                driveOptionsButton.setVisibility(View.VISIBLE);
                if (company == null) {
                    homeMasterNameText.setText(workspace == null ? "Company Workspace" : workspace.name());
                    homeDriveStateText.setText("Choose a company");
                    tintDriveStatusDot(R.color.home_text_secondary);
                } else {
                    homeMasterNameText.setText(company.name());
                    homeDriveStateText.setText(workspace == null
                            ? "Drive connected"
                            : "Workspace: " + workspace.name());
                    tintDriveStatusDot(R.color.home_primary);
                }
            } else {
                homeMasterNameText.setText(legacyMaster == null ? "Google Drive" : legacyMaster.name());
                homeDriveStateText.setText("Single-company Drive");
                chooseMasterButton.setVisibility(View.GONE);
                refreshAddressButton.setVisibility(View.VISIBLE);
                driveOptionsButton.setVisibility(View.VISIBLE);
                tintDriveStatusDot(R.color.home_primary);
            }
        }

        if (refreshAddressButton != null) {
            refreshAddressButton.setEnabled(canRead && !busy);
        }
        if (driveOptionsButton != null) {
            // The menu is also the only Account/recheck entry point, so it must remain usable
            // when Drive itself is intentionally blocked.
            driveOptionsButton.setEnabled(!busy);
        }
        renderCompanySwitchControl(canRead, company);
        renderPropertyCountAndEmptyState();
    }

    private void renderCompanySwitchControl(boolean canRead, DriveFolder company) {
        boolean available = folderPrefs != null
                && folderPrefs.hasWorkspace()
                && canRead;
        if (homeCompanyChevronText != null) {
            homeCompanyChevronText.setVisibility(available ? View.VISIBLE : View.GONE);
        }
        if (homeCompanyClickTarget != null) {
            homeCompanyClickTarget.setEnabled(available && !busy);
            homeCompanyClickTarget.setClickable(available && !busy);
            homeCompanyClickTarget.setContentDescription(company == null
                    ? "Choose company"
                    : "Current company: " + company.name() + ". Tap to choose company.");
        }
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
            int count = propertyFolders.size();
            homePropertyCountText.setText(count == 1 ? "1 property" : count + " properties");
        }
        if (homeEmptyText != null) {
            boolean connected = folderPrefs != null
                    && driveBindingGuard.current().isUsable()
                    && folderPrefs.getMasterFolder() != null;
            homeEmptyText.setVisibility(connected && !busy && propertyFolders.isEmpty()
                    ? View.VISIBLE : View.GONE);
        }
        renderHomeNextAction();
    }

    private void renderHomeNextAction() {
        if (homeNextActionButton == null || folderPrefs == null) {
            return;
        }
        DriveFolder master = folderPrefs.getMasterFolder();
        boolean workspaceConnected = driveBindingGuard.current().isUsable();
        boolean companySelected = master != null;
        DriveFolder saved = folderPrefs.getCurrentAddress();
        boolean savedPropertyAvailable = saved != null
                && DriveClient.findById(propertyFolders, saved.id()) != null;

        NextActionGuide.Action action = NextActionGuide.home(
                busy,
                workspaceConnected,
                companySelected,
                companyFolders.size(),
                propertyFolders.size(),
                savedPropertyAvailable);
        homeNextActionButton.setText(action.label());
        homeNextActionButton.setEnabled(action.enabled());
        homeNextActionButton.setOnClickListener(null);
        if (!action.enabled()) {
            return;
        }
        switch (action.kind()) {
            case CONNECT_DRIVE:
                homeNextActionButton.setOnClickListener(v -> chooseMasterFolder());
                break;
            case CHOOSE_COMPANY:
                homeNextActionButton.setOnClickListener(v -> showCompanyChooser());
                break;
            case ADD_COMPANY:
                homeNextActionButton.setOnClickListener(v -> showAddCompanyDialog());
                break;
            case ADD_PROPERTY:
                homeNextActionButton.setOnClickListener(v -> showAddressEntryDialog());
                break;
            case OPEN_WORK_ORDERS:
                homeNextActionButton.setOnClickListener(v -> openSavedPropertyFromHome());
                break;
            case CHOOSE_PROPERTY:
            default:
                break;
        }
    }

    private void showHomeInlineMessage(String message) {
        showHomeInlineMessage(message, StatusBanner.Tone.INFO);
    }

    private void showHomeSuccessMessage(String message) {
        showHomeInlineMessage(message, StatusBanner.Tone.SUCCESS);
    }

    private void showHomeInlineMessage(String message, StatusBanner.Tone tone) {
        if (homeStatusText == null) {
            return;
        }
        StatusBanner.apply(homeStatusText, tone);
        homeStatusText.setText(message == null ? "" : message);
        homeStatusText.setVisibility(message == null || message.isBlank() ? View.GONE : View.VISIBLE);
    }

    private void clearHomeInlineMessage() {
        showHomeInlineMessage(null, StatusBanner.Tone.INFO);
    }

    private void setBusy(String message) {
        busy = true;
        if (screen == Screen.ADDRESSES) {
            clearHomeInlineMessage();
            if (homeProgress != null) {
                homeProgress.setVisibility(View.VISIBLE);
            }
        } else {
            setStatusText(message);
        }
        chooseMasterButton.setEnabled(false);
        refreshAddressButton.setEnabled(false);
        driveOptionsButton.setEnabled(false);
        if (homeCompanyClickTarget != null) {
            homeCompanyClickTarget.setEnabled(false);
            homeCompanyClickTarget.setClickable(false);
        }
        useCreateAddressButton.setEnabled(false);
        backButton.setEnabled(false);
        refreshWorkOrdersButton.setEnabled(false);
        workOrderInput.setEnabled(false);
        dateButton.setEnabled(false);
        useCreateButton.setEnabled(false);
        reuseEmptyButton.setEnabled(false);
        clearReuseButton.setEnabled(false);
        photosButton.setEnabled(false);
        folderList.setEnabled(false);
        renderPropertyCountAndEmptyState();
        renderWorkOrderNextAction();
    }

    private void setNotBusy() {
        busy = false;
        Uri treeUri = folderPrefs.getMasterTreeUri();
        boolean canRead = driveBindingGuard.current().isUsable();
        boolean canWrite = canRead && treeUri != null && hasPersistedWritePermission(treeUri);

        if (homeProgress != null) {
            homeProgress.setVisibility(View.GONE);
        }
        boolean companyReady = folderPrefs.getMasterFolder() != null;
        renderCompanySwitchControl(canRead, folderPrefs.getCurrentCompany());
        folderList.setEnabled(canRead && companyReady);
        if (screen == Screen.ADDRESSES) {
            chooseMasterButton.setEnabled(true);
            refreshAddressButton.setEnabled(canRead);
            useCreateAddressButton.setEnabled(
                    canRead && canWrite && companyReady && !createBlockedUntilRefresh);
            photosButton.setEnabled(false);
            renderSavedMaster();
        } else {
            backButton.setEnabled(true);
            refreshWorkOrdersButton.setEnabled(canRead);
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
        renderWorkOrderNextAction();
    }

    private void setStatusText(String message) {
        setStatusText(message, StatusBanner.Tone.INFO);
    }

    private void setStatusText(String message, StatusBanner.Tone tone) {
        if (statusText == null) {
            return;
        }
        StatusBanner.apply(statusText, tone);
        statusText.setText(message == null ? "" : message);
        if (screen == Screen.WORK_ORDERS) {
            statusText.setVisibility(message == null || message.isBlank() ? View.GONE : View.VISIBLE);
        }
    }

    private void showMessage(String message) {
        if (screen == Screen.ADDRESSES) {
            showHomeInlineMessage(message, StatusBanner.Tone.INFO);
        } else {
            setStatusText(message, StatusBanner.Tone.INFO);
            statusText.setVisibility(View.VISIBLE);
        }
        setNotBusy();
    }

    private void showSuccessMessage(String message) {
        if (screen == Screen.ADDRESSES) {
            showHomeInlineMessage(message, StatusBanner.Tone.SUCCESS);
        } else {
            setStatusText(message, StatusBanner.Tone.SUCCESS);
            statusText.setVisibility(View.VISIBLE);
        }
        setNotBusy();
    }

    private void showError(String prefix, Throwable error) {
        String detail = error == null ? null : error.getMessage();
        String message = prefix + (detail == null ? "." : ": " + detail);
        if (screen == Screen.ADDRESSES) {
            showHomeInlineMessage(message, StatusBanner.Tone.ERROR);
        } else {
            setStatusText(message, StatusBanner.Tone.ERROR);
            statusText.setVisibility(View.VISIBLE);
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
            controller.setAppearanceLightStatusBars(!night);
            controller.setAppearanceLightNavigationBars(!night);
            int color = ContextCompat.getColor(this, R.color.home_background);
            getWindow().setStatusBarColor(color);
            getWindow().setNavigationBarColor(color);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
