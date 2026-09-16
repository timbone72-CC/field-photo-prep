package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class PropertyListAdapterAmbiguityInstrumentedTest {
    @Test
    public void ambiguousRowsShowRawProviderNameAndCompactIdentity() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        List<DriveFolder> folders = new ArrayList<>();
        DriveFolder first = new DriveFolder(
                "1YFAx3V-4Ce1WXbK_zD1SQzaQ-j2lVgcJ",
                "509_W_SOUTH_BOUNDARY_WALTERS_OK");
        DriveFolder second = new DriveFolder(
                "16h_YZITReuc6f896RP4PY0bk89nzWMIx",
                "509 SOUTH BOUNDARY WALTERS OK");
        folders.add(first);
        folders.add(second);

        PropertyListAdapter adapter = new PropertyListAdapter(context, folders);
        View row = adapter.getView(0, null, new FrameLayout(context));
        TextView name = row.findViewById(R.id.property_name);
        TextView disambiguator = row.findViewById(R.id.property_disambiguator);

        assertEquals(first.name(), name.getText().toString());
        assertEquals(View.VISIBLE, disambiguator.getVisibility());
        assertTrue(disambiguator.getText().toString().contains("Possible duplicate"));
        assertTrue(disambiguator.getText().toString().contains("ID …-j2lVgcJ"));
        assertEquals(1, disambiguator.getMaxLines());
    }
}
