package com.beetle.bauhinia.db.message;


import com.beetle.bauhinia.db.IMessage;
import com.google.gson.JsonObject;

public  class Attachment extends MessageContent {
    public int msg_id;
    public String address;
    public String url;

    public MessageType getType() {
        return MessageType.MESSAGE_ATTACHMENT;
    }



    public static Attachment newAttachment(int msgLocalID, String address) {
        Attachment attachment = new Attachment();
        JsonObject content = new JsonObject();
        JsonObject attachmentJson = new JsonObject();
        attachmentJson.addProperty("msg_id", msgLocalID);
        attachmentJson.addProperty("address", address);
        content.add(ATTACHMENT, attachmentJson);
        attachment.raw = content.toString();
        attachment.address = address;
        attachment.msg_id = msgLocalID;
        return attachment;
    }

    public static Attachment newURLAttachment(int msgLocalID, String url) {
        Attachment attachment = new Attachment();
        JsonObject content = new JsonObject();
        JsonObject attachmentJson = new JsonObject();
        attachmentJson.addProperty("msg_id", msgLocalID);
        attachmentJson.addProperty("url", url);
        content.add(ATTACHMENT, attachmentJson);
        attachment.raw = content.toString();
        attachment.url = url;
        attachment.msg_id = msgLocalID;
        return attachment;
    }
}