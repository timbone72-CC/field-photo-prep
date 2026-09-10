package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PhotoCaptureActivity extends Activity {
    private static final int REQUEST_CAPTURE_PHOTO = 2001;
    private static final String STATE_PENDING_CAPTURE_ID = "pending_capture_id";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, h:mm a");
    private static final PhotoPreparationGate PREPARATION_GATE = new PhotoPreparationGate();
    private static final PhotoUploadGate UPLOAD_GATE = new PhotoUploadGate();

    private FolderPrefs folderPrefs;
    private PendingPhotoStore photoStore;
    private PhotoPreparer photoPreparer;
    private DriveFolder address;
    private DriveFolder workOrder;
    private String pendingCaptureId;
    private String selectedPhotoId;

    private TextView statusText;
    private TextView pendingCountText;
    private TextView selectedPhotoText;
    private TextView preparedPhotoText;
    private LinearLayout pendingList;
    private Button takePhotoButton;
    private Button prepareButton;
    private Button uploadButton;
    private Button reconcileButton;
    private Button discardButton;

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
        }
        buildUi();

        if (address == null || workOrder == null) {
            statusText.setText("Choose an exact address and work order before taking photos.");
            takePhotoButton.setEnabled(false);
            prepareButton.setEnabled(false);
            uploadButton.setEnabled(false);
            reconcileButton.setEnabled(false);
            discardButton.setEnabled(false);
            return;
        }

        reconcileAndRefresh();
        if (PREPARATION_GATE.isBusy()) {
            statusText.setText("A photo is being prepared in the background. Protected originals are locked until it finishes.");
            renderSelectedPhoto();
        } else if (UPLOAD_GATE.isBusy()) {
            statusText.setText("A Drive upload or reconciliation check is already running. Wait for its queue result.");
            renderSelectedPhoto();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingCaptureId != null) {
            outState.putString(STATE_PENDING_CAPTURE_ID, pendingCaptureId);
        }
    }

    private void buildUi() {
        int pad = dp(16);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Field Photo Prep");
        title.setTextSize(24);
        root.addView(title);

        TextView phase = new TextView(this);
        phase.setText("Photos for selected work order");
        phase.setTextSize(14);
        root.addView(phase);

        statusText = new TextView(this);
        statusText.setPadding(0, dp(12), 0, dp(8));
        root.addView(statusText);

        TextView addressText = new TextView(this);
        addressText.setText(address == null ? "Address: none" : "Address: " + address.name());
        addressText.setTextSize(18);
        root.addView(addressText);

        TextView workOrderText = new TextView(this);
        workOrderText.setText(workOrder == null ? "Work order: none" : "Work order: " + workOrder.name());
        workOrderText.setTextSize(18);
        workOrderText.setPadding(0, dp(6), 0, 0);
        root.addView(workOrderText);

        TextView identityText = new TextView(this);
        identityText.setText(workOrder == null
                ? "Destination identity: none"
                : "Destination identity: …" + shortId(workOrder.id()));
        identityText.setPadding(0, dp(4), 0, dp(12));
        root.addView(identityText);

        takePhotoButton = new Button(this);
        takePhotoButton.setText("Take Photo");
        takePhotoButton.setOnClickListener(v -> beginCameraCapture());
        root.addView(takePhotoButton);

        pendingCountText = new TextView(this);
        pendingCountText.setTextSize(18);
        pendingCountText.setPadding(0, dp(16), 0, dp(6));
        root.addView(pendingCountText);

        pendingList = new LinearLayout(this);
        pendingList.setOrientation(LinearLayout.VERTICAL);
        root.addView(pendingList);

        selectedPhotoText = new TextView(this);
        selectedPhotoText.setPadding(0, dp(12), 0, dp(4));
        root.addView(selectedPhotoText);

        preparedPhotoText = new TextView(this);
        preparedPhotoText.setPadding(0, 0, 0, dp(6));
        root.addView(preparedPhotoText);

        prepareButton = new Button(this);
        prepareButton.setText("Prepare Selected Photo for Upload");
        prepareButton.setEnabled(false);
        prepareButton.setOnClickListener(v -> prepareSelectedPhoto());
        root.addView(prepareButton);

        uploadButton = new Button(this);
        uploadButton.setText("Upload Selected Prepared Photo");
        uploadButton.setEnabled(false);
        uploadButton.setOnClickListener(v -> uploadSelectedPhoto());
        root.addView(uploadButton);

        reconcileButton = new Button(this);
        reconcileButton.setText("Reconcile Uncertain Upload");
        reconcileButton.setEnabled(false);
        reconcileButton.setOnClickListener(v -> reconcileSelectedPhoto());
        root.addView(reconcileButton);

        discardButton = new Button(this);
        discardButton.setText("Discard Selected Temporary Photo");
        discardButton.setEnabled(false);
        discardButton.setOnClickListener(v -> confirmDiscardSelected());
        root.addView(discardButton);

        Button backButton = new Button(this);
        backButton.setText("Back to Work Order");
        backButton.setOnClickListener(v -> finish());
        root.addView(backButton);

        setContentView(scroll);
        renderSelectedPhoto();
    }

    private void beginCameraCapture() {
        if (address == null || workOrder == null) {
            statusText.setText("Choose an exact address and work order before taking photos.");
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            statusText.setText("Wait for photo preparation to finish before starting another capture.");
            return;
        }
        if (pendingCaptureId != null) {
            statusText.setText("A photo capture is already in progress.");
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
            Uri outputUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoStore.imageFile(record));
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
            cameraIntent.setClipData(ClipData.newRawUri("Field Photo Prep capture", outputUri));
            cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            statusText.setText("Opening camera for " + workOrder.name() + "…");
            startActivityForResult(cameraIntent, REQUEST_CAPTURE_PHOTO);
        } catch (ActivityNotFoundException error) {
            finishFailedCameraLaunch("No camera app is available", error);
        } catch (Exception error) {
            finishFailedCameraLaunch("Could not open the camera safely", error);
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

        String captureId = pendingCaptureId;
        pendingCaptureId = null;
        if (captureId == null) {
            statusText.setText("Camera returned without a tracked capture. Existing temporary photos were left unchanged.");
            refreshPhotoList();
            return;
        }

        try {
            PendingPhotoRecord preserved = photoStore.finishCaptureIfImageExists(captureId);
            if (preserved == null) {
                statusText.setText(resultCode == RESULT_OK
                        ? "Camera returned without usable image data. No empty photo was kept."
                        : "Camera cancelled. No photo data was saved.");
            } else if (resultCode == RESULT_OK) {
                statusText.setText("Photo protected locally and waiting for preparation/upload.");
            } else {
                statusText.setText("Camera did not report success, but image data exists, so the photo was preserved.");
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
                statusText.setText(incomplete
                        + " confirmed upload(s) still have local cleanup pending. Drive success remains confirmed.");
            } else if (cleaned > 0) {
                statusText.setText("Removed local image copies for " + cleaned
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
            renderPhotoList(matching, scan.unusableQueuedPhotoIds());
            renderScanWarningsIfNeeded(scan);
        } catch (Exception error) {
            showError("Could not read temporary photos", error);
        }
    }

    private void renderPhotoList(
            List<PendingPhotoRecord> records,
            List<String> unusableQueuedIds) {
        pendingList.removeAllViews();
        pendingCountText.setText(records.size() + " local photo record"
                + (records.size() == 1 ? "" : "s") + " for this work order");

        for (PendingPhotoRecord record : records) {
            Button photoButton = new Button(this);
            boolean unusable = unusableQueuedIds.contains(record.id());
            boolean prepared = hasPreparedCopy(record.id());
            boolean preparing = PREPARATION_GATE.isPreparing(record.id());
            boolean remoteBusy = UPLOAD_GATE.isUploading(record.id());
            String attempt = record.uploadAttemptCount() > 0
                    ? " · attempt " + record.uploadAttemptCount()
                    : "";
            String active = remoteBusy
                    ? (record.state() == PendingPhotoRecord.State.UNCERTAIN
                    ? " · RECONCILING"
                    : " · SENDING")
                    : preparing ? " · PREPARING" : prepared ? " · PREPARED" : "";
            String label = record.state().name() + attempt
                    + (unusable ? " · IMAGE MISSING" : "")
                    + active
                    + "\n" + formatTime(record.createdAtEpochMs())
                    + " · …" + shortId(record.id());
            photoButton.setText(label);
            photoButton.setOnClickListener(v -> {
                selectedPhotoId = record.id();
                renderSelectedPhoto();
            });
            pendingList.addView(photoButton);
        }
        renderSelectedPhoto();
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
        takePhotoButton.setEnabled(address != null
                && workOrder != null
                && pendingCaptureId == null
                && !preparationBusy);

        if (selectedPhotoId == null) {
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

        String attempt = selected.uploadAttemptCount() > 0
                ? " · attempt " + selected.uploadAttemptCount()
                : "";
        selectedPhotoText.setText("Selected temporary photo: …" + shortId(selectedPhotoId)
                + " · " + selected.state().name() + attempt);

        File prepared = getPreparedFileOrNull(selectedPhotoId);
        boolean hasPrepared = prepared != null && prepared.isFile() && prepared.length() > 0;
        boolean hasLocalCopy = hasAnyLocalCopy(selected);
        if (UPLOAD_GATE.isUploading(selectedPhotoId)) {
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
                preparedStatus += "\nUpload result: UNCERTAIN — reconcile before retry.";
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
            statusText.setText("Wait for the active Drive operation to finish before preparing another photo.");
            renderSelectedPhoto();
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            statusText.setText("A photo is already being prepared. Wait for it to finish before starting another.");
            renderSelectedPhoto();
            return;
        }

        try {
            PendingPhotoRecord record = photoStore.getById(photoId);
            if (record == null) {
                statusText.setText("The selected temporary photo no longer exists.");
                refreshPhotoList();
                return;
            }
            if (record.state() != PendingPhotoRecord.State.WAITING) {
                statusText.setText("Only a WAITING photo can be prepared. Current state: "
                        + record.state().name() + ".");
                renderSelectedPhoto();
                return;
            }
            if (!photoStore.hasImageData(record)) {
                statusText.setText("The protected original is missing or empty. Nothing was prepared.");
                return;
            }
        } catch (Exception error) {
            showError("Could not verify the selected photo before preparation", error);
            return;
        }

        if (!PREPARATION_GATE.tryBegin(photoId)) {
            statusText.setText("A photo is already being prepared. Wait for it to finish before starting another.");
            renderSelectedPhoto();
            return;
        }

        statusText.setText("Preparing photo in the background… Protected original is locked and remains safe.");
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
                statusText.setText("Prepared for upload: "
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
            statusText.setText("Wait for photo preparation to finish before uploading.");
            renderSelectedPhoto();
            return;
        }
        if (UPLOAD_GATE.isBusy()) {
            statusText.setText("A Drive upload or reconciliation check is already in progress.");
            renderSelectedPhoto();
            return;
        }

        try {
            PendingPhotoRecord record = photoStore.getById(photoId);
            if (record == null) {
                statusText.setText("The selected temporary photo no longer exists.");
                refreshPhotoList();
                return;
            }
            if (!record.canBeginUploadAttempt()) {
                statusText.setText("This photo cannot be uploaded from state "
                        + record.state().name() + ".");
                renderSelectedPhoto();
                return;
            }
            File prepared = photoPreparer.preparedFile(photoId);
            if (!prepared.isFile() || prepared.length() <= 0) {
                statusText.setText("Prepare this photo before uploading. Queue state was not changed.");
                renderSelectedPhoto();
                return;
            }
        } catch (Exception error) {
            showError("Could not verify the selected photo before upload", error);
            return;
        }

        if (!UPLOAD_GATE.tryBegin(photoId)) {
            statusText.setText("A Drive operation is already in progress.");
            renderSelectedPhoto();
            return;
        }

        statusText.setText("Starting Drive upload to the photo's stored work-order destination…");
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
                    statusText.setText(remote
                            + "Local original and prepared copy were removed; upload metadata was kept.");
                } else {
                    statusText.setText(remote
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
            statusText.setText("Wait for the active photo/Drive operation to finish before reconciliation.");
            renderSelectedPhoto();
            return;
        }

        try {
            PendingPhotoRecord record = photoStore.getById(photoId);
            if (record == null) {
                statusText.setText("The selected temporary photo no longer exists.");
                refreshPhotoList();
                return;
            }
            if (record.state() != PendingPhotoRecord.State.UNCERTAIN) {
                statusText.setText("Only an UNCERTAIN upload needs remote reconciliation.");
                renderSelectedPhoto();
                return;
            }
        } catch (Exception error) {
            showError("Could not read the uncertain photo before reconciliation", error);
            return;
        }

        if (!UPLOAD_GATE.tryBegin(photoId)) {
            statusText.setText("A Drive operation is already in progress.");
            renderSelectedPhoto();
            return;
        }

        statusText.setText(
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
                            statusText.setText(remote
                                    + "Upload is confirmed and local image copies were removed.");
                        } else {
                            statusText.setText(remote
                                    + "Upload is confirmed; local cleanup is incomplete but can be retried safely.");
                        }
                        break;
                    case CONFIRMED_ABSENT_RETRY_SAFE:
                        statusText.setText(
                                "Two settled Drive checks confirmed the expected remote photo is absent. Retry is now enabled for the original stored destination.");
                        break;
                    case REMAIN_UNCERTAIN:
                    default:
                        statusText.setText("Upload remains UNCERTAIN: " + completed.detail());
                        break;
                }
            }
            refreshPhotoList();
            renderSelectedPhoto();
        });
    }

    private void confirmDiscardSelected() {
        if (selectedPhotoId == null || workOrder == null) {
            return;
        }
        if (UPLOAD_GATE.isBusy()) {
            statusText.setText("Wait for the active Drive operation to finish before discarding a protected photo.");
            renderSelectedPhoto();
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            statusText.setText("Wait for photo preparation to finish before discarding a protected photo.");
            renderSelectedPhoto();
            return;
        }
        String id = selectedPhotoId;
        try {
            PendingPhotoRecord record = photoStore.getById(id);
            if (record == null) {
                statusText.setText("The selected temporary photo no longer exists.");
                refreshPhotoList();
                return;
            }
            if (!record.canDiscardLocally()) {
                statusText.setText("This photo is " + record.state().name()
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
            statusText.setText("Drive operation is still running. Nothing was discarded.");
            renderSelectedPhoto();
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            statusText.setText("Photo preparation is still running. Nothing was discarded.");
            renderSelectedPhoto();
            return;
        }
        try {
            File prepared = photoPreparer.preparedFile(id);
            if (prepared.exists() && !prepared.delete()) {
                throw new IllegalStateException("Could not remove the prepared copy. Protected original was left unchanged.");
            }
            photoStore.discard(id);
            if (id.equals(selectedPhotoId)) {
                selectedPhotoId = null;
            }
            statusText.setText("Selected temporary photo discarded locally. Drive was unchanged.");
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
            statusText.setText(result.corruptMetadataFiles().size()
                    + " temporary photo metadata record(s) need inspection. Paired image files were preserved.");
        } else if (!result.unusableQueuedPhotoIds().isEmpty()) {
            statusText.setText(result.unusableQueuedPhotoIds().size()
                    + " queued photo record(s) are missing usable protected image data. They were preserved for inspection.");
        } else if (!result.uncertainPhotoIds().isEmpty()) {
            statusText.setText(result.uncertainPhotoIds().size()
                    + " upload result(s) are UNCERTAIN after interruption. Select one and use Reconcile Uncertain Upload before retry.");
        } else {
            statusText.setText("Ready for photos. Select a photo below to prepare or upload it.");
        }
    }

    private void renderScanWarningsIfNeeded(PendingPhotoStore.ScanResult result) {
        if (!result.corruptMetadataFiles().isEmpty()) {
            statusText.setText(result.corruptMetadataFiles().size()
                    + " temporary photo metadata record(s) need inspection. Paired image files were preserved.");
        } else if (!result.unusableQueuedPhotoIds().isEmpty()) {
            statusText.setText(result.unusableQueuedPhotoIds().size()
                    + " queued photo record(s) are missing usable protected image data. They were preserved for inspection.");
        } else if (!result.uncertainPhotoIds().isEmpty()) {
            statusText.setText(result.uncertainPhotoIds().size()
                    + " upload result(s) are UNCERTAIN. Select one and reconcile it before retry.");
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

    private void showError(String prefix, Throwable error) {
        String detail = error == null ? null : error.getMessage();
        statusText.setText(prefix + (detail == null ? "." : ": " + detail));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
