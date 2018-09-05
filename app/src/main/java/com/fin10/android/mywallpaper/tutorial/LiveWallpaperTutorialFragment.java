package com.fin10.android.mywallpaper.tutorial;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.live.LiveWallpaperService;

public final class LiveWallpaperTutorialFragment extends TutorialFragment {

    private Button mSetLiveWallpaperButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.tutorial_live_wallpaper_layout, container, false);
        mSetLiveWallpaperButton = root.findViewById(R.id.set_live_wallpaper_button);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if (activity == null) return;

        if (LiveWallpaperService.isSet(activity)) {
            mSetLiveWallpaperButton.setText(R.string.completed);
            mSetLiveWallpaperButton.setTextColor(ActivityCompat.getColor(getActivity(), R.color.primary_text));
            mSetLiveWallpaperButton.setEnabled(false);
        } else {
            mSetLiveWallpaperButton.setText(R.string.set_live_wallpaper);
            mSetLiveWallpaperButton.setTextColor(Color.WHITE);
            mSetLiveWallpaperButton.setEnabled(true);
        }
    }
}
