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
    private RecoveryGuidancePolicy.Guidance recoveryGuidance;

    private TextView statusMessage;
    private TextView recoverySafeText;
    private TextView recoveryBlockedText;
    private TextView recoveryProtectedText;
    private TextView recoveryNextText;
    private TextView accountText;
    private TextView driveText;
    private TextView queueText;
    private TextView appText;
    private Button recoveryActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FieldPhotoPrepApplication app = (FieldPhotoPrepApplication) getApplication();
        authorizationManager = app.authorizationManager();
        collector = AppStatusCollector.create(this, authorizationManager);

        setContentView(R.layout.screen_app_status);
        statusMessage = findViewById(R.id.app_status_message);
        recoverySafeText = findViewById(R.id.app_status_recovery_safe);
        recoveryBlockedText = findViewById(R.id.app_status_recovery_blocked);
        recoveryProtectedText = findViewById(R.id.app_status_recovery_protected);
        recoveryNextText = findViewById(R.id.app_status_recovery_next);
        recoveryActionButton = findViewById(R.id.app_status_recovery_action);
        accountText = findViewById(R.id.app_status_account);
        driveText = findViewById(R.id.app_status_drive);
        queueText = findViewById(R.id.app_status_queue);
        appText = findViewById(R.id.app_status_app);

        recoveryActionButton.setOnClickListener(v -> runRecoveryAction());
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
        recoveryGuidance = RecoveryGuidancePolicy.from(snapshot);

        statusMessage.setText(message == null ? "" : message);
        statusMessage.setVisibility(
                message == null || message.isBlank() ? View.GONE : View.VISIBLE);

        recoverySafeText.setText("Safe now: " + recoveryGuidance.safeNow());
        recoveryBlockedText.setText("Blocked now: " + recoveryGuidance.blockedNow());
        recoveryProtectedText.setText(
                "Protected: " + recoveryGuidance.protectedData());

        if (recoveryGuidance.action() == RecoveryGuidancePolicy.Action.NONE) {
            recoveryNextText.setText("Next action: No recovery action is required.");
        } else if (recoveryGuidance.action() == RecoveryGuidancePolicy.Action.CONTACT_OWNER) {
            recoveryNextText.setText(
                    "Next action: Contact an Organization Owner. "
                            + "After access is restored, recheck this account.");
        } else {
            recoveryNextText.setText("Next action: " + recoveryGuidance.actionLabel());
        }
        renderRecoveryActionButton();

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
                            + "\nUnreadable local records: " + counts.unreadable()
                            + "\nSign-out blocking protected items: "
                            + counts.signOutBlocking());
        }

        appText.setText(
                "Version: " + (snapshot.appVersion() == null ? "unknown" : snapshot.appVersion())
                        + "\nCamera permission: "
                        + (snapshot.cameraPermissionGranted() ? "Granted" : "Not granted"));
    }

    private void renderRecoveryActionButton() {
        recoveryActionButton.setEnabled(true);
        switch (recoveryGuidance.action()) {
            case SIGN_IN:
            case RECHECK_ACCOUNT:
            case CONNECT_DRIVE:
            case OPEN_PHOTOS:
                recoveryActionButton.setText(recoveryGuidance.actionLabel());
                recoveryActionButton.setVisibility(View.VISIBLE);
                break;
            case CONTACT_OWNER:
                recoveryActionButton.setText("Recheck After Access Is Restored");
                recoveryActionButton.setVisibility(View.VISIBLE);
                break;
            case NONE:
            default:
                recoveryActionButton.setVisibility(View.GONE);
                break;
        }
    }

    private void runRecoveryAction() {
        if (recoveryGuidance == null) {
            refreshStatus(null);
        }
        switch (recoveryGuidance.action()) {
            case SIGN_IN:
                startActivity(new Intent(this, AuthActivity.class));
                break;
            case RECHECK_ACCOUNT:
            case CONTACT_OWNER:
                recheckAccount();
                break;
            case CONNECT_DRIVE:
                openExistingConnectDriveFlow();
                break;
            case OPEN_PHOTOS:
                openExistingPhotosFlow();
                break;
            case NONE:
            default:
                break;
        }
    }

    private void recheckAccount() {
        recoveryActionButton.setEnabled(false);
        statusMessage.setText("Rechecking account…");
        statusMessage.setVisibility(View.VISIBLE);
        authorizationManager.revalidateAsync().whenComplete((decision, error) ->
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    recoveryActionButton.setEnabled(true);
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

    private void openExistingPhotosFlow() {
        Intent intent = new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_PHOTOS_FROM_STATUS, true)
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
