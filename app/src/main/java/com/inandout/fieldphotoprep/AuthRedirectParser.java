package com.inandout.fieldphotoprep;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class AuthRedirectParser {
    enum Kind {
        RECOVERY,
        INVITE,
        ERROR,
        UNSUPPORTED
    }

    static final class Result {
        private final Kind kind;
        private final String accessToken;
        private final String refreshToken;
        private final long expiresAtEpochSeconds;
        private final String errorMessage;

        Result(
                Kind kind,
                String accessToken,
                String refreshToken,
                long expiresAtEpochSeconds,
                String errorMessage) {
            this.kind = kind;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAtEpochSeconds = expiresAtEpochSeconds;
            this.errorMessage = errorMessage;
        }

        Kind kind() { return kind; }
        String accessToken() { return accessToken; }
        String refreshToken() { return refreshToken; }
        long expiresAtEpochSeconds() { return expiresAtEpochSeconds; }
        String errorMessage() { return errorMessage; }

        boolean hasSessionTokens() {
            return accessToken != null && !accessToken.isBlank()
                    && refreshToken != null && !refreshToken.isBlank();
        }
    }

    private AuthRedirectParser() {}

    static Result parse(String rawUri, String expectedScheme, String expectedHost) {
        if (rawUri == null || rawUri.isBlank()) {
            return unsupported("Missing callback URL.");
        }
        try {
            URI uri = URI.create(rawUri);
            if (!expectedScheme.equals(uri.getScheme()) || !expectedHost.equals(uri.getHost())) {
                return unsupported("This sign-in link is not for this Field Photo Prep build.");
            }

            Map<String, String> values = new LinkedHashMap<>();
            addPairs(values, uri.getRawQuery());
            addPairs(values, uri.getRawFragment());

            String error = first(values, "error_description", "error");
            if (error != null && !error.isBlank()) {
                return new Result(Kind.ERROR, null, null, 0L, error);
            }

            String type = values.get("type");
            Kind kind;
            if ("recovery".equals(type)) {
                kind = Kind.RECOVERY;
            } else if ("invite".equals(type)) {
                kind = Kind.INVITE;
            } else {
                return unsupported("Unsupported authentication link.");
            }

            long expiresAt = parseLong(values.get("expires_at"));
            if (expiresAt <= 0) {
                long expiresIn = parseLong(values.get("expires_in"));
                if (expiresIn > 0) {
                    expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn;
                }
            }

            return new Result(
                    kind,
                    values.get("access_token"),
                    values.get("refresh_token"),
                    expiresAt,
                    null);
        } catch (RuntimeException e) {
            return unsupported("The authentication link is malformed.");
        }
    }

    private static Result unsupported(String message) {
        return new Result(Kind.UNSUPPORTED, null, null, 0L, message);
    }

    private static String first(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static void addPairs(Map<String, String> out, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int index = pair.indexOf('=');
            String rawKey = index >= 0 ? pair.substring(0, index) : pair;
            String rawValue = index >= 0 ? pair.substring(index + 1) : "";
            out.put(decode(rawKey), decode(rawValue));
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
