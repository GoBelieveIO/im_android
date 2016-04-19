package com.beetle.bauhinia.gallery.tool;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * @author hillwind
 */
public class StorageUtils {

    public static final String ALBUM_NAME = "ChatAlbum";

    public static String getAlbumDir(Context context) {
        String dir = getAppDir(context) + File.separator + ALBUM_NAME;
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            FileUtils.mkdirIfNeed(dirFile);
        }
        return dir;
    }

    public static String getAppDir(Context context) {
        String dir = getCacheDir(context) + File.separator + context.getPackageName();
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            FileUtils.mkdirIfNeed(dirFile);
        }
        return dir;
    }

    public static String getCacheDir(Context context) {
        String cachePath;
        if (isSDCardAvailable()) {
            cachePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }

        return cachePath;
    }

    public static boolean isSDCardAvailable() {
        String sdStatus = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(sdStatus);
    }

}
