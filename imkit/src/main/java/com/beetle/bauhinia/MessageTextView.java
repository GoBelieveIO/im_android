package com.beetle.bauhinia;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;

public class MessageTextView extends MessageRowView {
    public MessageTextView(Context context, boolean incomming, boolean isShowUserName) {
        super(context, incomming, isShowUserName);
        final int contentLayout;
        contentLayout = R.layout.chat_content_text;

        ViewGroup group = (ViewGroup)this.findViewById(R.id.content);
        group.addView(inflater.inflate(contentLayout, group, false));
    }

    @Override
    public void setMessage(IMessage msg) {
        super.setMessage(msg);

        IMessage.MessageType mediaType = message.content.getType();

        if (mediaType == IMessage.MessageType.MESSAGE_TEXT) {
            TextView content = (TextView) findViewById(R.id.text);
            String text = ((IMessage.Text) msg.content).text;
            content.setText(text);
        } else {
            TextView content = (TextView) findViewById(R.id.text);
            content.setText("unknown");
        }
        requestLayout();
    }



}