package com.beetle.bauhinia;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
                //发送失败
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.VISIBLE);
            } else {
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.GONE);
            }
        }
    }

    public View getContentView() {
        return contentView;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals("failure")) {
            if (message.isFailure()) {
                //发送失败
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.VISIBLE);
            } else {
                ImageView flagView = (ImageView) findViewById(R.id.flag);
                flagView.setVisibility(View.GONE);
            }
        } else if (event.getPropertyName().equals("ack")) {
            Log.i("gobelieve", "ack:" + message.isAck());
        }
    }
}