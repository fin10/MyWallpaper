package com.fin10.android.mywallpaper.list;

import android.support.annotation.NonNull;
import android.view.View;

interface OnItemEventListener {
    void onItemClick(@NonNull View itemView, int position);

    boolean onItemLongClick(@NonNull View itemView, int position);
}
