package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

public class Revoke extends Notification {
    public String msgid;

    public static Revoke newRevoke(String msgid) {
        Revoke revoke = new Revoke();
        JsonObject content = new JsonObject();
        JsonObject json = new JsonObject();
        json.addProperty("msgid", msgid);
        content.add(REVOKE, json);

        revoke.setRaw(content.toString());
        revoke.msgid = msgid;
        return revoke;
    }

    public MessageType getType() {
        return MessageType.MESSAGE_REVOKE;
    }
}
