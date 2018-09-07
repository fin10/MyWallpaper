package com.fin10.android.mywallpaper.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.NotNull;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

@Table(database = WallpaperDatabase.class)
public final class WallpaperModel extends BaseModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(WallpaperModel.class);

    @Column(name = "_id")
    @PrimaryKey
    private long id;

    @Column(name = "image_path")
    @NotNull
    private String imagePath;

    @Column(name = "synced", defaultValue = "false")
    private boolean synced;

    @NonNull
    public static List<WallpaperModel> getModels() {
        return SQLite.select()
                .from(WallpaperModel.class)
                .orderBy(WallpaperModel_Table._id, false)
                .queryList();
    }

    @Nullable
    public static WallpaperModel getModel(long id) {
        return SQLite.select()
                .from(WallpaperModel.class)
                .where(WallpaperModel_Table._id.eq(id))
                .querySingle();
    }

    @NonNull
    public static WallpaperModel addModel(@NonNull File file) {
        return addModel(System.currentTimeMillis(), file, false);
    }

    @NonNull
    public static WallpaperModel addModel(long id, @NonNull File file, boolean synced) {
        WallpaperModel model = new WallpaperModel();
        model.setId(id);
        model.setSynced(synced);
        model.setImagePath(file.getAbsolutePath());

        model.insert();
        EventBus.getDefault().post(new AddEvent(model));

        return model;
    }

    public static void removeModel(@NonNull WallpaperModel model) {
        File file = new File(model.getImagePath());
        boolean result = file.delete();
        if (!result) {
            LOGGER.error("failed to delete. {}", model.getImagePath());
            return;
        }

        long id = model.getId();
        model.delete();
        EventBus.getDefault().post(new RemoveEvent(id));
    }

    @Nullable
    public static WallpaperModel sample() {
        List<WallpaperModel> models = getModels();
        if (models.isEmpty()) return null;

        Collections.shuffle(models);
        return models.get(0);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    @Override
    public boolean update() {
        boolean result = super.update();
        if (result) EventBus.getDefault().post(new UpdateEvent(this));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WallpaperModel model = (WallpaperModel) o;

        return getId() == model.getId();
    }

    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    public static final class AddEvent {

        @NonNull
        public final WallpaperModel model;

        private AddEvent(@NonNull WallpaperModel model) {
            this.model = model;
        }
    }

    public static final class UpdateEvent {

        @NonNull
        public final WallpaperModel model;

        private UpdateEvent(@NonNull WallpaperModel model) {
            this.model = model;
        }
    }

    public static final class RemoveEvent {

        public final long id;

        private RemoveEvent(long id) {
            this.id = id;
        }
    }
}
