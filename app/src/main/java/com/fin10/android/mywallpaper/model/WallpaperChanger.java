package com.fin10.android.mywallpaper.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.fin10.android.mywallpaper.BuildConfig;
import com.fin10.android.mywallpaper.settings.PreferenceModel;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WallpaperChanger {

    private static final Logger LOGGER = LoggerFactory.getLogger(WallpaperChanger.class);

    public static boolean changeWallpaper(@NonNull final Context context, long id) {
        final WallpaperModel model = WallpaperModel.getModel(id);
        if (model == null) {
            LOGGER.error("{} not founds.", id);
            return false;
        }

        LOGGER.debug("id:{}", id);
        PreferenceModel.setCurrentWallpaper(context, model.getId());

        Intent intent = new Intent(Receiver.ACTION_WALLPAPER_CHANGED);
        intent.putExtra(Receiver.EXTRA_ID, model.getId());
        context.sendBroadcast(intent);

        return true;
    }

    public static final class Receiver extends BroadcastReceiver {

        private static final String ACTION_WALLPAPER_CHANGED = BuildConfig.APPLICATION_ID + ".action.wallpaper_changed";
        private static final String EXTRA_ID = BuildConfig.APPLICATION_ID + ".extra.id";

        @NonNull
        public static IntentFilter getIntentFilter() {
            return new IntentFilter(ACTION_WALLPAPER_CHANGED);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            LOGGER.debug("action:{}, id:{}", intent.getAction(), intent.getLongExtra(EXTRA_ID, -1));
            if (ACTION_WALLPAPER_CHANGED.equals(intent.getAction())) {
                EventBus.getDefault().post(new WallpaperChanger.ChangeWallpaperEvent(intent.getLongExtra(EXTRA_ID, -1)));
            }
        }
    }

    public static final class ChangeWallpaperEvent {

        public final long id;

        private ChangeWallpaperEvent(long id) {
            this.id = id;
        }
    }
}
