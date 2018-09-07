package com.fin10.android.mywallpaper.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.fin10.android.mywallpaper.settings.PreferenceModel;
import com.fin10.android.mywallpaper.settings.PreferenceModel_Table;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.migration.BaseMigration;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Database(name = WallpaperDatabase.NAME, version = WallpaperDatabase.VERSION)
public final class WallpaperDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(WallpaperDatabase.class);

    static final String NAME = "WallpaperDatabase";
    static final int VERSION = 3;

    public static void init(@NonNull Context context) {
        FlowManager.init(new FlowConfig.Builder(context).build());
    }

    @Migration(version = 2, database = WallpaperDatabase.class)
    public static class Migration2 extends BaseMigration {

        @Override
        public void migrate(@NonNull DatabaseWrapper database) {
            LOGGER.info("Starting DB migration version {} to 2", database.getVersion());
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
            LOGGER.info("DB migration completed.");
        }
    }

    @Migration(version = 3, database = WallpaperDatabase.class)
    public static class Migration3 extends BaseMigration {

        @Override
        public void migrate(@NonNull DatabaseWrapper database) {
            LOGGER.info("Starting DB migration version {} to 3", database.getVersion());

            final List<WallpaperModel> models = new ArrayList<>();
            final String tableName = FlowManager.getTableName(WallpaperModel.class);
            try (Cursor cursor = database.rawQuery("SELECT creation_time, image_path from " + tableName, null)) {
                LOGGER.info("There are {} items.", cursor.getCount());
                while (cursor.moveToNext()) {
                    WallpaperModel model = new WallpaperModel();
                    model.setId(cursor.getLong(0));
                    model.setImagePath(cursor.getString(1));
                    models.add(model);
                }
            }

            database.execSQL("DROP TABLE " + tableName);
            database.execSQL("CREATE TABLE " + tableName +
                    "( _id INTEGER PRIMARY KEY, image_path TEXT NOT NULL, synced INTEGER DEFAULT 0 )");
            models.forEach(model -> model.save(database));

            LOGGER.info("DB migration completed.");
        }
    }
}
