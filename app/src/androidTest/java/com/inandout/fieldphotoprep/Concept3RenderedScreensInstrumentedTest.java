package com.inandout.fieldphotoprep;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Actual production activities, adapters, image decoding and Android rendering.
 * Fixtures are isolated to instrumentation; no real provider is read or written.
 */
@RunWith(AndroidJUnit4.class)
public final class Concept3RenderedScreensInstrumentedTest {
    @Test
    public void renderBoundScreensAndVerifySelectionAndNavigation() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // This is a clean CI/emulator test app, never a physical field device.
        context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
        DriveFolder property = new DriveFolder("render-property", "1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST");
        DriveFolder work = new DriveFolder("render-work", "Pressure Test - 2026-09-13");
        FolderPrefs prefs = new FolderPrefs(context);
        prefs.setCurrentAddress(property);
        prefs.setCurrentWorkOrder(work);
        File fixtures = new File(context.getCacheDir(), "concept3-render-" + UUID.randomUUID());
        PendingPhotoStore store = new PendingPhotoStore(new File(fixtures, "pending"));
        PhotoPreparer preparer = new PhotoPreparer(new File(fixtures, "prepared"));
        List<PendingPhotoRecord> photos = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            PendingPhotoRecord photo = store.beginCapture(property, work);
            writeJpeg(store.imageFile(photo), i);
            photo = store.finishCaptureIfImageExists(photo.id());
            preparer.prepare(store, photo);
            if (i == 6) { store.beginUploadAttempt(photo.id()); store.markUploadFailed(photo.id(), "Test safe failure"); }
            if (i == 7) { store.beginUploadAttempt(photo.id()); store.markUploadUncertain(photo.id(), "Test uncertain outcome"); }
            photos.add(store.getById(photo.id()));
        }
        try (ActivityScenario<MainActivity> main = ActivityScenario.launch(MainActivity.class)) {
            main.onActivity(activity -> {
                try {
                    @SuppressWarnings("unchecked") List<DriveFolder> visible = (List<DriveFolder>) field(activity, "visibleFolders");
                    visible.clear();
                    visible.add(property);
                    visible.add(new DriveFolder("render-property-duplicate", property.name()));
                    String[] names = {"101_CHUCKER_LN_ELK_CITY_OK", "150_BLUESTEM_RD_WEATHERFORD_RD",
                            "120_S_BROADWAY_ST_SAYRE_OK_73662", "510_NE_CIMARRON_CIRCLE_LAWTON_OK",
                            "4831_SE_ELLSWORTH_AVE_LAWTON_OK", "2634_SW_H_AVE_LAWTON_OK",
                            "1112_SANTA_FE_DR_CLINTON_OK", "1607_CRESTVIEW_DR_CORDELL_OK"};
                    for (int i = 0; i < names.length; i++) visible.add(new DriveFolder("render-p-"+i, names[i]));
                    call(activity, "notifyFolderAdapters");
                    call(activity, "renderPropertyCountAndEmptyState");
                    ((TextView) activity.findViewById(R.id.home_master_name)).setText("HNP Jobs");
                    ((TextView) activity.findViewById(R.id.home_drive_state)).setText("Drive connected");
                    activity.findViewById(R.id.home_connect_button).setVisibility(View.GONE);
                    activity.findViewById(R.id.home_status_text).setVisibility(View.GONE);
                } catch (Exception e) { throw new AssertionError(e); }
            });
            screenshot("home");
            main.onActivity(activity -> {
                try {
                    ListView list = activity.findViewById(R.id.home_property_list);
                    assertTrue("Property list should show at least six rows at normal scale", list.getChildCount() >= 6);
                    assertFalse(((TextView)list.getChildAt(0).findViewById(R.id.property_name)).getText().toString().contains("PRESSURE TEST"));
                    assertEquals(View.VISIBLE, list.getChildAt(0).findViewById(R.id.property_disambiguator).getVisibility());
                    call(activity, "openAddress", new Class<?>[]{DriveFolder.class}, property);
                    @SuppressWarnings("unchecked") List<DriveFolder> visible = (List<DriveFolder>) field(activity, "visibleFolders");
                    visible.clear(); visible.add(work);
                    for (int i=1;i<8;i++) visible.add(new DriveFolder("render-w-"+i,"Cut Grass - 2026-09-" + (13-i)));
                    call(activity, "notifyFolderAdapters");
                    call(activity, "selectWorkOrder", new Class<?>[]{DriveFolder.class,String.class}, work,"Work order selected");
                    activity.findViewById(R.id.work_order_status).setVisibility(View.GONE);
                } catch (Exception e) { throw new AssertionError(e); }
            });
            screenshot("work-orders");
            main.onActivity(activity -> assertTrue(activity.findViewById(R.id.work_order_photos).isEnabled()));
            // Launch explicitly because ActivityScenario remains tied to MainActivity.
            try (ActivityScenario<PhotoCaptureActivity> photoScreen = ActivityScenario.launch(PhotoCaptureActivity.class)) {
                photoScreen.onActivity(activity -> {
                    try {
                        setField(activity,"photoStore",store); setField(activity,"photoPreparer",preparer);
                        call(activity,"renderPhotoList",new Class<?>[]{List.class,List.class},photos,new ArrayList<String>());
                        activity.findViewById(R.id.photos_status).setVisibility(View.GONE);
                        View row = ((android.widget.LinearLayout)activity.findViewById(R.id.photos_pending_list)).getChildAt(0);
                        assertNotNull(((ImageView)row.findViewById(R.id.photo_row_thumb)).getDrawable());
                        ((CheckBox)row.findViewById(R.id.photo_row_check)).setChecked(true);
                        assertEquals("Upload Selected (1)", ((Button)activity.findViewById(R.id.photos_upload_selected)).getText().toString());
                        View uncertain = ((android.widget.LinearLayout)activity.findViewById(R.id.photos_pending_list)).getChildAt(7);
                        assertFalse(uncertain.findViewById(R.id.photo_row_check).isEnabled());
                        assertEquals("render-work", store.getById(photos.get(0).id()).workOrderId());
                    } catch(Exception e) { throw new AssertionError(e); }
                });
                screenshot("photos");
                photoScreen.onActivity(activity -> activity.findViewById(R.id.nav_home).performClick());
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                main.onActivity(activity -> assertEquals(View.VISIBLE,activity.findViewById(R.id.home_root).getVisibility()));
            }
        } finally {
            context.getSharedPreferences("field_photo_prep", Context.MODE_PRIVATE).edit().clear().commit();
            delete(fixtures);
        }
    }

    private static void screenshot(String name) throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.waitForIdleSync();
        // UI traversal is asynchronous even after binding; a short settle allows image/list layout.
        Thread.sleep(400);
        Bitmap image = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(image);
        File directory = new File(instrumentation.getTargetContext().getFilesDir(),"concept3-screens");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream output=new FileOutputStream(new File(directory,InstrumentationRegistry.getArguments().getString("visualVariant", "light")+"-"+name+".png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,output));
        } finally { image.recycle(); }
    }
    private static Object field(Object owner,String name) throws Exception {
        Field field=owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static void setField(Object owner,String name,Object value) throws Exception {
        Field field=owner.getClass().getDeclaredField(name); field.setAccessible(true); field.set(owner,value);
    }
    private static void call(Object owner,String name) throws Exception { call(owner,name,new Class<?>[]{}); }
    private static void call(Object owner,String name,Class<?>[] types,Object... values) throws Exception {
        Method method=owner.getClass().getDeclaredMethod(name,types); method.setAccessible(true); method.invoke(owner,values);
    }
    private static void writeJpeg(File file,int i) throws Exception {
        Bitmap image=Bitmap.createBitmap(640,480,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(image); canvas.drawColor(Color.rgb(170+i*5,208,230));
        Paint paint=new Paint(); paint.setColor(Color.rgb(57,117+i*5,63)); canvas.drawRect(0,330,640,480,paint);
        paint.setColor(Color.rgb(219,198,167)); canvas.drawRect(110,160,530,340,paint);
        paint.setColor(Color.rgb(83,65,57)); canvas.drawRect(90,140,550,175,paint);
        paint.setColor(Color.rgb(48,83,113)); canvas.drawRect(160,200,245,260,paint); canvas.drawRect(380,200,465,260,paint);
        try(FileOutputStream output=new FileOutputStream(file)){ assertTrue(image.compress(Bitmap.CompressFormat.JPEG,90,output)); }
        finally { image.recycle(); }
    }
    private static void delete(File file) {
        if(file==null || !file.exists()) return;
        File[] children=file.listFiles(); if(children!=null) for(File child:children) delete(child);
        file.delete();
    }
}
