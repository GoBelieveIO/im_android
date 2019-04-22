/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.imkit.R;
import com.squareup.picasso.Picasso;

import java.beans.PropertyChangeEvent;

public class InMessageView extends MessageRowView {
    protected TextView nameView;

    public InMessageView(Context context, MessageContent.MessageType type, boolean isShowUserName) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);

        View convertView = inflater.inflate(R.layout.chat_container_left, this);
        nameView = (TextView)convertView.findViewById(R.id.name);
        if (isShowUserName) {
            nameView.setVisibility(View.VISIBLE);
        } else {
            nameView.setVisibility(View.GONE);
        }

        ViewGroup group = (ViewGroup)findViewById(R.id.content);
        addContent(type, group);
        if (type == MessageContent.MessageType.MESSAGE_TEXT) {
            //文本消息的背景直接设置到textview上
            group.setBackgroundResource(0);
            group.setPadding(0, 0, 0, 0);
        }
    }



    public void setMessage(IMessage msg) {
        super.setMessage(msg);
        if (nameView != null) {
            nameView.setText(msg.getSenderName());
        }
    }



    @Override
    public void propertyChange(PropertyChangeEvent event) {
         if (event.getPropertyName().equals("senderName")) {
            if (nameView != null) {
                nameView.setText(this.message.getSenderName());
            }
            ImageView headerView = (ImageView)findViewById(R.id.header);
            String avatar = this.message.getSenderAvatar();
            if (headerView != null && !TextUtils.isEmpty(avatar)) {
                Picasso.with(getContext())
                        .load(avatar)
                        .placeholder(R.drawable.avatar_contact)
                        .into(headerView);
            }
        }
    }

}
