package com.fin10.android.mywallpaper.drive;

import android.content.Context;
import android.support.annotation.NonNull;

import com.fin10.android.mywallpaper.model.WallpaperModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

final class DriveApiHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriveApiHelper.class);

    private static final String ROOT_FOLDER_TITLE = "My Wallpaper";

    private DriveApiHelper() {
    }

    @NonNull
    private static Task<DriveFolder> getFolder(@NonNull DriveResourceClient client) {
        Task<DriveFolder> rootFolderTask = client.getRootFolder();

        return rootFolderTask
                .continueWithTask(task -> {
                    DriveFolder root = rootFolderTask.getResult();
                    return client.queryChildren(root,
                            new Query.Builder()
                                    .addFilter(Filters.and(
                                            Filters.eq(SearchableField.TITLE, ROOT_FOLDER_TITLE),
                                            Filters.eq(SearchableField.TRASHED, false)
                                    ))
                                    .build());
                })
                .continueWithTask(task -> {
                    MetadataBuffer buffer = task.getResult();
                    if (buffer.getCount() == 0) {
                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(ROOT_FOLDER_TITLE)
                                .build();

                        return client.createFolder(rootFolderTask.getResult(), changeSet);
                    } else {
                        return Tasks.forResult(buffer.get(0).getDriveId().asDriveFolder());
                    }
                });
    }

    @NonNull
    static Task<DriveFile> upload(@NonNull Context context, @NonNull WallpaperModel model) {
        LOGGER.info("Uploading a model: {}", model.getId());

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            throw new IllegalStateException("Signed account not founds.");
        }

        DriveResourceClient client = Drive.getDriveResourceClient(context, account);
        return getFolder(client)
                .continueWithTask(task -> {
                    DriveFolder appFolder = task.getResult();
                    return client
                            .queryChildren(appFolder,
                                    new Query.Builder()
                                            .addFilter(Filters.and(
                                                    Filters.eq(SearchableField.TITLE, String.valueOf(model.getId())),
                                                    Filters.eq(SearchableField.TRASHED, false)
                                            ))
                                            .build())
                            .continueWithTask(queryChildrenTask -> {
                                MetadataBuffer buffer = queryChildrenTask.getResult();
                                try {
                                    return buffer.getCount() == 0 ?
                                            Tasks.forResult(appFolder) :
                                            Tasks.forException(new IllegalArgumentException(model.getId() + " already exists."));
                                } finally {
                                    buffer.release();
                                }
                            });
                })
                .continueWithTask(task -> {
                    DriveFolder appFolder = task.getResult();
                    return client.createContents()
                            .continueWithTask(createContentsTask -> {
                                DriveContents contents = createContentsTask.getResult();
                                FileUtils.copyFile(new File(model.getImagePath()), contents.getOutputStream());

                                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                        .setTitle(String.valueOf(model.getId()))
                                        .setMimeType("image/png")
                                        .build();

                                return client.createFile(appFolder, changeSet, contents);
                            });
                })
                .addOnSuccessListener(v -> LOGGER.info("Model uploaded: {}", model.getId()))
                .addOnFailureListener(e -> LOGGER.error("Failed to upload model: {}.", model.getId(), e));
    }

    @NonNull
    static Task<Set<String>> fetchDriveIds(@NonNull Context context) {
        LOGGER.info("Fetching drive files...");

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            throw new IllegalStateException("Signed account not founds.");
        }

        DriveResourceClient client = Drive.getDriveResourceClient(context, account);
        return getFolder(client)
                .continueWithTask(task -> client.queryChildren(task.getResult(),
                        new Query.Builder()
                                .addFilter(Filters.eq(SearchableField.TRASHED, false))
                                .build()))
                .continueWithTask(task -> {
                    MetadataBuffer buffer = task.getResult();
                    try {
                        Set<String> ids = StreamSupport.stream(buffer.spliterator(), false)
                                .map(Metadata::getTitle).collect(Collectors.toSet());
                        return Tasks.forResult(ids);
                    } finally {
                        buffer.release();
                    }
                })
                .addOnSuccessListener(ids -> LOGGER.info("{} files fetched.", ids.size()))
                .addOnFailureListener(e -> LOGGER.error("Failed to fetch drive ids.", e));
    }

    @NonNull
    static Task<File> download(@NonNull Context context, @NonNull String id) {
        LOGGER.info("Downloading a drive file: {}", id);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            throw new IllegalStateException("Signed account not founds.");
        }

        DriveResourceClient client = Drive.getDriveResourceClient(context, account);
        return getFolder(client)
                .continueWithTask(task -> client.queryChildren(task.getResult(),
                        new Query.Builder()
                                .addFilter(Filters.and(
                                        Filters.eq(SearchableField.TITLE, id),
                                        Filters.eq(SearchableField.TRASHED, false)
                                ))
                                .build()))
                .continueWithTask(task -> {
                    MetadataBuffer buffer = task.getResult();
                    try {
                        if (buffer.getCount() == 0)
                            return Tasks.forException(new IllegalArgumentException(id + " not founds."));
                        else return Tasks.forResult(buffer.get(0).getDriveId().asDriveFile());
                    } finally {
                        buffer.release();
                    }
                })
                .continueWithTask(task -> client.openFile(task.getResult(), DriveFile.MODE_READ_ONLY))
                .continueWithTask(task -> {
                    DriveContents contents = task.getResult();
                    File output = new File(context.getCacheDir().getAbsolutePath() + "/" + id + ".png");
                    FileUtils.copyInputStreamToFile(contents.getInputStream(), output);
                    return Tasks.forResult(output);
                })
                .addOnSuccessListener(imagePath -> LOGGER.info("Downloaded drive file to {}", imagePath))
                .addOnFailureListener(e -> LOGGER.error("Failed to download drive file: {}.", id, e));
    }

    static void dismiss(@NonNull Context context, @NonNull List<WallpaperModel> models) {
        LOGGER.info("{} models will be dismissed from drive.", models.size());

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            throw new IllegalStateException("Signed account not founds.");
        }

        Set<String> ids = models.stream()
                .map(WallpaperModel::getId)
                .map(String::valueOf)
                .collect(Collectors.toSet());

        DriveResourceClient client = Drive.getDriveResourceClient(context, account);
        getFolder(client)
                .continueWithTask(task -> client.queryChildren(task.getResult(),
                        new Query.Builder()
                                .addFilter(Filters.eq(SearchableField.TRASHED, false))
                                .build()))
                .continueWithTask(task -> {
                    MetadataBuffer buffer = task.getResult();
                    try {
                        Set<DriveResource> files = StreamSupport.stream(buffer.spliterator(), false)
                                .filter(data -> ids.contains(data.getTitle()))
                                .map(data -> data.getDriveId().asDriveResource())
                                .collect(Collectors.toSet());
                        return Tasks.whenAll(files.stream().map(client::delete).collect(Collectors.toSet()))
                                .continueWithTask(v -> Tasks.forResult(files.size()));
                    } finally {
                        buffer.release();
                    }
                })
                .addOnSuccessListener(count -> LOGGER.info("{} models are dismissed from drive.", count))
                .addOnFailureListener(e -> LOGGER.error("Failed to dismiss models from drive.", e));
    }
}
