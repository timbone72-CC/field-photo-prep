package com.inandout.fieldphotoprep;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds the visual pieces that were explicit in the approved Concept 3 mockup but were missing from
 * the first Phase 9 implementation: real local thumbnails, compact photo cards and state badges.
 *
 * This class is read-only with respect to photo/queue/upload state. The existing CheckBox and
 * Button instances remain the owners of selection/click behavior.
 */
final class Concept3PhotoEnhancer {
    private static final int THUMBNAIL_DP = 72;

    private Concept3PhotoEnhancer() {}

    static void enhance(Activity activity) {
        if (!(activity instanceof PhotoCaptureActivity)) {
            return;
        }

        DriveFolder workOrder = new FolderPrefs(activity).getCurrentWorkOrder();
        if (workOrder == null) {
            return;
        }

        PendingPhotoStore store = new PendingPhotoStore(
                new File(activity.getFilesDir(), "pending_photos"));
        PhotoPreparer preparer = new PhotoPreparer(
                new File(activity.getFilesDir(), "prepared_photos"));

        List<PendingPhotoRecord> records;
        try {
            records = store.recordsForWorkOrder(workOrder.id());
        } catch (Exception ignored) {
            return;
        }

        List<LinearLayout> rows = new ArrayList<>();
        collectPhotoRows(activity.findViewById(android.R.id.content), rows);
        int count = Math.min(records.size(), rows.size());
        for (int i = 0; i < count; i++) {
            enhanceRow(activity, rows.get(i), records.get(i), store, preparer, i + 1);
        }
    }

    private static void collectPhotoRows(View view, List<LinearLayout> rows) {
        if (view instanceof LinearLayout) {
            LinearLayout row = (LinearLayout) view;
            if (isRawPhotoRow(row) || isEnhancedPhotoRow(row)) {
                rows.add(row);
                return;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectPhotoRows(group.getChildAt(i), rows);
            }
        }
    }

    private static boolean isRawPhotoRow(LinearLayout row) {
        return row.getChildCount() == 2
                && row.getChildAt(0) instanceof CheckBox
                && row.getChildAt(1) instanceof Button;
    }

    private static boolean isEnhancedPhotoRow(LinearLayout row) {
        return row.getChildCount() == 3
                && row.getChildAt(0) instanceof CheckBox
                && row.getChildAt(1) instanceof ImageView
                && row.getChildAt(2) instanceof LinearLayout;
    }

    private static void enhanceRow(
            Activity activity,
            LinearLayout row,
            PendingPhotoRecord record,
            PendingPhotoStore store,
            PhotoPreparer preparer,
            int displayIndex) {
        CheckBox checkbox = (CheckBox) row.getChildAt(0);
        Button photoButton;
        ImageView thumbnail;
        LinearLayout details;

        if (isEnhancedPhotoRow(row)) {
            thumbnail = (ImageView) row.getChildAt(1);
            details = (LinearLayout) row.getChildAt(2);
            photoButton = findButton(details);
            if (photoButton == null) {
                return;
            }
            updateThumbnail(activity, thumbnail, record, store, preparer);
            return;
        }

        photoButton = (Button) row.getChildAt(1);
        String friendly = photoButton.getText() == null ? "" : photoButton.getText().toString();
        String[] lines = friendly.split("\\n", 2);
        String status = lines.length > 0 && !lines[0].isBlank() ? lines[0] : "Photo";
        String time = lines.length > 1 ? lines[1] : "";

        row.removeView(photoButton);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(
                FieldUi.dp(activity, 10),
                FieldUi.dp(activity, 9),
                FieldUi.dp(activity, 10),
                FieldUi.dp(activity, 9));
        row.setBackground(roundRect(
                activity,
                FieldUi.color(activity, R.color.fpp_surface),
                FieldUi.color(activity, R.color.fpp_outline)));

        checkbox.setText("");
        checkbox.setButtonTintList(ColorStateList.valueOf(
                FieldUi.color(activity, R.color.fpp_primary)));
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        checkParams.setMarginEnd(FieldUi.dp(activity, 8));
        checkbox.setLayoutParams(checkParams);

        thumbnail = new ImageView(activity);
        thumbnail.setContentDescription("Photo thumbnail");
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setClipToOutline(true);
        thumbnail.setBackground(roundRect(
                activity,
                FieldUi.color(activity, R.color.fpp_surface_variant),
                FieldUi.color(activity, R.color.fpp_outline)));
        thumbnail.setOutlineProvider(ViewOutlineProviders.roundedRect(FieldUi.dp(activity, 14)));
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                FieldUi.dp(activity, THUMBNAIL_DP),
                FieldUi.dp(activity, THUMBNAIL_DP));
        imageParams.setMarginEnd(FieldUi.dp(activity, 12));
        row.addView(thumbnail, 1, imageParams);
        updateThumbnail(activity, thumbnail, record, store, preparer);

        details = FieldUi.vertical(activity);
        details.setGravity(Gravity.CENTER_VERTICAL);

        photoButton.setText("Photo " + displayIndex + (time.isEmpty() ? "" : "\n" + time));
        photoButton.setAllCaps(false);
        photoButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        photoButton.setTextSize(14);
        photoButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        photoButton.setTextColor(FieldUi.color(activity, R.color.fpp_on_surface));
        photoButton.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        photoButton.setPadding(0, 0, 0, FieldUi.dp(activity, 4));
        photoButton.setMinHeight(0);
        details.addView(photoButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView badge = statusBadge(activity, status);
        details.addView(badge, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        row.addView(details, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f));
    }

    private static void updateThumbnail(
            Activity activity,
            ImageView thumbnail,
            PendingPhotoRecord record,
            PendingPhotoStore store,
            PhotoPreparer preparer) {
        File source = null;
        try {
            File original = store.imageFile(record);
            if (original.isFile() && original.length() > 0) {
                source = original;
            } else {
                File prepared = preparer.preparedFile(record.id());
                if (prepared.isFile() && prepared.length() > 0) {
                    source = prepared;
                }
            }
        } catch (Exception ignored) {
            source = null;
        }

        if (source == null) {
            thumbnail.setImageResource(R.drawable.ic_photo_24);
            thumbnail.setColorFilter(FieldUi.color(activity, R.color.fpp_on_surface_variant));
            thumbnail.setPadding(
                    FieldUi.dp(activity, 20),
                    FieldUi.dp(activity, 20),
                    FieldUi.dp(activity, 20),
                    FieldUi.dp(activity, 20));
            thumbnail.setAlpha(0.7f);
            return;
        }

        try {
            Bitmap bitmap = PhotoThumbnailLoader.load(
                    source,
                    FieldUi.dp(activity, THUMBNAIL_DP));
            if (bitmap == null) {
                throw new IllegalStateException("No thumbnail bitmap");
            }
            thumbnail.clearColorFilter();
            thumbnail.setPadding(0, 0, 0, 0);
            thumbnail.setAlpha(1f);
            thumbnail.setImageBitmap(bitmap);
        } catch (Exception ignored) {
            thumbnail.setImageResource(R.drawable.ic_photo_24);
            thumbnail.setColorFilter(FieldUi.color(activity, R.color.fpp_on_surface_variant));
            thumbnail.setPadding(
                    FieldUi.dp(activity, 20),
                    FieldUi.dp(activity, 20),
                    FieldUi.dp(activity, 20),
                    FieldUi.dp(activity, 20));
            thumbnail.setAlpha(0.7f);
        }
    }

    private static TextView statusBadge(Activity activity, String status) {
        int background;
        int foreground;
        String normalized = status == null ? "" : status.toLowerCase();
        if (normalized.contains("ready") || normalized.contains("uploaded")) {
            background = R.color.fpp_success_container;
            foreground = R.color.fpp_success;
        } else if (normalized.contains("attention")
                || normalized.contains("failed")
                || normalized.contains("inspection")) {
            background = R.color.fpp_error_container;
            foreground = R.color.fpp_error;
        } else if (normalized.contains("checking")
                || normalized.contains("uploading")
                || normalized.contains("preparing")) {
            background = R.color.fpp_primary_container;
            foreground = R.color.fpp_primary;
        } else {
            background = R.color.fpp_warning_container;
            foreground = R.color.fpp_warning;
        }
        return FieldUi.pill(activity, status, background, foreground);
    }

    private static Button findButton(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Button) {
                return (Button) child;
            }
        }
        return null;
    }

    private static GradientDrawable roundRect(Activity activity, int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(FieldUi.dp(activity, 16));
        drawable.setStroke(FieldUi.dp(activity, 1), stroke);
        return drawable;
    }

    /** Simple outline helper without a custom View subclass. */
    private static final class ViewOutlineProviders {
        private ViewOutlineProviders() {}

        static android.view.ViewOutlineProvider roundedRect(int radiusPx) {
            return new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
                }
            };
        }
    }
}
