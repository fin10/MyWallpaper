package com.fin10.android.mywallpaper.live;

import android.content.BroadcastReceiver;
import android.content.Context;

import com.fin10.android.mywallpaper.model.WallpaperChanger;
import com.fin10.android.mywallpaper.model.WallpaperModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class WallpaperChangeSchedulerTest {

    private final BroadcastReceiver receiver = new WallpaperChanger.Receiver();
    private final Thread thread = Thread.currentThread();
    private WallpaperModel model;

    @Before
    public void setUp() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        context.registerReceiver(receiver, WallpaperChanger.Receiver.getIntentFilter());
        EventBus.getDefault().register(this);
        model = WallpaperModel.addModel(context, new FileInputStream(Files.createTempFile("tmp-", ".png").toFile()));
    }

    @After
    public void tearDown() {
        Context context = ApplicationProvider.getApplicationContext();
        context.unregisterReceiver(receiver);
        EventBus.getDefault().unregister(this);
        WallpaperModel.removeModel(model);
    }

    @Test
    public void testOnReceive() {
        Context context = ApplicationProvider.getApplicationContext();
        new WallpaperChangeScheduler().onReceive(context, null);

        try {
            thread.join(1000);
            Assert.fail("Not received wallpaper changed event.");
        } catch (InterruptedException e) {
            //
        }
    }

    @Subscribe
    public void onWallpaperChanged(@NonNull WallpaperChanger.ChangeWallpaperEvent event) {
        thread.interrupt();
    }
}
