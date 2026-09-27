package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public final class AppStatusActivity extends Activity {
    private RuntimeAuthorizationManager authorizationManager;
    private AppStatusCollector collector;
    private AppStatusSnapshot snapshot;

    private TextView statusMessage;
    private TextView accountText;
    private TextView driveText;
    private TextView queueText;
    private TextView appText;
    private Button recheckButton;
    private Button connectDriveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FieldPhotoPrepApplication app = (FieldPhotoPrepApplication) getApplication();
        authorizationManager = app.authorizationManager();
        collector = AppStatusCollector.create(this, authorizationManager);

        setContentView(R.layout.screen_app_status);
        statusMessage = findViewById(R.id.app_status_message);
        accountText = findViewById(R.id.app_status_account);
        driveText = findViewById(R.id.app_status_drive);
        queueText = findViewById(R.id.app_status_queue);
        appText = findViewById(R.id.app_status_app);
        recheckButton = findViewById(R.id.app_status_recheck);
        connectDriveButton = findViewById(R.id.app_status_connect_drive);

        recheckButton.setOnClickListener(v -> recheckAccount());
        connectDriveButton.setOnClickListener(v -> openExistingConnectDriveFlow());
        findViewById(R.id.app_status_copy_support).setOnClickListener(v -> copySupportStatus());
        findViewById(R.id.app_status_close).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus(null);
    }

    private void refreshStatus(String message) {
        snapshot = collector.collect();
        statusMessage.setText(message == null ? "" : message);
        statusMessage.setVisibility(
                message == null || message.isBlank() ? View.GONE : View.VISIBLE);

        String organization = snapshot.organizationName() == null
                ? "Not available"
                : snapshot.organizationName();
        String role = snapshot.role() == null ? "Not available" : snapshot.role();
        String membership = snapshot.membershipStatus() == null
                ? "Not available"
                : snapshot.membershipStatus();

        accountText.setText(
                "State: " + snapshot.accountLabel()
                        + "\nOrganization: " + organization
                        + "\nRole: " + role
                        + "\nMembership: " + membership
                        + "\nLast validation: " + snapshot.lastValidationLabel()
                        + "\nGrace remaining: " + snapshot.graceRemainingLabel());

        String company = snapshot.currentCompanyName() == null
                ? "Not selected"
                : snapshot.currentCompanyName();
        driveText.setText(
                "State: " + snapshot.driveLabel()
                        + "\nClient company: " + company);

        AppStatusSnapshot.QueueCounts counts = snapshot.queueCounts();
        if (!counts.readable()) {
            queueText.setText(
                    "Local queue status is unavailable. Protected work was not changed.");
        } else {
            queueText.setText(
                    "Capturing: " + counts.capturing()
                            + "\nWaiting: " + counts.waiting()
                            + "\nUploading: " + counts.uploading()
                            + "\nFailed: " + counts.failed()
                            + "\nUncertain: " + counts.uncertain()
                            + "\nUploaded records: " + counts.uploaded()
                            + "\nProtected originals: " + counts.protectedOriginals()
                            + "\nCleanup pending: " + counts.cleanupPending()
                            + "\nUnreadable local records: " + counts.unreadable());
        }

        appText.setText(
                "Version: " + (snapshot.appVersion() == null ? "unknown" : snapshot.appVersion())
                        + "\nCamera permission: "
                        + (snapshot.cameraPermissionGranted() ? "Granted" : "Not granted"));

        recheckButton.setText(snapshot.requiresSignIn() ? "Sign In" : "Recheck Account");
        connectDriveButton.setVisibility(snapshot.canConnectDrive() ? View.VISIBLE : View.GONE);
    }

    private void recheckAccount() {
        if (snapshot == null || snapshot.requiresSignIn()) {
            startActivity(new Intent(this, AuthActivity.class));
            return;
        }

        recheckButton.setEnabled(false);
        statusMessage.setText("Rechecking account…");
        statusMessage.setVisibility(View.VISIBLE);
        authorizationManager.revalidateAsync().whenComplete((decision, error) ->
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    recheckButton.setEnabled(true);
                    refreshStatus(error == null
                            ? "Account status refreshed."
                            : "Account recheck could not complete. Protected work is unchanged.");
                }));
    }

    private void openExistingConnectDriveFlow() {
        Intent intent = new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_DRIVE_PICKER_FROM_STATUS, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void copySupportStatus() {
        if (snapshot == null) {
            refreshStatus(null);
        }
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(
                ClipData.newPlainText("Field Photo Prep Support Status", snapshot.supportSummary()));
        Toast.makeText(this, "Support status copied.", Toast.LENGTH_SHORT).show();
    }
}
