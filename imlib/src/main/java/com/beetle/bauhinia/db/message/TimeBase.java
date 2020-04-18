package com.beetle.bauhinia.db.message;


import com.beetle.bauhinia.db.IMessage;
import com.google.gson.JsonObject;

public  class TimeBase extends Notification {
    public int timestamp;
    public MessageType getType() {
        return MessageType.MESSAGE_TIME_BASE;
    }


    public static TimeBase newTimeBase(int timestamp) {
        TimeBase tb = new TimeBase();
        JsonObject content = new JsonObject();
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", timestamp);
        content.add(TIMEBASE, json);
        tb.raw = content.toString();
        tb.timestamp = timestamp;
        return tb;
    }

}
