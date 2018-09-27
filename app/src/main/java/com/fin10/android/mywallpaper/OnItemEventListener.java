package com.fin10.android.mywallpaper;

import android.view.View;

import androidx.annotation.NonNull;

interface OnItemEventListener {
    void onItemClick(@NonNull View itemView, int position);

    boolean onItemLongClick(@NonNull View itemView, int position);
}
