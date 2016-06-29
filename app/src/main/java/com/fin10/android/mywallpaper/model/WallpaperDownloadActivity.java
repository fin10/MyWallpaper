package com.fin10.android.mywallpaper.model;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.view.WindowManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.MainActivity;
import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.settings.SettingsFragment;
import com.fin10.android.mywallpaper.settings.WallpaperChangeScheduler;

import java.util.Arrays;
import java.util.regex.Pattern;

public final class WallpaperDownloadActivity extends AppCompatActivity {

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
            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                int count = clipData.getItemCount();
                if ("text/plain".equals(type)) {
                    for (int i = 0; i < count; ++i) {
                        String uri = String.valueOf(clipData.getItemAt(i).getText());
                        if (uri.contains(" ")) {
                            String[] texts = uri.split("\\s|\\n");
                            Log.d(Arrays.toString(texts));
                            String regex = "\\b(http|https)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
                            Pattern pattern = Pattern.compile(regex);
                            for (String text : texts) {
                                if (pattern.matcher(text).matches()) {
                                    uri = text;
                                    break;
                                }
                            }
                        }

                        DownloadService.download(this, Uri.parse(uri));
                    }
                } else if (type.startsWith("image/")) {
                    for (int i = 0; i < count; ++i) {
                        DownloadService.download(this, clipData.getItemAt(i).getUri());
                    }
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

        @NonNull
        private static Pair<Integer, Integer> getScreenSize(@NonNull Context context) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Point size = new Point();
            wm.getDefaultDisplay().getSize(size);
            return new Pair<>(size.x, size.y);
        }

        @NonNull
        private static Notification createDownloadingNotification(@NonNull Context context, @NonNull Uri uri) {
            return new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_wallpaper_white_48dp)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setContentTitle(context.getString(R.string.downloading_new_wallpaper))
                    .setContentText(String.valueOf(uri))
                    .setProgress(0, 0, true)
                    .setOngoing(true)
                    .setShowWhen(false)
                    .build();
        }

        @NonNull
        private static Notification createDownloadedNotification(@NonNull Context context, @NonNull Bitmap bitmap) {
            return new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_wallpaper_white_48dp)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setCategory(Notification.CATEGORY_STATUS)
                    .setContentTitle(context.getString(R.string.new_wallpaper))
                    .setContentText(context.getString(R.string.download_complete))
                    .setLargeIcon(bitmap)
                    .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                    .setContentIntent(createAppLaunchPendingIntent(context))
                    .setAutoCancel(true)
                    .build();
        }

        @NonNull
        private static Notification createFailedNotification(@NonNull Context context, @NonNull Uri uri) {
            return new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_wallpaper_white_48dp)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setCategory(Notification.CATEGORY_ERROR)
                    .setContentTitle(context.getString(R.string.failed_to_download))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(String.valueOf(uri)))
                    .setContentIntent(createAppLaunchPendingIntent(context))
                    .setAutoCancel(true)
                    .build();
        }

        @NonNull
        private static PendingIntent createAppLaunchPendingIntent(@NonNull Context context) {
            Intent intent = new Intent(context, MainActivity.class);
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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

            final Uri uri = intent.getParcelableExtra("uri");
            Log.d("uri:%s", uri);
            if (uri == null) {
                Log.e("[%d] the intent has no uri.", startId);
                return Service.START_NOT_STICKY;
            }

            NotificationManagerCompat.from(this).notify(uri.hashCode(), createDownloadingNotification(this, uri));

            Pair<Integer, Integer> size = getScreenSize(this);
            Glide.with(this)
                    .load(uri)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(new SimpleTarget<Bitmap>(size.first, size.second) {

                        @Override
                        public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            Log.d("W:%d, H:%d", resource.getWidth(), resource.getHeight());
                            new AsyncTask<Void, Void, Boolean>() {

                                @Override
                                protected Boolean doInBackground(Void... voids) {
                                    WallpaperModel result = WallpaperModel.addModel(String.valueOf(uri), resource);
                                    return result != null;
                                }

                                @Override
                                protected void onPostExecute(Boolean result) {
                                    if (result) {
                                        if (SettingsFragment.isAutoChangeEnabled(getBaseContext())) {
                                            WallpaperChangeScheduler.start(getBaseContext());
                                        }

                                        NotificationManagerCompat.from(getBaseContext())
                                                .notify(uri.hashCode(), createDownloadedNotification(getBaseContext(), resource));
                                    } else {
                                        NotificationManagerCompat.from(getBaseContext())
                                                .notify(uri.hashCode(), createFailedNotification(getBaseContext(), uri));
                                    }
                                }
                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);
                            if (e != null) e.printStackTrace();
                            NotificationManagerCompat.from(getBaseContext())
                                    .notify(uri.hashCode(), createFailedNotification(getBaseContext(), uri));
                        }
                    });

            return Service.START_NOT_STICKY;
        }
    }
}
