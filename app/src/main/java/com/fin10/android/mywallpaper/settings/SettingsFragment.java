package com.fin10.android.mywallpaper.settings;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;

import com.fin10.android.mywallpaper.BuildConfig;
import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.drive.LoginActivity;
import com.fin10.android.mywallpaper.drive.SyncScheduler;
import com.fin10.android.mywallpaper.live.LiveWallpaperService;
import com.fin10.android.mywallpaper.live.WallpaperChangeScheduler;

public final class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final int REQUEST_CODE_LOGIN = 1;
    private static final int REQUEST_CODE_LOGOUT = 2;

    private Snackbar mSnackBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        findPreference(getString(R.string.pref_key_auto_change_enabled)).setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.pref_key_auto_change_period)).setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.pref_key_sync_enabled)).setOnPreferenceChangeListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSnackBar = Snackbar.make(getActivity().findViewById(R.id.coordinator_layout), R.string.it_needs_to_set_live_wallpaper, Snackbar.LENGTH_SHORT);
        mSnackBar.setActionTextColor(ActivityCompat.getColor(getActivity(), R.color.primary));
        mSnackBar.setAction(R.string.set, new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(LiveWallpaperService.getIntentForSetLiveWallpaper());
                mSnackBar.dismiss();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("requestCode:%d, resultCode:%d", requestCode, resultCode);
        switch (requestCode) {
            case REQUEST_CODE_LOGIN: {
                if (resultCode == Activity.RESULT_OK) {
                    SwitchPreference pref = (SwitchPreference) findPreference(getString(R.string.pref_key_sync_enabled));
                    pref.setChecked(true);
                    SyncScheduler.start(getActivity());
                }
                break;
            }
            case REQUEST_CODE_LOGOUT: {
                SyncScheduler.stop(getActivity());
                SwitchPreference pref = (SwitchPreference) findPreference(getString(R.string.pref_key_sync_enabled));
                pref.setChecked(false);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("%s, newValue:%s", preference, newValue);
        String key = preference.getKey();
        if (TextUtils.equals(key, getString(R.string.pref_key_auto_change_enabled))) {
            boolean value = (boolean) newValue;
            if (value) {
                WallpaperInfo info = WallpaperManager.getInstance(getActivity()).getWallpaperInfo();
                if (info == null || !BuildConfig.APPLICATION_ID.equals(info.getPackageName())) {
                    if (!mSnackBar.isShown()) mSnackBar.show();
                    return false;
                }
            } else {
                WallpaperChangeScheduler.stop(getActivity());
            }
        } else if (TextUtils.equals(key, getString(R.string.pref_key_auto_change_period)) && preference.isEnabled()) {
            WallpaperChangeScheduler.stop(getActivity());
            WallpaperChangeScheduler.start(getActivity(), PreferenceUtils.getInterval(getActivity()));
        } else if (TextUtils.equals(key, getString(R.string.pref_key_sync_enabled))) {
            boolean value = (boolean) newValue;
            if (value) LoginActivity.login(this, REQUEST_CODE_LOGIN);
            else LoginActivity.logout(this, REQUEST_CODE_LOGOUT);
            return false;
        }

        return true;
    }
}
