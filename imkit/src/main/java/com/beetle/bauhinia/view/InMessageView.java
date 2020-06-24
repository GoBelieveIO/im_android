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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import java.beans.PropertyChangeEvent;
import java.util.List;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.imkit.R;

import co.lujun.androidtagview.TagContainerLayout;

public class InMessageView extends MessageRowView {
    protected TextView nameView;

    TagContainerLayout tagContainer;
    boolean isShowReply;//control topicView&replyButton

    public InMessageView(Context context, MessageContent.MessageType type, boolean isShowUserName) {
        this(context, type, isShowUserName, true);
    }

    public InMessageView(Context context, MessageContent.MessageType type, boolean isShowUserName, boolean isShowReply) {
        super(context);
        this.isShowReply = isShowReply;

        LayoutInflater inflater = LayoutInflater.from(context);

        View convertView = inflater.inflate(R.layout.chat_container_left, this);
        nameView = (TextView)convertView.findViewById(R.id.name);
        if (isShowUserName) {
            nameView.setVisibility(View.VISIBLE);
        } else {
            nameView.setVisibility(View.GONE);
        }
        replyButton = findViewById(R.id.reply);
        topicView = findViewById(R.id.topic);

        ViewGroup group = (ViewGroup)findViewById(R.id.content);
        addContent(type, group);

        contentFrame = findViewById(R.id.content_frame);
        tagContainer = findViewById(R.id.tags);
    }

    public void setMessage(IMessage msg) {
        super.setMessage(msg);
        nameView.setText(msg.getSenderName());
        updateReplyButton();
        if (isShowReply && (!TextUtils.isEmpty(this.message.getReference()) || this.message.getReferenceCount() > 0)) {
            topicView.setVisibility(View.VISIBLE);
        } else {
            topicView.setVisibility(View.GONE);
        }

        List<String> tags = msg.getTags();
        tagContainer.setTags(tags);
        if (tags.size() == 0) {
            tagContainer.setVisibility(View.GONE);
        } else {
            tagContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals("senderName")) {
            nameView.setText(this.message.getSenderName());
        } else if (event.getPropertyName().equals("senderAvatar")) {
            ImageView headerView = (ImageView) findViewById(R.id.header);
            String avatar = this.message.getSenderAvatar();
            if (headerView != null && !TextUtils.isEmpty(avatar)) {
                Picasso.get()
                        .load(avatar)
                        .placeholder(R.drawable.avatar_contact)
                        .into(headerView);
            }
        } else if (event.getPropertyName().equals("referenceCount")) {
            updateReplyButton();
            if (isShowReply && (!TextUtils.isEmpty(this.message.getReference()) || this.message.getReferenceCount() > 0)) {
                topicView.setVisibility(View.VISIBLE);
            } else {
                topicView.setVisibility(View.GONE);
            }
        } else if (event.getPropertyName().equals("tags")) {
            List<String> tags = this.message.getTags();
            tagContainer.setTags(tags);
            if (tags.size() == 0) {
                tagContainer.setVisibility(View.GONE);
            } else {
                tagContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    void updateReplyButton() {
        if (this.message.getReferenceCount() > 0 && isShowReply) {
            replyButton.setVisibility(View.VISIBLE);
            String s = String.format("%d条回复", this.message.getReferenceCount());
            replyButton.setText(s);
        } else {
            replyButton.setVisibility(View.GONE);
        }
    }

}
