package com.beetle.bauhinia.view;

import android.content.Context;
import android.widget.TextView;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.imkit.R;

public class MessageUnknownView extends MessageContentView {
    public MessageUnknownView(Context context) {
        super(context);
        inflater.inflate(R.layout.chat_content_text, this);
    }
    @Override
    public void setMessage(IMessage msg) {
        super.setMessage(msg);
        TextView content = (TextView) findViewById(R.id.text);
        if (msg.getType() == MessageContent.MessageType.MESSAGE_SECRET) {
            content.setText("消息未能解密");
        } else {
            content.setText("未知的消息类型");
        }
        requestLayout();
    }
}