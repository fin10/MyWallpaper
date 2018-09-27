package com.fin10.android.mywallpaper.tutorial;

import android.animation.ArgbEvaluator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.fin10.android.mywallpaper.MainActivity;
import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.live.LiveWallpaperService;
import com.fin10.android.mywallpaper.settings.PreferenceModel;
import com.ugurtekbas.fadingindicatorlibrary.FadingIndicator;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public final class TutorialActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {

    private final ArgbEvaluator mEvaluator = new ArgbEvaluator();
    private TutorialPageAdapter mAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        mAdapter = new TutorialPageAdapter(getSupportFragmentManager());

        mViewPager = findViewById(R.id.view_pager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(this);

        FadingIndicator indicator = findViewById(R.id.indicator);
        indicator.setViewPager(mViewPager);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewPager.removeOnPageChangeListener(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_button:
                startActivity(new Intent(this, MainActivity.class));
                PreferenceModel.setTutorialEnabled(this, false);
                finish();
                break;
            case R.id.set_live_wallpaper_button:
                startActivity(LiveWallpaperService.getIntentForSetLiveWallpaper());
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        int from = mAdapter.getColor(position);
        int to = mAdapter.getColor(position + 1);
        int color = (int) mEvaluator.evaluate(positionOffset, from, to);
        mViewPager.getBackground().setTint(color);
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private static final class TutorialPageAdapter extends FragmentPagerAdapter {

        private final List<Integer> mColors = new ArrayList<>();

        TutorialPageAdapter(FragmentManager fm) {
            super(fm);
            mColors.add(0xFF37AFBF);
            mColors.add(0xff208DB6);
            mColors.add(0xff8050FF);
            mColors.add(0xff009688);
            mColors.add(0xff446EB6);
        }

        @ColorInt
        int getColor(int position) {
            if (mColors.size() > position) {
                return mColors.get(position);
            }

            return Color.BLACK;
        }

        @Override
        public int getCount() {
            return mColors.size();
        }

        @Override
        public Fragment getItem(int position) {
            TutorialFragment fragment;
            Bundle args = new Bundle();
            switch (position) {
                case 0: {
                    fragment = new TutorialFragment();
                    args.putInt(TutorialFragment.Argument.LAYOUT_ID, R.layout.tutorial_1_layout);
                    fragment.setArguments(args);
                    return fragment;
                }
                case 1: {
                    fragment = new TutorialFragment();
                    args.putInt(TutorialFragment.Argument.LAYOUT_ID, R.layout.tutorial_2_layout);
                    fragment.setArguments(args);
                    return fragment;
                }
                case 2: {
                    fragment = new TutorialFragment();
                    args.putInt(TutorialFragment.Argument.LAYOUT_ID, R.layout.tutorial_3_layout);
                    fragment.setArguments(args);
                    return fragment;
                }
                case 3: {
                    return new LiveWallpaperTutorialFragment();
                }
                case 4: {
                    return new AutoChangeTutorialFragment();
                }
                default:
                    fragment = new TutorialFragment();
                    break;
            }

            return fragment;
        }
    }
}
