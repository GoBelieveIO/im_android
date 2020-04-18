package com.beetle.bauhinia.db.message;


import com.beetle.bauhinia.db.IMessage;
import com.google.gson.JsonObject;

public  class VOIP extends MessageContent {

    public static final int VOIP_FLAG_CANCELED = 1;
    public static final int VOIP_FLAG_REFUSED = 2;
    public static final int VOIP_FLAG_ACCEPTED = 3;
    public static final int VOIP_FLAG_UNRECEIVED = 4;



    public int flag;
    public int duration;//秒
    public boolean videoEnabled;

    public MessageType getType() {
        return MessageType.MESSAGE_VOIP;
    }



    public static VOIP newVOIP(int flag, int duration, boolean videoEnabled) {
        VOIP v = new VOIP();
        JsonObject content = new JsonObject();

        JsonObject json = new JsonObject();
        json.addProperty("flag", flag);
        json.addProperty("duration", duration);
        json.addProperty("video_enabled", videoEnabled);
        content.add(VOIP, json);

        v.raw = content.toString();
        v.flag = flag;
        v.duration = duration;
        v.videoEnabled = videoEnabled;

        return v;
    }

}
