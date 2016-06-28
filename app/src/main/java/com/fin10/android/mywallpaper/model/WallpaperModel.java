package com.fin10.android.mywallpaper.model;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.settings.SettingsFragment;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

@Table(database = WallpaperDatabase.class)
public final class WallpaperModel extends BaseModel {

    private static final List<OnEventListener> sListeners = new LinkedList<>();
    private static String ROOT_PATH;

    @Column(name = "_id", getterName = "getId")
    @PrimaryKey(autoincrement = true)
    long mId;

    @Column(name = "user_id", defaultValue = "\"device\"")
    String mUserId;

    @Column(name = "source")
    String mSource;

    @Column(name = "image_path", getterName = "getImagePath")
    String mImagePath;

    @Column(name = "creation_time")
    long mCreationTime;

    @Column(name = "applied_count", defaultValue = "0")
    long mAppliedCount;

    public static void init(@NonNull Context context) {
        FlowManager.init(new FlowConfig.Builder(context).build());
        XmlResourceParser parser = context.getResources().getXml(R.xml.filepaths);
        try {
            for (; !TextUtils.equals(parser.getName(), "files-path"); parser.next()) ;
            String path = parser.getAttributeValue(null, "path");
            ROOT_PATH = context.getFilesDir() + "/" + path;
            File file = new File(ROOT_PATH);
            if (!file.exists()) file.mkdir();
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        } finally {
            parser.close();
        }
    }

    public static void addEventListener(@NonNull OnEventListener listener) {
        synchronized (sListeners) {
            if (!sListeners.contains(listener)) {
                sListeners.add(listener);
            }
        }
    }

    public static void removeEventListener(@NonNull OnEventListener listener) {
        synchronized (sListeners) {
            sListeners.remove(listener);
        }
    }

    public static boolean isCurrentWallpaper(@NonNull Context context, @NonNull WallpaperModel model) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        long id = pref.getLong(context.getString(R.string.pref_key_current_wallpaper_id), -1);
        return id == model.getId();
    }

    static long getCount() {
        return SQLite.select()
                .from(WallpaperModel.class)
                .count();
    }

    @Nullable
    public static List<WallpaperModel> getModels() {
        return SQLite.select()
                .from(WallpaperModel.class)
                .orderBy(WallpaperModel_Table.creation_time, false)
                .queryList();
    }

    static boolean addModel(@NonNull Context context, @NonNull String source, @NonNull Bitmap bitmap) {
        FileOutputStream os = null;
        WallpaperModel model = new WallpaperModel();
        model.mCreationTime = System.currentTimeMillis();
        model.mSource = source;
        model.insert();

        try {
            File file = new File(ROOT_PATH, model.mId + ".png");
            Log.d("file:%s", file);
            os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();

            model.mImagePath = file.getAbsolutePath();
            model.update();

            long count = getCount();
            if (count == 2 && SettingsFragment.isAutoChangeEnabled(context)) {
                WallpaperChangeScheduler.start(context);
            }

            synchronized (sListeners) {
                for (OnEventListener listener : sListeners) {
                    listener.onAdded(model);
                }
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            model.delete();
        } finally {
            try {
                if (os != null) os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void removeModel(@NonNull final WallpaperModel model) {
        File file = new File(model.getImagePath());
        boolean result = file.delete();
        if (!result) {
            Log.e("failed to delete. %s", model.getImagePath());
            return;
        }

        model.delete();

        Handler handler = new Handler();
        synchronized (sListeners) {
            for (final OnEventListener listener : sListeners) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onRemoved(model.getId());
                    }
                });
            }
        }
    }

    public long getId() {
        return mId;
    }

    @NonNull
    public String getImagePath() {
        return mImagePath;
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

            synchronized (sListeners) {
                for (OnEventListener listener : sListeners) {
                    listener.onWallpaperChanged(mId);
                }
            }
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

    public interface OnEventListener {
        void onAdded(@NonNull WallpaperModel model);

        void onRemoved(long id);

        void onWallpaperChanged(long id);
    }
}
