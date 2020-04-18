package com.beetle.bauhinia.db.message;


import com.beetle.bauhinia.db.IMessage;
import com.google.gson.JsonObject;

public  class Headline extends Notification {
    public String headline;

    public String getDescription() {
        return this.headline;
    }
    public MessageType getType() {
        return MessageType.MESSAGE_HEADLINE;
    }



    public static Headline newHeadline(String headline) {
        Headline head = new Headline();
        JsonObject content = new JsonObject();
        JsonObject headlineJson = new JsonObject();
        headlineJson.addProperty("headline", headline);
        content.add(HEADLINE, headlineJson);
        head.raw = content.toString();
        head.headline = headline;
        head.description = headline;
        return head;
    }

}
