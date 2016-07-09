package com.fin10.android.mywallpaper.settings;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.fin10.android.mywallpaper.R;

import java.util.HashSet;
import java.util.Set;

public final class PreferenceUtils {

    private PreferenceUtils() {
    }

    public static void setTutorialEnabled(@NonNull Context context, boolean enabled) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putBoolean(context.getString(R.string.pref_key_tutorial_enabled), enabled).apply();
    }

    public static boolean isTutorialEnabled(@NonNull Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(context.getString(R.string.pref_key_tutorial_enabled), true);
    }

    public static void setAutoChangeEnabled(@NonNull Context context, boolean enabled) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putBoolean(context.getString(R.string.pref_key_auto_change_enabled), enabled).apply();
    }

    public static boolean isSyncEnabled(@NonNull Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(context.getString(R.string.pref_key_sync_enabled), false);
    }

    public static boolean isAutoChangeEnabled(@NonNull Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(context.getString(R.string.pref_key_auto_change_enabled), false);
    }

    public static void setPeriod(@NonNull Context context, int period) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putInt(context.getString(R.string.pref_key_auto_change_period), period).apply();
    }

    public static int getPeriod(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getInt(context.getString(R.string.pref_key_auto_change_period), PeriodPreference.Period.USUALLY);
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

    public static void setRemovedModel(@NonNull Context context, @NonNull Set<String> items) {
        synchronized (PreferenceUtils.class) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            pref.edit().putStringSet(context.getString(R.string.pref_key_need_to_remove_items), items).apply();
        }
    }

    @NonNull
    public static Set<String> getRemovedModels(@NonNull Context context) {
        synchronized (PreferenceUtils.class) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> result = pref.getStringSet(context.getString(R.string.pref_key_need_to_remove_items), null);
            return result != null ? new HashSet<>(result) : new HashSet<String>();
        }
    }

    public static void clearRemovedModels(@NonNull Context context) {
        synchronized (PreferenceUtils.class) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            pref.edit().remove(context.getString(R.string.pref_key_need_to_remove_items)).apply();
        }
    }
}
