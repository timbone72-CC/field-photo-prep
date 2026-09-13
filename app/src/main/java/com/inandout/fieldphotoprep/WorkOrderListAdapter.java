package com.inandout.fieldphotoprep;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

final class WorkOrderListAdapter extends ArrayAdapter<DriveFolder> {
    private final LayoutInflater inflater;
    private String selectedId;

    WorkOrderListAdapter(Context context, List<DriveFolder> folders) {
        super(context, R.layout.row_work_order, folders);
        inflater = LayoutInflater.from(context);
    }

    void setSelectedId(String selectedId) {
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView == null
                ? inflater.inflate(R.layout.row_work_order, parent, false)
                : convertView;
        DriveFolder folder = getItem(position);
        TextView title = row.findViewById(R.id.work_order_row_title);
        TextView date = row.findViewById(R.id.work_order_row_date);
        TextView state = row.findViewById(R.id.work_order_row_state);
        if (folder == null) {
            title.setText("");
            date.setText("");
            state.setVisibility(View.GONE);
            return row;
        }
        String name = PropertyDisplayName.fromDriveFolderName(folder.name());
        int split = name.lastIndexOf(" - ");
        if (split > 0 && split + 3 < name.length()) {
            title.setText(name.substring(0, split));
            date.setText(name.substring(split + 3));
        } else {
            title.setText(name);
            date.setText("Existing work order");
        }
        boolean selected = selectedId != null && selectedId.equals(folder.id());
        row.setBackgroundResource(selected ? R.drawable.bg_concept_selected : R.drawable.bg_concept_card);
        state.setVisibility(selected ? View.VISIBLE : View.GONE);
        return row;
    }
}
