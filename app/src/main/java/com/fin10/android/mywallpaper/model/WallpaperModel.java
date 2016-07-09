package com.fin10.android.mywallpaper.model;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.R;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.NotNull;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.greenrobot.eventbus.EventBus;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Table(database = WallpaperDatabase.class)
public final class WallpaperModel extends BaseModel {

    private static String ROOT_PATH;

    @Column(name = "_id", getterName = "getId")
    @PrimaryKey(autoincrement = true)
    long mId;

    @Column(name = "user_id", defaultValue = "\"device\"")
    String mUserId;

    @Column(name = "source")
    String mSource;

    @Column(name = "image_path", getterName = "getImagePath")
    @NotNull
    String mImagePath;

    @Column(name = "creation_time")
    @NotNull
    long mCreationTime;

    @Column(name = "applied_count", defaultValue = "0")
    long mAppliedCount;

    public static void init(@NonNull Context context) {
        FlowManager.init(new FlowConfig.Builder(context).build());
        try (XmlResourceParser parser = context.getResources().getXml(R.xml.filepaths)) {
            for (; !TextUtils.equals(parser.getName(), "files-path"); parser.next()) ;
            String path = parser.getAttributeValue(null, "path");
            ROOT_PATH = context.getFilesDir() + "/" + path;
            File file = new File(ROOT_PATH);
            if (!file.exists()) file.mkdir();
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isCurrentWallpaper(@NonNull Context context, @NonNull WallpaperModel model) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        long id = pref.getLong(context.getString(R.string.pref_key_current_wallpaper_id), -1);
        return id == model.getId();
    }

    @Nullable
    public static List<WallpaperModel> getModels() {
        return SQLite.select()
                .from(WallpaperModel.class)
                .orderBy(WallpaperModel_Table.creation_time, false)
                .queryList();
    }

    @Nullable
    static List<WallpaperModel> getLocalModels() {
        return SQLite.select()
                .from(WallpaperModel.class)
                .where(WallpaperModel_Table.source.like("http%"))
                .or(WallpaperModel_Table.source.like("content:%"))
                .or(WallpaperModel_Table.source.like("file:%"))
                .or(WallpaperModel_Table.source.isNull())
                .queryList();
    }

    @Nullable
    static WallpaperModel getModel(long id) {
        return SQLite.select()
                .from(WallpaperModel.class)
                .where(WallpaperModel_Table._id.eq(id))
                .querySingle();
    }

    @Nullable
    static WallpaperModel getModel(@NonNull String source) {
        return SQLite.select()
                .from(WallpaperModel.class)
                .where(WallpaperModel_Table.source.eq(source))
                .querySingle();
    }

    @Nullable
    static WallpaperModel addModel(@NonNull Bitmap bitmap) {
        return addModel(null, bitmap);
    }

    @Nullable
    static WallpaperModel addModel(@Nullable String source, @NonNull Bitmap bitmap) {
        FileOutputStream os = null;
        try {
            final WallpaperModel model = new WallpaperModel();
            model.mCreationTime = System.currentTimeMillis();
            model.mSource = source;

            File file = new File(ROOT_PATH, model.mCreationTime + ".png");
            Log.d("file:%s", file);
            os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();

            model.mImagePath = file.getAbsolutePath();
            model.insert();

            EventBus.getDefault().post(new AddEvent(model));

            return model;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static void removeModel(@NonNull final WallpaperModel model) {
        File file = new File(model.getImagePath());
        boolean result = file.delete();
        if (!result) {
            Log.e("failed to delete. %s", model.getImagePath());
            return;
        }

        model.delete();
        EventBus.getDefault().post(new RemoveEvent(model.getId()));
    }

    public long getId() {
        return mId;
    }

    @NonNull
    public String getImagePath() {
        return mImagePath;
    }

    public long getAppliedCount() {
        return mAppliedCount;
    }

    public void setAsWallpaper(@NonNull Context context) {
        InputStream is = null;
        try {
            is = new FileInputStream(mImagePath);
            WallpaperManager.getInstance(context).setStream(is);
            ++mAppliedCount;
            update();

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            pref.edit().putLong(context.getString(R.string.pref_key_current_wallpaper_id), mId).apply();

            EventBus.getDefault().post(new SetAsWallpaperEvent(mId));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
    public String getDriveId() {
        if (mSource.startsWith("http") || mSource.startsWith("content:") || mSource.startsWith("file:")) {
            return "";
        }

        return mSource;
    }

    public static final class AddEvent {

        @NonNull
        public final WallpaperModel model;

        private AddEvent(@NonNull WallpaperModel model) {
            this.model = model;
        }
    }

    public static final class RemoveEvent {

        public final long id;

        private RemoveEvent(long id) {
            this.id = id;
        }
    }

    public static final class SetAsWallpaperEvent {

        public final long id;

        private SetAsWallpaperEvent(long id) {
            this.id = id;
        }
    }

}
