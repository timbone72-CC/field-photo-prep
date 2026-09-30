package com.inandout.fieldphotoprep;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class PropertyListAdapter extends ArrayAdapter<DriveFolder> {
    private static final DateTimeFormatter LAST_USED_FORMAT =
            DateTimeFormatter.ofPattern("MMM d");

    private final LayoutInflater inflater;
    private final List<DriveFolder> folders;
    private final Map<String, Integer> protectedPhotoCountsByAddressId;
    private final Map<String, PropertyLifecycleStore.Snapshot> lifecycleByAddressId;

    PropertyListAdapter(Context context, List<DriveFolder> folders) {
        this(context, folders, Collections.emptyMap(), Collections.emptyMap());
    }

    PropertyListAdapter(
            Context context,
            List<DriveFolder> folders,
            Map<String, Integer> protectedPhotoCountsByAddressId) {
        this(context, folders, protectedPhotoCountsByAddressId, Collections.emptyMap());
    }

    PropertyListAdapter(
            Context context,
            List<DriveFolder> folders,
            Map<String, Integer> protectedPhotoCountsByAddressId,
            Map<String, PropertyLifecycleStore.Snapshot> lifecycleByAddressId) {
        super(context, R.layout.row_home_property, folders);
        this.inflater = LayoutInflater.from(context);
        this.folders = folders;
        this.protectedPhotoCountsByAddressId = protectedPhotoCountsByAddressId;
        this.lifecycleByAddressId = lifecycleByAddressId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = inflater.inflate(R.layout.row_home_property, parent, false);
        }

        TextView name = row.findViewById(R.id.property_name);
        TextView disambiguator = row.findViewById(R.id.property_disambiguator);
        TextView lifecycle = row.findViewById(R.id.property_lifecycle);
        TextView photoCount = row.findViewById(R.id.property_photo_count);
        DriveFolder folder = getItem(position);
        if (folder == null) {
            name.setText("");
            disambiguator.setVisibility(View.GONE);
            lifecycle.setVisibility(View.GONE);
            photoCount.setVisibility(View.GONE);
            return row;
        }

        String display = PropertyDisplayName.fromDriveFolderName(folder.name());
        boolean ambiguous = AddressFolderAmbiguity.hasAmbiguousPeer(folder, folders);
        name.setText(ambiguous ? folder.name() : display);

        if (ambiguous) {
            disambiguator.setText("Possible duplicate · ID …" + shortId(folder.id()));
            disambiguator.setVisibility(View.VISIBLE);
        } else if (hasDuplicateDisplayName(display)) {
            disambiguator.setText("Drive: " + folder.name() + " · ID …" + shortId(folder.id()));
            disambiguator.setVisibility(View.VISIBLE);
        } else {
            disambiguator.setText("");
            disambiguator.setVisibility(View.GONE);
        }

        PropertyLifecycleStore.Snapshot snapshot = lifecycleByAddressId == null
                ? null
                : lifecycleByAddressId.get(folder.id());
        renderLifecycle(lifecycle, snapshot);

        int count = protectedPhotoCountsByAddressId == null
                ? 0
                : protectedPhotoCountsByAddressId.getOrDefault(folder.id(), 0);
        if (count > 0) {
            photoCount.setText(count + (count == 1 ? " photo" : " photos"));
            photoCount.setVisibility(View.VISIBLE);
        } else {
            photoCount.setText("");
            photoCount.setVisibility(View.GONE);
        }
        return row;
    }

    private void renderLifecycle(
            TextView lifecycle,
            PropertyLifecycleStore.Snapshot snapshot) {
        boolean archived = snapshot != null
                && snapshot.state() == PropertyLifecycleStore.State.ARCHIVED;
        boolean hasLastUsed = snapshot != null && snapshot.hasLastUsed();

        if (!archived && !hasLastUsed) {
            lifecycle.setText("");
            lifecycle.setVisibility(View.GONE);
            return;
        }

        StringBuilder label = new StringBuilder();
        if (archived) {
            label.append("Archived");
        }
        if (hasLastUsed) {
            if (label.length() > 0) {
                label.append(" · ");
            }
            label.append("Last used ")
                    .append(LAST_USED_FORMAT.format(
                            Instant.ofEpochMilli(snapshot.lastUsedEpochMs())
                                    .atZone(ZoneId.systemDefault())));
        }
        lifecycle.setText(label.toString());
        lifecycle.setVisibility(View.VISIBLE);
    }

    private boolean hasDuplicateDisplayName(String displayName) {
        int count = 0;
        for (DriveFolder folder : folders) {
            if (PropertyDisplayName.fromDriveFolderName(folder.name()).equals(displayName)
                    && ++count > 1) {
                return true;
            }
        }
        return false;
    }

    private static String shortId(String id) {
        if (id == null || id.isEmpty()) {
            return "unknown";
        }
        return id.length() <= 8 ? id : id.substring(id.length() - 8);
    }
}
