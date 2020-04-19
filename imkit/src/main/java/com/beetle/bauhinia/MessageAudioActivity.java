package com.beetle.bauhinia;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.Audio;
import com.beetle.bauhinia.tools.AudioRecorder;
import com.beetle.bauhinia.tools.AudioUtil;
import com.beetle.bauhinia.tools.DeviceUtil;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.imkit.R;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.TimerTask;

public class MessageAudioActivity extends MessageBaseActivity {
    IMessage playingMessage;

    //录音相关
    protected Handler mHandler = new Handler();
    protected java.util.Timer sixtySecondsTimer;
    protected java.util.Timer recordingTimer;
    protected AlertDialog alertDialog;

    protected ImageView recordingImageBG;

    protected ImageView recordingImage;

    protected TextView recordingText;

    protected Date mBegin;

    protected String recordFileName;

    protected AudioRecorder audioRecorder;
    protected AudioUtil audioUtil;


    protected class VolumeTimerTask extends TimerTask {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MessageAudioActivity.this.refreshVolume();
                }
            });
        }
    }


    protected void showReleaseToCancelHint() {
        recordingText.setText(getString(R.string.release_to_cancel));
        recordingText.setBackgroundResource(R.drawable.ease_recording_text_hint_bg);
    }

    protected void showMoveUpToCancelHint() {
        recordingText.setText(getString(R.string.move_up_to_cancel));
        recordingText.setBackgroundColor(Color.TRANSPARENT);
    }

    protected void showRecordDialog() {
        AlertDialog.Builder builder;

        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(
                R.layout.conversation_recording_dialog,
                (ViewGroup) findViewById(R.id.conversation_recording));

        recordingImage = (ImageView) layout
                .findViewById(R.id.conversation_recording_range);
        recordingImageBG = (ImageView) layout
                .findViewById(R.id.conversation_recording_white);

        recordingText = (TextView) layout
                .findViewById(R.id.conversation_recording_text);

        showMoveUpToCancelHint();

        builder = new AlertDialog.Builder(this);
        alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().setContentView(layout);
    }

    protected void refreshVolume() {
        if (!this.audioRecorder.isRecording()) {
            return;
        }

        int max = this.audioRecorder.getMaxAmplitude();

        if (max != 0) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) recordingImage
                    .getLayoutParams();
            float scale = max / 7000.0f;
            if (scale < 0.3) {
                recordingImage
                        .setImageResource(R.drawable.record_red);
            } else {
                recordingImage
                        .setImageResource(R.drawable.record_green);
            }
            if (scale > 1) {
                scale = 1;
            }
            int height = recordingImageBG.getHeight()
                    - (int) (scale * recordingImageBG.getHeight());
            params.setMargins(0, 0, 0, -1 * height);
            recordingImage.setLayoutParams(params);

            ((View) recordingImage).scrollTo(0, height);
            // Log.i(TAG, "max amplitude: " + max);
            /**
             * 倒计时提醒
             */
            Date now = new Date();
            long between = (mBegin.getTime() + 60000)
                    - now.getTime();
            if (between < 10000) {
                int second = (int) (Math.floor((between / 1000)));
                if (second == 0) {
                    second = 1;
                }
                recordingText.setText("还剩: " + second + "秒");
            }
        }
    }

    protected void startRecord() {
        if (audioUtil.isPlaying()) {
            audioUtil.stopPlay();
        }

        mBegin = new Date();
        //删除上次录音生成的文件内容
        new File(recordFileName).delete();
        audioRecorder.startRecord();
        sixtySecondsTimer = new java.util.Timer();
        sixtySecondsTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "recording end by timeout");
                        MessageAudioActivity.this.stopRecord();
                    }
                });
            }
        }, 60000);

        recordingTimer = new java.util.Timer();
        recordingTimer.schedule(new MessageAudioActivity.VolumeTimerTask(), 0, 100);

        showRecordDialog();
    }

    protected void discardRecord() {
        // stop sixty seconds limit
        if (sixtySecondsTimer != null) {
            sixtySecondsTimer.cancel();
            sixtySecondsTimer = null;
        }
        // stop volume task
        if (recordingTimer != null) {
            recordingTimer.cancel();
            recordingTimer = null;
        }
        if (alertDialog != null) {
            alertDialog.dismiss();
        }

        if (MessageAudioActivity.this.audioRecorder.isRecording()) {
            MessageAudioActivity.this.audioRecorder.stopRecord();
        }
    }

    protected void stopRecord() {
        // stop sixty seconds limit
        if (sixtySecondsTimer != null) {
            sixtySecondsTimer.cancel();
            sixtySecondsTimer = null;
        }
        // stop volume task
        if (recordingTimer != null) {
            recordingTimer.cancel();
            recordingTimer = null;
        }
        if (alertDialog != null) {
            alertDialog.dismiss();
        }

        if (MessageAudioActivity.this.audioRecorder.isRecording()) {
            MessageAudioActivity.this.audioRecorder.stopRecord();
            String tfile = audioRecorder.getPathName();
            MessageAudioActivity.this.sendAudioMessage(tfile);
        }
    }



    protected void play(IMessage message) {
        Audio audio = (Audio) message.content;
        Log.i(TAG, "url:" + audio.url);
        if (FileCache.getInstance().isCached(audio.url)) {
            try {
                if (audioRecorder.isRecording()) {
                    audioRecorder.stopRecord();
                }
                if (playingMessage != null && playingMessage == message) {
                    //停止播放
                    audioUtil.stopPlay();
                    playingMessage.setPlaying(false);
                    playingMessage = null;
                } else {
                    if (playingMessage != null) {
                        audioUtil.stopPlay();
                        playingMessage.setPlaying(false);
                    }
                    audioUtil.startPlay(FileCache.getInstance().getCachedFilePath(audio.url));
                    playingMessage = message;
                    message.setPlaying(true);
                    if (!message.isListened() && !message.isOutgoing) {
                        message.setListened(true);
                        markMessageListened(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}