package com.fin10.android.mywallpaper.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.settings.SettingsFragment;

import java.util.List;
import java.util.Random;

public final class WallpaperChangeScheduler extends BroadcastReceiver {

    public static void start(@NonNull Context context) {
        long interval = SettingsFragment.getInterval(context);
        Log.d("interval:%d", interval);
        long count = WallpaperModel.getCount();
        if (count <= 1) {
            Log.d("not need to repeat. size:%d", count);
            return;
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, createOperation(context));
    }

    public static void stop(@NonNull Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(createOperation(context));
    }

    @NonNull
    private static PendingIntent createOperation(@NonNull Context context) {
        Intent intent = new Intent(context, WallpaperChangeScheduler.class);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("action:%s", action);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            start(context);
        } else {
            List<WallpaperModel> models = WallpaperModel.getModels();
            Random random = new Random();
            int count = models.size();
            while (count > 1) {
                WallpaperModel model = models.get(random.nextInt(count));
                if (!WallpaperModel.isCurrentWallpaper(context, model)) {
                    model.setAsWallpaper(context);
                    break;
                }
            }
        }
    }
}
