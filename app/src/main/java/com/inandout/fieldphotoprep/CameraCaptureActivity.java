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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

public final class CameraCaptureActivity extends ComponentActivity {
    public static final String EXTRA_CAPTURE_ID = "capture_id";
    public static final String EXTRA_LAST_CAPTURE_ID = "last_capture_id";
    public static final String EXTRA_CAPTURED_COUNT = "captured_count";
    public static final String EXTRA_ERROR_MESSAGE = "camera_error_message";

    private static final int REQUEST_CAMERA_PERMISSION = 3101;
    private static final String STATE_ACTIVE_CAPTURE_ID = "active_capture_id";
    private static final String STATE_LAST_CAPTURE_ID = "last_capture_id";
    private static final String STATE_CAPTURED_COUNT = "captured_count";
    private static final String STATE_SESSION_BLOCKED = "session_blocked";

    private PendingPhotoStore photoStore;
    private DriveFolder sessionAddress;
    private DriveFolder sessionWorkOrder;
    private String initialCaptureId;
    private String activeCaptureId;
    private String lastCapturedPhotoId;
    private File outputFile;
    private int capturedCount;

    private PreviewView previewView;
    private TextView statusText;
    private TextView countText;
    private Button shutterButton;
    private Button doneButton;
    private ImageCapture imageCapture;
    private boolean cameraReady;
    private boolean captureInProgress;
    private boolean sessionBlocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        photoStore = new PendingPhotoStore(new File(getFilesDir(), "pending_photos"));
        initialCaptureId = getIntent().getStringExtra(EXTRA_CAPTURE_ID);

        if (savedInstanceState != null) {
            activeCaptureId = savedInstanceState.getString(STATE_ACTIVE_CAPTURE_ID);
            lastCapturedPhotoId = savedInstanceState.getString(STATE_LAST_CAPTURE_ID);
            capturedCount = savedInstanceState.getInt(STATE_CAPTURED_COUNT, 0);
            sessionBlocked = savedInstanceState.getBoolean(STATE_SESSION_BLOCKED, false);
        } else {
            activeCaptureId = initialCaptureId;
        }

        PendingPhotoRecord templateRecord;
        try {
            if (initialCaptureId == null || initialCaptureId.trim().isEmpty()) {
                throw new IllegalStateException("Camera opened without a tracked capture identity.");
            }

            templateRecord = resolveSessionTemplate();
            sessionAddress = new DriveFolder(templateRecord.addressId(), templateRecord.addressName());
            sessionWorkOrder = new DriveFolder(
                    templateRecord.workOrderId(),
                    templateRecord.workOrderName());

            if (activeCaptureId != null) {
                PendingPhotoRecord activeRecord = photoStore.getById(activeCaptureId);
                if (activeRecord == null) {
                    throw new IllegalStateException("The reserved capture record no longer exists.");
                }
                if (activeRecord.state() != PendingPhotoRecord.State.CAPTURING) {
                    throw new IllegalStateException(
                            "The active photo is not in CAPTURING state: "
                                    + activeRecord.state().name() + ".");
                }
                requireSameSessionDestination(activeRecord);
                outputFile = photoStore.imageFile(activeRecord);
            }
        } catch (Exception error) {
            finishWithError(error.getMessage());
            return;
        }

        buildUi(sessionWorkOrder.name());
        updateCountUi();

        if (sessionBlocked) {
            statusText.setText("This camera session needs inspection. Tap Done to return without taking another photo.");
            updateControlState();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            statusText.setText("Camera permission is required to take field photos.");
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (activeCaptureId != null) {
            outState.putString(STATE_ACTIVE_CAPTURE_ID, activeCaptureId);
        }
        if (lastCapturedPhotoId != null) {
            outState.putString(STATE_LAST_CAPTURE_ID, lastCapturedPhotoId);
        }
        outState.putInt(STATE_CAPTURED_COUNT, capturedCount);
        outState.putBoolean(STATE_SESSION_BLOCKED, sessionBlocked);
    }

    private PendingPhotoRecord resolveSessionTemplate() throws Exception {
        PendingPhotoRecord template = null;
        if (lastCapturedPhotoId != null) {
            template = photoStore.getById(lastCapturedPhotoId);
        }
        if (template == null) {
            template = photoStore.getById(initialCaptureId);
        }
        if (template == null) {
            throw new IllegalStateException("The camera session's protected photo identity is unavailable.");
        }
        return template;
    }

    private void requireSameSessionDestination(PendingPhotoRecord record) {
        if (!sessionAddress.id().equals(record.addressId())
                || !sessionAddress.name().equals(record.addressName())
                || !sessionWorkOrder.id().equals(record.workOrderId())
                || !sessionWorkOrder.name().equals(record.workOrderName())) {
            throw new IllegalStateException("Camera session destination identity changed unexpectedly.");
        }
    }

    private void buildUi(String workOrderName) {
        int pad = dp(12);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    pad + systemBars.left,
                    pad + systemBars.top,
                    pad + systemBars.right,
                    pad + systemBars.bottom);
            return windowInsets;
        });

        TextView title = new TextView(this);
        title.setText("Camera");
        title.setTextSize(22);
        root.addView(title);

        TextView workOrderText = new TextView(this);
        workOrderText.setText("Work order: " + workOrderName);
        workOrderText.setPadding(0, dp(4), 0, dp(4));
        root.addView(workOrderText);

        countText = new TextView(this);
        countText.setTextSize(16);
        countText.setPadding(0, 0, 0, dp(8));
        root.addView(countText);

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
        controls.setPadding(0, dp(8), 0, dp(8));
        controls.setClickable(true);
        controls.setFocusable(true);
        controls.setElevation(dp(8));

        doneButton = new Button(this);
        doneButton.setText("Done");
        doneButton.setMinHeight(dp(60));
        doneButton.setOnClickListener(v -> finishSession());
        controls.addView(
                doneButton,
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        shutterButton = new Button(this);
        shutterButton.setText("Take Photo");
        shutterButton.setMinHeight(dp(60));
        shutterButton.setOnClickListener(v -> capturePhoto());
        LinearLayout.LayoutParams shutterParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        shutterParams.setMarginStart(dp(12));
        controls.addView(shutterButton, shutterParams);

        root.addView(
                controls,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
        controls.bringToFront();

        setContentView(root);
        ViewCompat.requestApplyInsets(root);
    }

    private void startCamera() {
        cameraReady = false;
        imageCapture = null;
        statusText.setText("Starting camera…");
        updateControlState();

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
                statusText.setText(capturedCount == 0
                        ? "Ready — tap Take Photo."
                        : "Ready for the next photo.");
                updateControlState();
            } catch (Exception error) {
                cameraReady = false;
                imageCapture = null;
                statusText.setText("Camera could not start: " + safeMessage(error)
                        + ". Tap Done to return without taking another photo.");
                updateControlState();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        if (captureInProgress) {
            statusText.setText("Saving photo…");
            return;
        }
        if (sessionBlocked) {
            statusText.setText("This camera session needs inspection. Tap Done to return.");
            return;
        }
        if (!cameraReady || imageCapture == null) {
            statusText.setText("Camera is still starting. Wait for the Ready message.");
            return;
        }

        try {
            reserveCaptureIfNeeded();
        } catch (Exception error) {
            statusText.setText("Could not reserve the next protected photo: " + safeMessage(error));
            return;
        }

        captureInProgress = true;
        statusText.setText("Saving photo " + (capturedCount + 1) + "…");
        updateControlState();

        if (previewView.getDisplay() != null) {
            imageCapture.setTargetRotation(previewView.getDisplay().getRotation());
        }

        final String shotCaptureId = activeCaptureId;
        final File shotOutputFile = outputFile;
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(shotOutputFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        handleSavedPhoto(shotCaptureId, shotOutputFile);
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        handleCaptureError(shotCaptureId, exception);
                    }
                });
    }

    private void reserveCaptureIfNeeded() throws Exception {
        if (activeCaptureId != null) {
            PendingPhotoRecord existing = photoStore.getById(activeCaptureId);
            if (existing == null || existing.state() != PendingPhotoRecord.State.CAPTURING) {
                throw new IllegalStateException("The active protected-photo reservation is invalid.");
            }
            requireSameSessionDestination(existing);
            outputFile = photoStore.imageFile(existing);
            return;
        }

        PendingPhotoRecord next = photoStore.beginCapture(sessionAddress, sessionWorkOrder);
        requireSameSessionDestination(next);
        activeCaptureId = next.id();
        outputFile = photoStore.imageFile(next);
    }

    private void handleSavedPhoto(String shotCaptureId, File shotOutputFile) {
        if (!shotCaptureId.equals(activeCaptureId)) {
            blockSession("Camera returned a different capture identity than the active protected photo.");
            return;
        }
        if (!shotOutputFile.isFile() || shotOutputFile.length() <= 0) {
            handleCaptureFailureAfterCallback(
                    shotCaptureId,
                    "Camera returned without usable image data.");
            return;
        }

        try {
            PendingPhotoRecord waiting = photoStore.finishCaptureIfImageExists(shotCaptureId);
            if (waiting == null) {
                throw new IllegalStateException("Camera reported success but no protected image data remained.");
            }
            requireSameSessionDestination(waiting);
            recordCompletedShot(waiting);
            statusText.setText("Photo " + capturedCount + " saved — ready for the next photo.");
        } catch (Exception error) {
            blockSession("Photo data exists but its protected state needs inspection: " + safeMessage(error));
            return;
        }

        captureInProgress = false;
        updateControlState();
    }

    private void handleCaptureError(String shotCaptureId, ImageCaptureException exception) {
        handleCaptureFailureAfterCallback(
                shotCaptureId,
                "Camera could not save the photo: " + safeMessage(exception));
    }

    private void handleCaptureFailureAfterCallback(String shotCaptureId, String message) {
        if (!shotCaptureId.equals(activeCaptureId)) {
            blockSession("Camera failure returned for a different protected photo. Tap Done to inspect the session.");
            return;
        }

        try {
            PendingPhotoRecord preserved = photoStore.finishCaptureIfImageExists(shotCaptureId);
            if (preserved != null) {
                requireSameSessionDestination(preserved);
                recordCompletedShot(preserved);
                statusText.setText(message + " Non-empty image data was protected as photo "
                        + capturedCount + ". You can continue or tap Done.");
            } else {
                clearActiveCapture();
                statusText.setText(message + " No image data was kept. You can try again or tap Done.");
            }
            captureInProgress = false;
            updateControlState();
        } catch (Exception error) {
            blockSession(message + " Protected photo state also needs inspection: " + safeMessage(error));
        }
    }

    private void recordCompletedShot(PendingPhotoRecord record) {
        lastCapturedPhotoId = record.id();
        capturedCount++;
        clearActiveCapture();
        updateCountUi();
    }

    private void clearActiveCapture() {
        activeCaptureId = null;
        outputFile = null;
    }

    private void blockSession(String message) {
        captureInProgress = false;
        sessionBlocked = true;
        cameraReady = false;
        statusText.setText(message + " Tap Done to return without overwriting anything.");
        updateControlState();
    }

    private void finishSession() {
        if (captureInProgress) {
            statusText.setText("Saving photo… wait for it to finish before leaving the camera.");
            return;
        }

        try {
            PendingPhotoRecord preserved = finalizeUnusedOrInterruptedReservation();
            if (preserved != null) {
                requireSameSessionDestination(preserved);
                recordCompletedShot(preserved);
            }
        } catch (Exception error) {
            finishWithError("Camera session ended, but a protected photo needs inspection: "
                    + safeMessage(error));
            return;
        }

        Intent result = new Intent();
        result.putExtra(EXTRA_CAPTURED_COUNT, capturedCount);
        if (lastCapturedPhotoId != null) {
            result.putExtra(EXTRA_LAST_CAPTURE_ID, lastCapturedPhotoId);
        }
        setResult(capturedCount > 0 ? RESULT_OK : RESULT_CANCELED, result);
        finish();
    }

    private PendingPhotoRecord finalizeUnusedOrInterruptedReservation() throws Exception {
        if (activeCaptureId == null) {
            return null;
        }

        PendingPhotoRecord active = photoStore.getById(activeCaptureId);
        if (active == null) {
            clearActiveCapture();
            return null;
        }
        requireSameSessionDestination(active);
        if (active.state() != PendingPhotoRecord.State.CAPTURING) {
            clearActiveCapture();
            return active;
        }

        PendingPhotoRecord preserved = photoStore.finishCaptureIfImageExists(activeCaptureId);
        clearActiveCapture();
        return preserved;
    }

    private void updateCountUi() {
        if (countText == null) {
            return;
        }
        countText.setText(capturedCount + " photo" + (capturedCount == 1 ? "" : "s")
                + " captured this session");
    }

    private void updateControlState() {
        if (shutterButton == null || doneButton == null) {
            return;
        }
        shutterButton.setEnabled(cameraReady && !captureInProgress && !sessionBlocked);
        doneButton.setEnabled(!captureInProgress);
    }

    @Override
    public void onBackPressed() {
        finishSession();
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
        result.putExtra(EXTRA_CAPTURED_COUNT, capturedCount);
        if (lastCapturedPhotoId != null) {
            result.putExtra(EXTRA_LAST_CAPTURE_ID, lastCapturedPhotoId);
        }
        setResult(capturedCount > 0 ? RESULT_OK : RESULT_CANCELED, result);
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
