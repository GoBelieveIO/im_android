package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

public class Secret extends MessageContent {
    public String ciphertext;
    public int type;


    public static Secret newSecret(String ciphertext, int type) {
        Secret s = new Secret();

        JsonObject content = new JsonObject();
        JsonObject audioJson = new JsonObject();
        audioJson.addProperty("ciphertext", ciphertext);
        audioJson.addProperty("type", type);
        content.add(SECRET, audioJson);
        s.raw = content.toString();

        s.ciphertext = ciphertext;
        s.type = type;
        return s;
    }


    public MessageType getType() {
        return MessageType.MESSAGE_SECRET;
    }

}
