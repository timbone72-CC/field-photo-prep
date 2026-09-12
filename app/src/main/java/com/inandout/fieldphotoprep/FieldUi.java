package com.inandout.fieldphotoprep;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

final class FieldUi {
    private FieldUi() {}

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    static TextView screenTitle(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(28);
        view.setTextColor(color(context, R.color.fpp_on_background));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, dp(context, 4), 0, dp(context, 2));
        return view;
    }

    static TextView screenSubtitle(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(color(context, R.color.fpp_on_surface_variant));
        view.setPadding(0, 0, 0, dp(context, 8));
        return view;
    }

    static TextView sectionTitle(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(15);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setTextColor(color(context, R.color.fpp_on_surface));
        view.setPadding(dp(context, 2), dp(context, 16), 0, dp(context, 8));
        return view;
    }

    static TextView body(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(15);
        view.setTextColor(color(context, R.color.fpp_on_surface));
        return view;
    }

    static TextView muted(Context context, String text) {
        TextView view = body(context, text);
        view.setTextSize(13);
        view.setTextColor(color(context, R.color.fpp_on_surface_variant));
        return view;
    }

    static TextView statusText(Context context) {
        TextView view = new TextView(context);
        view.setTextSize(14);
        view.setTextColor(color(context, R.color.fpp_on_surface_variant));
        view.setPadding(dp(context, 2), dp(context, 8), dp(context, 2), dp(context, 8));
        return view;
    }

    static MaterialCardView card(Context context) {
        MaterialCardView card = new MaterialCardView(context);
        card.setRadius(dp(context, 18));
        card.setCardElevation(dp(context, 1));
        card.setStrokeWidth(dp(context, 1));
        card.setStrokeColor(color(context, R.color.fpp_outline));
        card.setCardBackgroundColor(color(context, R.color.fpp_surface));
        card.setUseCompatPadding(false);
        return card;
    }

    static MaterialCardView paddedCard(Context context, View child) {
        MaterialCardView card = card(context);
        int pad = dp(context, 16);
        card.setContentPadding(pad, pad, pad, pad);
        card.addView(child, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    static LinearLayout.LayoutParams cardParams(Context context) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(context, 10);
        return params;
    }

    static MaterialButton primaryButton(Context context, String text) {
        MaterialButton button = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonStyle);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setCornerRadius(dp(context, 18));
        button.setMinHeight(dp(context, 52));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setBackgroundTintList(ColorStateList.valueOf(color(context, R.color.fpp_primary)));
        button.setTextColor(color(context, R.color.fpp_on_primary));
        return button;
    }

    static MaterialButton secondaryButton(Context context, String text) {
        MaterialButton button = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setCornerRadius(dp(context, 16));
        button.setMinHeight(dp(context, 48));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setStrokeColor(ColorStateList.valueOf(color(context, R.color.fpp_outline)));
        button.setTextColor(color(context, R.color.fpp_primary));
        return button;
    }

    static MaterialButton textButton(Context context, String text) {
        MaterialButton button = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonStyle);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinHeight(dp(context, 44));
        button.setCornerRadius(dp(context, 14));
        button.setBackgroundTintList(ColorStateList.valueOf(
                color(context, android.R.color.transparent)));
        button.setStrokeWidth(0);
        button.setTextColor(color(context, R.color.fpp_primary));
        return button;
    }

    static MaterialButton dangerButton(Context context, String text) {
        MaterialButton button = secondaryButton(context, text);
        int danger = color(context, R.color.fpp_error);
        button.setTextColor(danger);
        button.setStrokeColor(ColorStateList.valueOf(danger));
        return button;
    }

    static TextInputLayout input(Context context, String hint) {
        TextInputLayout layout = new TextInputLayout(context, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layout.setHint(hint);
        layout.setBoxCornerRadii(
                dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        TextInputEditText editText = new TextInputEditText(layout.getContext());
        editText.setSingleLine(true);
        layout.addView(editText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return layout;
    }

    static TextView pill(Context context, String text, int backgroundRes, int textRes) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(12);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setTextColor(color(context, textRes));
        view.setGravity(Gravity.CENTER);
        int h = dp(context, 10);
        int v = dp(context, 6);
        view.setPadding(h, v, h, v);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(color(context, backgroundRes));
        bg.setCornerRadius(dp(context, 999));
        view.setBackground(bg);
        return view;
    }

    static View divider(Context context) {
        View view = new View(context);
        view.setBackgroundColor(color(context, R.color.fpp_outline));
        return view;
    }

    @ColorInt
    static int color(Context context, int resId) {
        return ContextCompat.getColor(context, resId);
    }

    static void applyPageBackground(View view) {
        view.setBackgroundColor(color(view.getContext(), R.color.fpp_background));
    }

    static void setButtonIcon(MaterialButton button, int drawableRes) {
        button.setIconResource(drawableRes);
        button.setIconPadding(dp(button.getContext(), 8));
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setIconTint(ColorStateList.valueOf(button.getCurrentTextColor()));
    }
}
