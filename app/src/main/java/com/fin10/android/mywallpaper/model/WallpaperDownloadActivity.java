package com.fin10.android.mywallpaper.model;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.SparseArray;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.fin10.android.mywallpaper.BuildConfig;
import com.fin10.android.mywallpaper.MainActivity;
import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.drive.SyncManager;
import com.fin10.android.mywallpaper.settings.PreferenceModel;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public final class WallpaperDownloadActivity extends Activity {

    private static final Logger LOGGER = LoggerFactory.getLogger(WallpaperDownloadActivity.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            final Intent intent = getIntent();
            final String action = intent.getAction();
            final String type = intent.getType();
            if (TextUtils.isEmpty(type)) return;

            if (Intent.ACTION_SEND.equals(action)) {
                final Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                try {
                    DownloadService.download(this, stream);
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                final ArrayList<Uri> streams = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                for (Uri stream : streams) {
                    try {
                        DownloadService.download(this, stream);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        } finally {
            finish();
        }
    }

    private static final class Pack implements Serializable {
        final int id;
        final String name;
        final File file;

        Pack(int id, @NonNull String name, @NonNull File file) {
            this.id = id;
            this.name = name;
            this.file = file;
        }
    }

    public static final class DownloadService extends Service {

        private static final String CHANNEL_ID = "my_wallpaper";

        private static final String ACTION_CANCEL_NOTIFICATION = BuildConfig.APPLICATION_ID + ".action.cancel_notification";
        private static final String ACTION_DOWNLOAD_WALLPAPER = BuildConfig.APPLICATION_ID + ".action.download_wallpaper";

        private static final String EXTRA_PACK = BuildConfig.APPLICATION_ID + ".extra.pack";

        private final SparseArray<AsyncTask> tasks = new SparseArray<>();

        private static void download(@NonNull Context context, @NonNull Uri uri) throws IOException {
            LOGGER.debug("uri:{}", uri);
            if (uri.getPath() == null) throw new IllegalArgumentException("uri has no path");

            final InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) throw new FileNotFoundException("Failed to get stream from " + uri);

            final File temp = File.createTempFile("my-", null);
            FileUtils.copyInputStreamToFile(input, temp);

            final Intent i = new Intent(context, DownloadService.class);
            i.setAction(ACTION_DOWNLOAD_WALLPAPER);
            i.putExtra(EXTRA_PACK, new Pack(uri.hashCode(), uri.getPath(), temp));
            context.startService(i);
        }

        @NonNull
        private static Notification createDownloadingNotification(@NonNull Context context, @NonNull Pack pack) {
            return new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_wallpaper_24px)
                    .setColor(ContextCompat.getColor(context, R.color.primary))
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setContentTitle(context.getString(R.string.downloading_new_wallpaper))
                    .setContentText(pack.name)
                    .setProgress(0, 0, true)
                    .setOngoing(true)
                    .setShowWhen(false)
                    .addAction(R.drawable.ic_baseline_clear_24px,
                            context.getString(android.R.string.cancel),
                            createCancelPendingIntent(context, pack))
                    .build();
        }

        @NonNull
        private static PendingIntent createCancelPendingIntent(@NonNull Context context, @NonNull Pack pack) {
            Intent intent = new Intent(context, DownloadService.class);
            intent.setAction(ACTION_CANCEL_NOTIFICATION);
            intent.putExtra(EXTRA_PACK, pack);
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        @Override
        public void onCreate() {
            super.onCreate();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    CharSequence name = getString(R.string.app_name);
                    int importance = NotificationManager.IMPORTANCE_DEFAULT;
                    NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                    notificationManager.createNotificationChannel(channel);
                }
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.deleteNotificationChannel(CHANNEL_ID);
                }
            }
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, final int startId) {
            final Pack pack = (Pack) intent.getSerializableExtra(EXTRA_PACK);
            if (pack == null) {
                LOGGER.error("Pack not founds.");
                return Service.START_NOT_STICKY;
            }

            final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                LOGGER.error("Unable to get notification manager.");
                return Service.START_NOT_STICKY;
            }

            final String action = intent.getAction();
            if (ACTION_CANCEL_NOTIFICATION.equals(action)) {
                LOGGER.info("Canceling {} notification", pack.id);
                notificationManager.cancel(pack.id);
                AsyncTask task = tasks.get(pack.id);
                if (task != null) {
                    tasks.delete(pack.id);
                    task.cancel(true);
                } else {
                    LOGGER.error("{} task does not exists.", pack.id);
                }
            } else if (ACTION_DOWNLOAD_WALLPAPER.equals(action)) {
                notificationManager.notify(pack.id, createDownloadingNotification(this, pack));

                AsyncTask<Void, Void, Bitmap> task = new DownloadTask(this, tasks, pack);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                tasks.put(pack.id, task);
            }

            return Service.START_NOT_STICKY;
        }
    }

    private static final class DownloadTask extends AsyncTask<Void, Void, Bitmap> {

        private final WeakReference<Context> context;
        private final SparseArray<AsyncTask> tasks;
        private final Pack pack;

        DownloadTask(@NonNull Context context, @NonNull SparseArray<AsyncTask> tasks, @NonNull Pack pack) {
            this.context = new WeakReference<>(context);
            this.tasks = tasks;
            this.pack = pack;
        }

        @Nullable
        @Override
        protected Bitmap doInBackground(Void... voids) {
            Context context = this.context.get();
            if (context == null) return null;

            try {
                WallpaperModel result = WallpaperModel.addModel(context, pack.file);
                if (PreferenceModel.isSyncEnabled(context)) {
                    SyncManager.upload(context, result);
                }

                return Glide.with(context)
                        .asBitmap()
                        .load(result.getImagePath())
                        .apply(RequestOptions.centerCropTransform())
                        .submit(context.getResources().getDimensionPixelSize(android.R.dimen.thumbnail_width),
                                context.getResources().getDimensionPixelSize(android.R.dimen.thumbnail_height))
                        .get();
            } catch (InterruptedException | ExecutionException | IOException | SecurityException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(@Nullable Bitmap result) {
            Context context = this.context.get();
            if (context == null) return;

            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                LOGGER.error("Unable to get notification manager.");
                return;
            }

            if (result != null) {
                notificationManager.notify(pack.id, createDownloadedNotification(context, result));
                Toast.makeText(context, R.string.new_wallpaper_is_added, Toast.LENGTH_SHORT).show();
            } else {
                notificationManager.notify(pack.id, createFailedNotification(context, pack));
                Toast.makeText(context, R.string.failed_to_download, Toast.LENGTH_SHORT).show();
            }

            int idx = tasks.indexOfValue(this);
            if (idx >= 0) tasks.remove(idx);
        }

        @NonNull
        private static Notification createDownloadedNotification(@NonNull Context context, @NonNull Bitmap bitmap) {
            return new NotificationCompat.Builder(context, DownloadService.CHANNEL_ID)
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
        private static Notification createFailedNotification(@NonNull Context context, @NonNull Pack pack) {
            return new NotificationCompat.Builder(context, DownloadService.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_wallpaper_24px)
                    .setColor(ContextCompat.getColor(context, R.color.primary))
                    .setCategory(Notification.CATEGORY_ERROR)
                    .setContentTitle(context.getString(R.string.failed_to_download))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(pack.name))
                    .setContentIntent(createAppLaunchPendingIntent(context))
                    .setAutoCancel(true)
                    .build();
        }

        @NonNull
        private static PendingIntent createAppLaunchPendingIntent(@NonNull Context context) {
            Intent intent = new Intent(context, MainActivity.class);
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }
}
