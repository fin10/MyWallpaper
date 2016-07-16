package com.fin10.android.mywallpaper.drive;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.util.Pair;

import com.fin10.android.mywallpaper.FileUtils;
import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.model.WallpaperModel;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DriveApiHelper {

    private static final String TARGET_FOLDER_NAME = "My Wallpaper";

    private DriveApiHelper() {
    }

    @NonNull
    static GoogleApiClient createGoogleApiClient(@NonNull Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addScope(Drive.SCOPE_APPFOLDER)
                .build();
    }

    @WorkerThread
    @Nullable
    static String upload(@NonNull GoogleApiClient googleApiClient, @NonNull WallpaperModel model) {
        DriveFolder folder = getTargetFolder(googleApiClient, TARGET_FOLDER_NAME);
        if (folder == null) return null;

        DriveApi.MetadataBufferResult result = folder.queryChildren(googleApiClient,
                new Query.Builder()
                        .addFilter(Filters.eq(SearchableField.TITLE, String.valueOf(model.getCreationTime())))
                        .build())
                .await();
        try {
            if (!result.getStatus().isSuccess()) {
                Log.e(result.getStatus().toString());
                return null;
            }

            if (result.getMetadataBuffer().getCount() != 0) {
                Log.e("[%d] already exist.", model.getCreationTime());
                return null;
            }
        } finally {
            result.release();
        }

        DriveApi.DriveContentsResult contentsResult = Drive.DriveApi.newDriveContents(googleApiClient).await();
        if (!contentsResult.getStatus().isSuccess()) {
            Log.e(contentsResult.getStatus().toString());
            return null;
        }

        DriveContents contents = contentsResult.getDriveContents();
        ParcelFileDescriptor descriptor = contents.getParcelFileDescriptor();
        boolean success = FileUtils.write(model.getImagePath(), new FileOutputStream(descriptor.getFileDescriptor()));
        if (!success) return null;

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(String.valueOf(model.getCreationTime()))
                .setMimeType("image/png")
                .build();

        DriveFolder.DriveFileResult fileResult = folder.createFile(googleApiClient, changeSet, contents).await();
        if (!fileResult.getStatus().isSuccess()) {
            Log.e(fileResult.getStatus().toString());
            return null;
        }

        String id = fileResult.getDriveFile().getDriveId().toInvariantString();
        Log.d("[upload] %s", id);
        return id;
    }

    @WorkerThread
    @Nullable
    private static DriveFolder getTargetFolder(@NonNull GoogleApiClient googleApiClient, @NonNull String folderName) {
        DriveFolder root = Drive.DriveApi.getRootFolder(googleApiClient);
        DriveApi.MetadataBufferResult result = root.queryChildren(googleApiClient, new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, folderName))
                .addFilter(Filters.eq(SearchableField.TRASHED, false))
                .build())
                .await();

        try {
            if (!result.getStatus().isSuccess()) {
                Log.e(result.getStatus().toString());
                return null;
            }

            MetadataBuffer buffer = result.getMetadataBuffer();
            if (buffer.getCount() == 0) {
                return createTargetFolder(googleApiClient, root, folderName);
            } else {
                Metadata data = buffer.get(0);
                return data.getDriveId().asDriveFolder();
            }
        } finally {
            result.release();
        }
    }

    @WorkerThread
    @Nullable
    private static DriveFolder createTargetFolder(@NonNull GoogleApiClient googleApiClient, @NonNull DriveFolder parent, @NonNull String folderName) {
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(folderName)
                .build();
        DriveFolder.DriveFolderResult result = parent.createFolder(googleApiClient, changeSet).await();
        if (!result.getStatus().isSuccess()) {
            Log.e(result.getStatus().toString());
            return null;
        }

        return result.getDriveFolder();
    }

    @NonNull
    static String getUserId(@NonNull GoogleApiClient googleApiClient) {
        DriveFolder root = Drive.DriveApi.getRootFolder(googleApiClient);
        return root.getDriveId().toInvariantString();
    }

    @WorkerThread
    @NonNull
    static List<Pair<String, String>> getWallpaperIds(@NonNull GoogleApiClient googleApiClient) {
        DriveFolder root = getTargetFolder(googleApiClient, TARGET_FOLDER_NAME);
        if (root == null) return Collections.emptyList();

        DriveApi.MetadataBufferResult children = root.queryChildren(googleApiClient,
                new Query.Builder()
                        .addFilter(Filters.eq(SearchableField.TRASHED, false))
                        .build())
                .await();
        if (!children.getStatus().isSuccess()) {
            Log.e(children.getStatus().toString());
            return Collections.emptyList();
        }

        MetadataBuffer buffer = children.getMetadataBuffer();
        try {
            List<Pair<String, String>> datas = new ArrayList<>();
            for (Metadata data : buffer) {
                Log.d("[%s] getWebContentLink:%s", data.getTitle(), data.getWebContentLink());
                DriveId driveId = data.getDriveId();
                datas.add(Pair.create(driveId.toInvariantString(), driveId.encodeToString()));
            }

            return datas;
        } finally {
            buffer.release();
        }
    }

    @WorkerThread
    @NonNull
    static String download(@NonNull GoogleApiClient googleApiClient, @NonNull String id) {
        Log.d("[download] %s", id);
        DriveId driveId = DriveId.decodeFromString(id);
        DriveFile driveFile = driveId.asDriveFile();
        DriveApi.DriveContentsResult contentsResult = driveFile.open(googleApiClient, DriveFile.MODE_READ_ONLY, null).await();
        if (!contentsResult.getStatus().isSuccess()) {
            Log.e(contentsResult.getStatus().toString());
            return "";
        }

        return FileUtils.write(googleApiClient.getContext(), contentsResult.getDriveContents().getInputStream(), id + ".png");
    }

    @WorkerThread
    static void sync(@NonNull GoogleApiClient googleApiClient) {
        Status status = Drive.DriveApi.requestSync(googleApiClient).await();
        Log.d(status.toString());
    }

    @WorkerThread
    static boolean dismiss(@NonNull GoogleApiClient googleApiClient, @NonNull List<String> ids) {
        DriveFolder root = getTargetFolder(googleApiClient, TARGET_FOLDER_NAME);
        if (root == null) return false;

        DriveApi.MetadataBufferResult children = root.queryChildren(googleApiClient,
                new Query.Builder()
                        .addFilter(Filters.eq(SearchableField.TRASHED, false))
                        .build())
                .await();
        if (!children.getStatus().isSuccess()) {
            Log.e(children.getStatus().toString());
            return false;
        }

        MetadataBuffer buffer = children.getMetadataBuffer();
        try {
            for (Metadata data : buffer) {
                DriveId driveId = data.getDriveId();
                if (ids.contains(driveId.toInvariantString())) {
                    Status result = driveId.asDriveFile().trash(googleApiClient).await();
                    Log.d(result.toString());
                }
            }

            return true;
        } finally {
            buffer.release();
        }
    }
}
