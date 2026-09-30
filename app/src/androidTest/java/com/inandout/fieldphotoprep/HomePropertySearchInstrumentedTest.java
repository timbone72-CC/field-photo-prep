package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.ListView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class HomePropertySearchInstrumentedTest {
    @Test
    public void normalHomeShowsActiveOnlyAndSearchIncludesArchived() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences folderPrefs =
                context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE);
        SharedPreferences lifecyclePrefs =
                context.getSharedPreferences(PropertyLifecycleStore.PREFS_NAME, Context.MODE_PRIVATE);

        folderPrefs.edit()
                .clear()
                .putString("master_tree_uri", "content://com.example.documents/tree/company")
                .putString("master_folder_id", "company")
                .putString("master_folder_name", "Test Company")
                .commit();
        lifecyclePrefs.edit().clear().commit();

        PropertyLifecycleStore lifecycle = new PropertyLifecycleStore(context);
        lifecycle.markWorkActivity("company", "active", 100L);
        lifecycle.setStateForTest("company", "archived", PropertyLifecycleStore.State.ARCHIVED);
        lifecycle.markWorkActivity("company", "archived", 200L);
        lifecycle.setStateForTest("company", "deleted", PropertyLifecycleStore.State.DELETED);

        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            FirstRunAuthTestHelper.dismissRequiredGateIfPresent();
            scenario.onActivity(activity -> {
                try {
                    Method setFolders = MainActivity.class.getDeclaredMethod(
                            "setDiscoveredPropertyFolders",
                            List.class);
                    setFolders.setAccessible(true);
                    setFolders.invoke(activity, List.of(
                            new DriveFolder("active", "100_MAIN_ST"),
                            new DriveFolder("archived", "200_MAIN_ST"),
                            new DriveFolder("deleted", "300_MAIN_ST")));

                    ListView list = activity.findViewById(R.id.home_property_list);
                    assertEquals(1, list.getAdapter().getCount());
                    assertEquals(
                            "active",
                            ((DriveFolder) list.getAdapter().getItem(0)).id());

                    EditText search = activity.findViewById(R.id.home_property_search);
                    search.setText("main");

                    assertEquals(2, list.getAdapter().getCount());
                    assertEquals(
                            "archived",
                            ((DriveFolder) list.getAdapter().getItem(0)).id());
                    assertEquals(
                            "active",
                            ((DriveFolder) list.getAdapter().getItem(1)).id());
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
        } finally {
            folderPrefs.edit().clear().commit();
            lifecyclePrefs.edit().clear().commit();
        }
    }
}
