package com.fin10.android.mywallpaper.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.text.TextUtils;

import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.drive.LoginActivity;
import com.fin10.android.mywallpaper.drive.SyncScheduler;

public final class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final int REQUEST_CODE_LOGIN = 1;
    private static final int REQUEST_CODE_LOGOUT = 2;

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
            if (value) WallpaperChangeScheduler.start(getActivity(), PreferenceUtils.getInterval(getActivity()));
            else WallpaperChangeScheduler.stop(getActivity());
        } else if (TextUtils.equals(key, getString(R.string.pref_key_auto_change_period))) {
            WallpaperChangeScheduler.stop(getActivity());
            WallpaperChangeScheduler.start(getActivity(), PreferenceUtils.getInterval(getActivity()));
        } else if (TextUtils.equals(key, getString(R.string.pref_key_sync_enabled))) {
            boolean value = (boolean) newValue;
            if (value) LoginActivity.login(this, REQUEST_CODE_LOGIN);
            else LoginActivity.logout(this, REQUEST_CODE_LOGOUT);
        }

        return true;
    }
}
