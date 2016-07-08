package com.fin10.android.mywallpaper.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.fin10.android.mywallpaper.Log;
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DriveApiHelper {

    private static final String TARGET_FOLDER_NAME = "My Wallpaper";

    @Nullable
    static String upload(@NonNull GoogleApiClient googleApiClient, @NonNull WallpaperModel model) {
        DriveFolder folder = getTargetFolder(googleApiClient, TARGET_FOLDER_NAME);
        if (folder == null) return null;

        DriveApi.DriveContentsResult contentsResult = Drive.DriveApi.newDriveContents(googleApiClient).await();
        if (!contentsResult.getStatus().isSuccess()) {
            Log.e(contentsResult.getStatus().toString());
            return null;
        }

        DriveContents contents = contentsResult.getDriveContents();
        ParcelFileDescriptor descriptor = contents.getParcelFileDescriptor();

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(new File(model.mImagePath)));
            bos = new BufferedOutputStream(new FileOutputStream(descriptor.getFileDescriptor()));
            byte[] buf = new byte[4096];
            bis.read(buf);
            do {
                bos.write(buf);
            } while (bis.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(String.valueOf(model.mCreationTime))
                .setMimeType("image/png")
                .build();

        DriveFolder.DriveFileResult fileResult = folder.createFile(googleApiClient, changeSet, contents).await();
        if (!fileResult.getStatus().isSuccess()) {
            Log.e(fileResult.getStatus().toString());
            return null;
        }

        return fileResult.getDriveFile().getDriveId().toInvariantString();
    }

    @Nullable
    static DriveFolder getTargetFolder(@NonNull GoogleApiClient googleApiClient, @NonNull String folderName) {
        DriveFolder root = Drive.DriveApi.getRootFolder(googleApiClient);
        DriveApi.MetadataBufferResult result = root.queryChildren(googleApiClient, new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, folderName))
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

    static void download(@NonNull GoogleApiClient googleApiClient, @NonNull String id) {
        Log.d("%s", id);
        DriveId driveId = DriveId.decodeFromString(id);
        DriveFile driveFile = driveId.asDriveFile();
        DriveApi.DriveContentsResult contentsResult = driveFile.open(googleApiClient, DriveFile.MODE_READ_ONLY, null).await();
        if (!contentsResult.getStatus().isSuccess()) {
            Log.e(contentsResult.getStatus().toString());
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeStream(contentsResult.getDriveContents().getInputStream());
        WallpaperModel.addModel(driveId.toInvariantString(), bitmap);
    }

    static void sync(@NonNull GoogleApiClient googleApiClient) {
        Status status = Drive.DriveApi.requestSync(googleApiClient).await();
        Log.d(status.toString());
    }
}
