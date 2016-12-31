package com.fin10.android.mywallpaper;

import android.app.Application;

import com.fin10.android.mywallpaper.model.WallpaperDatabase;

public final class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WallpaperDatabase.init(this);
    }
}
