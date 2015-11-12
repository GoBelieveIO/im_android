package com.beetle.bauhinia;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;
import com.squareup.picasso.Picasso;

import java.beans.PropertyChangeEvent;

public class MessageLocationView extends MessageRowView {

    protected ProgressBar progressBar;

    public MessageLocationView(Context context, boolean incomming, boolean isShowUserName) {
        super(context);

        int contentLayout = R.layout.chat_content_location;

        View convertView;

        if (!incomming) {
            convertView = inflater.inflate(
                    R.layout.chat_container_right, this);
        } else {
            convertView = inflater.inflate(
                    R.layout.chat_container_left, this);

            if (isShowUserName) {
                TextView textView = (TextView)convertView.findViewById(R.id.name);
                textView.setVisibility(View.GONE);
            } else {
                TextView textView = (TextView)convertView.findViewById(R.id.name);
                textView.setVisibility(View.GONE);
            }
        }

        ViewGroup group = (ViewGroup)convertView.findViewById(R.id.content);
        group.addView(inflater.inflate(contentLayout, group, false));

        progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        this.contentView = group;

    }

    public void setMessage(IMessage msg, boolean incomming) {
        super.setMessage(msg, incomming);
        IMessage.Location loc = (IMessage.Location)this.message.content;
        if (loc.address != null && loc.address.length() > 0) {
            TextView content = (TextView) findViewById(R.id.text);
            content.setText(loc.address);
        }
        boolean geocoding = this.message.getGeocoding();
        if (geocoding) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
        requestLayout();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getPropertyName().equals("geocoding")) {
            boolean geocoding = this.message.getGeocoding();
            if (geocoding) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                IMessage.Location loc = (IMessage.Location)this.message.content;
                if (loc.address != null && loc.address.length() > 0) {
                    TextView content = (TextView) findViewById(R.id.text);
                    content.setText(loc.address);
                }
            }
        }
    }
}