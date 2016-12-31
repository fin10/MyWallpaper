package com.fin10.android.mywallpaper.settings;

import android.app.AlarmManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.fin10.android.mywallpaper.R;
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

    static void setValue(@NonNull String key, @NonNull String value) {
        PreferenceModel model = new PreferenceModel();
        model.mKey = key;
        model.mValue = value;
        if (model.exists()) model.update();
        else model.insert();
    }

    @Nullable
    static String getValue(@NonNull String key) {
        PreferenceModel model = SQLite.select()
                .from(PreferenceModel.class)
                .where(PreferenceModel_Table.key.eq(key))
                .querySingle();

        return model != null ? model.mValue : null;
    }

    public static long getCurrentWallpaper(@NonNull Context context) {
        String value = PreferenceModel.getValue(context.getString(R.string.pref_key_current_wallpaper_id));
        return TextUtils.isEmpty(value) ? -1 : Long.parseLong(value);
    }

    public static void setCurrentWallpaper(@NonNull Context context, long id) {
        String key = context.getString(R.string.pref_key_current_wallpaper_id);
        PreferenceModel.setValue(key, Long.toString(id));
    }

    public static void setTutorialEnabled(@NonNull Context context, boolean enabled) {
        PreferenceModel.setValue(context.getString(R.string.pref_key_tutorial_enabled), Boolean.toString(enabled));
    }

    public static boolean isTutorialEnabled(@NonNull Context context) {
        String value = PreferenceModel.getValue(context.getString(R.string.pref_key_tutorial_enabled));
        return TextUtils.isEmpty(value) || Boolean.parseBoolean(value);
    }

    public static void setAutoChangeEnabled(@NonNull Context context, boolean enabled) {
        PreferenceModel.setValue(context.getString(R.string.pref_key_auto_change_enabled), Boolean.toString(enabled));
    }

    public static boolean isAutoChangeEnabled(@NonNull Context context) {
        String value = PreferenceModel.getValue(context.getString(R.string.pref_key_auto_change_enabled));
        return !TextUtils.isEmpty(value) && Boolean.parseBoolean(value);
    }

    public static void setSyncEnabled(@NonNull Context context, boolean enabled) {
        PreferenceModel.setValue(context.getString(R.string.pref_key_sync_enabled), Boolean.toString(enabled));
    }

    public static boolean isSyncEnabled(@NonNull Context context) {
        String value = PreferenceModel.getValue(context.getString(R.string.pref_key_sync_enabled));
        return !TextUtils.isEmpty(value) && Boolean.parseBoolean(value);
    }

    public static void setPeriod(@NonNull Context context, int period) {
        PreferenceModel.setValue(context.getString(R.string.pref_key_auto_change_period), Integer.toString(period));
    }

    public static int getPeriod(Context context) {
        String value = PreferenceModel.getValue(context.getString(R.string.pref_key_auto_change_period));
        return TextUtils.isEmpty(value) ? PeriodPreference.Period.USUALLY : Integer.parseInt(value);
    }

    public static long getInterval(@NonNull Context context) {
        long interval = AlarmManager.INTERVAL_DAY;
        int period = getPeriod(context);
        switch (period) {
            case PeriodPreference.Period.SOMETIMES:
                interval = 3 * AlarmManager.INTERVAL_DAY;
                break;
            case PeriodPreference.Period.USUALLY:
                interval = AlarmManager.INTERVAL_DAY;
                break;
            case PeriodPreference.Period.FREQUENTLY:
                interval = 3 * AlarmManager.INTERVAL_HOUR;
                break;
        }

        return interval;
    }
}
