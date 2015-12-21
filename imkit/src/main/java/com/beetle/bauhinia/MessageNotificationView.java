package com.beetle.bauhinia;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;

public class MessageNotificationView extends MessageRowView {
    public MessageNotificationView(Context context) {
        super(context);
        final int contentLayout;
        contentLayout = R.layout.chat_content_text;

        ViewGroup group = (ViewGroup)this.findViewById(R.id.content);
        group.addView(inflater.inflate(contentLayout, group, false));
    }

    @Override
    public void setMessage(IMessage msg, boolean incomming) {
        super.setMessage(msg, incomming);

        TextView content = (TextView) findViewById(R.id.text);
        String text = ((IMessage.GroupNotification) msg.content).description;
        content.setText(text);

        requestLayout();
    }



}