/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaExtractor;
import android.media.MediaMetadataRetriever;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.text.TextUtils;
import java.io.IOException;

public class VideoUtil {

    public static final String MIMETYPE_AUDIO_AAC = "audio/mp4a-latm";
    public static final String MIMETYPE_VIDEO_AVC = "video/avc";

    public static class Metadata {
        public int width;
        public int height;
        public int duration;

        public String videoMime;
        public String audioMime;
    }

    public static Bitmap createVideoThumbnail(String filePath) {
        MediaMetadataRetriever instance = new MediaMetadataRetriever();
        try {
            instance.setDataSource(filePath);
            byte[] data = instance.getEmbeddedPicture();
            if (data != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (bitmap != null) return bitmap;
            } else {
                return instance.getFrameAtTime();
            }

            instance.release();
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
            ex.printStackTrace();
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
            ex.printStackTrace();
        } finally {
            instance.release();
        }
        return null;
    }

    public static Metadata getVideoMetadata(String filePath) {
        Metadata size = new Metadata();
        MediaMetadataRetriever instance = new MediaMetadataRetriever();
        try {
            instance.setDataSource(filePath);
            String w = instance.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = instance.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String d = instance.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            size.width = Integer.valueOf(w);
            size.height = Integer.valueOf(h);
            size.duration = Integer.valueOf(d);
            instance.release();
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
            ex.printStackTrace();
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
            ex.printStackTrace();
        } finally {
            instance.release();
        }

        getTrackInfo(filePath, size);
        return size;
    }

    private static boolean getTrackInfo(String filePath, Metadata meta) {
        MediaExtractor instance = new MediaExtractor();
        try {
            instance.setDataSource(filePath);
            int trackCount = instance.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = instance.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    if (TextUtils.isEmpty(meta.videoMime)) {
                        meta.videoMime = mime;
                    }
                } else if (mime.startsWith("audio/")) {
                    if (TextUtils.isEmpty(meta.audioMime)) {
                        meta.audioMime = mime;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            instance.release();
        }
    }


    public static boolean isAcc(String mime) {
        return mime != null && mime.equalsIgnoreCase(MIMETYPE_AUDIO_AAC);
    }
    public static boolean isH264(String mime) {
        return mime != null && mime.equalsIgnoreCase(MIMETYPE_VIDEO_AVC);
    }
}
