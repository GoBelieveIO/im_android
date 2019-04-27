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
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.SystemClock;
import android.util.Log;
import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.format.MediaFormatExtraConstants;
import net.ypresto.androidtranscoder.format.MediaFormatPresets;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoUtil {
    public static class Metadata {
        public int width;
        public int height;
        public int duration;
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
        return size;
    }



    public static final String AUDIO_RECORDING_FILE_NAME = "audio_Capturing-190814-034638.422.wav"; // Input PCM file
    public static final String COMPRESSED_AUDIO_FILE_NAME = "convertedmp4.m4a"; // Output MP4/M4A file
    public static final String COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm";
    public static final int COMPRESSED_AUDIO_FILE_BIT_RATE = 64000; // 64kbps
    public static final int SAMPLING_RATE = 48000;
    public static final int BUFFER_SIZE = 48000;
    public static final int CODEC_TIMEOUT_IN_MS = 5000;

    private static final String TAG = "im";

    public static class AACMediaFormat implements MediaFormatStrategy {
        @Override
        public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
            return null;
        }

        @Override
        public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
            // Use original sample rate, as resampling is not supported yet.
            final MediaFormat format = MediaFormat.createAudioFormat(MediaFormatExtraConstants.MIMETYPE_AUDIO_AAC,
                    inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE), 1);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 12*1024);
            format.setString(MediaFormat.KEY_MIME, MediaFormatExtraConstants.MIMETYPE_AUDIO_AAC);
            return format;
        }
    }
    public static void convert(String inputPath, String outPath) {



    };


}
