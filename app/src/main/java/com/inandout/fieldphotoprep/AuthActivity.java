package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AuthActivity extends Activity {
    private enum Mode {
        LOGIN,
        RECOVERY_REQUEST,
        PASSWORD_SETUP,
        CONNECTED
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SupabaseAuthClient authClient = new SupabaseAuthClient();

    private RuntimeAuthorizationManager authorizationManager;
    private TextView title;
    private TextView status;
    private EditText email;
    private EditText password;
    private EditText confirmPassword;
    private ProgressBar progress;
    private Button primary;
    private Button secondary;
    private Button close;

    private Mode mode = Mode.LOGIN;
    private SupabaseAuthClient.AuthTokens pendingRecoveryTokens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FieldPhotoPrepApplication app = (FieldPhotoPrepApplication) getApplication();
        authorizationManager = app.authorizationManager();
        setContentView(R.layout.screen_auth);
        bindViews();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void bindViews() {
        title = findViewById(R.id.auth_title);
        status = findViewById(R.id.auth_status);
        email = findViewById(R.id.auth_email);
        password = findViewById(R.id.auth_password);
        confirmPassword = findViewById(R.id.auth_confirm_password);
        progress = findViewById(R.id.auth_progress);
        primary = findViewById(R.id.auth_primary);
        secondary = findViewById(R.id.auth_secondary);
        close = findViewById(R.id.auth_close);
        close.setOnClickListener(v -> finish());
    }

    private void handleIntent(Intent intent) {
        String data = intent == null ? null : intent.getDataString();
        if (data == null || data.trim().isEmpty()) {
            AuthSessionState state = authorizationManager.storedSession();
            if (state == null) {
                showLogin(null);
            } else {
                AuthorizationDecision decision = authorizationManager.currentDecision();
                showConnected(state, storedSessionMessage(decision));
            }
            return;
        }

        AuthRedirectParser.Result redirect = AuthRedirectParser.parse(
                data,
                AuthConfig.redirectScheme(),
                AuthConfig.redirectHost());

        if (redirect.kind() == AuthRedirectParser.Kind.ERROR) {
            showLogin("The authentication link could not be used: " + redirect.errorMessage());
            return;
        }
        if ((redirect.kind() != AuthRedirectParser.Kind.RECOVERY
                && redirect.kind() != AuthRedirectParser.Kind.INVITE)
                || !redirect.hasSessionTokens()) {
            showLogin(redirect.errorMessage() == null
                    ? "The authentication link was incomplete or is not for this app."
                    : redirect.errorMessage());
            return;
        }

        setBusy(true);
        status.setText("Verifying the authentication link…");
        executor.execute(() -> {
            try {
                SupabaseAuthClient.AuthTokens tokens = authClient.tokensFromRedirect(
                        redirect.accessToken(),
                        redirect.refreshToken(),
                        redirect.expiresAtEpochSeconds());
                runOnUiThread(() -> {
                    pendingRecoveryTokens = tokens;
                    showPasswordSetup(
                            redirect.kind() == AuthRedirectParser.Kind.INVITE
                                    ? "Invitation accepted. Choose a password for this account."
                                    : "Choose a new password for this account.");
                });
            } catch (IOException e) {
                runOnUiThread(() -> showLogin("Could not verify the authentication link: " + e.getMessage()));
            }
        });
    }

    private void showLogin(String message) {
        mode = Mode.LOGIN;
        pendingRecoveryTokens = null;
        title.setText("Field Photo Prep Account");
        status.setText(message == null
                ? "Sign in to your Field Photo Prep organization."
                : message);
        email.setVisibility(View.VISIBLE);
        password.setVisibility(View.VISIBLE);
        confirmPassword.setVisibility(View.GONE);
        password.setHint("Password");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        primary.setText("Sign In");
        secondary.setText("Forgot Password");
        primary.setVisibility(View.VISIBLE);
        secondary.setVisibility(View.VISIBLE);
        primary.setOnClickListener(v -> signIn());
        secondary.setOnClickListener(v -> showRecoveryRequest());
        setBusy(false);
    }

    private void showRecoveryRequest() {
        mode = Mode.RECOVERY_REQUEST;
        title.setText("Reset Password");
        status.setText("Enter the account email. The reset email will open this Field Photo Prep build.");
        email.setVisibility(View.VISIBLE);
        password.setVisibility(View.GONE);
        confirmPassword.setVisibility(View.GONE);
        primary.setText("Send Recovery Email");
        secondary.setText("Back to Sign In");
        primary.setVisibility(View.VISIBLE);
        secondary.setVisibility(View.VISIBLE);
        primary.setOnClickListener(v -> sendRecovery());
        secondary.setOnClickListener(v -> showLogin(null));
        setBusy(false);
    }

    private void showPasswordSetup(String message) {
        mode = Mode.PASSWORD_SETUP;
        title.setText("Set Password");
        status.setText(message);
        email.setVisibility(View.GONE);
        password.setVisibility(View.VISIBLE);
        confirmPassword.setVisibility(View.VISIBLE);
        password.setText("");
        confirmPassword.setText("");
        password.setHint("New password");
        primary.setText("Set Password");
        secondary.setText("Cancel");
        primary.setVisibility(View.VISIBLE);
        secondary.setVisibility(View.VISIBLE);
        primary.setOnClickListener(v -> setRecoveredPassword());
        secondary.setOnClickListener(v -> showLogin(null));
        setBusy(false);
    }

    private String storedSessionMessage(AuthorizationDecision decision) {
        switch (decision.state()) {
            case VALIDATED:
                return "Account is active.";
            case GRACE:
                return "Account validation is temporarily offline. "
                        + "Field work remains available inside the current grace window.";
            case REVOKED:
                return "This membership is revoked. Existing protected work was kept.";
            case NO_MEMBERSHIP:
                return "This account does not currently have an active usable membership. "
                        + "Existing protected work was kept.";
            case RECHECK_REQUIRED:
                return "Account recheck is required before new field work or Drive writes. "
                        + "Existing protected work was kept.";
            case SIGN_IN_REQUIRED:
                return "Sign in again before new field work or Drive writes. "
                        + "Existing protected work was kept.";
            case DRIVE_DISCONNECTED:
            default:
                return "The account session is stored, but field work is currently restricted.";
        }
    }

    private void showConnected(AuthSessionState state, String message) {
        mode = Mode.CONNECTED;
        title.setText("Account Connected");
        status.setText(message
                + "\n\n"
                + state.email()
                + "\n"
                + state.organizationName()
                + "\n"
                + state.role());
        email.setVisibility(View.GONE);
        password.setVisibility(View.GONE);
        confirmPassword.setVisibility(View.GONE);
        primary.setText("Recheck Account");
        secondary.setText("Sign Out");
        secondary.setVisibility(View.VISIBLE);
        primary.setVisibility(View.VISIBLE);
        primary.setOnClickListener(v -> recheckStoredSession(state));
        secondary.setOnClickListener(v -> signOutSafely(state));
        setBusy(false);
    }

    private void signIn() {
        String emailValue = email.getText().toString().trim();
        String passwordValue = password.getText().toString();
        if (emailValue.trim().isEmpty() || passwordValue.trim().isEmpty()) {
            status.setText("Enter both email and password.");
            return;
        }

        setBusy(true);
        status.setText("Signing in…");
        executor.execute(() -> {
            try {
                SupabaseAuthClient.AuthTokens tokens = authClient.signInWithPassword(
                        emailValue,
                        passwordValue);
                AuthSessionState state = authClient.validateMembership(tokens);
                replaceAuthenticatedSessionSafely(state);
                runOnUiThread(() -> showConnected(state, "Signed in successfully."));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    status.setText("Sign in failed: " + e.getMessage());
                });
            }
        });
    }

    private void sendRecovery() {
        String emailValue = email.getText().toString().trim();
        if (emailValue.trim().isEmpty()) {
            status.setText("Enter the account email.");
            return;
        }

        setBusy(true);
        status.setText("Sending recovery email…");
        executor.execute(() -> {
            try {
                authClient.requestPasswordRecovery(emailValue, AuthConfig.redirectUri());
                runOnUiThread(() -> {
                    setBusy(false);
                    status.setText(
                            "Recovery email sent. Open it on this phone and tap the reset link. "
                                    + "It should return to Field Photo Prep.");
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    status.setText("Could not send recovery email: " + e.getMessage());
                });
            }
        });
    }

    private void setRecoveredPassword() {
        SupabaseAuthClient.AuthTokens tokens = pendingRecoveryTokens;
        if (tokens == null) {
            showLogin("The recovery session is no longer available. Request another recovery email.");
            return;
        }

        String first = password.getText().toString();
        String second = confirmPassword.getText().toString();
        if (first.length() < 8) {
            status.setText("Use at least 8 characters.");
            return;
        }
        if (!first.equals(second)) {
            status.setText("The passwords do not match.");
            return;
        }

        setBusy(true);
        status.setText("Updating password…");
        executor.execute(() -> {
            boolean passwordUpdated = false;
            try {
                authClient.updatePassword(tokens.accessToken(), first);
                passwordUpdated = true;

                SupabaseAuthClient.AuthTokens signedIn = authClient.signInWithPassword(
                        tokens.email(),
                        first);
                AuthSessionState state = authClient.validateMembership(signedIn);
                replaceAuthenticatedSessionSafely(state);
                runOnUiThread(() -> {
                    pendingRecoveryTokens = null;
                    showConnected(state, "Password updated and account verified.");
                });
            } catch (Exception e) {
                boolean changed = passwordUpdated;
                runOnUiThread(() -> {
                    if (changed) {
                        showLogin(
                                "Password was updated, but automatic sign-in did not finish. "
                                        + "Sign in with the new password.");
                        email.setText(tokens.email());
                    } else {
                        setBusy(false);
                        status.setText("Could not finish password setup: " + e.getMessage());
                    }
                });
            }
        });
    }

    private void recheckStoredSession(AuthSessionState stored) {
        setBusy(true);
        status.setText("Checking account…");
        authorizationManager.revalidateAsync().whenComplete((decision, error) ->
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (error != null || decision == null) {
                        setBusy(false);
                        status.setText(
                                "Could not recheck the account. Stored field data was not changed.");
                        return;
                    }

                    AuthSessionState current = authorizationManager.storedSession();
                    if (current == null
                            || decision.state() == AuthorizationDecision.State.SIGN_IN_REQUIRED) {
                        showLogin("Sign in again to continue Field Photo Prep work.");
                        return;
                    }

                    switch (decision.state()) {
                        case VALIDATED:
                            showConnected(current, "Account is active.");
                            break;
                        case GRACE:
                            showConnected(
                                    current,
                                    "The account service could not be reached. "
                                            + "Existing validation is still inside the offline grace window.");
                            break;
                        case REVOKED:
                            showConnected(
                                    current,
                                    "This membership is revoked. Existing protected work was kept.");
                            break;
                        case NO_MEMBERSHIP:
                            showConnected(
                                    current,
                                    "This account does not currently have an active usable membership. "
                                            + "Existing protected work was kept.");
                            break;
                        case RECHECK_REQUIRED:
                        case DRIVE_DISCONNECTED:
                        default:
                            showConnected(
                                    current,
                                    "Account recheck is required before new field work or Drive writes.");
                            break;
                    }
                }));
    }

    private void replaceAuthenticatedSessionSafely(AuthSessionState state)
            throws Exception {
        AuthSessionState previous = authorizationManager.storedSession();
        ProtectedWorkGuard.Result protectedWork = null;

        if (previous == null) {
            protectedWork = inspectProtectedWork();
            if (protectedWork.blocksSignOut()) {
                throw new IOException(
                        "Protected local work exists, but its previous Field Photo Prep identity "
                                + "is unavailable. The work was kept read-only and cannot be "
                                + "attached to a new account automatically.");
            }
        } else if (!previous.userId().equals(state.userId())
                || !previous.organizationId().equals(state.organizationId())) {
            protectedWork = inspectProtectedWork();
            if (protectedWork.blocksSignOut()) {
                throw new IOException(
                        "Protected work still belongs to the previously validated account. "
                                + "Resolve that work before switching Field Photo Prep identity.");
            }
        }
        authorizationManager.replaceAuthenticatedSession(state);
    }

    private void signOutSafely(AuthSessionState state) {
        setBusy(true);
        status.setText("Checking protected work before sign out…");
        executor.execute(() -> {
            final ProtectedWorkGuard.Result protectedWork;
            try {
                protectedWork = inspectProtectedWork();
            } catch (IOException error) {
                runOnUiThread(() -> {
                    setBusy(false);
                    status.setText(
                            "Sign out was not changed because protected local work could not be verified safely.");
                });
                return;
            }

            if (protectedWork.blocksSignOut()) {
                runOnUiThread(() -> {
                    setBusy(false);
                    status.setText(
                            "Sign out is blocked while "
                                    + protectedWork.blockingCount()
                                    + " protected photo"
                                    + (protectedWork.blockingCount() == 1 ? "" : "s")
                                    + " still need capture, upload, reconciliation, or confirmed cleanup. "
                                    + "Existing photos were not changed.");
                });
                return;
            }

            String accessToken = state == null ? null : state.accessToken();
            authorizationManager.clearAuthenticatedSession();
            runOnUiThread(() -> showLogin(
                    "Signed out. The Drive workspace and local field data were left unchanged."));

            if (accessToken != null && !accessToken.isBlank()) {
                try {
                    authClient.signOut(accessToken);
                } catch (IOException ignored) {
                    // Local sign-out is authoritative for this phone. A transient server failure
                    // must not restore the cleared local session.
                }
            }
        });
    }

    private ProtectedWorkGuard.Result inspectProtectedWork() throws IOException {
        PendingPhotoStore store = new PendingPhotoStore(
                new File(getFilesDir(), "pending_photos"));
        PhotoPreparer preparer = new PhotoPreparer(
                new File(getFilesDir(), "prepared_photos"));
        return new ProtectedWorkGuard(store, preparer).inspect();
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        primary.setEnabled(!busy);
        secondary.setEnabled(!busy);
        email.setEnabled(!busy);
        password.setEnabled(!busy);
        confirmPassword.setEnabled(!busy);
        close.setEnabled(!busy);
    }
}
