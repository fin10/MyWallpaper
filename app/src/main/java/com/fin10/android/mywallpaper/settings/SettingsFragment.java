package com.fin10.android.mywallpaper.settings;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.model.WallpaperModel;

public final class SettingsFragment extends PreferenceFragment {

    public static boolean isAutoChangeEnabled(@NonNull Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(context.getString(R.string.pref_key_auto_change_enabled), false);
    }

    public static long getInterval(@NonNull Context context) {
        long interval = 24 * 60 * 60 * 1000;
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
                interval = AlarmManager.INTERVAL_HALF_DAY;
                break;
        }

        return interval;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.settings, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Context context = getActivity();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean autoChangeEnabled = pref.getBoolean(getString(R.string.pref_key_auto_change_enabled), false);
        if (autoChangeEnabled) {
            WallpaperModel.AlarmReceiver.start(context);
        } else {
            WallpaperModel.AlarmReceiver.stop(context);
        }
    }
}
