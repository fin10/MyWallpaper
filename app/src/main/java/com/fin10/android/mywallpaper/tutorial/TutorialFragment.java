package com.fin10.android.mywallpaper.tutorial;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TutorialFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) return null;

        int layoutId = args.getInt(Argument.LAYOUT_ID);
        return inflater.inflate(layoutId, container, false);
    }

    public static final class Argument {

        static final String LAYOUT_ID = "layout_id";

        private Argument() {
        }
    }
}
