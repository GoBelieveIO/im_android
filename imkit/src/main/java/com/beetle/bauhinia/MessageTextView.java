package com.beetle.bauhinia;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;

public class MessageTextView extends MessageRowView {
    public MessageTextView(Context context, boolean incomming, boolean isShowUserName) {
        super(context);
        final int contentLayout;
        contentLayout = R.layout.chat_content_text;

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

        this.contentView = group;
    }

    @Override
    public void setMessage(IMessage msg, boolean incomming) {
        super.setMessage(msg, incomming);

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