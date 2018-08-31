package com.fin10.android.mywallpaper.live;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.fin10.android.mywallpaper.model.WallpaperChanger;
import com.fin10.android.mywallpaper.model.WallpaperModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WallpaperChangeScheduler extends BroadcastReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(WallpaperChangeScheduler.class);

    public static void start(@NonNull Context context, long interval) {
        LOGGER.debug("interval:{}", interval);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, createOperation(context));
    }

    public static void stop(@NonNull Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(createOperation(context));
    }

    @NonNull
    private static PendingIntent createOperation(@NonNull Context context) {
        Intent intent = new Intent(context, WallpaperChangeScheduler.class);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!LiveWallpaperService.isSet(context)) {
            LOGGER.debug("Live wallpaper has not been set.");
            stop(context);
            return;
        }

        WallpaperModel model = WallpaperModel.sample();
        if (model != null) {
            WallpaperChanger.changeWallpaper(context, model.getId());
        } else {
            LOGGER.error("There is no wallpapers.");
        }
    }
}
