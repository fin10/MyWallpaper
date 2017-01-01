package com.fin10.android.mywallpaper.model;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.live.LiveWallpaperService;
import com.fin10.android.mywallpaper.settings.PreferenceModel;
import com.fin10.android.mywallpaper.settings.PreferenceModel_Table;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.migration.BaseMigration;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

import java.util.Map;
import java.util.Set;

@Database(name = WallpaperDatabase.NAME, version = WallpaperDatabase.VERSION)
public final class WallpaperDatabase {

    static final String NAME = "WallpaperDatabase";
    static final int VERSION = 2;

    public static void init(@NonNull Context context) {
        FlowManager.init(new FlowConfig.Builder(context).build());
    }

    @Migration(version = 2, database = WallpaperDatabase.class)
    public static class Migration2 extends BaseMigration {

        @Override
        public void migrate(DatabaseWrapper database) {
            Log.d("DB Version: %d", database.getVersion());
            Context context = FlowManager.getContext();
            Map<String, ?> prefs = PreferenceManager.getDefaultSharedPreferences(context).getAll();
            Set<String> keySet = prefs.keySet();
            for (String key : keySet) {
                ContentValues values = new ContentValues();
                values.put(PreferenceModel_Table.key.toString(), key);
                values.put(PreferenceModel_Table.value.toString(), String.valueOf(prefs.get(key)));

                SQLite.insert(PreferenceModel.class)
                        .columnValues(values)
                        .execute(database);
            }
            Log.d("%d migration completed.", prefs.size());

            if (!LiveWallpaperService.isSet(context)) {
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                        LiveWallpaperService.getIntentForSetLiveWallpaper(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationManagerCompat manager = NotificationManagerCompat.from(context);
                manager.notify(0, new NotificationCompat.Builder(context)
                        .setColor(ActivityCompat.getColor(context, R.color.primary))
                        .setSmallIcon(R.drawable.ic_wallpaper_white_48dp)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(context.getString(R.string.live_wallpaper_needs_to_be_set)))
                        .setContentIntent(pendingIntent)
                        .addAction(R.drawable.ic_settings_white_24dp, context.getString(R.string.set), pendingIntent)
                        .setAutoCancel(true)
                        .build());
            }
        }
    }
}
