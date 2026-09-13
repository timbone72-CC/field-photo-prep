package com.inandout.fieldphotoprep;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/** Work-order rows scroll with the surrounding form; short lists leave no empty reservation. */
public final class ContentHeightListView extends ListView {
    public ContentHeightListView(Context context, AttributeSet attributes) {
        super(context, attributes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST));
    }
}
