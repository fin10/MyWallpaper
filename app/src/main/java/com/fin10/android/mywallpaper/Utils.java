package com.fin10.android.mywallpaper;

import android.content.Context;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.WindowManager;

public final class Utils {

    private Utils() {
    }

    @Nullable
    public static Pair<Integer, Integer> getScreenSize(@NonNull Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return null;

        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        return Pair.create(size.x, size.y);
    }
}
