package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

public class ACK extends Notification {
    public int error;
    public MessageType getType() {
        return MessageType.MESSAGE_ACK;
    }


    public static ACK newACK(int error) {
        ACK ack = new ACK();
        JsonObject content = new JsonObject();
        JsonObject json = new JsonObject();
        json.addProperty("error", error);
        content.add(ACK, json);
        ack.raw = content.toString();
        ack.error = error;
        return ack;
    }
}
