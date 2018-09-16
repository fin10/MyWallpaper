package com.fin10.android.mywallpaper.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.fin10.android.mywallpaper.settings.PreferenceModel;

import junit.framework.Assert;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;

@RunWith(AndroidJUnit4.class)
public final class WallpaperChangerTest {

    private final BroadcastReceiver receiver = new WallpaperChanger.Receiver();
    private final Thread thread = Thread.currentThread();
    private WallpaperModel model;

    @Before
    public void setUp() throws IOException {
        Context context = InstrumentationRegistry.getTargetContext();
        context.registerReceiver(receiver, WallpaperChanger.Receiver.getIntentFilter());
        EventBus.getDefault().register(this);
        model = WallpaperModel.addModel(context, Files.createTempFile("tmp-", ".png").toFile());
    }

    @After
    public void tearDown() {
        Context context = InstrumentationRegistry.getTargetContext();
        context.unregisterReceiver(receiver);
        EventBus.getDefault().unregister(this);
        WallpaperModel.removeModel(model);
    }

    @Test
    public void testChangeWallpaper() {
        Context context = InstrumentationRegistry.getTargetContext();
        WallpaperChanger.change(context, model.getId());

        try {
            thread.join(1000);
            Assert.fail("Not received wallpaper changed event.");
        } catch (InterruptedException e) {
            //
        }

        Assert.assertEquals(model.getId(), PreferenceModel.getCurrentWallpaper(context));
    }

    @Subscribe
    public void onWallpaperChanged(@NonNull WallpaperChanger.ChangeWallpaperEvent event) {
        thread.interrupt();
    }
}
