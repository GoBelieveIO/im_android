/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.tools;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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

    public void storeFile(String key, byte[] data) throws IOException {
        File file = new File(this.dir, getFileName(key));
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(data);
        fileOutputStream.flush();
        fileOutputStream.close();
    }


    public void removeFile(String key) {
        File file = new File(this.dir, getFileName(key));
        file.delete();
    }

    public String getCachedFilePath(String key) {
        File file = new File(this.dir, getFileName(key));
        return file.getAbsolutePath();
    }

    public boolean isCached(String key) {
        File file = new File(this.dir, getFileName(key));
        return file.exists();
    }

    private String getFileName(String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(key.getBytes());
            byte[] m = md5.digest();
            String name = BinAscii.bin2Hex(m);
            String ext = "";
            int pos = -1;
            try {
                URL url = new URL(key);
                pos = url.getPath().lastIndexOf(".");
                if (pos != -1) {
                    ext = url.getPath().substring(pos);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                pos = key.lastIndexOf(".");
                if (pos != -1) {
                    ext = key.substring(pos);
                }
            }
            return name + ext;
        } catch (NoSuchAlgorithmException e) {
            //opps
            System.exit(1);
            return "";
        }
    }
}
