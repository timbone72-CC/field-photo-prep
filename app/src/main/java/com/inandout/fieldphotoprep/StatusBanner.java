package com.inandout.fieldphotoprep;

import android.widget.TextView;

import androidx.core.content.ContextCompat;

final class StatusBanner {
    enum Tone {
        INFO,
        SUCCESS,
        ERROR
    }

    private StatusBanner() {
    }

    static void apply(TextView view, Tone tone) {
        if (view == null) {
            return;
        }
        switch (tone) {
            case SUCCESS:
                view.setBackgroundResource(R.drawable.bg_concept_success_status);
                view.setTextColor(ContextCompat.getColor(
                        view.getContext(), R.color.home_primary_dark));
                break;
            case ERROR:
                view.setBackgroundResource(R.drawable.bg_concept_status);
                view.setTextColor(ContextCompat.getColor(
                        view.getContext(), R.color.home_error));
                break;
            case INFO:
            default:
                view.setBackgroundResource(R.drawable.bg_concept_info_status);
                view.setTextColor(ContextCompat.getColor(
                        view.getContext(), R.color.home_blue_dark));
                break;
        }
    }
}
