package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

public class Readed extends MessageContent {
    public String msgid;

    public static Readed newReaded(String msgid) {
        JsonObject content = new JsonObject();
        JsonObject json = new JsonObject();
        json.addProperty("msgid", msgid);
        content.add(READED, json);
        Readed readed = new Readed();
        readed.setRaw(content.toString());
        readed.msgid = msgid;
        return readed;
    }

    public MessageContent.MessageType getType() {
        return MessageType.MESSAGE_READED;
    }
}
