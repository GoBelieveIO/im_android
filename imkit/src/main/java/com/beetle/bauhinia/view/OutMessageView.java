/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.imkit.R;
import com.squareup.picasso.Picasso;

import java.beans.PropertyChangeEvent;

public class OutMessageView extends MessageRowView {

    public OutMessageView(Context context, MessageContent.MessageType type) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.chat_container_right, this);
        ViewGroup group = (ViewGroup)findViewById(R.id.content);
        addContent(type, group);
        if (type == MessageContent.MessageType.MESSAGE_TEXT) {
            //文本消息的背景直接设置到textview上
            group.setBackgroundResource(0);
            group.setPadding(0, 0, 0, 0);
        }
    }


    public void setMessage(IMessage msg) {
        super.setMessage(msg);
        if (msg.isOutgoing) {
            if (msg.isFailure()) {
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.VISIBLE);
                ProgressBar sendingProgressBar = (ProgressBar) findViewById(R.id.sending_progress_bar);
                sendingProgressBar.setVisibility(View.GONE);
            } else if (msg.isAck()) {
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.GONE);
                ProgressBar sendingProgressBar = (ProgressBar) findViewById(R.id.sending_progress_bar);
                sendingProgressBar.setVisibility(View.GONE);
            } else if (msg.getUploading()) {
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.GONE);
                ProgressBar sendingProgressBar = (ProgressBar) findViewById(R.id.sending_progress_bar);
                sendingProgressBar.setVisibility(View.GONE);
            } else {
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.GONE);
                ProgressBar sendingProgressBar = (ProgressBar) findViewById(R.id.sending_progress_bar);
                sendingProgressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals("failure") ||
                event.getPropertyName().equals("ack") ||
                event.getPropertyName().equals("uploading")||
                event.getPropertyName().equals("flags")) {
            if (this.message.isFailure()) {
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.VISIBLE);
                ProgressBar sendingProgressBar = (ProgressBar) findViewById(R.id.sending_progress_bar);
                sendingProgressBar.setVisibility(View.GONE);
            } else if (this.message.isAck()) {
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.GONE);
                ProgressBar sendingProgressBar = (ProgressBar) findViewById(R.id.sending_progress_bar);
                sendingProgressBar.setVisibility(View.GONE);
            } else if (this.message.getUploading()) {
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.GONE);
                ProgressBar sendingProgressBar = (ProgressBar) findViewById(R.id.sending_progress_bar);
                sendingProgressBar.setVisibility(View.GONE);
            } else {
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.GONE);
                ProgressBar sendingProgressBar = (ProgressBar) findViewById(R.id.sending_progress_bar);
                sendingProgressBar.setVisibility(View.VISIBLE);
            }
        } else if (event.getPropertyName().equals("senderName")) {
            ImageView headerView = (ImageView)findViewById(R.id.header);
            String avatar = this.message.getSenderAvatar();
            if (headerView != null && !TextUtils.isEmpty(avatar)) {
                Picasso.with(getContext())
                        .load(avatar)
                        .placeholder(R.drawable.avatar_contact)
                        .into(headerView);
            }
        }
    }


}
