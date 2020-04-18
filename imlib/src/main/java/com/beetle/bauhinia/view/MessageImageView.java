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

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.Image;
import com.beetle.imlib.R;
import com.squareup.picasso.Picasso;

import java.beans.PropertyChangeEvent;

public class MessageImageView extends MessageContentView {

    protected ProgressBar uploadingProgressBar;
    protected View maskView;

    public MessageImageView(Context context) {
        super(context);
        int contentLayout = R.layout.chat_content_image;
        inflater.inflate(contentLayout, this);
        uploadingProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
        maskView = findViewById(R.id.mask);
    }

    public void setMessage(IMessage msg) {
        super.setMessage(msg);

        ImageView imageView = (ImageView)findViewById(R.id.image);

        String url = ((Image) msg.content).url;
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
                    .load(url + "@256w_256h_0c")
                    .placeholder(R.drawable.image_download_fail)
                    .into(imageView);
        }

        boolean uploading = msg.getUploading();
        boolean downloading = msg.getDownloading();
        if (uploading || downloading) {
            if (maskView != null) {
                maskView.setVisibility(View.VISIBLE);
            }
            uploadingProgressBar.setVisibility(View.VISIBLE);
        } else {
            if (maskView != null) {
                maskView.setVisibility(View.GONE);
            }
            uploadingProgressBar.setVisibility(View.GONE);
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
            } else {
                if (maskView != null) {
                    maskView.setVisibility(View.GONE);
                }
                uploadingProgressBar.setVisibility(View.GONE);
            }
        }

        ImageView imageView = (ImageView)findViewById(R.id.image);
        if (event.getPropertyName().equals("downloading")) {
            if (message.secret) {
                String url = ((Image) message.content).url;
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
