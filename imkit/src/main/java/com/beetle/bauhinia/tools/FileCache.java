package com.beetle.bauhinia.tools;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by houxh on 14-12-3.
 */
public class FileCache {
    public static FileCache instance = new FileCache();
    public static FileCache getInstance() {
        return instance;
    }

    private File dir;
    public void setDir(File dir) {
        this.dir = dir;
    }

    public void storeFile(String key, InputStream inputStream) throws IOException {
        File file = new File(this.dir, getFileName(key));
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        IOUtils.copy(inputStream, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public void storeByteArray(String key, ByteArrayOutputStream byteStream) throws IOException {
        File file = new File(this.dir, getFileName(key));
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        byteStream.writeTo(fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public void removeFile(String key) {
        File file = new File(this.dir, getFileName(key));
        file.delete();
    }

    public String getCachedFilePath(String key) {
        File file = new File(this.dir, getFileName(key));
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        return null;
    }

    public boolean isCached(String key) {
        return getCachedFilePath(key) != null;
    }

    private String getFileName(String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(key.getBytes());
            byte[] m = md5.digest();
            return BinAscii.bin2Hex(m);
        } catch (NoSuchAlgorithmException e) {
            //opps
            System.exit(1);
            return "";
        }
    }
}
