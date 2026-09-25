package com.inandout.fieldphotoprep;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class PropertyListAdapter extends ArrayAdapter<DriveFolder> {
    private final LayoutInflater inflater;
    private final List<DriveFolder> folders;
    private final Map<String, Integer> protectedPhotoCountsByAddressId;

    PropertyListAdapter(Context context, List<DriveFolder> folders) {
        this(context, folders, Collections.emptyMap());
    }

    PropertyListAdapter(
            Context context,
            List<DriveFolder> folders,
            Map<String, Integer> protectedPhotoCountsByAddressId) {
        super(context, R.layout.row_home_property, folders);
        this.inflater = LayoutInflater.from(context);
        this.folders = folders;
        this.protectedPhotoCountsByAddressId = protectedPhotoCountsByAddressId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = inflater.inflate(R.layout.row_home_property, parent, false);
        }

        TextView name = row.findViewById(R.id.property_name);
        TextView disambiguator = row.findViewById(R.id.property_disambiguator);
        TextView photoCount = row.findViewById(R.id.property_photo_count);
        DriveFolder folder = getItem(position);
        if (folder == null) {
            name.setText("");
            disambiguator.setVisibility(View.GONE);
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
