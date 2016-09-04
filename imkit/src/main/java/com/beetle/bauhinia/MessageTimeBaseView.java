package com.beetle.bauhinia;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;

public class MessageTimeBaseView extends MessageRowView {
    public MessageTimeBaseView(Context context) {
        super(context);
        final int contentLayout;
        contentLayout = R.layout.chat_content_small_text;

        ViewGroup group = (ViewGroup)this.findViewById(R.id.content);
        group.addView(inflater.inflate(contentLayout, group, false));
    }

    public void setTimeBaseMessage(IMessage msg, String t) {
        super.setMessage(msg, true);
        TextView content = (TextView) findViewById(R.id.text);
        content.setText(t);
        requestLayout();
    }




}