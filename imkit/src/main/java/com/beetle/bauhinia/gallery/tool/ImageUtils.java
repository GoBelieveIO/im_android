package com.beetle.bauhinia.gallery.tool;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by hillwind
 */
public class ImageUtils {

    public static String savePNGImage(Context context, String srcPath, Bitmap bitmap) throws IOException {
        if (TextUtils.isEmpty(srcPath) || bitmap == null) {
            return null;
        }
        String fileName = Md5FileNameUtils.getMd5FileName(srcPath);
        return savePNGImageWithFileName(context, fileName, bitmap);
    }

    public static String savePNGImageWithFileName(Context context, String fileName, Bitmap bitmap) throws IOException {
        if (TextUtils.isEmpty(fileName) || bitmap == null) {
            return null;
        }

        String filePath = null;
        FileOutputStream fileOutputStream = null;
        try {
            File imgFile = new File(StorageUtils.getAlbumDir(context), fileName + ".png");
            fileOutputStream = new FileOutputStream(imgFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            updateMediaStore(context, imgFile);
            filePath = imgFile.getAbsolutePath();
        } finally {
            Closeables.closeQuietly(fileOutputStream);
        }

        return filePath;
    }

    public static void updateMediaStore(Context context, File savedFile) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(savedFile);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

}
