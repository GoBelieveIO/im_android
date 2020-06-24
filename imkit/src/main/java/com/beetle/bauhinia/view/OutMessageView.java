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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.tools.DisplayUtils;
import com.beetle.imkit.R;
import com.squareup.picasso.Picasso;

import java.beans.PropertyChangeEvent;
import java.util.List;

import co.lujun.androidtagview.TagContainerLayout;

public class OutMessageView extends MessageRowView {

    public Button readedButton;
    TagContainerLayout tagContainer;
    boolean isShowReply;

    public OutMessageView(Context context, MessageContent.MessageType type, boolean isShowReply, boolean isShowReaded) {
        super(context);
        this.isShowReply = isShowReply;

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.chat_container_right, this);
        readedButton = findViewById(R.id.readed);
        replyButton = findViewById(R.id.reply);
        topicView = findViewById(R.id.topic);

        if (isShowReaded) {
            readedButton.setVisibility(View.VISIBLE);
        } else {
            readedButton.setVisibility(View.GONE);
        }

        ViewGroup group = (ViewGroup)findViewById(R.id.content);
        addContent(type, group);

        contentFrame = findViewById(R.id.content_frame);
        tagContainer = findViewById(R.id.tags);
    }

    public void setMessage(IMessage msg) {
        super.setMessage(msg);
        readedButton.setTag(msg);

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
        updateReadedButton();

        updateReplyButton();
        ViewGroup group = (ViewGroup)findViewById(R.id.content);
        if (isShowReply && this.message.getReferenceCount() > 0) {
            //保证readedButton和replyButton之间有一定的间隔
            group.setMinimumWidth(DisplayUtils.dp2px(getContext(), 160));
        } else if (isShowReply && !TextUtils.isEmpty(this.message.getReference())) {
            //保证readedButton和topicView之间有一定的间隔
            group.setMinimumWidth(DisplayUtils.dp2px(getContext(), 120));
        } else {
            group.setMinimumWidth(0);
        }
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

        } else if (event.getPropertyName().equals("senderAvatar")) {
            ImageView headerView = (ImageView) findViewById(R.id.header);
            String avatar = this.message.getSenderAvatar();
            if (headerView != null && !TextUtils.isEmpty(avatar)) {
                Picasso.get()
                        .load(avatar)
                        .placeholder(R.drawable.avatar_contact)
                        .into(headerView);
            }
        } else if (event.getPropertyName().equals("readed") || event.getPropertyName().equals("readerCount")) {
            updateReadedButton();
        } else if (event.getPropertyName().equals("referenceCount")) {
            updateReplyButton();
            ViewGroup group = (ViewGroup)findViewById(R.id.content);
            if (this.message.getReferenceCount() > 0) {
                group.setMinimumWidth(DisplayUtils.dp2px(getContext(), 160));
            } else {
                group.setMinimumWidth(0);
            }
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

    void updateReadedButton() {
        if (this.message.receiverCount > 0) {
            if (this.message.readerCount < this.message.receiverCount) {
                int unreadCount = this.message.receiverCount - this.message.readerCount;
                String s = String.format(getContext().getString(R.string.message_unread_count), unreadCount);
                readedButton.setText(s);
            } else {
                readedButton.setText(R.string.message_readed);
            }
        } else if (this.message.readerCount > 0) {
            String s = String.format(getContext().getString(R.string.message_readed_count), this.message.readerCount);
            readedButton.setText(s);
        } else {
            if (this.message.isReaded()) {
                readedButton.setText(R.string.message_readed);
            } else {
                readedButton.setText(R.string.message_unread);
            }
        }
    }

    void updateReplyButton() {
        if (this.message.getReferenceCount() > 0) {
            replyButton.setVisibility(View.VISIBLE);
            String s = String.format("%d条回复", this.message.getReferenceCount());
            replyButton.setText(s);
        } else {
            replyButton.setVisibility(View.GONE);
        }
    }

}
