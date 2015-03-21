
package com.beetle.bauhinia.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;
import com.beetle.imkit.R;
/**
 * 录音、播音的操作都是异步的，不用另起线程来执行
 * 
 * @author mk
 */
public class AudioUtil{
    private static final String TAG = "AudioUtil";

    private Context mContext;

    private MediaPlayer mPlayer;

    private MediaPlayer mPlayerEnd;

    private static MediaPlayer mDurationPlayer = new MediaPlayer();

    private ArrayList<OnCompletionListener> mOnCompletionListeners = new ArrayList<OnCompletionListener>();

    private ArrayList<OnStopListener> mOnStopListeners = new ArrayList<OnStopListener>();

    private boolean isRecording = false;

    private long mRecordTimeStamp = 0;

    public final static int STOP_REASON_RECORDING = 0;

    public final static int STOP_REASON_OTHER = 1;

    // 正在播放的文件名，多播放控制用
    public String playingFile = "";

    public AudioUtil(Context context) {
        mContext = context;
        mPlayer = new MediaPlayer();

    }

    /**
     * 播放结束时调用此函数
     * 
     * @param l
     */
    public void setOnCompletionListener(OnCompletionListener l) {
        mOnCompletionListeners.add(l);
    }

    public void setOnStopListener(OnStopListener l) {
        mOnStopListeners.add(l);
    }

    public static long getAudioDuration(String fileName) throws IOException {
        long duration = 0;
        if (mDurationPlayer == null) {
            return duration;
        }
        try {
            mDurationPlayer.reset();
            mDurationPlayer.setDataSource(fileName);
            mDurationPlayer.prepare();
            duration = mDurationPlayer.getDuration();
            mDurationPlayer.stop();
        } catch (IOException e) {
            Log.e(TAG, "IOException:" + e.getMessage());
            throw e;
        } catch (IllegalStateException e) {
            Log.e(TAG, "getAudioDuration start playing IllegalStateException");
            throw e;
        }
        return duration;
    }

    /**
     * 开始播放录音
     * 
     * @param path 录音文件的路径
     * @return START_SUCCESS:播放成功；（TODO：文件不存在，编码不支持等）
     * @throws IOException
     * @throws IllegalStateException
     */
    public void startPlay(final String fileName) throws IllegalStateException, IOException {
        if (fileName == null) {
            Log.e(TAG, "file name is null");
            return;
        }
        playingFile = fileName;

        if (mPlayer == null) {
            return;
        }
        if (mPlayer.isPlaying()) { // 先停止当然的播放
            stopPlaying();
        }

        startPlaying(fileName);
    }

    /**
     * 手动停止播音（正常情况下会自己结束）
     */
    public void stopPlay() throws IllegalStateException {
        if (mPlayer != null && mPlayer.isPlaying()) {
            stopPlaying();
        }
    }

    /**
     * 是否正在播放录音
     * 
     * @return true-正在播放录音
     */
    public synchronized boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    /**
     * 释放录音，播音的资源。（可以在退出单个私聊界面的时侯释放，不必每次录音结束都调用。 释放完之后，这个实例将不可再用）
     */
    public void release() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }


    private void startPlaying(final String fileName)
            throws IllegalStateException, IOException {
        if (mPlayer == null) {
            return;
        }
        try {
            mPlayer.reset();
            mPlayer.setDataSource(fileName);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if (!am.isBluetoothA2dpOn() && !am.isWiredHeadsetOn()) {
                am.setSpeakerphoneOn(true);
            }

            am.setMode(AudioManager.STREAM_MUSIC);
            mPlayer.prepare();
            mPlayer.start();


            Log.i(TAG, "start play");
            OnCompletionListener mOnCompletionListener = new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer arg0) {
                    if (mPlayerEnd != null) {
                        mPlayerEnd.release();
                    }
                    mPlayerEnd = MediaPlayer.create(mContext, R.raw.play_end);
                    mPlayerEnd.start();
                    for (Iterator<OnCompletionListener> itr = mOnCompletionListeners
                            .iterator(); itr
                            .hasNext();) {
                        OnCompletionListener curr = itr.next();
                        curr.onCompletion(arg0);
                    }
                }
            };
            mPlayer.setOnCompletionListener(mOnCompletionListener);
        } catch (IOException e) {
            Log.e(TAG, "IOException");
            throw e;
        } catch (IllegalStateException e) {
            Log.e(TAG, "start playing IllegalStateException");
            throw e;
        }
    }

    private void stopPlaying() throws IllegalStateException {
        stopPlaying(AudioUtil.STOP_REASON_OTHER);
    }

    private void stopPlaying(int reason) throws IllegalStateException {
        if (mPlayer != null) {
            try {
                Log.i(TAG, "_stop play");
                mPlayer.stop();
                for (Iterator<OnStopListener> itr = mOnStopListeners.iterator(); itr
                        .hasNext();) {
                    OnStopListener curr = itr.next();
                    curr.onStop(reason);
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "stop playing IllegalStateException");
                throw e;
            }
        }
    }


    public interface OnStopListener {
        void onStop(int reason);
    }
}
