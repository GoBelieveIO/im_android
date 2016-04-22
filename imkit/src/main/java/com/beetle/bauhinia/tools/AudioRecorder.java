package com.beetle.bauhinia.tools;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;

import android.util.Log;

import com.beetle.imkit.R;

public class AudioRecorder {
    private static final String TAG = "AudioRecorder";

    private Context mContext;
    private MediaPlayer mPlayerHintStart;
    private MediaPlayer mPlayerHintEnd;


    public AudioRecorder(Context context, String path) {
        this.pathName = path;
        mContext = context;
        mPlayerHintStart = MediaPlayer.create(context, R.raw.record_start);
        mPlayerHintEnd = MediaPlayer.create(context, R.raw.record_end);
    }

    private boolean start = false;

    private MediaRecorder recorder;
    private String pathName;

    public boolean isRecording() {
        return this.recorder != null;
    }

    public String getPathName() {
        return pathName;
    }
    public int getMaxAmplitude() {
        return recorder.getMaxAmplitude();
    }

    public void startRecord() {
        if (this.recorder != null) {
            return;
        }

        try {
            if (mPlayerHintStart != null) {
                mPlayerHintStart.release();
            }
            mPlayerHintStart = MediaPlayer.create(mContext,
                    R.raw.record_start);
            mPlayerHintStart.start();
            mPlayerHintStart
                    .setOnCompletionListener(new OnCompletionListener() {

                        @Override
                        public void onCompletion(MediaPlayer arg0) {

                        }
                    });

            start = true;
            try {
                Log.i(TAG, "start record");
                AudioRecorder.this.recorder = new MediaRecorder();

                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                recorder.setOutputFile(AudioRecorder.this.pathName);
                recorder.prepare();
                recorder.start();
            } catch (Exception e) {
                Log.e(TAG,
                        "Record start error:  " + e != null ? e
                                .getMessage() : "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecord() {
        if (this.recorder == null) {
            return;
        }

        this.recorder.stop();
        this.recorder.reset();
        this.recorder.release();
        this.recorder = null;

        if (start) {
            if (mPlayerHintEnd != null) {
                mPlayerHintEnd.release();
            }
            mPlayerHintEnd = MediaPlayer.create(mContext, R.raw.record_end);
            mPlayerHintEnd.start();
        }
    }
}
