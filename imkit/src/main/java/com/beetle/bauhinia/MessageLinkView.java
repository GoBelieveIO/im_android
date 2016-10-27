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

public class MessageLinkView extends MessageRowView {

    public MessageLinkView(Context context, boolean incomming, boolean isShowUserName) {
        super(context, incomming, isShowUserName);

        int contentLayout = R.layout.chat_content_link;

        ViewGroup group = (ViewGroup)this.findViewById(R.id.content);
        group.addView(inflater.inflate(contentLayout, group, false));
    }

    public void setMessage(IMessage msg) {
        super.setMessage(msg);
        IMessage.Link linkMsg = (IMessage.Link) msg.content;

        ImageView imageView = (ImageView)findViewById(R.id.image);
        Picasso.with(context)
                .load(linkMsg.image)
                .placeholder(R.drawable.image_download_fail)
                .into(imageView);

        TextView titleView = (TextView)findViewById(R.id.title);
        titleView.setText(linkMsg.title);

        TextView contentView = (TextView)findViewById(R.id.descreption);
        contentView.setText(linkMsg.content);

        requestLayout();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);

    }
}