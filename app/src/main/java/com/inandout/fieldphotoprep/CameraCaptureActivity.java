package com.inandout.fieldphotoprep;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.util.Locale;

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
    private static final String STATE_FLASH_MODE = "flash_mode";
    private static final String STATE_TORCH_ENABLED = "torch_enabled";
    private static final String STATE_LINEAR_ZOOM = "linear_zoom";

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
    private TextView zoomText;
    private Button shutterButton;
    private Button doneButton;
    private Button flashButton;
    private Button torchButton;
    private Button zoomResetButton;
    private SeekBar zoomSlider;
    private ScaleGestureDetector zoomGestureDetector;
    private ImageCapture imageCapture;
    private Camera camera;
    private ZoomState lastZoomState;
    private CameraFlashMode flashMode = CameraFlashMode.AUTO;
    private float requestedLinearZoom;
    private boolean torchEnabled;
    private boolean hasFlashUnit;
    private boolean torchChangeInProgress;
    private boolean cameraReady;
    private boolean captureInProgress;
    private boolean sessionBlocked;
    private boolean updatingZoomSlider;

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
            String savedFlashMode = savedInstanceState.getString(STATE_FLASH_MODE);
            if (savedFlashMode != null) {
                try {
                    flashMode = CameraFlashMode.valueOf(savedFlashMode);
                } catch (IllegalArgumentException ignored) {
                    flashMode = CameraFlashMode.AUTO;
                }
            }
            torchEnabled = savedInstanceState.getBoolean(STATE_TORCH_ENABLED, false);
            requestedLinearZoom = Math.max(
                    0f,
                    Math.min(1f, savedInstanceState.getFloat(STATE_LINEAR_ZOOM, 0f)));
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
        updateLightingUi();
        updateZoomUi();

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
        outState.putString(STATE_FLASH_MODE, flashMode.name());
        outState.putBoolean(STATE_TORCH_ENABLED, torchEnabled);
        outState.putFloat(STATE_LINEAR_ZOOM, requestedLinearZoom);
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
        countText.setPadding(0, 0, 0, dp(6));
        root.addView(countText);

        LinearLayout lightingControls = new LinearLayout(this);
        lightingControls.setOrientation(LinearLayout.HORIZONTAL);
        lightingControls.setGravity(Gravity.CENTER);
        lightingControls.setPadding(0, 0, 0, dp(8));

        flashButton = new Button(this);
        flashButton.setMinHeight(dp(48));
        flashButton.setOnClickListener(v -> cycleFlashMode());
        lightingControls.addView(
                flashButton,
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        torchButton = new Button(this);
        torchButton.setMinHeight(dp(48));
        torchButton.setOnClickListener(v -> toggleTorch());
        LinearLayout.LayoutParams torchParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        torchParams.setMarginStart(dp(8));
        lightingControls.addView(torchButton, torchParams);
        root.addView(lightingControls);

        LinearLayout zoomHeader = new LinearLayout(this);
        zoomHeader.setOrientation(LinearLayout.HORIZONTAL);
        zoomHeader.setGravity(Gravity.CENTER_VERTICAL);

        zoomText = new TextView(this);
        zoomText.setText("Zoom: 1.0×");
        zoomText.setTextSize(16);
        zoomHeader.addView(
                zoomText,
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        zoomResetButton = new Button(this);
        zoomResetButton.setText("Reset 1×");
        zoomResetButton.setMinHeight(dp(44));
        zoomResetButton.setOnClickListener(v -> resetZoom());
        zoomHeader.addView(
                zoomResetButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(zoomHeader);

        zoomSlider = new SeekBar(this);
        zoomSlider.setMax(CameraZoomMath.SLIDER_STEPS);
        zoomSlider.setProgress(CameraZoomMath.progressFromLinearZoom(requestedLinearZoom));
        zoomSlider.setContentDescription("Camera zoom slider");
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || updatingZoomSlider) {
                    return;
                }
                requestLinearZoom(CameraZoomMath.linearZoomFromProgress(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No durable state changes are owned by the slider.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // ZoomState observation keeps the readout synchronized with CameraX.
            }
        });
        root.addView(
                zoomSlider,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout previewFrame = new FrameLayout(this);
        LinearLayout.LayoutParams previewFrameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f);
        root.addView(previewFrame, previewFrameParams);

        previewView = new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setClickable(true);
        previewView.setFocusable(false);
        previewView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        zoomGestureDetector = new ScaleGestureDetector(
                this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        return applyPinchZoom(detector.getScaleFactor());
                    }
                });
        previewView.setOnTouchListener((view, event) -> {
            if (!cameraReady
                    || camera == null
                    || captureInProgress
                    || sessionBlocked
                    || zoomGestureDetector == null) {
                return false;
            }
            return zoomGestureDetector.onTouchEvent(event);
        });
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
        if (camera != null) {
            camera.getCameraInfo().getZoomState().removeObservers(this);
        }
        camera = null;
        lastZoomState = null;
        hasFlashUnit = false;
        torchChangeInProgress = false;
        statusText.setText("Starting camera…");
        updateControlState();

        final var providerFuture = ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = providerFuture.get();

                Preview preview = new Preview.Builder().build();
                ImageCapture boundImageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(toImageCaptureFlashMode(flashMode))
                        .build();

                if (previewView.getDisplay() != null) {
                    boundImageCapture.setTargetRotation(previewView.getDisplay().getRotation());
                } else {
                    boundImageCapture.setTargetRotation(Surface.ROTATION_0);
                }

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.unbindAll();
                Camera boundCamera = cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        boundImageCapture);

                imageCapture = boundImageCapture;
                camera = boundCamera;
                hasFlashUnit = boundCamera.getCameraInfo().hasFlashUnit();
                if (!hasFlashUnit) {
                    torchEnabled = false;
                } else if (torchEnabled) {
                    boolean restoreTorch = torchEnabled;
                    torchEnabled = false;
                    requestTorchState(restoreTorch, false);
                }

                boundCamera.getCameraInfo().getZoomState().observe(this, zoomState -> {
                    if (camera != boundCamera || zoomState == null) {
                        return;
                    }
                    lastZoomState = zoomState;
                    requestedLinearZoom = zoomState.getLinearZoom();
                    updateZoomUi();
                });
                boundCamera.getCameraControl().setLinearZoom(requestedLinearZoom);

                cameraReady = true;
                statusText.setText(capturedCount == 0
                        ? "Ready — tap Take Photo."
                        : "Ready for the next photo.");
                updateControlState();
            } catch (Exception error) {
                cameraReady = false;
                imageCapture = null;
                camera = null;
                lastZoomState = null;
                hasFlashUnit = false;
                torchEnabled = false;
                statusText.setText("Camera could not start: " + safeMessage(error)
                        + ". Tap Done to return without taking another photo.");
                updateControlState();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void cycleFlashMode() {
        if (!cameraReady || imageCapture == null || !hasFlashUnit || captureInProgress) {
            return;
        }
        flashMode = flashMode.next();
        imageCapture.setFlashMode(toImageCaptureFlashMode(flashMode));
        updateLightingUi();
        statusText.setText(flashMode.buttonLabel() + ".");
    }

    private void toggleTorch() {
        if (!cameraReady || camera == null || !hasFlashUnit || captureInProgress || torchChangeInProgress) {
            return;
        }
        requestTorchState(!torchEnabled, true);
    }

    private void requestTorchState(boolean requested, boolean announce) {
        if (camera == null || !hasFlashUnit) {
            torchEnabled = false;
            updateLightingUi();
            return;
        }

        final boolean previous = torchEnabled;
        torchEnabled = requested;
        torchChangeInProgress = true;
        updateLightingUi();
        updateControlState();

        final var torchFuture = camera.getCameraControl().enableTorch(requested);
        torchFuture.addListener(() -> {
            try {
                torchFuture.get();
                if (announce) {
                    statusText.setText(requested ? "Torch on." : "Torch off.");
                }
            } catch (Exception error) {
                torchEnabled = previous;
                statusText.setText("Torch could not change: " + safeMessage(error));
            } finally {
                torchChangeInProgress = false;
                updateLightingUi();
                updateControlState();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private int toImageCaptureFlashMode(CameraFlashMode mode) {
        switch (mode) {
            case ON:
                return ImageCapture.FLASH_MODE_ON;
            case OFF:
                return ImageCapture.FLASH_MODE_OFF;
            case AUTO:
            default:
                return ImageCapture.FLASH_MODE_AUTO;
        }
    }

    private void updateLightingUi() {
        if (flashButton == null || torchButton == null) {
            return;
        }
        if (cameraReady && !hasFlashUnit) {
            flashButton.setText("Flash: Unavailable");
            torchButton.setText("Torch: Unavailable");
        } else {
            flashButton.setText(flashMode.buttonLabel());
            torchButton.setText(torchEnabled ? "Torch: On" : "Torch: Off");
        }
    }

    private boolean applyPinchZoom(float scaleFactor) {
        if (!canAdjustZoom()) {
            return false;
        }
        float requestedRatio = CameraZoomMath.pinchTarget(
                lastZoomState.getZoomRatio(),
                scaleFactor,
                lastZoomState.getMinZoomRatio(),
                lastZoomState.getMaxZoomRatio());
        try {
            camera.getCameraControl().setZoomRatio(requestedRatio);
            return true;
        } catch (RuntimeException error) {
            statusText.setText("Zoom could not change: " + safeMessage(error));
            return false;
        }
    }

    private void requestLinearZoom(float linearZoom) {
        if (!canAdjustZoom()) {
            return;
        }
        requestedLinearZoom = Math.max(0f, Math.min(1f, linearZoom));
        try {
            camera.getCameraControl().setLinearZoom(requestedLinearZoom);
        } catch (RuntimeException error) {
            statusText.setText("Zoom could not change: " + safeMessage(error));
        }
    }

    private void resetZoom() {
        if (!canAdjustZoom()) {
            return;
        }
        requestLinearZoom(0f);
        statusText.setText("Zoom reset to 1×.");
    }

    private boolean canAdjustZoom() {
        return cameraReady
                && camera != null
                && lastZoomState != null
                && lastZoomState.getMaxZoomRatio() > lastZoomState.getMinZoomRatio() + 0.001f
                && !captureInProgress
                && !sessionBlocked;
    }

    private void updateZoomUi() {
        if (zoomText == null || zoomSlider == null || zoomResetButton == null) {
            return;
        }

        if (lastZoomState == null) {
            zoomText.setText(cameraReady ? "Zoom: unavailable" : "Zoom: starting…");
            updatingZoomSlider = true;
            zoomSlider.setProgress(CameraZoomMath.progressFromLinearZoom(requestedLinearZoom));
            updatingZoomSlider = false;
            zoomSlider.setEnabled(false);
            zoomResetButton.setEnabled(false);
            return;
        }

        zoomText.setText(String.format(Locale.US, "Zoom: %.1f×", lastZoomState.getZoomRatio()));
        updatingZoomSlider = true;
        zoomSlider.setProgress(CameraZoomMath.progressFromLinearZoom(lastZoomState.getLinearZoom()));
        updatingZoomSlider = false;

        boolean adjustable = canAdjustZoom();
        zoomSlider.setEnabled(adjustable);
        zoomResetButton.setEnabled(adjustable && lastZoomState.getLinearZoom() > 0.001f);
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
        if (torchChangeInProgress) {
            statusText.setText("Wait for the torch setting to finish changing.");
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
        if (torchChangeInProgress) {
            statusText.setText("Wait for the torch setting to finish changing before leaving the camera.");
            return;
        }

        if (camera != null && hasFlashUnit && torchEnabled) {
            try {
                camera.getCameraControl().enableTorch(false);
            } catch (RuntimeException ignored) {
                // Lifecycle shutdown will also release the camera. Photo state is independent of torch cleanup.
            }
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
        shutterButton.setEnabled(cameraReady
                && !captureInProgress
                && !sessionBlocked
                && !torchChangeInProgress);
        doneButton.setEnabled(!captureInProgress && !torchChangeInProgress);
        if (flashButton != null) {
            flashButton.setEnabled(cameraReady
                    && hasFlashUnit
                    && !captureInProgress
                    && !sessionBlocked
                    && !torchChangeInProgress);
        }
        if (torchButton != null) {
            torchButton.setEnabled(cameraReady
                    && hasFlashUnit
                    && !captureInProgress
                    && !sessionBlocked
                    && !torchChangeInProgress);
        }
        updateLightingUi();
        updateZoomUi();
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
        if (camera != null && hasFlashUnit && torchEnabled) {
            try {
                camera.getCameraControl().enableTorch(false);
            } catch (RuntimeException ignored) {
                // Best-effort torch shutdown; protected photo state remains authoritative.
            }
        }
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
