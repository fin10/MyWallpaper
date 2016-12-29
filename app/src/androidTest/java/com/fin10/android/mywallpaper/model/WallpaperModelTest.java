package com.fin10.android.mywallpaper.model;

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

    @Before
    public void setUp() throws Exception {
        if (!initialized) {
            WallpaperDatabase.init(InstrumentationRegistry.getTargetContext());
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
        WallpaperModel model = WallpaperModel.addModel(WallpaperModel.UserId.DEVICE, "test", "test.png");
        Assert.assertNotNull(model);
    }

    @Test
    public void testGetModels() throws Exception {
        WallpaperModel model = WallpaperModel.addModel(WallpaperModel.UserId.DEVICE, "test", "test.png");
        Assert.assertNotNull(model);

        List<WallpaperModel> models = WallpaperModel.getModels(WallpaperModel.UserId.DEVICE);
        Assert.assertEquals(model, models.get(0));
    }

    @Test
    public void testRemoveModel() throws Exception {
        WallpaperModel model = WallpaperModel.addModel(WallpaperModel.UserId.DEVICE, "test", "test.png");
        Assert.assertNotNull(model);

        List<WallpaperModel> models = WallpaperModel.getModels(WallpaperModel.UserId.DEVICE);
        Assert.assertEquals(model, models.get(0));

        WallpaperModel.removeModel(model);
        models = WallpaperModel.getModels(WallpaperModel.UserId.DEVICE);
        Assert.assertEquals(0, models.size());
    }

    @Test
    public void testGetUserIdModels() throws Exception {
        WallpaperModel model = WallpaperModel.addModel(WallpaperModel.UserId.DEVICE, "test", "test.png");
        List<WallpaperModel> models = WallpaperModel.getModels(WallpaperModel.UserId.DEVICE);
        Assert.assertEquals(model, models.get(0));
    }
}
