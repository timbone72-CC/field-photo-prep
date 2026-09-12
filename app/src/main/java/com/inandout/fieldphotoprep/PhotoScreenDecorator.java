package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

/**
 * Concept 3 presentation adapter for the existing photo workflow.
 *
 * This class deliberately does not own photo, queue, preparation, upload, retry,
 * reconciliation, or cleanup behavior. It reuses the exact View instances built and
 * wired by PhotoCaptureActivity. CameraCaptureActivity is not decorated here and
 * remains a locked design surface.
 */
final class PhotoScreenDecorator {
    private static final int EXPECTED_ROOT_CHILDREN = 19;

    private PhotoScreenDecorator() {}

    static void decorate(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }
        ViewGroup contentRoot = (ViewGroup) content;
        if (contentRoot.getChildCount() != 1 || !(contentRoot.getChildAt(0) instanceof ScrollView)) {
            return;
        }
        ScrollView scroll = (ScrollView) contentRoot.getChildAt(0);
        if (scroll.getChildCount() != 1 || !(scroll.getChildAt(0) instanceof LinearLayout)) {
            return;
        }
        LinearLayout oldRoot = (LinearLayout) scroll.getChildAt(0);
        if (oldRoot.getChildCount() != EXPECTED_ROOT_CHILDREN) {
            return;
        }

        View[] views = new View[EXPECTED_ROOT_CHILDREN];
        for (int i = 0; i < EXPECTED_ROOT_CHILDREN; i++) {
            views[i] = oldRoot.getChildAt(i);
        }
        if (!hasExpectedShape(views)) {
            return;
        }

        TextView title = (TextView) views[0];
        TextView phase = (TextView) views[1];
        TextView status = (TextView) views[2];
        TextView address = (TextView) views[3];
        TextView workOrder = (TextView) views[4];
        TextView identity = (TextView) views[5];
        Button openCamera = (Button) views[6];
        TextView pendingCount = (TextView) views[7];
        TextView batchSelection = (TextView) views[8];
        LinearLayout batchControls = (LinearLayout) views[9];
        Button uploadSelected = (Button) views[10];
        LinearLayout pendingList = (LinearLayout) views[11];
        TextView selectedPhoto = (TextView) views[12];
        TextView preparedPhoto = (TextView) views[13];
        Button prepare = (Button) views[14];
        Button uploadOne = (Button) views[15];
        Button reconcile = (Button) views[16];
        Button discard = (Button) views[17];
        Button back = (Button) views[18];

        oldRoot.removeAllViews();
        oldRoot.setPadding(FieldUi.dp(activity, 16), FieldUi.dp(activity, 8),
                FieldUi.dp(activity, 16), FieldUi.dp(activity, 22));
        FieldUi.applyPageBackground(oldRoot);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(FieldUi.color(activity, R.color.fpp_background));

        buildHeader(activity, oldRoot, title, phase, workOrder, address, back);
        styleStatus(activity, status);
        oldRoot.addView(status);
        buildDestinationCard(activity, oldRoot, workOrder, address, identity);

        styleCameraAction(activity, openCamera);
        oldRoot.addView(openCamera, fullWidthParams(activity, 4, 14));

        pendingCount.setTextSize(18);
        pendingCount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pendingCount.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface));
        pendingCount.setPadding(FieldUi.dp(activity, 2), FieldUi.dp(activity, 4),
                0, FieldUi.dp(activity, 2));
        oldRoot.addView(pendingCount);

        batchSelection.setTextSize(13);
        batchSelection.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
        batchSelection.setPadding(FieldUi.dp(activity, 2), 0, 0, FieldUi.dp(activity, 8));
        oldRoot.addView(batchSelection);

        for (int i = 0; i < batchControls.getChildCount(); i++) {
            View child = batchControls.getChildAt(i);
            if (child instanceof Button) {
                styleSecondary(activity, (Button) child);
            }
        }
        oldRoot.addView(batchControls, fullWidthParams(activity, 0, 10));

        pendingList.setPadding(0, 0, 0, 0);
        pendingList.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                stylePhotoRow(activity, child);
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
                // Queue ownership remains in PhotoCaptureActivity.
            }
        });
        for (int i = 0; i < pendingList.getChildCount(); i++) {
            stylePhotoRow(activity, pendingList.getChildAt(i));
        }
        oldRoot.addView(pendingList);

        TextView actionsHeading = FieldUi.sectionTitle(activity, "Selected photo");
        oldRoot.addView(actionsHeading);
        buildSelectedActionsCard(
                activity, oldRoot, selectedPhoto, preparedPhoto,
                prepare, uploadOne, reconcile, discard);

        // Concept 3 keeps the main upload action fixed at the bottom. The same existing
        // Button instance and listener are reused; only its parent and presentation change.
        contentRoot.removeView(scroll);
        LinearLayout shell = FieldUi.vertical(activity);
        FieldUi.applyPageBackground(shell);
        shell.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout uploadBar = FieldUi.vertical(activity);
        uploadBar.setPadding(FieldUi.dp(activity, 16), FieldUi.dp(activity, 8),
                FieldUi.dp(activity, 16), FieldUi.dp(activity, 12));
        uploadBar.setBackgroundColor(FieldUi.color(activity, R.color.fpp_surface));
        styleUploadAction(activity, uploadSelected);
        uploadBar.addView(uploadSelected, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        shell.addView(uploadBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        contentRoot.addView(shell, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private static void buildHeader(
            Activity activity,
            LinearLayout root,
            TextView title,
            TextView phase,
            TextView workOrder,
            TextView address,
            Button back) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        styleTextButton(activity, back, "Back");
        back.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_arrow_back_24, 0, 0, 0);
        back.setCompoundDrawablePadding(FieldUi.dp(activity, 6));
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        backParams.setMarginEnd(FieldUi.dp(activity, 8));
        header.addView(back, backParams);

        LinearLayout heading = FieldUi.vertical(activity);
        title.setText("Photos");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(FieldUi.color(activity, R.color.fpp_on_background));
        title.setPadding(0, 0, 0, 0);
        heading.addView(title);

        phase.setText(stripPrefix(address.getText().toString(), "Address: ", "Selected property"));
        phase.setTextSize(13);
        phase.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
        phase.setPadding(0, 0, 0, 0);
        heading.addView(phase);
        header.addView(heading, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);
    }

    private static void styleStatus(Activity activity, TextView status) {
        status.setTextSize(13);
        status.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
        status.setPadding(FieldUi.dp(activity, 2), FieldUi.dp(activity, 5),
                FieldUi.dp(activity, 2), FieldUi.dp(activity, 9));
    }

    private static void buildDestinationCard(
            Activity activity,
            LinearLayout root,
            TextView workOrder,
            TextView address,
            TextView identity) {
        LinearLayout context = FieldUi.vertical(activity);
        TextView destinationLabel = FieldUi.muted(activity, "CURRENT WORK ORDER");
        destinationLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        destinationLabel.setTextColor(FieldUi.color(activity, R.color.fpp_primary));
        context.addView(destinationLabel);

        workOrder.setText(stripPrefix(workOrder.getText().toString(), "Work order: ", "No work order"));
        workOrder.setTextSize(17);
        workOrder.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        workOrder.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface));
        workOrder.setPadding(0, FieldUi.dp(activity, 4), 0, FieldUi.dp(activity, 2));
        context.addView(workOrder);

        address.setText(stripPrefix(address.getText().toString(), "Address: ", "No address"));
        address.setTextSize(14);
        address.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
        address.setPadding(0, 0, 0, FieldUi.dp(activity, 4));
        context.addView(address);

        identity.setTextSize(11);
        identity.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
        identity.setAlpha(0.62f);
        identity.setPadding(0, 0, 0, 0);
        context.addView(identity);

        MaterialCardView card = FieldUi.paddedCard(activity, context);
        card.setCardBackgroundColor(FieldUi.color(activity, R.color.fpp_primary_container));
        card.setStrokeColor(FieldUi.color(activity, R.color.fpp_primary));
        root.addView(card, FieldUi.cardParams(activity));
    }

    private static void buildSelectedActionsCard(
            Activity activity,
            LinearLayout root,
            TextView selectedPhoto,
            TextView preparedPhoto,
            Button prepare,
            Button uploadOne,
            Button reconcile,
            Button discard) {
        LinearLayout content = FieldUi.vertical(activity);
        selectedPhoto.setTextSize(13);
        selectedPhoto.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        selectedPhoto.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface));
        selectedPhoto.setPadding(0, 0, 0, FieldUi.dp(activity, 4));
        content.addView(selectedPhoto);

        preparedPhoto.setTextSize(13);
        preparedPhoto.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface_variant));
        preparedPhoto.setPadding(0, 0, 0, FieldUi.dp(activity, 10));
        content.addView(preparedPhoto);

        styleSecondary(activity, prepare);
        styleSecondary(activity, uploadOne);
        styleSecondary(activity, reconcile);
        styleDanger(activity, discard);
        addAction(content, prepare, activity);
        addAction(content, uploadOne, activity);
        addAction(content, reconcile, activity);
        addAction(content, discard, activity);
        root.addView(FieldUi.paddedCard(activity, content), FieldUi.cardParams(activity));
    }

    private static boolean hasExpectedShape(View[] views) {
        return views[0] instanceof TextView
                && views[1] instanceof TextView
                && views[2] instanceof TextView
                && views[3] instanceof TextView
                && views[4] instanceof TextView
                && views[5] instanceof TextView
                && views[6] instanceof Button
                && views[7] instanceof TextView
                && views[8] instanceof TextView
                && views[9] instanceof LinearLayout
                && views[10] instanceof Button
                && views[11] instanceof LinearLayout
                && views[12] instanceof TextView
                && views[13] instanceof TextView
                && views[14] instanceof Button
                && views[15] instanceof Button
                && views[16] instanceof Button
                && views[17] instanceof Button
                && views[18] instanceof Button;
    }

    private static void stylePhotoRow(Activity activity, View child) {
        if (!(child instanceof LinearLayout)) {
            return;
        }
        LinearLayout row = (LinearLayout) child;
        if (row.getChildCount() < 2
                || !(row.getChildAt(0) instanceof CheckBox)
                || !(row.getChildAt(1) instanceof Button)) {
            return;
        }
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = FieldUi.dp(activity, 10);
        row.setPadding(pad, FieldUi.dp(activity, 8), pad, FieldUi.dp(activity, 8));
        row.setBackground(roundRect(activity,
                FieldUi.color(activity, R.color.fpp_surface),
                FieldUi.color(activity, R.color.fpp_outline)));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = FieldUi.dp(activity, 9);
        row.setLayoutParams(rowParams);

        CheckBox checkbox = (CheckBox) row.getChildAt(0);
        checkbox.setText("");
        checkbox.setButtonTintList(ColorStateList.valueOf(
                FieldUi.color(activity, R.color.fpp_primary)));

        Button photoButton = (Button) row.getChildAt(1);
        String original = photoButton.getText().toString();
        photoButton.setText(friendlyPhotoLabel(original));
        photoButton.setAllCaps(false);
        photoButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        photoButton.setTextSize(14);
        photoButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        photoButton.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface));
        photoButton.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        photoButton.setMinHeight(FieldUi.dp(activity, 52));
    }

    private static String friendlyPhotoLabel(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "Photo";
        }
        String[] lines = raw.split("\\n", 2);
        String stateLine = lines[0];
        String timeLine = lines.length > 1 ? lines[1] : "";
        int idSeparator = timeLine.indexOf(" · …");
        if (idSeparator >= 0) {
            timeLine = timeLine.substring(0, idSeparator);
        }

        String status;
        if (stateLine.contains("UNCERTAIN")) {
            status = stateLine.contains("RECONCILING") ? "Checking upload" : "Needs attention";
        } else if (stateLine.contains("UPLOADED")) {
            status = "Uploaded";
        } else if (stateLine.contains("UPLOADING") || stateLine.contains("SENDING")) {
            status = "Uploading";
        } else if (stateLine.contains("FAILED")) {
            status = "Upload failed — safe to retry";
        } else if (stateLine.contains("IMAGE MISSING")) {
            status = "Photo needs inspection";
        } else if (stateLine.contains("PREPARING")) {
            status = "Preparing";
        } else if (stateLine.contains("PREPARED")) {
            status = "Ready to upload";
        } else if (stateLine.contains("WAITING")) {
            status = "Waiting for preparation";
        } else if (stateLine.contains("CAPTURING")) {
            status = "Capturing";
        } else {
            status = "Photo";
        }
        return timeLine.isEmpty() ? status : status + "\n" + timeLine;
    }

    private static void addAction(LinearLayout parent, Button button, Activity activity) {
        parent.addView(button, fullWidthParams(activity, 0, 8));
    }

    private static LinearLayout.LayoutParams fullWidthParams(
            Activity activity, int topMargin, int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = FieldUi.dp(activity, topMargin);
        params.bottomMargin = FieldUi.dp(activity, bottomMargin);
        return params;
    }

    private static void styleCameraAction(Activity activity, Button button) {
        button.setText("Open Camera");
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(FieldUi.dp(activity, 58));
        button.setTextColor(FieldUi.color(activity, R.color.fpp_on_camera));
        button.setBackgroundTintList(ColorStateList.valueOf(
                FieldUi.color(activity, R.color.fpp_camera)));
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_camera_24, 0, 0, 0);
        button.setCompoundDrawablePadding(FieldUi.dp(activity, 8));
        button.setGravity(Gravity.CENTER);
    }

    private static void styleUploadAction(Activity activity, Button button) {
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(FieldUi.dp(activity, 56));
        button.setTextColor(FieldUi.color(activity, R.color.fpp_on_primary));
        button.setBackgroundTintList(ColorStateList.valueOf(
                FieldUi.color(activity, R.color.fpp_primary)));
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_upload_24, 0, 0, 0);
        button.setCompoundDrawablePadding(FieldUi.dp(activity, 8));
        button.setGravity(Gravity.CENTER);
    }

    private static void styleSecondary(Activity activity, Button button) {
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setMinHeight(FieldUi.dp(activity, 46));
        button.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface));
        button.setBackgroundTintList(ColorStateList.valueOf(
                FieldUi.color(activity, R.color.fpp_surface_variant)));
    }

    private static void styleTextButton(Activity activity, Button button, String text) {
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(FieldUi.dp(activity, 44));
        button.setTextColor(FieldUi.color(activity, R.color.fpp_primary));
        button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
    }

    private static void styleDanger(Activity activity, Button button) {
        styleSecondary(activity, button);
        button.setTextColor(FieldUi.color(activity, R.color.fpp_error));
        button.setBackgroundTintList(ColorStateList.valueOf(
                FieldUi.color(activity, R.color.fpp_error_container)));
    }

    private static GradientDrawable roundRect(Activity activity, int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(FieldUi.dp(activity, 16));
        drawable.setStroke(FieldUi.dp(activity, 1), stroke);
        return drawable;
    }

    private static String stripPrefix(String value, String prefix, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }
}
