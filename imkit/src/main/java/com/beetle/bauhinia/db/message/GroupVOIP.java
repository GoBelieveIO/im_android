package com.beetle.bauhinia.db.message;

import com.beetle.bauhinia.db.IMessage;
import com.google.gson.JsonObject;

public  class GroupVOIP extends Notification {
    public long initiator;
    public boolean finished;

    public MessageType getType() {
        return MessageType.MESSAGE_GROUP_VOIP;
    }


    public static GroupVOIP newGroupVOIP(long initiator, boolean finished) {
        GroupVOIP gv = new GroupVOIP();
        JsonObject content = new JsonObject();

        JsonObject json = new JsonObject();
        json.addProperty("initiator", initiator);
        json.addProperty("finished", finished);
        content.add(GROUP_VOIP, json);
        gv.raw = content.toString();

        gv.initiator = initiator;
        gv.finished = finished;
        return gv;
    }



}