/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.view;

import android.content.Context;
import android.widget.TextView;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.imlib.R;

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
