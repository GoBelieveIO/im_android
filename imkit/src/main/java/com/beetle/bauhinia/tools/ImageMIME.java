package com.beetle.bauhinia.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Created by tsung on 11/10/14.
 */
public class ImageMIME {
    public static String getMimeType(File file) {
        try {
            InputStream inputStream = new FileInputStream(file);
            if (isValidPNG(inputStream)) {
                return "image/png";
            } else if (isValidJPEG(inputStream, file.length())) {
                return "image/jpeg";
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Check if the image is a PNG. The first eight bytes of a PNG file always
     * contain the following (decimal) values: 137 80 78 71 13 10 26 10 / Hex:
     * 89 50 4e 47 0d 0a 1a 0a
     */
    public static boolean isValidPNG(InputStream is) {
        try {
            byte[] b = new byte[8];
            is.read(b, 0, 8);
            if (Arrays.equals(b, new BigInteger("89504e470d0a1a0a", 16).toByteArray())) {
                return false;
            }
        } catch (Exception e) {
            //Ignore
            return false;
        }
        return true;
    }

    /**
     * Check if the image is a JPEG. JPEG image files begin with FF D8 and end
     * with FF D9
     */
    public static boolean isValidJPEG(InputStream is, long size) {
        try {
            byte[] b = new byte[2];
            is.read(b, 0, 2);
            // check first 2 bytes:
            if ((b[0]&0xff) != 0xff || (b[1]&0xff) != 0xd8) {
                return false;
            }
            // check last 2 bytes:
            is.skip(size-4);
            is.read(b, 0, 2);
            if ((b[0]&0xff) != 0xff || (b[1]&0xff) != 0xd9) {
                return false;
            }
        } catch (Exception e) {
            // Ignore
            return false;
        }
        return true;
    }
}
