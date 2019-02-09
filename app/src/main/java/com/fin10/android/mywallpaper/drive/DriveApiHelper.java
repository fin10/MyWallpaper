package com.fin10.android.mywallpaper.drive;

import android.content.Context;

import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.model.WallpaperModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;

final class DriveApiHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriveApiHelper.class);
    private static final ThreadPoolExecutor EXECUTOR;

    private static final String ROOT_FOLDER = "appDataFolder";

    static {
        int numCores = Runtime.getRuntime().availableProcessors();
        EXECUTOR = new ThreadPoolExecutor(numCores * 2, numCores * 2,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    private DriveApiHelper() {
    }

    @NonNull
    private static Drive getDrive(@NonNull Context context, @NonNull GoogleSignInAccount account) {
        final HttpTransport httpTransport = new NetHttpTransport();
        final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        final HttpRequestInitializer initializer = GoogleAccountCredential.usingOAuth2(
                context,
                account.getGrantedScopes().stream().map(Scope::toString).collect(Collectors.toSet())
        ).setSelectedAccount(account.getAccount());

        return new Drive.Builder(httpTransport, jsonFactory, initializer)
                .setApplicationName(context.getString(R.string.app_name))
                .build();
    }

    @NonNull
    static Task<File> upload(@NonNull Context context, @NonNull WallpaperModel model) {
        LOGGER.info("Uploading a model: {}", model.getId());

        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            throw new IllegalStateException("Signed account not founds.");
        }

        final Drive service = getDrive(context, account);

        return Tasks.call(EXECUTOR, () -> {
            final FileList fileList = service.files().list().setQ(String.format("name = '%s' and trashed = false", model.getId())).execute();
            if (!fileList.getFiles().isEmpty()) {
                throw new IllegalArgumentException(model.getId() + " already exists in drive");
            }

            final File content = new File();
            content.setParents(Collections.singletonList(ROOT_FOLDER));
            content.setName(String.valueOf(model.getId()));

            final FileContent mediaContent = new FileContent("image/png", new java.io.File(model.getImagePath()));
            return service.files().create(content, mediaContent).execute();
        })
                .addOnSuccessListener(v -> LOGGER.info("Model uploaded: {}", model.getId()))
                .addOnFailureListener(e -> LOGGER.error("Failed to upload model: {}.", model.getId(), e));
    }

    @NonNull
    static Task<Set<String>> fetchDriveIds(@NonNull Context context) {
        LOGGER.info("Fetching drive files...");

        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            throw new IllegalStateException("Signed account not founds.");
        }

        final Drive service = getDrive(context, account);

        return Tasks.call(EXECUTOR, () -> {
            final FileList fileList = service.files().list().setSpaces(ROOT_FOLDER).setQ("trashed = false").execute();
            return fileList.getFiles().stream().map(File::getName).collect(Collectors.toSet());
        })
                .addOnSuccessListener(ids -> LOGGER.info("{} files fetched.", ids.size()))
                .addOnFailureListener(e -> LOGGER.error("Failed to fetch drive ids.", e));
    }

    @NonNull
    static Task<java.io.File> download(@NonNull Context context, @NonNull String id) {
        LOGGER.info("Downloading a drive file: {}", id);

        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            throw new IllegalStateException("Signed account not founds.");
        }

        final Drive service = getDrive(context, account);

        return Tasks.call(EXECUTOR, () -> {
            final FileList fileList = service.files().list().setSpaces(ROOT_FOLDER).setQ(String.format("name = '%s' and trashed = false", id)).execute();
            final Optional<File> file = fileList.getFiles().stream().findFirst();
            if (!file.isPresent()) {
                throw new FileNotFoundException(id + " not exists in drive");
            }

            final java.io.File output = new java.io.File(context.getCacheDir().getAbsolutePath() + "/" + id + ".png");
            service.files().get(file.get().getId()).executeMediaAndDownloadTo(new FileOutputStream(output));

            return output;
        })
                .addOnSuccessListener(imagePath -> LOGGER.info("Downloaded drive file to {}", imagePath))
                .addOnFailureListener(e -> LOGGER.error("Failed to download drive file: {}.", id, e));
    }

    static void dismiss(@NonNull Context context, @NonNull List<WallpaperModel> models) {
        LOGGER.info("{} models will be dismissed from drive.", models.size());

        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            throw new IllegalStateException("Signed account not founds.");
        }

        final Set<String> ids = models.stream()
                .map(WallpaperModel::getId)
                .map(String::valueOf)
                .collect(Collectors.toSet());

        final Drive service = getDrive(context, account);

        Tasks.call(EXECUTOR, () -> {
            final FileList fileList = service.files().list().setSpaces(ROOT_FOLDER).setQ("trashed = false").execute();
            return fileList.getFiles().stream()
                    .filter(file -> ids.contains(file.getName()))
                    .map(file -> {
                        try {
                            service.files().delete(file.getId()).execute();
                            return 1;
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .mapToInt(v -> v).sum();
        })
                .addOnSuccessListener(count -> LOGGER.info("{} models are dismissed from drive.", count))
                .addOnFailureListener(e -> LOGGER.error("Failed to dismiss models from drive.", e));
    }
}
