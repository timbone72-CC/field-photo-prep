package com.inandout.fieldphotoprep;

import android.content.Context;
import android.content.SharedPreferences;

final class PropertyLifecycleStore {
    static final String PREFS_NAME = "property_lifecycle";

    enum State {
        ACTIVE,
        ARCHIVED,
        DELETED
    }

    static final class Snapshot {
        private final State state;
        private final long lastUsedEpochMs;

        Snapshot(State state, long lastUsedEpochMs) {
            this.state = state == null ? State.ACTIVE : state;
            this.lastUsedEpochMs = Math.max(0L, lastUsedEpochMs);
        }

        State state() {
            return state;
        }

        long lastUsedEpochMs() {
            return lastUsedEpochMs;
        }

        boolean hasLastUsed() {
            return lastUsedEpochMs > 0L;
        }
    }

    private final SharedPreferences prefs;

    PropertyLifecycleStore(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    Snapshot snapshot(String companyId, String addressId) {
        String key = identityKey(companyId, addressId);
        String rawState = prefs.getString(key + ".state", State.ACTIVE.name());
        State state;
        try {
            state = State.valueOf(rawState);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            state = State.ACTIVE;
        }
        long lastUsed = Math.max(0L, prefs.getLong(key + ".last_used_ms", 0L));
        return new Snapshot(state, lastUsed);
    }

    void markWorkActivity(String companyId, String addressId) {
        markWorkActivity(companyId, addressId, System.currentTimeMillis());
    }

    void markWorkActivity(String companyId, String addressId, long epochMs) {
        if (epochMs <= 0L) {
            throw new IllegalArgumentException("Work activity time must be positive.");
        }
        String key = identityKey(companyId, addressId);
        long previous = Math.max(0L, prefs.getLong(key + ".last_used_ms", 0L));
        long effective = Math.max(previous, epochMs);
        if (!prefs.edit().putLong(key + ".last_used_ms", effective).commit()) {
            throw new IllegalStateException("Could not persist property work activity.");
        }
    }

    void setState(String companyId, String addressId, State state) {
        if (state == null) {
            throw new IllegalArgumentException("Property lifecycle state is required.");
        }
        String key = identityKey(companyId, addressId);
        if (!prefs.edit().putString(key + ".state", state.name()).commit()) {
            throw new IllegalStateException("Could not persist property lifecycle state.");
        }
    }

    private static String identityKey(String companyId, String addressId) {
        String company = requireIdentity(companyId, "Company");
        String address = requireIdentity(addressId, "Address");
        return "property." + company.length() + ":" + company + address;
    }

    private static String requireIdentity(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " provider identity is required.");
        }
        return value;
    }
}
