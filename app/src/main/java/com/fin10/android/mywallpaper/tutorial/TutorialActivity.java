package com.fin10.android.mywallpaper.tutorial;

import android.animation.ArgbEvaluator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.fin10.android.mywallpaper.MainActivity;
import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.settings.PreferenceUtils;
import com.ugurtekbas.fadingindicatorlibrary.FadingIndicator;

import java.util.ArrayList;
import java.util.List;

public final class TutorialActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    private TutorialPageAdapter mAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        mAdapter = new TutorialPageAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(this);

        FadingIndicator indicator = (FadingIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mViewPager);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewPager.removeOnPageChangeListener(this);
    }

    public void onClick(View view) {
        startActivity(new Intent(this, MainActivity.class));
        PreferenceUtils.setTutorialEnabled(this, false);
        finish();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        int from = mAdapter.getColor(position);
        int to = mAdapter.getColor(position + 1);
        int color = (int) new ArgbEvaluator().evaluate(positionOffset, from, to);
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

        public TutorialPageAdapter(FragmentManager fm) {
            super(fm);
            mColors.add(0xFF37AFBF);
            mColors.add(0xff208DB6);
            mColors.add(0xff8050FF);
            mColors.add(0xff446EB6);
        }

        @ColorInt
        public int getColor(int position) {
            if (mColors.size() > position) {
                return mColors.get(position);
            }

            return Color.BLACK;
        }

        @Override
        public int getCount() {
            return 4;
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
