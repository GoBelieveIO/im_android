package com.beetle.bauhinia.view;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.imlib.R;

public class MessageClassroomView extends MessageContentView {
    public MessageClassroomView(Context context) {
        super(context);
        inflater.inflate(R.layout.chat_content_voip, this);
    }

    @Override
    public void setMessage(IMessage msg) {
        super.setMessage(msg);
        ImageView v = findViewById(R.id.phone);
        v.setImageResource(R.drawable.classroom);
        String text = "发起了群课堂";
        TextView content = (TextView) findViewById(R.id.text);
        content.setText(text);
        requestLayout();
    }
}
