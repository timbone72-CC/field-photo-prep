package com.inandout.fieldphotoprep;

final class AuthConfig {
    private AuthConfig() {}

    static String supabaseUrl() {
        return BuildConfig.SUPABASE_URL;
    }

    static String publishableKey() {
        return BuildConfig.SUPABASE_PUBLISHABLE_KEY;
    }

    static String redirectScheme() {
        return BuildConfig.AUTH_REDIRECT_SCHEME;
    }

    static String redirectHost() {
        return BuildConfig.AUTH_REDIRECT_HOST;
    }

    static String redirectUri() {
        return redirectScheme() + "://" + redirectHost();
    }
}
