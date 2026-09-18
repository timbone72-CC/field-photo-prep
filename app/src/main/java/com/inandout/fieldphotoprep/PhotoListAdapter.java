package com.inandout.fieldphotoprep;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Virtualized presentation adapter for the Photos screen.
 *
 * This class owns row inflation/recycling and thumbnail presentation only. It does not own photo
 * persistence, queue state, Drive behavior, upload/retry, reconciliation, or deletion.
 */
final class PhotoListAdapter extends BaseAdapter {
    interface Listener {
        void onBatchSelectionChanged(String photoId, boolean checked);

        void onPhotoClicked(String photoId);
    }

    static final class Item {
        final PendingPhotoRecord record;
        final boolean checkboxEnabled;
        final String statusText;
        final String timeText;
        final File thumbnailSource;

        Item(
                PendingPhotoRecord record,
                boolean checkboxEnabled,
                String statusText,
                String timeText,
                File thumbnailSource) {
            this.record = record;
            this.checkboxEnabled = checkboxEnabled;
            this.statusText = statusText;
            this.timeText = timeText;
            this.thumbnailSource = thumbnailSource;
        }
    }

    private static final int MAX_CACHED_THUMBNAILS = 24;

    private final LayoutInflater inflater;
    private final Listener listener;
    private final int thumbnailTargetPx;
    private final ExecutorService thumbnailExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> thumbnailCache =
            new LruCache<>(MAX_CACHED_THUMBNAILS);
    private final Set<String> thumbnailLoadsInFlight = new HashSet<>();
    private final List<Item> items = new ArrayList<>();
    private final LinkedHashSet<String> batchSelectedPhotoIds = new LinkedHashSet<>();

    private String selectedPhotoId;
    private volatile boolean closed;

    PhotoListAdapter(Context context, Listener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        this.thumbnailTargetPx = Math.max(
                1,
                Math.round(160f * context.getResources().getDisplayMetrics().density));
    }

    void replaceItems(
            List<Item> nextItems,
            Set<String> nextBatchSelection,
            String nextSelectedPhotoId) {
        items.clear();
        if (nextItems != null) {
            items.addAll(nextItems);
        }
        batchSelectedPhotoIds.clear();
        if (nextBatchSelection != null) {
            batchSelectedPhotoIds.addAll(nextBatchSelection);
        }
        selectedPhotoId = nextSelectedPhotoId;
        notifyDataSetChanged();
    }

    void setSelectedPhotoId(String photoId) {
        if (photoId == null ? selectedPhotoId == null : photoId.equals(selectedPhotoId)) {
            return;
        }
        selectedPhotoId = photoId;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Item getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).record.id().hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder;
        if (row == null) {
            row = inflater.inflate(R.layout.row_photo, parent, false);
            holder = new ViewHolder(row);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        Item item = getItem(position);
        PendingPhotoRecord record = item.record;
        String photoId = record.id();

        holder.batchCheckBox.setOnCheckedChangeListener(null);
        holder.batchCheckBox.setVisibility(record.state() == PendingPhotoRecord.State.UPLOADED
                ? View.INVISIBLE
                : View.VISIBLE);
        holder.batchCheckBox.setChecked(batchSelectedPhotoIds.contains(photoId));
        holder.batchCheckBox.setEnabled(item.checkboxEnabled);
        holder.batchCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                batchSelectedPhotoIds.add(photoId);
            } else {
                batchSelectedPhotoIds.remove(photoId);
            }
            listener.onBatchSelectionChanged(photoId, isChecked);
        });

        holder.stateText.setText(item.statusText);
        holder.timeText.setText(item.timeText);
        row.setBackgroundResource(photoId.equals(selectedPhotoId)
                ? R.drawable.bg_concept_selected
                : R.drawable.bg_concept_card);
        row.setOnClickListener(v -> listener.onPhotoClicked(photoId));

        bindThumbnail(holder.thumbnail, item);
        return row;
    }

    void shutdown() {
        closed = true;
        thumbnailExecutor.shutdownNow();
        synchronized (thumbnailLoadsInFlight) {
            thumbnailLoadsInFlight.clear();
        }
        thumbnailCache.evictAll();
    }

    private void bindThumbnail(ImageView view, Item item) {
        File source = item.thumbnailSource;
        if (source == null || !source.isFile() || source.length() <= 0L) {
            view.setTag(null);
            view.setImageDrawable(null);
            return;
        }

        String key = thumbnailKey(item.record.id(), source);
        view.setTag(key);

        Bitmap cached = thumbnailCache.get(key);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }

        view.setImageDrawable(null);
        synchronized (thumbnailLoadsInFlight) {
            if (closed || !thumbnailLoadsInFlight.add(key)) {
                return;
            }
        }

        thumbnailExecutor.execute(() -> {
            Bitmap decoded = decodeThumbnail(source, thumbnailTargetPx);
            if (decoded != null && !closed) {
                thumbnailCache.put(key, decoded);
            }
            synchronized (thumbnailLoadsInFlight) {
                thumbnailLoadsInFlight.remove(key);
            }

            if (closed) {
                return;
            }
            mainHandler.post(() -> {
                if (closed || !key.equals(view.getTag())) {
                    return;
                }
                Bitmap ready = thumbnailCache.get(key);
                if (ready != null) {
                    view.setImageBitmap(ready);
                }
            });
        });
    }

    private static Bitmap decodeThumbnail(File source, int targetPx) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            int sample = 1;
            while (bounds.outWidth / sample > targetPx * 2
                    || bounds.outHeight / sample > targetPx * 2) {
                sample *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, sample);
            return BitmapFactory.decodeFile(source.getAbsolutePath(), options);
        } catch (RuntimeException | OutOfMemoryError ignored) {
            return null;
        }
    }

    private static String thumbnailKey(String photoId, File source) {
        return photoId
                + "|"
                + source.getAbsolutePath()
                + "|"
                + source.length()
                + "|"
                + source.lastModified();
    }

    private static final class ViewHolder {
        final CheckBox batchCheckBox;
        final ImageView thumbnail;
        final TextView stateText;
        final TextView timeText;

        ViewHolder(View row) {
            batchCheckBox = row.findViewById(R.id.photo_row_check);
            thumbnail = row.findViewById(R.id.photo_row_thumb);
            stateText = row.findViewById(R.id.photo_row_status);
            timeText = row.findViewById(R.id.photo_row_time);
        }
    }
}
