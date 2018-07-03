package com.beetle.bauhinia.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.imkit.R;

public class MiddleMessageView extends MessageRowView {

    public MiddleMessageView(Context context, MessageContent.MessageType type) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.chat_container_center, this);
        ViewGroup group = (ViewGroup)findViewById(R.id.content);
        addContent(type, group);
    }

    public void setMessage(IMessage msg) {
        super.setMessage(msg);
    }
}
