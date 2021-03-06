package com.fin10.android.mywallpaper.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.drive.LoginActivity;
import com.fin10.android.mywallpaper.drive.SyncManager;
import com.fin10.android.mywallpaper.live.LiveWallpaperService;
import com.fin10.android.mywallpaper.live.WallpaperChangeScheduler;
import com.google.android.material.snackbar.Snackbar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.core.app.ActivityCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

public final class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsFragment.class);

    private static final int REQUEST_CODE_LOGIN = 1;
    private static final int REQUEST_CODE_LOGOUT = 2;

    private Snackbar mSnackBar;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        Context context = getActivity();
        SwitchPreference switchPreference = (SwitchPreference) findPreference(getString(R.string.pref_key_auto_change_enabled));
        switchPreference.setIconSpaceReserved(false);
        switchPreference.setChecked(PreferenceModel.isAutoChangeEnabled(context));
        switchPreference.setOnPreferenceChangeListener(this);

        PeriodPreference periodPreference = (PeriodPreference) findPreference(getString(R.string.pref_key_auto_change_period));
        periodPreference.setPeriod(PreferenceModel.getPeriod(context));
        periodPreference.setOnPreferenceChangeListener(this);

        switchPreference = (SwitchPreference) findPreference(getString(R.string.pref_key_sync_enabled));
        switchPreference.setIconSpaceReserved(false);
        switchPreference.setChecked(PreferenceModel.isSyncEnabled(context));
        switchPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSnackBar = Snackbar.make(getActivity().findViewById(R.id.coordinator_layout), R.string.it_needs_to_set_live_wallpaper, Snackbar.LENGTH_SHORT);
        mSnackBar.setActionTextColor(ActivityCompat.getColor(getActivity(), R.color.primary));
        mSnackBar.setAction(R.string.set, view -> {
            startActivity(LiveWallpaperService.getIntentForSetLiveWallpaper());
            mSnackBar.dismiss();
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOGGER.debug("requestCode:{}, resultCode:{}", requestCode, resultCode);
        switch (requestCode) {
            case REQUEST_CODE_LOGIN: {
                if (resultCode == Activity.RESULT_OK) {
                    Context context = getActivity();
                    PreferenceModel.setSyncEnabled(context, true);
                    SwitchPreference pref = (SwitchPreference) findPreference(getString(R.string.pref_key_sync_enabled));
                    pref.setChecked(true);
                    SyncManager.start(context);
                }
                break;
            }
            case REQUEST_CODE_LOGOUT: {
                if (resultCode == Activity.RESULT_OK) {
                    Context context = getActivity();
                    PreferenceModel.setSyncEnabled(context, false);
                    SwitchPreference pref = (SwitchPreference) findPreference(getString(R.string.pref_key_sync_enabled));
                    pref.setChecked(false);
                    SyncManager.stop(context);
                }
                break;
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        LOGGER.debug("{}, newValue:{}", key, newValue);
        if (TextUtils.equals(key, getString(R.string.pref_key_auto_change_enabled))) {
            boolean value = (boolean) newValue;
            if (value) {
                if (!LiveWallpaperService.isSet(getActivity())) {
                    if (!mSnackBar.isShown()) mSnackBar.show();
                    return false;
                } else {
                    WallpaperChangeScheduler.stop(getActivity());
                    WallpaperChangeScheduler.start(getActivity(), PreferenceModel.getInterval(getActivity()));
                }
            } else {
                WallpaperChangeScheduler.stop(getActivity());
            }
        } else if (TextUtils.equals(key, getString(R.string.pref_key_auto_change_period)) && preference.isEnabled()) {
            WallpaperChangeScheduler.stop(getActivity());
            WallpaperChangeScheduler.start(getActivity(), PreferenceModel.getInterval((int) newValue));
        } else if (TextUtils.equals(key, getString(R.string.pref_key_sync_enabled))) {
            boolean value = (boolean) newValue;
            if (value) LoginActivity.login(this, REQUEST_CODE_LOGIN);
            else LoginActivity.logout(this, REQUEST_CODE_LOGOUT);
            return false;
        }

        PreferenceModel.setValue(key, String.valueOf(newValue));
        return true;
    }
}
