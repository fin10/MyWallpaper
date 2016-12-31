package com.fin10.android.mywallpaper.live;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.fin10.android.mywallpaper.BuildConfig;
import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.model.WallpaperChanger;
import com.fin10.android.mywallpaper.model.WallpaperModel;

public final class WallpaperChangeScheduler extends BroadcastReceiver {

    public static void start(@NonNull Context context, long interval) {
        Log.d("interval:%d", interval);
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
        WallpaperInfo info = WallpaperManager.getInstance(context).getWallpaperInfo();
        if (info == null || !BuildConfig.APPLICATION_ID.equals(info.getPackageName())) {
            Log.d("Live wallpaper has not been set.");
            stop(context);
            return;
        }

        WallpaperModel model = WallpaperModel.sample();
        if (model != null) {
            WallpaperChanger.changeWallpaper(context, model.getId());
        } else {
            Log.e("There is no wallpapers.");
        }
    }
}
