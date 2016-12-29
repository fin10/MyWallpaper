package com.fin10.android.mywallpaper.settings;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.fin10.android.mywallpaper.R;

public final class PreferenceUtils {

    private PreferenceUtils() {
    }

    public static void setTutorialEnabled(@NonNull Context context, boolean enabled) {
        String key = context.getString(R.string.pref_key_tutorial_enabled);
        PreferenceModel.setValue(key, Boolean.toString(enabled));
    }

    public static boolean isTutorialEnabled(@NonNull Context context) {
        String value = PreferenceModel.getValue(context.getString(R.string.pref_key_tutorial_enabled));
        return TextUtils.isEmpty(value) || Boolean.parseBoolean(value);
    }

    public static void setAutoChangeEnabled(@NonNull Context context, boolean enabled) {
        String key = context.getString(R.string.pref_key_auto_change_enabled);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putBoolean(key, enabled).apply();

        PreferenceModel.setValue(key, Boolean.toString(enabled));
    }

    public static boolean isSyncEnabled(@NonNull Context context) {
        String value = PreferenceModel.getValue(context.getString(R.string.pref_key_sync_enabled));
        return !TextUtils.isEmpty(value) && Boolean.parseBoolean(value);
    }

    public static boolean isAutoChangeEnabled(@NonNull Context context) {
        String value = PreferenceModel.getValue(context.getString(R.string.pref_key_auto_change_enabled));
        return !TextUtils.isEmpty(value) && Boolean.parseBoolean(value);
    }

    public static void setPeriod(@NonNull Context context, int period) {
        String key = context.getString(R.string.pref_key_auto_change_period);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putInt(key, period).apply();
        PreferenceModel.setValue(key, Integer.toString(period));
    }

    public static int getPeriod(Context context) {
        String value = PreferenceModel.getValue(context.getString(R.string.pref_key_auto_change_period));
        return TextUtils.isEmpty(value) ? PeriodPreference.Period.USUALLY : Integer.parseInt(value);
    }

    public static long getInterval(@NonNull Context context) {
        long interval = AlarmManager.INTERVAL_DAY;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        int period = pref.getInt(context.getString(R.string.pref_key_auto_change_period), PeriodPreference.Period.USUALLY);
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
