package com.beetle.bauhinia;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;

import java.beans.PropertyChangeEvent;

public class MessageNotificationView extends MessageRowView {
    public MessageNotificationView(Context context) {
        super(context);
        final int contentLayout;
        contentLayout = R.layout.chat_content_small_text;

        ViewGroup group = (ViewGroup)this.findViewById(R.id.content);
        group.addView(inflater.inflate(contentLayout, group, false));
    }

    @Override
    public void setMessage(IMessage msg) {
        super.setMessage(msg);

        TextView content = (TextView) findViewById(R.id.text);
        String text = ((IMessage.Notification) msg.content).getDescription();
        content.setText(text);

        requestLayout();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getPropertyName().equals("downloading")) {
            TextView content = (TextView) findViewById(R.id.text);
            String text = ((IMessage.Notification) this.message.content).getDescription();
            content.setText(text);
        }
    }

}