package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AuthRedirectParserTest {
    private static final String SCHEME = "com.inandout.fieldphotoprep.internal";
    private static final String HOST = "auth-callback";

    @Test
    public void recoveryCallbackAcceptsExactBuildUriAndTokens() {
        AuthRedirectParser.Result result = AuthRedirectParser.parse(
                SCHEME + "://" + HOST
                        + "#access_token=access123"
                        + "&refresh_token=refresh456"
                        + "&expires_at=2000000000"
                        + "&type=recovery",
                SCHEME,
                HOST);

        assertEquals(AuthRedirectParser.Kind.RECOVERY, result.kind());
        assertEquals("access123", result.accessToken());
        assertEquals("refresh456", result.refreshToken());
        assertEquals(2000000000L, result.expiresAtEpochSeconds());
        assertTrue(result.hasSessionTokens());
    }

    @Test
    public void inviteCallbackCarriesExactInvitationIdAcrossQueryAndFragment() {
        AuthRedirectParser.Result result = AuthRedirectParser.parse(
                SCHEME + "://" + HOST
                        + "?fpp_invitation_id=11111111-2222-4333-8444-555555555555"
                        + "#access_token=access123"
                        + "&refresh_token=refresh456"
                        + "&expires_at=2000000000"
                        + "&type=invite",
                SCHEME,
                HOST);

        assertEquals(AuthRedirectParser.Kind.INVITE, result.kind());
        assertEquals("11111111-2222-4333-8444-555555555555", result.invitationId());
        assertTrue(result.hasSessionTokens());
    }

    @Test
    public void inviteCallbackWithoutSessionTokensRemainsIncomplete() {
        AuthRedirectParser.Result result = AuthRedirectParser.parse(
                SCHEME + "://" + HOST
                        + "?fpp_invitation_id=11111111-2222-4333-8444-555555555555"
                        + "&type=invite",
                SCHEME,
                HOST);

        assertEquals(AuthRedirectParser.Kind.INVITE, result.kind());
        assertEquals("11111111-2222-4333-8444-555555555555", result.invitationId());
        assertFalse(result.hasSessionTokens());
    }

    @Test
    public void wrongBuildSchemeFailsClosed() {
        AuthRedirectParser.Result result = AuthRedirectParser.parse(
                "com.inandout.fieldphotoprep://" + HOST
                        + "#access_token=a&refresh_token=b&type=recovery",
                SCHEME,
                HOST);

        assertEquals(AuthRedirectParser.Kind.UNSUPPORTED, result.kind());
        assertFalse(result.hasSessionTokens());
    }

    @Test
    public void wrongHostFailsClosed() {
        AuthRedirectParser.Result result = AuthRedirectParser.parse(
                SCHEME + "://other#access_token=a&refresh_token=b&type=recovery",
                SCHEME,
                HOST);

        assertEquals(AuthRedirectParser.Kind.UNSUPPORTED, result.kind());
    }

    @Test
    public void callbackErrorIsSurfacedWithoutTreatingItAsSession() {
        AuthRedirectParser.Result result = AuthRedirectParser.parse(
                SCHEME + "://" + HOST
                        + "?error=access_denied"
                        + "&error_description=Email+link+expired",
                SCHEME,
                HOST);

        assertEquals(AuthRedirectParser.Kind.ERROR, result.kind());
        assertEquals("Email link expired", result.errorMessage());
        assertFalse(result.hasSessionTokens());
    }

    @Test
    public void malformedUriFailsClosed() {
        AuthRedirectParser.Result result = AuthRedirectParser.parse(
                "%%%not-a-uri",
                SCHEME,
                HOST);

        assertEquals(AuthRedirectParser.Kind.UNSUPPORTED, result.kind());
    }
}
