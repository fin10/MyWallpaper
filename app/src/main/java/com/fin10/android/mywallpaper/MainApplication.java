package com.fin10.android.mywallpaper;

import android.app.Application;
import android.preference.PreferenceManager;

import com.fin10.android.mywallpaper.model.WallpaperDatabase;

public final class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        WallpaperDatabase.init(this);
    }
}
