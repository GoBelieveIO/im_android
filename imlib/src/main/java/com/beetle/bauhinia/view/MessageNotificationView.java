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
import com.beetle.bauhinia.db.message.Notification;
import com.beetle.imlib.R;

import java.beans.PropertyChangeEvent;

public class MessageNotificationView extends MessageContentView {
    public MessageNotificationView(Context context) {
        super(context);
        final int contentLayout;
        contentLayout = R.layout.chat_content_small_text;
        inflater.inflate(contentLayout, this);
    }

    @Override
    public void setMessage(IMessage msg) {
        super.setMessage(msg);

        TextView content = (TextView) findViewById(R.id.text);
        String text = ((Notification) msg.content).getDescription();
        content.setText(text);

        requestLayout();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getPropertyName().equals("downloading")) {
            TextView content = (TextView) findViewById(R.id.text);
            String text = ((Notification) this.message.content).getDescription();
            content.setText(text);
        }
    }

}
