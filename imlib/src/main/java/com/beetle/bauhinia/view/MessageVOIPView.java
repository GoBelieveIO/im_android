/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.VOIP;
import com.beetle.imlib.R;

/**
 * Created by houxh on 2017/11/14.
 */

public class MessageVOIPView extends MessageContentView {

    public MessageVOIPView(Context context) {
        super(context);
        inflater.inflate(R.layout.chat_content_voip, this);
    }

    @Override
    public void setMessage(IMessage msg) {
        super.setMessage(msg);

        VOIP voip = (VOIP)msg.content;

        int m = voip.duration/60;
        int s = voip.duration%60;
        String t = String.format("%02d:%02d", m, s);

        String text = "未知状态";
        if (msg.isOutgoing) {
            switch (voip.flag) {
                case VOIP.VOIP_FLAG_ACCEPTED:
                    text = t;
                    break;
                case VOIP.VOIP_FLAG_REFUSED:
                    text = "对方已拒绝";
                    break;
                case VOIP.VOIP_FLAG_CANCELED:
                    text = "已取消";
                    break;
                case VOIP.VOIP_FLAG_UNRECEIVED:
                    text = "对方未接听";
                    break;
            }
        } else {
            switch (voip.flag) {
                case VOIP.VOIP_FLAG_ACCEPTED:
                    text = t;
                    break;
                case VOIP.VOIP_FLAG_REFUSED:
                    text = "已拒绝";
                    break;
                case VOIP.VOIP_FLAG_CANCELED:
                    text = "对方已取消";
                    break;
                case VOIP.VOIP_FLAG_UNRECEIVED:
                    text = "未接听";
                    break;
            }
        }
        if (msg.isOutgoing) {
            View v = findViewById(R.id.phone);
            ViewGroup parent = (ViewGroup)findViewById(R.id.voip);
            parent.removeView(v);
            parent.addView(v);
        }

        MessageContent.MessageType mediaType = message.getType();
        TextView content = (TextView) findViewById(R.id.text);
        content.setText(text);
        requestLayout();
    }
}
