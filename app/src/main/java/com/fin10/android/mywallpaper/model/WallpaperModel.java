package com.fin10.android.mywallpaper.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.fin10.android.mywallpaper.FileUtils;
import com.fin10.android.mywallpaper.Log;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.NotNull;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

@Table(database = WallpaperDatabase.class)
public final class WallpaperModel extends BaseModel {

    @Column(name = "_id", getterName = "getId")
    @PrimaryKey(autoincrement = true)
    long mId;

    @Column(name = "user_id", defaultValue = "\"" + UserId.DEVICE + "\"")
    String mUserId;

    @Column(name = "source", getterName = "getSource")
    String mSource;

    @Column(name = "image_path", getterName = "getImagePath")
    @NotNull
    String mImagePath;

    @Column(name = "creation_time", getterName = "getCreationTime")
    @NotNull
    long mCreationTime;

    @Column(name = "applied_count", defaultValue = "0")
    long mAppliedCount;

    public static void init(@NonNull Context context) {
        FlowManager.init(new FlowConfig.Builder(context).build());
    }

    @NonNull
    public static List<WallpaperModel> getModels() {
        return SQLite.select()
                .from(WallpaperModel.class)
                .orderBy(WallpaperModel_Table.creation_time, false)
                .queryList();
    }

    @NonNull
    public static List<WallpaperModel> getModels(@NonNull String userId) {
        return SQLite.select()
                .from(WallpaperModel.class)
                .where(WallpaperModel_Table.user_id.eq(userId))
                .orderBy(WallpaperModel_Table.creation_time, false)
                .queryList();
    }

    @Nullable
    public static WallpaperModel getModel(long id) {
        return SQLite.select()
                .from(WallpaperModel.class)
                .where(WallpaperModel_Table._id.eq(id))
                .querySingle();
    }

    @Nullable
    public static WallpaperModel getModel(@NonNull String userId, long id) {
        return SQLite.select()
                .from(WallpaperModel.class)
                .where(WallpaperModel_Table.user_id.eq(userId))
                .and(WallpaperModel_Table._id.eq(id))
                .querySingle();
    }

    @Nullable
    public static WallpaperModel getModel(@NonNull String source) {
        return SQLite.select()
                .from(WallpaperModel.class)
                .where(WallpaperModel_Table.source.eq(source))
                .querySingle();
    }

    @NonNull
    public static WallpaperModel addModel(@NonNull String userId, @NonNull String source, @NonNull String path) {
        WallpaperModel model = new WallpaperModel();
        model.mCreationTime = System.currentTimeMillis();
        model.mUserId = userId;
        model.mSource = source;
        model.mImagePath = path;
        model.insert();

        EventBus.getDefault().post(new AddEvent(model));

        return model;
    }

    @Nullable
    static WallpaperModel addModel(@NonNull Context context, @NonNull String source, @NonNull File file) {
        try {
            WallpaperModel model = new WallpaperModel();
            model.mCreationTime = System.currentTimeMillis();
            model.mSource = source;
            model.mImagePath = FileUtils.write(context, new FileInputStream(file), model.mCreationTime + ".png");
            if (!TextUtils.isEmpty(model.mImagePath)) {
                model.insert();
                EventBus.getDefault().post(new AddEvent(model));
                return model;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void removeModel(@NonNull WallpaperModel model) {
        File file = new File(model.getImagePath());
        boolean result = file.delete();
        if (!result) {
            Log.e("failed to delete. %s", model.getImagePath());
            return;
        }

        long id = model.getId();
        model.delete();
        EventBus.getDefault().post(new RemoveEvent(id));
    }

    public long getId() {
        return mId;
    }

    public long getCreationTime() {
        return mCreationTime;
    }

    @NonNull
    public String getImagePath() {
        return mImagePath;
    }

    public long getAppliedCount() {
        return mAppliedCount;
    }

    public void update(@NonNull String userId, @NonNull String source) {
        mUserId = userId;
        mSource = source;
        update();
    }

    @Override
    public void update() {
        super.update();
        EventBus.getDefault().post(new UpdateEvent(this));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WallpaperModel model = (WallpaperModel) o;

        return mId == model.mId;

    }

    @Override
    public int hashCode() {
        return (int) (mId ^ (mId >>> 32));
    }

    @NonNull
    public String getSource() {
        return mSource != null ? mSource : "";
    }

    void incrementAppliedCount() {
        ++mAppliedCount;
        update();
    }

    public static final class UserId {

        public static final String DEVICE = "device";

        private UserId() {
        }
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
