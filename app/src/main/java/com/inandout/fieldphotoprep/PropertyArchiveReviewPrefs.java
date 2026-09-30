package com.inandout.fieldphotoprep;

import android.content.Context;
import android.content.SharedPreferences;

final class PropertyArchiveReviewPrefs {
    static final String PREFS_NAME = "property_archive_review";
    private static final String KEY_THRESHOLD = "suggest_archive_after";

    enum Threshold {
        DAYS_30("30 days"),
        DAYS_60("60 days"),
        DAYS_90("90 days"),
        MONTHS_6("6 months"),
        YEAR_1("1 year"),
        NEVER("Never");

        private final String label;

        Threshold(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        static Threshold fromStored(String value) {
            if (value == null || value.isBlank()) {
                return DAYS_90;
            }
            try {
                return Threshold.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                return DAYS_90;
            }
        }
    }

    private final SharedPreferences prefs;

    PropertyArchiveReviewPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    Threshold threshold() {
        return Threshold.fromStored(prefs.getString(KEY_THRESHOLD, Threshold.DAYS_90.name()));
    }

    void setThreshold(Threshold threshold) {
        if (threshold == null) {
            throw new IllegalArgumentException("Archive review threshold is required.");
        }
        if (!prefs.edit().putString(KEY_THRESHOLD, threshold.name()).commit()) {
            throw new IllegalStateException("Could not persist archive review threshold.");
        }
    }

    static CharSequence[] labels() {
        Threshold[] thresholds = Threshold.values();
        CharSequence[] labels = new CharSequence[thresholds.length];
        for (int i = 0; i < thresholds.length; i++) {
            labels[i] = thresholds[i].label();
        }
        return labels;
    }

    static int indexOf(Threshold threshold) {
        Threshold target = threshold == null ? Threshold.DAYS_90 : threshold;
        Threshold[] thresholds = Threshold.values();
        for (int i = 0; i < thresholds.length; i++) {
            if (thresholds[i] == target) {
                return i;
            }
        }
        return 2;
    }
}
