package com.fin10.android.mywallpaper.live;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fin10.android.mywallpaper.model.WallpaperChanger;
import com.fin10.android.mywallpaper.model.WallpaperModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;

public final class WallpaperChangeScheduler extends BroadcastReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(WallpaperChangeScheduler.class);

    public static void start(@NonNull Context context, long interval) {
        LOGGER.info("Starting wallpaper change scheduler with {} intervals.", interval);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null)
            am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + interval, interval, createOperation(context));
        else LOGGER.error("Unable to get AlarmManager");
    }

    public static void stop(@NonNull Context context) {
        LOGGER.info("Stopping wallpaper change scheduler.");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(createOperation(context));
        else LOGGER.error("Unable to get AlarmManager");
    }

    @NonNull
    private static PendingIntent createOperation(@NonNull Context context) {
        Intent intent = new Intent(context, WallpaperChangeScheduler.class);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LOGGER.info("Wallpaper change event received.");
        if (!LiveWallpaperService.isSet(context)) {
            LOGGER.error("Live wallpaper has not been set.");
            stop(context);
            return;
        }

        WallpaperModel model = WallpaperModel.sample();
        if (model != null) {
            WallpaperChanger.change(context, model.getId());
        } else {
            LOGGER.error("There is no wallpapers.");
        }
    }
}
