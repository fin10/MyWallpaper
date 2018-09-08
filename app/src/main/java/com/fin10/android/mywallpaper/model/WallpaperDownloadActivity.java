package com.fin10.android.mywallpaper.model;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.fin10.android.mywallpaper.BuildConfig;
import com.fin10.android.mywallpaper.MainActivity;
import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.Utils;
import com.fin10.android.mywallpaper.drive.SyncManager;
import com.fin10.android.mywallpaper.settings.PreferenceModel;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public final class WallpaperDownloadActivity extends AppCompatActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(WallpaperDownloadActivity.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = getIntent();
            String action = intent.getAction();
            String type = intent.getType();
            if (TextUtils.isEmpty(type)) return;

            if (Intent.ACTION_SEND.equals(action)) {
                if ("text/plain".equals(type)) {
                    String uri = intent.getStringExtra(Intent.EXTRA_TEXT);
                    if (!TextUtils.isEmpty(uri)) {
                        if (uri.contains(" ")) {
                            String[] texts = uri.split("\\s|\\n");
                            String regex = "\\b(http|https)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
                            Pattern pattern = Pattern.compile(regex);
                            for (String text : texts) {
                                if (pattern.matcher(text).matches()) {
                                    uri = text;
                                    break;
                                }
                            }
                        } else if (!uri.startsWith("http")) {
                            uri = "https://" + uri;
                        }
                    }

                    DownloadService.download(this, Uri.parse(uri));

                } else if (type.startsWith("image/")) {
                    Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    DownloadService.download(this, stream);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                if (type.startsWith("image/")) {
                    ArrayList<Uri> streams = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    for (Uri stream : streams) {
                        DownloadService.download(this, stream);
                    }
                }
            }
        } finally {
            finish();
        }
    }

    public static final class DownloadService extends Service {

        private static final String ACTION_CANCEL_NOTIFICATION = BuildConfig.APPLICATION_ID + ".action.cancel_notification";
        private static final String ACTION_DOWNLOAD_WALLPAPER = BuildConfig.APPLICATION_ID + ".action.download_wallpaper";
        private static final String EXTRA_ID = BuildConfig.APPLICATION_ID + ".extra.id";
        private static final String EXTRA_URI = BuildConfig.APPLICATION_ID + ".extra.uri";

        private final SparseArray<AsyncTask> mTaskMap = new SparseArray<>();

        private static void download(@NonNull Context context, @NonNull Uri uri) {
            Intent i = new Intent(context, DownloadService.class);
            i.setAction(ACTION_DOWNLOAD_WALLPAPER);
            i.putExtra(EXTRA_ID, uri.hashCode());
            i.putExtra(EXTRA_URI, uri);
            context.startService(i);
        }

        @NonNull
        private static Notification createDownloadingNotification(@NonNull Context context, @NonNull Uri uri, int id) {
            return new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_baseline_wallpaper_24px)
                    .setColor(ContextCompat.getColor(context, R.color.primary))
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setContentTitle(context.getString(R.string.downloading_new_wallpaper))
                    .setContentText(String.valueOf(uri))
                    .setProgress(0, 0, true)
                    .setOngoing(true)
                    .setShowWhen(false)
                    .addAction(R.drawable.ic_baseline_clear_24px,
                            context.getString(android.R.string.cancel),
                            createCancelPendingIntent(context, id))
                    .build();
        }

        @NonNull
        private static Notification createDownloadedNotification(@NonNull Context context, @NonNull Bitmap bitmap) {
            return new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_baseline_wallpaper_24px)
                    .setColor(ContextCompat.getColor(context, R.color.primary))
                    .setCategory(Notification.CATEGORY_STATUS)
                    .setContentTitle(context.getString(R.string.download_complete))
                    .setContentText(context.getString(R.string.new_wallpaper_is_added))
                    .setLargeIcon(bitmap)
                    .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                    .setContentIntent(createAppLaunchPendingIntent(context))
                    .setAutoCancel(true)
                    .build();
        }

        @NonNull
        private static Notification createFailedNotification(@NonNull Context context, @NonNull Uri uri) {
            return new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_baseline_wallpaper_24px)
                    .setColor(ContextCompat.getColor(context, R.color.primary))
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

        @NonNull
        private static PendingIntent createCancelPendingIntent(@NonNull Context context, int id) {
            Intent intent = new Intent(context, DownloadService.class);
            intent.setAction(ACTION_CANCEL_NOTIFICATION);
            intent.putExtra(EXTRA_ID, id);
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, final int startId) {
            String action = intent.getAction();
            final int id = intent.getIntExtra(EXTRA_ID, -1);
            if (id == -1) {
                LOGGER.error("There is no id.");
                stopSelf();
                return Service.START_NOT_STICKY;
            }

            if (ACTION_CANCEL_NOTIFICATION.equals(action)) {
                LOGGER.debug("cancel {} notification", id);
                NotificationManagerCompat.from(this).cancel(id);
                AsyncTask task = mTaskMap.get(id);
                if (task != null) {
                    mTaskMap.delete(id);
                    task.cancel(true);
                } else {
                    LOGGER.error("{} task does not exists.", id);
                }
                stopSelf();

            } else if (ACTION_DOWNLOAD_WALLPAPER.equals(action)) {
                final Uri uri = intent.getParcelableExtra(EXTRA_URI);
                LOGGER.debug("uri:{}", uri);
                if (uri == null) {
                    LOGGER.error("the intent has no uri.");
                    stopSelf();
                    return Service.START_NOT_STICKY;
                }

                NotificationManagerCompat.from(this).notify(id, createDownloadingNotification(this, uri, id));

                AsyncTask<Context, Void, Bitmap> task = new AsyncTask<Context, Void, Bitmap>() {

                    @Nullable
                    @Override
                    protected Bitmap doInBackground(Context... contexts) {
                        Context context = contexts[0];
                        Pair<Integer, Integer> size = Utils.getScreenSize(context);
                        try {
                            File file = Glide.with(context)
                                    .load(uri)
                                    .downloadOnly(size.first, size.second)
                                    .get();

                            File copied = new File(context.getDataDir().getAbsoluteFile() + "/" + System.currentTimeMillis() + ".png");
                            FileUtils.copyFile(file, copied);

                            WallpaperModel result = WallpaperModel.addModel(copied);
                            if (result != null && PreferenceModel.isSyncEnabled(getBaseContext())) {
                                SyncManager.upload(getBaseContext(), result);
                            }

                            return Glide.with(context)
                                    .load(file.getAbsolutePath())
                                    .asBitmap()
                                    .centerCrop()
                                    .into(context.getResources().getDimensionPixelSize(android.R.dimen.thumbnail_width),
                                            context.getResources().getDimensionPixelSize(android.R.dimen.thumbnail_height))
                                    .get();
                        } catch (InterruptedException | ExecutionException | IOException e) {
                            LOGGER.error(e.getLocalizedMessage(), e);
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(@Nullable Bitmap result) {
                        if (result != null) {
                            NotificationManagerCompat.from(getBaseContext())
                                    .notify(uri.hashCode(), createDownloadedNotification(getBaseContext(), result));
                            Toast.makeText(getBaseContext(), R.string.new_wallpaper_is_added, Toast.LENGTH_SHORT).show();
                        } else {
                            NotificationManagerCompat.from(getBaseContext())
                                    .notify(uri.hashCode(), createFailedNotification(getBaseContext(), uri));
                            Toast.makeText(getBaseContext(), R.string.failed_to_download, Toast.LENGTH_SHORT).show();
                        }

                        mTaskMap.remove(id);
                        stopSelf();
                    }
                };
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
                mTaskMap.put(id, task);
            }

            return Service.START_NOT_STICKY;
        }
    }
}
