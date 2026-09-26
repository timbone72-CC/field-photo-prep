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
    private interface OwnerOperation {
        void run(AuthSessionState state) throws Exception;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authorizationManager =
                ((FieldPhotoPrepApplication) getApplication()).authorizationManager();
        setContentView(R.layout.screen_member_admin);

        organizationText = findViewById(R.id.member_admin_organization);
        statusText = findViewById(R.id.member_admin_status);
        progress = findViewById(R.id.member_admin_progress);
        inviteEmail = findViewById(R.id.member_admin_invite_email);
        inviteRole = findViewById(R.id.member_admin_invite_role);
        inviteButton = findViewById(R.id.member_admin_invite_button);
        refreshButton = findViewById(R.id.member_admin_refresh_button);
        closeButton = findViewById(R.id.member_admin_close);
        membersContainer = findViewById(R.id.member_admin_members);
        invitationsContainer = findViewById(R.id.member_admin_invitations);

        inviteRole.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"MEMBER", "OWNER"}));

        inviteButton.setOnClickListener(v -> sendInvitation());
        refreshButton.setOnClickListener(v -> refreshAdminData());
        closeButton.setOnClickListener(v -> finish());

        AuthSessionState stored = authorizationManager.storedSession();
        organizationText.setText(stored == null ? "" : stored.organizationName());
        renderEmpty("Checking Owner access…");
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAdminData();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void refreshAdminData() {
        runAsFreshOwner("Refreshing members…", state -> {
            List<SupabaseAuthClient.AdminMember> members =
                    authClient.listAdminMembers(state.accessToken(), state.organizationId());
            List<SupabaseAuthClient.AdminInvitation> invitations =
                    authClient.listAdminInvitations(state.accessToken(), state.organizationId());
            runOnUiThread(() -> renderAdminData(state, members, invitations, null));
        });
    }

    private void sendInvitation() {
        String email = inviteEmail.getText().toString().trim();
        String role = String.valueOf(inviteRole.getSelectedItem());
        if (email.isEmpty()) {
            statusText.setText("Enter the email address to invite.");
            return;
        }

        runAsFreshOwner("Sending invitation…", state -> {
            String outcome = authClient.sendOwnerInvitation(
                    state.accessToken(),
                    state.organizationId(),
                    email,
                    role,
                    AuthConfig.redirectUri());
            List<SupabaseAuthClient.AdminMember> members =
                    authClient.listAdminMembers(state.accessToken(), state.organizationId());
            List<SupabaseAuthClient.AdminInvitation> invitations =
                    authClient.listAdminInvitations(state.accessToken(), state.organizationId());
            runOnUiThread(() -> {
                inviteEmail.setText("");
                renderAdminData(
                        state,
                        members,
                        invitations,
                        "RESENT".equals(outcome)
                                ? "Invitation resent."
                                : "Invitation sent.");
            });
        });
    }

    private void changeMemberRole(SupabaseAuthClient.AdminMember member) {
        String newRole = "OWNER".equals(member.role()) ? "MEMBER" : "OWNER";
        runAsFreshOwner("Updating member role…", state -> {
            authClient.changeMembershipRole(
                    state.accessToken(),
                    state.organizationId(),
                    member.membershipId(),
                    newRole);
            handlePossibleCurrentMembershipChange(state, member.membershipId());
        });
    }

    private void revokeMember(SupabaseAuthClient.AdminMember member) {
        runAsFreshOwner("Revoking member…", state -> {
            if (state.membershipId().equals(member.membershipId())) {
                ProtectedWorkGuard.Result protectedWork = inspectProtectedWork();
                if (protectedWork.blocksSignOut()) {
                    throw new IOException(
                            "Your own membership cannot be revoked while "
                                    + protectedWork.blockingCount()
                                    + " protected photo"
                                    + (protectedWork.blockingCount() == 1 ? "" : "s")
                                    + " still need safe resolution.");
                }
            }

            authClient.revokeMembership(
                    state.accessToken(),
                    state.organizationId(),
                    member.membershipId());
            handlePossibleCurrentMembershipChange(state, member.membershipId());
        });
    }

    private void reactivateMember(SupabaseAuthClient.AdminMember member) {
        runAsFreshOwner("Reactivating member…", state -> {
            authClient.reactivateMembership(
                    state.accessToken(),
                    state.organizationId(),
                    member.membershipId());
            reloadAfterMutation(state, "Member reactivated.");
        });
    }

    private void resendInvitation(SupabaseAuthClient.AdminInvitation invitation) {
        runAsFreshOwner("Resending invitation…", state -> {
            String outcome = authClient.sendOwnerInvitation(
                    state.accessToken(),
                    state.organizationId(),
                    invitation.email(),
                    invitation.intendedRole(),
                    AuthConfig.redirectUri());
            reloadAfterMutation(
                    state,
                    "RESENT".equals(outcome)
                            ? "Invitation resent."
                            : "Invitation sent.");
        });
    }

    private void changeInvitationRole(SupabaseAuthClient.AdminInvitation invitation) {
        String newRole = "OWNER".equals(invitation.intendedRole()) ? "MEMBER" : "OWNER";
        runAsFreshOwner("Updating invitation role…", state -> {
            authClient.updateInvitationRole(
                    state.accessToken(),
                    state.organizationId(),
                    invitation.invitationId(),
                    newRole);
            reloadAfterMutation(state, "Invitation role updated.");
        });
    }

    private void cancelInvitation(SupabaseAuthClient.AdminInvitation invitation) {
        runAsFreshOwner("Cancelling invitation…", state -> {
            authClient.cancelInvitation(
                    state.accessToken(),
                    state.organizationId(),
                    invitation.invitationId());
            reloadAfterMutation(state, "Invitation cancelled.");
        });
    }

    private void handlePossibleCurrentMembershipChange(
            AuthSessionState prior,
            String changedMembershipId) throws IOException {
        if (!prior.membershipId().equals(changedMembershipId)) {
            reloadAfterMutation(prior, "Member updated.");
            return;
        }

        authorizationManager.revalidateAsync().whenComplete((decision, error) ->
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (error != null
                            || decision == null
                            || !decision.allowsMemberAdministration()) {
                        setBusy(false);
                        statusText.setText(
                                "Your membership changed. Owner administration is no longer available.");
                        inviteButton.setEnabled(false);
                        refreshButton.setEnabled(false);
                        return;
                    }
                    refreshAdminData();
                }));
    }

    private void reloadAfterMutation(AuthSessionState state, String message) throws IOException {
        List<SupabaseAuthClient.AdminMember> members =
                authClient.listAdminMembers(state.accessToken(), state.organizationId());
        List<SupabaseAuthClient.AdminInvitation> invitations =
                authClient.listAdminInvitations(state.accessToken(), state.organizationId());
        runOnUiThread(() -> renderAdminData(state, members, invitations, message));
    }

    private void runAsFreshOwner(String message, OwnerOperation operation) {
        setBusy(true);
        statusText.setText(message);

        authorizationManager.revalidateAsync().whenComplete((decision, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (error != null || decision == null || !decision.allowsMemberAdministration()) {
                runOnUiThread(() -> {
                    setBusy(false);
                    statusText.setText(
                            "A current online ACTIVE Owner validation is required to manage members.");
                    inviteButton.setEnabled(false);
                    refreshButton.setEnabled(true);
                });
                return;
            }

            AuthSessionState state = authorizationManager.storedSession();
            if (state == null
                    || !"OWNER".equals(state.role())
                    || !"ACTIVE".equals(state.membershipStatus())) {
                runOnUiThread(() -> {
                    setBusy(false);
                    statusText.setText("Owner session is unavailable. Recheck the account.");
                });
                return;
            }

            executor.execute(() -> {
                try {
                    operation.run(state);
                } catch (Exception operationError) {
                    runOnUiThread(() -> {
                        setBusy(false);
                        String detail = operationError.getMessage();
                        statusText.setText(
                                detail == null || detail.isBlank()
                                        ? "Member administration request failed."
                                        : detail);
                    });
                }
            });
        });
    }

    private void renderAdminData(
            AuthSessionState state,
            List<SupabaseAuthClient.AdminMember> members,
            List<SupabaseAuthClient.AdminInvitation> invitations,
            String message) {
        organizationText.setText(state.organizationName());
        statusText.setText(message == null
                ? members.size() + " member"
                        + (members.size() == 1 ? "" : "s")
                        + " · "
                        + invitations.size()
                        + " invitation"
                        + (invitations.size() == 1 ? "" : "s")
                : message);

        membersContainer.removeAllViews();
        for (SupabaseAuthClient.AdminMember member : members) {
            membersContainer.addView(memberRow(member));
        }

        invitationsContainer.removeAllViews();
        for (SupabaseAuthClient.AdminInvitation invitation : invitations) {
            invitationsContainer.addView(invitationRow(invitation));
        }

        setBusy(false);
    }

    private View memberRow(SupabaseAuthClient.AdminMember member) {
        LinearLayout row = card();
        TextView summary = text(
                member.email()
                        + "\n"
                        + member.role()
                        + " · "
                        + member.status());
        row.addView(summary);

        LinearLayout actions = actions();
        if ("ACTIVE".equals(member.status())) {
            Button roleButton = button(
                    "OWNER".equals(member.role()) ? "Make Member" : "Make Owner");
            roleButton.setOnClickListener(v -> changeMemberRole(member));
            actions.addView(roleButton);

            Button revoke = button("Revoke");
            revoke.setOnClickListener(v -> revokeMember(member));
            actions.addView(revoke);
        } else if ("REVOKED".equals(member.status())) {
            Button reactivate = button("Reactivate");
            reactivate.setOnClickListener(v -> reactivateMember(member));
            actions.addView(reactivate);
        }
        if (actions.getChildCount() > 0) {
            row.addView(actions);
        }
        return row;
    }

    private View invitationRow(SupabaseAuthClient.AdminInvitation invitation) {
        LinearLayout row = card();
        TextView summary = text(
                invitation.email()
                        + "\n"
                        + invitation.intendedRole()
                        + " · "
                        + invitation.status()
                        + " · delivery "
                        + invitation.deliveryStatus());
        row.addView(summary);

        if ("PENDING".equals(invitation.status())) {
            LinearLayout actions = actions();

            Button roleButton = button(
                    "OWNER".equals(invitation.intendedRole())
                            ? "Change to Member"
                            : "Change to Owner");
            roleButton.setOnClickListener(v -> changeInvitationRole(invitation));
            actions.addView(roleButton);

            Button resend = button("Resend");
            resend.setOnClickListener(v -> resendInvitation(invitation));
            actions.addView(resend);

            Button cancel = button("Cancel");
            cancel.setOnClickListener(v -> cancelInvitation(invitation));
            actions.addView(cancel);

            row.addView(actions);
        }
        return row;
    }

    private LinearLayout card() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(params);
        row.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        return row;
    }

    private LinearLayout actions() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, 0);
        return row;
    }

    private TextView text(String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(15);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        button.setLayoutParams(params);
        return button;
    }

    private void renderEmpty(String message) {
        membersContainer.removeAllViews();
        invitationsContainer.removeAllViews();
        statusText.setText(message);
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        inviteEmail.setEnabled(!busy);
        inviteRole.setEnabled(!busy);
        inviteButton.setEnabled(!busy);
        refreshButton.setEnabled(!busy);
        closeButton.setEnabled(!busy);
    }

    private ProtectedWorkGuard.Result inspectProtectedWork() throws IOException {
        PendingPhotoStore store =
                new PendingPhotoStore(new File(getFilesDir(), "pending_photos"));
        PhotoPreparer preparer =
                new PhotoPreparer(new File(getFilesDir(), "prepared_photos"));
        return new ProtectedWorkGuard(store, preparer).inspect();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
