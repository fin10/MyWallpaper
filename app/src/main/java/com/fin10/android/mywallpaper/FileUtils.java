package com.fin10.android.mywallpaper;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class FileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    private static String sRootPath;

    private FileUtils() {
    }

    @NonNull
    private static synchronized String getRootPath(@NonNull Context context) {
        if (sRootPath == null) {
            try (XmlResourceParser parser = context.getResources().getXml(R.xml.filepaths)) {
                for (; !TextUtils.equals(parser.getName(), "files-path"); parser.next()) ;
                String path = parser.getAttributeValue(null, "path");
                sRootPath = context.getFilesDir() + "/" + path;
                File file = new File(sRootPath);
                boolean result = file.exists() || file.mkdirs();
                if (!result) LOGGER.error("failed to make root folder, {}", sRootPath);
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
        }

        return sRootPath;
    }

    @NonNull
    public static String write(@NonNull Context context, @NonNull InputStream inputStream, @NonNull String name) {
        File file = new File(getRootPath(context) + "/" + name);
        try {
            if (file.createNewFile()) {
                copy(inputStream, new FileOutputStream(file));
                return file.getAbsolutePath();
            } else {
                LOGGER.error("{} already exists.", name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static boolean write(@NonNull String filePath, @NonNull OutputStream output) {
        try {
            copy(new FileInputStream(filePath), output);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copy(@NonNull InputStream input, @NonNull OutputStream output) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(input);
            bos = new BufferedOutputStream(output);
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
                input.close();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
