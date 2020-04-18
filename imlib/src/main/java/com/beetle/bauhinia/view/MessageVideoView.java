/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.view;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import android.widget.TextView;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.Video;
import com.beetle.imlib.R;
import com.squareup.picasso.Picasso;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.beans.PropertyChangeEvent;

public class MessageVideoView extends MessageContentView {

    protected ProgressBar uploadingProgressBar;
    protected View maskView;
    protected TextView durationView;
    protected View playView;

    public MessageVideoView(Context context) {
        super(context);
        int contentLayout = R.layout.chat_content_video;
        inflater.inflate(contentLayout, this);
        uploadingProgressBar = (ProgressBar)findViewById(R.id.progress_bar);

        maskView = findViewById(R.id.mask);
        durationView = (TextView)findViewById(R.id.duration);
        playView = findViewById(R.id.play);
    }

    public void setMessage(IMessage msg) {
        super.setMessage(msg);

        ImageView imageView = (ImageView)findViewById(R.id.image);

        Video video = (Video)msg.content;
        String url = video.thumbnail;
        if (msg.secret) {
            if (!url.startsWith("file:")) {
                url = "";
            }
            Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.image_download_fail)
                    .into(imageView);
        } else {
            Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.image_download_fail)
                    .into(imageView);
        }

        Period period = new Period().withSeconds(video.duration);
        PeriodFormatter periodFormatter = new PeriodFormatterBuilder()
                .appendMinutes()
                .appendSeparator(":")
                .appendSeconds()
                .appendSuffix("\"")
                .toFormatter();
        durationView.setText(periodFormatter.print(period));


        boolean uploading = msg.getUploading();
        boolean downloading = msg.getDownloading();
        if (uploading || downloading) {
            if (maskView != null) {
                maskView.setVisibility(View.VISIBLE);
            }
            uploadingProgressBar.setVisibility(View.VISIBLE);
            playView.setVisibility(View.GONE);
        } else {
            if (maskView != null) {
                maskView.setVisibility(View.GONE);
            }
            uploadingProgressBar.setVisibility(View.GONE);
            playView.setVisibility(View.VISIBLE);
        }

        requestLayout();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getPropertyName().equals("uploading") ||
                event.getPropertyName().equals("downloading")) {
            boolean uploading = this.message.getUploading();
            boolean downloading = this.message.getDownloading();
            if (uploading || downloading) {
                if (maskView != null) {
                    maskView.setVisibility(View.VISIBLE);
                }
                uploadingProgressBar.setVisibility(View.VISIBLE);
                playView.setVisibility(View.GONE);
            } else {
                if (maskView != null) {
                    maskView.setVisibility(View.GONE);
                }
                uploadingProgressBar.setVisibility(View.GONE);
                playView.setVisibility(View.VISIBLE);
            }
        }

        ImageView imageView = (ImageView)findViewById(R.id.image);
        if (event.getPropertyName().equals("downloading")) {
            if (message.secret) {
                String url = ((Video) message.content).thumbnail;
                if (!url.startsWith("file:")) {
                    url = "";
                }
                Picasso.get()
                        .load(url)
                        .placeholder(R.drawable.image_download_fail)
                        .into(imageView);
            }
        }
    }
}
