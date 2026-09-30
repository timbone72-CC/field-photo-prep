package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class PropertyArchiveReviewPrefsInstrumentedTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(PropertyArchiveReviewPrefs.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @After
    public void tearDown() {
        context.getSharedPreferences(PropertyArchiveReviewPrefs.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void defaultIsNinetyDaysAndSelectionPersists() {
        PropertyArchiveReviewPrefs first = new PropertyArchiveReviewPrefs(context);
        assertEquals(PropertyArchiveReviewPrefs.Threshold.DAYS_90, first.threshold());

        first.setThreshold(PropertyArchiveReviewPrefs.Threshold.MONTHS_6);

        PropertyArchiveReviewPrefs restored = new PropertyArchiveReviewPrefs(context);
        assertEquals(PropertyArchiveReviewPrefs.Threshold.MONTHS_6, restored.threshold());
    }
}
