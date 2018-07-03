package com.beetle.bauhinia.db.message;

public class File extends MessageContent {
    public String url;
    public String filename;
    public int size;

    public MessageType getType() {
        return MessageType.MESSAGE_FILE;
    }

}
