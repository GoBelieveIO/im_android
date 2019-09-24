package com.beetle.bauhinia.db.message;

import com.beetle.bauhinia.db.IMessage;

public  class Link extends MessageContent {
    public String title;
    public String content;
    public String url;
    public String image;
    public MessageType getType() { return MessageType.MESSAGE_LINK; }
}