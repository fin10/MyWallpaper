package com.fin10.android.mywallpaper.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.settings.SettingsFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public final class WallpaperModel {

    private static final List<OnEventListener> sListeners = new LinkedList<>();
    private static String sPath;
    private final String mPath;

    private WallpaperModel(@NonNull String path) {
        mPath = path;
    }

    public static void init(@NonNull Context context) {
        sPath = context.getFilesDir() + "/wallpapers/";
        File file = new File(sPath);
        if (!file.exists()) file.mkdir();
    }

    public static void addEventListener(@NonNull OnEventListener listener) {
        synchronized (sListeners) {
            if (!sListeners.contains(listener)) {
                sListeners.add(listener);
            }
        }
    }

    public static void removeEventListener(@NonNull OnEventListener listener) {
        synchronized (sListeners) {
            sListeners.remove(listener);
        }
    }

    public static boolean isCurrentWallpaper(@NonNull Context context, @NonNull WallpaperModel model) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String path = pref.getString("current_wallpaper", "");

        return TextUtils.equals(path, model.getPath());
    }

    private static int getCount() {
        return getModels().size();
    }

    @NonNull
    public static List<WallpaperModel> getModels() {
        List<WallpaperModel> models = new ArrayList<>();
        File file = new File(sPath);
        File[] files = file.listFiles();
        for (File f : files) {
            models.add(new WallpaperModel(f.getAbsolutePath()));
        }

        return models;
    }

    static boolean addModel(@NonNull Context context, @NonNull Bitmap bitmap) {
        FileOutputStream os = null;
        try {
            File file = new File(sPath, System.currentTimeMillis() + ".png");
            Log.d("file:%s", file);
            os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();

            int count = getCount();
            if (count == 2 && SettingsFragment.isAutoChangeEnabled(context)) {
                AlarmReceiver.start(context);
            }

            WallpaperModel model = new WallpaperModel(file.getAbsolutePath());
            synchronized (sListeners) {
                for (OnEventListener listener : sListeners) {
                    listener.onAdded(model);
                }
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void removeModel(@NonNull final WallpaperModel model) {
        File file = new File(model.getPath());
        boolean result = file.delete();
        if (!result) {
            Log.e("failed to delete. %s", model.getPath());
            return;
        }

        Handler handler = new Handler();
        synchronized (sListeners) {
            for (final OnEventListener listener : sListeners) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onRemoved(model);
                    }
                });
            }
        }
    }

    public void setAsWallpaper(@NonNull Context context) {
        InputStream is = null;
        try {
            is = new FileInputStream(mPath);
            WallpaperManager.getInstance(context).setStream(is);

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            pref.edit().putString("current_wallpaper", mPath).apply();

            synchronized (sListeners) {
                for (OnEventListener listener : sListeners) {
                    listener.onWallpaperChanged(this);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    public String getPath() {
        return mPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WallpaperModel model = (WallpaperModel) o;

        return mPath.equals(model.mPath);

    }

    @Override
    public int hashCode() {
        return mPath.hashCode();
    }

    public interface OnEventListener {
        void onAdded(@NonNull WallpaperModel model);

        void onRemoved(@NonNull WallpaperModel model);

        void onWallpaperChanged(@NonNull WallpaperModel model);
    }

    public static final class AlarmReceiver extends BroadcastReceiver {

        public static void start(@NonNull Context context) {
            long interval = SettingsFragment.getInterval(context);
            Log.d("interval:%d", interval);
            int count = WallpaperModel.getCount();
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
            Intent intent = new Intent(context, AlarmReceiver.class);
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
}
