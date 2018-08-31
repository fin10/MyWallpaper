package com.fin10.android.mywallpaper;

import android.app.Application;

import com.fin10.android.mywallpaper.model.WallpaperDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MainApplication extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainApplication.class);

    @Override
    public void onCreate() {
        super.onCreate();
        LOGGER.debug("application created.");
        WallpaperDatabase.init(this);
    }
}
