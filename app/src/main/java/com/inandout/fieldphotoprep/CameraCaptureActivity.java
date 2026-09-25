package com.inandout.fieldphotoprep;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import androidx.camera.core.CameraInfo;
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
    private static final String STATE_EFFECTIVE_ZOOM = "effective_zoom";
    private static final String STATE_PHYSICAL_WIDE = "physical_wide";
    private static final long ZOOM_SLIDER_HIDE_DELAY_MS = 2600L;

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
    private Button wideButton;
    private Button oneXButton;
    private Button threeXButton;
    private SeekBar zoomSlider;
    private LinearLayout zoomSliderPanel;
    private ScaleGestureDetector zoomGestureDetector;

    private ProcessCameraProvider cameraProvider;
    private Preview cameraPreview;
    private ImageCapture imageCapture;
    private Camera camera;
    private ZoomState lastZoomState;

    private CameraSelector physicalWideSelector;
    private float physicalWideIntrinsicRatio = 1f;
    private float logicalWideRatio = 1f;
    private float defaultMaxRatio = 1f;
    private float activeIntrinsicRatio = 1f;
    private float requestedEffectiveZoomRatio = 1f;
    private Float pendingEffectiveZoomRatio;
    private boolean restorePhysicalWideRequested;
    private boolean activePhysicalWide;
    private boolean logicalWideAvailable;
    private boolean physicalWideSuppressed;
    private boolean threeXAvailable;

    private CameraFlashMode flashMode = CameraFlashMode.AUTO;
    private boolean torchEnabled;
    private boolean hasFlashUnit;
    private boolean torchChangeInProgress;
    private boolean cameraReady;
    private boolean captureInProgress;
    private boolean sessionBlocked;
    private boolean updatingZoomSlider;
    private boolean zoomSliderTracking;
    private AuthorizationActionGuard authorizationGuard;

    private final Runnable hideZoomSliderRunnable = () -> {
        if (!zoomSliderTracking && zoomSliderPanel != null) {
            zoomSliderPanel.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FieldPhotoPrepApplication app = (FieldPhotoPrepApplication) getApplication();
        authorizationGuard = new AuthorizationActionGuard(app.authorizationManager());
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
            requestedEffectiveZoomRatio = validEffectiveRatio(
                    savedInstanceState.getFloat(STATE_EFFECTIVE_ZOOM, 1f));
            restorePhysicalWideRequested =
                    savedInstanceState.getBoolean(STATE_PHYSICAL_WIDE, false);
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
        refreshAllCameraUi();

        if (sessionBlocked) {
            statusText.setText(
                    "This camera session needs inspection. Tap Done to return without taking another photo.");
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
    protected void onResume() {
        super.onResume();
        FieldPhotoPrepApplication app = (FieldPhotoPrepApplication) getApplication();
        RuntimeAuthorizationManager manager = app.authorizationManager();
        if (manager == null) {
            return;
        }
        manager.revalidateAsync().thenAccept(decision -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || shutterButton == null) {
                return;
            }
            if (!decision.allowsNewCapture() && !captureInProgress) {
                statusText.setText(
                        "Account access needs to be rechecked before another photo. "
                                + "The current protected photos were kept.");
            }
            updateControlState();
        }));
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
        outState.putFloat(STATE_EFFECTIVE_ZOOM, requestedEffectiveZoomRatio);
        outState.putBoolean(STATE_PHYSICAL_WIDE, activePhysicalWide);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        buildUi(sessionWorkOrder == null ? "Unknown" : sessionWorkOrder.name());
        if (cameraPreview != null && previewView != null) {
            cameraPreview.setSurfaceProvider(previewView.getSurfaceProvider());
        }
        updateTargetRotation();
        refreshAllCameraUi();
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
            throw new IllegalStateException(
                    "The camera session's protected photo identity is unavailable.");
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
        if (zoomSliderPanel != null) {
            zoomSliderPanel.removeCallbacks(hideZoomSliderRunnable);
        }
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        previewView = new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setClickable(true);
        previewView.setFocusable(false);
        previewView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        installZoomGesture();
        root.addView(
                previewView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(10), dp(6), dp(10), dp(6));
        topBar.setBackgroundColor(Color.argb(145, 0, 0, 0));

        TextView workOrderText = new TextView(this);
        workOrderText.setText(workOrderName);
        workOrderText.setTextColor(Color.WHITE);
        workOrderText.setTextSize(15);
        workOrderText.setSingleLine(true);
        topBar.addView(
                workOrderText,
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        flashButton = makeCompactCameraButton("Flash");
        flashButton.setOnClickListener(v -> cycleFlashMode());
        topBar.addView(flashButton);

        torchButton = makeCompactCameraButton("Torch");
        torchButton.setOnClickListener(v -> toggleTorch());
        LinearLayout.LayoutParams torchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        torchParams.setMarginStart(dp(6));
        topBar.addView(torchButton, torchParams);

        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        if (landscape) {
            topParams.setMarginEnd(dp(112));
        }
        root.addView(topBar, topParams);

        statusText = new TextView(this);
        statusText.setText("Starting camera…");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(14);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(dp(10), dp(5), dp(10), dp(5));
        statusText.setBackground(makeRoundedBackground(Color.argb(125, 0, 0, 0), 18));
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        statusParams.topMargin = dp(58);
        if (landscape) {
            statusParams.rightMargin = dp(112);
        }
        root.addView(statusText, statusParams);

        buildZoomSliderOverlay(root, landscape);

        if (landscape) {
            buildLandscapeControls(root);
        } else {
            buildPortraitControls(root);
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });

        setContentView(root);
        ViewCompat.requestApplyInsets(root);
    }

    private void installZoomGesture() {
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
            boolean handled = zoomGestureDetector.onTouchEvent(event);
            if (handled) {
                showZoomSlider();
            }
            return handled;
        });
    }

    private void buildZoomSliderOverlay(FrameLayout root, boolean landscape) {
        zoomSliderPanel = new LinearLayout(this);
        zoomSliderPanel.setOrientation(LinearLayout.VERTICAL);
        zoomSliderPanel.setPadding(dp(14), dp(8), dp(14), dp(8));
        zoomSliderPanel.setBackground(makeRoundedBackground(Color.argb(185, 0, 0, 0), 18));
        zoomSliderPanel.setVisibility(View.GONE);

        zoomText = new TextView(this);
        zoomText.setText("Zoom: 1.0×");
        zoomText.setTextColor(Color.WHITE);
        zoomText.setTextSize(16);
        zoomText.setGravity(Gravity.CENTER);
        zoomSliderPanel.addView(
                zoomText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        zoomSlider = new SeekBar(this);
        zoomSlider.setMax(CameraZoomMath.SLIDER_STEPS);
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
                zoomSliderTracking = true;
                zoomSlider.removeCallbacks(hideZoomSliderRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                zoomSliderTracking = false;
                scheduleZoomSliderHide();
            }
        });
        zoomSliderPanel.addView(
                zoomSlider,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        int width = landscape ? dp(340) : FrameLayout.LayoutParams.MATCH_PARENT;
        FrameLayout.LayoutParams zoomParams = new FrameLayout.LayoutParams(
                width,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        if (landscape) {
            zoomParams.setMargins(dp(16), 0, dp(128), dp(16));
        } else {
            zoomParams.setMargins(dp(18), 0, dp(18), dp(142));
        }
        root.addView(zoomSliderPanel, zoomParams);
    }

    private void buildPortraitControls(FrameLayout root) {
        LinearLayout bottomPanel = new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setPadding(dp(12), dp(8), dp(12), dp(10));
        bottomPanel.setBackgroundColor(Color.argb(165, 0, 0, 0));

        LinearLayout zoomPresets = makeZoomPresetRow();
        bottomPanel.addView(
                zoomPresets,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout shutterRow = new LinearLayout(this);
        shutterRow.setOrientation(LinearLayout.HORIZONTAL);
        shutterRow.setGravity(Gravity.CENTER_VERTICAL);
        shutterRow.setPadding(0, dp(4), 0, 0);

        doneButton = makeBottomTextButton("Done");
        doneButton.setOnClickListener(v -> finishSession());
        shutterRow.addView(
                doneButton,
                new LinearLayout.LayoutParams(0, dp(72), 1f));

        shutterButton = makeShutterButton();
        shutterButton.setOnClickListener(v -> capturePhoto());
        LinearLayout.LayoutParams shutterParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        shutterParams.setMarginStart(dp(8));
        shutterParams.setMarginEnd(dp(8));
        shutterRow.addView(shutterButton, shutterParams);

        countText = new TextView(this);
        countText.setTextColor(Color.WHITE);
        countText.setTextSize(14);
        countText.setGravity(Gravity.CENTER);
        shutterRow.addView(
                countText,
                new LinearLayout.LayoutParams(0, dp(72), 1f));

        bottomPanel.addView(
                shutterRow,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(
                bottomPanel,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM));
    }

    private void buildLandscapeControls(FrameLayout root) {
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        rail.setPadding(dp(8), dp(8), dp(8), dp(8));
        rail.setBackgroundColor(Color.argb(170, 0, 0, 0));

        LinearLayout zoomPresets = makeZoomPresetColumn();
        rail.addView(
                zoomPresets,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f));

        shutterButton = makeShutterButton();
        shutterButton.setOnClickListener(v -> capturePhoto());
        rail.addView(
                shutterButton,
                new LinearLayout.LayoutParams(dp(72), dp(72)));

        doneButton = makeBottomTextButton("Done");
        doneButton.setOnClickListener(v -> finishSession());
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48));
        doneParams.topMargin = dp(8);
        rail.addView(doneButton, doneParams);

        countText = new TextView(this);
        countText.setTextColor(Color.WHITE);
        countText.setTextSize(13);
        countText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        countParams.topMargin = dp(4);
        rail.addView(countText, countParams);

        FrameLayout.LayoutParams railParams = new FrameLayout.LayoutParams(
                dp(112),
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.END);
        root.addView(rail, railParams);
    }

    private LinearLayout makeZoomPresetRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        wideButton = makeZoomPresetButton("0.5×");
        wideButton.setOnClickListener(v -> selectWidePreset());
        row.addView(wideButton);

        oneXButton = makeZoomPresetButton("1×");
        oneXButton.setOnClickListener(v -> selectDefaultPreset(1f));
        LinearLayout.LayoutParams oneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        oneParams.setMarginStart(dp(8));
        row.addView(oneXButton, oneParams);

        threeXButton = makeZoomPresetButton("3×");
        threeXButton.setOnClickListener(v -> selectDefaultPreset(3f));
        LinearLayout.LayoutParams threeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        threeParams.setMarginStart(dp(8));
        row.addView(threeXButton, threeParams);
        return row;
    }

    private LinearLayout makeZoomPresetColumn() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);

        wideButton = makeZoomPresetButton("0.5×");
        wideButton.setOnClickListener(v -> selectWidePreset());
        column.addView(wideButton);

        oneXButton = makeZoomPresetButton("1×");
        oneXButton.setOnClickListener(v -> selectDefaultPreset(1f));
        LinearLayout.LayoutParams oneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        oneParams.topMargin = dp(5);
        column.addView(oneXButton, oneParams);

        threeXButton = makeZoomPresetButton("3×");
        threeXButton.setOnClickListener(v -> selectDefaultPreset(3f));
        LinearLayout.LayoutParams threeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        threeParams.topMargin = dp(5);
        column.addView(threeXButton, threeParams);
        return column;
    }

    private Button makeCompactCameraButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(38));
        button.setMinimumHeight(0);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackground(makeRoundedBackground(Color.argb(150, 40, 40, 40), 18));
        return button;
    }

    private Button makeBottomTextButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setBackground(makeRoundedBackground(Color.argb(140, 45, 45, 45), 24));
        return button;
    }

    private Button makeZoomPresetButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setMinWidth(dp(52));
        button.setMinimumWidth(0);
        button.setMinHeight(dp(38));
        button.setMinimumHeight(0);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackground(makeRoundedBackground(Color.argb(175, 35, 35, 35), 22));
        return button;
    }

    private Button makeShutterButton() {
        Button button = new Button(this);
        button.setText("");
        button.setContentDescription("Take photo");
        GradientDrawable shutter = new GradientDrawable();
        shutter.setShape(GradientDrawable.OVAL);
        shutter.setColor(Color.WHITE);
        shutter.setStroke(dp(4), Color.argb(220, 210, 210, 210));
        button.setBackground(shutter);
        return button;
    }

    private GradientDrawable makeRoundedBackground(int color, int radiusDp) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radiusDp));
        return background;
    }

    private void startCamera() {
        cameraReady = false;
        imageCapture = null;
        cameraPreview = null;
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
                cameraProvider = providerFuture.get();
                boolean restorePhysical = restorePhysicalWideRequested;
                float restoreRatio = requestedEffectiveZoomRatio;
                restorePhysicalWideRequested = false;

                bindCamera(
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        1f,
                        false,
                        restorePhysical ? 1f : restoreRatio,
                        true);

                if (restorePhysical
                        && !logicalWideAvailable
                        && physicalWideSelector != null
                        && !physicalWideSuppressed) {
                    try {
                        bindCamera(
                                physicalWideSelector,
                                physicalWideIntrinsicRatio,
                                true,
                                restoreRatio,
                                false);
                    } catch (Exception error) {
                        physicalWideSuppressed = true;
                        activePhysicalWide = false;
                        activeIntrinsicRatio = 1f;
                        bindCamera(
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                1f,
                                false,
                                1f,
                                true);
                    }
                }
            } catch (Exception error) {
                cameraReady = false;
                imageCapture = null;
                cameraPreview = null;
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

    private void bindCamera(
            CameraSelector selector,
            float intrinsicRatio,
            boolean physicalWide,
            float targetEffectiveRatio,
            boolean discoverCapabilities) throws Exception {
        if (cameraProvider == null) {
            throw new IllegalStateException("Camera provider is not ready.");
        }

        if (camera != null) {
            camera.getCameraInfo().getZoomState().removeObservers(this);
            if (torchEnabled && hasFlashUnit) {
                try {
                    camera.getCameraControl().enableTorch(false);
                } catch (RuntimeException ignored) {
                    // Rebinding the camera also releases the prior torch session.
                }
            }
        }
        torchEnabled = false;
        torchChangeInProgress = false;
        cameraReady = false;
        lastZoomState = null;
        pendingEffectiveZoomRatio = validEffectiveRatio(targetEffectiveRatio);

        Preview preview = new Preview.Builder().build();
        ImageCapture boundImageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(toImageCaptureFlashMode(flashMode))
                .build();

        int rotation = currentDisplayRotation();
        preview.setTargetRotation(rotation);
        boundImageCapture.setTargetRotation(rotation);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        Camera boundCamera = cameraProvider.bindToLifecycle(
                this,
                selector,
                preview,
                boundImageCapture);

        cameraPreview = preview;
        imageCapture = boundImageCapture;
        camera = boundCamera;
        activeIntrinsicRatio = CameraLensMath.isUsableRatio(intrinsicRatio) ? intrinsicRatio : 1f;
        activePhysicalWide = physicalWide;
        hasFlashUnit = boundCamera.getCameraInfo().hasFlashUnit();

        if (discoverCapabilities) {
            discoverCameraCapabilities(boundCamera);
        }

        boundCamera.getCameraInfo().getZoomState().observe(this, zoomState -> {
            if (camera != boundCamera || zoomState == null) {
                return;
            }
            lastZoomState = zoomState;
            if (!activePhysicalWide) {
                logicalWideRatio = zoomState.getMinZoomRatio();
                logicalWideAvailable = CameraLensMath.isUltraWide(logicalWideRatio);
                defaultMaxRatio = zoomState.getMaxZoomRatio();
                threeXAvailable = defaultMaxRatio >= 2.95f;
            }
            float effective = CameraLensMath.effectiveRatio(
                    activeIntrinsicRatio,
                    zoomState.getZoomRatio());
            requestedEffectiveZoomRatio = effective;
            updateZoomUi();

            if (pendingEffectiveZoomRatio != null) {
                float requested = pendingEffectiveZoomRatio;
                pendingEffectiveZoomRatio = null;
                applyEffectiveZoom(requested);
            }
        });

        cameraReady = true;
        ZoomState initialZoom = boundCamera.getCameraInfo().getZoomState().getValue();
        if (initialZoom != null) {
            lastZoomState = initialZoom;
            updateZoomUi();
            if (pendingEffectiveZoomRatio != null) {
                float requested = pendingEffectiveZoomRatio;
                pendingEffectiveZoomRatio = null;
                applyEffectiveZoom(requested);
            }
        }

        statusText.setText(capturedCount == 0
                ? "Ready — frame the photo and tap the shutter."
                : "Ready for the next photo.");
        updateControlState();
    }

    private void discoverCameraCapabilities(Camera defaultCamera) {
        ZoomState defaultZoom = defaultCamera.getCameraInfo().getZoomState().getValue();
        if (defaultZoom != null) {
            logicalWideRatio = defaultZoom.getMinZoomRatio();
            logicalWideAvailable = CameraLensMath.isUltraWide(logicalWideRatio);
            defaultMaxRatio = defaultZoom.getMaxZoomRatio();
            threeXAvailable = defaultMaxRatio >= 2.95f;
        } else {
            logicalWideRatio = 1f;
            logicalWideAvailable = false;
            defaultMaxRatio = 1f;
            threeXAvailable = false;
        }

        physicalWideSelector = null;
        physicalWideIntrinsicRatio = 1f;
        if (physicalWideSuppressed) {
            updateQuickZoomUi();
            return;
        }

        CameraInfo best = null;
        float bestRatio = 1f;

        try {
            for (CameraInfo info : defaultCamera.getCameraInfo().getPhysicalCameraInfos()) {
                float ratio = safeIntrinsicRatio(info);
                if (isBackFacing(info)
                        && CameraLensMath.isUltraWide(ratio)
                        && (best == null || ratio < bestRatio)) {
                    best = info;
                    bestRatio = ratio;
                }
            }
        } catch (RuntimeException ignored) {
            // Some providers expose no physical-camera metadata.
        }

        if (best == null && cameraProvider != null) {
            try {
                for (CameraInfo info : cameraProvider.getAvailableCameraInfos()) {
                    float ratio = safeIntrinsicRatio(info);
                    if (isBackFacing(info)
                            && CameraLensMath.isUltraWide(ratio)
                            && (best == null || ratio < bestRatio)) {
                        best = info;
                        bestRatio = ratio;
                    }
                }
            } catch (RuntimeException ignored) {
                // Ordinary default-camera capture remains available.
            }
        }

        if (best != null) {
            try {
                physicalWideSelector = best.getCameraSelector();
                physicalWideIntrinsicRatio = bestRatio;
            } catch (RuntimeException ignored) {
                physicalWideSelector = null;
                physicalWideIntrinsicRatio = 1f;
            }
        }
        updateQuickZoomUi();
    }

    private boolean isBackFacing(CameraInfo info) {
        try {
            return info.getLensFacing() == CameraSelector.LENS_FACING_BACK;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private float safeIntrinsicRatio(CameraInfo info) {
        try {
            float ratio = info.getIntrinsicZoomRatio();
            return CameraLensMath.isUsableRatio(ratio) ? ratio : 1f;
        } catch (RuntimeException error) {
            return 1f;
        }
    }

    private void selectWidePreset() {
        if (!cameraReady || captureInProgress || sessionBlocked) {
            return;
        }
        showZoomSlider();

        if (logicalWideAvailable) {
            if (activePhysicalWide) {
                try {
                    bindCamera(
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            1f,
                            false,
                            logicalWideRatio,
                            true);
                } catch (Exception error) {
                    statusText.setText("Wide camera could not open: " + safeMessage(error));
                }
            } else {
                applyEffectiveZoom(logicalWideRatio);
            }
            return;
        }

        if (physicalWideSelector == null || physicalWideSuppressed) {
            statusText.setText("Ultra-wide is not exposed by this phone to the app.");
            return;
        }

        try {
            bindCamera(
                    physicalWideSelector,
                    physicalWideIntrinsicRatio,
                    true,
                    physicalWideIntrinsicRatio,
                    false);
            statusText.setText("Ultra-wide " + formatRatio(physicalWideIntrinsicRatio) + ".");
        } catch (Exception error) {
            physicalWideSuppressed = true;
            physicalWideSelector = null;
            statusText.setText("Ultra-wide could not open; normal 1× camera restored.");
            try {
                bindCamera(
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        1f,
                        false,
                        1f,
                        true);
            } catch (Exception restoreError) {
                cameraReady = false;
                statusText.setText("Camera needs to be reopened: " + safeMessage(restoreError));
                updateControlState();
            }
        }
    }

    private void selectDefaultPreset(float effectiveRatio) {
        if (!cameraReady || captureInProgress || sessionBlocked) {
            return;
        }
        showZoomSlider();

        if (activePhysicalWide) {
            try {
                bindCamera(
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        1f,
                        false,
                        effectiveRatio,
                        true);
            } catch (Exception error) {
                statusText.setText("Normal camera could not open: " + safeMessage(error));
            }
        } else {
            applyEffectiveZoom(effectiveRatio);
        }

        if (Math.abs(effectiveRatio - 1f) < 0.01f) {
            statusText.setText("Zoom reset to 1×.");
        }
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
        if (!cameraReady
                || camera == null
                || !hasFlashUnit
                || captureInProgress
                || torchChangeInProgress) {
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
            flashButton.setText("Flash —");
            torchButton.setText("Torch —");
        } else {
            flashButton.setText(compactFlashLabel());
            torchButton.setText(torchEnabled ? "Torch On" : "Torch Off");
        }
    }

    private String compactFlashLabel() {
        switch (flashMode) {
            case ON:
                return "Flash On";
            case OFF:
                return "Flash Off";
            case AUTO:
            default:
                return "Flash Auto";
        }
    }

    private boolean applyPinchZoom(float scaleFactor) {
        if (!canAdjustZoom()) {
            return false;
        }
        float requestedLocalRatio = CameraZoomMath.pinchTarget(
                lastZoomState.getZoomRatio(),
                scaleFactor,
                lastZoomState.getMinZoomRatio(),
                lastZoomState.getMaxZoomRatio());
        try {
            camera.getCameraControl().setZoomRatio(requestedLocalRatio);
            showZoomSlider();
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
        float requested = Math.max(0f, Math.min(1f, linearZoom));
        try {
            camera.getCameraControl().setLinearZoom(requested);
            showZoomSlider();
        } catch (RuntimeException error) {
            statusText.setText("Zoom could not change: " + safeMessage(error));
        }
    }

    private void applyEffectiveZoom(float effectiveRatio) {
        if (!canAdjustZoom()) {
            pendingEffectiveZoomRatio = validEffectiveRatio(effectiveRatio);
            return;
        }
        float localRatio = CameraLensMath.localRatioForEffective(
                effectiveRatio,
                activeIntrinsicRatio,
                lastZoomState.getMinZoomRatio(),
                lastZoomState.getMaxZoomRatio());
        try {
            camera.getCameraControl().setZoomRatio(localRatio);
        } catch (RuntimeException error) {
            statusText.setText("Zoom could not change: " + safeMessage(error));
        }
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
        if (zoomText == null || zoomSlider == null) {
            updateQuickZoomUi();
            return;
        }

        if (lastZoomState == null) {
            zoomText.setText(cameraReady ? "Zoom unavailable" : "Zoom starting…");
            updatingZoomSlider = true;
            zoomSlider.setProgress(0);
            updatingZoomSlider = false;
            zoomSlider.setEnabled(false);
            updateQuickZoomUi();
            return;
        }

        float effective = CameraLensMath.effectiveRatio(
                activeIntrinsicRatio,
                lastZoomState.getZoomRatio());
        requestedEffectiveZoomRatio = effective;
        zoomText.setText("Zoom " + formatRatio(effective));

        updatingZoomSlider = true;
        zoomSlider.setProgress(
                CameraZoomMath.progressFromLinearZoom(lastZoomState.getLinearZoom()));
        updatingZoomSlider = false;
        zoomSlider.setEnabled(canAdjustZoom());
        updateQuickZoomUi();
    }

    private void updateQuickZoomUi() {
        if (wideButton != null) {
            float wideRatio = currentWideRatio();
            boolean available = logicalWideAvailable
                    || (physicalWideSelector != null && !physicalWideSuppressed);
            wideButton.setVisibility(available ? View.VISIBLE : View.GONE);
            if (available) {
                wideButton.setText(formatRatio(wideRatio));
                wideButton.setEnabled(cameraReady && !captureInProgress && !sessionBlocked);
                wideButton.setAlpha(isNearCurrentRatio(wideRatio) ? 1f : 0.72f);
            }
        }

        if (oneXButton != null) {
            oneXButton.setEnabled(cameraReady && !captureInProgress && !sessionBlocked);
            oneXButton.setAlpha(isNearCurrentRatio(1f) ? 1f : 0.72f);
        }

        if (threeXButton != null) {
            threeXButton.setVisibility(threeXAvailable ? View.VISIBLE : View.GONE);
            threeXButton.setEnabled(
                    cameraReady && threeXAvailable && !captureInProgress && !sessionBlocked);
            threeXButton.setAlpha(isNearCurrentRatio(3f) ? 1f : 0.72f);
        }
    }

    private float currentWideRatio() {
        if (logicalWideAvailable) {
            return logicalWideRatio;
        }
        if (physicalWideSelector != null && !physicalWideSuppressed) {
            return physicalWideIntrinsicRatio;
        }
        return 1f;
    }

    private boolean isNearCurrentRatio(float ratio) {
        if (lastZoomState == null) {
            return false;
        }
        float effective = CameraLensMath.effectiveRatio(
                activeIntrinsicRatio,
                lastZoomState.getZoomRatio());
        return Math.abs(effective - ratio) < 0.08f;
    }

    private String formatRatio(float ratio) {
        if (Math.abs(ratio - Math.round(ratio)) < 0.04f) {
            return Math.round(ratio) + "×";
        }
        return String.format(Locale.US, "%.1f×", ratio);
    }

    private float validEffectiveRatio(float ratio) {
        return CameraLensMath.isUsableRatio(ratio) ? ratio : 1f;
    }

    private void showZoomSlider() {
        if (zoomSliderPanel == null) {
            return;
        }
        zoomSliderPanel.setVisibility(View.VISIBLE);
        scheduleZoomSliderHide();
    }

    private void scheduleZoomSliderHide() {
        if (zoomSliderPanel == null || zoomSliderTracking) {
            return;
        }
        zoomSliderPanel.removeCallbacks(hideZoomSliderRunnable);
        zoomSliderPanel.postDelayed(hideZoomSliderRunnable, ZOOM_SLIDER_HIDE_DELAY_MS);
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
            authorizationGuard.requireNewCapture();
            reserveCaptureIfNeeded();
        } catch (Exception error) {
            statusText.setText("Could not reserve the next protected photo: " + safeMessage(error));
            updateControlState();
            return;
        }

        captureInProgress = true;
        statusText.setText("Saving photo " + (capturedCount + 1) + "…");
        if (zoomSliderPanel != null) {
            zoomSliderPanel.setVisibility(View.GONE);
        }
        updateControlState();
        updateTargetRotation();

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

    private void updateTargetRotation() {
        int rotation = currentDisplayRotation();
        if (cameraPreview != null) {
            cameraPreview.setTargetRotation(rotation);
        }
        if (imageCapture != null) {
            imageCapture.setTargetRotation(rotation);
        }
    }

    private int currentDisplayRotation() {
        if (previewView != null && previewView.getDisplay() != null) {
            return previewView.getDisplay().getRotation();
        }
        return Surface.ROTATION_0;
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
                throw new IllegalStateException(
                        "Camera reported success but no protected image data remained.");
            }
            requireSameSessionDestination(waiting);
            recordCompletedShot(waiting);
            statusText.setText(
                    "Photo " + capturedCount + " saved — ready for the next photo.");
        } catch (Exception error) {
            blockSession(
                    "Photo data exists but its protected state needs inspection: "
                            + safeMessage(error));
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
            blockSession(
                    "Camera failure returned for a different protected photo. "
                            + "Tap Done to inspect the session.");
            return;
        }

        try {
            PendingPhotoRecord preserved = photoStore.finishCaptureIfImageExists(shotCaptureId);
            if (preserved != null) {
                requireSameSessionDestination(preserved);
                recordCompletedShot(preserved);
                statusText.setText(
                        message + " Non-empty image data was protected as photo "
                                + capturedCount + ". You can continue or tap Done.");
            } else {
                clearActiveCapture();
                statusText.setText(
                        message + " No image data was kept. You can try again or tap Done.");
            }
            captureInProgress = false;
            updateControlState();
        } catch (Exception error) {
            blockSession(
                    message + " Protected photo state also needs inspection: "
                            + safeMessage(error));
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
            statusText.setText(
                    "Wait for the torch setting to finish changing before leaving the camera.");
            return;
        }

        if (camera != null && hasFlashUnit && torchEnabled) {
            try {
                camera.getCameraControl().enableTorch(false);
            } catch (RuntimeException ignored) {
                // Lifecycle shutdown will also release the camera.
            }
        }

        try {
            PendingPhotoRecord preserved = finalizeUnusedOrInterruptedReservation();
            if (preserved != null) {
                requireSameSessionDestination(preserved);
                recordCompletedShot(preserved);
            }
        } catch (Exception error) {
            finishWithError(
                    "Camera session ended, but a protected photo needs inspection: "
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
        countText.setText(
                capturedCount + " photo" + (capturedCount == 1 ? "" : "s"));
    }

    private void refreshAllCameraUi() {
        updateCountUi();
        updateLightingUi();
        updateZoomUi();
        updateControlState();
    }

    private void updateControlState() {
        if (shutterButton == null || doneButton == null) {
            return;
        }
        shutterButton.setEnabled(
                cameraReady
                        && !captureInProgress
                        && !sessionBlocked
                        && !torchChangeInProgress
                        && authorizationAllowsCapture());
        shutterButton.setAlpha(shutterButton.isEnabled() ? 1f : 0.45f);
        doneButton.setEnabled(!captureInProgress && !torchChangeInProgress);
        doneButton.setAlpha(doneButton.isEnabled() ? 1f : 0.45f);

        if (flashButton != null) {
            flashButton.setEnabled(
                    cameraReady
                            && hasFlashUnit
                            && !captureInProgress
                            && !sessionBlocked
                            && !torchChangeInProgress);
            flashButton.setAlpha(flashButton.isEnabled() ? 1f : 0.5f);
        }
        if (torchButton != null) {
            torchButton.setEnabled(
                    cameraReady
                            && hasFlashUnit
                            && !captureInProgress
                            && !sessionBlocked
                            && !torchChangeInProgress);
            torchButton.setAlpha(torchButton.isEnabled() ? 1f : 0.5f);
        }
        if (zoomSlider != null) {
            zoomSlider.setEnabled(canAdjustZoom());
        }

        updateLightingUi();
        updateZoomUi();
    }

    private boolean authorizationAllowsCapture() {
        return authorizationGuard != null && authorizationGuard.allowsNewCapture();
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
        result.putExtra(
                EXTRA_ERROR_MESSAGE,
                message == null || message.trim().isEmpty()
                        ? "Camera operation failed."
                        : message);
        result.putExtra(EXTRA_CAPTURED_COUNT, capturedCount);
        if (lastCapturedPhotoId != null) {
            result.putExtra(EXTRA_LAST_CAPTURE_ID, lastCapturedPhotoId);
        }
        setResult(capturedCount > 0 ? RESULT_OK : RESULT_CANCELED, result);
        finish();
    }

    private String safeMessage(Throwable error) {
        if (error == null
                || error.getMessage() == null
                || error.getMessage().trim().isEmpty()) {
            return error == null ? "unknown error" : error.getClass().getSimpleName();
        }
        return error.getMessage();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
