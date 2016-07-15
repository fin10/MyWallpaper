package com.fin10.android.mywallpaper.model;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class WallpaperModelTest {

    private boolean initialized = false;

    @NonNull
    private static Bitmap createTestBitmap() {
        return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    }

    @Before
    public void setUp() throws Exception {
        if (!initialized) {
            WallpaperModel.init(InstrumentationRegistry.getTargetContext());
            initialized = true;
        }
    }

    @After
    public void tearDown() throws Exception {
        List<WallpaperModel> models = WallpaperModel.getModels();
        for (WallpaperModel model : models) {
            WallpaperModel.removeModel(model);
        }
    }

    @Test
    public void testAddModel() throws Exception {
        WallpaperModel model = WallpaperModel.addModel(InstrumentationRegistry.getTargetContext(), createTestBitmap());
        Assert.assertNotNull(model);
    }

    @Test
    public void testGetModels() throws Exception {
        WallpaperModel model = WallpaperModel.addModel(InstrumentationRegistry.getTargetContext(), createTestBitmap());
        Assert.assertNotNull(model);

        List<WallpaperModel> models = WallpaperModel.getLocalModels();
        Assert.assertEquals(model, models.get(0));
    }

    @Test
    public void testRemoveModel() throws Exception {
        WallpaperModel model = WallpaperModel.addModel(InstrumentationRegistry.getTargetContext(), createTestBitmap());
        Assert.assertNotNull(model);

        List<WallpaperModel> models = WallpaperModel.getLocalModels();
        Assert.assertEquals(model, models.get(0));

        WallpaperModel.removeModel(model);
        models = WallpaperModel.getLocalModels();
        Assert.assertEquals(0, models.size());
    }

    @Test
    public void testGetLocalModels() throws Exception {
        WallpaperModel model = WallpaperModel.addModel(InstrumentationRegistry.getTargetContext(), createTestBitmap());
        List<WallpaperModel> models = WallpaperModel.getLocalModels();
        Assert.assertEquals(model, models.get(0));
    }

    @Test
    public void testGetSyncedModels() throws Exception {
        WallpaperModel model = WallpaperModel.addModel(InstrumentationRegistry.getTargetContext(), createTestBitmap());
        List<WallpaperModel> models = WallpaperModel.getLocalModels();
        Assert.assertEquals(model, models.get(0));
        models = WallpaperModel.getSyncedModels();
        Assert.assertTrue(models.isEmpty());
    }
}
