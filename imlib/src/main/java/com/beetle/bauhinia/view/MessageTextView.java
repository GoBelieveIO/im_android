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
import com.beetle.bauhinia.db.message.Text;
import com.beetle.imlib.R;

public class MessageTextView extends MessageContentView {
    private TextView textView;

    public MessageTextView(Context context) {
        super(context);
        inflater.inflate(R.layout.chat_content_text, this);
        textView = (TextView)findViewById(R.id.text);
    }

    @Override
    public void setMessage(IMessage msg) {
        super.setMessage(msg);

        this.textView.setTag(message);

        MessageContent.MessageType mediaType = message.content.getType();

        if (mediaType == MessageContent.MessageType.MESSAGE_TEXT) {
            TextView content = (TextView) findViewById(R.id.text);
            Text text = (Text)msg.content;
            if (text.spanText != null) {
                content.setText(text.spanText);
            } else {
                content.setText(text.text);
            }
        }
        requestLayout();
    }
}
