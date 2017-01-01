package com.fin10.android.mywallpaper.live;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.fin10.android.mywallpaper.model.WallpaperChanger;
import com.fin10.android.mywallpaper.model.WallpaperModel;
import com.fin10.android.mywallpaper.model.WallpaperModelTest;

import junit.framework.Assert;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WallpaperChangeSchedulerTest {

    private final BroadcastReceiver receiver = new WallpaperChanger.Receiver();
    private final Thread thread = Thread.currentThread();
    private final Context context = InstrumentationRegistry.getTargetContext();
    private final WallpaperModel model = WallpaperModelTest.createTestModel();

    @Before
    public void setUp() throws Exception {
        context.registerReceiver(receiver, WallpaperChanger.Receiver.getIntentFilter());
        EventBus.getDefault().register(this);
        model.insert();
    }

    @After
    public void tearDown() throws Exception {
        context.unregisterReceiver(receiver);
        EventBus.getDefault().unregister(this);
        model.delete();
    }

    @Test
    public void testOnReceive() throws Exception {
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
