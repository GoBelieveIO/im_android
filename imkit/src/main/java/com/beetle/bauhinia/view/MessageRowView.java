package com.beetle.bauhinia.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.beetle.bauhinia.db.message.MessageContent;
import com.squareup.picasso.Picasso;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;

public class MessageRowView extends FrameLayout implements PropertyChangeListener {
    protected IMessage message;
    protected MessageContentView contentView;
    protected View contentFrame;
    protected Button replyButton;
    protected ImageView topicView;

    public MessageRowView(Context context) {
        super(context);
    }

    protected void addContent(MessageContent.MessageType type, ViewGroup viewGroup) {
        Context context = getContext();
        MessageContentView v = null;
        switch (type) {
            case MESSAGE_TEXT:
                v = new MessageTextView(context);
                break;
            case MESSAGE_AUDIO:
                v = new MessageAudioView(context);
                break;
            case MESSAGE_IMAGE:
                v = new MessageImageView(context);
                break;
            case MESSAGE_LOCATION:
                v = new MessageLocationView(context);
                break;
            case MESSAGE_VOIP:
                v = new MessageVOIPView(context);
                break;
            case MESSAGE_LINK:
                v = new MessageLinkView(context);
                break;
            case MESSAGE_GROUP_NOTIFICATION:
            case MESSAGE_TIME_BASE:
            case MESSAGE_HEADLINE:
            case MESSAGE_GROUP_VOIP:
            case MESSAGE_REVOKE:
            case MESSAGE_ACK:
                v = new MessageNotificationView(context);
                break;
            case MESSAGE_FILE:
                v = new MessageFileView(context);
                break;
            case MESSAGE_VIDEO:
                v = new MessageVideoView(context);
                break;
            case MESSAGE_CLASSROOM:
                v = new MessageClassroomView(context);
                break;
            default:
                v = new MessageUnknownView(context);
                break;
        }

        if (v != null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER_VERTICAL);
            v.setLayoutParams(params);
            contentView = v;
            viewGroup.addView(v);
        }
    }


    public void setMessage(IMessage msg) {
        if (this.message != null) {
            this.message.removePropertyChangeListener(this);
        }
        this.message = msg;
        this.message.addPropertyChangeListener(this);

        this.contentView.setMessage(msg);
        this.contentView.setTag(this.message);
        if (this.contentFrame != null) {
            this.contentFrame.setTag(this.message);
        }
        if (this.replyButton != null) {
            this.replyButton.setTag(this.message);
        }

        ImageView headerView = (ImageView)findViewById(R.id.header);
        String avatar = msg.getSenderAvatar();
        if (headerView != null && !TextUtils.isEmpty(avatar)) {
            Picasso.get()
                    .load(avatar)
                    .placeholder(R.drawable.avatar_contact)
                    .into(headerView);
        }
    }

    public IMessage getMessage() {
        return message;
    }

    public View getContentView() {
        return contentView;
    }

    public View getContentFrame() {
        return contentFrame;
    }

    public Button getReplyButton() {
        return replyButton;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {

    }
}