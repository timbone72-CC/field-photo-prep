package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Applies the approved Concept 3 presentation to MainActivity without owning any
 * Drive, folder, work-order, identity, or navigation behavior.
 */
final class MainScreenDecorator {
    private MainScreenDecorator() {}

    static void decorate(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }
        ViewGroup contentRoot = (ViewGroup) content;
        if (contentRoot.getChildCount() != 1 || !(contentRoot.getChildAt(0) instanceof LinearLayout)) {
            return;
        }
        LinearLayout root = (LinearLayout) contentRoot.getChildAt(0);
        if (root.getChildCount() < 6) {
            return;
        }

        FieldUi.applyPageBackground(root);
        root.setPadding(FieldUi.dp(activity, 16), FieldUi.dp(activity, 8),
                FieldUi.dp(activity, 16), FieldUi.dp(activity, 10));

        styleHeader(activity, root.getChildAt(0));
        styleStatus(activity, root.getChildAt(1));
        styleAddressHome(activity, root.getChildAt(2));
        styleWorkOrders(activity, root.getChildAt(3));
        styleListHeading(activity, root.getChildAt(4));
        styleFolderList(activity, root.getChildAt(5));
    }

    private static void styleHeader(Activity activity, View view) {
        if (!(view instanceof LinearLayout)) {
            return;
        }
        LinearLayout header = (LinearLayout) view;
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, FieldUi.dp(activity, 2));

        for (int i = 0; i < header.getChildCount(); i++) {
            View child = header.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                button.setCornerRadius(FieldUi.dp(activity, 14));
                button.setMinHeight(FieldUi.dp(activity, 42));
            } else if (child instanceof LinearLayout) {
                LinearLayout titles = (LinearLayout) child;
                if (titles.getChildCount() > 0 && titles.getChildAt(0) instanceof TextView) {
                    TextView title = (TextView) titles.getChildAt(0);
                    title.setTextSize(24);
                    title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    title.setTextColor(FieldUi.color(activity, R.color.fpp_on_background));
                }
                if (titles.getChildCount() > 1 && titles.getChildAt(1) instanceof TextView) {
                    TextView subtitle = (TextView) titles.getChildAt(1);
                    subtitle.setTextSize(13);
                    subtitle.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
                }
            }
        }
    }

    private static void styleStatus(Activity activity, View view) {
        if (!(view instanceof TextView)) {
            return;
        }
        TextView status = (TextView) view;
        status.setTextSize(13);
        status.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
        status.setPadding(FieldUi.dp(activity, 2), FieldUi.dp(activity, 4),
                FieldUi.dp(activity, 2), FieldUi.dp(activity, 8));
    }

    private static void styleAddressHome(Activity activity, View view) {
        if (!(view instanceof LinearLayout)) {
            return;
        }
        LinearLayout controls = (LinearLayout) view;
        if (controls.getChildCount() == 0) {
            return;
        }

        View drive = controls.getChildAt(0);
        if (drive instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) drive;
            card.setRadius(FieldUi.dp(activity, 20));
            card.setStrokeWidth(0);
            card.setCardElevation(0);
            card.setCardBackgroundColor(FieldUi.color(activity, R.color.fpp_primary_container));
            tintCardText(activity, card);
        }

        if (controls.getChildCount() > 1 && controls.getChildAt(1) instanceof MaterialButton) {
            MaterialButton newAddress = (MaterialButton) controls.getChildAt(1);
            newAddress.setCornerRadius(FieldUi.dp(activity, 18));
            newAddress.setMinHeight(FieldUi.dp(activity, 54));
        }
    }

    private static void styleWorkOrders(Activity activity, View view) {
        if (!(view instanceof ScrollView)) {
            return;
        }
        ScrollView scroll = (ScrollView) view;
        if (scroll.getChildCount() != 1 || !(scroll.getChildAt(0) instanceof LinearLayout)) {
            return;
        }
        LinearLayout body = (LinearLayout) scroll.getChildAt(0);
        body.setPadding(0, 0, 0, FieldUi.dp(activity, 4));

        if (body.getChildCount() > 0 && body.getChildAt(0) instanceof TextView) {
            TextView helper = (TextView) body.getChildAt(0);
            helper.setTextSize(13);
            helper.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
        }

        if (body.getChildCount() > 1 && body.getChildAt(1) instanceof MaterialCardView) {
            MaterialCardView selected = (MaterialCardView) body.getChildAt(1);
            selected.setRadius(FieldUi.dp(activity, 20));
            selected.setStrokeWidth(FieldUi.dp(activity, 1));
            selected.setStrokeColor(FieldUi.color(activity, R.color.fpp_primary));
            selected.setCardBackgroundColor(FieldUi.color(activity, R.color.fpp_primary_container));
        }

        for (int i = 2; i < body.getChildCount(); i++) {
            if (body.getChildAt(i) instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) body.getChildAt(i);
                card.setRadius(FieldUi.dp(activity, 18));
                card.setCardElevation(0);
            }
        }
    }

    private static void styleListHeading(Activity activity, View view) {
        if (!(view instanceof TextView)) {
            return;
        }
        TextView label = (TextView) view;
        label.setTextSize(17);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface));
        label.setPadding(FieldUi.dp(activity, 2), FieldUi.dp(activity, 12),
                0, FieldUi.dp(activity, 8));
    }

    private static void styleFolderList(Activity activity, View view) {
        if (!(view instanceof ListView)) {
            return;
        }
        ListView list = (ListView) view;
        list.setClipToPadding(false);
        list.setPadding(0, 0, 0, FieldUi.dp(activity, 12));
        list.setDividerHeight(FieldUi.dp(activity, 8));
    }

    private static void tintCardText(Activity activity, View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            text.setTextColor(FieldUi.color(activity, R.color.fpp_on_primary_container));
            return;
        }
        if (view instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) view;
            button.setTextColor(FieldUi.color(activity, R.color.fpp_primary));
            button.setStrokeColor(ColorStateList.valueOf(
                    FieldUi.color(activity, R.color.fpp_primary)));
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintCardText(activity, group.getChildAt(i));
            }
        }
    }
}
