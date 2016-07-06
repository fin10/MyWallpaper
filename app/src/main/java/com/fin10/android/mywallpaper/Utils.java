package com.fin10.android.mywallpaper;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.view.WindowManager;

import java.util.Set;

public final class Utils {

    private Utils() {
    }

    @NonNull
    public static Pair<Integer, Integer> getScreenSize(@NonNull Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        return new Pair<>(size.x, size.y);
    }


    @NonNull
    public static String toString(@Nullable Bundle bundle) {
        if (bundle == null) return "null";

        StringBuilder strBuilder = new StringBuilder();
        Set<String> keySet = bundle.keySet();
        for (String key : keySet) {
            strBuilder.append("[");
            strBuilder.append(key);
            strBuilder.append("] ");
            strBuilder.append(bundle.get(key));
            strBuilder.append(", ");
        }

        return strBuilder.toString();
    }
}
