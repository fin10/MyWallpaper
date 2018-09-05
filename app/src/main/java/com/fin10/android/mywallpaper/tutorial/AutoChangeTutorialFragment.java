package com.fin10.android.mywallpaper.tutorial;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.live.LiveWallpaperService;
import com.fin10.android.mywallpaper.settings.PreferenceModel;

public final class AutoChangeTutorialFragment extends TutorialFragment implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {

    private Switch mSwitch;
    private SeekBar mSeekBar;
    private Snackbar mSnackBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tutorial_auto_change_layout, container, false);
        mSwitch = root.findViewById(R.id.auto_change_switch);
        mSwitch.setOnCheckedChangeListener(this);

        mSeekBar = root.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Activity activity = getActivity();
        if (activity == null) return;

        super.onActivityCreated(savedInstanceState);
        mSnackBar = Snackbar.make(activity.findViewById(R.id.coordinator_layout), R.string.it_needs_to_set_live_wallpaper, Snackbar.LENGTH_SHORT);
        mSnackBar.setActionTextColor(ActivityCompat.getColor(getActivity(), R.color.primary));
        mSnackBar.setAction(R.string.set, view -> {
            startActivity(LiveWallpaperService.getIntentForSetLiveWallpaper());
            mSnackBar.dismiss();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if (activity == null) return;

        mSwitch.setChecked(PreferenceModel.isAutoChangeEnabled(activity));
        mSeekBar.setEnabled(mSwitch.isChecked());
        mSeekBar.setProgress(PreferenceModel.getPeriod(getActivity()));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Activity activity = getActivity();
        if (activity == null) return;

        if (isChecked) {
            if (!LiveWallpaperService.isSet(activity)) {
                if (!mSnackBar.isShown()) mSnackBar.show();
                buttonView.setChecked(false);
                return;
            }
        }

        PreferenceModel.setAutoChangeEnabled(getActivity(), isChecked);
        mSeekBar.setEnabled(isChecked);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        PreferenceModel.setPeriod(seekBar.getContext(), progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
