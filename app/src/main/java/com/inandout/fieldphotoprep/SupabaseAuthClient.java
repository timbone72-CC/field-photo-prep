package com.inandout.fieldphotoprep;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

final class SupabaseAuthClient {
    static final class AuthException extends IOException {
        private final int statusCode;

        AuthException(String message) {
            this(message, 0, null);
        }

        AuthException(String message, Throwable cause) {
            this(message, 0, cause);
        }

        AuthException(String message, int statusCode) {
            this(message, statusCode, null);
        }

        private AuthException(String message, int statusCode, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }

        int statusCode() {
            return statusCode;
        }

        boolean isRefreshCredentialRejected() {
            return statusCode == 400 || statusCode == 401;
        }

        boolean isUnauthorized() {
            return statusCode == 401;
        }

        boolean isTemporaryServerFailure() {
            return statusCode == 408
                    || statusCode == 429
                    || (statusCode >= 500 && statusCode <= 599);
        }
    }

    static final class AuthTokens {
        private final String accessToken;
        private final String refreshToken;
        private final long expiresAtEpochSeconds;
        private final String userId;
        private final String email;

        AuthTokens(
                String accessToken,
                String refreshToken,
                long expiresAtEpochSeconds,
                String userId,
                String email) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAtEpochSeconds = expiresAtEpochSeconds;
            this.userId = userId;
            this.email = email == null ? "" : email;
        }

        String accessToken() { return accessToken; }
        String refreshToken() { return refreshToken; }
        long expiresAtEpochSeconds() { return expiresAtEpochSeconds; }
        String userId() { return userId; }
        String email() { return email; }
    }

    static final class UserIdentity {
        private final String id;
        private final String email;

        UserIdentity(String id, String email) {
            this.id = id;
            this.email = email == null ? "" : email;
        }

        String id() { return id; }
        String email() { return email; }
    }

    static final class StoredMembershipValidation {
        enum Kind {
            ACTIVE,
            REVOKED,
            NO_MEMBERSHIP,
            IDENTITY_MISMATCH
        }

        private final Kind kind;
        private final AuthSessionState activeState;

        private StoredMembershipValidation(Kind kind, AuthSessionState activeState) {
            this.kind = kind;
            this.activeState = activeState;
        }

        static StoredMembershipValidation active(AuthSessionState state) {
            return new StoredMembershipValidation(Kind.ACTIVE, state);
        }

        static StoredMembershipValidation revoked() {
            return new StoredMembershipValidation(Kind.REVOKED, null);
        }

        static StoredMembershipValidation noMembership() {
            return new StoredMembershipValidation(Kind.NO_MEMBERSHIP, null);
        }

        static StoredMembershipValidation identityMismatch() {
            return new StoredMembershipValidation(Kind.IDENTITY_MISMATCH, null);
        }

        Kind kind() { return kind; }
        AuthSessionState activeState() { return activeState; }
    }

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 20000;

    AuthTokens signInWithPassword(String email, String password) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException e) {
            throw new AuthException("Could not prepare sign-in request.", e);
        }
        JSONObject response = requestObject(
                "POST",
                "/auth/v1/token?grant_type=password",
                body,
                null);
        return parseTokens(response);
    }

    AuthTokens refreshSession(String refreshToken) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("refresh_token", refreshToken);
        } catch (JSONException e) {
            throw new AuthException("Could not prepare session refresh.", e);
        }
        JSONObject response = requestObject(
                "POST",
                "/auth/v1/token?grant_type=refresh_token",
                body,
                null);
        return parseTokens(response);
    }

    void signOut(String accessToken) throws IOException {
        requestObject("POST", "/auth/v1/logout", null, accessToken);
    }

    void requestPasswordRecovery(String email, String redirectUri) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
        } catch (JSONException e) {
            throw new AuthException("Could not prepare recovery request.", e);
        }
        String path = "/auth/v1/recover?redirect_to="
                + encode(redirectUri);
        requestObject("POST", path, body, null);
    }

    UserIdentity getUser(String accessToken) throws IOException {
        JSONObject response = requestObject("GET", "/auth/v1/user", null, accessToken);
        return parseUser(response);
    }

    UserIdentity updatePassword(String accessToken, String newPassword) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("password", newPassword);
        } catch (JSONException e) {
            throw new AuthException("Could not prepare password update.", e);
        }
        JSONObject response = requestObject("PUT", "/auth/v1/user", body, accessToken);
        return parseUser(response);
    }

    AuthSessionState validateMembership(AuthTokens tokens) throws IOException {
        UserIdentity user;
        if (tokens.userId() == null || tokens.userId().trim().isEmpty()) {
            user = getUser(tokens.accessToken());
        } else {
            user = new UserIdentity(tokens.userId(), tokens.email());
        }

        String membershipPath = "/rest/v1/fpp_memberships"
                + "?select=id,organization_id,role,status"
                + "&user_id=eq." + encode(user.id())
                + "&status=eq.ACTIVE";
        JSONArray memberships = requestArray("GET", membershipPath, null, tokens.accessToken());
        if (memberships.length() == 0) {
            throw new AuthException("This account does not have an active Field Photo Prep membership.");
        }
        if (memberships.length() > 1) {
            throw new AuthException("This account has more than one active organization. Organization selection is not available yet.");
        }

        try {
            JSONObject membership = memberships.getJSONObject(0);
            String membershipId = membership.getString("id");
            String organizationId = membership.getString("organization_id");
            String role = membership.getString("role");
            String membershipStatus = membership.getString("status");

            String organizationPath = "/rest/v1/fpp_organizations"
                    + "?select=id,name,status"
                    + "&id=eq." + encode(organizationId)
                    + "&status=eq.ACTIVE";
            JSONArray organizations = requestArray("GET", organizationPath, null, tokens.accessToken());
            if (organizations.length() != 1) {
                throw new AuthException("The Field Photo Prep organization is unavailable or ambiguous.");
            }
            JSONObject organization = organizations.getJSONObject(0);

            return new AuthSessionState(
                    tokens.accessToken(),
                    tokens.refreshToken(),
                    tokens.expiresAtEpochSeconds(),
                    user.id(),
                    user.email(),
                    organizationId,
                    organization.getString("name"),
                    membershipId,
                    role,
                    membershipStatus,
                    Instant.now().getEpochSecond());
        } catch (JSONException e) {
            throw new AuthException("Field Photo Prep membership data was incomplete.", e);
        }
    }

    StoredMembershipValidation validateStoredMembership(
            AuthTokens tokens,
            AuthSessionState stored,
            long validatedAtEpochSeconds) throws IOException {
        if (tokens == null || stored == null) {
            throw new AuthException("Stored membership validation requires a session and identity snapshot.");
        }
        if (validatedAtEpochSeconds <= 0L) {
            throw new AuthException("Stored membership validation requires a trustworthy validation time.");
        }

        UserIdentity user = getUser(tokens.accessToken());
        if (!stored.userId().equals(user.id())) {
            return StoredMembershipValidation.identityMismatch();
        }

        String membershipPath = "/rest/v1/fpp_memberships"
                + "?select=id,organization_id,role,status"
                + "&user_id=eq." + encode(user.id());
        JSONArray memberships = requestArray("GET", membershipPath, null, tokens.accessToken());

        try {
            JSONObject exactMembership = null;
            int activeMembershipCount = 0;
            for (int index = 0; index < memberships.length(); index++) {
                JSONObject candidate = memberships.getJSONObject(index);
                if ("ACTIVE".equals(candidate.optString("status", ""))) {
                    activeMembershipCount++;
                }
                if (stored.membershipId().equals(candidate.optString("id", ""))) {
                    exactMembership = candidate;
                }
            }

            if (exactMembership == null) {
                return StoredMembershipValidation.noMembership();
            }

            String membershipId = exactMembership.getString("id");
            String organizationId = exactMembership.getString("organization_id");
            String role = exactMembership.getString("role");
            String status = exactMembership.getString("status");

            if (!stored.membershipId().equals(membershipId)
                    || !stored.organizationId().equals(organizationId)) {
                return StoredMembershipValidation.identityMismatch();
            }
            if ("REVOKED".equals(status)) {
                return StoredMembershipValidation.revoked();
            }
            if (!"ACTIVE".equals(status)
                    || (!"OWNER".equals(role) && !"MEMBER".equals(role))
                    || activeMembershipCount != 1) {
                return StoredMembershipValidation.noMembership();
            }

            String organizationPath = "/rest/v1/fpp_organizations"
                    + "?select=id,name,status"
                    + "&id=eq." + encode(organizationId);
            JSONArray organizations = requestArray(
                    "GET",
                    organizationPath,
                    null,
                    tokens.accessToken());
            if (organizations.length() != 1) {
                return StoredMembershipValidation.noMembership();
            }

            JSONObject organization = organizations.getJSONObject(0);
            if (!organizationId.equals(organization.getString("id"))
                    || !"ACTIVE".equals(organization.getString("status"))) {
                return StoredMembershipValidation.noMembership();
            }

            return StoredMembershipValidation.active(new AuthSessionState(
                    tokens.accessToken(),
                    tokens.refreshToken(),
                    tokens.expiresAtEpochSeconds(),
                    user.id(),
                    user.email(),
                    organizationId,
                    organization.getString("name"),
                    membershipId,
                    role,
                    status,
                    validatedAtEpochSeconds));
        } catch (JSONException e) {
            throw new AuthException("Field Photo Prep membership data was incomplete.", e);
        }
    }

    AuthTokens tokensFromRedirect(
            String accessToken,
            String refreshToken,
            long expiresAtEpochSeconds) throws IOException {
        if (accessToken == null || accessToken.trim().isEmpty()
                || refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new AuthException("The authentication link did not contain a complete session.");
        }
        UserIdentity user = getUser(accessToken);
        long expiry = expiresAtEpochSeconds;
        if (expiry <= 0) {
            expiry = Instant.now().plusSeconds(3600).getEpochSecond();
        }
        return new AuthTokens(accessToken, refreshToken, expiry, user.id(), user.email());
    }

    private AuthTokens parseTokens(JSONObject response) throws AuthException {
        try {
            String accessToken = response.getString("access_token");
            String refreshToken = response.getString("refresh_token");
            long expiresAt = response.optLong("expires_at", 0L);
            if (expiresAt <= 0) {
                long expiresIn = response.optLong("expires_in", 3600L);
                expiresAt = Instant.now().plusSeconds(Math.max(60L, expiresIn)).getEpochSecond();
            }
            JSONObject userJson = response.optJSONObject("user");
            String userId = userJson == null ? null : userJson.optString("id", null);
            String email = userJson == null ? "" : userJson.optString("email", "");
            return new AuthTokens(accessToken, refreshToken, expiresAt, userId, email);
        } catch (JSONException e) {
            throw new AuthException("Supabase returned an incomplete authentication session.", e);
        }
    }

    private UserIdentity parseUser(JSONObject response) throws AuthException {
        String id = response.optString("id", "");
        if (id.trim().isEmpty()) {
            throw new AuthException("Supabase returned an invalid user identity.");
        }
        return new UserIdentity(id, response.optString("email", ""));
    }

    private JSONObject requestObject(
            String method,
            String path,
            JSONObject body,
            String accessToken) throws IOException {
        String response = request(method, path, body, accessToken);
        if (response == null || response.trim().isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new AuthException("Supabase returned an invalid response.", e);
        }
    }

    private JSONArray requestArray(
            String method,
            String path,
            JSONObject body,
            String accessToken) throws IOException {
        String response = request(method, path, body, accessToken);
        try {
            return new JSONArray(response == null ? "[]" : response);
        } catch (JSONException e) {
            throw new AuthException("Supabase returned invalid membership data.", e);
        }
    }

    private String request(
            String method,
            String path,
            JSONObject body,
            String accessToken) throws IOException {
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(AuthConfig.supabaseUrl() + path);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("apikey", AuthConfig.publishableKey());
            connection.setRequestProperty("Accept", "application/json");
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            }
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }
            }

            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String response = readBody(stream);
            if (code < 200 || code >= 300) {
                throw new AuthException(safeErrorMessage(code, response), code);
            }
            return response;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            return out.toString();
        }
    }

    private static String safeErrorMessage(int code, String response) {
        String message = null;
        if (response != null && !response.trim().isEmpty()) {
            try {
                JSONObject json = new JSONObject(response);
                message = firstNonBlank(
                        json.optString("msg", null),
                        json.optString("message", null),
                        json.optString("error_description", null),
                        json.optString("error", null));
            } catch (JSONException ignored) {
                // Do not expose an arbitrary raw server response.
            }
        }
        if (message == null || message.trim().isEmpty()) {
            message = "Authentication request failed (" + code + ").";
        }
        return message;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static String encode(String value) throws AuthException {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new AuthException("UTF-8 is unavailable.", e);
        }
    }
}
