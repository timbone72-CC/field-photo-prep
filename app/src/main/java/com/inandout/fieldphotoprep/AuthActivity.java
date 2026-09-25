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

import java.io.IOException;
import java.security.GeneralSecurityException;
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

    private SecureAuthStore authStore;
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
        authStore = new SecureAuthStore(this);
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
        if (data == null || data.isBlank()) {
            AuthSessionState state = authStore.load();
            if (state != null && state.isActiveOwnerOrMember()) {
                showConnected(state, "Account session is stored on this phone.");
            } else {
                showLogin(null);
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
        primary.setOnClickListener(v -> setRecoveredPassword());
        secondary.setOnClickListener(v -> showLogin(null));
        setBusy(false);
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
        secondary.setVisibility(View.GONE);
        primary.setVisibility(View.VISIBLE);
        primary.setOnClickListener(v -> recheckStoredSession(state));
        setBusy(false);
    }

    private void signIn() {
        String emailValue = email.getText().toString().trim();
        String passwordValue = password.getText().toString();
        if (emailValue.isBlank() || passwordValue.isBlank()) {
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
                authStore.save(state);
                runOnUiThread(() -> showConnected(state, "Signed in successfully."));
            } catch (IOException | GeneralSecurityException e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    status.setText("Sign in failed: " + e.getMessage());
                });
            }
        });
    }

    private void sendRecovery() {
        String emailValue = email.getText().toString().trim();
        if (emailValue.isBlank()) {
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
        if (first.length() < 12) {
            status.setText("Use at least 12 characters.");
            return;
        }
        if (!first.equals(second)) {
            status.setText("The passwords do not match.");
            return;
        }

        setBusy(true);
        status.setText("Updating password…");
        executor.execute(() -> {
            try {
                authClient.updatePassword(tokens.accessToken(), first);
                AuthSessionState state = authClient.validateMembership(tokens);
                authStore.save(state);
                runOnUiThread(() -> {
                    pendingRecoveryTokens = null;
                    showConnected(state, "Password updated and account verified.");
                });
            } catch (IOException | GeneralSecurityException e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    status.setText("Could not finish password setup: " + e.getMessage());
                });
            }
        });
    }

    private void recheckStoredSession(AuthSessionState stored) {
        setBusy(true);
        status.setText("Checking account…");
        executor.execute(() -> {
            try {
                SupabaseAuthClient.AuthTokens refreshed = authClient.refreshSession(stored.refreshToken());
                AuthSessionState state = authClient.validateMembership(refreshed);
                authStore.save(state);
                runOnUiThread(() -> showConnected(state, "Account is active."));
            } catch (IOException | GeneralSecurityException e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    status.setText(
                            "Could not recheck the account. Stored field data was not changed.\n\n"
                                    + e.getMessage());
                });
            }
        });
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
