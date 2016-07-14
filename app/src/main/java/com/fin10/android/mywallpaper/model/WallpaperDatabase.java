package com.fin10.android.mywallpaper.model;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.migration.BaseMigration;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

@Database(name = WallpaperDatabase.NAME, version = WallpaperDatabase.VERSION)
public final class WallpaperDatabase {

    public static final String NAME = "WallpaperDatabase";
    public static final int VERSION = 2;

    @Migration(version = 2, database = WallpaperDatabase.class)
    public static final class Version2Migration extends BaseMigration {

        @Override
        public void migrate(DatabaseWrapper database) {
            SQLite.update(WallpaperModel.class)
                    .set(WallpaperModel_Table.source.eq((String) null))
                    .execute(database);
        }
    }

}
