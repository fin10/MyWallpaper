package com.fin10.android.mywallpaper;

import android.app.Application;

import com.fin10.android.mywallpaper.model.WallpaperModel;

public final class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WallpaperModel.init(this);
    }
}
