package com.fin10.android.mywallpaper.tutorial;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TutorialFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        int layoutId = args.getInt(Argument.LAYOUT_ID);
        return inflater.inflate(layoutId, container, false);
    }

    public static final class Argument {

        static final String LAYOUT_ID = "layout_id";

        private Argument() {
        }
    }
}
