package com.inandout.fieldphotoprep;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureAuthStore {
    static final String PREFS_NAME = "fpp_auth_secure_v1";
    private static final String VALUE_KEY = "encrypted_state";
    private static final String KEY_ALIAS = "field_photo_prep_auth_v1";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String CIPHER = "AES/GCM/NoPadding";

    private final SharedPreferences prefs;

    SecureAuthStore(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    synchronized void save(AuthSessionState state) throws GeneralSecurityException {
        try {
            byte[] plaintext = toJson(state).toString().getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] ciphertext = cipher.doFinal(plaintext);
            JSONObject envelope = new JSONObject()
                    .put("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP));
            if (!prefs.edit().putString(VALUE_KEY, envelope.toString()).commit()) {
                throw new GeneralSecurityException("Could not persist encrypted auth state.");
            }
        } catch (JSONException e) {
            throw new GeneralSecurityException("Could not encode auth state.", e);
        }
    }

    synchronized AuthSessionState load() {
        String stored = prefs.getString(VALUE_KEY, null);
        if (stored == null || stored.isBlank()) {
            return null;
        }
        try {
            JSONObject envelope = new JSONObject(stored);
            byte[] iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP);
            byte[] ciphertext = Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return fromJson(new JSONObject(new String(plaintext, StandardCharsets.UTF_8)));
        } catch (Exception e) {
            clear();
            return null;
        }
    }

    synchronized void clear() {
        prefs.edit().remove(VALUE_KEY).commit();
    }

    boolean hasStoredSession() {
        return prefs.contains(VALUE_KEY);
    }

    private SecretKey getOrCreateKey() throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
            if (entry instanceof KeyStore.SecretKeyEntry secretKeyEntry) {
                return secretKeyEntry.getSecretKey();
            }

            KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE);
            generator.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build());
            return generator.generateKey();
        } catch (GeneralSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralSecurityException("Could not access Android Keystore.", e);
        }
    }

    private static JSONObject toJson(AuthSessionState state) throws JSONException {
        return new JSONObject()
                .put("accessToken", state.accessToken())
                .put("refreshToken", state.refreshToken())
                .put("expiresAt", state.expiresAtEpochSeconds())
                .put("userId", state.userId())
                .put("email", state.email())
                .put("organizationId", state.organizationId())
                .put("organizationName", state.organizationName())
                .put("membershipId", state.membershipId())
                .put("role", state.role())
                .put("membershipStatus", state.membershipStatus())
                .put("lastValidatedAt", state.lastMembershipValidatedAtEpochSeconds());
    }

    private static AuthSessionState fromJson(JSONObject json) throws JSONException {
        return new AuthSessionState(
                json.getString("accessToken"),
                json.getString("refreshToken"),
                json.getLong("expiresAt"),
                json.getString("userId"),
                json.optString("email", ""),
                json.getString("organizationId"),
                json.getString("organizationName"),
                json.getString("membershipId"),
                json.getString("role"),
                json.getString("membershipStatus"),
                json.getLong("lastValidatedAt"));
    }
}
