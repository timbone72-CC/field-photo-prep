package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;

@RunWith(AndroidJUnit4.class)
public final class MultiCompanyDeviceFindingRegressionInstrumentedTest {
    private static final String PREFS = "field_photo_prep";

    @Test
    public void companySwitchClearsNewWorkOrderDraftNameAndDate() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        raw.edit()
                .clear()
                .putString("workspace_tree_uri", "content://com.example.documents/tree/photos")
                .putString("workspace_folder_id", "workspace")
                .putString("workspace_folder_name", "Photos")
                .putString("current_company_folder_id", "company-b")
                .putString("current_company_folder_name", "TEST COMPANY B")
                .commit();

        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    EditText input = activity.findViewById(R.id.work_order_name_input);
                    input.setText("TEST SECURE");

                    Field dateField = MainActivity.class.getDeclaredField("selectedDate");
                    dateField.setAccessible(true);
                    dateField.set(activity, LocalDate.of(2020, 1, 2));

                    Method selectCompany = MainActivity.class.getDeclaredMethod(
                            "selectCompany", DriveFolder.class);
                    selectCompany.setAccessible(true);
                    selectCompany.invoke(activity, new DriveFolder("company-a", "TEST COMPANY A"));

                    assertEquals("", input.getText().toString());
                    assertEquals(LocalDate.now(), dateField.get(activity));
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            });
        } finally {
            raw.edit().clear().commit();
        }
    }

    @Test
    public void mainActivitySuccessAndErrorMessagesUseDifferentSemanticTones() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();

        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    TextView status = activity.findViewById(R.id.home_status_text);

                    Method success = MainActivity.class.getDeclaredMethod(
                            "showHomeSuccessMessage", String.class);
                    success.setAccessible(true);
                    success.invoke(activity, "Company renamed.");
                    assertEquals(
                            ContextCompat.getColor(activity, R.color.home_primary_dark),
                            status.getCurrentTextColor());

                    Method error = MainActivity.class.getDeclaredMethod(
                            "showError", String.class, Throwable.class);
                    error.setAccessible(true);
                    error.invoke(activity, "Could not rename", new IllegalStateException("test"));
                    assertEquals(
                            ContextCompat.getColor(activity, R.color.home_error),
                            status.getCurrentTextColor());
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
        }
    }

    @Test
    public void photoActivitySuccessAndErrorMessagesUseDifferentSemanticTones() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();

        Intent intent = new Intent(context, PhotoCaptureActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try (ActivityScenario<PhotoCaptureActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    TextView status = activity.findViewById(R.id.photos_status);

                    Method success = PhotoCaptureActivity.class.getDeclaredMethod(
                            "showPhotoSuccess", String.class);
                    success.setAccessible(true);
                    success.invoke(activity, "Upload confirmed.");
                    assertEquals(
                            ContextCompat.getColor(activity, R.color.home_primary_dark),
                            status.getCurrentTextColor());

                    Method error = PhotoCaptureActivity.class.getDeclaredMethod(
                            "showError", String.class, Throwable.class);
                    error.setAccessible(true);
                    error.invoke(activity, "Upload failed", new IllegalStateException("test"));
                    assertEquals(
                            ContextCompat.getColor(activity, R.color.home_error),
                            status.getCurrentTextColor());

                    assertTrue(status.getVisibility() == TextView.VISIBLE);
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
        }
    }
}
