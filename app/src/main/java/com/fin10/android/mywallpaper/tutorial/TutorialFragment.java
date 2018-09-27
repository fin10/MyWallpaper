package com.fin10.android.mywallpaper.tutorial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TutorialFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) return null;

        int layoutId = args.getInt(Argument.LAYOUT_ID);
        return inflater.inflate(layoutId, container, false);
    }

    static final class Argument {

        static final String LAYOUT_ID = "layout_id";

        private Argument() {
        }
    }
}
