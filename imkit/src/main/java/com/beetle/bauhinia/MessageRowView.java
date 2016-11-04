package com.beetle.bauhinia;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;
import com.squareup.picasso.Picasso;

public class MessageRowView extends FrameLayout implements PropertyChangeListener {

    protected Context context;

    protected IMessage message;

    protected View contentView;
    protected TextView nameView;

    protected LayoutInflater inflater;

    public MessageRowView(Context context) {
        super(context);
        this.context = context;
        this.inflater = LayoutInflater.from(context);

        inflater.inflate(
                R.layout.chat_container_center, this);
        ViewGroup group = (ViewGroup)findViewById(R.id.content);
        this.contentView = group;
    }

    public MessageRowView(Context context, boolean incomming, boolean isShowUserName) {
        super(context);
        this.context = context;
        this.inflater = LayoutInflater.from(context);

        View convertView;
        if (!incomming) {
            convertView = inflater.inflate(
                    R.layout.chat_container_right, this);
        } else {
            convertView = inflater.inflate(
                    R.layout.chat_container_left, this);

            nameView = (TextView)convertView.findViewById(R.id.name);
            if (isShowUserName) {
                nameView.setVisibility(View.VISIBLE);
            } else {
                nameView.setVisibility(View.GONE);
            }
        }

        ViewGroup group = (ViewGroup)findViewById(R.id.content);
        this.contentView = group;
    }

    public void setMessage(IMessage msg) {
        if (this.message != null) {
            this.message.removePropertyChangeListener(this);
        }
        this.message = msg;
        this.message.addPropertyChangeListener(this);

        this.contentView.setTag(this.message);

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
        } else {
            if (nameView != null) {
                nameView.setText(msg.getSenderName());
            }
        }

        ImageView headerView = (ImageView)findViewById(R.id.header);
        String avatar = msg.getSenderAvatar();
        if (headerView != null && !TextUtils.isEmpty(avatar)) {
            Picasso.with(context)
                    .load(avatar)
                    .placeholder(R.drawable.image_download_fail)
                    .into(headerView);
        }
    }

    public View getContentView() {
        return contentView;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals("failure") ||
                event.getPropertyName().equals("ack") ||
                event.getPropertyName().equals("uploading")) {
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
            if (nameView != null) {
                nameView.setText(this.message.getSenderName());
            }
            ImageView headerView = (ImageView)findViewById(R.id.header);
            String avatar = this.message.getSenderAvatar();
            if (headerView != null && !TextUtils.isEmpty(avatar)) {
                Picasso.with(context)
                        .load(avatar)
                        .placeholder(R.drawable.image_download_fail)
                        .into(headerView);
            }
        }
    }
}