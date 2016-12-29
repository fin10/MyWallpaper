package com.fin10.android.mywallpaper.model;

import android.content.ContentValues;
import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.fin10.android.mywallpaper.Log;
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
        }
    }
}
