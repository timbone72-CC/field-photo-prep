package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.widget.TextView;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class PhotoCaptureActivity extends Activity {
    private static final int REQUEST_CAPTURE_PHOTO = 2001;
    private static final String STATE_PENDING_CAPTURE_ID = "pending_capture_id";
    private static final String STATE_BATCH_SELECTION_IDS = "batch_selection_ids";
    private static final String BATCH_UPLOAD_GATE_ID = "__selected_batch_upload__";
    private static final String BATCH_RECONCILE_GATE_ID = "__all_uncertain_reconcile__";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, h:mm a");
    private static final PhotoPreparationGate PREPARATION_GATE = new PhotoPreparationGate();
    private static final PhotoUploadGate UPLOAD_GATE = new PhotoUploadGate();

    private final LinkedHashSet<String> batchSelectedPhotoIds = new LinkedHashSet<>();

    private FolderPrefs folderPrefs;
    private PendingPhotoStore photoStore;
    private PhotoPreparer photoPreparer;
    private DriveFolder address;
    private DriveFolder workOrder;
    private String pendingCaptureId;
    private String selectedPhotoId;
    private volatile String activeBatchPhotoId;
    private volatile String activeReconcilePhotoId;
    private volatile int activeReconcilePosition;
    private volatile int activeReconcileTotal;

    private TextView statusText;
    private TextView pendingCountText;
    private TextView batchSelectionText;
    private TextView selectedPhotoText;
    private TextView preparedPhotoText;
    private LinearLayout pendingList;
    private Button takePhotoButton;
    private Button selectAllReadyButton;
    private Button clearSelectionButton;
    private Button uploadBatchButton;
    private Button discardBatchButton;
    private Button reconcileAllButton;
    private Button prepareButton;
    private Button uploadButton;
    private Button reconcileButton;
    private Button discardButton;
    private View selectedActionsPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        folderPrefs = new FolderPrefs(this);
        photoStore = new PendingPhotoStore(new File(getFilesDir(), "pending_photos"));
        photoPreparer = new PhotoPreparer(new File(getFilesDir(), "prepared_photos"));
        address = folderPrefs.getCurrentAddress();
        workOrder = folderPrefs.getCurrentWorkOrder();
        if (savedInstanceState != null) {
            pendingCaptureId = savedInstanceState.getString(STATE_PENDING_CAPTURE_ID);
            ArrayList<String> restoredSelection =
                    savedInstanceState.getStringArrayList(STATE_BATCH_SELECTION_IDS);
            if (restoredSelection != null) {
                batchSelectedPhotoIds.addAll(restoredSelection);
            }
        }
        buildUi();

        if (address == null || workOrder == null) {
            showPhotoStatus("Choose an exact address and work order before taking photos.");
            takePhotoButton.setEnabled(false);
            selectAllReadyButton.setEnabled(false);
            clearSelectionButton.setEnabled(false);
            uploadBatchButton.setEnabled(false);
            discardBatchButton.setEnabled(false);
            reconcileAllButton.setEnabled(false);
            prepareButton.setEnabled(false);
            uploadButton.setEnabled(false);
            reconcileButton.setEnabled(false);
            discardButton.setEnabled(false);
            return;
        }

        reconcileAndRefresh();
        if (PREPARATION_GATE.isBusy()) {
            showPhotoStatus("A photo is being prepared in the background. Protected originals are locked until it finishes.");
            renderSelectedPhoto();
        } else if (UPLOAD_GATE.isBusy()) {
            showPhotoStatus("A Drive upload, selected batch, or reconciliation check is already running. Wait for its queue result.");
            renderSelectedPhoto();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingCaptureId != null) {
            outState.putString(STATE_PENDING_CAPTURE_ID, pendingCaptureId);
        }
        if (!batchSelectedPhotoIds.isEmpty()) {
            outState.putStringArrayList(
                    STATE_BATCH_SELECTION_IDS,
                    new ArrayList<>(batchSelectedPhotoIds));
        }
    }

private void buildUi() {
    setContentView(R.layout.screen_photos);
    View root = findViewById(R.id.photos_root);
    findViewById(R.id.nav_photos).setSelected(true);
    statusText = findViewById(R.id.photos_status);
    pendingCountText = findViewById(R.id.photos_pending_count);
    batchSelectionText = findViewById(R.id.photos_batch_selection);
    selectedPhotoText = findViewById(R.id.photos_selected_text);
    preparedPhotoText = findViewById(R.id.photos_prepared_text);
    pendingList = findViewById(R.id.photos_pending_list);
    selectedActionsPanel = findViewById(R.id.photos_selected_panel);
    takePhotoButton = findViewById(R.id.photos_open_camera);
    selectAllReadyButton = findViewById(R.id.photos_select_all);
    clearSelectionButton = findViewById(R.id.photos_clear_selection);
    uploadBatchButton = findViewById(R.id.photos_upload_selected);
    discardBatchButton = findViewById(R.id.photos_discard_selected);
    reconcileAllButton = findViewById(R.id.photos_reconcile_all);
    prepareButton = findViewById(R.id.photos_prepare);
    uploadButton = findViewById(R.id.photos_upload_one);
    reconcileButton = findViewById(R.id.photos_reconcile);
    discardButton = findViewById(R.id.photos_discard);

    TextView addressText = findViewById(R.id.photos_address);
    TextView workOrderText = findViewById(R.id.photos_work_order);
    addressText.setText(address == null ? "No property selected"
            : PropertyDisplayName.fromDriveFolderName(address.name()));
    workOrderText.setText(workOrder == null ? "No work order selected"
            : PropertyDisplayName.readableFolderName(workOrder.name()));

    takePhotoButton.setOnClickListener(v -> beginCameraCapture());
    selectAllReadyButton.setOnClickListener(v -> selectAllReadyPhotos());
    clearSelectionButton.setOnClickListener(v -> clearBatchSelection());
    uploadBatchButton.setOnClickListener(v -> uploadSelectedBatch());
    discardBatchButton.setOnClickListener(v -> confirmDiscardSelectedBatch());
    reconcileAllButton.setOnClickListener(v -> reconcileAllUncertain());
    prepareButton.setOnClickListener(v -> prepareSelectedPhoto());
    uploadButton.setOnClickListener(v -> uploadSelectedPhoto());
    reconcileButton.setOnClickListener(v -> reconcileSelectedPhoto());
    discardButton.setOnClickListener(v -> confirmDiscardSelected());
    findViewById(R.id.photos_back).setOnClickListener(v -> finish());
    findViewById(R.id.nav_home).setOnClickListener(v -> {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("field_tab", "home");
        startActivity(intent);
        finish();
    });
    findViewById(R.id.nav_work_orders).setOnClickListener(v -> {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("field_tab", "work_orders");
        startActivity(intent);
        finish();
    });

    ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
        var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        view.setPadding(0, bars.top, 0, bars.bottom);
        return insets;
    });
    ViewCompat.requestApplyInsets(root);
    WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
            getWindow(), getWindow().getDecorView());
    boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
    controller.setAppearanceLightStatusBars(!night);
    controller.setAppearanceLightNavigationBars(!night);
    int barColor = ContextCompat.getColor(this, R.color.home_background);
    getWindow().setStatusBarColor(barColor);
    getWindow().setNavigationBarColor(barColor);

    updateBatchSelectionUi();
    renderSelectedPhoto();
}


        private void beginCameraCapture() {
        if (address == null || workOrder == null) {
            showPhotoStatus("Choose an exact address and work order before taking photos.");
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            showPhotoStatus("Wait for photo preparation to finish before starting another capture.");
            return;
        }
        if (pendingCaptureId != null) {
            showPhotoStatus("A photo capture is already in progress.");
            return;
        }

        PendingPhotoRecord record;
        try {
            record = photoStore.beginCapture(address, workOrder);
        } catch (Exception error) {
            showError("Could not protect a local photo before opening the camera", error);
            return;
        }

        pendingCaptureId = record.id();
        try {
            Intent cameraIntent = new Intent(this, CameraCaptureActivity.class);
            cameraIntent.putExtra(CameraCaptureActivity.EXTRA_CAPTURE_ID, record.id());
            showPhotoStatus("Opening in-app camera for " + workOrder.name() + "…");
            startActivityForResult(cameraIntent, REQUEST_CAPTURE_PHOTO);
        } catch (Exception error) {
            finishFailedCameraLaunch("Could not open the in-app camera safely", error);
        }
    }

    private void finishFailedCameraLaunch(String prefix, Exception error) {
        String captureId = pendingCaptureId;
        pendingCaptureId = null;
        if (captureId != null) {
            try {
                photoStore.finishCaptureIfImageExists(captureId);
            } catch (Exception cleanupError) {
                showError(prefix + "; local reservation also needs inspection", cleanupError);
                refreshPhotoList();
                return;
            }
        }
        showError(prefix, error);
        refreshPhotoList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE_PHOTO) {
            return;
        }

        String initialCaptureId = pendingCaptureId;
        pendingCaptureId = null;
        if (initialCaptureId == null) {
            showPhotoStatus("Camera returned without a tracked session. Existing temporary photos were left unchanged.");
            refreshPhotoList();
            return;
        }

        String cameraError = data == null
                ? null
                : data.getStringExtra(CameraCaptureActivity.EXTRA_ERROR_MESSAGE);
        String lastCaptureId = data == null
                ? null
                : data.getStringExtra(CameraCaptureActivity.EXTRA_LAST_CAPTURE_ID);
        int capturedCount = data == null
                ? 0
                : data.getIntExtra(CameraCaptureActivity.EXTRA_CAPTURED_COUNT, 0);

        try {
            PendingPhotoRecord selectedFromSession = null;
            if (lastCaptureId != null) {
                selectedFromSession = photoStore.getById(lastCaptureId);
                if (selectedFromSession != null
                        && selectedFromSession.state() == PendingPhotoRecord.State.CAPTURING) {
                    selectedFromSession = photoStore.finishCaptureIfImageExists(lastCaptureId);
                }
                if (selectedFromSession != null
                        && (address == null
                        || workOrder == null
                        || !address.id().equals(selectedFromSession.addressId())
                        || !workOrder.id().equals(selectedFromSession.workOrderId()))) {
                    throw new IllegalStateException(
                            "The camera session returned a photo for a different stored destination.");
                }
            }

            if (selectedFromSession == null) {
                PendingPhotoRecord initial = photoStore.getById(initialCaptureId);
                if (initial != null && initial.state() == PendingPhotoRecord.State.CAPTURING) {
                    initial = photoStore.finishCaptureIfImageExists(initialCaptureId);
                }
                if (initial != null && initial.state() != PendingPhotoRecord.State.CAPTURING) {
                    selectedFromSession = initial;
                }
            }

            if (selectedFromSession != null) {
                selectedPhotoId = selectedFromSession.id();
                int count = capturedCount > 0 ? capturedCount : 1;
                String summary = count + " photo" + (count == 1 ? "" : "s")
                        + " captured and protected locally. Last photo selected.";
                if (cameraError != null && !cameraError.trim().isEmpty()) {
                    summary += " Camera note: " + cameraError;
                }
                showPhotoStatus(summary);
            } else if (cameraError != null && !cameraError.trim().isEmpty()) {
                showPhotoStatus(cameraError);
            } else if (resultCode == RESULT_OK) {
                showPhotoStatus("Camera session finished without a selectable photo. Existing local photo data was left unchanged.");
            } else {
                showPhotoStatus("Camera closed. No photo data was saved.");
            }
        } catch (Exception error) {
            showError("Camera returned, but the protected photo state needs inspection", error);
        }
        refreshPhotoList();
    }

    private void reconcileAndRefresh() {
        try {
            PendingPhotoStore.ScanResult result = photoStore.reconcileInterruptedCaptures();
            renderScanWarnings(result);
        } catch (Exception error) {
            showError("Could not reconcile temporary photos", error);
        }
        cleanupPreviouslyConfirmedLocalCopies();
        refreshPhotoList();
    }

    private void cleanupPreviouslyConfirmedLocalCopies() {
        try {
            PendingPhotoStore.ScanResult scan = photoStore.scan();
            ConfirmedPhotoCleanup cleanup = new ConfirmedPhotoCleanup(photoStore, photoPreparer);
            int cleaned = 0;
            int incomplete = 0;
            for (PendingPhotoRecord record : scan.records()) {
                if (record.state() != PendingPhotoRecord.State.UPLOADED || !hasAnyLocalCopy(record)) {
                    continue;
                }
                ConfirmedPhotoCleanup.Result result = cleanup.cleanup(record.id());
                if (result.complete()) {
                    cleaned++;
                } else {
                    incomplete++;
                }
            }
            if (incomplete > 0) {
                showPhotoStatus(incomplete
                        + " confirmed upload(s) still have local cleanup pending. Drive success remains confirmed.");
            } else if (cleaned > 0) {
                showPhotoStatus("Removed local image copies for " + cleaned
                        + " previously confirmed upload(s). Drive copies and metadata were kept.");
            }
        } catch (Exception error) {
            showError("Confirmed uploads remain safe, but local cleanup could not be completed", error);
        }
    }

    private void refreshPhotoList() {
        if (workOrder == null) {
            return;
        }
        try {
            PendingPhotoStore.ScanResult scan = photoStore.scan();
            List<PendingPhotoRecord> matching = new ArrayList<>();
            for (PendingPhotoRecord record : scan.records()) {
                if (workOrder.id().equals(record.workOrderId())) {
                    matching.add(record);
                }
            }
            if (selectedPhotoId != null && findById(matching, selectedPhotoId) == null) {
                selectedPhotoId = null;
            }
            if (!UPLOAD_GATE.isBusy()) {
                pruneBatchSelection(matching, scan.unusableQueuedPhotoIds());
            }
            updateReconcileAllUi(scan);
            renderPhotoList(matching, scan.unusableQueuedPhotoIds());
            renderScanWarningsIfNeeded(scan);
        } catch (Exception error) {
            showError("Could not read temporary photos", error);
        }
    }

    private void pruneBatchSelection(
            List<PendingPhotoRecord> records,
            List<String> unusableQueuedIds) {
        batchSelectedPhotoIds.removeIf(photoId -> {
            PendingPhotoRecord record = findById(records, photoId);
            if (record == null) {
                return true;
            }
            boolean unusable = unusableQueuedIds.contains(photoId);
            return !isBatchUploadEligible(record, unusable, hasPreparedCopy(photoId))
                    && !isBatchDiscardEligible(record);
        });
    }

private void renderPhotoList(
        List<PendingPhotoRecord> records,
        List<String> unusableQueuedIds) {
    pendingList.removeAllViews();
    pendingCountText.setText("Photos (" + records.size() + ")");

    boolean controlsBusy = PREPARATION_GATE.isBusy() || UPLOAD_GATE.isBusy();
    LayoutInflater inflater = LayoutInflater.from(this);
    for (PendingPhotoRecord record : records) {
        boolean unusable = unusableQueuedIds.contains(record.id());
        boolean prepared = hasPreparedCopy(record.id());
        boolean preparing = PREPARATION_GATE.isPreparing(record.id());
        boolean remoteBusy = isPhotoRemoteBusy(record.id());
        boolean batchEligible = isBatchUploadEligible(record, unusable, prepared);
        boolean discardEligible = isBatchDiscardEligible(record);

        View row = inflater.inflate(R.layout.row_photo, pendingList, false);
        CheckBox batchCheckBox = row.findViewById(R.id.photo_row_check);
        ImageView thumbnail = row.findViewById(R.id.photo_row_thumb);
        TextView stateText = row.findViewById(R.id.photo_row_status);
        TextView timeText = row.findViewById(R.id.photo_row_time);

        batchCheckBox.setVisibility(record.state() == PendingPhotoRecord.State.UPLOADED
                ? View.INVISIBLE : View.VISIBLE);
        batchCheckBox.setChecked(batchSelectedPhotoIds.contains(record.id()));
        batchCheckBox.setEnabled((batchEligible || discardEligible) && !controlsBusy);
        batchCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                batchSelectedPhotoIds.add(record.id());
            } else {
                batchSelectedPhotoIds.remove(record.id());
            }
            updateBatchSelectionUi();
        });

        String state = photoStatusLabel(record, unusable, prepared, preparing, remoteBusy);
        stateText.setText(state);
        timeText.setText(formatTime(record.createdAtEpochMs()) + " · …" + shortId(record.id()));
        if (selectedPhotoId != null && selectedPhotoId.equals(record.id())) {
            row.setBackgroundResource(R.drawable.bg_concept_selected);
        }
        loadThumbnail(thumbnail, record);
        row.setOnClickListener(v -> {
            selectedPhotoId = record.id();
            renderSelectedPhoto();
            refreshPhotoList();
        });
        pendingList.addView(row);
    }
    updateBatchSelectionUi();
    renderSelectedPhoto();
}

private String photoStatusLabel(
        PendingPhotoRecord record,
        boolean unusable,
        boolean prepared,
        boolean preparing,
        boolean remoteBusy) {
    if (unusable) return "Needs photo data";
    if (remoteBusy && record.state() == PendingPhotoRecord.State.UNCERTAIN) return "Reconciling";
    if (remoteBusy) return "Uploading";
    if (preparing) return "Preparing";
    if (record.state() == PendingPhotoRecord.State.UPLOADED) return "Uploaded";
    if (record.state() == PendingPhotoRecord.State.UNCERTAIN) return "Needs attention";
    if (record.state() == PendingPhotoRecord.State.FAILED) return prepared ? "Ready to retry" : "Retry preparation";
    return prepared ? "Ready to upload" : "Waiting";
}

private void loadThumbnail(ImageView view, PendingPhotoRecord record) {
    File source = getPreparedFileOrNull(record.id());
    try {
        if (source == null || !source.isFile() || source.length() <= 0) {
            source = photoStore.imageFile(record);
        }
        if (source == null || !source.isFile() || source.length() <= 0) {
            view.setImageDrawable(null);
            return;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);
        int sample = 1;
        int target = dp(160);
        while (bounds.outWidth / sample > target * 2 || bounds.outHeight / sample > target * 2) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = Math.max(1, sample);
        Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath(), options);
        view.setImageBitmap(bitmap);
    } catch (Exception ignored) {
        view.setImageDrawable(null);
    }
}


        private boolean isBatchUploadEligible(
            PendingPhotoRecord record,
            boolean unusable,
            boolean prepared) {
        return record != null
                && !unusable
                && prepared
                && record.canBeginUploadAttempt();
    }

    private boolean isBatchDiscardEligible(PendingPhotoRecord record) {
        return record != null && record.canDiscardLocally();
    }

    private boolean isPhotoRemoteBusy(String photoId) {
        if (UPLOAD_GATE.isUploading(photoId)) {
            return true;
        }
        String active = UPLOAD_GATE.activePhotoId();
        if (BATCH_UPLOAD_GATE_ID.equals(active)) {
            return photoId != null && photoId.equals(activeBatchPhotoId);
        }
        return BATCH_RECONCILE_GATE_ID.equals(active)
                && photoId != null
                && photoId.equals(activeReconcilePhotoId);
    }

    private boolean isCurrentSelectionUploadEligible() {
        if (batchSelectedPhotoIds.isEmpty() || address == null || workOrder == null) {
            return false;
        }
        try {
            for (String photoId : batchSelectedPhotoIds) {
                PendingPhotoRecord record = photoStore.getById(photoId);
                if (record == null
                        || !address.id().equals(record.addressId())
                        || !workOrder.id().equals(record.workOrderId())
                        || !record.canBeginUploadAttempt()
                        || !photoStore.hasImageData(record)
                        || !hasPreparedCopy(photoId)) {
                    return false;
                }
            }
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    private boolean isCurrentSelectionDiscardEligible() {
        if (batchSelectedPhotoIds.isEmpty() || address == null || workOrder == null) {
            return false;
        }
        try {
            for (String photoId : batchSelectedPhotoIds) {
                PendingPhotoRecord record = photoStore.getById(photoId);
                if (record == null
                        || !address.id().equals(record.addressId())
                        || !workOrder.id().equals(record.workOrderId())
                        || !record.canDiscardLocally()) {
                    return false;
                }
            }
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    private void updateBatchSelectionUi() {
        if (batchSelectionText == null
                || selectAllReadyButton == null
                || clearSelectionButton == null
                || uploadBatchButton == null
                || discardBatchButton == null) {
            return;
        }
        int selectedCount = batchSelectedPhotoIds.size();
        boolean remoteBusy = UPLOAD_GATE.isBusy();
        boolean preparationBusy = PREPARATION_GATE.isBusy();
        boolean batchRunning = BATCH_UPLOAD_GATE_ID.equals(UPLOAD_GATE.activePhotoId());
        boolean uploadEligible = selectedCount > 0 && isCurrentSelectionUploadEligible();
        boolean discardEligible = selectedCount > 0 && isCurrentSelectionDiscardEligible();

        String text = selectedCount + " selected";
        if (batchRunning) {
            text += activeBatchPhotoId == null
                    ? " · starting upload"
                    : " · sending …" + shortId(activeBatchPhotoId);
        }
        batchSelectionText.setText(text);
        uploadBatchButton.setText("Upload Selected (" + selectedCount + ")");
        discardBatchButton.setText("Discard Selected (" + selectedCount + ")");
        selectAllReadyButton.setEnabled(workOrder != null && !remoteBusy && !preparationBusy);
        clearSelectionButton.setEnabled(selectedCount > 0 && !remoteBusy);
        uploadBatchButton.setEnabled(uploadEligible && !remoteBusy && !preparationBusy);
        discardBatchButton.setEnabled(discardEligible && !remoteBusy && !preparationBusy);
    }

    private void updateReconcileAllUi(PendingPhotoStore.ScanResult scan) {
        if (reconcileAllButton == null || scan == null) {
            return;
        }
        boolean running = BATCH_RECONCILE_GATE_ID.equals(UPLOAD_GATE.activePhotoId());
        int uncertainCount = scan.uncertainPhotoIds().size();
        if (running) {
            reconcileAllButton.setVisibility(View.VISIBLE);
            reconcileAllButton.setEnabled(false);
            if (activeReconcilePosition > 0 && activeReconcileTotal > 0) {
                reconcileAllButton.setText("Reconciling " + activeReconcilePosition
                        + " of " + activeReconcileTotal + "…");
            } else {
                reconcileAllButton.setText("Starting Reconciliation…");
            }
            return;
        }
        reconcileAllButton.setText("Reconcile All Uncertain (" + uncertainCount + ")");
        reconcileAllButton.setVisibility(uncertainCount > 0 ? View.VISIBLE : View.GONE);
        reconcileAllButton.setEnabled(uncertainCount > 0
                && !PREPARATION_GATE.isBusy()
                && !UPLOAD_GATE.isBusy());
    }

    private void selectAllReadyPhotos() {
        if (workOrder == null || UPLOAD_GATE.isBusy() || PREPARATION_GATE.isBusy()) {
            return;
        }
        try {
            PendingPhotoStore.ScanResult scan = photoStore.scan();
            batchSelectedPhotoIds.clear();
            for (PendingPhotoRecord record : scan.records()) {
                if (!workOrder.id().equals(record.workOrderId())) {
                    continue;
                }
                boolean unusable = scan.unusableQueuedPhotoIds().contains(record.id());
                if (isBatchUploadEligible(record, unusable, hasPreparedCopy(record.id()))) {
                    batchSelectedPhotoIds.add(record.id());
                }
            }
            showPhotoStatus(batchSelectedPhotoIds.size() + " ready photo"
                    + (batchSelectedPhotoIds.size() == 1 ? "" : "s")
                    + " selected. You can uncheck any photo, upload the ready selection, or discard the selected local photos.");
            refreshPhotoList();
        } catch (Exception error) {
            showError("Could not select the ready photos safely", error);
        }
    }

    private void clearBatchSelection() {
        if (UPLOAD_GATE.isBusy()) {
            showPhotoStatus("Wait for the active Drive operation to finish before changing this selection.");
            return;
        }
        batchSelectedPhotoIds.clear();
        showPhotoStatus("Selection cleared. No photo or Drive state changed.");
        refreshPhotoList();
    }

    private void confirmDiscardSelectedBatch() {
        if (batchSelectedPhotoIds.isEmpty() || address == null || workOrder == null) {
            return;
        }
        if (UPLOAD_GATE.isBusy()) {
            showPhotoStatus("Wait for the active Drive operation to finish before discarding selected local photos.");
            updateBatchSelectionUi();
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            showPhotoStatus("Wait for photo preparation to finish before discarding selected local photos.");
            updateBatchSelectionUi();
            return;
        }

        ArrayList<String> requested = new ArrayList<>(batchSelectedPhotoIds);
        final List<String> snapshot;
        try {
            LocalPhotoDiscardCoordinator coordinator =
                    new LocalPhotoDiscardCoordinator(photoStore, photoPreparer);
            snapshot = coordinator.validateSnapshot(requested, address.id(), workOrder.id());
        } catch (Exception error) {
            showError("Selected photos are no longer safe to discard as one batch", error);
            refreshPhotoList();
            return;
        }

        int count = snapshot.size();
        new AlertDialog.Builder(this)
                .setTitle("Discard " + count + " selected photo" + (count == 1 ? "?" : "s?"))
                .setMessage("Work order: " + workOrder.name()
                        + "\nSelected local photos: " + count
                        + "\n\nThis removes only these photos' app-private local originals and prepared copies. It does not delete anything from Drive.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Discard Selected", (dialog, which) -> discardSelectedBatch(snapshot))
                .show();
    }

    private void discardSelectedBatch(List<String> snapshot) {
        if (snapshot == null || snapshot.isEmpty() || address == null || workOrder == null) {
            return;
        }
        if (UPLOAD_GATE.isBusy()) {
            showPhotoStatus("Drive operation is still running. Nothing was discarded.");
            updateBatchSelectionUi();
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            showPhotoStatus("Photo preparation is still running. Nothing was discarded.");
            updateBatchSelectionUi();
            return;
        }

        LocalPhotoDiscardCoordinator coordinator =
                new LocalPhotoDiscardCoordinator(photoStore, photoPreparer);
        LocalPhotoDiscardCoordinator.BatchResult result = coordinator.discardBatch(
                snapshot, address.id(), workOrder.id());

        for (int index = 0; index < result.discardedCount() && index < snapshot.size(); index++) {
            String discardedId = snapshot.get(index);
            batchSelectedPhotoIds.remove(discardedId);
            if (discardedId.equals(selectedPhotoId)) {
                selectedPhotoId = null;
            }
        }

        if (result.complete()) {
            showPhotoStatus(result.discardedCount() + " selected temporary photo"
                    + (result.discardedCount() == 1 ? " was" : "s were")
                    + " discarded locally. Drive was unchanged.");
        } else if (result.discardedCount() == 0) {
            showPhotoStatus("Nothing was discarded. " + result.failureDetail()
                    + " Drive was unchanged.");
        } else {
            showPhotoStatus("Discard stopped after " + result.discardedCount() + " of "
                    + result.selectedCount() + " selected photos. " + result.failureDetail()
                    + " Later photos were left untouched. Drive was unchanged.");
        }
        refreshPhotoList();
    }

    private void uploadSelectedBatch() {
        if (batchSelectedPhotoIds.isEmpty()) {
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            showPhotoStatus("Wait for photo preparation to finish before starting the selected upload batch.");
            updateBatchSelectionUi();
            return;
        }
        if (UPLOAD_GATE.isBusy()) {
            showPhotoStatus("A Drive upload, batch, or reconciliation check is already in progress.");
            updateBatchSelectionUi();
            return;
        }

        final List<String> snapshot;
        try {
            snapshot = validatedBatchSnapshot();
        } catch (Exception error) {
            showError("Could not verify the selected batch before upload", error);
            return;
        }

        if (snapshot.size() != batchSelectedPhotoIds.size()) {
            showPhotoStatus("Every selected photo must be prepared and upload-eligible before Upload Selected can run. Review the current selection or use Discard Selected for local-only removal.");
            refreshPhotoList();
            return;
        }
        if (snapshot.isEmpty()) {
            showPhotoStatus("None of the selected photos are ready for a normal upload attempt.");
            refreshPhotoList();
            return;
        }

        if (!UPLOAD_GATE.tryBegin(BATCH_UPLOAD_GATE_ID)) {
            showPhotoStatus("A Drive operation is already in progress.");
            updateBatchSelectionUi();
            return;
        }

        activeBatchPhotoId = null;
        showPhotoStatus("Starting selected batch: " + snapshot.size()
                + " photo" + (snapshot.size() == 1 ? "" : "s")
                + ", one Drive upload at a time…");
        updateBatchSelectionUi();
        refreshPhotoList();

        Thread worker = new Thread(
                () -> uploadBatchInBackground(snapshot),
                "FieldPhotoPrep-batch-upload");
        try {
            worker.start();
        } catch (RuntimeException | Error error) {
            activeBatchPhotoId = null;
            UPLOAD_GATE.finish(BATCH_UPLOAD_GATE_ID);
            showError("Could not start the selected upload batch; no new batch attempt was started", error);
            refreshPhotoList();
        }
    }

    private List<String> validatedBatchSnapshot() throws Exception {
        List<String> ready = new ArrayList<>();
        for (String photoId : batchSelectedPhotoIds) {
            PendingPhotoRecord record = photoStore.getById(photoId);
            if (record == null) {
                continue;
            }
            if (address == null
                    || workOrder == null
                    || !address.id().equals(record.addressId())
                    || !workOrder.id().equals(record.workOrderId())) {
                continue;
            }
            if (!record.canBeginUploadAttempt()) {
                continue;
            }
            if (!photoStore.hasImageData(record)) {
                continue;
            }
            File prepared = photoPreparer.preparedFile(photoId);
            if (!prepared.isFile() || prepared.length() <= 0) {
                continue;
            }
            ready.add(photoId);
        }
        return ready;
    }

    private void uploadBatchInBackground(List<String> photoIds) {
        PhotoBatchUploadRunner.BatchResult batchResult = null;
        Throwable batchFailure = null;
        try {
            DrivePhotoUploader uploader = new DrivePhotoUploader(
                    getContentResolver(),
                    folderPrefs.getMasterTreeUri());
            PhotoUploadCoordinator coordinator = new PhotoUploadCoordinator(
                    photoStore,
                    photoPreparer,
                    uploader);
            PhotoBatchUploadRunner runner = new PhotoBatchUploadRunner();
            final int[] position = {0};
            batchResult = runner.run(photoIds, photoId -> {
                position[0]++;
                activeBatchPhotoId = photoId;
                int currentPosition = position[0];
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    showPhotoStatus("Uploading selected photo " + currentPosition + " of "
                            + photoIds.size() + " to its stored work-order destination…");
                    refreshPhotoList();
                });
                return attemptOneBatchPhoto(coordinator, photoId);
            });
        } catch (Throwable error) {
            batchFailure = error;
        } finally {
            activeBatchPhotoId = null;
            UPLOAD_GATE.finish(BATCH_UPLOAD_GATE_ID);
        }

        final PhotoBatchUploadRunner.BatchResult completedBatch = batchResult;
        final Throwable completedFailure = batchFailure;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (completedFailure != null || completedBatch == null) {
                showError(
                        "Selected batch stopped because its result could not be verified safely. Remaining photos were not intentionally started",
                        completedFailure == null
                                ? new IllegalStateException("Batch did not return a result.")
                                : completedFailure);
            } else {
                batchSelectedPhotoIds.removeAll(completedBatch.confirmedPhotoIds());
                showPhotoStatus(formatBatchResult(completedBatch));
            }
            refreshPhotoList();
        });
    }

    private PhotoBatchUploadRunner.AttemptResult attemptOneBatchPhoto(
            PhotoUploadCoordinator coordinator,
            String photoId) {
        try {
            PendingPhotoRecord completed = coordinator.upload(photoId);
            if (completed.state() != PendingPhotoRecord.State.UPLOADED) {
                return PhotoBatchUploadRunner.AttemptResult.stopUnverified(
                        "Upload returned without confirmed UPLOADED state.");
            }
            return PhotoBatchUploadRunner.AttemptResult.confirmed(
                    cleanupConfirmedBatchPhoto(coordinator, photoId));
        } catch (Throwable error) {
            final PendingPhotoRecord current;
            try {
                current = photoStore.getById(photoId);
            } catch (Exception stateError) {
                return PhotoBatchUploadRunner.AttemptResult.stopUnverified(
                        "Upload stopped and the photo's queue state could not be reread safely. "
                                + "No later selected photo was attempted.");
            }

            if (current == null) {
                return PhotoBatchUploadRunner.AttemptResult.stopUnverified(
                        "Upload stopped and the active photo record is no longer readable. "
                                + "No later selected photo was attempted.");
            }

            String detail = errorDetail(error, "Upload did not complete safely.");
            switch (current.state()) {
                case UPLOADED:
                    return PhotoBatchUploadRunner.AttemptResult.confirmed(
                            cleanupConfirmedBatchPhoto(coordinator, photoId));
                case FAILED:
                case WAITING:
                    return PhotoBatchUploadRunner.AttemptResult.safeFailure(detail);
                case UNCERTAIN:
                    return PhotoBatchUploadRunner.AttemptResult.stopUncertain(
                            detail + " Remote state is UNCERTAIN; reconcile this photo before retry.");
                case UPLOADING:
                    return PhotoBatchUploadRunner.AttemptResult.stopUncertain(
                            detail + " Queue state still shows UPLOADING, so remote safety is not proven. "
                                    + "No later selected photo was attempted.");
                case CAPTURING:
                default:
                    return PhotoBatchUploadRunner.AttemptResult.stopUnverified(
                            detail + " The active photo is in an unexpected state; no later selected photo was attempted.");
            }
        }
    }

    private boolean cleanupConfirmedBatchPhoto(
            PhotoUploadCoordinator coordinator,
            String photoId) {
        try {
            ConfirmedPhotoCleanup.Result cleanup = coordinator.cleanupConfirmedLocalData(photoId);
            return cleanup != null && cleanup.complete();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private String formatBatchResult(PhotoBatchUploadRunner.BatchResult result) {
        StringBuilder summary = new StringBuilder();
        summary.append("Batch finished: ")
                .append(result.confirmedCount())
                .append(" of ")
                .append(result.selectedCount())
                .append(" selected photo")
                .append(result.selectedCount() == 1 ? "" : "s")
                .append(" confirmed in Drive.");

        if (result.safeFailureCount() > 0) {
            summary.append(" ")
                    .append(result.safeFailureCount())
                    .append(" retry-safe failure")
                    .append(result.safeFailureCount() == 1 ? " was" : "s were")
                    .append(" kept locally.");
        }
        if (result.cleanupPendingCount() > 0) {
            summary.append(" ")
                    .append(result.cleanupPendingCount())
                    .append(" confirmed upload")
                    .append(result.cleanupPendingCount() == 1 ? " has" : "s have")
                    .append(" local cleanup pending.");
        }
        if (result.stoppedEarly()) {
            summary.append(" Batch stopped at photo …")
                    .append(shortId(result.stoppedPhotoId()))
                    .append(result.stopOutcome() == PhotoBatchUploadRunner.Outcome.STOP_UNCERTAIN
                            ? " because remote state is uncertain."
                            : " because the active result could not be verified safely.");
            if (result.unattemptedCount() > 0) {
                summary.append(" ")
                        .append(result.unattemptedCount())
                        .append(" later selected photo")
                        .append(result.unattemptedCount() == 1 ? " was" : "s were")
                        .append(" not attempted.");
            }
            if (result.stopDetail() != null) {
                summary.append(" ").append(result.stopDetail());
            }
        }
        return summary.toString();
    }

    private String errorDetail(Throwable error, String fallback) {
        if (error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()) {
            return fallback;
        }
        return error.getMessage();
    }

    private void renderSelectedPhoto() {
        if (selectedPhotoText == null
                || preparedPhotoText == null
                || prepareButton == null
                || uploadButton == null
                || reconcileButton == null
                || discardButton == null
                || takePhotoButton == null) {
            return;
        }

        boolean preparationBusy = PREPARATION_GATE.isBusy();
        boolean remoteBusy = UPLOAD_GATE.isBusy();
        updateBatchSelectionUi();
        takePhotoButton.setEnabled(address != null
                && workOrder != null
                && pendingCaptureId == null
                && !preparationBusy);

        if (selectedPhotoId == null) {
            if (selectedActionsPanel != null) selectedActionsPanel.setVisibility(View.GONE);
            selectedPhotoText.setText("Selected temporary photo: none");
            preparedPhotoText.setText("Prepared copy: none selected");
            prepareButton.setEnabled(false);
            uploadButton.setEnabled(false);
            reconcileButton.setEnabled(false);
            discardButton.setEnabled(false);
            return;
        }

        PendingPhotoRecord selected;
        try {
            selected = photoStore.getById(selectedPhotoId);
        } catch (Exception error) {
            showError("Could not read the selected photo state", error);
            prepareButton.setEnabled(false);
            uploadButton.setEnabled(false);
            reconcileButton.setEnabled(false);
            discardButton.setEnabled(false);
            return;
        }
        if (selected == null) {
            selectedPhotoText.setText("Selected temporary photo: no longer available");
            preparedPhotoText.setText("Prepared copy: unavailable");
            prepareButton.setEnabled(false);
            uploadButton.setEnabled(false);
            reconcileButton.setEnabled(false);
            discardButton.setEnabled(false);
            return;
        }

        if (selectedActionsPanel != null) selectedActionsPanel.setVisibility(View.VISIBLE);

        String attempt = selected.uploadAttemptCount() > 0
                ? " · attempt " + selected.uploadAttemptCount()
                : "";
        selectedPhotoText.setText("Selected temporary photo: …" + shortId(selectedPhotoId)
                + " · " + selected.state().name() + attempt);

        File prepared = getPreparedFileOrNull(selectedPhotoId);
        boolean hasPrepared = prepared != null && prepared.isFile() && prepared.length() > 0;
        boolean hasLocalCopy = hasAnyLocalCopy(selected);
        if (isPhotoRemoteBusy(selectedPhotoId)) {
            if (selected.state() == PendingPhotoRecord.State.UNCERTAIN) {
                preparedPhotoText.setText("Prepared copy: "
                        + (hasPrepared ? formatBytes(prepared.length()) : "unavailable")
                        + "\nReconciliation: checking stored destination …"
                        + shortId(selected.workOrderId()) + ". No remote write is allowed.");
            } else {
                preparedPhotoText.setText("Prepared copy: "
                        + (hasPrepared ? formatBytes(prepared.length()) : "unavailable")
                        + "\nUpload: sending to stored destination …"
                        + shortId(selected.workOrderId()) + ".");
            }
        } else if (PREPARATION_GATE.isPreparing(selectedPhotoId)) {
            preparedPhotoText.setText("Prepared copy: preparing in background…");
        } else {
            String preparedStatus;
            if (selected.state() == PendingPhotoRecord.State.UPLOADED) {
                preparedStatus = hasLocalCopy
                        ? "Local cleanup: pending"
                        : "Local copies: cleaned up";
            } else {
                preparedStatus = hasPrepared
                        ? "Prepared copy: " + formatBytes(prepared.length())
                        : "Prepared copy: not created";
            }
            if (selected.state() == PendingPhotoRecord.State.UNCERTAIN) {
                preparedStatus += "\nUpload result: UNCERTAIN"
                        + (selected.statusDetail() == null
                        ? "."
                        : " — " + selected.statusDetail())
                        + "\nReconcile before retry.";
            } else if (selected.state() == PendingPhotoRecord.State.UPLOADED) {
                preparedStatus += "\nUpload result: confirmed"
                        + (selected.remoteFileId() == null
                        ? ""
                        : " · remote …" + shortId(selected.remoteFileId()));
            } else if (selected.state() == PendingPhotoRecord.State.FAILED
                    && selected.statusDetail() != null) {
                preparedStatus += "\nLast upload/reconciliation result: " + selected.statusDetail();
            }
            preparedPhotoText.setText(preparedStatus);
        }

        prepareButton.setEnabled(!preparationBusy
                && !remoteBusy
                && selected.state() == PendingPhotoRecord.State.WAITING);
        uploadButton.setEnabled(!preparationBusy
                && !remoteBusy
                && hasPrepared
                && selected.canBeginUploadAttempt());
        reconcileButton.setEnabled(!preparationBusy
                && !remoteBusy
                && selected.state() == PendingPhotoRecord.State.UNCERTAIN);
        discardButton.setEnabled(!preparationBusy
                && !remoteBusy
                && selected.canDiscardLocally());
    }

    private void prepareSelectedPhoto() {
        final String photoId = selectedPhotoId;
        if (photoId == null) {
            return;
        }
        if (UPLOAD_GATE.isBusy()) {
            showPhotoStatus("Wait for the active Drive operation to finish before preparing another photo.");
            renderSelectedPhoto();
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            showPhotoStatus("A photo is already being prepared. Wait for it to finish before starting another.");
            renderSelectedPhoto();
            return;
        }

        try {
            PendingPhotoRecord record = photoStore.getById(photoId);
            if (record == null) {
                showPhotoStatus("The selected temporary photo no longer exists.");
                refreshPhotoList();
                return;
            }
            if (record.state() != PendingPhotoRecord.State.WAITING) {
                showPhotoStatus("Only a WAITING photo can be prepared. Current state: "
                        + record.state().name() + ".");
                renderSelectedPhoto();
                return;
            }
            if (!photoStore.hasImageData(record)) {
                showPhotoStatus("The protected original is missing or empty. Nothing was prepared.");
                return;
            }
        } catch (Exception error) {
            showError("Could not verify the selected photo before preparation", error);
            return;
        }

        if (!PREPARATION_GATE.tryBegin(photoId)) {
            showPhotoStatus("A photo is already being prepared. Wait for it to finish before starting another.");
            renderSelectedPhoto();
            return;
        }

        showPhotoStatus("Preparing photo in the background… Protected original is locked and remains safe.");
        renderSelectedPhoto();
        refreshPhotoList();

        Thread worker = new Thread(() -> preparePhotoInBackground(photoId),
                "FieldPhotoPrep-prepare");
        try {
            worker.start();
        } catch (RuntimeException | Error error) {
            PREPARATION_GATE.finish(photoId);
            showError("Could not start background photo preparation; protected original was kept", error);
            renderSelectedPhoto();
        }
    }

    private void preparePhotoInBackground(String photoId) {
        PreparedPhotoResult result = null;
        Throwable failure = null;
        try {
            PendingPhotoRecord record = photoStore.getById(photoId);
            if (record == null) {
                throw new IllegalStateException("The selected temporary photo no longer exists.");
            }
            if (record.state() != PendingPhotoRecord.State.WAITING) {
                throw new IllegalStateException("The photo is no longer in waiting state.");
            }
            if (!photoStore.hasImageData(record)) {
                throw new IllegalStateException("The protected original is missing or empty.");
            }
            result = photoPreparer.prepare(photoStore, record);
        } catch (Throwable error) {
            failure = error;
        } finally {
            PREPARATION_GATE.finish(photoId);
        }

        final PreparedPhotoResult completedResult = result;
        final Throwable completedFailure = failure;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (completedFailure == null && completedResult != null) {
                showPhotoStatus("Prepared for upload: "
                        + formatBytes(completedResult.originalBytes()) + " → "
                        + formatBytes(completedResult.preparedBytes()) + " · "
                        + completedResult.width() + "×" + completedResult.height()
                        + ". Protected original kept.");
            } else {
                showError("Could not prepare the selected photo; protected original was kept",
                        completedFailure == null
                                ? new IllegalStateException("Photo preparation did not return a result.")
                                : completedFailure);
            }
            refreshPhotoList();
            renderSelectedPhoto();
        });
    }

    private void uploadSelectedPhoto() {
        final String photoId = selectedPhotoId;
        if (photoId == null) {
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            showPhotoStatus("Wait for photo preparation to finish before uploading.");
            renderSelectedPhoto();
            return;
        }
        if (UPLOAD_GATE.isBusy()) {
            showPhotoStatus("A Drive upload or reconciliation check is already in progress.");
            renderSelectedPhoto();
            return;
        }

        try {
            PendingPhotoRecord record = photoStore.getById(photoId);
            if (record == null) {
                showPhotoStatus("The selected temporary photo no longer exists.");
                refreshPhotoList();
                return;
            }
            if (!record.canBeginUploadAttempt()) {
                showPhotoStatus("This photo cannot be uploaded from state "
                        + record.state().name() + ".");
                renderSelectedPhoto();
                return;
            }
            File prepared = photoPreparer.preparedFile(photoId);
            if (!prepared.isFile() || prepared.length() <= 0) {
                showPhotoStatus("Prepare this photo before uploading. Queue state was not changed.");
                renderSelectedPhoto();
                return;
            }
        } catch (Exception error) {
            showError("Could not verify the selected photo before upload", error);
            return;
        }

        if (!UPLOAD_GATE.tryBegin(photoId)) {
            showPhotoStatus("A Drive operation is already in progress.");
            renderSelectedPhoto();
            return;
        }

        showPhotoStatus("Starting Drive upload to the photo's stored work-order destination…");
        renderSelectedPhoto();
        refreshPhotoList();

        Thread worker = new Thread(() -> uploadPhotoInBackground(photoId),
                "FieldPhotoPrep-upload");
        try {
            worker.start();
        } catch (RuntimeException | Error error) {
            UPLOAD_GATE.finish(photoId);
            showError("Could not start the Drive upload; queue state was not changed", error);
            renderSelectedPhoto();
        }
    }

    private void uploadPhotoInBackground(String photoId) {
        PendingPhotoRecord completed = null;
        ConfirmedPhotoCleanup.Result cleanupResult = null;
        Throwable uploadFailure = null;
        Throwable cleanupFailure = null;
        try {
            DrivePhotoUploader uploader = new DrivePhotoUploader(
                    getContentResolver(),
                    folderPrefs.getMasterTreeUri());
            PhotoUploadCoordinator coordinator = new PhotoUploadCoordinator(
                    photoStore,
                    photoPreparer,
                    uploader);
            completed = coordinator.upload(photoId);
            if (completed.state() == PendingPhotoRecord.State.UPLOADED) {
                try {
                    cleanupResult = coordinator.cleanupConfirmedLocalData(photoId);
                } catch (Throwable error) {
                    cleanupFailure = error;
                }
            }
        } catch (Throwable error) {
            uploadFailure = error;
        } finally {
            UPLOAD_GATE.finish(photoId);
        }

        final PendingPhotoRecord completedRecord = completed;
        final ConfirmedPhotoCleanup.Result completedCleanup = cleanupResult;
        final Throwable completedUploadFailure = uploadFailure;
        final Throwable completedCleanupFailure = cleanupFailure;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (completedUploadFailure == null
                    && completedRecord != null
                    && completedRecord.state() == PendingPhotoRecord.State.UPLOADED) {
                String remote = "Drive upload confirmed · remote …"
                        + shortId(completedRecord.remoteFileId()) + ". ";
                if (completedCleanupFailure == null
                        && completedCleanup != null
                        && completedCleanup.complete()) {
                    showPhotoStatus(remote
                            + "Local original and prepared copy were removed; upload metadata was kept.");
                } else {
                    showPhotoStatus(remote
                            + "Local cleanup is incomplete, but remote success remains confirmed and cleanup can be retried safely.");
                }
            } else {
                showError("Drive upload did not reach confirmed success; local photo data was kept",
                        completedUploadFailure == null
                                ? new IllegalStateException("Upload did not return a confirmed queue result.")
                                : completedUploadFailure);
            }
            refreshPhotoList();
            renderSelectedPhoto();
        });
    }

    private void reconcileSelectedPhoto() {
        final String photoId = selectedPhotoId;
        if (photoId == null) {
            return;
        }
        if (PREPARATION_GATE.isBusy() || UPLOAD_GATE.isBusy()) {
            showPhotoStatus("Wait for the active photo/Drive operation to finish before reconciliation.");
            renderSelectedPhoto();
            return;
        }

        try {
            PendingPhotoRecord record = photoStore.getById(photoId);
            if (record == null) {
                showPhotoStatus("The selected temporary photo no longer exists.");
                refreshPhotoList();
                return;
            }
            if (record.state() != PendingPhotoRecord.State.UNCERTAIN) {
                showPhotoStatus("Only an UNCERTAIN upload needs remote reconciliation.");
                renderSelectedPhoto();
                return;
            }
        } catch (Exception error) {
            showError("Could not read the uncertain photo before reconciliation", error);
            return;
        }

        if (!UPLOAD_GATE.tryBegin(photoId)) {
            showPhotoStatus("A Drive operation is already in progress.");
            renderSelectedPhoto();
            return;
        }

        showPhotoStatus(
                "Reconciling UNCERTAIN upload against its stored work-order destination. No remote file will be created, changed, or deleted.");
        renderSelectedPhoto();
        refreshPhotoList();

        Thread worker = new Thread(() -> reconcilePhotoInBackground(photoId),
                "FieldPhotoPrep-reconcile");
        try {
            worker.start();
        } catch (RuntimeException | Error error) {
            UPLOAD_GATE.finish(photoId);
            showError("Could not start Drive reconciliation; queue state was not changed", error);
            renderSelectedPhoto();
        }
    }

    private void reconcilePhotoInBackground(String photoId) {
        PhotoUploadCoordinator.ReconciliationResult reconciliation = null;
        ConfirmedPhotoCleanup.Result cleanupResult = null;
        Throwable failure = null;
        Throwable cleanupFailure = null;
        try {
            DrivePhotoUploader uploader = new DrivePhotoUploader(
                    getContentResolver(),
                    folderPrefs.getMasterTreeUri());
            DrivePhotoReconciler reconciler = new DrivePhotoReconciler(
                    getContentResolver(),
                    folderPrefs.getMasterTreeUri());
            PhotoUploadCoordinator coordinator = new PhotoUploadCoordinator(
                    photoStore,
                    photoPreparer,
                    uploader,
                    reconciler);
            reconciliation = coordinator.reconcileUncertain(photoId);
            if (reconciliation.record().state() == PendingPhotoRecord.State.UPLOADED) {
                try {
                    cleanupResult = coordinator.cleanupConfirmedLocalData(photoId);
                } catch (Throwable error) {
                    cleanupFailure = error;
                }
            }
        } catch (Throwable error) {
            failure = error;
        } finally {
            UPLOAD_GATE.finish(photoId);
        }

        final PhotoUploadCoordinator.ReconciliationResult completed = reconciliation;
        final ConfirmedPhotoCleanup.Result completedCleanup = cleanupResult;
        final Throwable completedFailure = failure;
        final Throwable completedCleanupFailure = cleanupFailure;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (completedFailure != null || completed == null) {
                showError("Drive reconciliation could not complete; UNCERTAIN state remains protected",
                        completedFailure == null
                                ? new IllegalStateException("Reconciliation did not return a result.")
                                : completedFailure);
            } else {
                switch (completed.outcome()) {
                    case CONFIRMED_MATCH:
                        String remote = "Remote photo matched exactly · remote …"
                                + shortId(completed.record().remoteFileId()) + ". ";
                        if (completedCleanupFailure == null
                                && completedCleanup != null
                                && completedCleanup.complete()) {
                            showPhotoStatus(remote
                                    + "Upload is confirmed and local image copies were removed.");
                        } else {
                            showPhotoStatus(remote
                                    + "Upload is confirmed; local cleanup is incomplete but can be retried safely.");
                        }
                        break;
                    case CONFIRMED_ABSENT_RETRY_SAFE:
                        showPhotoStatus(
                                "Two settled Drive checks confirmed the expected remote photo is absent. Retry is now enabled for the original stored destination.");
                        break;
                    case REMAIN_UNCERTAIN:
                    default:
                        showPhotoStatus("Upload remains UNCERTAIN: " + completed.detail());
                        break;
                }
            }
            refreshPhotoList();
            renderSelectedPhoto();
        });
    }

    private void reconcileAllUncertain() {
        if (PREPARATION_GATE.isBusy()) {
            showPhotoStatus("Wait for photo preparation to finish before reconciling the backlog.");
            return;
        }
        if (UPLOAD_GATE.isBusy()) {
            showPhotoStatus("A Drive upload or reconciliation check is already in progress.");
            return;
        }

        final List<String> snapshot = new ArrayList<>();
        try {
            PendingPhotoStore.ScanResult scan = photoStore.scan();
            for (PendingPhotoRecord record : scan.records()) {
                if (record.state() == PendingPhotoRecord.State.UNCERTAIN) {
                    snapshot.add(record.id());
                }
            }
        } catch (Exception error) {
            showError("Could not read the UNCERTAIN backlog safely", error);
            return;
        }

        if (snapshot.isEmpty()) {
            showPhotoStatus("There are no UNCERTAIN uploads to reconcile.");
            refreshPhotoList();
            return;
        }

        if (!UPLOAD_GATE.tryBegin(BATCH_RECONCILE_GATE_ID)) {
            showPhotoStatus("A Drive operation is already in progress.");
            return;
        }

        activeReconcilePhotoId = null;
        activeReconcilePosition = 0;
        activeReconcileTotal = snapshot.size();
        refreshPhotoList();
        showPhotoStatus("Starting read-only reconciliation of " + snapshot.size()
                + " UNCERTAIN photo" + (snapshot.size() == 1 ? "" : "s")
                + ". No Drive file will be created, changed, or deleted.");

        Thread worker = new Thread(
                () -> reconcileAllInBackground(snapshot),
                "FieldPhotoPrep-reconcile-all");
        try {
            worker.start();
        } catch (RuntimeException | Error error) {
            activeReconcilePhotoId = null;
            activeReconcilePosition = 0;
            activeReconcileTotal = 0;
            UPLOAD_GATE.finish(BATCH_RECONCILE_GATE_ID);
            showError("Could not start bulk Drive reconciliation; queue state was not changed", error);
            refreshPhotoList();
        }
    }

    private void reconcileAllInBackground(List<String> photoIds) {
        PhotoReconciliationBatchRunner.BatchResult batchResult = null;
        Throwable batchFailure = null;
        try {
            DrivePhotoUploader uploader = new DrivePhotoUploader(
                    getContentResolver(),
                    folderPrefs.getMasterTreeUri());
            DrivePhotoReconciler reconciler = new DrivePhotoReconciler(
                    getContentResolver(),
                    folderPrefs.getMasterTreeUri());
            PhotoUploadCoordinator coordinator = new PhotoUploadCoordinator(
                    photoStore,
                    photoPreparer,
                    uploader,
                    reconciler);
            PhotoReconciliationBatchRunner runner = new PhotoReconciliationBatchRunner();
            final int[] position = {0};
            batchResult = runner.run(photoIds, photoId -> {
                position[0]++;
                activeReconcilePhotoId = photoId;
                activeReconcilePosition = position[0];
                int currentPosition = position[0];
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    refreshPhotoList();
                    showPhotoStatus("Reconciling UNCERTAIN photo " + currentPosition + " of "
                            + photoIds.size() + " against its stored work-order destination…");
                });
                return attemptOneReconciliation(coordinator, photoId);
            });
        } catch (Throwable error) {
            batchFailure = error;
        } finally {
            activeReconcilePhotoId = null;
            activeReconcilePosition = 0;
            activeReconcileTotal = 0;
            UPLOAD_GATE.finish(BATCH_RECONCILE_GATE_ID);
        }

        final PhotoReconciliationBatchRunner.BatchResult completedBatch = batchResult;
        final Throwable completedFailure = batchFailure;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            refreshPhotoList();
            if (completedFailure != null || completedBatch == null) {
                showError("Bulk Drive reconciliation stopped unexpectedly; unresolved photos remain protected",
                        completedFailure == null
                                ? new IllegalStateException("Bulk reconciliation did not return a result.")
                                : completedFailure);
            } else {
                showPhotoStatus(formatReconciliationBatchResult(completedBatch));
            }
        });
    }

    private PhotoReconciliationBatchRunner.AttemptResult attemptOneReconciliation(
            PhotoUploadCoordinator coordinator,
            String photoId) throws Exception {
        PhotoUploadCoordinator.ReconciliationResult result = coordinator.reconcileUncertain(photoId);
        switch (result.outcome()) {
            case CONFIRMED_MATCH:
                boolean cleanupComplete = false;
                try {
                    ConfirmedPhotoCleanup.Result cleanup =
                            coordinator.cleanupConfirmedLocalData(photoId);
                    cleanupComplete = cleanup != null && cleanup.complete();
                } catch (Throwable ignored) {
                    cleanupComplete = false;
                }
                return PhotoReconciliationBatchRunner.AttemptResult.confirmed(cleanupComplete);
            case CONFIRMED_ABSENT_RETRY_SAFE:
                return PhotoReconciliationBatchRunner.AttemptResult.retrySafe();
            case REMAIN_UNCERTAIN:
            default:
                return PhotoReconciliationBatchRunner.AttemptResult.uncertain(result.detail());
        }
    }

    private String formatReconciliationBatchResult(
            PhotoReconciliationBatchRunner.BatchResult result) {
        StringBuilder summary = new StringBuilder();
        summary.append("Reconciliation finished: ")
                .append(result.checkedCount())
                .append(" checked · ")
                .append(result.confirmedCount())
                .append(" confirmed · ")
                .append(result.retrySafeCount())
                .append(" missing/retry-safe · ")
                .append(result.unresolvedCount())
                .append(" still unresolved.");
        if (result.errorCount() > 0) {
            summary.append(" ")
                    .append(result.errorCount())
                    .append(" check")
                    .append(result.errorCount() == 1 ? " had" : "s had")
                    .append(" an error and stayed protected.");
        }
        if (result.cleanupPendingCount() > 0) {
            summary.append(" ")
                    .append(result.cleanupPendingCount())
                    .append(" confirmed upload")
                    .append(result.cleanupPendingCount() == 1 ? " has" : "s have")
                    .append(" local cleanup pending; remote identity remains confirmed.");
        }
        return summary.toString();
    }

    private void confirmDiscardSelected() {
        if (selectedPhotoId == null || address == null || workOrder == null) {
            return;
        }
        if (UPLOAD_GATE.isBusy()) {
            showPhotoStatus("Wait for the active Drive operation to finish before discarding a protected photo.");
            renderSelectedPhoto();
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            showPhotoStatus("Wait for photo preparation to finish before discarding a protected photo.");
            renderSelectedPhoto();
            return;
        }
        String id = selectedPhotoId;
        try {
            PendingPhotoRecord record = photoStore.getById(id);
            if (record == null) {
                showPhotoStatus("The selected temporary photo no longer exists.");
                refreshPhotoList();
                return;
            }
            if (!record.canDiscardLocally()) {
                showPhotoStatus("This photo is " + record.state().name()
                        + ". It must be kept because upload/duplicate-protection evidence may still be needed.");
                renderSelectedPhoto();
                return;
            }
        } catch (Exception error) {
            showError("Could not verify whether the selected photo is safe to discard", error);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Discard temporary photo?")
                .setMessage("Work order: " + workOrder.name()
                        + "\nPhoto: …" + shortId(id)
                        + "\n\nThis removes this photo's app-private original and prepared copy. It does not delete anything from Drive.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Discard", (dialog, which) -> discardSelected(id))
                .show();
    }

    private void discardSelected(String id) {
        if (UPLOAD_GATE.isBusy()) {
            showPhotoStatus("Drive operation is still running. Nothing was discarded.");
            renderSelectedPhoto();
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            showPhotoStatus("Photo preparation is still running. Nothing was discarded.");
            renderSelectedPhoto();
            return;
        }
        try {
            LocalPhotoDiscardCoordinator coordinator =
                    new LocalPhotoDiscardCoordinator(photoStore, photoPreparer);
            coordinator.discardOne(id, address.id(), workOrder.id());
            batchSelectedPhotoIds.remove(id);
            if (id.equals(selectedPhotoId)) {
                selectedPhotoId = null;
            }
            showPhotoStatus("Selected temporary photo discarded locally. Drive was unchanged.");
        } catch (Exception error) {
            showError("Could not completely discard the selected temporary photo", error);
        }
        refreshPhotoList();
    }

    private boolean hasPreparedCopy(String id) {
        File prepared = getPreparedFileOrNull(id);
        return prepared != null && prepared.isFile() && prepared.length() > 0;
    }

    private boolean hasAnyLocalCopy(PendingPhotoRecord record) {
        try {
            File original = photoStore.imageFile(record);
            File prepared = photoPreparer.preparedFile(record.id());
            return original.exists() || prepared.exists();
        } catch (Exception ignored) {
            return true;
        }
    }

    private File getPreparedFileOrNull(String id) {
        try {
            return photoPreparer.preparedFile(id);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void renderScanWarnings(PendingPhotoStore.ScanResult result) {
        if (!result.corruptMetadataFiles().isEmpty()) {
            showPhotoStatus(result.corruptMetadataFiles().size()
                    + " temporary photo metadata record(s) need inspection. Paired image files were preserved.");
        } else if (!result.unusableQueuedPhotoIds().isEmpty()) {
            showPhotoStatus(result.unusableQueuedPhotoIds().size()
                    + " queued photo record(s) are missing usable protected image data. They were preserved for inspection.");
        } else if (!result.uncertainPhotoIds().isEmpty()) {
            showPhotoStatus(result.uncertainPhotoIds().size()
                    + " upload result(s) are UNCERTAIN. Use Reconcile All Uncertain to check them without retrying or changing Drive files.");
        } else {
            showPhotoStatus(null);
        }
    }

    private void renderScanWarningsIfNeeded(PendingPhotoStore.ScanResult result) {
        if (UPLOAD_GATE.isBusy()) {
            return;
        }
        if (!result.corruptMetadataFiles().isEmpty()) {
            showPhotoStatus(result.corruptMetadataFiles().size()
                    + " temporary photo metadata record(s) need inspection. Paired image files were preserved.");
        } else if (!result.unusableQueuedPhotoIds().isEmpty()) {
            showPhotoStatus(result.unusableQueuedPhotoIds().size()
                    + " queued photo record(s) are missing usable protected image data. They were preserved for inspection.");
        } else if (!result.uncertainPhotoIds().isEmpty()) {
            showPhotoStatus(result.uncertainPhotoIds().size()
                    + " upload result(s) are UNCERTAIN. Use Reconcile All Uncertain or select one for an individual check.");
        }
    }

    private PendingPhotoRecord findById(List<PendingPhotoRecord> records, String id) {
        for (PendingPhotoRecord record : records) {
            if (record.id().equals(id)) {
                return record;
            }
        }
        return null;
    }

    private String formatTime(long epochMs) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format(Locale.US, "%.1f MB", bytes / (1024d * 1024d));
        }
        if (bytes >= 1024L) {
            return String.format(Locale.US, "%.0f KB", bytes / 1024d);
        }
        return bytes + " B";
    }

    private String shortId(String id) {
        return id == null ? "unknown" : (id.length() <= 8 ? id : id.substring(id.length() - 8));
    }


    private void showPhotoStatus(String message) {
        if (statusText == null) {
            return;
        }
        statusText.setText(message == null ? "" : message);
        statusText.setVisibility(message == null || message.trim().isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showError(String prefix, Throwable error) {
        String detail = error == null ? null : error.getMessage();
        showPhotoStatus(prefix + (detail == null ? "." : ": " + detail));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
