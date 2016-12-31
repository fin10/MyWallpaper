package com.fin10.android.mywallpaper.live;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.service.wallpaper.WallpaperService;
import android.support.annotation.NonNull;
import android.view.SurfaceHolder;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.fin10.android.mywallpaper.BuildConfig;
import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.model.WallpaperChanger;
import com.fin10.android.mywallpaper.model.WallpaperModel;
import com.fin10.android.mywallpaper.settings.PreferenceModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public final class LiveWallpaperService extends WallpaperService {

    private final BroadcastReceiver mReceiver = new WallpaperChanger.Receiver();

    @NonNull
    public static Intent getIntentForSetLiveWallpaper() {
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(BuildConfig.APPLICATION_ID, LiveWallpaperService.class.getName()));
        return intent;
    }

    @Override
    public void onCreate() {
        Log.d("enter");
        super.onCreate();
        registerReceiver(mReceiver, WallpaperChanger.Receiver.getIntentFilter());

        if (PreferenceModel.isAutoChangeEnabled(this)) {
            WallpaperChangeScheduler.start(this, PreferenceModel.getInterval(this));
        }
    }

    @Override
    public void onDestroy() {
        Log.d("enter");
        super.onDestroy();
        unregisterReceiver(mReceiver);
        WallpaperChangeScheduler.stop(this);
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends WallpaperService.Engine {

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            EventBus.getDefault().register(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            EventBus.getDefault().unregister(this);
        }

        @Override
        public void onSurfaceChanged(final SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            Log.d("enter");
            long id = PreferenceModel.getCurrentWallpaper(getBaseContext());
            updateWallpaper(getBaseContext(), holder, width, height, id);
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onWallpaperChanged(@NonNull WallpaperChanger.ChangeWallpaperEvent event) {
            Log.d("enter");
            SurfaceHolder holder = getSurfaceHolder();
            Rect frame = holder.getSurfaceFrame();
            updateWallpaper(getBaseContext(), holder, frame.width(), frame.height(), event.id);
        }

        private void updateWallpaper(@NonNull Context context, final SurfaceHolder holder, int width, final int height, long id) {
            Log.d("[%d:%d] %d", width, height, id);
            WallpaperModel model = WallpaperModel.getModel(id);
            if (model == null) {
                Log.e("[%d] Not found.", id);
                return;
            }

            Glide.with(context)
                    .load(model.getImagePath())
                    .asBitmap()
                    .centerCrop()
                    .into(new SimpleTarget<Bitmap>(width, height) {

                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            Canvas canvas = holder.lockCanvas();
                            if (canvas == null) {
                                Log.e("canvas is null.");
                            } else {
                                canvas.drawBitmap(resource, 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));
                                holder.unlockCanvasAndPost(canvas);
                            }
                        }
                    });
        }
    }
}
