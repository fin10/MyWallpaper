package com.fin10.android.mywallpaper.drive;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.model.WallpaperModel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public final class SyncScheduler {

    private static final String INTENT_ACTION_SYNC = "sync";
    private static final String INTENT_ACTION_UPLOAD = "upload";
    private static final String INTENT_ACTION_DISMISS = "dismiss";
    private static final String EXTRA_MODEL_ID = "extra_model_id";
    private static final String EXTRA_MODEL_ID_LIST = "extra_model_id_list";

    private static final int JOB_ID = 1;

    public static void sync(@NonNull Context context) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(INTENT_ACTION_SYNC);
        context.startService(intent);
    }

    public static void upload(@NonNull Context context, @NonNull WallpaperModel model) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(INTENT_ACTION_UPLOAD);
        intent.putExtra(EXTRA_MODEL_ID, model.getId());
        context.startService(intent);
    }

    public static void dismiss(@NonNull Context context, @NonNull List<WallpaperModel> models) {
        ArrayList<String> ids = new ArrayList<>(models.size());
        for (WallpaperModel model : models) {
            String source = model.getSource();
            if (!TextUtils.isEmpty(source)) ids.add(source);
        }

        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(INTENT_ACTION_DISMISS);
        intent.putExtra(EXTRA_MODEL_ID_LIST, ids);
        context.startService(intent);
    }

    public static void start(@NonNull Context context) {
        Log.d("[start]");
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, ScheduleService.class))
                .setPersisted(true)
                .setPeriodic(DateUtils.DAY_IN_MILLIS)
                .setRequiresCharging(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .build();

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(jobInfo);
    }

    public static void stop(@NonNull Context context) {
        Log.d("[stop]");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JOB_ID);
    }

    public static final class SyncService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private GoogleApiClient mGoogleApiClient;
        private Intent mIntent;

        @Override
        public void onCreate() {
            Log.d("[onCreate]");
            super.onCreate();
            mGoogleApiClient = DriveApiHelper.createGoogleApiClient(this);
            mGoogleApiClient.registerConnectionCallbacks(this);
            mGoogleApiClient.registerConnectionFailedListener(this);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            mIntent = intent;
            mGoogleApiClient.connect();
            return START_NOT_STICKY;
        }

        @Override
        public void onDestroy() {
            Log.d("[onDestroy]");
            super.onDestroy();
            mGoogleApiClient.disconnect();
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d("[onConnected]");
            final String action = mIntent.getAction();
            new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... voids) {
                    DriveApiHelper.sync(mGoogleApiClient);
                    if (INTENT_ACTION_SYNC.equals(action)) {
                        List<Pair<String, String>> ids = DriveApiHelper.getWallpaperIds(mGoogleApiClient);
                        List<WallpaperModel> models = WallpaperModel.getSyncedModels();
                        List<WallpaperModel> removeItems = new ArrayList<>();
                        for (WallpaperModel model : models) {
                            boolean found = false;
                            for (Pair<String, String> id : ids) {
                                if (model.getSource().equals(id.first)) {
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) removeItems.add(model);
                        }

                        for (WallpaperModel model : removeItems) {
                            model.delete();
                            Log.d("[%s] removed.", model.getCreationTime());
                        }

                        models = WallpaperModel.getLocalModels();
                        for (WallpaperModel model : models) {
                            String id = DriveApiHelper.upload(mGoogleApiClient, model);
                            if (!TextUtils.isEmpty(id)) {
                                model.setSource(id);
                                model.update();
                            }
                        }

                        for (Pair<String, String> id : ids) {
                            WallpaperModel model = WallpaperModel.getModel(id.first);
                            if (model == null) {
                                String path = DriveApiHelper.getPath(mGoogleApiClient, id.second);
                                if (!TextUtils.isEmpty(path)) {
                                    WallpaperModel.addModel(id.first, path);
                                }
                            }
                        }
                    } else if (INTENT_ACTION_UPLOAD.equals(action)) {
                        long modelId = mIntent.getLongExtra(EXTRA_MODEL_ID, -1);
                        Log.d("modelId:%d", modelId);
                        WallpaperModel model = WallpaperModel.getModel(modelId);
                        if (model != null) {
                            String id = DriveApiHelper.upload(mGoogleApiClient, model);
                            if (!TextUtils.isEmpty(id)) {
                                model.setSource(id);
                                model.update();
                            }
                        }
                    } else if (INTENT_ACTION_DISMISS.equals(action)) {
                        List<String> removed = mIntent.getStringArrayListExtra(EXTRA_MODEL_ID_LIST);
                        Log.d("removed:%d", removed.size());
                        return DriveApiHelper.dismiss(mGoogleApiClient, removed);
                    }

                    return true;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    EventBus.getDefault().post(new SyncEvent(result));
                    stopSelf();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d("[onConnectionSuspended] cause:%d", cause);
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d("[onConnectionFailed] %s", connectionResult.toString());
            EventBus.getDefault().post(new SyncEvent(false));
            stopSelf();
        }
    }

    public static final class ScheduleService extends JobService {

        private JobParameters mJobParams;

        @Override
        public boolean onStartJob(JobParameters jobParameters) {
            Log.d("[onStartJob] id:%d", jobParameters.getJobId());
            EventBus.getDefault().register(this);
            mJobParams = jobParameters;
            sync(this);
            return true;
        }

        @Override
        public boolean onStopJob(JobParameters jobParameters) {
            Log.d("[onStopJob] id:%d", jobParameters.getJobId());
            EventBus.getDefault().unregister(this);
            mJobParams = null;
            return true;
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onSyncEvent(@NonNull SyncEvent event) {
            jobFinished(mJobParams, true);
        }
    }

    public static final class SyncEvent {

        public final boolean success;

        private SyncEvent(boolean success) {
            this.success = success;
        }
    }
}
