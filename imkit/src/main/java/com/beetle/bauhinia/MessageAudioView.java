package com.beetle.bauhinia;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import java.beans.PropertyChangeEvent;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;

public class MessageAudioView extends MessageRowView {

    protected ProgressBar uploadingProgressBar;


    public MessageAudioView(Context context, boolean incomming, boolean isShowUserName) {
        super(context, incomming, isShowUserName);

        final int contentLayout;
        contentLayout = R.layout.chat_content_audio;

        ViewGroup group = (ViewGroup)findViewById(R.id.content);
        group.addView(inflater.inflate(contentLayout, group, false));
    }

    class AudioHolder  {
        ImageView control;
        TextView duration;
        ImageView listen;

        AudioHolder(View view) {
            control = (ImageView)view.findViewById(R.id.play_control);
            duration = (TextView)view.findViewById(R.id.duration);
            listen = (ImageView)view.findViewById(R.id.listen);
        }
    }

    @Override
    public void setMessage(IMessage msg) {
        super.setMessage(msg);

        boolean playing = message.getPlaying();
        View convertView = this;

        final IMessage.Audio audio = (IMessage.Audio) msg.content;
        AudioHolder audioHolder =  new AudioHolder(convertView);

        if (playing) {
            AnimationDrawable voiceAnimation;
            if (!msg.isOutgoing) {
                audioHolder.control.setImageResource(R.drawable.voice_from_icon);
            } else {
                audioHolder.control.setImageResource(R.drawable.voice_to_icon);
            }
            voiceAnimation = (AnimationDrawable) audioHolder.control.getDrawable();
            voiceAnimation.start();
        } else {
            if (!msg.isOutgoing) {
                audioHolder.control.setImageResource(R.drawable.ease_chatfrom_voice_playing);
            } else {
                audioHolder.control.setImageResource(R.drawable.ease_chatto_voice_playing);
            }
        }

        Period period = new Period().withSeconds((int) audio.duration);
        PeriodFormatter periodFormatter = new PeriodFormatterBuilder()
                .appendMinutes()
                .appendSeparator(":")
                .appendSeconds()
                .appendSuffix("\"")
                .toFormatter();
        audioHolder.duration.setText(periodFormatter.print(period));

        if (!msg.isListened() && !msg.isOutgoing) {
            audioHolder.listen.setVisibility(VISIBLE);
        } else {
            audioHolder.listen.setVisibility(GONE);
        }

        convertView.requestLayout();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getPropertyName().equals("playing")) {
            Log.i("gobelieve", "playing changed");
            boolean playing = this.message.getPlaying();
            AudioHolder audioHolder =  new AudioHolder(this);
            if (playing) {
                AnimationDrawable voiceAnimation;
                if (!this.message.isOutgoing) {
                    audioHolder.control.setImageResource(R.drawable.voice_from_icon);
                } else {
                    audioHolder.control.setImageResource(R.drawable.voice_to_icon);
                }
                voiceAnimation = (AnimationDrawable) audioHolder.control.getDrawable();
                voiceAnimation.start();
                Log.i("gobelieve", "start animation");
            } else {
                if (!this.message.isOutgoing) {
                    audioHolder.control.setImageResource(R.drawable.ease_chatfrom_voice_playing);
                } else {
                    audioHolder.control.setImageResource(R.drawable.ease_chatto_voice_playing);
                }
            }
        } else if (event.getPropertyName().equals("listened")) {
            AudioHolder audioHolder =  new AudioHolder(this);
            if (!this.message.isListened() && !this.message.isOutgoing) {
                audioHolder.listen.setVisibility(VISIBLE);
            } else {
                audioHolder.listen.setVisibility(GONE);
            }
        }
    }

}