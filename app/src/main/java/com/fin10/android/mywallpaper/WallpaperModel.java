package com.fin10.android.mywallpaper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

    private static void addModel(@NonNull String path) {
        Log.d("path:%s", path);
        WallpaperModel model = new WallpaperModel(path);

        synchronized (sListeners) {
            for (OnEventListener listener : sListeners) {
                listener.onAdded(model);
            }
        }
    }

    public static void removeModel(@NonNull WallpaperModel model) {
        File file = new File(model.getPath());
        boolean result = file.delete();
        Log.d("result:%b", result);

        synchronized (sListeners) {
            for (OnEventListener listener : sListeners) {
                listener.onRemoved(model);
            }
        }
    }

    @NonNull
    public String getPath() {
        return mPath;
    }

    public interface OnEventListener {
        void onAdded(@NonNull WallpaperModel model);

        void onRemoved(@NonNull WallpaperModel model);
    }

    public static final class DownloadService extends Service {

        public static void putWallpaper(@NonNull Context context, @NonNull Uri uri) {
            Intent i = new Intent(context, WallpaperModel.DownloadService.class);
            i.putExtra("uri", uri);
            context.startService(i);
        }

        @Override
        public void onCreate() {
            super.onCreate();
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (intent == null) {
                Log.e("[%d] the intent is null.", startId);
                return Service.START_NOT_STICKY;
            }

            Uri uri = intent.getParcelableExtra("uri");
            Log.d("uri:%s", uri);
            if (uri == null) {
                Log.e("[%d] the intent has no uri.", startId);
                return Service.START_NOT_STICKY;
            }

            Glide.with(this)
                    .load(uri)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(new SimpleTarget<Bitmap>() {

                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            FileOutputStream os = null;
                            try {
                                File file = new File(sPath, System.currentTimeMillis() + ".png");
                                Log.d("file:%s", file);
                                os = new FileOutputStream(file);
                                resource.compress(Bitmap.CompressFormat.PNG, 100, os);
                                os.flush();

                                WallpaperModel.addModel(file.getAbsolutePath());

                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    if (os != null) os.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);
                            e.printStackTrace();
                        }
                    });

            return Service.START_NOT_STICKY;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }
    }
}
