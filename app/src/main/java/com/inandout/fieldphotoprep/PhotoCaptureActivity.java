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
            discardButton.setEnabled(false);
            return;
        }

        reconcileAndRefresh();
        if (PREPARATION_GATE.isBusy()) {
            statusText.setText("A photo is being prepared in the background. Protected originals are locked until it finishes.");
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
        phase.setText("Phase 6A · Photo preparation");
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
        refreshPhotoList();
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
            renderPhotoList(matching, scan.unusableWaitingPhotoIds());
            renderScanWarningsIfNeeded(scan);
        } catch (Exception error) {
            showError("Could not read temporary photos", error);
        }
    }

    private void renderPhotoList(
            List<PendingPhotoRecord> records,
            List<String> unusableWaitingIds) {
        pendingList.removeAllViews();
        pendingCountText.setText(records.size() + " temporary photo"
                + (records.size() == 1 ? "" : "s") + " for this work order");

        for (PendingPhotoRecord record : records) {
            Button photoButton = new Button(this);
            boolean unusable = unusableWaitingIds.contains(record.id());
            boolean prepared = hasPreparedCopy(record.id());
            boolean preparing = PREPARATION_GATE.isPreparing(record.id());
            String label = record.state().name()
                    + (unusable ? " · IMAGE MISSING" : "")
                    + (preparing ? " · PREPARING" : prepared ? " · PREPARED" : "")
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
                || discardButton == null
                || takePhotoButton == null) {
            return;
        }

        boolean preparationBusy = PREPARATION_GATE.isBusy();
        takePhotoButton.setEnabled(address != null
                && workOrder != null
                && pendingCaptureId == null
                && !preparationBusy);

        selectedPhotoText.setText(selectedPhotoId == null
                ? "Selected temporary photo: none"
                : "Selected temporary photo: …" + shortId(selectedPhotoId));
        if (selectedPhotoId == null) {
            preparedPhotoText.setText("Prepared copy: none selected");
            prepareButton.setEnabled(false);
            discardButton.setEnabled(false);
            return;
        }

        if (PREPARATION_GATE.isPreparing(selectedPhotoId)) {
            preparedPhotoText.setText("Prepared copy: preparing in background…");
        } else {
            File prepared = getPreparedFileOrNull(selectedPhotoId);
            preparedPhotoText.setText(prepared != null && prepared.isFile() && prepared.length() > 0
                    ? "Prepared copy: " + formatBytes(prepared.length())
                    : "Prepared copy: not created");
        }
        prepareButton.setEnabled(!preparationBusy);
        discardButton.setEnabled(!preparationBusy);
    }

    private void prepareSelectedPhoto() {
        final String photoId = selectedPhotoId;
        if (photoId == null) {
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
                statusText.setText("Wait for capture to finish before preparing this photo.");
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

    private void confirmDiscardSelected() {
        if (selectedPhotoId == null || workOrder == null) {
            return;
        }
        if (PREPARATION_GATE.isBusy()) {
            statusText.setText("Wait for photo preparation to finish before discarding a protected photo.");
            renderSelectedPhoto();
            return;
        }
        String id = selectedPhotoId;
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
        } else if (!result.unusableWaitingPhotoIds().isEmpty()) {
            statusText.setText(result.unusableWaitingPhotoIds().size()
                    + " waiting photo record(s) are missing usable image data. They were not reported as safe uploads.");
        } else {
            statusText.setText("Temporary photo protection ready.");
        }
    }

    private void renderScanWarningsIfNeeded(PendingPhotoStore.ScanResult result) {
        if (!result.corruptMetadataFiles().isEmpty()) {
            statusText.setText(result.corruptMetadataFiles().size()
                    + " temporary photo metadata record(s) need inspection. Paired image files were preserved.");
        } else if (!result.unusableWaitingPhotoIds().isEmpty()) {
            statusText.setText(result.unusableWaitingPhotoIds().size()
                    + " waiting photo record(s) are missing usable image data. They were preserved for inspection.");
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
        return id.length() <= 8 ? id : id.substring(id.length() - 8);
    }

    private void showError(String prefix, Throwable error) {
        String detail = error == null ? null : error.getMessage();
        statusText.setText(prefix + (detail == null ? "." : ": " + detail));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
