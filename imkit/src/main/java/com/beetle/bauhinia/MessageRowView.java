package com.beetle.bauhinia;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;

public class MessageRowView extends FrameLayout implements PropertyChangeListener {

    protected Context context;

    protected IMessage message;

    protected View contentView;

    protected LayoutInflater inflater;

    protected boolean incomming;

    public MessageRowView(Context context) {
        super(context);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setMessage(IMessage msg, boolean incomming) {
        if (this.message != null) {
            this.message.removePropertyChangeListener(this);
        }
        this.message = msg;
        this.message.addPropertyChangeListener(this);
        this.incomming = incomming;

        this.contentView.setTag(this.message);

        if (!incomming) {
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
        }
    }
}