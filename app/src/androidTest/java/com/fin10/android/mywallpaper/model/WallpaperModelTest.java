package com.fin10.android.mywallpaper.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class WallpaperModelTest {

    private static final long TEST_ID = Long.MAX_VALUE;
    private static final long TEST_CREATION_TIME = 123;
    private static final String TEST_USER_ID = "_test_user_id";
    private static final String TEST_SOURCE = "_test";
    private static final String TEST_IMAGE_PATH = "_test.png";

    private final Thread thread = Thread.currentThread();
    private final Context context = InstrumentationRegistry.getTargetContext();
    private boolean initialized = false;

    @NonNull
    static WallpaperModel createTestModel() {
        WallpaperModel model = new WallpaperModel();
        model.mId = TEST_ID;
        model.mUserId = TEST_USER_ID;
        model.mSource = TEST_SOURCE;
        model.mImagePath = TEST_IMAGE_PATH;
        model.mCreationTime = TEST_CREATION_TIME;
        model.mAppliedCount = 0;
        return model;
    }

    @Before
    public void setUp() throws Exception {
        EventBus.getDefault().register(this);
        if (!initialized) {
            WallpaperDatabase.init(InstrumentationRegistry.getTargetContext());
            initialized = true;
        }
    }

    @After
    public void tearDown() throws Exception {
        EventBus.getDefault().unregister(this);
        SQLite.delete(WallpaperModel.class)
                .where(WallpaperModel_Table.user_id.eq(TEST_USER_ID))
                .execute();
    }

    @Test
    public void testAddModelWithPath() throws Exception {
        WallpaperModel model = WallpaperModel.addModel(TEST_USER_ID, TEST_SOURCE, TEST_IMAGE_PATH);
        Assert.assertEquals(TEST_USER_ID, model.mUserId);
        Assert.assertEquals(TEST_SOURCE, model.mSource);
        Assert.assertEquals(TEST_IMAGE_PATH, model.mImagePath);

        try {
            thread.join(1000);
            Assert.fail("Not received add event.");
        } catch (InterruptedException e) {
            //
        }
    }

    @Test
    public void testAddModelWithFile() throws Exception {
        File file = File.createTempFile("_test", ".tmp");
        WallpaperModel model = WallpaperModel.addModel(context, TEST_SOURCE, file);
        Assert.assertEquals(TEST_SOURCE, model.mSource);

        try {
            thread.join(1000);
            Assert.fail("Not received add event.");
        } catch (InterruptedException e) {
            //
        }
    }

    @Test
    public void testGetModels() throws Exception {
        WallpaperModel model = createTestModel();
        model.insert();

        List<WallpaperModel> models = WallpaperModel.getModels();
        Assert.assertFalse(models.isEmpty());
        for (WallpaperModel m : models) {
            if (m.mUserId.equals(TEST_USER_ID)) {
                Assert.assertEquals(model.mId, m.mId);
                Assert.assertEquals(model.mUserId, m.mUserId);
                Assert.assertEquals(model.mSource, m.mSource);
                Assert.assertEquals(model.mImagePath, m.mImagePath);
                Assert.assertEquals(model.mCreationTime, m.mCreationTime);
                return;
            }
        }

        Assert.fail("Not found.");
    }

    @Test
    public void testGetModelsWithUserId() throws Exception {
        WallpaperModel model = createTestModel();
        model.insert();

        List<WallpaperModel> models = WallpaperModel.getModels(TEST_USER_ID);
        Assert.assertEquals(model.mId, models.get(0).mId);
        Assert.assertEquals(model.mSource, models.get(0).mSource);
        Assert.assertEquals(model.mImagePath, models.get(0).mImagePath);
        Assert.assertEquals(model.mCreationTime, models.get(0).mCreationTime);
        Assert.assertEquals(TEST_USER_ID, models.get(0).mUserId);
    }

    @Test
    public void testRemoveModel() throws Exception {
        File file = File.createTempFile("_test", ".tmp");
        WallpaperModel model = createTestModel();
        model.mImagePath = file.getAbsolutePath();
        model.insert();

        WallpaperModel.removeModel(model);

        try {
            thread.join(1000);
            Assert.fail("Not received remove event.");
        } catch (InterruptedException e) {
            //
        }

        WallpaperModel removed = SQLite.select()
                .from(WallpaperModel.class)
                .where(WallpaperModel_Table._id.eq(model.mId))
                .querySingle();
        Assert.assertNull(removed);
    }

    @Test
    public void testIncrementAppliedCount() throws Exception {
        WallpaperModel model = createTestModel();
        model.insert();
        long beforeCount = model.mAppliedCount;
        model.incrementAppliedCount();

        try {
            thread.join(1000);
            Assert.fail("Not received update event.");
        } catch (InterruptedException e) {
            //
        }

        Assert.assertEquals(beforeCount + 1, model.mAppliedCount);
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
