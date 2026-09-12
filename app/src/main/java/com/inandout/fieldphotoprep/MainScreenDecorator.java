package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Structural Concept 3 presentation for MainActivity.
 *
 * This deliberately owns presentation only. All Drive/folder/navigation listeners remain attached
 * to the original MainActivity views. The redesign makes the field workflow compact instead of
 * enlarging the existing development-oriented screen.
 */
final class MainScreenDecorator {
    private static final String TAG = "concept3-main-structural-v2";

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

        if (TAG.equals(root.getTag())) {
            refreshVisibleFolderRows(activity, root.getChildAt(5));
            return;
        }
        root.setTag(TAG);

        FieldUi.applyPageBackground(root);
        applySystemBarInsets(activity, root);

        styleHeader(activity, root.getChildAt(0));
        styleStatus(activity, root.getChildAt(1));
        styleAddressHome(activity, root.getChildAt(2));
        styleWorkOrders(activity, root.getChildAt(3));
        styleListHeading(activity, root.getChildAt(4));
        styleFolderList(activity, root.getChildAt(5));
    }

    private static void applySystemBarInsets(Activity activity, LinearLayout root) {
        final int horizontal = FieldUi.dp(activity, 16);
        final int topBase = FieldUi.dp(activity, 6);
        final int bottomBase = FieldUi.dp(activity, 10);
        root.setPadding(horizontal, topBase, horizontal, bottomBase);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(
                    horizontal,
                    topBase + insets.getSystemWindowInsetTop(),
                    horizontal,
                    bottomBase + insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
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
                button.setCornerRadius(FieldUi.dp(activity, 12));
                button.setMinHeight(FieldUi.dp(activity, 38));
                button.setTextSize(13);
            } else if (child instanceof LinearLayout) {
                LinearLayout titles = (LinearLayout) child;
                if (titles.getChildCount() > 0 && titles.getChildAt(0) instanceof TextView) {
                    TextView title = (TextView) titles.getChildAt(0);
                    title.setTextSize(21);
                    title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    title.setTextColor(FieldUi.color(activity, R.color.fpp_on_background));
                    title.setPadding(0, 0, 0, 0);
                }
                if (titles.getChildCount() > 1 && titles.getChildAt(1) instanceof TextView) {
                    TextView subtitle = (TextView) titles.getChildAt(1);
                    subtitle.setTextSize(12);
                    subtitle.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
                    subtitle.setPadding(0, 0, 0, FieldUi.dp(activity, 2));
                }
            }
        }
    }

    private static void styleStatus(Activity activity, View view) {
        if (!(view instanceof TextView)) {
            return;
        }
        TextView status = (TextView) view;
        status.setTextSize(12);
        status.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
        status.setPadding(FieldUi.dp(activity, 1), FieldUi.dp(activity, 2),
                FieldUi.dp(activity, 1), FieldUi.dp(activity, 5));
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
            compactDriveCard(activity, (MaterialCardView) drive);
        }

        if (controls.getChildCount() > 1 && controls.getChildAt(1) instanceof MaterialButton) {
            MaterialButton newAddress = (MaterialButton) controls.getChildAt(1);
            newAddress.setCornerRadius(FieldUi.dp(activity, 14));
            newAddress.setMinHeight(FieldUi.dp(activity, 44));
            newAddress.setTextSize(14);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.END;
            params.topMargin = FieldUi.dp(activity, 4);
            params.bottomMargin = FieldUi.dp(activity, 2);
            newAddress.setLayoutParams(params);
        }
    }

    private static void compactDriveCard(Activity activity, MaterialCardView card) {
        card.setRadius(FieldUi.dp(activity, 14));
        card.setStrokeWidth(FieldUi.dp(activity, 1));
        card.setStrokeColor(FieldUi.color(activity, R.color.fpp_outline));
        card.setCardElevation(0);
        card.setCardBackgroundColor(FieldUi.color(activity, R.color.fpp_surface));
        card.setContentPadding(
                FieldUi.dp(activity, 12),
                FieldUi.dp(activity, 9),
                FieldUi.dp(activity, 12),
                FieldUi.dp(activity, 9));

        View child = card.getChildCount() > 0 ? card.getChildAt(0) : null;
        if (!(child instanceof LinearLayout)) {
            return;
        }
        LinearLayout content = (LinearLayout) child;
        if (content.getChildCount() > 0 && content.getChildAt(0) instanceof TextView) {
            TextView title = (TextView) content.getChildAt(0);
            title.setText("Drive");
            title.setTextSize(12);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setTextColor(FieldUi.color(activity, R.color.fpp_primary));
            title.setPadding(0, 0, 0, 0);
        }
        if (content.getChildCount() > 1 && content.getChildAt(1) instanceof TextView) {
            TextView master = (TextView) content.getChildAt(1);
            master.setTextSize(14);
            master.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            master.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface));
            master.setPadding(0, FieldUi.dp(activity, 2), 0, FieldUi.dp(activity, 5));
        }
        if (content.getChildCount() > 2 && content.getChildAt(2) instanceof LinearLayout) {
            LinearLayout buttons = (LinearLayout) content.getChildAt(2);
            buttons.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            for (int i = 0; i < buttons.getChildCount(); i++) {
                View buttonView = buttons.getChildAt(i);
                if (buttonView instanceof MaterialButton) {
                    MaterialButton button = (MaterialButton) buttonView;
                    button.setMinHeight(FieldUi.dp(activity, 36));
                    button.setTextSize(12);
                    button.setCornerRadius(FieldUi.dp(activity, 11));
                    CharSequence text = button.getText();
                    if (text != null && "Connect Drive".contentEquals(text)) {
                        TextView master = content.getChildCount() > 1 && content.getChildAt(1) instanceof TextView
                                ? (TextView) content.getChildAt(1) : null;
                        if (master != null && master.getText() != null
                                && master.getText().toString().startsWith("Connected to ")) {
                            button.setText("Change");
                        }
                    }
                }
            }
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
        body.setPadding(0, 0, 0, FieldUi.dp(activity, 2));

        if (body.getChildCount() > 0 && body.getChildAt(0) instanceof TextView) {
            TextView helper = (TextView) body.getChildAt(0);
            helper.setTextSize(12);
            helper.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
            helper.setPadding(FieldUi.dp(activity, 1), 0, 0, FieldUi.dp(activity, 4));
        }

        for (int i = 1; i < body.getChildCount(); i++) {
            if (body.getChildAt(i) instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) body.getChildAt(i);
                card.setRadius(FieldUi.dp(activity, 14));
                card.setCardElevation(0);
                card.setContentPadding(
                        FieldUi.dp(activity, 12),
                        FieldUi.dp(activity, 10),
                        FieldUi.dp(activity, 12),
                        FieldUi.dp(activity, 10));
                if (i == 1) {
                    card.setStrokeWidth(FieldUi.dp(activity, 1));
                    card.setStrokeColor(FieldUi.color(activity, R.color.fpp_primary));
                    card.setCardBackgroundColor(FieldUi.color(activity, R.color.fpp_primary_container));
                }
                compactCardText(activity, card);
            }
        }
    }

    private static void compactCardText(Activity activity, View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getTextSize() / activity.getResources().getDisplayMetrics().scaledDensity > 16f) {
                text.setTextSize(15);
            }
            return;
        }
        if (view instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) view;
            button.setMinHeight(FieldUi.dp(activity, 42));
            button.setTextSize(13);
            button.setCornerRadius(FieldUi.dp(activity, 13));
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                compactCardText(activity, group.getChildAt(i));
            }
        }
    }

    private static void styleListHeading(Activity activity, View view) {
        if (!(view instanceof TextView)) {
            return;
        }
        TextView label = (TextView) view;
        if ("Addresses".contentEquals(label.getText())) {
            label.setText("Properties");
        }
        label.setTextSize(16);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface));
        label.setPadding(FieldUi.dp(activity, 1), FieldUi.dp(activity, 7),
                0, FieldUi.dp(activity, 5));
    }

    private static void styleFolderList(Activity activity, View view) {
        if (!(view instanceof ListView)) {
            return;
        }
        ListView list = (ListView) view;
        list.setClipToPadding(false);
        list.setPadding(0, 0, 0, FieldUi.dp(activity, 8));
        list.setDividerHeight(FieldUi.dp(activity, 6));
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                styleVisibleListChildren(activity, list);
            }

            @Override
            public void onScroll(
                    AbsListView view,
                    int firstVisibleItem,
                    int visibleItemCount,
                    int totalItemCount) {
                styleVisibleListChildren(activity, list);
            }
        });
        list.post(() -> styleVisibleListChildren(activity, list));
    }

    private static void refreshVisibleFolderRows(Activity activity, View view) {
        if (view instanceof ListView) {
            styleVisibleListChildren(activity, (ListView) view);
        }
    }

    private static void styleVisibleListChildren(Activity activity, ListView list) {
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            if (!(child instanceof MaterialCardView)) {
                continue;
            }
            MaterialCardView card = (MaterialCardView) child;
            card.setRadius(FieldUi.dp(activity, 14));
            card.setCardElevation(0);
            card.setStrokeWidth(FieldUi.dp(activity, 1));
            card.setStrokeColor(FieldUi.color(activity, R.color.fpp_outline));
            card.setMinimumHeight(FieldUi.dp(activity, 66));
            View body = card.getChildCount() > 0 ? card.getChildAt(0) : null;
            if (body instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) body;
                layout.setPadding(
                        FieldUi.dp(activity, 13),
                        FieldUi.dp(activity, 9),
                        FieldUi.dp(activity, 13),
                        FieldUi.dp(activity, 9));
                if (layout.getChildCount() > 0 && layout.getChildAt(0) instanceof TextView) {
                    TextView name = (TextView) layout.getChildAt(0);
                    name.setText(humanizeFolderName(name.getText()));
                    name.setTextSize(15);
                    name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    name.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface));
                    name.setPadding(0, 0, 0, 0);
                }
                if (layout.getChildCount() > 1 && layout.getChildAt(1) instanceof TextView) {
                    TextView meta = (TextView) layout.getChildAt(1);
                    meta.setTextSize(12);
                    meta.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
                    meta.setPadding(0, FieldUi.dp(activity, 2), 0, 0);
                }
            }
        }
    }

    private static CharSequence humanizeFolderName(CharSequence raw) {
        if (raw == null) {
            return "";
        }
        return raw.toString()
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
