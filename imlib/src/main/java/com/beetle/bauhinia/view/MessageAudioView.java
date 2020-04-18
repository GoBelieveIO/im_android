/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.view;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.beetle.bauhinia.db.message.Audio;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import java.beans.PropertyChangeEvent;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imlib.R;

public class MessageAudioView extends MessageContentView {
    public MessageAudioView(Context context) {
        super(context);
        inflater.inflate(R.layout.chat_content_audio, this);
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

        final Audio audio = (Audio) msg.content;
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
