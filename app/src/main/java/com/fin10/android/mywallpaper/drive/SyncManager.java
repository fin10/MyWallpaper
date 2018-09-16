package com.fin10.android.mywallpaper.drive;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.fin10.android.mywallpaper.model.WallpaperModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.AbstractExecutionThreadService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

public final class SyncManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncManager.class);

    private static final String INTENT_ACTION_SYNC = "sync";
    private static final int JOB_ID = 1;

    public static void sync(@NonNull Context context) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(INTENT_ACTION_SYNC);
        context.startService(intent);
    }

    public static void upload(@NonNull Context context, @NonNull WallpaperModel model) {
        if (model.isSynced()) LOGGER.error("{} model already synchronized.", model.getId());
        else DriveApiHelper.upload(context, model).addOnSuccessListener(driveFile -> {
            model.setSynced(true);
            model.update();
        });
    }

    public static void dismiss(@NonNull Context context, @NonNull List<WallpaperModel> models) {
        if (models.isEmpty()) LOGGER.debug("There is no models to dismiss.");
        else DriveApiHelper.dismiss(context, models);
    }

    public static void start(@NonNull Context context) {
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, SyncService.class))
                .setPersisted(true)
                .setPeriodic(DateUtils.DAY_IN_MILLIS)
                .setRequiresCharging(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .build();

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) jobScheduler.schedule(jobInfo);
        else LOGGER.error("Unable to get job scheduler.");
    }

    public static void stop(@NonNull Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) jobScheduler.cancel(JOB_ID);
        else LOGGER.error("Unable to get job scheduler.");
    }

    public static final class SyncService extends JobService {

        private final SyncExecutor executor = new SyncExecutor();

        private JobParameters jobParams;

        private static class SyncExecutor extends AbstractExecutionThreadService {

            private final BlockingQueue<WeakReference<Context>> queue = new LinkedBlockingDeque<>();

            @NonNull
            private static Task<Void> uploadModels(@NonNull Context context, @NonNull List<WallpaperModel> models) {
                return Tasks.whenAll(models.stream()
                        .filter(model -> !model.isSynced())
                        .map(model -> DriveApiHelper.upload(context, model).addOnSuccessListener(driveFile -> {
                            model.setSynced(true);
                            model.update();
                        }))
                        .collect(Collectors.toList()));
            }

            @NonNull
            private static Task<Void> downloadModels(@NonNull Context context, @NonNull Set<String> ids) {
                return Tasks.whenAll(ids.stream().map(id -> {
                    try {
                        long longId = Long.parseLong(id);
                        WallpaperModel model = WallpaperModel.getModel(longId);
                        if (model != null) return null;

                        return DriveApiHelper.download(context, id)
                                .addOnSuccessListener(file -> {
                                    try {
                                        WallpaperModel.addModel(context, longId, file, true);
                                    } catch (IOException e) {
                                        LOGGER.error(e.getLocalizedMessage(), e);
                                    }
                                });
                    } catch (NumberFormatException e) {
                        LOGGER.error(e.getLocalizedMessage(), e);
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList()));
            }

            private static void removeModels(@NonNull List<WallpaperModel> models, @NonNull Set<String> ids) {
                models.stream()
                        .filter(model -> {
                            for (String id : ids) {
                                if (id.equals(String.valueOf(model.getId()))) return false;
                            }
                            return true;
                        })
                        .forEach(model -> {
                            LOGGER.info("{} model has been removed on drive. It will be removed on local, too.", model.getId());
                            WallpaperModel.removeModel(model);
                        });
            }

            @Override
            protected void run() {
                while (isRunning()) {
                    try {
                        Context context = queue.take().get();
                        if (context == null) return;

                        final CountDownLatch countDownLatch = new CountDownLatch(1);

                        List<WallpaperModel> models = WallpaperModel.getModels();
                        uploadModels(context, models)
                                .continueWithTask(task -> DriveApiHelper.fetchDriveIds(context))
                                .continueWithTask(task -> {
                                    removeModels(models, task.getResult());
                                    return task;
                                })
                                .continueWithTask(task -> downloadModels(context, task.getResult()))
                                .addOnSuccessListener(result -> {
                                    LOGGER.info("Drive sync finished.");
                                    EventBus.getDefault().post(new SyncEvent(true));
                                    countDownLatch.countDown();
                                })
                                .addOnFailureListener(e -> {
                                    LOGGER.error("Failed to sync drive files.", e);
                                    EventBus.getDefault().post(new SyncEvent(false));
                                    countDownLatch.countDown();
                                });

                        countDownLatch.await();

                    } catch (InterruptedException e) {
                        LOGGER.error(e.getLocalizedMessage(), e);
                    }
                }
            }
        }

        @Override
        public void onCreate() {
            super.onCreate();
            executor.startAsync();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            executor.stopAsync();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            final String action = intent.getAction();
            if (INTENT_ACTION_SYNC.equals(action)) {
                executor.queue.add(new WeakReference<>(getApplicationContext()));
            }

            return START_NOT_STICKY;
        }

        @Override
        public boolean onStartJob(JobParameters jobParameters) {
            LOGGER.debug("sync service is started with {}", jobParameters.getJobId());
            EventBus.getDefault().register(this);
            jobParams = jobParameters;
            return executor.queue.add(new WeakReference<>(getApplicationContext()));
        }

        @Override
        public boolean onStopJob(JobParameters jobParameters) {
            LOGGER.debug("sync service is stopped with {}", jobParameters.getJobId());
            return true;
        }

        @Subscribe
        public void onSyncEvent(@NonNull SyncEvent event) {
            LOGGER.debug("sync event received.");
            jobFinished(jobParams, true);
            EventBus.getDefault().unregister(this);
            jobParams = null;
        }
    }

    public static final class SyncEvent {

        public final boolean success;

        private SyncEvent(boolean success) {
            this.success = success;
        }
    }
}
