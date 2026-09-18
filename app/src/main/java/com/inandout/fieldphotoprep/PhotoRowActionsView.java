package com.inandout.fieldphotoprep;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;

/**
 * Row-level doorway to the existing per-photo action owners.
 *
 * This view does not own photo state or Drive behavior. It first invokes the row's existing
 * selection handler, then exposes only the already-enabled action owners from
 * PhotoCaptureActivity. Those owner buttons may be visually hidden because the row popup is the
 * normal action surface.
 */
public final class PhotoRowActionsView extends ImageButton {
    private static final int[] ACTION_BUTTON_IDS = {
            R.id.photos_prepare,
            R.id.photos_upload_one,
            R.id.photos_reconcile,
            R.id.photos_discard
    };

    public PhotoRowActionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(view -> showActionsForRow());
    }

    private void showActionsForRow() {
        if (!(getParent() instanceof View)) {
            return;
        }
        View row = (View) getParent();
        if (!(row.getParent() instanceof ViewGroup)) {
            return;
        }

        View activityRoot = getRootView();
        if (!row.performClick()) {
            return;
        }

        PopupMenu menu = new PopupMenu(getContext(), this);
        int order = 0;
        for (int actionId : ACTION_BUTTON_IDS) {
            Button action = activityRoot.findViewById(actionId);
            if (action != null && action.isEnabled()) {
                menu.getMenu().add(0, actionId, order++, action.getText());
            }
        }

        if (menu.getMenu().size() == 0) {
            menu.getMenu().add("No actions available").setEnabled(false);
        }

        menu.setOnMenuItemClickListener(item -> {
            Button action = activityRoot.findViewById(item.getItemId());
            return action != null && action.isEnabled() && action.performClick();
        });
        menu.show();
    }
}
