package com.fin10.android.mywallpaper;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.fin10.android.mywallpaper.model.WallpaperModel;

public final class PutWallpaperActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = getIntent();
            String action = intent.getAction();
            String type = intent.getType();
            ClipData clipData = intent.getClipData();
            if (TextUtils.isEmpty(type) && clipData.getItemCount() <= 0) {
                return;
            }

            if (Intent.ACTION_SEND.equals(action)) {
                if ("text/plain".equals(type)) {
                    WallpaperModel.DownloadService.putWallpaper(this, Uri.parse(clipData.getItemAt(0).getText().toString()));
                } else if (type.startsWith("image/")) {
                    WallpaperModel.DownloadService.putWallpaper(this, Uri.parse(clipData.getItemAt(0).getUri().toString()));
                }
            }
        } finally {
            finish();
        }
    }
}
