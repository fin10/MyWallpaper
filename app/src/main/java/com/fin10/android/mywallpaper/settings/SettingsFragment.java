package com.fin10.android.mywallpaper.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.model.SyncScheduler;

public final class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final int REQUEST_CODE_LOGIN = 1;
    private static final int REQUEST_CODE_LOGOUT = 2;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        findPreference(getString(R.string.pref_key_auto_change_enabled)).setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.pref_key_auto_change_period)).setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.pref_key_sync_enabled)).setOnPreferenceChangeListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("[onActivityResult] requestCode:%d, resultCode:%d", requestCode, resultCode);
        switch (requestCode) {
            case REQUEST_CODE_LOGIN: {
                if (resultCode == Activity.RESULT_OK) {
                    SyncScheduler.start(getActivity());
                } else {
                    SwitchPreference pref = (SwitchPreference) findPreference(getString(R.string.pref_key_sync_enabled));
                    pref.setChecked(false);
                }
                break;
            }
            case REQUEST_CODE_LOGOUT: {
                SyncScheduler.stop(getActivity());
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("%s, newValue:%s", preference, newValue);
        String key = preference.getKey();
        if (TextUtils.equals(key, getString(R.string.pref_key_auto_change_enabled))) {
            boolean value = (boolean) newValue;
            if (value) WallpaperChangeScheduler.start(getActivity(), SettingsFragment.getInterval(getActivity()));
            else WallpaperChangeScheduler.stop(getActivity());
        } else if (TextUtils.equals(key, getString(R.string.pref_key_auto_change_period))) {
            WallpaperChangeScheduler.stop(getActivity());
            WallpaperChangeScheduler.start(getActivity(), SettingsFragment.getInterval(getActivity()));
        } else if (TextUtils.equals(key, getString(R.string.pref_key_sync_enabled))) {
            boolean value = (boolean) newValue;
            if (value) LoginActivity.login(this, REQUEST_CODE_LOGIN);
            else LoginActivity.logout(this, REQUEST_CODE_LOGOUT);
        }

        return true;
    }
}
