package com.beetle.bauhinia.gallery.tool;

import java.io.File;

/**
 * Created by hillwind
 */
public class FileUtils {

    public static void mkdirIfNeed(File file) {
        if (file != null && !file.exists()) {
            mkdirIfNeed(file.getParentFile());
            file.mkdir();
        }
    }

}
