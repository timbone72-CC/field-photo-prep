package com.inandout.fieldphotoprep;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.io.File;

public final class CameraCaptureActivity extends ComponentActivity {
    public static final String EXTRA_CAPTURE_ID = "capture_id";
    public static final String EXTRA_ERROR_MESSAGE = "camera_error_message";

    private static final int REQUEST_CAMERA_PERMISSION = 3101;

    private PendingPhotoStore photoStore;
    private String captureId;
    private File outputFile;

    private PreviewView previewView;
    private TextView statusText;
    private Button shutterButton;
    private Button cancelButton;
    private ImageCapture imageCapture;
    private boolean cameraReady;
    private boolean captureInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        photoStore = new PendingPhotoStore(new File(getFilesDir(), "pending_photos"));
        captureId = getIntent().getStringExtra(EXTRA_CAPTURE_ID);

        PendingPhotoRecord record;
        try {
            if (captureId == null || captureId.trim().isEmpty()) {
                throw new IllegalStateException("Camera opened without a tracked capture identity.");
            }
            record = photoStore.getById(captureId);
            if (record == null) {
                throw new IllegalStateException("The reserved capture record no longer exists.");
            }
            if (record.state() != PendingPhotoRecord.State.CAPTURING) {
                throw new IllegalStateException(
                        "The reserved photo is not in CAPTURING state: " + record.state().name() + ".");
            }
            outputFile = photoStore.imageFile(record);
        } catch (Exception error) {
            finishWithError(error.getMessage());
            return;
        }

        buildUi(record.workOrderName());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            statusText.setText("Camera permission is required to take field photos.");
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void buildUi(String workOrderName) {
        int pad = dp(12);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Camera");
        title.setTextSize(22);
        root.addView(title);

        TextView workOrderText = new TextView(this);
        workOrderText.setText("Work order: " + workOrderName);
        workOrderText.setPadding(0, dp(4), 0, dp(8));
        root.addView(workOrderText);

        FrameLayout previewFrame = new FrameLayout(this);
        LinearLayout.LayoutParams previewFrameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f);
        root.addView(previewFrame, previewFrameParams);

        previewView = new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setClickable(false);
        previewView.setFocusable(false);
        previewView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        previewFrame.addView(
                previewView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        statusText = new TextView(this);
        statusText.setText("Starting camera…");
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        statusText.setPadding(0, dp(8), 0, dp(8));
        root.addView(statusText);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, dp(4), 0, 0);
        controls.setClickable(true);
        controls.setFocusable(true);
        controls.setElevation(dp(8));

        cancelButton = new Button(this);
        cancelButton.setText("Cancel");
        cancelButton.setMinHeight(dp(56));
        cancelButton.setOnClickListener(v -> cancelWithoutCapture());
        controls.addView(
                cancelButton,
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        shutterButton = new Button(this);
        shutterButton.setText("Take Photo");
        shutterButton.setMinHeight(dp(56));
        shutterButton.setOnClickListener(v -> capturePhoto());
        LinearLayout.LayoutParams shutterParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        shutterParams.setMarginStart(dp(8));
        controls.addView(shutterButton, shutterParams);

        root.addView(
                controls,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
        controls.bringToFront();

        setContentView(root);
    }

    private void startCamera() {
        cameraReady = false;
        imageCapture = null;
        statusText.setText("Starting camera…");

        final var providerFuture = ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = providerFuture.get();

                Preview preview = new Preview.Builder().build();
                ImageCapture boundImageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                if (previewView.getDisplay() != null) {
                    boundImageCapture.setTargetRotation(previewView.getDisplay().getRotation());
                } else {
                    boundImageCapture.setTargetRotation(Surface.ROTATION_0);
                }

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        boundImageCapture);

                imageCapture = boundImageCapture;
                cameraReady = true;
                statusText.setText("Ready — tap Take Photo.");
            } catch (Exception error) {
                cameraReady = false;
                imageCapture = null;
                statusText.setText("Camera could not start: " + safeMessage(error)
                        + ". Tap Cancel to return without saving a photo.");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        if (captureInProgress) {
            statusText.setText("Saving photo…");
            return;
        }
        if (!cameraReady || imageCapture == null) {
            statusText.setText("Camera is still starting. Wait for ‘Ready — tap Take Photo.’");
            return;
        }
        if (outputFile == null) {
            statusText.setText("The protected photo destination is unavailable. Cancel and reopen the camera.");
            return;
        }

        captureInProgress = true;
        shutterButton.setEnabled(false);
        cancelButton.setEnabled(false);
        statusText.setText("Saving photo…");

        if (previewView.getDisplay() != null) {
            imageCapture.setTargetRotation(previewView.getDisplay().getRotation());
        }

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        if (!outputFile.isFile() || outputFile.length() <= 0) {
                            finishWithError("Camera returned without usable image data.");
                            return;
                        }

                        Intent result = new Intent();
                        result.putExtra(EXTRA_CAPTURE_ID, captureId);
                        setResult(RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        captureInProgress = false;
                        shutterButton.setEnabled(true);
                        cancelButton.setEnabled(true);
                        statusText.setText("Photo was not saved: " + safeMessage(exception)
                                + ". You can try Take Photo again or Cancel.");
                    }
                });
    }

    private void cancelWithoutCapture() {
        if (captureInProgress) {
            statusText.setText("Saving photo…");
            return;
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (captureInProgress) {
            statusText.setText("Saving photo…");
            return;
        }
        cancelWithoutCapture();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CAMERA_PERMISSION) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            finishWithError("Camera permission was not granted. No photo was saved.");
        }
    }

    private void finishWithError(String message) {
        Intent result = new Intent();
        result.putExtra(EXTRA_ERROR_MESSAGE,
                message == null || message.trim().isEmpty() ? "Camera operation failed." : message);
        setResult(RESULT_CANCELED, result);
        finish();
    }

    private String safeMessage(Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()) {
            return error == null ? "unknown error" : error.getClass().getSimpleName();
        }
        return error.getMessage();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
