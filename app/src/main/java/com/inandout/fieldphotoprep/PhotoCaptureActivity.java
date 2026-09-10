package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class PhotoCaptureActivity extends Activity {
    private static final int REQUEST_CAPTURE_PHOTO = 2001;
    private static final String STATE_PENDING_CAPTURE_ID = "pending_capture_id";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private FolderPrefs folderPrefs;
    private PendingPhotoStore photoStore;
    private DriveFolder address;
    private DriveFolder workOrder;
    private String pendingCaptureId;
    private String selectedPhotoId;

    private TextView statusText;
    private TextView pendingCountText;
    private TextView selectedPhotoText;
    private LinearLayout pendingList;
    private Button takePhotoButton;
    private Button discardButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        folderPrefs = new FolderPrefs(this);
        photoStore = new PendingPhotoStore(new File(getFilesDir(), "pending_photos"));
        address = folderPrefs.getCurrentAddress();
        workOrder = folderPrefs.getCurrentWorkOrder();
        if (savedInstanceState != null) {
            pendingCaptureId = savedInstanceState.getString(STATE_PENDING_CAPTURE_ID);
        }
        buildUi();

        if (address == null || workOrder == null) {
            statusText.setText("Choose an exact address and work order before taking photos.");
            takePhotoButton.setEnabled(false);
            discardButton.setEnabled(false);
            return;
        }

        reconcileAndRefresh();
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
        phase.setText("Phase 5 · Photo capture");
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
        selectedPhotoText.setPadding(0, dp(12), 0, dp(6));
        root.addView(selectedPhotoText);

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
                statusText.setText("Photo protected locally and waiting for a future Drive upload.");
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
            String label = record.state().name()
                    + (unusable ? " · IMAGE MISSING" : "")
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
        if (selectedPhotoText == null || discardButton == null) {
            return;
        }
        selectedPhotoText.setText(selectedPhotoId == null
                ? "Selected temporary photo: none"
                : "Selected temporary photo: …" + shortId(selectedPhotoId));
        discardButton.setEnabled(selectedPhotoId != null);
    }

    private void confirmDiscardSelected() {
        if (selectedPhotoId == null || workOrder == null) {
            return;
        }
        String id = selectedPhotoId;
        new AlertDialog.Builder(this)
                .setTitle("Discard temporary photo?")
                .setMessage("Work order: " + workOrder.name()
                        + "\nPhoto: …" + shortId(id)
                        + "\n\nThis removes only this app-private temporary photo. It does not delete anything from Drive.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Discard", (dialog, which) -> discardSelected(id))
                .show();
    }

    private void discardSelected(String id) {
        try {
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

    private String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(id.length() - 8);
    }

    private void showError(String prefix, Throwable error) {
        String detail = error.getMessage();
        statusText.setText(prefix + (detail == null ? "." : ": " + detail));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
