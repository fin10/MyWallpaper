package com.fin10.android.mywallpaper.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

import com.fin10.android.mywallpaper.R;

public final class PeriodPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private int mPeriod;

    public PeriodPreference(Context context) {
        super(context);
        init();
    }

    public PeriodPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PeriodPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PeriodPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_auto_change_period);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        SeekBar seekBar = view.findViewById(R.id.seek_bar);
        seekBar.setProgress(mPeriod);
        seekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setPeriod(restoreValue ? getPersistedInt(mPeriod) : (Integer) defaultValue);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!callChangeListener(progress)) {
            seekBar.setProgress(mPeriod);
            return;
        }

        setPeriod(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    void setPeriod(int period) {
        final boolean changed = mPeriod != period;
        if (changed) {
            mPeriod = period;
            persistInt(period);
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    interface Period {
        int SOMETIMES = 0;
        int USUALLY = 1;
        int FREQUENTLY = 2;
    }
}
