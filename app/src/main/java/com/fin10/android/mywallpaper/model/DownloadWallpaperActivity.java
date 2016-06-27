package com.fin10.android.mywallpaper.model;

import android.app.Service;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.R;

import java.util.Arrays;
import java.util.regex.Pattern;

public final class DownloadWallpaperActivity extends AppCompatActivity {

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

            Log.d("%s:%s", type, clipData);
            if (Intent.ACTION_SEND.equals(action)) {
                if ("text/plain".equals(type)) {
                    String uri = String.valueOf(clipData.getItemAt(0).getText());
                    if (uri.contains(" ")) {
                        String[] texts = uri.split("\\s|\\n");
                        Log.d(Arrays.toString(texts));
                        String regex = "\\b(http?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
                        Pattern pattern = Pattern.compile(regex);
                        for (String text : texts) {
                            if (pattern.matcher(text).matches()) {
                                uri = text;
                                break;
                            }
                        }
                    }

                    DownloadService.download(this, Uri.parse(uri));
                } else if (type.startsWith("image/")) {
                    DownloadService.download(this, clipData.getItemAt(0).getUri());
                }
            }
        } finally {
            finish();
        }
    }

    public static final class DownloadService extends Service {

        private static void download(@NonNull Context context, @NonNull Uri uri) {
            Intent i = new Intent(context, DownloadService.class);
            i.putExtra("uri", uri);
            context.startService(i);
        }

        @Override
        public void onCreate() {
            super.onCreate();
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (intent == null) {
                Log.e("[%d] the intent is null.", startId);
                return Service.START_NOT_STICKY;
            }

            Uri uri = intent.getParcelableExtra("uri");
            Log.d("uri:%s", uri);
            if (uri == null) {
                Log.e("[%d] the intent has no uri.", startId);
                return Service.START_NOT_STICKY;
            }

            Glide.with(this)
                    .load(uri)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(new SimpleTarget<Bitmap>() {

                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            boolean result = WallpaperModel.addModel(getBaseContext(), resource);
                            Toast.makeText(getBaseContext(),
                                    result ? R.string.new_wallpaper_is_added : R.string.failed_to_add_wallpaper, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);
                            if (e != null) e.printStackTrace();
                            Toast.makeText(getBaseContext(), R.string.failed_to_add_wallpaper, Toast.LENGTH_LONG).show();
                        }
                    });

            return Service.START_NOT_STICKY;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }
    }
}
