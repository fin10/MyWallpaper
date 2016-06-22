package com.fin10.android.mywallpaper;

import android.app.Application;

public final class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WallpaperModel.init(this);
    }
}
