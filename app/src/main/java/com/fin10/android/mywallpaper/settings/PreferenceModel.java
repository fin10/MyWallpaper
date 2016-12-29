package com.fin10.android.mywallpaper.settings;

import android.support.annotation.NonNull;

import com.fin10.android.mywallpaper.model.WallpaperDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = WallpaperDatabase.class)
public final class PreferenceModel extends BaseModel {

    @Column(name = "key")
    @PrimaryKey
    String mKey;

    @Column(name = "value")
    String mValue;

    public static void setValue(@NonNull String key, @NonNull String value) {
        PreferenceModel model = new PreferenceModel();
        model.mKey = key;
        model.mValue = value;
        if (model.exists()) model.update();
        else model.insert();
    }

    @NonNull
    public static String getValue(@NonNull String key) {
        PreferenceModel model = SQLite.select()
                .from(PreferenceModel.class)
                .where(PreferenceModel_Table.key.eq(key))
                .querySingle();

        return model != null ? model.mValue : "";
    }
}
