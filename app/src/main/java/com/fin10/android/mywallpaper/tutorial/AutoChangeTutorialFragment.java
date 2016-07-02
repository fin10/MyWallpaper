package com.fin10.android.mywallpaper.tutorial;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.settings.SettingsFragment;

public final class AutoChangeTutorialFragment extends TutorialFragment implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {

    private SeekBar mSeekBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tutorial_4_layout, container, false);
        Switch sw = (Switch) root.findViewById(R.id.auto_change_switch);
        mSeekBar = (SeekBar) root.findViewById(R.id.seek_bar);

        sw.setOnCheckedChangeListener(this);
        sw.setChecked(SettingsFragment.isAutoChangeEnabled(getActivity()));

        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setEnabled(sw.isChecked());
        mSeekBar.setProgress(SettingsFragment.getPeriod(getActivity()));

        return root;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SettingsFragment.setAutoChangeEnabled(getActivity(), isChecked);
        mSeekBar.setEnabled(isChecked);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        SettingsFragment.setPeriod(seekBar.getContext(), progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
