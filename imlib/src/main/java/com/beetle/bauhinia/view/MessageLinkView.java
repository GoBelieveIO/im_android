/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.view;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;
import com.beetle.bauhinia.db.message.Link;
import com.squareup.picasso.Picasso;
import java.beans.PropertyChangeEvent;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.imlib.R;

public class MessageLinkView extends MessageContentView {

    public MessageLinkView(Context context) {
        super(context);
        int contentLayout = R.layout.chat_content_link;
        inflater.inflate(contentLayout, this);
    }

    public void setMessage(IMessage msg) {
        super.setMessage(msg);
        Link linkMsg = (Link) msg.content;

        ImageView imageView = (ImageView)findViewById(R.id.image);
        Picasso.get()
                .load(linkMsg.image)
                .placeholder(R.drawable.image_download_fail)
                .into(imageView);

        TextView titleView = (TextView)findViewById(R.id.title);
        titleView.setText(linkMsg.title);

        TextView contentView = (TextView)findViewById(R.id.descreption);
        contentView.setText(linkMsg.content);

        requestLayout();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);

    }
}
