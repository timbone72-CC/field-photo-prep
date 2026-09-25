package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MemberAdminActivity extends Activity {
    private interface Mutation {
        SupabaseAuthClient.AdminResult run(AuthSessionState state) throws Exception;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SupabaseAuthClient authClient = new SupabaseAuthClient();

    private RuntimeAuthorizationManager authorizationManager;
    private TextView organizationText;
    private TextView statusText;
    private ProgressBar progress;
    private EditText inviteEmail;
    private Spinner inviteRole;
    private Button inviteButton;
    private Button refreshButton;
    private Button closeButton;
    private LinearLayout membersContainer;
    private LinearLayout invitationsContainer;
    private boolean busy;
    private boolean adminAllowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authorizationManager =
                ((FieldPhotoPrepApplication) getApplication()).authorizationManager();
        setContentView(R.layout.screen_member_admin);
        bindViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFromServer();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void bindViews() {
        organizationText = findViewById(R.id.member_admin_org);
        statusText = findViewById(R.id.member_admin_status);
        progress = findViewById(R.id.member_admin_progress);
        inviteEmail = findViewById(R.id.member_admin_invite_email);
        inviteRole = findViewById(R.id.member_admin_invite_role);
        inviteButton = findViewById(R.id.member_admin_invite);
        refreshButton = findViewById(R.id.member_admin_refresh);
        closeButton = findViewById(R.id.member_admin_close);
        membersContainer = findViewById(R.id.member_admin_members);
        invitationsContainer = findViewById(R.id.member_admin_invitations);

        inviteRole.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"MEMBER", "OWNER"}));

        inviteButton.setOnClickListener(v -> sendInvitation());
        refreshButton.setOnClickListener(v -> refreshFromServer());
        closeButton.setOnClickListener(v -> finish());
        setAdminAllowed(false);
    }

    private void refreshFromServer() {
        if (busy) {
            return;
        }
        setBusy(true, "Checking Owner authorization…");
        authorizationManager.revalidateAsync().whenComplete((decision, error) -> {
            if (error != null || decision == null || !decision.allowsMemberAdministration()) {
                runOnUiThread(() -> {
                    setBusy(false, null);
                    setAdminAllowed(false);
                    membersContainer.removeAllViews();
                    invitationsContainer.removeAllViews();
                    showStatus(
                            "Member administration requires a current online VALIDATED OWNER session.");
                });
                return;
            }

            AuthSessionState state = authorizationManager.storedSession();
            if (state == null
                    || !state.organizationId().equals(decision.organizationId())
                    || !"OWNER".equals(state.role())
                    || !"ACTIVE".equals(state.membershipStatus())) {
                runOnUiThread(() -> {
                    setBusy(false, null);
                    setAdminAllowed(false);
                    showStatus("Owner identity changed. Recheck the account before administering members.");
                });
                return;
            }

            loadLists(state, "Members refreshed.");
        });
    }

    private void loadLists(AuthSessionState state, String successMessage) {
        executor.execute(() -> {
            try {
                List<SupabaseAuthClient.AdminMember> members =
                        authClient.listAdminMembers(state.accessToken(), state.organizationId());
                List<SupabaseAuthClient.AdminInvitation> invitations =
                        authClient.listAdminInvitations(state.accessToken(), state.organizationId());

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    organizationText.setText(state.organizationName());
                    renderMembers(state, members);
                    renderInvitations(state, invitations);
                    setAdminAllowed(true);
                    setBusy(false, null);
                    showStatus(successMessage);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setAdminAllowed(false);
                    setBusy(false, null);
                    showStatus("Could not refresh member administration: " + safeMessage(e));
                });
            }
        });
    }

    private void sendInvitation() {
        String email = inviteEmail.getText().toString().trim().toLowerCase();
        String role = String.valueOf(inviteRole.getSelectedItem());
        if (email.isEmpty() || !email.contains("@")) {
            showStatus("Enter the email address to invite.");
            return;
        }

        performMutation(
                "Sending invitation…",
                false,
                state -> authClient.sendOwnerInvitation(
                        state.accessToken(),
                        state.organizationId(),
                        email,
                        role,
                        AuthConfig.redirectUri()),
                () -> inviteEmail.setText(""));
    }

    private void changeInvitationRole(
            SupabaseAuthClient.AdminInvitation invitation,
            String newRole) {
        performMutation(
                "Changing invitation role…",
                false,
                state -> authClient.updateInvitationRole(
                        state.accessToken(),
                        state.organizationId(),
                        invitation.invitationId(),
                        newRole),
                null);
    }

    private void cancelInvitation(SupabaseAuthClient.AdminInvitation invitation) {
        performMutation(
                "Cancelling invitation…",
                false,
                state -> authClient.cancelInvitation(
                        state.accessToken(),
                        state.organizationId(),
                        invitation.invitationId()),
                null);
    }

    private void resendInvitation(SupabaseAuthClient.AdminInvitation invitation) {
        performMutation(
                "Resending invitation…",
                false,
                state -> authClient.sendOwnerInvitation(
                        state.accessToken(),
                        state.organizationId(),
                        invitation.email(),
                        invitation.intendedRole(),
                        AuthConfig.redirectUri()),
                null);
    }

    private void changeMemberRole(
            AuthSessionState current,
            SupabaseAuthClient.AdminMember member,
            String newRole) {
        boolean affectsCurrent = current.membershipId().equals(member.membershipId());
        performMutation(
                "Changing member role…",
                affectsCurrent,
                state -> authClient.changeMembershipRole(
                        state.accessToken(),
                        state.organizationId(),
                        member.membershipId(),
                        newRole),
                null);
    }

    private void revokeMember(
            AuthSessionState current,
            SupabaseAuthClient.AdminMember member) {
        boolean affectsCurrent = current.membershipId().equals(member.membershipId());
        performMutation(
                "Revoking membership…",
                affectsCurrent,
                state -> {
                    if (affectsCurrent) {
                        ProtectedWorkGuard.Result protectedWork = inspectProtectedWork();
                        if (protectedWork.blocksSignOut()) {
                            throw new IOException(
                                    "Self-revocation is blocked while "
                                            + protectedWork.blockingCount()
                                            + " protected photo"
                                            + (protectedWork.blockingCount() == 1 ? "" : "s")
                                            + " still need resolution.");
                        }
                    }
                    return authClient.revokeMembership(
                            state.accessToken(),
                            state.organizationId(),
                            member.membershipId());
                },
                null);
    }

    private void reactivateMember(SupabaseAuthClient.AdminMember member) {
        performMutation(
                "Reactivating membership…",
                false,
                state -> authClient.reactivateMembership(
                        state.accessToken(),
                        state.organizationId(),
                        member.membershipId()),
                null);
    }

    private void performMutation(
            String workingMessage,
            boolean affectsCurrentMembership,
            Mutation mutation,
            Runnable afterSuccess) {
        if (busy) {
            return;
        }
        setBusy(true, workingMessage);

        authorizationManager.revalidateAsync().whenComplete((decision, error) -> {
            if (error != null || decision == null || !decision.allowsMemberAdministration()) {
                runOnUiThread(() -> {
                    setBusy(false, null);
                    setAdminAllowed(false);
                    showStatus(
                            "Member administration requires a current online VALIDATED OWNER session.");
                });
                return;
            }

            AuthSessionState state = authorizationManager.storedSession();
            if (state == null || !state.organizationId().equals(decision.organizationId())) {
                runOnUiThread(() -> {
                    setBusy(false, null);
                    setAdminAllowed(false);
                    showStatus("Owner identity changed. Recheck the account and try again.");
                });
                return;
            }

            executor.execute(() -> {
                try {
                    SupabaseAuthClient.AdminResult result = mutation.run(state);
                    if (!result.succeeded()) {
                        throw new IOException(messageForOutcome(result.outcome()));
                    }

                    runOnUiThread(() -> {
                        if (afterSuccess != null) {
                            afterSuccess.run();
                        }
                    });

                    if (affectsCurrentMembership) {
                        authorizationManager.revalidateAsync().whenComplete((after, recheckError) ->
                                runOnUiThread(() -> {
                                    if (recheckError != null
                                            || after == null
                                            || !after.allowsMemberAdministration()) {
                                        membersContainer.removeAllViews();
                                        invitationsContainer.removeAllViews();
                                        setAdminAllowed(false);
                                        setBusy(false, null);
                                        showStatus(
                                                "Your Membership changed. Owner administration is no longer available on this account.");
                                    } else {
                                        AuthSessionState refreshed =
                                                authorizationManager.storedSession();
                                        if (refreshed == null) {
                                            setBusy(false, null);
                                            setAdminAllowed(false);
                                            showStatus("Account recheck is required.");
                                        } else {
                                            loadLists(refreshed, messageForOutcome(result.outcome()));
                                        }
                                    }
                                }));
                    } else {
                        loadLists(state, messageForOutcome(result.outcome()));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        setBusy(false, null);
                        setAdminAllowed(true);
                        showStatus("Member administration did not change: " + safeMessage(e));
                    });
                }
            });
        });
    }

    private void renderMembers(
            AuthSessionState current,
            List<SupabaseAuthClient.AdminMember> members) {
        membersContainer.removeAllViews();
        if (members.isEmpty()) {
            membersContainer.addView(simpleText("No members returned."));
            return;
        }

        for (SupabaseAuthClient.AdminMember member : members) {
            LinearLayout card = card();
            String email = member.email().isEmpty() ? member.userId() : member.email();
            TextView info = simpleText(
                    email
                            + "\n"
                            + member.role()
                            + " · "
                            + member.status()
                            + (current.membershipId().equals(member.membershipId())
                                    ? " · This device"
                                    : ""));
            info.setTextSize(16f);
            card.addView(info);

            LinearLayout actions = actionRow();

            Button roleButton = actionButton(
                    "OWNER".equals(member.role()) ? "Make Member" : "Make Owner");
            roleButton.setOnClickListener(v -> changeMemberRole(
                    current,
                    member,
                    "OWNER".equals(member.role()) ? "MEMBER" : "OWNER"));
            actions.addView(roleButton);

            if ("REVOKED".equals(member.status())) {
                Button reactivate = actionButton("Reactivate");
                reactivate.setOnClickListener(v -> reactivateMember(member));
                actions.addView(reactivate);
            } else {
                Button revoke = actionButton("Revoke");
                revoke.setOnClickListener(v -> revokeMember(current, member));
                actions.addView(revoke);
            }

            card.addView(actions);
            membersContainer.addView(card);
        }
    }

    private void renderInvitations(
            AuthSessionState current,
            List<SupabaseAuthClient.AdminInvitation> invitations) {
        invitationsContainer.removeAllViews();
        if (invitations.isEmpty()) {
            invitationsContainer.addView(simpleText("No invitations."));
            return;
        }

        for (SupabaseAuthClient.AdminInvitation invitation : invitations) {
            LinearLayout card = card();
            TextView info = simpleText(
                    invitation.email()
                            + "\n"
                            + invitation.intendedRole()
                            + " · "
                            + invitation.status()
                            + " · delivery "
                            + invitation.deliveryStatus()
                            + (invitation.deliveryAttemptCount() > 0
                                    ? " (" + invitation.deliveryAttemptCount() + ")"
                                    : ""));
            info.setTextSize(16f);
            card.addView(info);

            if ("PENDING".equals(invitation.status())) {
                LinearLayout actions = actionRow();

                Button resend = actionButton("Resend");
                resend.setOnClickListener(v -> resendInvitation(invitation));
                actions.addView(resend);

                Button role = actionButton(
                        "OWNER".equals(invitation.intendedRole())
                                ? "Change to Member"
                                : "Change to Owner");
                role.setOnClickListener(v -> changeInvitationRole(
                        invitation,
                        "OWNER".equals(invitation.intendedRole()) ? "MEMBER" : "OWNER"));
                actions.addView(role);

                Button cancel = actionButton("Cancel");
                cancel.setOnClickListener(v -> cancelInvitation(invitation));
                actions.addView(cancel);

                card.addView(actions);
            }

            invitationsContainer.addView(card);
        }
    }

    private ProtectedWorkGuard.Result inspectProtectedWork() throws IOException {
        PendingPhotoStore store = new PendingPhotoStore(new File(getFilesDir(), "pending_photos"));
        PhotoPreparer preparer = new PhotoPreparer(new File(getFilesDir(), "prepared_photos"));
        return new ProtectedWorkGuard(store, preparer).inspect();
    }

    private void setAdminAllowed(boolean allowed) {
        adminAllowed = allowed;
        if (!busy) {
            inviteEmail.setEnabled(allowed);
            inviteRole.setEnabled(allowed);
            inviteButton.setEnabled(allowed);
        }
        refreshButton.setEnabled(!busy);
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        inviteEmail.setEnabled(!value && adminAllowed);
        inviteRole.setEnabled(!value && adminAllowed);
        inviteButton.setEnabled(!value && adminAllowed);
        refreshButton.setEnabled(!value);
        closeButton.setEnabled(!value);
        if (message != null) {
            showStatus(message);
        }
    }

    private void showStatus(String message) {
        statusText.setText(message == null ? "" : message);
        statusText.setVisibility(
                message == null || message.trim().isEmpty() ? View.GONE : View.VISIBLE);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackgroundResource(R.drawable.bg_concept_card);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);
        return card;
    }

    private LinearLayout actionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);
        return row;
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        params.setMargins(dp(2), 0, dp(2), 0);
        button.setLayoutParams(params);
        return button;
    }

    private TextView simpleText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(15f);
        return text;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String messageForOutcome(String outcome) {
        if ("LAST_OWNER_BLOCKED".equals(outcome)) {
            return "At least one ACTIVE Owner must remain.";
        }
        switch (outcome) {
            case "SENT":
                return "Invitation sent.";
            case "RESENT":
                return "Invitation resent.";
            case "UPDATED":
                return "Change saved.";
            case "UNCHANGED":
                return "No change was needed.";
            case "CANCELLED":
                return "Invitation cancelled.";
            case "REVOKED":
                return "Membership revoked.";
            case "ACTIVE":
                return "Membership active.";
            default:
                return outcome == null || outcome.trim().isEmpty()
                        ? "Server did not confirm the change."
                        : outcome.replace('_', ' ');
        }
    }

    private static String safeMessage(Throwable error) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "The server did not confirm the operation."
                : message;
    }
}
