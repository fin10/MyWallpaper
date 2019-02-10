package com.fin10.android.mywallpaper.model;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public final class WallpaperModelTest {

    private static final long TEST_ID = Long.MAX_VALUE;
    private static File TEST_IMAGE_FILE;

    private final Thread thread = Thread.currentThread();

    static {
        try {
            WallpaperDatabase.init(ApplicationProvider.getApplicationContext());
            TEST_IMAGE_FILE = File.createTempFile("tmp-", ".png");
        } catch (Exception e) {
            Assert.fail(e.getLocalizedMessage());
        }
    }

    @Before
    public void setUp() {
        EventBus.getDefault().register(this);
    }

    @After
    public void tearDown() {
        EventBus.getDefault().unregister(this);
        SQLite.delete(WallpaperModel.class)
                .where(WallpaperModel_Table._id.eq(TEST_ID))
                .execute();
    }

    @Test
    public void testAddModel() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        WallpaperModel model = WallpaperModel.addModel(context, TEST_ID, TEST_IMAGE_FILE, false);
        Assert.assertEquals(TEST_ID, model.getId());
        Assert.assertNotNull(model.getImagePath());

        try {
            thread.join(1000);
            Assert.fail("Not received add event.");
        } catch (InterruptedException e) {
            //
        }
    }

    @Test
    public void testGetModels() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        WallpaperModel model = WallpaperModel.addModel(context, TEST_ID, TEST_IMAGE_FILE, false);

        WallpaperModel m = WallpaperModel.getModel(model.getId());
        Assert.assertNotNull(m);
        Assert.assertEquals(model.getId(), m.getId());
        Assert.assertEquals(model.getImagePath(), m.getImagePath());
    }

    @Test
    public void testRemoveModel() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        WallpaperModel model = WallpaperModel.addModel(context, TEST_ID, TEST_IMAGE_FILE, false);
        WallpaperModel.removeModel(model);

        try {
            thread.join(1000);
            Assert.fail("Not received remove event.");
        } catch (InterruptedException e) {
            //
        }

        WallpaperModel removed = SQLite.select()
                .from(WallpaperModel.class)
                .where(WallpaperModel_Table._id.eq(model.getId()))
                .querySingle();
        Assert.assertNull(removed);
    }

    @Subscribe
    public void onAdded(@NonNull WallpaperModel.AddEvent event) {
        thread.interrupt();
    }

    @Subscribe
    public void onUpdated(@NonNull WallpaperModel.UpdateEvent event) {
        thread.interrupt();
    }

    @Subscribe
    public void onRemoved(@NonNull WallpaperModel.RemoveEvent event) {
        thread.interrupt();
    }
}
